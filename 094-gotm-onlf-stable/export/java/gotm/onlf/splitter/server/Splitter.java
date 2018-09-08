/***
 * Splitter.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.server;

import java.io.*;
import java.net.*;
import java.util.*;

import zs42.parts.*;
import zs42.junction.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class Splitter
{
  static class Packet
  {
    int stream_id;
    byte[] encoding;
    
    Packet(int stream_id, byte[] encoding)
    {
      this.stream_id = stream_id;
      this.encoding = encoding;
    }
  }
  
  static class SplitterContext
  {
    byte[] pass_root;
    byte[] pass_user;
    
    // all_packets protected by <code>this</code> object's monitor
    BlockingJunction  all_packetj = (new BlockingJunction());
    volatile int      all_packetc = 0;
    /******/ Packet[] all_packets = (new Packet[SESSION_PACKETS_MAXIMUM]);
    
    DataOutputStream feedback;
    
    SplitterContext(byte[] pass_root, byte[] pass_user, DataOutputStream feedback)
    {
      this.pass_root = pass_root;
      this.pass_user = pass_user;
      
      this.feedback = feedback;
    }
    
    synchronized void synchronizedEnqueuePacket(Packet packet)
    {
      int i = all_packetc;
      all_packets[i++] = packet;
      all_packetc = i;
      all_packetj.changed();
    }
  }
  
  static abstract class BaseServer extends AbstractServer<SplitterContext>
  {
    static int identifier = 0;
    
    static volatile Boolean should_accept = Boolean.TRUE;
    static final F0<Boolean> should_accept_reader = new F0<Boolean>() { public Boolean invoke() { return should_accept; } };
    
    int id;
    
    SplitterContext ctx;
    
    IncomingNetworkStream inp;
    OutgoingNetworkStream out;
    
    byte[] source_descriptor;
    
    BaseServer()
    {
      synchronized (BaseServer.class) {
        id = identifier++;
      }
    }
    
    void init(SplitterContext context, Socket client)
    {
      ctx = context;
      
      try {
        inp = (new IncomingNetworkStream(client.getInputStream(),  TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
        out = (new OutgoingNetworkStream(client.getOutputStream(), TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
        
        source_descriptor = ("" + client.getRemoteSocketAddress()).getBytes("UTF-8");
      } catch (Exception e) {
        throw (new BAD(e));
      }
    }
  }
  
  static class RootServer extends BaseServer
  {
    void handle()
    {
      while (true) {
        // turn network buffer
        inp.turn();
        
        // read packet
        GroupirPacket groupir = GroupirPacket.recv(inp, ctx.pass_root);
        
        // make sure the packet has the required metadata
        if (groupir.dI.length < PACKET_METADATA_LENGTH) throw (new BAD());
        
        // extract stream identifier
        int stream_id = groupir.dI[PACKET_METADATA_STREAM_ID];
        
        // check for stop condition
        if (stream_id == STREAM_STOP) break;
        
        // punch server (receive) timecode (UPDATE: no longer!)
        // groupir.dI[PACKET_METADATA_SERVER_TC] = ((int)(microTime()));
        
        // enqueue packet
        ctx.synchronizedEnqueuePacket((new Packet(stream_id, groupir.send(ctx.pass_user))));
      }
    }
    
    public void handle(SplitterContext context, Socket client)
    {
      try {
        init(context, client);
        handle();
        client.close();
      } catch (Throwable e) {
        log("" + id + ": root port thread failure", e);
        // do not bomb; try to continue operating
      }
    }
  }
  
  static class UserServer extends BaseServer
  {
    final BlockingJunction stream_command_junction = (new BlockingJunction());
    
    volatile long stream_block_vector = 0;
    volatile long stream_trans_vector = 0;
    volatile long stream_skip_packetc = 0;
    
    void handle()
    {
      Authentication.challenge(inp, out, ctx.pass_user);
      
      log("" + id + ": new client passed authentication");
      
      final int ini_packetc = ctx.all_packetc;
      
      // dedicated input thread
      (new Thread() {
          public void run() {
            try {
              long instance_id;
              
              // read instance id
              {
                inp.turn();
                inp.readahead(8);
                instance_id = (inp.rL() & (~(1L << 63)));
                
                log("" + id + ": got instance id: " + instance_id);
                
                synchronized (ctx.feedback) {
                  DataOutputStream out = ctx.feedback;
                  
                  out.writeInt(8 + 1 + source_descriptor.length);
                  out.writeLong(instance_id);
                  out.writeByte(0); // token
                  out.write(source_descriptor);
                }
              }
              
              int receive_counter = 0;
              
              byte[] receive_buffer = (new byte[MAXIMUM_FEEDBACK_PACKET_LENGTH]);
              
              while (true) {
                inp.turn();
                inp.readahead(4);
                int length = inp.rI();
                
                if (length == -1) {
                  // stream select vector
                  inp.turn();
                  inp.readahead(8 + 8 + 8);
                  stream_block_vector = ~(inp.rL()); // invert to obtain stream block vector; usually write enable will be set now
                  stream_trans_vector = inp.rL();
                  stream_skip_packetc = inp.rL();
                  
                  log("" + id + ": new stream vectors (block " + stream_block_vector + ", trans " + stream_trans_vector + ") and skip initial " + stream_skip_packetc + " packets");
                  
                  stream_command_junction.changed();
                } else {
                  // return packet (student feedback or status notification)
                  
                  if (!((0 <= length) && (length <= MAXIMUM_FEEDBACK_PACKET_LENGTH))) throw null;
                  if (!((receive_counter += length) < MAXIMUM_FEEDBACK_OUTPUT_LENGTH)) throw null;
                  
                  log("" + id + ": return packet of length " + length + ", reading now");
                  
                  inp.turn();
                  inp.readahead(length);
                  inp.rB(receive_buffer, 0, length);
                  
                  log("" + id + ": read, saving now");
                  
                  synchronized (ctx.feedback) {
                    DataOutputStream out = ctx.feedback;
                    
                    out.writeInt(length + 8);
                    out.writeLong(instance_id);
                    out.write(receive_buffer, 0, length);
                  }
                  
                  log("" + id + ": saved");
                }
              }
            } catch (Throwable e) {
              while (true) {
                // the following causes are somewhat expected; ignore them
                if (e.getCause() instanceof SocketException) break;
                
                // something unexpected happened; report it
                log("" + id + ": user port dedicated input thread failure", e); break;
              }
              
              // do not bomb; try to continue operating
            }
          }
        }).start();
      
      // current thread becomes dedicated output thread
      {
        log("" + id + ": enter send loop");
        
        BlockingJunction.BlockingWaiter packet_waiter = ctx.all_packetj.waiter();
        BlockingJunction.BlockingWaiter stream_waiter = stream_command_junction.waiter();
        
        int all_packeti = 0;
        
        while (true) {
          log("" + id + ": waiting for a packet to become available");
          while (packet_waiter.waitfor(ctx.all_packetc > all_packeti));
          
          int cur_packeti = all_packeti++;
          Packet packet = ctx.all_packets[cur_packeti];
          log("" + id + ": got packet (stream_id " + packet.stream_id + ")");
          
          log("" + id + ": waiting for write enable");
          while (stream_waiter.waitfor((stream_block_vector >>> 63) == 1));
          
          log("" + id + ": checking packet's stream identifier against block vector");
          if ((stream_block_vector & (1 << packet.stream_id)) != 0) continue;
          
          if (cur_packeti < ini_packetc) {
            log("" + id + ": checking packet's stream identifier against transient vector");
            if ((stream_trans_vector & (1 << packet.stream_id)) != 0) continue;
          }
          
          if (stream_skip_packetc > 0) {
            if (packet.stream_id == STREAM_STOP) {
              log("" + id + ": found stop condition on a packet that would be skipped ... THIS IS BAD STUFF !!!");
              break;
            } else {
              log("" + id + ": skipping a packet (length " + packet.encoding.length + ")");
              stream_skip_packetc--;
              continue;
            }
          }
          
          log("" + id + ": writing a packet (length " + packet.encoding.length + ")");
          out.wB(packet.encoding);
          out.writeback();
          
          log("" + id + ": checking for stop condition");
          if (packet.stream_id == STREAM_STOP) break;
        }
        
        log("" + id + ": leave send loop");
      }
    }
    
    public void handle(SplitterContext context, Socket client)
    {
      try {
        init(context, client);
        handle();
        client.close();
      } catch (Throwable e) {
        log("" + id + ": user port thread failure", e);
        // do not bomb; try to continue operating
      }
    }
  }
  
  static class StopServer extends BaseServer
  {
    void handle()
    {
      Authentication.challenge(inp, out, ctx.pass_root);
      
      log("stop request passed authentication");
      
      // post stop condition
      int now = ((int)(microTime()));
      int[] metadata = new int[] { STREAM_STOP, now, now };
      if (metadata.length != PACKET_METADATA_LENGTH) throw new BAD();
      byte[] encoding = (new GroupirPacket(emptyL, metadata, emptyS, emptyB)).send(ctx.pass_user);
      ctx.synchronizedEnqueuePacket(new Packet(STREAM_STOP, encoding));
      
      // stop accepting new connections
      BaseServer.should_accept = Boolean.FALSE;
      
      log("starting countdown");
      
      try {
        // everyone has 10 seconds to receieve the empty packet and shut down cleanly
        Thread.sleep(10000);
      } catch (Exception e) {
        throw new BAD(e);
      }
      
      log("finished countdown");
      
      System.exit(0);
    }
    
    public void handle(SplitterContext context, Socket client)
    {
      try {
        init(context, client);
        handle();
        client.close();
      } catch (Throwable e) {
        log("" + id + ": stop port thread failure", e);
        // do not bomb; try to continue operating
      }
    }
  }
  
  public static void main(String[] args)
  {
    int nr = 0;
    
    final int ARGI_PASS_ROOT = (nr++);
    final int ARGI_PASS_USER = (nr++);
    final int ARGI_PORT_ROOT = (nr++);
    final int ARGI_PORT_USER = (nr++);
    final int ARGI_PORT_STOP = (nr++);
    final int ARGI_FEED_NAME = (nr++);
    
    try {
      byte[] pass_root = read_file_bytes(args[ARGI_PASS_ROOT], (new byte[SHAZ]));
      byte[] pass_user = read_file_bytes(args[ARGI_PASS_USER], (new byte[SHAZ]));
      
      final DataOutputStream feedback = (new DataOutputStream(new FileOutputStream(args[ARGI_FEED_NAME])));
      
      (new Thread()
        {
          public void run()
          {
            try {
              while (true) {
                feedback.flush();
                
                try {
                  sleep(250);
                } catch (InterruptedException e) {
                  // ignore
                }
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
      
      final SplitterContext context = (new SplitterContext(pass_root, pass_user, feedback));
      
      int port_root = Integer.parseInt(args[ARGI_PORT_ROOT]);
      int port_user = Integer.parseInt(args[ARGI_PORT_USER]);
      int port_stop = Integer.parseInt(args[ARGI_PORT_STOP]);
      
      F0<RootServer> root_factory = (new F0<RootServer>() { public RootServer invoke() { return (new RootServer()); } });
      F0<UserServer> user_factory = (new F0<UserServer>() { public UserServer invoke() { return (new UserServer()); } });
      F0<StopServer> stop_factory = (new F0<StopServer>() { public StopServer invoke() { return (new StopServer()); } });
      
      AbstractServer.launch(root_factory, context, port_root, BaseServer.should_accept_reader, fatal_F1, fatal_F1, fatal_F1);
      AbstractServer.launch(user_factory, context, port_user, BaseServer.should_accept_reader, fatal_F1, fatal_F1, fatal_F1);
      AbstractServer.launch(stop_factory, context, port_stop, BaseServer.should_accept_reader, fatal_F1, fatal_F1, fatal_F1);
    } catch (Throwable e) {
      fatal(e);
    }
  }
}

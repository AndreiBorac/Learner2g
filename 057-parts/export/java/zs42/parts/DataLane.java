/***
 * DataLane.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.security.*;

/***
 * DataLane is a packet router. Every node receives each packet posted
 * by any node. Nodes receive their own packets unless they install
 * "completions" on those packets instead. If a completion is
 * specified, it is run in lieu of receiving the packet. This allows
 * nodes to "effect" the operations required by packets they generate
 * without re-parsing the packets and allows special case processing
 * for self-generated packets. Of course, where such special case
 * handling is not needed, it is simpler to allow normal parsing and
 * processing of the self-generated packet.
 ***/
public class DataLane
{
  private static final String FILE_NONE = "/dev/null";
  
  private static final String FILE_MARKER = "DATALANE";
  
  private static final int FLUSH_DELAY = 0; /* milliseconds */
  
  private static final int LONG_DELAY = 1000 * 1000; /* milliseconds */
  
  public static class Packet
  {
    final TreeMap<String, String> attrs = (new TreeMap<String, String>());
    final TreeMap<String, byte[]> items = (new TreeMap<String, byte[]>());
    
    F0<Void> completion = null;
    
    public Packet()
    {
    }
    
    public Packet(F0<Void> completion)
    {
      this.completion = completion;
    }
    
    public Packet setCompletion(F0<Void> completion)
    {
      this.completion = completion;
      
      return this;
    }
    
    public Packet setAttr(String key, String val)
    {
      attrs.put(key, val);
      
      return this;
    }
    
    public boolean hasAttr(String key)
    {
      return attrs.containsKey(key);
    }
    
    public String getAttr(String key)
    {
      String val = attrs.get(key);
      
      if (val == null) throw null;
      
      return val;
    }
    
    public Packet setItem(String key, byte[] val)
    {
      items.put(key, val);
      
      return this;
    }
    
    public boolean hasItem(String key)
    {
      return items.containsKey(key);
    }
    
    public byte[] getItem(String key)
    {
      byte[] val = items.get(key);
      
      if (val == null) throw null;
      
      return val;
    }
    
    public void writeTo(DataOutputStream out) throws IOException
    {
      // attrs
      {
        out.writeInt(attrs.size());
        
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
          SimpleIO.writeByteArrayString(out, entry.getKey());
          SimpleIO.writeByteArrayString(out, entry.getValue());
        }
      }
      
      // items
      {
        out.writeInt(items.size());
        
        for (Map.Entry<String, byte[]> entry : items.entrySet()) {
          SimpleIO.writeByteArrayString(out, entry.getKey());
          SimpleIO.writeByteArray(out, entry.getValue());
        }
      }
    }
    
    public static Packet readFrom(DataInputStream inp) throws IOException
    {
      Packet packet = (new Packet());
      
      // attrs
      {
        int attrc = inp.readInt();
        
        while (attrc-- > 0) {
          packet.attrs.put(SimpleIO.readByteArrayString(inp), SimpleIO.readByteArrayString(inp));
        }
      }
      
      // items
      {
        int itemc = inp.readInt();
        
        while (itemc-- > 0) {
          packet.items.put(SimpleIO.readByteArrayString(inp), SimpleIO.readByteArray(inp));
        }
      }
      
      return packet;
    }
  }
  
  final byte[] kmac;
  final ArrayList<Packet> history = (new ArrayList<Packet>());
  final LinkedBlockingQueue<Packet> program = (new LinkedBlockingQueue<Packet>());
  final ArrayList<LinkedBlockingQueue<Packet>> targets = (new ArrayList<LinkedBlockingQueue<Packet>>());
  
  final ThreadLocal<ByteArrayOutputStream> encodePacketBuffer = (new ThreadLocal<ByteArrayOutputStream>() { protected ByteArrayOutputStream initialValue() { return (new ByteArrayOutputStream()); } } );
  
  byte[] encodePacketToBytes(Packet packet) throws IOException
  {
    ByteArrayOutputStream tmp = encodePacketBuffer.get();
    
    tmp.reset();
    
    packet.writeTo((new DataOutputStream(tmp)));
    
    return tmp.toByteArray();
  }
  
  final ThreadLocal<MessageDigest> encodePacketDigest = (new ThreadLocal<MessageDigest>() { protected MessageDigest initialValue() { try { return MessageDigest.getInstance("SHA-256"); } catch (Throwable e) { throw Log.fatal(e); } } } );
  
  void encodePacketWithMac(Packet packet, DataOutputStream out) throws IOException
  {
    byte[] data = encodePacketToBytes(packet);
    
    MessageDigest md = encodePacketDigest.get();
    
    md.reset();
    md.update(kmac);
    byte[] calc = md.digest(data);
    
    out.write(calc);
    SimpleIO.writeByteArray(out, data);
  }
  
  Packet decodePacketWithMac(DataInputStream inp, boolean trusted) throws IOException
  {
    MessageDigest md = encodePacketDigest.get();
    
    byte[] csum = (new byte[md.getDigestLength()]);
    
    inp.readFully(csum);
    
    byte[] data = SimpleIO.readByteArray(inp);
    
    md.reset();
    md.update(kmac);
    byte[] calc = md.digest(data);
    
    if (!trusted) {
      for (int i = 0; i < calc.length; i++) {
        if (csum[i] != calc[i]) throw null;
      }
    }
    
    return Packet.readFrom((new DataInputStream((new ByteArrayInputStream(data)))));
  }
  
  LinkedBlockingQueue<Packet> newTarget()
  {
    synchronized (this) {
      LinkedBlockingQueue<Packet> target = (new LinkedBlockingQueue<Packet>());
      
      for (Packet packet : history) {
        target.add(packet);
      }
      
      targets.add(target);
      
      return target;
    }
  }
  
  void postPacketFromTarget(LinkedBlockingQueue<Packet> target_shadow, Packet packet)
  {
    try {
      synchronized (this) {
        history.add(packet);
        
        for (LinkedBlockingQueue<Packet> target : targets) {
          if (target != target_shadow) {
            target.put(packet);
          }
        }
      }
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public DataLane(final byte[] kmac, final String load, final String save, final String[] list, final int port)
  {
    this.kmac = kmac;
    
    /* preload */
    {
      if (!(load.equals(FILE_NONE))) {
        try {
          DataInputStream dis = (new DataInputStream(new BufferedInputStream(new FileInputStream(load))));
          
          if (!(SimpleIO.readByteArrayString(dis).equals(FILE_MARKER))) throw null;
          
          while (true) {
            program.put(decodePacketWithMac(dis, false));
          }
        } catch (EOFException e) {
          // expected
        } catch (Throwable e) {
          throw (new RuntimeException(e));
        }
      }
    }
    
    /* tracing target */
    {
      final LinkedBlockingQueue<Packet> tracing = newTarget();
      
      Log.startLoggedThread
        ((new Log.LoggedThread()
          {
            protected void run() throws Exception
            {
              DataOutputStream dos = (new DataOutputStream(new BufferedOutputStream(new FileOutputStream(save))));
              
              SimpleIO.writeByteArrayString(dos, FILE_MARKER);
              
              ArrayList<Packet> caching = (new ArrayList<Packet>());
              
              while (true) {
                caching.add(tracing.take());
                tracing.drainTo(caching);
                
                for (Packet packet : caching) {
                  encodePacketWithMac(packet, dos);
                }
                
                dos.flush();
                
                /*
                  surprisingly, the file target is also responsible
                  for queueing packets for the application logic. this
                  is to ensure that input packets are written to disk
                  before they have the opportunity to crash the
                  application. this also means that FLUSH_DELAY should
                  be set rather low to preserve responsiveness.
                  
                  for the lowest possible latency, FLUSH_DELAY can be
                  set to zero; in this case, the save file should
                  reside on ramfs or tmpfs. a script can copy it to
                  permanent storage when it stops growing.
                 */
                
                for (Packet packet : caching) {
                  program.put(packet);
                }
                
                caching.clear();
                
                if (FLUSH_DELAY != 0) {
                  Thread.sleep(FLUSH_DELAY);
                }
              }
            }
          }));
    }
    
    /* networking - client-side */
    {
      for (String peer : list) {
        String[] pair = peer.split(Pattern.quote(":"));
        
        if (pair.length != 2) {
          Log.log("skipping malformed peer specification '" + peer + "'");
          continue;
        }
        
        final String peer_host = pair[0];
        final int    peer_port = Integer.parseInt(pair[1]);
        
        Log.startLoggedThread
          ((new Log.LoggedThread()
            {
              protected void run() throws Exception
              {
                final Socket socket = (new Socket(peer_host, peer_port));
                startDaemons(socket);
              }
            }));
      }
    }
    
    /* networking - server-side */
    {
      AbstractServer.launch
        (port,
         (new F1<Void, Socket>()
          {
            public Void invoke(Socket client)
            {
              try {
                startDaemons(client);
                
                /* we need to hang here; otherwise, AbstractServer would close the connection */
                {
                  while (true) {
                    Thread.sleep(LONG_DELAY);
                  }
                }
              } catch (Throwable e) {
                throw Log.fatal(e);
              }
            }
          }));
    }
  }
  
  void startRecvDaemon(final LinkedBlockingQueue<Packet> target, final DataInputStream inp)
  {
    Log.startLoggedThread
      ((new Log.LoggedThread()
        {
          protected void run() throws Exception
          {
            while (true) {
              Packet packet = decodePacketWithMac(inp, false);
              postPacketFromTarget(target, packet);
            }
          }
        }));
  }
  
  void startSendDaemon(final LinkedBlockingQueue<Packet> target, final DataOutputStream out)
  {
    Log.startLoggedThread
      ((new Log.LoggedThread()
        {
          protected void run() throws Exception
          {
            while (true) {
              Packet packet = target.take();
              encodePacketWithMac(packet, out);
              out.flush();
            }
          }
        }));
  }
  
  void startDaemons(Socket peer) throws IOException
  {
    final LinkedBlockingQueue<Packet> target = newTarget();
    
    startRecvDaemon(target, (new DataInputStream (new BufferedInputStream (peer.getInputStream ()))));
    startSendDaemon(target, (new DataOutputStream(new BufferedOutputStream(peer.getOutputStream()))));
  }
  
  public void postPacket(Packet packet)
  {
    if (packet.completion == null) {
      packet.completion = F0.NULL;
    }
    
    postPacketFromTarget(program, packet);
  }
  
  public Packet recvPacket()
  {
    try {
      while (true) {
        Packet packet = program.take();
        
        if (packet.completion != null) {
          packet.completion.invoke();
        } else {
          return packet;
        }
      }
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
}

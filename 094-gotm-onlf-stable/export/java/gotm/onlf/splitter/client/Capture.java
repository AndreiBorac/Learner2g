/***
 * Capture.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.client;

import java.io.*;
import java.net.*;
import java.security.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class Capture
{
  public static void main(String[] args)
  {
    final int ARGI_HOST_USER = 0;
    final int ARGI_PORT_USER = 1;
    final int ARGI_PASS_USER = 2;
    final int ARGI_WANT_BITS = 3;
    final int ARGI_SAVE_FILE = 4;
    
    try {
      log("connecting");
      
      byte[] pass_user = read_file_bytes(args[ARGI_PASS_USER], new byte[SHAZ]);
      Socket socket = new Socket(args[ARGI_HOST_USER], Integer.parseInt(args[ARGI_PORT_USER]));
      FileOutputStream target = new FileOutputStream(args[ARGI_SAVE_FILE]);
      
      IncomingNetworkStream inp = new IncomingNetworkStream(socket.getInputStream(),  TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1);
      OutgoingNetworkStream out = new OutgoingNetworkStream(socket.getOutputStream(), TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1);
      OutgoingNetworkStream dst = new OutgoingNetworkStream(target,                   TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1);
      
      log("connected");
      
      // solve challenge
      {
        log("authenticating");
        if (!Authentication.response(inp, out, pass_user)) throw (new BAD("failed authentication"));
        log("authenticated");
      }
      
      // send stream select vector
      out.wL(Long.parseLong(args[ARGI_WANT_BITS]));
      out.wL(0); // skip no packets
      out.writeback();
      
      log("wrote selector");
      log("enter receive loop");
      
      // big digest
      MessageDigest md = sha256();
      
      while (true) {
        // turn network input buffer
        inp.turn();
        
        // receive packet
        GroupirPacket groupir = GroupirPacket.recv(inp, pass_user);
        
        // obtain macless packet encoding
        byte[] encoding = groupir.send(null);
        
        // make sure the packet has the required metadata
        if (groupir.dI.length < PACKET_METADATA_LENGTH) throw new BAD("illegal packet");
        
        // extract stream identifier
        int stream_id = groupir.dI[PACKET_METADATA_STREAM_ID];
        
        // check for stop condition
        if (stream_id == STREAM_STOP) break;
        
        // write packet
        md.update(encoding);
        dst.wB(encoding);
        dst.writeback();
      }
      
      log("leave receieve loop");
      log("closing down");
      
      target.close();
      
      FileOutputStream mac = (new FileOutputStream(args[ARGI_SAVE_FILE] + ".sha256"));
      mac.write(join_bytes(hexify(md.digest()), new byte[] { '\n' }));
      mac.close();
      
      log("risky part");
      
      socket.close();
      
      log("closed down");
    } catch (Exception e) {
      fatal(e);
      System.exit(1);
    }
  }
}

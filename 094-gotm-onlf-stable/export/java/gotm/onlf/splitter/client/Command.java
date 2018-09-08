/***
 * Command.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.client;

import java.net.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.utilities.Utilities.*;

public class Command
{
  static final int BUFZ = 65536;
  
  public static void main(String[] args)
  {
    final int ARGI_HOST_USER = 0;
    final int ARGI_PORT_STOP = 1;
    final int ARGI_PASS_ROOT = 2;
    
    try {
      log("connecting");
      
      byte[] pass_root = read_file_bytes(args[ARGI_PASS_ROOT], new byte[SHAZ]);
      Socket socket = new Socket(args[ARGI_HOST_USER], Integer.parseInt(args[ARGI_PORT_STOP]));
      
      IncomingNetworkStream inp = new IncomingNetworkStream(socket.getInputStream(),  BUFZ, throw_F0, throw_F1);
      OutgoingNetworkStream out = new OutgoingNetworkStream(socket.getOutputStream(), BUFZ, throw_F0, throw_F1);
      
      log("connected");
      
      // solve challenge
      {
        log("authenticating");
        if (!Authentication.response(inp, out, pass_root)) throw new BAD("failed authentication");
        log("authenticated");
      }
      
      log("closing down");
      
      socket.close();
      
      log("closed down");
    } catch (Exception e) {
      fatal(e);
      System.exit(1);
    }
  }
}

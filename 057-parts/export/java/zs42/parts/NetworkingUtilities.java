/***
 * NetworkingUtilities.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;

public class NetworkingUtilities
{
  static final int CONNECTION_TIMEOUT_MS = 1000;
  
  public static Socket connect(final String host, final int port)
  {
    Socket socket = null;
    
    System.out.println("connecting to host '" + host + "', port " + port);
    
    {
      while (true) {
        System.out.println("trying to connect (timeout " + CONNECTION_TIMEOUT_MS + "ms) ...");
        
        try {
          socket = (new Socket());
          socket.connect((new InetSocketAddress(host, port)), CONNECTION_TIMEOUT_MS);
          break;
        } catch (IOException e) {
          try {
            socket.close();
          } catch (Throwable e2) {
            // ignored; continue to retry
          }
          
          // ignored; retry
        }
      }
    }
    
    return socket;
  }
}

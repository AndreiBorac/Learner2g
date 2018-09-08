/***
 * Interleaver.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.server;

import java.io.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class Interleaver
{
  public static void main(String[] args)
  {
    final DataOutputStream out = (new DataOutputStream(System.out));
    
    (new Thread()
      {
        public void run()
        {
          try {
            while (true) {
              out.flush();
              
              try {
                sleep(250);
              } catch (InterruptedException e) {
                // ignored
              }
            }
          } catch (Throwable e) {
            fatal(e);
          }
        }
      }).start();
    
    for (final String argi : args) {
      (new Thread()
        {
          public void run()
          {
            try {
              {
                DataInputStream dis = (new DataInputStream(new RetryInputStream(new FileInputStream(argi))));
                
                byte[] buffer = (new byte[8 + MAXIMUM_FEEDBACK_PACKET_LENGTH]);
                
                while (true) {
                  int length = dis.readInt();
                  if (length > buffer.length) throw null;
                  dis.readFully(buffer, 0, length);
                  
                  synchronized (out) {
                    out.writeInt(length);
                    out.write(buffer, 0, length);
                  }
                }
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
    }
  }
}

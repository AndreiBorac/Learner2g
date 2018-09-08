/***
 * Replay.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.client;



public class Replay
{
  public static void main(String[] args)
  {
    final int ARGI_HOST = 0;
    final int ARGI_PORT = 1;
    final int ARGI_PASS = 2;
    final int ARGI_FILE = 3;
    final int ARGI_LAST = 4;
    
    if (args.length != ARGI_LAST) {
      System.out.println("usage:\n  java gotm.onlf.splitter.client.Replay (host) (port) (pass) (file)");
      System.exit(1);
      throw null;
    }
    
    // TODO
  }
}

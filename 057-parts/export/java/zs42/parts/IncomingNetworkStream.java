/***
 * IncomingNetworkStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.io.*;

public class IncomingNetworkStream extends DataQueue
{
  public InputStream src;
  
  F1<RuntimeException, Throwable> err;
  
  public IncomingNetworkStream(InputStream src, int cap, F0<RuntimeException> err_logic, F1<RuntimeException, Throwable> err_stream)
  {
    super(cap, err_logic);
    this.src = src;
    this.err = err_stream;
  }
  
  /***
   * reads ahead the smallest possible amount of bytes (perhaps
   * zero) that are required to ensure that at least
   * <code>amt</code> bytes are buffered.
   ***/
  public final void readahead(int amt)
  {
    if (!(0 <= amt)) throw super.err.invoke();
    
    int rem = amt - (lim - pos);
    
    try {
      while (rem > 0) {
        int did = src.read(buf, lim, rem);
        if (did < 0) throw new EOFException(); // will be caught as IOException below
        lim += did;
        rem -= did;
      }
    } catch (IOException e) {
      throw err.invoke(e);
    }
  }
}

/***
 * OutgoingNetworkStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.io.*;

public class OutgoingNetworkStream extends DataQueue
{
  public OutputStream dst;
  
  F1<RuntimeException, Throwable> err;
  
  public OutgoingNetworkStream(OutputStream dst, int cap, F0<RuntimeException> err_logic, F1<RuntimeException, Throwable> err_stream)
  {
    super(cap, err_logic);
    this.dst = dst;
    this.err = err_stream;
  }
  
  /***
   * writes exactly <code>amt</code> bytes.
   ***/
  public final void writeback(int amt)
  {
    if (!((0 <= amt) && (amt <= (lim - pos)))) throw super.err.invoke();
    
    try {
      dst.write(buf, pos, amt);
      dst.flush();
      pos += amt;
    } catch (IOException e) {
      throw err.invoke(e);
    }
  }
  
  /***
   * writes all cached bytes, then effects <code>turn()</code>.
   ***/
  public final void writeback()
  {
    writeback(lim - pos);
    turn();
  }
}

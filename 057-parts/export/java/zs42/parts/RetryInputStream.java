/***
 * RetryInputStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.io.*;

public class RetryInputStream extends FilterInputStream
{
  public static final int DEFAULT_LAPSE_MS = 250;
  
  final int lapse_ms;
  
  public RetryInputStream(InputStream inner, int lapse_ms)
  {
    super(inner);
    this.lapse_ms = lapse_ms;
  }
  
  public RetryInputStream(InputStream inner)
  {
    this(inner, DEFAULT_LAPSE_MS);
  }
  
  private void hang()
  {
    try {
      Thread.sleep(lapse_ms);
    } catch (InterruptedException e) {
      // ignored
    }
  }
  
  public int read() throws IOException
  {
    int ret;
    
    while ((ret = super.read()) == -1) {
      hang();
    }
    
    return ret;
  }
  
  public int read(byte[] arr) throws IOException
  {
    return read(arr, 0, arr.length);
  }
  
  public int read(byte[] arr, int off, int lim) throws IOException
  {
    int ret;
    
    while ((ret = super.read(arr, off, lim)) == -1) {
      hang();
    }
    
    return ret;
  }
}

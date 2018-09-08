/***
 * HttpARQ.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;

public class HttpARQ
{
  final int   retry_ns;
  final int timeout_ms;
  final int   sleep_ms;
  
  long deadzone_ts;
  
  public HttpARQ(int retry_ns, int timeout_ms, int sleep_ms)
  {
    this.   retry_ns =   retry_ns;
    this. timeout_ms = timeout_ms;
    this.   sleep_ms =   sleep_ms;
    
    this.deadzone_ts = (System.nanoTime() - retry_ns); // set first deadzone in the past
  }
  
  public HttpARQ()
  {
    this(1000000000, 5000, 270);
  }
  
  byte[] buf = null;
  int    off;
  int    lim;
  
  int    len;
  
  /***
   * sets the buffer that subsequent <code>fetchURL</code> calls will
   * write message body data into.
   ***/
  public void setBuffer(byte[] buf, int off, int lim)
  {
    this.buf = buf;
    this.off = off;
    this.lim = lim;
  }
  
  /***
   * directs the next <code>fetchURL</code> call to allocate a buffer
   * sized to exactly contain the message body. subsequent calls to
   * <code>fetchURL</code> will not "grow" such an
   * automatically-allocated buffer, so <code>clearBuffer</code>
   * should be called before every invocation of
   * <code>fetchURL</code>.
   ***/
  public void clearBuffer()
  {
    buf = null;
  }
  
  public byte[] getBuffer()
  {
    return buf;
  }
  
  public int getOffset()
  {
    return off;
  }
  
  public int getLength()
  {
    return len;
  }
  
  /***
   * returns <code>false</code> if the message body would not fit in
   * the prescribed buffer; for all other errors, the method continues
   * repeating the query in the hope that the error will go away.
   * 
   * it is safe to ignore the return value of this method if it is
   * immediately preceeded by <code>clearBuffer()</code>.
   ***/
  public boolean fetchURL(String url)
  {
    while (true) {
      long now;
      
      // wait to clear deadzone (to avoid issuing requests too frequently)
      // also set the next deadzone, since we're going to attempt a request below
      {
        while (((now = System.nanoTime()) - deadzone_ts) < 0) {
          try {
            Thread.sleep(sleep_ms);
          } catch (InterruptedException e) {
            throw (new RuntimeException(e));
          }
        }
        
        deadzone_ts = (now + retry_ns);
      }
      
      InputStream inp = null;
      
      try {
        // connect
        {
          HttpURLConnection con = ((HttpURLConnection)((new URL(url)).openConnection()));
          
          con.setRequestMethod("GET");
          con.setDoInput(true);
          
          con.setConnectTimeout(timeout_ms);
          con.setReadTimeout(timeout_ms);
          
          con.connect();
          
          len = con.getContentLength();
          inp = con.getInputStream();
          
          if (buf == null) {
            buf = (new byte[len]);
            off = 0;
            lim = buf.length;
          }
          
          if (len > (lim - off)) {
            return false; // response body would not fit
          }
        }
        
        // read response body
        {
          int pos = off;
          int amt;
          
          // read loop
          {
            int rci;
            
            while ((pos < lim) && ((rci = inp.read(buf, pos, (lim - pos))) > 0)) pos += rci;
            
            // close the input stream
            {
              InputStream hup = inp;
              inp = null;
              hup.close();
            }
          }
          
          amt = (pos - off);
          
          // check for truncated or extended response (the former is bad, the latter is truncated)
          {
            if (amt < len) throw (new IOException("HttpARQ: truncated response; received " + amt + " < " + len + " bytes"));
            if (amt > len) { Log.log("HttpARQ: extended response; received " + amt + " > " + len + " bytes"); amt = len; }
          }
        }
        
        // apparent success if we got this far ...
        return true;
      } catch (Throwable e1) {
        Log.log(e1);
      } finally {
        if (inp != null) {
          try {
            inp.close();
          } catch (Throwable e2) {
            Log.log(e2);
          }
        }
      }
    }
  }
}

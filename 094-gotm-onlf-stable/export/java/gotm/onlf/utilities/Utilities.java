/***
 * Utilities.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.utilities;

import java.io.*;
import java.security.*;

import zs42.parts.F0;
import zs42.parts.F1;

public class Utilities
{
  /***
   * CAST
   ***/
  
  @SuppressWarnings("unchecked")
  public static <DST, SRC> DST cast_unchecked(DST ign, SRC obj)
  {
    return ((DST)(obj));
  }
  
  public static double D(int v)
  {
    return ((double)(v));
  }
  
  public static double D(long v)
  {
    return ((double)(v));
  }
  
  /***
   * EXCEPTION
   ***/
  
  public static class BAD extends RuntimeException
  {
    public BAD() { }
    public BAD(String message) { super(message); }
    public BAD(String message, Throwable cause) { super(message, cause); }
    public BAD(Throwable cause) { super(cause); }
  }
  
  public static final F0<RuntimeException> throw_F0 = (new F0<RuntimeException>() { public RuntimeException invoke() { return new RuntimeException(); } });
  public static final F1<RuntimeException, Throwable> throw_F1 = (new F1<RuntimeException, Throwable>() { public RuntimeException invoke(Throwable e) { return new RuntimeException(e); } });
  public static final F1<Void, Throwable> fatal_F1 = (new F1<Void, Throwable>() { public Void invoke(Throwable e) { fatal(e); while (true); } });
  
  public static synchronized RuntimeException fatal(Throwable e) {
    e.printStackTrace(System.err);
    System.err.flush();
    Runtime.getRuntime().halt(1);
    while (true);
  }
  
  /***
   * LOGGING
   ***/
  
  public static PrintStream logging_target;
  
  static
  {
    logging_target =
      (new PrintStream
       (new OutputStream()
         {
           public void write(int val) { }
           public void write(byte[] arr) { }
           public void write(byte[] arr, int off, int len) { }
         }));
    
    try {
      if (Boolean.parseBoolean(System.getProperty("USE_128_BIT_VECTORS"))) {
        logging_target = System.err;
      }
    } catch (SecurityException e) {
      // ignored; will attach the null target below
    }
  }
  
  public static synchronized void log(String txt)
  {
    logging_target.println(txt);
    logging_target.flush();
  }
  
  public static synchronized void log(Throwable err)
  {
    logging_target.println("ENTER EXCEPTION");
    err.printStackTrace(logging_target);
    logging_target.println("LEAVE EXCEPTION");
    logging_target.flush();
  }
  
  public static synchronized void log(String msg, Throwable err)
  {
    log(msg);
    log(err);
  }
  
  /***
   * RATE-TRACING
   ***/
  
  public static class RateTracer
  {
    private volatile long eventc = 0;
    
    public RateTracer(final String label)
    {
      (new Thread()
        {
          public void run()
          {
            try {
              long prev_ns = System.nanoTime();
              long prev_ec = 0;
              
              while (true) {
                sleep(1000);
                
                long next_ns = System.nanoTime();
                long next_ec = eventc;
                
                log
                  ("RateTracer:" +
                   " for '" + label + "'" +
                   " got " + (next_ec - prev_ec) +
                   " events in " + (next_ns - prev_ns) +
                   " ns, which is " + (D(next_ec - prev_ec) / (D(next_ns - prev_ns) / D(1000000000))) +
                   " hz, or every " + ((D(next_ns - prev_ns) / D(1000000)) / D(next_ec - prev_ec)) + " ms");
                
                prev_ns = next_ns;
                prev_ec = next_ec;
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
    }
    
    public void trigger()
    {
      eventc++;
    }
  }
  
  /***
   * EMPTY
   ***/
  
  public static final byte  [] emptyB = new byte  [0];
  public static final short [] emptyS = new short [0];
  public static final int   [] emptyI = new int   [0];
  public static final long  [] emptyL = new long  [0];
  
  /***
   * DIGEST
   ***/
  
  public static final int SHAZ = 256/8;
  
  static MessageDigest ini256;
  
  static { try { ini256 = MessageDigest.getInstance("SHA-256"); } catch (Exception e) { fatal(e); } }
  
  public static MessageDigest sha256()
  {
    try {
      return ((MessageDigest)(ini256.clone()));
    } catch (CloneNotSupportedException e) {
      throw new BAD(e);
    }
  }
  
  public static byte[] csum_bytes(byte[] arr, int off, int len)
  {
    MessageDigest md = sha256();
    md.update(arr, off, len);
    return md.digest();
  }
  
  public static byte[] csum_bytes(byte[]... vec)
  {
    MessageDigest md = sha256();
    
    for (byte[] arr : vec) {
      md.update(arr);
    }
    
    return md.digest();
  }
  
  public static byte[] csum_mkspecial(int code)
  {
    byte[] csum = new byte[SHAZ];
    for (int i = 0; i < (SHAZ - 1); i++) csum[i] = -1;
    csum[SHAZ - 1] = (byte)(code);
    return csum;
  }
  
  public static int csum_isspecial(byte[] csum)
  {
    for (int i = 0; i < (SHAZ - 1); i++) if (csum[i] != -1) return 0;
    return (csum[SHAZ - 1] & 0xFF);
  }
  
  /***
   * RANDOM
   ***/
  
  public static byte[] secure_random(byte[] seed)
  {
    byte[] more = new byte[SHAZ];
    
    long time = System.nanoTime();
    
    for (int i = 0; i < more.length; i++) {
      more[i] = (byte)(time);
      time >>>= 8;
    }
    
    return csum_bytes(seed, more);
  }
  
  /***
   * TIME
   ***/
  
  static final long adjustment;
  
  static
  {
    long value = -1;
    
    for (int i = 0; i < 100; i++) {
      long ms = System.currentTimeMillis();
      long ns = System.nanoTime();
      
      long expected_ns = ms * 1000000;
      
      value = (expected_ns - ns);
    }
    
    if (value == -1) throw null;
    
    adjustment = value;
  }
  
  public static long microTime()
  {
    return ((System.nanoTime() + adjustment) / 1000);
  }
  
  /***
   * LINEAR
   ***/
  
  public static int assure_equal_int(int A, int B)
  {
    if (A != B) throw new BAD();
    return A;
  }
  
  public static boolean test_equal_bytes(byte[] A, byte[] B)
  {
    for (int i = 0, l = assure_equal_int(A.length, B.length); i < l; i++) {
      if (A[i] != B[i]) {
        return false;
      }
    }
    
    return true;
  }
  
  public static byte[] xor_bytes(byte[] A, byte[] B)
  {
    byte[] C = new byte[assure_equal_int(A.length, B.length)];
    for (int i = 0; i < C.length; i++) C[i] = (byte)(A[i] ^ B[i]);
    return C;
  }
  
  public static byte[] join_bytes(byte[]... vec)
  {
    int len = 0;
    
    for (byte[] arr : vec) {
      len += arr.length;
    }
    
    int    pos = 0;
    byte[] out = new byte[len];
    
    for (byte[] arr : vec) {
      for (byte val : arr) {
        out[pos++] = val;
      }
    }
    
    return out;
  }
  
  /***
   * CONV
   ***/
  
  static final byte[] hexify_nibble = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  
  public static byte[] hexify(byte[] inp)
  {
    byte[] out = new byte[inp.length << 1];
    
    for (int i = 0; i < inp.length; i++) {
      out[(i << 1)    ] = hexify_nibble[(inp[i] >> 4) & 0xF];
      out[(i << 1) + 1] = hexify_nibble[(inp[i]     ) & 0xF];
    }
    
    return out;
  }
  
  /***
   * I/O
   ***/
  
  public static byte[] read_file_bytes(String path, byte[] dest)
  {
    try {
      DataInputStream dis =
        (new DataInputStream
         (new FileInputStream
          (path)));
      dis.readFully(dest);
      dis.close();
      return dest;
    } catch (Exception e) {
      throw new BAD(e);
    }
  }
}

/***
 * DataQueue.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public class DataQueue
{
  public int    pos;
  public int    lim;
  public byte[] buf;
  
  public F0<RuntimeException> err;
  
  public DataQueue(int cap, F0<RuntimeException> err)
  {
    pos = 0;
    lim = 0;
    buf = (new byte[cap]);
    
    this.err = err;
  }
  
  public final boolean highWaterReached()
  {
    return (lim >= (buf.length >> 1));
  }
  
  public final void assure_turned()
  {
    if (!((pos == 0) && (lim == 0))) throw err.invoke();
  }
  
  public final void turn()
  {
    if (pos != lim) throw err.invoke();
    
    pos = 0;
    lim = 0;
  }
  
  public final void reel()
  {
    int pos2 = 0;
    int lim2 = 0;
    
    while (pos < lim) {
      buf[lim2++] = buf[pos++];
    }
    
    pos = pos2;
    lim = lim2;
  }
  
  public final int rB()
  {
    int out = 0;
    out <<= 8; out |= buf[pos++] & 0xFF;
    if (pos > lim) throw err.invoke();
    return out;
  }
  
  public final int rS()
  {
    int out = 0;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    if (pos > lim) throw err.invoke();
    return out;
  }
  
  public final int rI()
  {
    int out = 0;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    if (pos > lim) throw err.invoke();
    return out;
  }
  
  public final long rL()
  {
    long out = 0;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    out <<= 8; out |= buf[pos++] & 0xFF;
    if (pos > lim) throw err.invoke();
    return out;
  }
  
  public final byte[] rB(byte[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      int out = 0;
      out <<= 8; out |= buf[pos++] & 0xFF;
      arr[idx] = (byte)(out);
    }
    
    if (pos > lim) throw err.invoke();
    
    return arr;
  }
  
  public final short[] rS(short[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      int out = 0;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      arr[idx] = (short)(out);
    }
    
    if (pos > lim) throw err.invoke();
    
    return arr;
  }
  
  public final int[] rI(int[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      int out = 0;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      arr[idx] = out;
    }
    
    if (pos > lim) throw err.invoke();
    
    return arr;
  }
  
  public final long[] rL(long[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      long out = 0;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      out <<= 8; out |= buf[pos++] & 0xFF;
      arr[idx] = out;
    }
    
    if (pos > lim) throw err.invoke();
    
    return arr;
  }
  
  public final void wB(int val)
  {
    buf[lim++] = ((byte)(val));
  }
  
  public final void wS(int val)
  {
    buf[lim++] = ((byte)(val >>  8));
    buf[lim++] = ((byte)(val      ));
  }
  
  public final void wI(int val)
  {
    buf[lim++] = ((byte)(val >> 24));
    buf[lim++] = ((byte)(val >> 16));
    buf[lim++] = ((byte)(val >>  8));
    buf[lim++] = ((byte)(val      ));
  }
  
  public final void wL(long val)
  {
    buf[lim++] = ((byte)(val >> 56));
    buf[lim++] = ((byte)(val >> 48));
    buf[lim++] = ((byte)(val >> 40));
    buf[lim++] = ((byte)(val >> 32));
    buf[lim++] = ((byte)(val >> 24));
    buf[lim++] = ((byte)(val >> 16));
    buf[lim++] = ((byte)(val >>  8));
    buf[lim++] = ((byte)(val      ));
  }
  
  public final byte[] wB(byte[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      buf[lim++] = arr[idx];
    }
    
    return arr;
  }
  
  public final short[] wS(short[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      buf[lim++] = ((byte)(arr[idx] >>  8));
      buf[lim++] = ((byte)(arr[idx]      ));
    }
    
    return arr;
  }
  
  public final int[] wI(int[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      buf[lim++] = ((byte)(arr[idx] >> 24));
      buf[lim++] = ((byte)(arr[idx] >> 16));
      buf[lim++] = ((byte)(arr[idx] >>  8));
      buf[lim++] = ((byte)(arr[idx]      ));
    }
    
    return arr;
  }
  
  public final long[] wL(long[] arr, int off, int len)
  {
    for (int idx = off, end = off + len; idx < end; idx++) {
      buf[lim++] = ((byte)(arr[idx] >> 56));
      buf[lim++] = ((byte)(arr[idx] >> 48));
      buf[lim++] = ((byte)(arr[idx] >> 40));
      buf[lim++] = ((byte)(arr[idx] >> 32));
      buf[lim++] = ((byte)(arr[idx] >> 24));
      buf[lim++] = ((byte)(arr[idx] >> 16));
      buf[lim++] = ((byte)(arr[idx] >>  8));
      buf[lim++] = ((byte)(arr[idx]      ));
    }
    
    return arr;
  }
  
  public final byte  [] rB(byte  [] arr) { return rB(arr, 0, arr.length); }
  public final short [] rS(short [] arr) { return rS(arr, 0, arr.length); }
  public final int   [] rI(int   [] arr) { return rI(arr, 0, arr.length); }
  public final long  [] rL(long  [] arr) { return rL(arr, 0, arr.length); }
  
  public final byte  [] wB(byte  [] arr) { return wB(arr, 0, arr.length); }
  public final short [] wS(short [] arr) { return wS(arr, 0, arr.length); }
  public final int   [] wI(int   [] arr) { return wI(arr, 0, arr.length); }
  public final long  [] wL(long  [] arr) { return wL(arr, 0, arr.length); }
}

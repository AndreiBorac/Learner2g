/***
 * DataQueue.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class DataQueue extends RootObject
{
  byte[] arr;
  int    off;
  int    lim;
  
  /***
   * (package-private) constructor.
   ***/
  DataQueue(byte[] arr, int off, int lim)
  {
    this.arr = arr;
    this.off = off;
    this.lim = lim;
  }
  
  public static DataQueue newEmpty(int cap)
  {
    if (cap < 1) throw null;
    return new DataQueue(new byte[cap], 0, 0);
  }
  
  public static DataQueue newLinkOf(byte[] arr)
  {
    return new DataQueue(arr, 0, arr.length);
  }
  
  public static DataQueue newLinkOf(byte[] arr, int off, int lim)
  {
    return new DataQueue(arr, off, lim);
  }
  
  public int off()
  {
    return off;
  }
  
  public int lim()
  {
    return lim;
  }
  
  public byte[] dma()
  {
    return arr;
  }
  
  public void turn()
  {
    if (!(off == lim)) throw null;
    
    off = 0;
    lim = 0;
  }
  
  public void prepareR(int amt)
  {
    if ((lim - off) < amt) {
      throw null;
    }
  }
  
  public void prepareW(int amt)
  {
    if ((arr.length - lim) < amt) {
      int cap = arr.length;
      do { cap <<= 1; } while ((cap - lim) < amt);
      
      byte[] old = arr;
      arr = new byte[cap];
      
      for (int pos = off; pos < lim; pos++) {
        arr[pos] = old[pos];
      }
    }
  }
  
  public int rB()
  {
    prepareR(1 << 0);
    int out = 0;
    out <<= 8; out |= arr[off++] & 0xFF;
    return out;
  }
  
  public int rS()
  {
    prepareR(1 << 1);
    int out = 0;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    return out;
  }
  
  public int rI()
  {
    prepareR(1 << 2);
    int out = 0;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    return out;
  }
  
  public long rJ()
  {
    prepareR(1 << 3);
    long out = 0;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    out <<= 8; out |= arr[off++] & 0xFF;
    return out;
  }
  
  public void wB(int val)
  {
    prepareW(1 << 0);
    arr[lim++] = (byte)(val      );
  }
  
  public void wS(int val)
  {
    prepareW(1 << 1);
    arr[lim++] = (byte)(val >>  8);
    arr[lim++] = (byte)(val      );
  }
  
  public void wI(int val)
  {
    prepareW(1 << 2);
    arr[lim++] = (byte)(val >> 24);
    arr[lim++] = (byte)(val >> 16);
    arr[lim++] = (byte)(val >>  8);
    arr[lim++] = (byte)(val      );
  }
  
  public void wJ(long val)
  {
    prepareW(1 << 3);
    arr[lim++] = (byte)(val >> 56);
    arr[lim++] = (byte)(val >> 48);
    arr[lim++] = (byte)(val >> 40);
    arr[lim++] = (byte)(val >> 32);
    arr[lim++] = (byte)(val >> 24);
    arr[lim++] = (byte)(val >> 16);
    arr[lim++] = (byte)(val >>  8);
    arr[lim++] = (byte)(val      );
  }
  
  public void rBa(byte[] dst_arr, int dst_off, int dst_lim)
  {
    checkbounds(dst_off, dst_lim, dst_arr.length);
    prepareR(dst_lim - dst_off);
    
    for (int dst_pos = dst_off; dst_pos < dst_lim; dst_pos++) {
      dst_arr[dst_pos] = arr[off++];
    }
  }
  
  public void wBa(byte[] src_arr, int src_off, int src_lim)
  {
    checkbounds(src_off, src_lim, src_arr.length);
    prepareW(src_lim - src_off);
    
    for (int src_pos = src_off; src_pos < src_lim; src_pos++) {
      arr[lim++] = src_arr[src_pos];
    }
  }
  
  public rB rrB(int amt)
  {
    if (amt < 0) throw null;
    prepareR(amt);
    return rB.newLinkOf(arr, off, off += amt);
  }
  
  public vB rvB(int amt)
  {
    if (amt < 0) throw null;
    prepareR(amt);
    return vB.newLinkOf(arr, off, off += amt);
  }
  
  public void wrB(rB src)
  {
    wBa(src.arr, src.off, src.lim);
  }
}

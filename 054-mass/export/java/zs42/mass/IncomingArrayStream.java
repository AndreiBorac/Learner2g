/***
 * IncomingArrayStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class IncomingArrayStream extends RootObject
{
  byte[] arr;
  int    off; // start of readable region
  int    pos; // cursor
  int    lim; // end of readable region; totally ignored
  
  /***
   * (public) constructor.
   ***/
  public IncomingArrayStream()
  {
    iniLinkOf(cB.getSharedEmpty());
  }
  
  /***
   * (public) constructor.
   ***/
  public IncomingArrayStream(byte[] arr, int off, int lim)
  {
    iniLinkOf(arr, off, lim);
  }
  
  private void initialize(byte[] arr, int off, int lim)
  {
    checkbounds(off, lim, arr.length);
    
    this.arr = arr;
    this.off = off;
    this.pos = off;
    this.lim = lim;
  }
  
  public IncomingArrayStream iniLinkOf(byte[] arr)
  {
    initialize(arr, 0, arr.length);
    return this;
  }
  
  public IncomingArrayStream iniLinkOf(byte[] arr, int off, int lim)
  {
    initialize(arr, off, lim);
    return this;
  }
  
  public IncomingArrayStream iniLinkOf(rB src)
  {
    src = src.checkLinkable();
    initialize(src.arr, src.off, src.lim);
    return this;
  }
  
  public IncomingArrayStream iniLinkOf(rB src, int src_off, int src_lim)
  {
    src = src.checkLinkable();
    initialize(src.arr, src_off, src_lim);
    return this;
  }
  
  public static IncomingArrayStream newLinkOf(byte[] arr)
  {
    return (new IncomingArrayStream(arr, 0, arr.length));
  }
  
  public static IncomingArrayStream newLinkOf(byte[] arr, int off, int lim)
  {
    return (new IncomingArrayStream(arr, off, lim));
  }
  
  public static IncomingArrayStream newLinkOf(rB src)
  {
    src = src.checkLinkable();
    return (new IncomingArrayStream(src.arr, src.off, src.lim));
  }
  
  public static IncomingArrayStream newLinkOf(rB src, int src_off, int src_lim)
  {
    src = src.checkLinkable();
    return (new IncomingArrayStream(src.arr, src_off, src_lim));
  }
  
  /***
   * returns the number of bytes remaining to be read. it may be
   * negative if the source array has already been read past the
   * declared limit point.
   ***/
  public int rem()
  {
    return lim - pos;
  }
  
  /***
   * returns the number of bytes read so far.
   ***/
  public int len()
  {
    return pos - off;
  }
  
  public void skip(int amt)
  {
    if (amt < 0) throw null;
    pos += amt;
  }
  
  public void aX(int log)
  {
    pos += ((-len()) & ((1 << log) - 1));
  }
  
  public void aB() { aX(0); }
  public void aS() { aX(1); }
  public void aI() { aX(2); }
  public void aJ() { aX(3); }
  
  public byte rBB()
  {
    return ((byte)(rBI()));
  }
  
  public short rSS()
  {
    return ((short)(rSI()));
  }
  
  /***
   * unsigned conversion!
   ***/
  public int rBI()
  {
    int out = 0;
    out <<= 8; out |= arr[pos++] & 0xFF;
    return out;
  }
  
  /***
   * unsigned conversion!
   ***/
  public int rSI()
  {
    int out = 0;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    return out;
  }
  
  public int rII()
  {
    int out = 0;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    return out;
  }
  
  /***
   * unsigned conversion!
   ***/
  public long rIJ()
  {
    long out = 0;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    return out;
  }
  
  public long rJJ()
  {
    long out = 0;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    out <<= 8; out |= arr[pos++] & 0xFF;
    return out;
  }
  
  public byte[] rBa(int amt)
  {
    if (amt < 0) throw null;
    
    byte[] out = new byte[amt];
    
    for (int i = 0; i < amt; i++) {
      out[i] = arr[pos++];
    }
    
    return out;
  }
  
  public rB rBr(int amt)
  {
    if (amt < 0) throw null;
    return rB.newLinkOf(arr, pos, pos += amt);
  }
  
  public vB rBv(int amt)
  {
    if (amt < 0) throw null;
    return vB.newLinkOf(arr, pos, pos += amt);
  }
}

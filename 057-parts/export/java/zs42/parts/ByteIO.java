/***
 * ByteIO.java
 * copyright (c) 2013 by andrei borac
 ***/

package zs42.parts;

public final class ByteIO
{
  final byte[] arr;
  
  int off = 0;
  
  public ByteIO(byte[] arr)
  {
    this.arr = arr;
  }
  
  public byte[] getBacking()
  {
    return arr;
  }
  
  public int getOffset()
  {
    return off;
  }
  
  public void setOffset(int pos)
  {
    off = pos;
  }
  
  public int getRemaining()
  {
    return (arr.length - off);
  }
  
  // READ
  
  public int r_sB()
  {
    return arr[off++];
  }
  
  public int r_uB()
  {
    return (r_sB() & 0xFF);
  }
  
  public int r_uS_be()
  {
    return ((r_uB() << 8) | r_uB());
  }
  
  public int r_uS_el()
  {
    return (r_uB() | (r_uB() << 8));
  }
  
  public int r_sS_be()
  {
    return ((int)((short)(r_uS_be())));
  }
  
  public int r_sS_el()
  {
    return ((int)((short)(r_uS_el())));
  }
  
  public int r_sI_be()
  {
    return ((r_uS_be() << 16) | r_uS_be());
  }
  
  public int r_sI_el()
  {
    return (r_uS_el() | (r_uS_el() << 16));
  }
  
  public long r_uI_be()
  {
    return (((long)(r_sI_be())) & 0xFFFFFFFFL);
  }
  
  public long r_uI_el()
  {
    return (((long)(r_sI_el())) & 0xFFFFFFFFL);
  }
  
  public long r_sJ_be()
  {
    return ((r_uI_be() << 32) | r_uI_be());
  }
  
  public long r_sJ_el()
  {
    return (r_uI_el() | (r_uI_el() << 32));
  }
  
  // WRITE
  
  public void w_B(int val)
  {
    arr[off++] = ((byte)(val));
  }
  
  public void w_S_be(int val)
  {
    w_B(val >> 8); w_B(val);
  }
  
  public void w_S_el(int val)
  {
    w_B(val); w_B(val >> 8);
  }
  
  public void w_I_be(int val)
  {
    w_S_be(val >> 16); w_S_be(val);
  }
  
  public void w_I_el(int val)
  {
    w_S_el(val); w_S_el(val >> 16);
  }
  
  public void w_J_be(long val)
  {
    w_I_be(((int)(val >> 32))); w_I_be(((int)(val)));
  }
  
  public void w_J_el(long val)
  {
    w_I_el(((int)(val))); w_I_el(((int)(val >> 32)));
  }
}

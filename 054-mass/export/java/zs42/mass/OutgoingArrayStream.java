/***
 * OutgoingArrayStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class OutgoingArrayStream extends RootObject
{
  /***
   * (package-private) constructor.
   ***/
  OutgoingArrayStream()
  {
    // nothing to do
  }
  
  public static abstract class Producer extends RootObject
  {
    protected abstract void produce(Consumer out);
  }
  
  public static abstract class Consumer extends RootObject
  {
    /** returns length written so far */ public abstract int len();
    
    /** restore byte  alignment */ public abstract void aB();
    /** restore short alignment */ public abstract void aS();
    /** restore int   alignment */ public abstract void aI();
    /** restore long  alignment */ public abstract void aJ();
    
    /** write single byte  */ public abstract void wB(byte  val);
    /** write single short */ public abstract void wS(short val);
    /** write single int   */ public abstract void wI(int   val);
    /** write single long  */ public abstract void wJ(long  val);
    
    /** write byte  array */ public abstract void wB(rB src);
    /** write short array */ public abstract void wS(rS src);
    /** write int   array */ public abstract void wI(rI src);
    /** write long  array */ public abstract void wJ(rJ src);
  }
  
  private static class CalcConsumer extends Consumer
  {
    int len = 0;
    
    public int len()
    {
      return len;
    }
    
    public void aX(int log)
    {
      len += ((-len) & ((1 << log) - 1));
    }
    
    public void aB() { aX(0); }
    public void aS() { aX(1); }
    public void aI() { aX(2); }
    public void aJ() { aX(3); }
    
    public void wB(byte  val) { len += (1 << 0); }
    public void wS(short val) { len += (1 << 1); }
    public void wI(int   val) { len += (1 << 2); }
    public void wJ(long  val) { len += (1 << 3); }
    
    public void wB(rB src) { len += (src.len() << 0); }
    public void wS(rS src) { len += (src.len() << 1); }
    public void wI(rI src) { len += (src.len() << 2); }
    public void wJ(rJ src) { len += (src.len() << 3); }
  }
  
  private static class StowConsumer extends Consumer
  {
    byte[] arr;
    int    off;
    int    lim;
    
    StowConsumer(byte[] arr, int off)
    {
      this.arr = arr;
      this.off = off;
    }
    
    public int len()
    {
      return lim - off;
    }
    
    public void aX(int siz)
    {
      lim += ((-len()) & (siz - 1));
    }
    
    public void aB() {        }
    public void aS() { aX(2); }
    public void aI() { aX(4); }
    public void aJ() { aX(8); }
    
    public void wB(byte val)
    {
      arr[lim++] = (byte)(val      );
    }
  
    public void wS(short val)
    {
      arr[lim++] = (byte)(val >>  8);
      arr[lim++] = (byte)(val      );
    }
  
    public void wI(int val)
    {
      arr[lim++] = (byte)(val >> 24);
      arr[lim++] = (byte)(val >> 16);
      arr[lim++] = (byte)(val >>  8);
      arr[lim++] = (byte)(val      );
    }
  
    public void wJ(long val)
    {
      arr[lim++] = (byte)(val >> 56);
      arr[lim++] = (byte)(val >> 48);
      arr[lim++] = (byte)(val >> 40);
      arr[lim++] = (byte)(val >> 32);
      arr[lim++] = (byte)(val >> 24);
      arr[lim++] = (byte)(val >> 16);
      arr[lim++] = (byte)(val >>  8);
      arr[lim++] = (byte)(val      );
    }
    
    public void wB(rB src) { for (int pos = src.off(); pos < src.lim(); pos++) { wB(src.get(pos)); } }
    public void wS(rS src) { for (int pos = src.off(); pos < src.lim(); pos++) { wS(src.get(pos)); } }
    public void wI(rI src) { for (int pos = src.off(); pos < src.lim(); pos++) { wI(src.get(pos)); } }
    public void wJ(rJ src) { for (int pos = src.off(); pos < src.lim(); pos++) { wJ(src.get(pos)); } }
  }
  
  /***
   * for backward-compatibility; to be phased out eventually.
   ***/
  public static byte[] produce(Producer client)
  {
    return produce0(client);
  }
  
  public static byte[] produce0(Producer client)
  {
    CalcConsumer calc = new CalcConsumer();
    client.produce(calc);
    StowConsumer stow = new StowConsumer((new byte[calc.len]), 0);
    client.produce(stow);
    if (stow.lim != calc.len) throw null;
    byte[] retv = stow.arr;
    stow.arr = null;
    return retv;
  }
  
  public static cB produceC(Producer client)
  {
    // WARNING: like most LinkOfUnchecked, this is not MT-safe
    byte[] bak = produce0(client);
    return (new cB()).iniLinkOfUnchecked(bak, 0, bak.length);
  }
}

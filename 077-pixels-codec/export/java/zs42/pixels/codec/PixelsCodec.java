/***
 * PixelsCodec.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.pixels.codec;

import java.io.*;
import java.util.*;

import zs42.mass.*;
import zs42.buff.*;
import zs42.nats.codec.*;

import static zs42.mass.Static.*;

public class PixelsCodec
{
  public static class Dimensions
  {
    public final int H, W, HxW;
    
    public Dimensions(int H, int W)
    {
      this.H = H;
      this.W = W;
      this.HxW = H * W;
    }
  }
  
  public static enum PixelOrder
  {
    ARGB_8888
    {
      public int bytesPerPixel()
      {
        return 4;
      }
      
      public int scanPixel(byte[] arr, int off)
      {
        off++; // skip alpha channel
        
        return
          ((arr[off++] & 0xFF) << 0x10) |
          ((arr[off++] & 0xFF) << 0x08) |
          ((arr[off++] & 0xFF) << 0x00) ;
      }
      
      public void postPixel(byte[] arr, int off, int rgb)
      {
        arr[off++] = ((byte)(0xFF       )); // fully opaque (?) alpha channel
        arr[off++] = ((byte)(rgb >> 0x10));
        arr[off++] = ((byte)(rgb >> 0x08));
        arr[off++] = ((byte)(rgb >> 0x00));
      }
    },
    
    BGRA_8888
    {
      public int bytesPerPixel()
      {
        return 4;
      }
      
      public int scanPixel(byte[] arr, int off)
      {
        // off++; // no need to skip alpha channel; it's last
        
        return
          ((arr[off++] & 0xFF) << 0x00) |
          ((arr[off++] & 0xFF) << 0x08) |
          ((arr[off++] & 0xFF) << 0x10) ;
      }
      
      public void postPixel(byte[] arr, int off, int rgb)
      {
        arr[off++] = ((byte)(rgb >> 0x00));
        arr[off++] = ((byte)(rgb >> 0x08));
        arr[off++] = ((byte)(rgb >> 0x10));
        arr[off++] = ((byte)(0xFF       )); // fully opaque (?) alpha channel
      }
    },
    
    RGB_888
    {
      public int bytesPerPixel()
      {
        return 3;
      }
      
      public int scanPixel(byte[] arr, int off)
      {
        return
          ((arr[off++] & 0xFF) << 0x10) |
          ((arr[off++] & 0xFF) << 0x08) |
          ((arr[off++] & 0xFF) << 0x00) ;
      }
      
      public void postPixel(byte[] arr, int off, int rgb)
      {
        arr[off++] = ((byte)(rgb >> 0x10));
        arr[off++] = ((byte)(rgb >> 0x08));
        arr[off++] = ((byte)(rgb >> 0x00));
      }
    };
    
    public abstract int  bytesPerPixel();
    public abstract int  scanPixel(byte[] arr, int off);
    public abstract void postPixel(byte[] arr, int off, int rgb);
  }
  
  public static class Framebuffer
  {
    protected final int[] xRGB;
    
    public Framebuffer(Dimensions dims)
    {
      xRGB = (new int[dims.HxW]);
    }
    
    public int[] getRaster()
    {
      return xRGB;
    }
    
    public boolean equalsPeer(Framebuffer peer)
    {
      if (xRGB.length != peer.xRGB.length) throw null;
      return Arrays.equals(xRGB, peer.xRGB);
    }
    
    public void copyFromPeer(Framebuffer peer)
    {
      System.arraycopy(peer.xRGB, 0, this.xRGB, 0, eqI(peer.xRGB.length, this.xRGB.length));
    }
    
    public void copyFromPeer(Framebuffer peer, int len)
    {
      if (!(len <= eqI(peer.xRGB.length, this.xRGB.length))) throw null;
      System.arraycopy(peer.xRGB, 0, this.xRGB, 0, len);
    }
    
    /***
     * copies only changed pixels from the peer buffer, according to
     * the change list.
     ***/
    public void copyFromPeer(Framebuffer peer, Buffer.xI list)
    {
      Buffer.oI list_o = list.iterate();

      int num = list_o.remaining();
      
      while (num > 0) {
        int off = list_o.rI(); num--;
        int lim = list_o.rI(); num--;
        
        System.arraycopy(peer.xRGB, off, this.xRGB, off, (lim - off));
      }
      
      list_o.release();
    }
    
    public void recvStream(byte[] inp, PixelOrder ord) throws IOException
    {
      // scan pixels
      {
        int off = 0;
        
        for (int i = 0; i < xRGB.length; i++) {
          xRGB[i] = ord.scanPixel(inp, off);
          off += ord.bytesPerPixel();
        }
      }
    }
    
    public void recvStream(InputStream inp, PixelOrder ord, wiL<vB> aux) throws IOException
    {
      vB  buf = aux.get();
      int amt = ord.bytesPerPixel() * xRGB.length;
      
      if ((buf == null) || (buf.len() != amt)) {
        aux.put((buf = vB.newArray(amt)));
      }
      
      // read fully
      {
        int buf_off = buf.off();
        
        while (buf_off < buf.lim()) {
          int got = inp.read(buf.arr(), buf_off, (buf.lim() - buf_off));
          if (got <= 0) throw null;
          buf_off += got;
        }
      }
      
      recvStream(buf.arr(), ord);
    }
    
    public void recvStream(RandomAccessFile inp, PixelOrder ord, wiL<vB> aux) throws IOException
    {
      vB  buf = aux.get();
      int amt = ord.bytesPerPixel() * xRGB.length;
      
      if ((buf == null) || (buf.len() != amt)) {
        aux.put((buf = vB.newArray(amt)));
      }
      
      // read fully
      {
        int buf_off = buf.off();
        
        while (buf_off < buf.lim()) {
          int got = inp.read(buf.arr(), buf_off, (buf.lim() - buf_off));
          if (got <= 0) throw null;
          buf_off += got;
        }
      }
      
      recvStream(buf.arr(), ord);
    }
    
    public void sendStream(DataOutputStream dos, PixelOrder ord, wiL<vB> aux) throws IOException
    {
      vB  buf = aux.get();
      int amt = ord.bytesPerPixel() * xRGB.length;
      
      if ((buf == null) || (buf.len() != amt)) {
        aux.put((buf = vB.newArray(amt)));
      }
      
      {
        int buf_off = 0;
        
        for (int i = 0; i < xRGB.length; i++) {
          ord.postPixel(buf.arr(), buf_off, xRGB[i]);
          buf_off += ord.bytesPerPixel();
        }
      }
      
      dos.write(buf.arr());
    }
  }
  
  public static class _001
  {
    static class RleBitCodec
    {
      // beware of tricky carry logic (necessary b/c it must read one
      // symbol past the current run to determine that the current run
      // has ended)
      private static int runlength(Buffer.oZ inp, int rem, boolean exp, int[] pre, Buffer.nI rle)
      {
        int old_rem = rem;
        
        boolean last = exp;
        
        while ((rem > 0) && (rem-- > 0) && ((last = inp.rZ()) == exp));
        
        int amt = pre[0] + (old_rem - rem);
        pre[0] = 0;
        
        if ((rem > 0) || (last != exp)) {
          amt -= 1;
          pre[0] += 1;
        }
        
        rle.aI(amt);
        
        return rem;
      }
      
      /***
       * (raw) -&gt; (rl0, rl1)
       ***/
      static void encode(Buffer.xZ raw, Buffer.xI rl0, Buffer.xI rl1)
      {
        if (rl0.length() != 0) throw null;
        if (rl1.length() != 0) throw null;
        
        Buffer.oZ raw_o = raw.iterate();
        
        Buffer.nI rl0_n = rl0.append();
        Buffer.nI rl1_n = rl1.append();
        
        int rem = raw_o.remaining();
        
        int[] pre = (new int[] { 0 });
        
        if (rem > 0) {
          rem = runlength(raw_o, rem, false, pre, rl0_n);
          rem = runlength(raw_o, rem, true,  pre, rl1_n);
          
          while ((rem > 0) || (pre[0] > 0)) {
            rem = runlength(raw_o, rem, false, pre, rl0_n);
            rem = runlength(raw_o, rem, true , pre, rl1_n);
          }
        }
        
        rl1_n.release();
        rl0_n.release();
        
        raw_o.release();
      }
      
      /***
       * (raw) &gt;- (rl0, rl1)
       ***/
      static void decode(Buffer.xZ raw, Buffer.xI rl0, Buffer.xI rl1)
      {
        Buffer.oI rl0_o = rl0.iterate();
        Buffer.oI rl1_o = rl1.iterate();
        
        Buffer.nZ raw_n = raw.append();

        int num = rl0_o.remaining();
        if (rl1_o.remaining() != num) throw null;
        
        while (num-- > 0)
          {
            {
              int nr0 = rl0_o.rI();
              while (nr0-- > 0) raw_n.aZ(false);
            }
            
            {
              int nr1 = rl1_o.rI();
              while (nr1-- > 0) raw_n.aZ(true);
            }
          }
        
        rl1_o.release();
        rl0_o.release();
        
        raw_n.release();
      }
    }
    
    static class PlainNatCodec
    {
      /** 00aaabbb paired 3-bit values */
      static final int Bx00 = 0;
      /** 01xxxxxx single 6-bit value */
      static final int Bx01 = 1;
      /** 10xx...x single 14-bit value */
      static final int Bx10 = 2;
      /** 11xx...x single 30-bit value */
      static final int Bx11 = 3;
      
      static int ctr00 = 0;
      static int ctr01 = 0;
      static int ctr10 = 0;
      static int ctr11 = 0;
      
      /*
      static final int[] distr = (new int[256]);
      
      static String statistics()
      {
        StringBuilder out = (new StringBuilder());
        out.append("modes " + ctr00 + ":" + ctr01 + ":" + ctr10 + ":" + ctr11 + "\n");
        out.append("distr ");
        
        double sum = 0;
        
        for (int i = 0; i < distr.length; i++) {
          sum += distr[i];
        }
        
        for (int i = 0; i < distr.length; i++) {
          double fra = distr[i] / sum;
          
          if (fra < 0.001) {
            out.append("0 ");
          } else {
            out.append(String.format("%(,.3f ", fra));
          }
        }
        
        return out.toString();
      }
      */
      
      /***
       * (raw) -&gt; (crm)
       ***/
      static void encode(Buffer.xI raw, Buffer.xB crm)
      {
        // gather statistics
        /*
        {
          Buffer.oI raw_o = raw.iterate();
          
          int num = raw_o.remaining();
          
          while (num-- > 0) {
            int val = raw_o.rI();
            
            if ((0 <= val) && (val <= 255)) {
              distr[val]++;
            }
          }
          
          raw_o.release();
        }
        */
        
        // actually encode
        {
          Buffer.oI raw_o = raw.iterate();
          
          Buffer.nB crm_n = crm.append();
          
          int off = 0; // off is always the element to be read next
          int lim = raw_o.remaining();
          
          int A, B;
          
          if (off < lim) {
            A = raw_o.rI(); off++;
            
            while (true) {

              if (off < lim) {
                B = raw_o.rI();
                off++;
              } else {
                B = (1 << 30);
              }
              
              if ((A < 0) || (B < 0)) throw null;
              
              if ((A < (1 << 3)) && (B < (1 << 3))) {
                crm_n.aB(((byte)((A << 3) | B)));
                ctr00++;
                
                if (off < lim) {
                  B = raw_o.rI();
                  off++;
                } else {
                  break;
                }
              } else if (A < (1 <<  6)) {
                crm_n.aB(((byte)((Bx01 << 6) | A)));
                ctr01++;
              } else if (A < (1 << 14)) {
                crm_n.aB(((byte)((Bx10 << 6) | (A >> 8))));
                crm_n.aB(((byte)(A)));
                ctr10++;
              } else if (A < (1 << 30)) {
                crm_n.aB(((byte)((Bx11 << 6) | (A >> 24))));
                crm_n.aB(((byte)(A >> 16)));
                crm_n.aB(((byte)(A >>  8)));
                crm_n.aB(((byte)(A >>  0)));
                ctr11++;
              } else {
                throw null;
              }
              
              A = B;
              
              if (A == (1 << 30)) break;
            }
          }
          
          crm_n.release();
          raw_o.release();
        }
      }
      
      /***
       * (raw) &lt;- (crm)
       ***/
      static void decode(Buffer.xI raw, Buffer.xB crm)
      {
        Buffer.oB crm_o = crm.iterate();
        Buffer.nI raw_n = raw.append();
        
        int off = 0;
        int lim = crm_o.remaining();
        
        while (off < lim) {
          int cdw = (crm_o.rB() & 0xFF);
          off++;
          
          switch (((cdw >> 6) & Bx11)) {
          case Bx00:
            {
              raw_n.aI(((cdw >> 3) & 0x7));
              raw_n.aI((cdw & 0x7));
              break;
            }
            
          case Bx01:
            {
              raw_n.aI((cdw & 0x3F));
              break;
            }
            
          case Bx10:
            {
              int val = (cdw & 0x3F);
              if (off >= lim) throw null;
              val = ((val << 8) | (crm_o.rB() & 0xFF));
              off++;
              raw_n.aI(val);
              break;
            }
            
          case Bx11:
            {
              int val = (cdw & 0x3F);
              if (off >= lim) throw null;
              val = ((val << 8) | (crm_o.rB() & 0xFF));
              off++;
              if (off >= lim) throw null;
              val = ((val << 8) | (crm_o.rB() & 0xFF));
              off++;
              if (off >= lim) throw null;
              val = ((val << 8) | (crm_o.rB() & 0xFF));
              off++;
              raw_n.aI(val);
              break;
            }
            
          default:
            throw null;
          }
        }
        
        raw_n.release();
        crm_o.release();
      }
    }
    
    /***
     * WARNING: NOT DEBUGGED !
     ***/
    static class TrendyDifferentialNatCodec
    {
      /***
       * (raw) -&gt; (nng, dif)
       ***/
      static void encode(Buffer.xI raw, Buffer.xZ nng, Buffer.xI dif)
      {
        Buffer.oI raw_o = raw.iterate();
        
        Buffer.nZ nng_n = nng.append();
        Buffer.nI dif_n = dif.append();
        
        int raw_len = raw_o.remaining();
        
        int mem = 0;
        
        while (raw_len-- > 0) {
          int val = raw_o.rI();
          
          if (val < 0) {
            throw null;
          }
          
          if (val >= mem) {
            nng_n.aZ(true);
            dif_n.aI(val - mem - 0);
          } else {
            nng_n.aZ(false);
            dif_n.aI(mem - val - 1);
          }
          
          mem = val;
        }
        
        nng_n.release();
        dif_n.release();
        
        raw_o.release();
      }
      
      /***
       * (raw) &lt;- (nng, dif)
       ***/
      static void decode(Buffer.xI raw, Buffer.xZ nng, Buffer.xI dif)
      {
        Buffer.oZ nng_o = nng.iterate();
        Buffer.oI dif_o = dif.iterate();
        
        Buffer.nI raw_n = raw.append();
        
        int num = nng_o.remaining();
        
        int mem = 0;
        
        while (num-- > 0) {
          if (nng_o.rZ()) {
            raw_n.aI((mem += (dif_o.rI() + 0)));
          } else {
            raw_n.aI((mem -= (dif_o.rI() + 1)));
          }
        }
        
        raw_n.release();
        
        dif_o.release();
        nng_o.release();
      }
    }
    
    static class NoisyDifferentialNatCodec
    {
      /***
       * (raw) -&gt; (dif)
       ***/
      static void encode(Buffer.xI raw, Buffer.xI dif)
      {
        Buffer.oI raw_o = raw.iterate();
        Buffer.nI dif_n = dif.append();
        
        int raw_len = raw_o.remaining();
        int mem = 0;
        
        while (raw_len-- > 0) {
          int val = raw_o.rI();
          
          /*
          if (val < 0) {
            throw null;
          }
          */
          
          if (val >= mem) {
            dif_n.aI(((val - mem - 0) << 1) + 0);
          } else {
            dif_n.aI(((mem - val - 1) << 1) + 1);
          }
          
          mem = val;
        }
        
        dif_n.release();
        raw_o.release();
      }
      
      /***
       * (raw) &lt;- (dif)
       ***/
      static void decode(Buffer.xI raw, Buffer.xI dif)
      {
        Buffer.oI dif_o = dif.iterate();
        Buffer.nI raw_n = raw.append();
        
        int dif_len = dif_o.remaining();
        int mem = 0;
        
        while (dif_len-- > 0) {
          int cdw = dif_o.rI();
          
          if ((cdw & 1) == 0) {
            raw_n.aI((mem += (cdw >> 1)));
          } else {
            raw_n.aI((mem -= ((cdw >> 1) + 1)));
          }
        }
        
        raw_n.release();
        dif_o.release();
      }
    }
    
    static class PaletteNatCodec
    {
      static int pullup(vI plt, int idx)
      {
        int val = plt.get(idx);
        
        // shift
        {
          int sto = plt.get(0);
          
          for (int i = 1; i <= idx; i++) {
            sto = plt.rot(idx, sto);
          }
        }
        
        plt.put(0, val);
        
        return val;
      }
      
      /***
       * (raw) -&gt; (crm) | (plt) [*]
       ***/
      static void encode(Buffer.xI raw, Buffer.xI crm, vI plt)
      {
        int plt_len = plt.len();
        if (plt.off() != 0) throw null;
        
        Buffer.oI raw_o = raw.iterate();
        Buffer.nI crm_n = crm.append();
        
        int raw_len = raw_o.remaining();
        
        outer:
        while (raw_len-- > 0) {
          int val = raw_o.rI();
          
          for (int i = 0; i < plt_len; i++) {
            if (plt.get(i) == val) {
              pullup(plt, i);
              crm_n.aI(i);
              continue outer;
            }
          }
          
          plt.put(plt_len - 1, val);
          pullup(plt, plt_len - 1);
          crm_n.aI(val + plt_len);
        }
        
        crm_n.release();
        raw_o.release();
      }
      
      /***
       * (raw) &lt;- (crm) | (plt) [*]
       ***/
      static void decode(Buffer.xI raw, Buffer.xI crm, vI plt)
      {
        int plt_len = plt.len();
        if (plt.off() != 0) throw null;
        
        Buffer.oI crm_o = crm.iterate();
        Buffer.nI raw_n = raw.append();
        
        int crm_len = crm_o.remaining();
        
        while (crm_len-- > 0) {
          int cdw = crm_o.rI();
          
          if (cdw < plt_len) {
            raw_n.aI(pullup(plt, cdw));
          } else {
            plt.put(plt_len - 1, cdw - plt_len);
            raw_n.aI(pullup(plt, plt_len - 1));
          }
        }
        
        raw_n.release();
        crm_o.release();
      }
    }
    
    static class NatDifRleCodec
    {
      /***
       * (raw) -&gt; (out)
       ***/
      static void encode(final BufferCentral central, Buffer.xZ raw, Buffer.xB crm)
      {
        Buffer.xI rl0 = central.acquireI();
        Buffer.xI rl1 = central.acquireI();
        
        RleBitCodec.encode(raw, rl0, rl1);
        
        Buffer.xI rl0_dif = central.acquireI();
        Buffer.xI rl1_dif = central.acquireI();
        
        NoisyDifferentialNatCodec.encode(rl0, rl0_dif);
        NoisyDifferentialNatCodec.encode(rl1, rl1_dif);
        
        rl0.release();
        rl1.release();
        
        PlainNatCodec.encode(rl0_dif, crm);
        PlainNatCodec.encode(rl1_dif, crm);
        
        rl0_dif.release();
        rl1_dif.release();
      }
      
      /***
       * (raw) &lt;- (out)
       ***/
      static void decode(final BufferCentral central, Buffer.xZ raw, Buffer.xB crm)
      {
        Buffer.xI rl0_dif = central.acquireI();
        Buffer.xI rl1_dif = central.acquireI();
        
        {
          Buffer.xI all = central.acquireI();
          
          PlainNatCodec.decode(all, crm);
          
          int all_len = all.length();
          int all_half_len = (all_len >> 1);
          if ((all_half_len << 1) != all_len) throw null;
          
          {
            Buffer.oI all_o = all.iterate();
            
            {
              Buffer.nI rl0_dif_n = rl0_dif.prepend();
              Buffer.sI.alias(rl0_dif_n, all_o, all_half_len);
              rl0_dif_n.release();
            }
            
            {
              Buffer.nI rl1_dif_n = rl1_dif.prepend();
              Buffer.sI.alias(rl1_dif_n, all_o, all_half_len);
              rl1_dif_n.release();
            }
            
            all_o.release();
          }
          
          all.release();
        }
        
        {
          Buffer.xI rl0 = central.acquireI();
          Buffer.xI rl1 = central.acquireI();
          
          NoisyDifferentialNatCodec.decode(rl0, rl0_dif);
          NoisyDifferentialNatCodec.decode(rl1, rl1_dif);
          
          RleBitCodec.decode(raw, rl0, rl1);
          
          rl0.release();
          rl1.release();
        }
        
        rl0_dif.release();
        rl1_dif.release();
      }
    }
    
    static class RasterCodec
    {
      static int min(int A, int B) { return ((A <= B) ? A : B); }
      
      /***
       * (next) -&gt; (same, left, upup, xrgb) | (prev, dims)
       ***/
      static void encode(final BufferCentral central, Framebuffer next, Buffer.xZ same, Buffer.xZ left, Buffer.xZ upup, Buffer.xI xrgb, Framebuffer prev, Dimensions dims, int trip)
      {
        final boolean Y = true;
        final boolean N = false;
        
        final int H = dims.H;
        final int W = dims.W;
        
        Buffer.nZ same_n = same.append();
        Buffer.nZ left_n = left.append();
        Buffer.nZ upup_n = upup.append();
        Buffer.nI xrgb_n = xrgb.append();
        
        for (int i = 0, l = min(1, trip); i < l; i++) {
          /* same */ if (next.xRGB[i] == prev.xRGB[i    ]) { same_n.aZ(Y); continue; } same_n.aZ(N);
          xrgb_n.aI(next.xRGB[i]);
        }
        
        // OLD CODE
        // for (int i = 1, l = min(W, trip); i < l; i++) {
        //   /* same */ if (next.xRGB[i] == prev.xRGB[i    ]) { same_n.aZ(Y); continue; } same_n.aZ(N);
        //   /* left */ if (next.xRGB[i] == next.xRGB[i - 1]) { left_n.aZ(Y); continue; } left_n.aZ(N);
        //   xrgb_n.aI(next.xRGB[i]);
        // }
        
        // NEW CODE
        {
          int i = 1;
          int l = min(W, trip);
          
          while (i < l) {
            boolean reset = false;
            
            /* same */ if (!reset) { while ((i < l) && (next.xRGB[i] == prev.xRGB[i    ])) { same_n.aZ(Y);                                        i++; reset = true; } }
            /* left */ if (!reset) { while ((i < l) && (next.xRGB[i] == next.xRGB[i - 1])) { same_n.aZ(N); left_n.aZ(Y);                          i++; reset = true; } }
            /* xRGB */ if (!reset) { if     (i < l)                                        { same_n.aZ(N); left_n.aZ(N); xrgb_n.aI(next.xRGB[i]); i++; reset = true; } }
          }
        }
        
        // OLD CODE
        // for (int i = W, l = min(dims.HxW, trip); i < l; i++) {
        //   /* same */ if (next.xRGB[i] == prev.xRGB[i    ]) { same_n.aZ(true); continue; } same_n.aZ(false);
        //   /* left */ if (next.xRGB[i] == next.xRGB[i - 1]) { left_n.aZ(true); continue; } left_n.aZ(false);
        //   /* upup */ if (next.xRGB[i] == next.xRGB[i - W]) { upup_n.aZ(true); continue; } upup_n.aZ(false);
        //   xrgb_n.aI(next.xRGB[i]);
        // }
        
        // NEW CODE
        {
          int i = W;
          int l = min(dims.HxW, trip);
          
          while (i < l) {
            boolean reset = false;
            
            /* same */ if (!reset) { while ((i < l) && (next.xRGB[i] == prev.xRGB[i    ])) { same_n.aZ(Y);                                                      i++; reset = true; } }
            /* left */ if (!reset) { while ((i < l) && (next.xRGB[i] == next.xRGB[i - 1])) { same_n.aZ(N); left_n.aZ(Y);                                        i++; reset = true; } }
            /* upup */ if (!reset) { while ((i < l) && (next.xRGB[i] == next.xRGB[i - W])) { same_n.aZ(N); left_n.aZ(N); upup_n.aZ(Y);                          i++; reset = true; } }
            /* xRGB */ if (!reset) { if     (i < l)                                        { same_n.aZ(N); left_n.aZ(N); upup_n.aZ(N); xrgb_n.aI(next.xRGB[i]); i++; reset = true; } }
          }
        }
        
        for (int i = trip, l = dims.HxW; i < l; i++) {
          same_n.aZ(Y);
        }
        
        same_n.release();
        left_n.release();
        upup_n.release();
        xrgb_n.release();
      }
      
      /***
       * (next) &lt;- (same, left, upup, xrgb) | (prev, dims)
       ***/
      static void decode(final BufferCentral central, Framebuffer next, Buffer.xZ same, Buffer.xZ left, Buffer.xZ upup, Buffer.xI xrgb, Framebuffer prev, Dimensions dims, Buffer.xI list)
      {
        final int H = dims.H;
        final int W = dims.W;
        
        Buffer.oZ same_o = same.iterate();
        Buffer.oZ left_o = left.iterate();
        Buffer.oZ upup_o = upup.iterate();
        Buffer.oI xrgb_o = xrgb.iterate();
        
        for (int i = 0; i < 1; i++) {
          /* same */ if (same_o.rZ()) { next.xRGB[i] = prev.xRGB[i]; continue; }
          next.xRGB[i] = xrgb_o.rI();
        }
        
        for (int i = 1; i < W; i++) {
          /* same */ if (same_o.rZ()) { next.xRGB[i] = prev.xRGB[i    ]; continue; }
          /* left */ if (left_o.rZ()) { next.xRGB[i] = next.xRGB[i - 1]; continue; }
          next.xRGB[i] = xrgb_o.rI();
        }
        
        for (int i = W; i < dims.HxW; i++) {
          /* same */ if (same_o.rZ()) { next.xRGB[i] = prev.xRGB[i    ]; continue; }
          /* left */ if (left_o.rZ()) { next.xRGB[i] = next.xRGB[i - 1]; continue; }
          /* upup */ if (upup_o.rZ()) { next.xRGB[i] = next.xRGB[i - W]; continue; }
          next.xRGB[i] = xrgb_o.rI();
        }
        
        same_o.release();
        left_o.release();
        upup_o.release();
        xrgb_o.release();

        // determine runs of changed pixels
        {
          if (list != null) {
            {
              Buffer.nI list_n = list.append();
              {
                int off = 0, lim;
                
                while (off < dims.HxW) {
                  // determine start of run
                  if (next.xRGB[off] == prev.xRGB[off]) {
                    off++;
                    continue;
                  }
                  
                  // determine end of run
                  for (lim = off + 1; lim < dims.HxW && next.xRGB[lim] != prev.xRGB[lim]; lim++) ;
                  
                  list_n.aI(off);
                  list_n.aI(lim);
                  off = lim;
                }
              }
              list_n.release();
            }
          }
        }
      }
    }
    
    static class ScreenCodec
    {
      /***
       * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
       * number of pixels to process (for splitting large updates).
       ***/
      static void encode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, int trip)
      {
        Buffer.xZ same_raw = central.acquireZ();
        Buffer.xZ left_raw = central.acquireZ();
        Buffer.xZ upup_raw = central.acquireZ();
        Buffer.xI xrgb_raw = central.acquireI();
        
        RasterCodec.encode(central, next, same_raw, left_raw, upup_raw, xrgb_raw, prev, dims, trip);
        
        Buffer.xB same_crm = central.acquireB();
        Buffer.xB left_crm = central.acquireB();
        Buffer.xB upup_crm = central.acquireB();
        Buffer.xI xrgb_plt = central.acquireI();
        
        NatDifRleCodec.encode(central, same_raw, same_crm);
        NatDifRleCodec.encode(central, left_raw, left_crm);
        NatDifRleCodec.encode(central, upup_raw, upup_crm);
        
        PaletteNatCodec.encode(xrgb_raw, xrgb_plt, vI.newLinkOf((new int[8])));
        
        same_raw.release();
        left_raw.release();
        upup_raw.release();
        xrgb_raw.release();
        
        // write header
        {
          Buffer.xI size_raw = central.acquireI(); // temporary
          
          // write sizes to temporary buffer
          {
            Buffer.nI size_raw_n = size_raw.prepend();
            size_raw_n.aI(same_crm.length());
            size_raw_n.aI(left_crm.length());
            size_raw_n.aI(upup_crm.length());
            size_raw_n.release();
          }
          
          // write header code
          {
            Buffer.nB crm_n = crm.append(); // header length to be filled in below
            int head = crm.length();
            PlainNatCodec.encode(size_raw, crm);
            crm_n.aB((byte)(crm.length() - head)); // i.e., here
            crm_n.release();
          }
          
          size_raw.release();
        }
        
        // write same, left, upup payloads
        {
          Buffer.nB crm_n = crm.append();
          
          Buffer.oB same_crm_o = same_crm.iterate();
          Buffer.oB left_crm_o = left_crm.iterate();
          Buffer.oB upup_crm_o = upup_crm.iterate();
          
          Buffer.sB.copy(crm_n, same_crm_o, same_crm_o.remaining());
          Buffer.sB.copy(crm_n, left_crm_o, left_crm_o.remaining());
          Buffer.sB.copy(crm_n, upup_crm_o, upup_crm_o.remaining());
          
          same_crm_o.release();
          left_crm_o.release();
          upup_crm_o.release();
          
          crm_n.release();
        }
        
        // write palette payload
        PlainNatCodec.encode(xrgb_plt, crm);
        
        same_crm.release();
        left_crm.release();
        upup_crm.release();
        xrgb_plt.release();
      }
      
      /***
       * (next) &lt;- (crm) | (prev, dims). appends to <code>list</code>
       * pairs of integers (<code>off</code> (inclusive)
       * <code>lim</code> (exclusive)) specifying the pixels that have
       * been changed.
       ***/
      static void decode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
      {
        int len_same;
        int len_left;
        int len_upup;
        
        Buffer.oB crm_o = crm.iterate();
        
        {
          Buffer.xI size_raw = central.acquireI(); // temporary
          
          // read header; sizes go into temporary buffer
          {
            int len_hdr = crm_o.rB();
            
            Buffer.xB hdr_crm = central.acquireB();
            Buffer.nB hdr_crm_n = hdr_crm.prepend();
            Buffer.sB.alias(hdr_crm_n, crm_o, len_hdr);
            hdr_crm_n.release();
            
            PlainNatCodec.decode(size_raw, hdr_crm);
            
            hdr_crm.release();
          }
          
          // read temporary buffer of sizes
          {
            Buffer.oI size_raw_o = size_raw.iterate();
            len_same = size_raw_o.rI();
            len_left = size_raw_o.rI();
            len_upup = size_raw_o.rI();
            size_raw_o.release();
          }
          
          size_raw.release();
        }
        
        Buffer.xZ same_raw = central.acquireZ();
        Buffer.xZ left_raw = central.acquireZ();
        Buffer.xZ upup_raw = central.acquireZ();
        
        Buffer.xB same_crm = central.acquireB();
        Buffer.xB left_crm = central.acquireB();
        Buffer.xB upup_crm = central.acquireB();
        
        {
          Buffer.nB same_crm_n = same_crm.prepend();
          Buffer.nB left_crm_n = left_crm.prepend();
          Buffer.nB upup_crm_n = upup_crm.prepend();
          
          Buffer.sB.alias(same_crm_n, crm_o, len_same);
          Buffer.sB.alias(left_crm_n, crm_o, len_left);
          Buffer.sB.alias(upup_crm_n, crm_o, len_upup);
          
          same_crm_n.release();
          left_crm_n.release();
          upup_crm_n.release();
        }
        
        NatDifRleCodec.decode(central, same_raw, same_crm);
        NatDifRleCodec.decode(central, left_raw, left_crm);
        NatDifRleCodec.decode(central, upup_raw, upup_crm);
        
        same_crm.release();
        left_crm.release();
        upup_crm.release();
        
        Buffer.xI xrgb_plt = central.acquireI();
        Buffer.xB xrgb_plt_crm = central.acquireB();
        
        {
          Buffer.nB xrgb_plt_crm_n = xrgb_plt_crm.prepend();
          Buffer.sB.alias(xrgb_plt_crm_n, crm_o, crm_o.remaining());
          xrgb_plt_crm_n.release();
        }
        
        PlainNatCodec.decode(xrgb_plt, xrgb_plt_crm);
        
        xrgb_plt_crm.release();
        
        Buffer.xI xrgb_raw = central.acquireI();
        
        PaletteNatCodec.decode(xrgb_raw, xrgb_plt, vI.newLinkOf((new int[8])));
        
        xrgb_plt.release();
        
        RasterCodec.decode(central, next, same_raw, left_raw, upup_raw, xrgb_raw, prev, dims, list);
        
        xrgb_raw.release();
        
        same_raw.release();
        left_raw.release();
        upup_raw.release();
        
        crm_o.release();
      }
    }
    
    /***
     * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
     * number of pixels to process (for splitting large updates).
     ***/
    public static void encode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, int trip)
    {
      ScreenCodec.encode(central, next, crm, prev, dims, trip);
    }
    
    /***
     * (next) &lt;- (crm) | (prev, dims)
     ***/
    public static void decode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
    {
      ScreenCodec.decode(central, next, crm, prev, dims, list);
    }
  }
  
  public static class _002
  {
    static int sqrd(int z)
    {
      return (z * z);
    }
    
    static int z2n(int z)
    {
      if (z > 0) {
        return ((z << 1) - 1);
      } else {
        return ((-z) << 1);
      }
    }
    
    static int n2z(int n)
    {
      if ((n & 1) == 0) {
        return (-(n >> 1));
      } else {
        return ((n >> 1) + 1);
      }
    }
    
    static class XorBitFilter
    {
      static final ThreadLocal<boolean[]> tls_sav =
        (new ThreadLocal<boolean[]>()
          {
            protected boolean[] initialValue()
            {
              return (new boolean[0]);
            }
          });
      
      static boolean[] tls_get(final int blk)
      {
        boolean[] sav = tls_sav.get();
        
        if (sav.length != blk) {
          tls_sav.set((new boolean[blk]));
          return tls_get(blk);
        }
        
        for (int i = 0; i < sav.length; i++) {
          sav[i] = false;
        }
        
        return sav;
      }
      
      static boolean xor(boolean A, boolean B)
      {
        return ((!A & B) | (A & !B));
      }
      
      /***
       * (raw) &lt;-&gt; (raw) | (blk)
       ***/
      static void apply_forward(Buffer.xZ raw, final int blk)
      {
        if (!(blk > 0)) throw null;
        
        boolean[] sav = tls_get(blk);
        
        final Buffer.oZ raw_o = raw.iterate();
        
        int rem = raw_o.remaining();
        
        while (rem >= blk) {
          for (int idx = 0; idx < blk; idx++) {
            sav[idx] = raw_o.rotZ(xor(raw_o.peekZ(), sav[idx]));
          }
          
          rem -= blk;
        }
        
        {
          int idx = 0;
          
          while (rem-- > 0) {
            sav[idx] = raw_o.rotZ(xor(raw_o.peekZ(), sav[idx]));
            idx++;
          }
        }
        
        raw_o.release();
      }
      
      /***
       * (raw) &lt;-&gt; (raw) | (blk)
       ***/
      static void apply_inverse(Buffer.xZ raw, final int blk)
      {
        if (!(blk > 0)) throw null;
        
        boolean[] sav = tls_get(blk);
        
        final Buffer.oZ raw_o = raw.iterate();
        
        int rem = raw_o.remaining();
        
        while (rem >= blk) {
          for (int idx = 0; idx < blk; idx++) {
            raw_o.wZ((sav[idx] = xor(raw_o.peekZ(), sav[idx])));
          }
          
          rem -= blk;
        }
        
        {
          int idx = 0;
          
          while (rem-- > 0) {
            raw_o.wZ((sav[idx] = xor(raw_o.peekZ(), sav[idx])));
            idx++;
          }
        }
        
        raw_o.release();
      }
    }
    
    static class DifNatFilter
    {
      /***
       * (raw) &lt;-&gt; (raw)
       ***/
      static void apply_forward(Buffer.xI raw)
      {
        Buffer.oI raw_o = raw.iterate();
        
        int mem = 0;
        
        int rem = raw_o.remaining();
        
        while (rem-- > 0) {
          int inp = raw_o.peekI();
          int out;
          
          // TODO: change this to use the highest bit for indicating
          // the sign (rather than the lowest). this can be done by
          // having the parity of the bitlength indicate the sign and
          // remving the highest bit when decoding.
          /*
          if (inp >= mem) {
            out = ((inp - mem - 0) << 1) + 0;
          } else {
            out = ((mem - inp - 1) << 1) + 1;
          }
          */
          out = z2n(inp - mem);
          mem = inp;
          
          raw_o.wI(out);
        }
        
        raw_o.release();
      }
      
      /***
       * (raw) &lt;-&gt; (raw)
       ***/
      static void apply_inverse(Buffer.xI raw)
      {
        Buffer.oI raw_o = raw.iterate();
        
        int mem = 0;
        
        int rem = raw_o.remaining();
        
        while (rem-- > 0) {
          int inp = raw_o.peekI();
          int out;
          
          /*
          if ((inp & 1) == 0) {
            out = (mem += ((inp >> 1) + 0));
          } else {
            out = (mem -= ((inp >> 1) + 1));
          }
          */
          out = (mem += n2z(inp));
          
          raw_o.wI(out);
        }
        
        raw_o.release();
      }
    }
    
    static class BasicRleBitCodec
    {
      // beware of tricky carry logic (necessary b/c it must read one
      // symbol past the current run to determine that the current run
      // has ended). "rem" is the number of symbols remaining to be
      // read from "inp", "exp" is the expected symbol type to be
      // encoded as a run and "pre[0]" is an input/output value that
      // indicates a carry (0 or 1). "rle" is the target run-length
      // buffer to write to.
      private static int runlength(Buffer.oZ inp, int rem, boolean exp, int[] pre, Buffer.nI rle)
      {
        int old_rem = rem;
        
        boolean last = exp;
        
        while ((rem > 0) && (rem-- > 0) && ((last = inp.rZ()) == exp));
        
        int amt = pre[0] + (old_rem - rem);
        pre[0] = 0;
        
        if ((rem > 0) || (last != exp)) {
          amt -= 1;
          pre[0] += 1;
        }
        
        rle.aI(amt - 1); // minus one because runs are always at least one symbol
        
        return rem;
      }
      
      /***
       * (raw) -&gt; (rl0, rl1)
       ***/
      static void encode(Buffer.xZ raw, Buffer.xI rl0, Buffer.xI rl1)
      {
        Buffer.oZ raw_o = raw.iterate();
        
        Buffer.nI rl0_n = rl0.append();
        Buffer.nI rl1_n = rl1.append();
        
        int rem = raw_o.remaining();
        int[] pre = (new int[] { 0 });
        
        if (rem > 0) {
          boolean first = raw_o.rZ();
          rem -= 1;
          pre[0] += 1;
          
          if (first == false) {
            rl0_n.aI(0);
          } else {
            rl0_n.aI(1);
            rem = runlength(raw_o, rem, true , pre, rl1_n);
          }
          
          while ((rem > 0) || (pre[0] > 0)) {
            /*                          */ rem = runlength(raw_o, rem, false, pre, rl0_n);
            if ((rem > 0) || (pre[0] > 0)) rem = runlength(raw_o, rem, true , pre, rl1_n);
          }
        }
        
        rl1_n.release();
        rl0_n.release();
        
        raw_o.release();
      }
      
      /***
       * (raw) &lt;- (rl0, rl1)
       ***/
      static void decode(Buffer.xZ raw, Buffer.xI rl0, Buffer.xI rl1)
      {
        Buffer.oI rl0_o = rl0.iterate();
        Buffer.oI rl1_o = rl1.iterate();
        
        Buffer.nZ raw_n = raw.append();
        
        if (!rl0_o.available()) {
          return; // nothing to do; no data was encoded
        }
        
        // handle first run (if it is a special case)
        {
          boolean first;
          
          switch (rl0_o.rI()) {
          case 0: first = false; break;
          case 1: first =  true; break;
          
          default: throw null;
          }
          
          if (first == false) {
            // nothing to do
          } else {
            {
              int nr1 = rl1_o.rI() + 1;
              while (nr1-- > 0) raw_n.aZ(true);
            }
          }
        }
        
        int rem = Math.min(rl0_o.remaining(), rl1_o.remaining());
        
        while (rem-- > 0) {
          {
            int nr0 = rl0_o.rI() + 1;
            while (nr0-- > 0) raw_n.aZ(false);
          }
          
          {
            int nr1 = rl1_o.rI() + 1;
            while (nr1-- > 0) raw_n.aZ(true);
          }
        }
        
        int rl0_rem = rl0_o.remaining();
        int rl1_rem = rl1_o.remaining();
        
        if (rl1_rem > 0) throw null;
        
        if (rl0_rem > 0) {
          if (!(rl0_rem == 1)) throw null;
          
          {
            int nr0 = rl0_o.rI() + 1;
            while (nr0-- > 0) raw_n.aZ(false);
          }
        }
        
        /*
        if ((rl0_rem | rl1_rem) > 0) {
          if (rl0_rem > 0) {
            if ((rl0_rem + rl1_rem) != 1) throw null;
            
            {
              int nr0 = rl0_o.rI() + 1;
              while (nr0-- > 0) raw_n.aZ(false);
            }
          }
          
          /*
          if (rl1_rem > 0) {
            if ((rl1_rem + rl0_rem) != 1) throw null;
            
            {
              int nr0 = rl0_o.rI() + 1;
              while (nr0-- > 0) raw_n.aZ(false);
            }
          }
          */
        //}
        
        rl1_o.release();
        rl0_o.release();
        
        raw_n.release();
      }
    }
    
    static class RleBitCodec
    {
      /***
       * (raw) -&gt; (crm)
       ***/
      static void encode(final BufferCentral central, Buffer.xZ raw, Buffer.xI crm)
      {
        Buffer.xI rl0 = central.acquireI();
        Buffer.xI rl1 = central.acquireI();
        
        _002.BasicRleBitCodec.encode(raw, rl0, rl1);
        _002.PlexNatCodec.encode(crm, rl0, rl1);
        
        rl0.release();
        rl1.release();
      }
      
      /***
       * (raw) &lt;- (crm)
       ***/
      static void decode(final BufferCentral central, Buffer.xZ raw, Buffer.xI crm)
      {
        Buffer.xI rl0 = central.acquireI();
        Buffer.xI rl1 = central.acquireI();
        
        _002.PlexNatCodec.decode(crm, rl0, rl1);
        _002.BasicRleBitCodec.decode(raw, rl0, rl1);
        
        rl0.release();
        rl1.release();
      }
    }
    
    static class WinnowRgbCodec
    {
      // TODO: determine good a good squared-distance threshold, and
      // adjust the fast threshold so that it does not exclude
      // anything that would pass the squared-distance threshold
      private static final int GRADIENT_QUAD_THRESHOLD = 75; // 3; //((3 * (2 * 2)) + 1);
      private static final int GRADIENT_FAST_THRESHOLD = GRADIENT_QUAD_THRESHOLD;
      
      // fast difference without branch or multiply
      static int fdif(int a, int b)
      {
        return ((~((a - b) | (b - a))) + 1);
      }
      
      /***
       * (raw) -&gt; (crm)
       ***/
      static void encode(final BufferCentral central, Buffer.xI raw, Buffer.xI crm)
      {
        Buffer.xZ cho = central.acquireZ(); // choice buffer (false = non-gradient (palatte), true = gradient)
        Buffer.xI non = central.acquireI(); // non-gradient buffer
        Buffer.xI dxR = central.acquireI(); // gradient buffer (R)
        Buffer.xI dxG = central.acquireI(); // gradient buffer (G)
        Buffer.xI dxB = central.acquireI(); // gradient buffer (B)
        {
          Buffer.nZ cho_n = cho.prepend();
          Buffer.nI non_n = non.prepend();
          Buffer.nI dxR_n = dxR.prepend();
          Buffer.nI dxG_n = dxG.prepend();
          Buffer.nI dxB_n = dxB.prepend();
          {
            Buffer.oI raw_o = raw.iterate();
            {
              int rem = raw_o.remaining();
              
              int prev_R = 0;
              int prev_G = 0;
              int prev_B = 0;
              
              while (rem-- > 0) {
                int next_X = raw_o.rI();
                
                int next_R = ((next_X >> 16)       );
                int next_G = ((next_X >>  8) & 0xFF);
                int next_B = ((next_X >>  0) & 0xFF);
                
                if ((((fdif(next_R , prev_R)) + (fdif(next_G , prev_G)) + (fdif(next_B , prev_B))) < GRADIENT_FAST_THRESHOLD) &&
                    (((sqrd(next_R - prev_R)) + (sqrd(next_G - prev_G)) + (sqrd(next_B - prev_B))) < GRADIENT_QUAD_THRESHOLD)) {
                  cho_n.aZ(true);
                  dxR_n.aI(z2n(next_R - prev_R));
                  dxG_n.aI(z2n(next_G - prev_G));
                  dxB_n.aI(z2n(next_B - prev_B));
                } else {
                  cho_n.aZ(false);
                  non_n.aI(next_X);
                }
                
                prev_R = next_R;
                prev_G = next_G;
                prev_B = next_B;
              }
            }
            raw_o.release();
          }
          cho_n.release();
          non_n.release();
          dxR_n.release();
          dxG_n.release();
          dxB_n.release();
        }
        
        //System.err.println("WinnowRgbCodec: of " + raw.length() + " raw pixels, " + dxR.length() + " were encoded as gradients");
        
        /* tested: it doesn't help
        DifNatFilter.apply_forward(dxR);
        DifNatFilter.apply_forward(dxG);
        DifNatFilter.apply_forward(dxB);
        */
        
        {
          Buffer.xI cho_rle = central.acquireI();
          Buffer.xI non_plt = central.acquireI();
          
          _002.RleBitCodec.encode(central, cho, cho_rle);
          _001.PaletteNatCodec.encode(non, non_plt, vI.newLinkOf((new int[8])));
          
          //System.err.println("cho_rle: " + cho_rle.depiction());
          
          _002.PlexNatCodec.encode(crm, cho_rle, dxR, dxG, dxB, non_plt);
          
          cho_rle.release();
          non_plt.release();
        }
        
        cho.release();
        non.release();
        dxR.release();
        dxG.release();
        dxB.release();
      }
      
      /***
       * (raw) &lt;- (crm)
       ***/
      static void decode(final BufferCentral central, Buffer.xI raw, Buffer.xI crm)
      {
        Buffer.xZ cho = central.acquireZ();
        Buffer.xI non = central.acquireI();
        Buffer.xI dxR = central.acquireI();
        Buffer.xI dxG = central.acquireI();
        Buffer.xI dxB = central.acquireI();
        
        {
          Buffer.xI cho_rle = central.acquireI();
          Buffer.xI non_plt = central.acquireI();
          
          _002.PlexNatCodec.decode(crm, cho_rle, dxR, dxG, dxB, non_plt);
          
          _002.RleBitCodec.decode(central, cho, cho_rle);
          _001.PaletteNatCodec.decode(non, non_plt, vI.newLinkOf((new int[8])));
          
          cho_rle.release();
          non_plt.release();
        }
        
        /*
        DifNatFilter.apply_inverse(dxR);
        DifNatFilter.apply_inverse(dxG);
        DifNatFilter.apply_inverse(dxB);
        */
        
        {
          Buffer.oZ cho_o = cho.iterate();
          Buffer.oI non_o = non.iterate();
          Buffer.oI dxR_o = dxR.iterate();
          Buffer.oI dxG_o = dxG.iterate();
          Buffer.oI dxB_o = dxB.iterate();
          {
            Buffer.nI raw_n = raw.append();
            {
              int rem = cho_o.remaining();
              
              int prev_R = 0;
              int prev_G = 0;
              int prev_B = 0;
              
              while (rem-- > 0) {
                int next_X;
                
                int next_R;
                int next_G;
                int next_B;
                
                if (cho_o.rZ()) {
                  next_R = prev_R + n2z(dxR_o.rI());
                  next_G = prev_G + n2z(dxG_o.rI());
                  next_B = prev_B + n2z(dxB_o.rI());
                  
                  next_X = ((((next_R << 8) + next_G) << 8) + next_B);
                } else {
                  next_X = non_o.rI();
                  
                  next_R = ((next_X >> 16)       );
                  next_G = ((next_X >>  8) & 0xFF);
                  next_B = ((next_X >>  0) & 0xFF);
                }
                
                raw_n.aI(next_X);
                
                prev_R = next_R;
                prev_G = next_G;
                prev_B = next_B;
              }
            }
            raw_n.release();
          }
          cho_o.release();
          non_o.release();
          dxR_o.release();
          dxG_o.release();
          dxB_o.release();
        }
        
        cho.release();
        non.release();
        dxR.release();
        dxG.release();
        dxB.release();
      }
    }
    
    static class PlexNatCodec
    {
      /***
       * (crm) &lt;- (raw+)
       * 
       * multiplexes <code>raw</code> and appends the result to
       * <code>crm</code>.
       * 
       * <b>warning:</b> this codec's methods reverse the usual
       * argument order convention in order to allow varargs.
       * 
       * <b>warning:</b> aliasing is used instead of copying.
       ***/
      static void encode(Buffer.xI crm, Buffer.xI... raw)
      {
        Buffer.nI crm_n = crm.append();
        
        for (int i = 0; i < raw.length; i++) {
          {
            Buffer.oI raw_o = raw[i].iterate();
            {
              int raw_length = raw_o.remaining();
              
              if (i < (raw.length - 1)) {
                crm_n.aI(raw_length);
              }
              
              Buffer.sI.alias(crm_n, raw_o, raw_length);
            }
            raw_o.release();
          }
        }
        
        crm_n.release();
      }
      
      /***
       * (crm) &lt;- (raw+)
       * 
       * demultiplexes <code>crm</code> and appends the results to
       * each of <code>raw</code>. it is critical that the number of
       * arguments passed in as <code>raw</code> matches the number of
       * arguments during encoding.
       * 
       * <b>warning:</b> this codec's methods reverse the usual
       * argument order convention in order to allow varargs.
       * 
       * <b>warning:</b> aliasing is used instead of copying.
       ***/
      static void decode(Buffer.xI crm, Buffer.xI... raw)
      {
        Buffer.oI crm_o = crm.iterate();
        {
          for (int i = 0; i < raw.length; i++) {
            int raw_length;
            
            if (i < (raw.length - 1)) {
              raw_length = crm_o.rI();
            } else {
              raw_length = crm_o.remaining();
            }
            
            Buffer.nI raw_n = raw[i].append();
            Buffer.sI.alias(raw_n, crm_o, raw_length);
            raw_n.release();
          }
        }
        crm_o.release();
      }
    }
    
    static class ScreenCodec
    {
      /***
       * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
       * number of pixels to process (for splitting large updates).
       ***/
      static void encode(final BufferCentral central, Framebuffer next, Buffer.xI crm, Framebuffer prev, Dimensions dims, int trip)
      {
        final Buffer.xZ same_bits = central.acquireZ();
        final Buffer.xZ left_bits = central.acquireZ();
        final Buffer.xZ upup_bits = central.acquireZ();
        final Buffer.xI xrgb_rgbs = central.acquireI();
        
        final Buffer.xI same_rles = central.acquireI();
        final Buffer.xI left_rles = central.acquireI();
        final Buffer.xI upup_rles = central.acquireI();
        final Buffer.xI xrgb_encs = central.acquireI();
        
        _001.RasterCodec.encode(central, next, same_bits, left_bits, upup_bits, xrgb_rgbs, prev, dims, trip);
        
        _002.XorBitFilter.apply_forward(same_bits, dims.W);
        
        _002.RleBitCodec.encode(central, same_bits, same_rles);
        _002.RleBitCodec.encode(central, left_bits, left_rles);
        _002.RleBitCodec.encode(central, upup_bits, upup_rles);
        
        //_002.DifNatFilter.apply_forward(same_rles);
        //_002.DifNatFilter.apply_forward(left_rles);
        //_002.DifNatFilter.apply_forward(upup_rles);
        
        _002.WinnowRgbCodec.encode(central, xrgb_rgbs, xrgb_encs);
        //_001.PaletteNatCodec.encode(xrgb_rgbs, xrgb_encs, vI.newLinkOf((new int[8])));
        
        _002.PlexNatCodec.encode(crm, same_rles, left_rles, upup_rles, xrgb_encs);
        
        same_rles.release();
        left_rles.release();
        upup_rles.release();
        xrgb_encs.release();
        
        same_bits.release();
        left_bits.release();
        upup_bits.release();
        xrgb_rgbs.release();
      }
      
      /***
       * (next) &lt;- (crm) | (prev, dims). appends to <code>list</code>
       * pairs of integers (<code>off</code> (inclusive)
       * <code>lim</code> (exclusive)) specifying the pixels that have
       * been changed.
       ***/
      static void decode(final BufferCentral central, Framebuffer next, Buffer.xI crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
      {
        final Buffer.xZ same_bits = central.acquireZ();
        final Buffer.xZ left_bits = central.acquireZ();
        final Buffer.xZ upup_bits = central.acquireZ();
        final Buffer.xI xrgb_rgbs = central.acquireI();
        
        final Buffer.xI same_rles = central.acquireI();
        final Buffer.xI left_rles = central.acquireI();
        final Buffer.xI upup_rles = central.acquireI();
        final Buffer.xI xrgb_encs = central.acquireI();
        
        _002.PlexNatCodec.decode(crm, same_rles, left_rles, upup_rles, xrgb_encs);
        
        _002.WinnowRgbCodec.decode(central, xrgb_rgbs, xrgb_encs);
        //_001.PaletteNatCodec.decode(xrgb_rgbs, xrgb_encs, vI.newLinkOf((new int[8])));
        
        //_002.DifNatFilter.apply_inverse(same_rles);
        //_002.DifNatFilter.apply_inverse(left_rles);
        //_002.DifNatFilter.apply_inverse(upup_rles);
        
        _002.RleBitCodec.decode(central, same_bits, same_rles);
        _002.RleBitCodec.decode(central, left_bits, left_rles);
        _002.RleBitCodec.decode(central, upup_bits, upup_rles);
        
        _002.XorBitFilter.apply_inverse(same_bits, dims.W);
        
        _001.RasterCodec.decode(central, next, same_bits, left_bits, upup_bits, xrgb_rgbs, prev, dims, list);
        
        same_rles.release();
        left_rles.release();
        upup_rles.release();
        xrgb_encs.release();
        
        same_bits.release();
        left_bits.release();
        upup_bits.release();
        xrgb_rgbs.release();
      }
    }
    
    /***
     * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
     * number of pixels to process (for splitting large updates).
     * <code>quad</code> enables quick and dirty mode.
     ***/
    public static void encode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, int trip, wiL<int[][]> aux, boolean quad)
    {
      Buffer.xI temp = central.acquireI();
      
      _002.ScreenCodec.encode(central, next, temp, prev, dims, trip);
      
      NaturalNumberCodec.enable_native_code(true);
      NaturalNumberCodec.encode(central, temp, crm, aux, quad ? 16 : 128);
      
      temp.release();
    }
    
    /***
     * (next) &lt;- (crm) | (prev, dims)
     ***/
    public static void decode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
    {
      Buffer.xI temp = central.acquireI();
      
      NaturalNumberCodec.decode(central, temp, crm);
      
      _002.ScreenCodec.decode(central, next, temp, prev, dims, list);
      
      temp.release();
    }
  }
  
  public static class _003
  {
    static class RasterCodec
    {
      static int min(int A, int B) { return ((A <= B) ? A : B); }
      
      /***
       * notes: bounding-box format is four natural numbers: (offH,
       * limH, offW, limW).
       ***/
      
      static int cmprow(Framebuffer next, Framebuffer prev, Dimensions dims, final int iy)
      {
        int d = 0;
        
        for (int ix = iy, lx = (iy + dims.W); ix < lx; ix += 1) {
          d |= (next.xRGB[ix] ^ prev.xRGB[ix]);
        }
        
        return d;
      }
      
      static int cmpcol(Framebuffer next, Framebuffer prev, Dimensions dims, final int ix)
      {
        int d = 0;
        
        for (int iy = ix, ly = (ix + dims.HxW); iy < ly; iy += dims.W) {
          d |= (next.xRGB[iy] ^ prev.xRGB[iy]);
        }
        
        return d;
      }
      
      /***
       * (next) -&gt; (bbox, same, left, upup, xrgb) | (prev, dims)
       ***/
      static void encode(final BufferCentral central, Framebuffer next, Buffer.xI bbox, Buffer.xZ same, Buffer.xZ left, Buffer.xZ upup, Buffer.xI xrgb, Framebuffer prev, Dimensions dims, int trip, final int[] complexity)
      {
        final boolean Y = true;
        final boolean N = false;
        
        int offH, limH;
        int offW, limW;
        
        // determine bounding box
        {
          // offH
          {
            offH = 0;
            
            for (int y = 0, iy = 0; y < (dims.H >> 1); y += 1, iy += dims.W) {
              if (cmprow(next, prev, dims, iy) == 0) {
                offH = y + 1;
              } else {
                break;
              }
            }
          }
          
          // limH
          {
            limH = dims.H;
            
            for (int y = (dims.H - 1), iy = (dims.HxW - dims.W); y >= offH; y -= 1, iy -= dims.W) {
              if (cmprow(next, prev, dims, iy) == 0) {
                limH = y;
              } else {
                break;
              }
            }
          }
          
          // offW
          {
            offW = 0;
            
            for (int ix = 0; ix < (dims.W >> 1); ix += 1) {
              if (cmpcol(next, prev, dims, ix) == 0) {
                offW = ix + 1;
              } else {
                break;
              }
            }
          }
          
          // limW
          {
            limW = dims.W;
            
            for (int ix = (dims.W - 1); ix >= offW; ix -= 1) {
              if (cmpcol(next, prev, dims, ix) == 0) {
                limW = ix;
              } else {
                break;
              }
            }
          }
          
          complexity[0] = (limH - offH) * (limW - offW);
          
          //System.out.println("bounding box: offH=" + offH + ", limH=" + limH + ", offW=" + offW + ", limW=" + limW + " (area=" + ((limH - offH) * (limW - offW)) + ")");
        }
        
        // write bounding box data
        {
          Buffer.nI bbox_n = bbox.append();
          
          bbox_n.aI(offH);
          bbox_n.aI(limH);
          bbox_n.aI(offW);
          bbox_n.aI(limW);
          
          bbox_n.release();
        }
        
        final int H = (limH - offH);
        final int W = (limW - offW);
        
        if (((H | W) == 0)) return; // nothing to do
        
        final int x0 = (offH * dims.W) + offW;
        final int xS = (dims.W - W);
        
        int prior_same_length = same.length();
        
        Buffer.nZ same_n = same.append();
        Buffer.nZ left_n = left.append();
        Buffer.nZ upup_n = upup.append();
        Buffer.nI xrgb_n = xrgb.append();
        
        int left_xRGB = 0;
        
        // first pixel
        {
          for (int i = x0, l = min((x0 + 1), trip); i < l; i++) {
            /* same */ if (next.xRGB[i] == prev.xRGB[i]) { same_n.aZ(Y); continue; } same_n.aZ(N);
            xrgb_n.aI(next.xRGB[i]);
          }
          
          left_xRGB = next.xRGB[x0];
        }
        
        // subsequent pixels of the first row
        {
          int i = x0 + 1;
          int l = min((x0 + W), trip);
          
          while (i < l) {
            boolean reset = false;
            
            /* same */ if (!reset) { while ((i < l) && (next.xRGB[i] == prev.xRGB[i])                               ) { same_n.aZ(Y);                                        left_xRGB = next.xRGB[i]; i++; reset = true; } }
            /* left */ if (!reset) { while ((i < l) && (next.xRGB[i] != prev.xRGB[i]) && (next.xRGB[i] == left_xRGB)) { same_n.aZ(N); left_n.aZ(Y);                          left_xRGB = next.xRGB[i]; i++; reset = true; } }
            /* xRGB */ if (!reset) {                                                                                  { same_n.aZ(N); left_n.aZ(N); xrgb_n.aI(next.xRGB[i]); left_xRGB = next.xRGB[i]; i++; reset = true; } }
          }
        }
        
        // subsequent rows
        {
          int i = x0 + W + xS;
          
          for (int y = 1; y < H; y++) {
            int l = min((i + W), trip);
            
            while (i < l) {
              boolean reset = false;
              
              /* same */ if (!reset) { while ((i < l) && (next.xRGB[i] == prev.xRGB[i])                                           ) { same_n.aZ(Y);                                                      left_xRGB = next.xRGB[i]; i++; reset = true; } }
              /* left */ if (!reset) { while ((i < l) && (next.xRGB[i] != prev.xRGB[i]) && (next.xRGB[i] == left_xRGB            )) { same_n.aZ(N); left_n.aZ(Y);                                        left_xRGB = next.xRGB[i]; i++; reset = true; } }
              /* upup */ if (!reset) { while ((i < l) && (next.xRGB[i] != prev.xRGB[i]) && (next.xRGB[i] == next.xRGB[i - dims.W])) { same_n.aZ(N); left_n.aZ(N); upup_n.aZ(Y);                          left_xRGB = next.xRGB[i]; i++; reset = true; } }
              /* xRGB */ if (!reset) {                                                                                              { same_n.aZ(N); left_n.aZ(N); upup_n.aZ(N); xrgb_n.aI(next.xRGB[i]); left_xRGB = next.xRGB[i]; i++; reset = true; } }
            }
            
            i += xS;
          }
        }
        
        same_n.release();
        left_n.release();
        upup_n.release();
        xrgb_n.release();
        
        int shortfall = ((H * W) - same.length());
        
        if (shortfall > 0) {
          Buffer.nZ post_n = same.append();
          
          while (shortfall-- > 0) {
            post_n.aZ(Y);
          }
          
          post_n.release();
        }
      }
      
      /***
       * (next) &lt;- (bbox, same, left, upup, xrgb) | (prev, dims)
       ***/
      static void decode(final BufferCentral central, Framebuffer next, Buffer.xI bbox, Buffer.xZ same, Buffer.xZ left, Buffer.xZ upup, Buffer.xI xrgb, Framebuffer prev, Dimensions dims, Buffer.xI list)
      {
        Buffer.oI bbox_o = bbox.iterate();
        
        int offH = bbox_o.rI();
        int limH = bbox_o.rI();
        int offW = bbox_o.rI();
        int limW = bbox_o.rI();
        
        bbox_o.release();
        
        final int H = (limH - offH);
        final int W = (limW - offW);
        
        if (((H | W) == 0)) return; // nothing to do
        
        final int x0 = (offH * dims.W) + offW;
        final int xS = (dims.W - W);
        
        Buffer.oZ same_o = same.iterate();
        Buffer.oZ left_o = left.iterate();
        Buffer.oZ upup_o = upup.iterate();
        Buffer.oI xrgb_o = xrgb.iterate();
        
        int xRGB = 0, left_xRGB = 0;
        
        for (int i = x0; i < x0 + 1; i++, left_xRGB = xRGB) {
          /* same */ if (same_o.rZ()) { next.xRGB[i] = xRGB = prev.xRGB[i]; continue; }
          /* xRGB */ next.xRGB[i] = xRGB = xrgb_o.rI();
        }
        
        for (int i = x0 + 1; i < x0 + W; i++, left_xRGB = xRGB) {
          /* same */ if (same_o.rZ()) { next.xRGB[i] = xRGB = prev.xRGB[i    ]; continue; }
          /* left */ if (left_o.rZ()) { next.xRGB[i] = xRGB = left_xRGB; continue; }
          /* xRGB */ next.xRGB[i] = xRGB = xrgb_o.rI();
        }
        
        int off = x0 + W + xS;
        int lim;
        
        for (int y = 1; y < H; y++) {
          lim = off + W;
          for (int i = off ; i < lim; i++, left_xRGB = xRGB) {
              /* same */ if (same_o.rZ()) { next.xRGB[i] = xRGB = prev.xRGB[i    ]; continue; }
              /* left */ if (left_o.rZ()) { next.xRGB[i] = xRGB = left_xRGB; continue; }
              /* upup */ if (upup_o.rZ()) { next.xRGB[i] = xRGB = next.xRGB[i - dims.W]; continue; }
              /* xRGB */ next.xRGB[i] = xRGB = xrgb_o.rI();
          }
          off = lim + xS;
        }
        
        same_o.release();
        left_o.release();
        upup_o.release();
        xrgb_o.release();
        
        boolean in_place = (next == prev);
        
        // determine runs of changed pixels
        if (list != null || !in_place) {
          Buffer.xI changed = central.acquireI();
          same_o = same.iterate();
          Buffer.nI changed_n = changed.append();
          boolean run_is_active = false;
          
          for (int y = 0, i = x0; y < H; y++, i += xS) {
            lim = i + W;
            for ( ; i < lim; i++) {
              if (!run_is_active) {
                if (!same_o.rZ()) {
                  changed_n.aI(i);
                  run_is_active = true;
                }
              } else {
                if (same_o.rZ()) {
                  changed_n.aI(i);
                  run_is_active = false;
                }
              }
            }
            if (run_is_active) {
              changed_n.aI(lim);
              run_is_active = false;
            }
          }
          
          if (same_o.remaining() != 0) throw null;
          
          changed_n.release();
          same_o.release();
          
          if (!in_place) {
            Buffer.oI changed_o = changed.iterate();
            
            int num = changed_o.remaining();
            
            off = 0;
            while (num > 0) {
              lim = changed_o.rI();
              num--;
              
              for (int i = off; i < lim; i++) next.xRGB[i] = prev.xRGB[i];
              off = changed_o.rI();
              num--;
            }
            lim = dims.HxW;
            
            for (int i = off; i < lim; i++) next.xRGB[i] = prev.xRGB[i];
            
            changed_o.release();
          }
          
          if (list != null) {
            
            Buffer.oI changed_o = changed.iterate();
            Buffer.nI list_n = list.append();
            
            Buffer.sI.copy(list_n, changed_o, changed_o.remaining());
            
            list_n.release();
            changed_o.release();
          }
          
          changed.release();
        }
      }
    }
    
    static class ScreenCodec
    {
      /***
       * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
       * number of pixels to process (for splitting large updates).
       ***/
      static void encode(final BufferCentral central, Framebuffer next, Buffer.xI crm, Framebuffer prev, Dimensions dims, int trip, final int[] complexity)
      {
        final Buffer.xI bbox_vals = central.acquireI();
        
        final Buffer.xZ same_bits = central.acquireZ();
        final Buffer.xZ left_bits = central.acquireZ();
        final Buffer.xZ upup_bits = central.acquireZ();
        final Buffer.xI xrgb_rgbs = central.acquireI();
        
        final Buffer.xI same_rles = central.acquireI();
        final Buffer.xI left_rles = central.acquireI();
        final Buffer.xI upup_rles = central.acquireI();
        final Buffer.xI xrgb_encs = central.acquireI();
        
        _003.RasterCodec.encode(central, next, bbox_vals, same_bits, left_bits, upup_bits, xrgb_rgbs, prev, dims, trip, complexity);
        
        _002.XorBitFilter.apply_forward(same_bits, dims.W);
        
        _002.RleBitCodec.encode(central, same_bits, same_rles);
        _002.RleBitCodec.encode(central, left_bits, left_rles);
        _002.RleBitCodec.encode(central, upup_bits, upup_rles);
        
        //_002.DifNatFilter.apply_forward(same_rles);
        //_002.DifNatFilter.apply_forward(left_rles);
        //_002.DifNatFilter.apply_forward(upup_rles);
        
        _002.WinnowRgbCodec.encode(central, xrgb_rgbs, xrgb_encs);
        //_001.PaletteNatCodec.encode(xrgb_rgbs, xrgb_encs, vI.newLinkOf((new int[8])));
        
        _002.PlexNatCodec.encode(crm, bbox_vals, same_rles, left_rles, upup_rles, xrgb_encs);
        
        same_rles.release();
        left_rles.release();
        upup_rles.release();
        xrgb_encs.release();
        
        same_bits.release();
        left_bits.release();
        upup_bits.release();
        xrgb_rgbs.release();
        
        bbox_vals.release();
      }
      
      /***
       * (next) &lt;- (crm) | (prev, dims). appends to <code>list</code>
       * pairs of integers (<code>off</code> (inclusive)
       * <code>lim</code> (exclusive)) specifying the pixels that have
       * been changed.
       ***/
      static void decode(final BufferCentral central, Framebuffer next, Buffer.xI crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
      {
        final Buffer.xI bbox_vals = central.acquireI();
        
        final Buffer.xZ same_bits = central.acquireZ();
        final Buffer.xZ left_bits = central.acquireZ();
        final Buffer.xZ upup_bits = central.acquireZ();
        final Buffer.xI xrgb_rgbs = central.acquireI();
        
        final Buffer.xI same_rles = central.acquireI();
        final Buffer.xI left_rles = central.acquireI();
        final Buffer.xI upup_rles = central.acquireI();
        final Buffer.xI xrgb_encs = central.acquireI();
        
        _002.PlexNatCodec.decode(crm, bbox_vals, same_rles, left_rles, upup_rles, xrgb_encs);
        
        _002.WinnowRgbCodec.decode(central, xrgb_rgbs, xrgb_encs);
        //_001.PaletteNatCodec.decode(xrgb_rgbs, xrgb_encs, vI.newLinkOf((new int[8])));
        
        //_002.DifNatFilter.apply_inverse(same_rles);
        //_002.DifNatFilter.apply_inverse(left_rles);
        //_002.DifNatFilter.apply_inverse(upup_rles);
        
        _002.RleBitCodec.decode(central, same_bits, same_rles);
        _002.RleBitCodec.decode(central, left_bits, left_rles);
        _002.RleBitCodec.decode(central, upup_bits, upup_rles);
        
        _002.XorBitFilter.apply_inverse(same_bits, dims.W);
        
        _003.RasterCodec.decode(central, next, bbox_vals, same_bits, left_bits, upup_bits, xrgb_rgbs, prev, dims, list);
        
        same_rles.release();
        left_rles.release();
        upup_rles.release();
        xrgb_encs.release();
        
        same_bits.release();
        left_bits.release();
        upup_bits.release();
        xrgb_rgbs.release();
        
        bbox_vals.release();
      }
    }
    
    /***
     * (next) -&gt; (crm) | (prev, dims). <code>trip</code> caps the
     * number of pixels to process (for splitting large updates).
     * <code>quad</code> enables quick and dirty mode.
     ***/
    public static void encode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, int trip, wiL<int[][]> aux, boolean quad, final int[] complexity)
    {
      Buffer.xI temp = central.acquireI();
      
      _003.ScreenCodec.encode(central, next, temp, prev, dims, trip, complexity);
      
      NaturalNumberCodec.enable_native_code(true);
      NaturalNumberCodec.encode(central, temp, crm, aux, quad ? 16 : 128);
      
      temp.release();
    }
    
    /***
     * (next) &lt;- (crm) | (prev, dims)
     ***/
    public static void decode(final BufferCentral central, Framebuffer next, Buffer.xB crm, Framebuffer prev, Dimensions dims, Buffer.xI list)
    {
      Buffer.xI temp = central.acquireI();
      
      NaturalNumberCodec.decode(central, temp, crm);
      
      _003.ScreenCodec.decode(central, next, temp, prev, dims, list);
      
      temp.release();
    }
  }
  
  public static class Test
  {
    static final Random         random = (new Random(0));
    static final BufferCentral central = (new BufferCentral());
    
    static int rand2(int maxbits)
    {
      return ((random.nextInt() >>> (32 - maxbits)) >>> (random.nextInt(maxbits)));
    }
    
    static void repeat0(F0<Nothing> proc, int millis, boolean debug_buffers)
    {
      final long enter = System.nanoTime();
      final long leave = enter + (((long)(millis)) * ((long)(1000000)));
      
      int  ctr = 0;
      long now;
      
      while ((now = System.nanoTime()) < leave) {
        proc.invoke();
        ctr++;
      }
      
      System.out.println("" + ctr + " iteration(s) in " + (((double)(now - enter)) / 1000000000.0) + " seconds(s)");
      
      if (debug_buffers) {
        System.out.println(central.depiction());
      }
    }
    
    static void repeat(F0<Nothing> proc, int millis, boolean debug_buffers)
    {
      for (int i = 0; i < 3; i++) {
        repeat0(proc, millis, debug_buffers);
      }
    }
    
    public static void main(String[] args)
    {
      final TreeMap<String, F0<Nothing>> map = (new TreeMap<String, F0<Nothing>>());
      
      map.put
        ("_001.RleBitCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xZ raw = central.acquireZ();
              Buffer.xZ res = central.acquireZ();
              Buffer.xI rl0 = central.acquireI();
              Buffer.xI rl1 = central.acquireI();
              
              // generate
              {
                Buffer.nZ raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aZ(random.nextInt(2) == 0);
                }
                
                raw_n.release();
              }
              
              _001.RleBitCodec.encode(raw, rl0, rl1);
              _001.RleBitCodec.decode(res, rl0, rl1);
              
              if (false) {
                try {
                  System.out.write(raw.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(rl0.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(rl1.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(res.depict().toNewArrayB());
                  System.out.println();
                  System.out.println();
                } catch (IOException e) {}
              }
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oZ res_o = res.iterate();
                Buffer.oZ raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rZ() != raw_o.rZ()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              raw.release();
              res.release();
              rl0.release();
              rl1.release();
              
              return null;
            }
          }));
      
      map.put
        ("_001.PlainNatCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xI raw = central.acquireI();
              Buffer.xB crm = central.acquireB();
              Buffer.xI res = central.acquireI();
              
              // generate
              {
                Buffer.nI raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  switch (random.nextInt(4)) {
                  case 0: raw_n.aI(random.nextInt(1 <<  8)); break;
                  case 1: raw_n.aI(random.nextInt(1 << 16)); break;
                  case 2: raw_n.aI(random.nextInt(1 << 24)); break;
                  case 3: raw_n.aI(random.nextInt(1 << 30)); break; // yes, 30 (don't want sign trouble)
                  default: throw null;
                  }
                }
                
                raw_n.release();
              }
              
              _001.PlainNatCodec.encode(raw, crm);
              _001.PlainNatCodec.decode(res, crm);
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oI res_o = res.iterate();
                Buffer.oI raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rI() != raw_o.rI()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              res.release();
              crm.release();
              raw.release();
              
              return null;
            }
          }));
      
      map.put
        ("TrendyDifferentialNatCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xI raw = central.acquireI();
              Buffer.xZ nng = central.acquireZ();
              Buffer.xI dif = central.acquireI();
              Buffer.xI res = central.acquireI();
              
              // generate
              {
                Buffer.nI raw_n = raw.append();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aI(random.nextInt(128) - 64);
                }
                
                raw_n.release();
              }
              
              _001.TrendyDifferentialNatCodec.encode(raw, nng, dif);
              _001.TrendyDifferentialNatCodec.decode(res, nng, dif);
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oI res_o = res.iterate();
                Buffer.oI raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rI() != raw_o.rI()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              res.release();
              raw.release();
              
              return null;
            }
          }));
      
      final F1<F0<Nothing>, F3<Nothing, Buffer.xI, Buffer.xI, Buffer.xI>> DifferentialNatCodecTestFactory =
        (new F1<F0<Nothing>, F3<Nothing, Buffer.xI, Buffer.xI, Buffer.xI>>()
         {
           public F0<Nothing> invoke(final F3<Nothing, Buffer.xI, Buffer.xI, Buffer.xI> core)
           {
             return
               (new F0<Nothing>()
                 {
                   public Nothing invoke()
                   {
                     int len = rand2(16);
                     
                     Buffer.xI raw = central.acquireI();
                     Buffer.xI dif = central.acquireI();
                     Buffer.xI res = central.acquireI();
                     
                     // generate
                     {
                       Buffer.nI raw_n = raw.prepend();
                       
                       for (int i = 0; i < len; i++) {
                         raw_n.aI(random.nextInt(128) - 64);
                       }
                       
                       raw_n.release();
                     }
                     
                     core.invoke(raw, dif, res);
                     
                     // verify
                     {
                       if (res.length() != raw.length()) throw null;
                       
                       Buffer.oI raw_o = raw.iterate();
                       Buffer.oI res_o = res.iterate();
                       
                       for (int i = 0; i < len; i++) {
                         if (res_o.rI() != raw_o.rI()) {
                           System.out.println(raw.depiction());
                           System.out.println(dif.depiction());
                           System.out.println(res.depiction());
                           throw null;
                         }
                       }
                       
                       res_o.release();
                       raw_o.release();
                     }
                     
                     raw.release();
                     dif.release();
                     res.release();
                     
                     return null;
                   }
                 });
           }
         });
      
      map.put
        ("_001.NoisyDifferentialNatCodec",
         (DifferentialNatCodecTestFactory.invoke
          (new F3<Nothing, Buffer.xI, Buffer.xI, Buffer.xI>()
           {
             public Nothing invoke(Buffer.xI raw, Buffer.xI dif, Buffer.xI res)
             {
               _001.NoisyDifferentialNatCodec.encode(raw, dif);
               _001.NoisyDifferentialNatCodec.decode(res, dif);
               
               return null;
             }
           })));
      
      map.put
        ("_001.PaletteNatCodec",
         (new F0<Nothing>()
          {
            private int rand()
            {
              if (random.nextInt(2) == 0) {
                return random.nextInt(8);
              } else {
                return random.nextInt(65536);
              }
            }
            
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xI raw = central.acquireI();
              Buffer.xI crm = central.acquireI();
              Buffer.xI res = central.acquireI();
              
              // generate
              {
                Buffer.nI raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aI(rand());
                }
                
                raw_n.release();
              }
              
              {
                vI plt = vI.newLinkOf((new int[8]));
                _001.PaletteNatCodec.encode(raw, crm, plt);
              }
              
              {
                vI plt = vI.newLinkOf((new int[8]));
                _001.PaletteNatCodec.decode(res, crm, plt);
              }
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oI res_o = res.iterate();
                Buffer.oI raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rI() != raw_o.rI()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              raw.release();
              crm.release();
              res.release();
              
              return null;
            }
          }));
        
      map.put
        ("_001.NatDifRleCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xZ raw = central.acquireZ();
              Buffer.xB crm = central.acquireB();
              Buffer.xZ res = central.acquireZ();
              
              // generate
              {
                Buffer.nZ raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aZ(random.nextInt(2) == 0);
                }
                
                raw_n.release();
              }
              
              _001.NatDifRleCodec.encode(central, raw, crm);
              _001.NatDifRleCodec.decode(central, res, crm);
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oZ res_o = res.iterate();
                Buffer.oZ raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rZ() != raw_o.rZ()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              raw.release();
              crm.release();
              res.release();
              
              return null;
            }
          }));
      
      map.put
        ("_001.RasterCodec",
         (new F0<Nothing>()
          {
            final int H = 33;
            final int W = 33;
            
            final Dimensions dims = (new Dimensions(H, W));
            
            final Framebuffer prev = (new Framebuffer(dims));
            final Framebuffer next = (new Framebuffer(dims));
            final Framebuffer pred = (new Framebuffer(dims));
            
            int col6()
            {
              switch (random.nextInt(6)) {
              case 0: return 0xFF0000; // red
              case 1: return 0x00FF00; // green
              case 2: return 0x0000FF; // blue
              case 3: return 0xFFFF00; // ?
              case 4: return 0x00FFFF; // ?
              case 5: return 0xFF00FF; // ?
              default: throw null;
              }
            }
            
            public Nothing invoke()
            {
              for (int i = 0; i < dims.HxW; i++) {
                prev.xRGB[i] = col6();
                next.xRGB[i] = col6();
              }
              
              Buffer.xZ same = central.acquireZ();
              Buffer.xZ left = central.acquireZ();
              Buffer.xZ upup = central.acquireZ();
              Buffer.xI xrgb = central.acquireI();
              Buffer.xI list = central.acquireI();
              
              _001.RasterCodec.encode(central, next, same, left, upup, xrgb, prev, dims, dims.HxW);
              _001.RasterCodec.decode(central, pred, same, left, upup, xrgb, prev, dims, list);
              
              same.release();
              left.release();
              upup.release();
              xrgb.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (next.xRGB[i] != pred.xRGB[i]) {
                  throw null;
                }
              }
              
              pred.copyFromPeer(prev);
              pred.copyFromPeer(next, list);
              
              list.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (next.xRGB[i] != pred.xRGB[i]) {
                  throw null;
                }
              }
              
              return null;
            }
          }));
      
      map.put
        ("_001.ScreenCodec",
         (new F0<Nothing>()
          {
            final int H = 33;
            final int W = 33;
            
            final Dimensions dims = (new Dimensions(H, W));
            
            final Framebuffer prev = (new Framebuffer(dims));
            final Framebuffer next = (new Framebuffer(dims));
            final Framebuffer pred = (new Framebuffer(dims));
            
            int col6()
            {
              switch (random.nextInt(6)) {
              case 0: return 0xFF0000; // red
              case 1: return 0x00FF00; // green
              case 2: return 0x0000FF; // blue
              case 3: return 0xFFFF00; // ?
              case 4: return 0x00FFFF; // ?
              case 5: return 0xFF00FF; // ?
              default: throw null;
              }
            }
            
            public Nothing invoke()
            {
              for (int i = 0; i < dims.HxW; i++) {
                prev.xRGB[i] = col6();
                next.xRGB[i] = col6();
              }
              
              Buffer.xB crm = central.acquireB();
              Buffer.xI list = central.acquireI();
              
              _001.ScreenCodec.encode(central, next, crm, prev, dims, dims.HxW);
              _001.ScreenCodec.decode(central, pred, crm, prev, dims, list);
              
              crm.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              pred.copyFromPeer(prev);
              pred.copyFromPeer(next, list);
              
              list.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              return null;
            }
          }));
      
      map.put
        ("_002.XorBitFilter",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              final int len = rand2(16);
              final int blk = rand2(8) + 1;
              
              final int multiplier = 1;
              
              Buffer.xZ fg = central.acquireZ();
              Buffer.xZ bg = central.acquireZ();
              
              {
                Buffer.nZ fg_n = bg.prepend();
                Buffer.nZ bg_n = fg.prepend();
                
                for (int i = 0; i < len; i++) {
                  boolean x = (random.nextInt(2) == 1);
                  fg_n.aZ(x);
                  bg_n.aZ(x);
                }
                
                fg_n.release();
                bg_n.release();
              }
              
              for (int i = 0; i < multiplier; i++) {
                _002.XorBitFilter.apply_forward(fg, blk);
                _002.XorBitFilter.apply_inverse(fg, blk);
              }
              
              if (!Arrays.equals(fg.toNewArrayZ(), bg.toNewArrayZ())) throw null;
              
              fg.release();
              bg.release();
              
              return null;
            }
          }));
      
      map.put
        ("_002.PlexNatCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(8);
              
              Buffer.xI[] inp_vec = (new Buffer.xI[len]);
              Buffer.xI[] out_vec = (new Buffer.xI[len]);
              
              Buffer.xI crm = central.acquireI();
              
              for (int i = 0; i < len; i++) {
                inp_vec[i] = central.acquireI();
                out_vec[i] = central.acquireI();
                
                Buffer.nI raw_n = inp_vec[i].prepend();
                
                {
                  int l = rand2(8);
                  
                  while (l-- > 0) {
                    raw_n.aI(rand2(30));
                  }
                }
                
                raw_n.release();
              }
              
              _002.PlexNatCodec.encode(crm, inp_vec);
              _002.PlexNatCodec.decode(crm, out_vec);
              
              crm.release();
              
              for (int i = 0; i < len; i++) {
                if (!Arrays.equals(inp_vec[i].toNewArrayI(), out_vec[i].toNewArrayI())) throw null;
              }
              
              for (int i = 0; i < len; i++) {
                inp_vec[i].release();
                out_vec[i].release();
              }
              
              return null;
            }
          }));
      
      map.put
        ("_002.RleBitCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xZ raw = central.acquireZ();
              Buffer.xZ res = central.acquireZ();
              Buffer.xI crm = central.acquireI();
              
              // generate
              {
                Buffer.nZ raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aZ(random.nextInt(2) == 0);
                }
                
                raw_n.release();
              }
              
              _002.RleBitCodec.encode(central, raw, crm);
              _002.RleBitCodec.decode(central, res, crm);
              
              // verify
              if (!Arrays.equals(res.toNewArrayZ(), raw.toNewArrayZ())) throw null;
              
              raw.release();
              res.release();
              crm.release();
              
              return null;
            }
          }));
      
      map.put
        ("_002.DifNatFilter",
         (DifferentialNatCodecTestFactory.invoke
          (new F3<Nothing, Buffer.xI, Buffer.xI, Buffer.xI>()
           {
             public Nothing invoke(Buffer.xI raw, Buffer.xI dif, Buffer.xI res)
             {
               Buffer.sI.copy(dif, raw);
               _002.DifNatFilter.apply_forward(dif);
               
               Buffer.sI.copy(res, dif);
               _002.DifNatFilter.apply_inverse(res);
               
               return null;
             }
           })));
      
      map.put
        ("_001.PaletteNatCodec",
         (new F0<Nothing>()
          {
            private int rand()
            {
              if (random.nextInt(2) == 0) {
                return random.nextInt(8);
              } else {
                return random.nextInt(65536);
              }
            }
            
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xI raw = central.acquireI();
              Buffer.xI crm = central.acquireI();
              Buffer.xI res = central.acquireI();
              
              // generate
              {
                Buffer.nI raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aI(rand());
                }
                
                raw_n.release();
              }
              
              {
                vI plt = vI.newLinkOf((new int[8]));
                _001.PaletteNatCodec.encode(raw, crm, plt);
              }
              
              {
                vI plt = vI.newLinkOf((new int[8]));
                _001.PaletteNatCodec.decode(res, crm, plt);
              }
              
              // verify
              {
                if (res.length() != raw.length()) throw null;
                
                Buffer.oI res_o = res.iterate();
                Buffer.oI raw_o = raw.iterate();
                
                for (int i = 0; i < len; i++) {
                  if (res_o.rI() != raw_o.rI()) {
                    throw null;
                  }
                }
                
                raw_o.release();
                res_o.release();
              }
              
              raw.release();
              crm.release();
              res.release();
              
              return null;
            }
          }));
      
      map.put
        ("_002.WinnowRgbCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(4); //rand2(16);
              
              Buffer.xI raw = central.acquireI();
              Buffer.xI crm = central.acquireI();
              Buffer.xI res = central.acquireI();
              
              // generate
              {
                Buffer.nI raw_n = raw.prepend();
                
                int last = 0;
                
                for (int i = 0; i < len; i++) {
                  switch (random.nextInt(3)) {
                  case 0:
                    raw_n.aI(last = rand2(24));
                    //System.out.println("b0: " + last);
                    break;
                    
                  case 1:
                    raw_n.aI(last = (last + ((((rand2(2) << 8) + rand2(2)) << 8) + rand2(2))));
                    //System.out.println("b1: " + last);
                    break;
                    
                  case 2:
                    raw_n.aI(rand2(3));
                    //System.out.println("b3: " + last);
                    break;
                  }
                }
                
                raw_n.release();
              }
              
              _002.WinnowRgbCodec.encode(central, raw, crm);
              _002.WinnowRgbCodec.decode(central, res, crm);
              
              // verify
              if (!Arrays.equals(res.toNewArrayI(), raw.toNewArrayI())) {
                System.out.println("RAW FOLLOWS: " + raw.depiction());
                System.out.println("CRM_FOLLOWS: " + crm.depiction());
                System.out.println("RES FOLLOWS: " + res.depiction());
                throw null;
              }
              
              raw.release();
              crm.release();
              res.release();
              
              return null;
            }
          }));
      
      map.put
        ("_002.ScreenCodec.Z",
         (new F0<Nothing>()
          {
            final int H = 33;
            final int W = 33;
            
            final Dimensions dims = (new Dimensions(H, W));
            
            final Framebuffer prev = (new Framebuffer(dims));
            final Framebuffer next = (new Framebuffer(dims));
            final Framebuffer pred = (new Framebuffer(dims));
            
            int col6()
            {
              switch (random.nextInt(2)) {
              case 0:
                {
                  switch (random.nextInt(6)) {
                  case 0: return 0xFF0000; // red
                  case 1: return 0x00FF00; // green
                  case 2: return 0x0000FF; // blue
                  case 3: return 0xFFFF00; // ?
                  case 4: return 0x00FFFF; // ?
                  case 5: return 0xFF00FF; // ?
                  default: throw null;
                  }
                }
                
              case 1:
                {
                  return rand2(24);
                }
                
              default:
                throw null;
              }
            }
            
            public Nothing invoke()
            {
              for (int i = 0; i < dims.HxW; i++) {
                prev.xRGB[i] = col6();
                next.xRGB[i] = col6();
              }
              
              Buffer.xI crm = central.acquireI();
              Buffer.xI list = central.acquireI();
              
              _002.ScreenCodec.encode(central, next, crm, prev, dims, dims.HxW);
              _002.ScreenCodec.decode(central, pred, crm, prev, dims, list);
              
              crm.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              pred.copyFromPeer(prev);
              pred.copyFromPeer(next, list);
              
              list.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              return null;
            }
          }));
      
      map.put
        ("_002.ScreenCodec.B",
         (new F0<Nothing>()
          {
            final int H = 33;
            final int W = 33;
            
            final Dimensions dims = (new Dimensions(H, W));
            
            final Framebuffer prev = (new Framebuffer(dims));
            final Framebuffer next = (new Framebuffer(dims));
            final Framebuffer pred = (new Framebuffer(dims));
            
            int col6()
            {
              switch (random.nextInt(2)) {
              case 0:
                {
                  switch (random.nextInt(6)) {
                  case 0: return 0xFF0000; // red
                  case 1: return 0x00FF00; // green
                  case 2: return 0x0000FF; // blue
                  case 3: return 0xFFFF00; // ?
                  case 4: return 0x00FFFF; // ?
                  case 5: return 0xFF00FF; // ?
                  default: throw null;
                  }
                }
                
              case 1:
                {
                  return rand2(24);
                }
                
              default:
                throw null;
              }
            }
            
            public Nothing invoke()
            {
              for (int i = 0; i < dims.HxW; i++) {
                prev.xRGB[i] = col6();
                next.xRGB[i] = col6();
              }
              
              Buffer.xB crm = central.acquireB();
              Buffer.xI list = central.acquireI();
              
              _002.encode(central, next, crm, prev, dims, dims.HxW, null, true);
              _002.decode(central, pred, crm, prev, dims, list);
              
              crm.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              pred.copyFromPeer(prev);
              pred.copyFromPeer(next, list);
              
              list.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              return null;
            }
          }));
      
      map.put
        ("_002.BasicRleBitCodec",
         (new F0<Nothing>()
          {
            public Nothing invoke()
            {
              int len = rand2(16);
              
              Buffer.xZ raw = central.acquireZ();
              Buffer.xZ res = central.acquireZ();
              Buffer.xI rl0 = central.acquireI();
              Buffer.xI rl1 = central.acquireI();
              
              // generate
              {
                Buffer.nZ raw_n = raw.prepend();
                
                for (int i = 0; i < len; i++) {
                  raw_n.aZ(random.nextInt(2) == 0);
                }
                
                raw_n.release();
              }
              
              _002.BasicRleBitCodec.encode(raw, rl0, rl1);
              _002.BasicRleBitCodec.decode(res, rl0, rl1);
              
              if (false) {
                try {
                  System.out.write(raw.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(rl0.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(rl1.depict().toNewArrayB());
                  System.out.println();
                  System.out.write(res.depict().toNewArrayB());
                  System.out.println();
                  System.out.println();
                } catch (IOException e) {}
              }
              
              // verify
              {
                if (!Arrays.equals(res.toNewArrayZ(), raw.toNewArrayZ())) {
                  System.out.println(raw.depiction());
                  System.out.println(rl0.depiction());
                  System.out.println(rl1.depiction());
                  System.out.println(res.depiction());
                  throw null;
                }
              }
              
              raw.release();
              res.release();
              rl0.release();
              rl1.release();
              
              return null;
            }
          }));
      
      map.put
        ("_003.ScreenCodec",
         (new F0<Nothing>()
          {
            final int H = 33;
            final int W = 33;
            
            final Dimensions dims = (new Dimensions(H, W));
            
            final Framebuffer prev = (new Framebuffer(dims));
            final Framebuffer next = (new Framebuffer(dims));
            final Framebuffer pred = (new Framebuffer(dims));
            
            int col6()
            {
              switch (random.nextInt(2)) {
              case 0:
                {
                  switch (random.nextInt(6)) {
                  case 0: return 0xFF0000; // red
                  case 1: return 0x00FF00; // green
                  case 2: return 0x0000FF; // blue
                  case 3: return 0xFFFF00; // ?
                  case 4: return 0x00FFFF; // ?
                  case 5: return 0xFF00FF; // ?
                  default: throw null;
                  }
                }
                
              case 1:
                {
                  return rand2(24);
                }
                
              default:
                throw null;
              }
            }
            
            public Nothing invoke()
            {
              for (int i = 0; i < dims.HxW; i++) {
                next.xRGB[i] = prev.xRGB[i] = col6();
              }

              for (int i = 0; i < dims.HxW; i++) {
                if (random.nextInt(dims.H) == 1) {
                  next.xRGB[i] = col6();
                }
              }
              
              Buffer.xB crm = central.acquireB();
              Buffer.xI list = central.acquireI();
              
              final int[] complexity = (new int[1]);
              
              _003.encode(central, next, crm, prev, dims, dims.HxW, null, true, complexity);
              _003.decode(central, pred, crm, prev, dims, list);
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              // in place test
              _003.decode(central, prev, crm, prev, dims, list);
              
              crm.release();

              for (int i = 0; i < dims.HxW; i++) {
                if (prev.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              pred.copyFromPeer(prev);
              pred.copyFromPeer(next, list);
              
              list.release();
              
              for (int i = 0; i < dims.HxW; i++) {
                if (pred.xRGB[i] != next.xRGB[i]) {
                  throw null;
                }
              }
              
              return null;
            }
          }));
      
      {
        NaturalNumberCodec.enable_native_code(true);
      }
      
      {
        final boolean debug_buffers = false;
        
        {
          final int millis = Integer.parseInt(args[0]);
          
          for (int i = 1; i < args.length; i++) {
            System.out.println(args[i]);
            repeat(map.get(args[i]), millis, debug_buffers);
          }
        }
        
        if (!debug_buffers) {
          System.out.println(central.depiction());
        }
      }
    }
  }
}

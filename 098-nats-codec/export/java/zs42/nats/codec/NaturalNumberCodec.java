/***
 * NaturalNumberCodec.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.nats.codec;

import java.io.*;

import zs42.mass.*;
import zs42.buff.*;

public class NaturalNumberCodec
{
  /***
   * returns the "bit length" of <code>v</code>. the bit length of
   * <code>v</code> is the smallest <code>x</code> such that <code>(v
   * &amp; ((1 &lt;&lt; x) - 1)) == v</code>. the bit length is only
   * defined for non-negative integers. note that the bit length of
   * zero is zero.
   ***/
  public static int bit_length_32(int v)
  {
    if (v < 0) throw null;
    
    int x = 0;
    
    while ((v & ((1 << x) - 1)) != v) x++;
    
    return x;
  }
  
  /***
   * returns the "bit length" of <code>v</code>. see the documentation
   * for the 32-bit version.
   ***/
  public static int bit_length_64(long v)
  {
    if (v < 0) throw null;
    
    int x = 0;
    
    while ((v & ((1L << x) - 1)) != v) x++;
    
    return x;
  }
  
  /***
   * returns an integer formed from the next <code>n</code> bits from
   * <code>inp</code> interpreted in big-endian order. returns
   * <code>0</code> if <code>n == 0</code>.
   ***/
  public static int recv_bits(Buffer.oZ inp, int n)
  {
    int i = n;
    int v = 0;
    
    while (i-- > 0) {
      v |= ((inp.rZ() ? 1 : 0) << i);
    }
    
    return v;
  }
  
  /***
   * writes the big-endian representation of the last <code>n</code>
   * bits of <code>v</code> to <code>out</code>. has no effect if
   * <code>n == 0</code>.
   ***/
  public static void send_bits(Buffer.nZ out, int n, int v)
  {
    int i = n;
    
    while (i-- > 0) {
      out.aZ((((v >> i) & 1) == 1));
    }
  }
  
  /***
   * returns the length, in bits, of the univeral encoding of
   * <code>v</code>.
   ***/
  public static int encode_universal_length(int v)
  {
    return ((bit_length_32(v)) << 1);
  }
  
  /***
   * writes the univeral encoding of <code>v</code> to
   * <code>out</code>.
   ***/
  public static void encode_universal(Buffer.nZ out, int v)
  {
    int n = bit_length_32(v);
    
    if (n == 0) {
      out.aZ(true);
    } else {
      for (int i = 0; i < n; i++) {
        out.aZ(false);
      }
      
      send_bits(out, n, v);
    }
  }
  
  /***
   * returns the result of decoding one non-negative integer from
   * <code>inp</code>.
   ***/
  public static int decode_universal(Buffer.oZ inp)
  {
    int n = 0;
    
    while (inp.rZ() == false) n++;
    
    /****/ if (n == 0) {
      return 0;
    } else {
      n--;
      return ((1 << n) | recv_bits(inp, n));
    }
  }
  
  private final static int TEMP_NAUX = 3;
  private final static int TEMP_SIZE = 512;
  
  private static void resize_auxiliary(wiL<int[][]> aux, int len)
  {
    int[][] tables = null;
    if (aux != null) tables = aux.get();
    if (tables == null) tables = (new int[TEMP_NAUX][TEMP_SIZE]);
    if (aux != null) aux.put(tables);
    
    int deslen = zs42.mass.Static.hinterpret(len, 0);
    
    for (int i = 0; i < TEMP_NAUX; i++) {
      if (tables[i].length < deslen) {
        tables[i] = (new int[deslen]);
      }
    }
  }
  
  private static int span_maximum_bit_length(int[] blen, int off, int lim)
  {
    if (!(off < lim)) throw null;
    
    int maxlen = 0;
    
    for (int i = off; i < lim; i++) {
      maxlen = Math.max(maxlen, blen[i]);
    }
    
    return maxlen;
  }
  
  private static int encode_span_length(int maxlen, int off, int lim)
  {
    if (!(off < lim)) throw null;
    
    int length = 0;
    
    length += encode_universal_length(maxlen);
    length += encode_universal_length((lim - off));
    length += ((lim - off) * maxlen);
    
    return length;
  }
  
  private static int encode_span_length(int[] blen, int off, int lim)
  {
    return encode_span_length(span_maximum_bit_length(blen, off, lim), off, lim);
  }
  
  /***
   * meaning:
   * 
   *  numv - number of input values
   *  blen - bit-length of each input value
   *  optc - optimal encoded length (cost) of input tail
   *  optn - number of input values to join to attain optl
   ***/
  private static void optimize(int numv, int[] blen, int[] optc, int[] optn, int xlen)
  {
    // first pass
    // dynamic programming
    // for speed, do not consider runs longer than xlen
    {
      int off = numv;
      
      while (off-- > 0) {
        int local_optc = Integer.MAX_VALUE;
        int local_optn = -1;
        
        int maxlen = 0;
        
        for (int lim = off + 1, end = Math.min(off + xlen, numv); lim <= end; lim++) {
          // try joining (inclusive) off..lim (exclusive)
          
          // including one more value
          maxlen = Math.max(maxlen, blen[lim - 1]);
          
          // calculate cost
          int cost = encode_span_length(maxlen, off, lim);
          if (lim < numv) cost += optc[lim];
          
          // update optimal solution
          if (cost < local_optc) {
            local_optc = cost;
            local_optn = (lim - off);
          }
        }
        
        if (local_optn == -1) throw null;
        
        optc[off] = local_optc;
        optn[off] = local_optn;
      }
    }
    
    // second pass
    // merge adjacent runs of equal bit-length
    // on optimal path only, of course
    {
      int off = 0;
      
      if (off < numv) {
        int lim = off + optn[off];
        
        while (lim < numv) {
          int off2 = lim;
          int lim2 = off2 + optn[off2];
          
          if (span_maximum_bit_length(blen, off, lim) == span_maximum_bit_length(blen, off2, lim2)) {
            optn[off] += optn[off2];
          } else {
            off = off2;
          }
          
          lim = lim2;
        }
      }
    }
  }
  
  private static boolean try_native_code = true;
  private static boolean use_native_code = false;
  
  /***
   * enables the native code optimizer. approximately 50% higher
   * throughput. if the native code optimizer cannot be loaded for any
   * reason and <code>force</code> is <code>true</code>, the program
   * is forcefully terminated. otherwise, the bytecode optimizer is
   * used as a fallback. returns <code>true</code> iff the native
   * optimizer will be used.
   ***/
  public static synchronized boolean enable_native_code(boolean force)
  {
    try {
      if (try_native_code) {
        try_native_code = false;
        Runtime.getRuntime().load((new File(System.getProperty("zs42.nats.codec.NaturalNumberCodec.sopath"))).getAbsolutePath());
        use_native_code = true;
      }
    } catch (Throwable e) {
      e.printStackTrace(System.err);
      if (force) {
        Runtime.getRuntime().halt(1);
        while (true);
      } else {
        // no need to bomb; ideally, things should continue running on bytecodes ...
      }
    }
    
    return use_native_code;
  }
  
  /***
   * (raw) -&gt; (crm)
   * 
   * <code>xlen</code> specifies the optimization level. performance
   * decreases linearly with increasing <code>xlen</code> (in the
   * limit of the input length being much longer than
   * <code>xlen</code>). the recommended value is 128 (or 64 if native
   * code is not enabled). most likely, a higher optimization level
   * would yield only marginally better encoding, at the cost of much
   * larger latencies.
   ***/
  public static void encode_lowlevel(Buffer.xI raw, Buffer.xZ crm, wiL<int[][]> aux, int xlen)
  {
    final int raw_length = raw.length();
    
    resize_auxiliary(aux, raw_length);
    
    int[] blen;
    int[] optc;
    int[] optn;

    if (aux != null) {
      blen = aux.get()[0];
      optc = aux.get()[1];
      optn = aux.get()[2];
    } else {
      blen = (new int[raw_length]);
      optc = (new int[raw_length]);
      optn = (new int[raw_length]);
    }
    
    // fill in blen
    {
      Buffer.oI raw_o = raw.iterate();
      
      for (int i = 0; i < raw_length; i++) {
        blen[i] = bit_length_32(raw_o.rI());
      }
      
      raw_o.release();
    }
    
    // determine optn
    if (xlen < 1) throw null;
    
    if (use_native_code) {
      Optimizer.optimize(raw_length, blen, optc, optn, xlen);
    } else {
      optimize(raw_length, blen, optc, optn, xlen);
    }
    
    // encode according to optn
    {
      Buffer.nZ crm_n = crm.append();
      {
        encode_universal(crm_n, raw_length);
        
        {
          Buffer.oI raw_o = raw.iterate();
          {
            int off = 0;
            
            while (off < raw_length) {
              int lim = off + optn[off];
              int nrb = span_maximum_bit_length(blen, off, lim);
              
              // look-ahead loop: while bit-length for adjacent spans
              // is equal, encode them together. this allows spans
              // longer than xlen (in particularily favorable
              // circumstances only, of course).
              // 
              // update: this is now done in the optimizer itself,
              // which is a somewhat cleaner design.
              // 
              // {
              //   while ((lim < raw_length) && (span_maximum_bit_length(blen, lim, lim + optn[lim]) == nrb)) {
              //     lim += optn[lim];
              //     throw null;
              //   }
              // }
              
              encode_universal(crm_n, nrb);
              encode_universal(crm_n, ((lim - off) - 1)); // minus one because span length is always greater than 0
              
              while (off < lim) {
                send_bits(crm_n, nrb, raw_o.rI());
                off++;
              }
            }
          }
          raw_o.release();
        }
      }
      
      crm_n.release();
    }
  }
  
  /***
   * (raw) &lt;- (crm)
   ***/
  public static void decode_lowlevel(Buffer.xI raw, Buffer.xZ crm, boolean allow_trailing)
  {
    Buffer.nI raw_n = raw.append();
    {
      Buffer.oZ crm_o = crm.iterate();
      {
        int raw_length = decode_universal(crm_o);
        
        while (raw_length > 0) {
          int nrb = decode_universal(crm_o);
          int len = decode_universal(crm_o) + 1; // plus one to cancel minus one during encoding
          
          raw_length -= len; // cannot move to bottom of loop because len is mutated
          
          if (nrb > 0) {
            while (len-- > 0) {
              raw_n.aI(recv_bits(crm_o, nrb));
            }
          } else {
            while (len-- > 0) {
              raw_n.aI(0);
            }
          }
        }
        
        if (!allow_trailing) {
          if (raw_length != 0) {
            throw null;
          }
        }
      }
      crm_o.release();
    }
    raw_n.release();
  }
  
  /***
   * (raw) -&gt; (crm)
   * 
   * <code>xlen</code> specifies the optimization level. performance
   * decreases linearly with increasing <code>xlen</code> (in the
   * limit of the input length being much longer than
   * <code>xlen</code>). the recommended value is 128 (or 64 if native
   * code is not enabled). most likely, a higher optimization level
   * would yield only marginally better encoding, at the cost of much
   * larger latencies.
   ***/
  public static void encode(BufferCentral central, Buffer.xI raw, Buffer.xB crm, wiL<int[][]> aux, int xlen)
  {
    Buffer.xZ tmp = central.acquireZ();
    
    encode_lowlevel(raw, tmp, aux, xlen);
    
    {
      Buffer.oZ tmp_o = tmp.iterate();
      Buffer.nB crm_n = crm.append();
      {
        int rem = tmp_o.remaining();
        
        while (rem >= 8) {
          int pak =
            ((tmp_o.rZ() ? 1 : 0) << 7) |
            ((tmp_o.rZ() ? 1 : 0) << 6) |
            ((tmp_o.rZ() ? 1 : 0) << 5) |
            ((tmp_o.rZ() ? 1 : 0) << 4) |
            
            ((tmp_o.rZ() ? 1 : 0) << 3) |
            ((tmp_o.rZ() ? 1 : 0) << 2) |
            ((tmp_o.rZ() ? 1 : 0) << 1) |
            ((tmp_o.rZ() ? 1 : 0)     ) ;
          
          crm_n.aB(((byte)(pak)));
          
          rem -= 8;
        }
        
        if (rem > 0) {
          int pak = 0;
          
                     if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          pak <<= 1; if (rem > 0) { pak |= (tmp_o.rZ() ? 1 : 0); rem--; }
          
          crm_n.aB(((byte)(pak)));
        }
        
        if (rem > 0) {
          throw null;
        }
      }
      tmp_o.release();
      crm_n.release();
    }
    
    //System.out.println(tmp.depiction());
    //System.out.println(crm.depiction());
    
    tmp.release();
  }
  
  /***
   * (raw) &lt;- (crm)
   ***/
  public static void decode(BufferCentral central, Buffer.xI raw, Buffer.xB crm)
  {
    Buffer.xZ tmp = central.acquireZ();
    
    {
      Buffer.oB crm_o = crm.iterate();
      Buffer.nZ tmp_n = tmp.prepend();
      {
        int rem = crm_o.remaining();
        
        while (rem-- > 0) {
          int pak = crm_o.rB();
          
          tmp_n.aZ(((((pak >> 7) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 6) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 5) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 4) & 1) == 0) ? (false) : (true)));
          
          tmp_n.aZ(((((pak >> 3) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 2) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 1) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak     ) & 1) == 0) ? (false) : (true)));
        }
      }
      crm_o.release();
      tmp_n.release();
    }
    
    //System.out.println(crm.depiction());
    //System.out.println(tmp.depiction());
    
    decode_lowlevel(raw, tmp, true);
    
    tmp.release();
  }
  
  public static void decode_remote_source(BufferCentral central, Buffer.xI raw, Buffer.oB crm_o)
  {
    Buffer.xZ tmp = central.acquireZ();
    
    {
      Buffer.nZ tmp_n = tmp.prepend();
      {
        int rem = crm_o.remaining();
        
        while (rem-- > 0) {
          int pak = crm_o.rB();
          
          tmp_n.aZ(((((pak >> 7) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 6) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 5) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 4) & 1) == 0) ? (false) : (true)));
          
          tmp_n.aZ(((((pak >> 3) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 2) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak >> 1) & 1) == 0) ? (false) : (true)));
          tmp_n.aZ(((((pak     ) & 1) == 0) ? (false) : (true)));
        }
      }
      tmp_n.release();
    }
    
    decode_lowlevel(raw, tmp, true);
    
    tmp.release();
  }
}

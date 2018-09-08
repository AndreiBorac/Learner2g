/***
 * Buffer.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.buff;

import zs42.mass.*;

import static zs42.mass.Static.*;

public class Buffer extends RootObject
{
  static int enAsciiZ(byte[] tarr, boolean val)
  {
    tarr[0] = ((byte)(val ? 'T' : 'F'));
    return 1;
  }
  
  static int enAsciiB(byte[] tarr, byte  val) { return enAsciiJ(tarr, val); }
  static int enAsciiS(byte[] tarr, short val) { return enAsciiJ(tarr, val); }
  static int enAsciiI(byte[] tarr, int   val) { return enAsciiJ(tarr, val); }
  
  static final byte[] n2e63 = (new byte[] { ((byte)('-')), ((byte)('9')), ((byte)('2')), ((byte)('2')), ((byte)('3')), ((byte)('3')), ((byte)('7')), ((byte)('2')), ((byte)('0')), ((byte)('3')), ((byte)('6')), ((byte)('8')), ((byte)('5')), ((byte)('4')), ((byte)('7')), ((byte)('7')), ((byte)('5')), ((byte)('8')), ((byte)('0')), ((byte)('8')) });
  
  static int enAsciiJ(byte[] tarr, long val)
  {
    int tlen = 0;
    
    // handle -(2^63) whose sign you cannot simply flip
    {
      if (val == 0x8000000000000000L) {
        for (int i = 0; i < n2e63.length; i++) {
          tarr[i] = n2e63[i];
        }
        
        return n2e63.length;
      }
    }
    
    // handle 0 whose magnitude you cannot simply represent with the
    // empty string
    {
      if (val == 0) {
        tarr[0] = ((byte)('0'));
        return 1;
      }
    }
    
    // base conversion
    {
      boolean sign = false;
      
      if (val < 0) {
        sign = true;
        val = -val;
      }
      
      while (val > 0) {
        tarr[tlen++] = ((byte)('0' + (val % 10)));
        val /= 10;
      }
      
      if (sign) {
        tarr[tlen++] = ((byte)('-'));
      }
    }
    
    // reversi
    {
      for (int L = 0, R = (tlen - 1); L < R; L++, R--) {
        byte vL = tarr[L];
        byte vR = tarr[R];
        
        tarr[L] = vR;
        tarr[R] = vL;
      }
    }
    
    return tlen;
  }
  
  static int enAsciiF(byte[] tarr, float val) { return enAsciiD(tarr, val); }
  
  static int enAsciiD(byte[] tarr, double val)
  {
    tarr[0] = 'f';
    tarr[1] = 'p';
    return 2;
  }
  
  static abstract class Worker extends RootObject
  {
    final Throwable trace = (BufferCentral.trace ? (new Throwable()) : null);
    
    final BufferCentral central;
    Worker(BufferCentral central) { this.central = central; }
    void reset() { }
    Worker initialize() { return this; }
  }
  
  // !!! for 3 ?N ?E ?D Z boolean 1 B byte 1 S short 2 I int 4 J long 8 F float 4 D double 8 // !!!
  public static class Page?N extends Worker
  {
    int ref; // reference count
    int len; // allocated length (last written byte plus one)
    
    final ?E[] buf;
    
    Page?N(BufferCentral central) { super(central); buf = (new ?E[central.PAGE_SIZE / ?D]); }
    void reset() { super.reset(); ref = 0; len = 0; }
    Page?N initialize() { super.initialize(); return this; }
  }
  // !!!
  
  // !!! for 1 ?N Z B S I J F D // !!!
  public static class Link?N extends Worker
  {
    int    off;
    int    lim;
    Page?N bak;
    Link?N lnk;
    
    Link?N(BufferCentral central) { super(central); }
    void reset() { super.reset(); off = 0; lim = 0; bak = null; lnk = null; }
    Link?N initialize(int off, int lim, Page?N bak, Link?N lnk) { super.initialize(); this.off = off; this.lim = lim; this.bak = bak; this.lnk = lnk; return bh_initialize(); }
    
    private Link?N bh_initialize()
    {
      bak.ref++;
      return this;
    }
  }
  // !!!
  
  // !!! for 2 ?N ?E Z boolean B byte S short I int J long F float D double // !!!
  /***
   * "insert cursor".
   ***/
  public static class n?N extends Worker
  {
    Link?N pre; // "previous" link before the insertion point
    Page?N bak; // page we're current writing to (not necessarily pre.bak (!))
    
    int off; // offset into current page we started writing at
    int lim; // limit where we stopped writing into current page
    
    n?N(BufferCentral central) { super(central); }
    void reset() { super.reset(); pre = null; bak = null; }
    n?N initialize(Link?N pre, Page?N bak) { super.initialize(); this.pre = pre; this.bak = bak; return bh_initialize(); }
    
    /***
     * (private) completes initialization by reserving the remainder
     * of the page for backing. returns <code>this</code>.
     ***/
    private n?N bh_initialize()
    {
      bak.ref++;
      this.off = this.lim = bak.len;
      bak.len = bak.buf.length;
      return this;
    }
    
    /***
     * (private) flushes the current insert region to the containing
     * buffer.
     ***/
    private void flush()
    {
      if (lim > off) {
        pre = pre.lnk = central.pool_link?N.acquire().initialize(off, lim, bak, pre.lnk);
      }
      
      // release remainer if not all consumed
      bak.len = lim;
      
      // we no longer reference this backing page
      if ((--bak.ref) == 0) {
        central.pool_page?N.release(bak);
      }
      
      bak = null;
    }
    
    /***
     * (private) flushes and allocates a fresh backing page.
     ***/
    private void avanti()
    {
      flush();
      bak = central.pool_page?N.acquire().initialize();
      bh_initialize();
    }
    
    /***
     * EXPERIMENTAL: try to flush written data.
     ***/
    public void commit()
    {
      if (lim > off) {
        pre = pre.lnk = central.pool_link?N.acquire().initialize(off, lim, bak, pre.lnk);
        off = lim;
      }
    }
    
    /***
     * appends the given value to the insert region. returns
     * <code>this</code>.
     ***/
    public n?N a?N(?E val)
    {
      if (lim < bak.buf.length) {
        bak.buf[lim++] = val;
        return this;
      } else {
        return bh_a?N(val);
      }
    }
    
    private n?N bh_a?N(?E val)
    {
      avanti();
      return a?N(val);
    }
    
    /***
     * appends the contents of the given slice to the insert
     * region. returns <code>this</code>.
     ***/
    public n?N a?N_all(r?N inp)
    {
      // TODO: optimize
      
      for (int pos = inp.off(); pos < inp.lim(); pos++) {
        a?N(inp.get(pos));
      }
      
      return this;
    }
    
    /***
     * allowed call sequence for DMA is
     * <code>(dma_enter)((dma_bak|dma_off|dma_lim)*)(dma_leave)</code>,
     * with other intervening calls to other methods. DMA should write
     * to the array returned by <code>dma_bak</code> between indices
     * <code>dma_off</code> (inclusive) and <code>dma_lim</code>
     * (exclusive). then DMA should pass the number of elements
     * written to <code>dma_leave</code>.
     ***/
    public void dma_enter()
    {
      if (lim > (bak.buf.length >> 1)) {
        avanti();
      }
    }
    
    /***
     * see <code>dma_enter</code>.
     ***/
    public ?E[] dma_bak()
    {
      return bak.buf;
    }
    
    /***
     * see <code>dma_enter</code>.
     ***/
    public int dma_off()
    {
      return lim;
    }
    
    /***
     * see <code>dma_enter</code>.
     ***/
    public int dma_lim()
    {
      return bak.buf.length;
    }
    
    /***
     * see <code>dma_enter</code>.
     ***/
    public void dma_leave(int amt)
    {
      lim += amt;
    }
    
    /***
     * flushes the current insert region to the containing buffer and
     * releases the insert cursor (<code>this</code>).
     ***/
    public void release()
    {
      flush();
      central.pool_n?N.release(this);
    }
  }
  // !!!
  
  // !!! for 2 ?N ?E Z boolean B byte S short I int J long F float D double // !!!
  /***
   * "overwrite cursor".
   ***/
  public static class o?N extends Worker
  {
    Link?N cur; // current link
    Page?N bak; // page we're currently reading/writing to/from
    
    int off; // offset into the current page at which we're reading/writing
    int lim; // limit in the current page where the current link's region ends
    
    o?N(BufferCentral central) { super(central); }
    void reset() { super.reset(); cur = null; bak = null; }
    o?N initialize(Link?N cur) { super.initialize(); this.cur = cur; return bh_initialize(); }
    
    /***
     * (private) completes initialization by initializing cached
     * values.
     ***/
    private o?N bh_initialize()
    {
      bak = cur.bak;
      off = cur.off;
      lim = cur.lim;
      return this;
    }
    
    /***
     * equiv <code>(remaining() &gt; 0)</code>, except very efficient.
     ***/
    public boolean available()
    {
      if (off < lim) {
        return true;
      } else {
        Link?N lap = cur;
        
        do {
          lap = lap.lnk;
        } while ((lap != null) && (!(lap.off < lap.lim)));
        
        return (lap != null);
      }
    }
    
    /***
     * returns the number of elements remaining before
     * end-of-stream. this method is somewhat expensive, as it
     * involves iterating through all the remaining links. where
     * sensibly possible, the return value should be cached.
     ***/
    public int remaining()
    {
      int rem = (lim - off);
      
      Link?N ptr = cur;
      
      while ((ptr = ptr.lnk) != null) {
        rem += (ptr.lim - ptr.off);
      }
      
      return rem;
    }
    
    /***
     * returns an insertion cursor at the current position, and
     * advances the current position past the insertion point. values
     * written to the insert cursor appear after the value most
     * recently read and before the value to be read next.
     ***/
    public n?N insert()
    {
      Link?N tail;
      
      // determine tail that will follow insertion point -- must be non-null
      {
        if ((off == lim) && (cur.lnk != null)) {
          tail = cur.lnk;
        } else {
          cur.lim = off; // cut
          tail = central.pool_link?N.acquire().initialize(off, lim, cur.bak, cur.lnk); // paste
        }
      }
      
      // allocate new empty link for insertion point which links tail
      // the insertion point will always stay (off=0, lim=0) so backing page does not really matter
      Link?N insp = central.pool_link?N.acquire().initialize(0, 0, cur.bak, tail);
      
      // finally, cur links insertion point
      cur.lnk = insp;
      
      // jump past the insertion point
      cur = tail;
      bh_initialize();
      
      // return new append cursor to append to insertion point
      return central.pool_n?N.acquire().initialize(insp, insp.bak /* insp.bak = "old" cur.bak will only be used if it has space */);
    }
   
    /***
     * (package-private) advance to the next link. obviously, this
     * method should not be called unless the current link has been
     * fully consumed.
     ***/
    void avanti()
    {
      do {
        cur = cur.lnk;
        bh_initialize();
      } while (!(off < lim));
    }
    
    /***
     * skip the given number of elements.
     ***/
    public void skip(int amt)
    {
      while ((lim - off) < amt) {
        amt -= (lim - off);
        avanti();
      }
      
      off += amt;
    }
    
    /***
     * read and advance the cursor.
     ***/
    public ?E r?N()
    {
      if (off < lim) {
        return bak.buf[off++];
      } else {
        return bh_r?N();
      }
    }
    
    private ?E bh_r?N()
    {
      avanti();
      return r?N();
    }
    
    /***
     * write and advance the cursor. the value under the cursor is
     * overwritten. this method never extends the buffer (see
     * <code>n?E</code> for that functionality). returns
     * <code>this</code>.
     ***/
    public o?N w?N(?E val)
    {
      if (off < lim) {
        bak.buf[off++] = val;
        return this;
      } else {
        return bh_w?N(val);
      }
    }
    
    private o?N bh_w?N(?E val)
    {
      avanti();
      return w?N(val);
    }
    
    /***
     * read without advancing the cursor.
     ***/
    public ?E peek?N()
    {
      if (off < lim) {
        return bak.buf[off];
      } else {
        return bh_peek?N();
      }
    }
    
    public ?E bh_peek?N()
    {
      avanti();
      return peek?N();
    }
    
    /***
     * read and write ("rotate") and advance the cursor.
     ***/
    public ?E rot?N(?E val)
    {
      if (off < lim) {
        ?E old = bak.buf[off];
        bak.buf[off] = val;
        off++;
        return old;
      } else {
        return bh_rot?N(val);
      }
    }
    
    private ?E bh_rot?N(?E val)
    {
      avanti();
      return rot?N(val);
    }
    
    /***
     * allowed call sequence for DMA is
     * <code>(dma_enter)((dma_bak|dma_off|dma_lim)*)(dma_leave)</code>,
     * with other intervening calls to other methods. DMA should read
     * from the array returned by <code>dma_bak</code> between indices
     * <code>dma_off</code> (inclusive) and <code>dma_lim</code>
     * (exclusive). then DMA should pass the number of elements read
     * to <code>dma_leave</code>.
     ***/
    public void dma_enter()
    {
      if (off < lim) {
        // nothing to do
      } else {
        bh_dma_enter();
      }
    }
    
    private void bh_dma_enter()
    {
      avanti();
    }
    
    public ?E[] dma_bak()
    {
      return bak.buf;
    }
    
    public int dma_off()
    {
      return off;
    }
    
    public int dma_lim()
    {
      return lim;
    }
    
    public void dma_leave(int amt)
    {
      off += amt;
    }
    
    /***
     * returns this object to the pool.
     ***/
    public void release()
    {
      central.pool_o?N.release(this);
    }
  }
  // !!!
  
  // !!! for 2 ?N ?E Z boolean B byte S short I int J long F float D double // !!!
  /***
   * buffer for primitives of type <code>?E</code>.
   ***/
  public static class x?N extends Worker
  {
    Link?N fst;
    
    x?N(BufferCentral central) { super(central); }
    void reset() { super.reset(); fst = null; }
    x?N initialize() { super.initialize(); return bh_initialize(); }
    
    private x?N bh_initialize()
    {
      fst = central.pool_link?N.acquire().initialize(0, 0, central.pool_page?N.acquire().initialize(), null);
      return this;
    }
    
    public int linkCount()
    {
      int ctr = 0;
      
      Link?N ptr = fst;
      
      while (ptr != null) {
        ctr++;
        ptr = ptr.lnk;
      }
      
      return ctr;
    }
    
    public int pageCount()
    {
      int ctr = 0;
      
      Link?N ptr = fst;
      Page?N bak = null;
      
      while (ptr != null) {
        if (ptr.bak != bak) {
          ctr++;
          bak = ptr.bak;
        }
        
        ptr = ptr.lnk;
      }
      
      return ctr;
    }
    
    /***
     * removes redundant links from the page chain. this should only
     * be invoked when there are no open cursors.
     ***/
    public void pack()
    {
      Link?N ptr = fst;
      
      while (ptr.lnk != null) {
        if (ptr.lnk.off == ptr.lnk.lim) {
          if (--(ptr.lnk.bak.ref) == 0) {
            central.pool_page?N.release(ptr.lnk.bak);
          }
          
          Link?N old = ptr.lnk;
          ptr.lnk = old.lnk;
          central.pool_link?N.release(old);
        } else {
          ptr = ptr.lnk;
        }
      }
      
      if (fst.off == fst.lim) {
        if (fst.lnk != null) { /* can't leave just "null" */
          if (--(fst.bak.ref) == 0) {
            central.pool_page?N.release(fst.bak);
          }
          
          Link?N old = fst;
          fst = old.lnk;
          central.pool_link?N.release(old);
        }
      }
    }
    
    /***
     * equiv <code>(o = iterate(), retv = o.remaining(), o.release(),
     * retv)</code>, except possibly more efficient by not allocating
     * an intermediate overwrite cursor.
     ***/
    public int length()
    {
      // for now, do it the slow way
      o?N o = iterate();
      int len = o.remaining();
      o.release();
      return len;
    }
    
    /***
     * returns a new overwrite cursor, initially positioned at the
     * head of this buffer.
     ***/
    public o?N iterate()
    {
      return central.pool_o?N.acquire().initialize(fst);
    }
    
    /***
     * equiv <code>(o = iterate(), a = o.insert(), o.release(),
     * a)</code>, except possibly more efficient by not allocating an
     * intermediate overwrite cursor.
     ***/
    public n?N prepend()
    {
      // for now, do it the slow way
      o?N o = iterate();
      n?N n = o.insert();
      o.release();
      return n;
    }
    
    /***
     * equiv <code>(o = iterate(), o.skip(o.remaining()), a =
     * o.insert(), o.release(), a)</code>, except possibly more
     * efficient by not allocating an intermediate overwrite
     * cursor. UPDATE: not equivalent to the code just
     * mentioned. always seeks to the very last link before inserting,
     * even in the presence of zero-length links. this makes multiple
     * simultaneous <code>append()</code> regions work.
     ***/
    public n?N append()
    {
      // for now, do it the slow way
      o?N o = iterate();
      
      // fast-forward to the last link
      {
        while (o.cur.lnk != null) {
          o.cur = o.cur.lnk;
        }
        
        o.bh_initialize();
      }
      
      // skip if there is anything to skip
      o.skip(o.remaining());
      
      // finally ...
      n?N n = o.insert();
      o.release();
      return n;
    }
    
    /***
     * returns a new primitive ?E array containing the elements in
     * this buffer.
     ***/
    public ?E[] toNewArray?N()
    {
      o?N o = iterate();
      
      ?E[] out = (new ?E[o.remaining()]);
      
      for (int i = 0; i < out.length; i++) {
        out[i] = o.r?N();
      }
      
      o.release();
      
      return out;
    }
    
    /***
     * releases this buffer and all backing pages with no other
     * referents to the pool.
     ***/
    public void release()
    {
      while (fst != null) {
        if ((--fst.bak.ref) == 0) {
          central.pool_page?N.release(fst.bak);
        }
        
        Link?N pre = fst;
        fst = fst.lnk;
        central.pool_link?N.release(pre);
      }
      
      central.pool_x?N.release(this);
    }
    
    static final cB depict_head = cB.newCopyOf(_("((Buffer ?E)"));
    static final cB depict_tail = cB.newCopyOf(_(")"));
    static final cB depict_int1 = cB.newCopyOf(_(" [off="));
    static final cB depict_int2 = cB.newCopyOf(_(", lim="));
    static final cB depict_int3 = cB.newCopyOf(_(", bak@"));
    static final cB depict_int4 = cB.newCopyOf(_(", bak.ref="));
    static final cB depict_int5 = cB.newCopyOf(_("]"));
    
    /***
     * writes an ASCII depiction of <code>this</code> to the given
     * <code>nB</code>.
     ***/
    public void depict(nB out, boolean content)
    {
      out.aB_all(depict_head);
      
      // structure
      {
        Link?N ptr = fst;
        
        while (ptr != null) {
          out.aB_all(zs42.mass.sB.joinC(depict_int1, Static.enAsciiI(ptr.off), depict_int2, Static.enAsciiI(ptr.lim), depict_int3, Static.enAsciiI(ptr.bak.hashCode()), depict_int4, Static.enAsciiI(ptr.bak.ref), depict_int5));
          ptr = ptr.lnk;
        }
      }
      
      // content
      if (content) {
        o?N o = iterate();
        
        int    tlen = 0;
        byte[] tarr = (new byte[32]);
        
        {
          int rem = o.remaining();
          
          while (rem-- > 0) {
            out.aB(((byte)(' ')));
            
            ?E val = o.r?N();
            tlen = enAscii?N(tarr, val);
            
            for (int i = 0; i < tlen; i++) {
              out.aB(tarr[i]);
            }
          }
        }
        
        o.release();
      }
      
      out.aB_all(depict_tail);
    }
    
    /***
     * writes an ASCII depiction of <code>this</code> to the given
     * <code>nB</code>.
     ***/
    public void depict(nB out)
    {
      depict(out, true);
    }
    
    /***
     * returns a byte buffer that is an ASCII depiction of
     * <code>this</code>.
     ***/
    public xB depict(boolean content)
    {
      xB out = central.acquireB();
      nB app = out.prepend();
      depict(app, content);
      app.release();
      return out;
    }
    
    /***
     * returns a byte buffer that is an ASCII depiction of
     * <code>this</code>.
     ***/
    public xB depict()
    {
      return depict(true);
    }
    
    /***
     * returns a String containing only ASCII characters that is a
     * depiction of <code>this</code>.
     ***/
    public String depiction(boolean content)
    {
      xB depiction = depict(content);
      String retv = (new String(depiction.toNewArrayB()));
      depiction.release();
      return retv;
    }
    
    /***
     * returns a String containing only ASCII characters that is a
     * depiction of <code>this</code>.
     ***/
    public String depiction()
    {
      return depiction(true);
    }
  }
  // !!!
  
  // !!! for 1 ?X s-=8;dst.aB((byte)(x>>s)) // !!!
  static void encode_be_Z(nB dst, oZ src) { dst.aB(src.rZ() ? ((byte)(1)) : ((byte)(0))); }
  static void encode_be_B(nB dst, oB src) { int  x = src.rB(); int s =  8; ?X; }
  static void encode_be_S(nB dst, oS src) { int  x = src.rS(); int s = 16; ?X; ?X; }
  static void encode_be_I(nB dst, oI src) { int  x = src.rI(); int s = 32; ?X; ?X; ?X; ?X; }
  static void encode_be_J(nB dst, oJ src) { long x = src.rJ(); int s = 64; ?X; ?X; ?X; ?X; ?X; ?X; ?X; ?X; }
  static void encode_be_F(nB dst, oF src) { throw null; }
  static void encode_be_D(nB dst, oD src) { throw null; }
  // !!!
  
  // !!! for 1 ?X x<<=8;x|=(src.rB()&0xFF); // !!!
  static void decode_be_Z(nZ dst, oB src) { dst.aZ((src.rB() == 0) ? (false) : (true)); }
  static void decode_be_B(nB dst, oB src) { dst.aB(src.rB()); }
  static void decode_be_S(nS dst, oB src) { short x = 0; ?X; ?X;                         dst.aS(x); }
  static void decode_be_I(nI dst, oB src) { int   x = 0; ?X; ?X; ?X; ?X;                 dst.aI(x); }
  static void decode_be_J(nJ dst, oB src) { long  x = 0; ?X; ?X; ?X; ?X; ?X; ?X; ?X; ?X; dst.aJ(x); }
  static void decode_be_F(nF dst, oB src) { throw null; }
  static void decode_be_D(nD dst, oB src) { throw null; }
  // !!!
  
  // !!! for 2 ?N ?E Z boolean B byte S short I int J long F float D double // !!!
  /***
   * container class for utility methods involving buffers/cursors for
   * primitives of type <code>?E</code>.
   ***/
  public static class s?N extends RootObject
  {
    /***
     * (private) non-instantiable.
     ***/
    private s?N() { throw null; }
    
    /***
     * converts <code>len</code> elements from <code>src</code> to
     * <code>dst</code>, advancing <code>src</code> by <code>X *
     * len</code> bytes, where <code>X</code> is the length of the
     * representation of one element.
     ***/
    public static void decode(n?N dst, oB src, int len, boolean be)
    {
      // nothing fancy for now
      if (be) {
        while (len-- > 0) {
          decode_be_?N(dst, src);
        }
      } else {
        throw null; // el not supported yet
      }
    }
    
    /***
     * equiv <code>recv(dst, src, len, true)</code>.
     ***/
    public static void decode(n?N dst, oB src, int len)
    {
      decode(dst, src, len, true);
    }
    
    /***
     * converts <code>len</code> elements from <code>src</code> to
     * <code>dst</code>, writing <code>X * len</code> bytes to
     * <code>dst</code>, where <code>X</code> is the length of the
     * representation of one element.
     ***/
    public static void encode(nB dst, o?N src, int len, boolean be)
    {
      // nothing fancy for now
      if (be) {
        while (len-- > 0) {
          encode_be_?N(dst, src);
        }
      } else {
        throw null; // el not supported yet
      }
    }
    
    /***
     * equiv <code>encode(dst, src, len, true)</code>.
     ***/
    public static void encode(nB dst, o?N src, int len)
    {
      encode(dst, src, len, true);
    }
    
    /***
     * copies <code>len</code> elements from <code>src</code> to
     * <code>dst</code>, advancing <code>src</code> by
     * <code>len</code> elements.
     ***/
    public static void copy(n?N dst, o?N src, int len)
    {
      // nothing fancy for now
      while (len-- > 0) {
        dst.a?N(src.r?N());
      }
    }
    
    /***
     * equiv <code>(n = dst.append(); copy(n, src, src.remaining());
     * n.release())</code>, except possibly more efficient.
     ***/
    public static void copy(x?N dst, o?N src)
    {
      Buffer.n?N dst_n = dst.append();
      copy(dst_n, src, src.remaining());
      dst_n.release();
    }
    
    public static void copy(x?N dst, x?N src, int len)
    {
      Buffer.o?N src_o = src.iterate();
      Buffer.n?N dst_n = dst.append();
      copy(dst_n, src_o, len);
      dst_n.release();
      src_o.release();
    }
    
    public static void copy(x?N dst, x?N src)
    {
      copy(dst, src, src.length());
    }
    
    /***
     * copies <code>lim - off</code> elements from <code>src +
     * off</code> to <code>dst</code>, advancing <code>dst</code> by
     * <code>lim - off</code> elements.
     ***/
    public static void copy(n?N dst, ?E[] src, int off, int lim)
    {
      // nothing fancy for now
      while (lim > off) {
        dst.a?N(src[off++]);
      }
    }
    
    /***
     * equiv <code>(n = dst.append(); copy(n, src, 0, src.length);
     * n.release())</code>, except possibly more efficient.
     ***/
    public static void copy(x?N dst, ?E[] src)
    {
      Buffer.n?N dst_n = dst.append();
      copy(dst_n, src, 0, src.length);
      dst_n.release();
    }
    
    /***
     * aliases <code>len</code> elements from <code>src</code> to
     * <code>dst</code>, advancing <code>src</code> by
     * <code>len</code> elements.
     ***/
    public static void alias(n?N dst, o?N src, int len)
    {
      if (dst.central != src.central) throw null;
      
      dst.avanti();
      
      // alias full source links
      {
        int amt;
        
        while ((amt = (src.lim - src.off)) < len) {
          bh_alias(dst, src, amt);
          src.avanti();
          len -= amt;
        }
      }
      
      // alias remainder
      {
        if (len > 0) {
          bh_alias(dst, src, len);
        }
      }
    }
    
    public static void alias(x?N dst, x?N src, int len)
    {
      Buffer.o?N src_o = src.iterate();
      Buffer.n?N dst_n = dst.append();
      alias(dst_n, src_o, len);
      dst_n.release();
      src_o.release();
    }
    
    public static void alias(x?N dst, x?N src)
    {
      alias(dst, src, src.length());
    }
    
    private static void bh_alias(n?N dst, o?N src, int amt)
    {
      dst.pre = dst.pre.lnk = dst.central.pool_link?N.acquire().initialize(src.off, (src.off += amt), src.bak, dst.pre.lnk);
    }
  }
  // !!!
}

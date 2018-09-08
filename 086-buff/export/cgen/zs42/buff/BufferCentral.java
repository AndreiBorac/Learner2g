/***
 * BufferCentral.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.buff;

import java.io.*;
import java.util.*;

import zs42.mass.*;

import static zs42.mass.Static.*;

public class BufferCentral extends RootObject
{
  /* ENTER FOR DEBUGGING ONLY */
  static final boolean trace = false;
  /* LEAVE FOR DEBUGGING ONLY */
  
  static class Pool<E> extends RootObject
  {
    final wL<E>          stack = wL.newEmpty();
    final F0<E>          creat;
    final F1<Nothing, E> reset;
    
    int counter = 0;
    
    Pool(F0<E> creat, F1<Nothing, E> reset)
    {
      this.creat = creat;
      this.reset = reset;
    }
    
    E acquire()
    {
      int len = stack.len();
      
      if (len > 0) {
        E out = stack.get(len - 1);
        stack.sln(len - 1);
        if (trace) { reset.invoke(out); } /* reset "again" to fill in correct stack trace */
        return out;
      } else {
        counter++;
        E out = creat.invoke();
        reset.invoke(out);
        return out;
      }
    }
    
    void release(E inp)
    {
      reset.invoke(inp);
      stack.app(inp);
    }
    
    void cleanup()
    {
      stack.sln(0);
    }
  }
  
  // !!! for 1 ?N Z B S I J F D // !!!
  final Pool<Buffer.Page?N> pool_page?N = (new Pool<Buffer.Page?N>((new F0<Buffer.Page?N>() { public Buffer.Page?N invoke() { return (new Buffer.Page?N(BufferCentral.this)); } }), (new F1<Nothing, Buffer.Page?N>() { public Nothing invoke(Buffer.Page?N elm) { elm.reset(); return null; } })));
  // !!!
  
  // !!! for 1 ?N Z B S I J F D // !!!
  final Pool<Buffer.Link?N> pool_link?N = (new Pool<Buffer.Link?N>((new F0<Buffer.Link?N>() { public Buffer.Link?N invoke() { return (new Buffer.Link?N(BufferCentral.this)); } }), (new F1<Nothing, Buffer.Link?N>() { public Nothing invoke(Buffer.Link?N elm) { elm.reset(); return null; } })));
  // !!!
  
  // !!! for 1 ?N Z B S I J F D // !!!
  final Pool<Buffer.o?N> pool_o?N = (new Pool<Buffer.o?N>((new F0<Buffer.o?N>() { public Buffer.o?N invoke() { Buffer.o?N retv = (new Buffer.o?N(BufferCentral.this)); if (trace) { all_o?N.add(retv); }; return retv; } }), (new F1<Nothing, Buffer.o?N>() { public Nothing invoke(Buffer.o?N elm) { elm.reset(); if (trace) { elm.trace.fillInStackTrace(); }; return null; } })));
  final Pool<Buffer.n?N> pool_n?N = (new Pool<Buffer.n?N>((new F0<Buffer.n?N>() { public Buffer.n?N invoke() { Buffer.n?N retv = (new Buffer.n?N(BufferCentral.this)); if (trace) { all_n?N.add(retv); }; return retv; } }), (new F1<Nothing, Buffer.n?N>() { public Nothing invoke(Buffer.n?N elm) { elm.reset(); if (trace) { elm.trace.fillInStackTrace(); }; return null; } })));
  final Pool<Buffer.x?N> pool_x?N = (new Pool<Buffer.x?N>((new F0<Buffer.x?N>() { public Buffer.x?N invoke() { Buffer.x?N retv = (new Buffer.x?N(BufferCentral.this)); if (trace) { all_x?N.add(retv); }; return retv; } }), (new F1<Nothing, Buffer.x?N>() { public Nothing invoke(Buffer.x?N elm) { elm.reset(); if (trace) { elm.trace.fillInStackTrace(); }; return null; } })));
  // !!!
  
  // !!! for 1 ?N Z B S I J F D // !!!
  final ArrayList<Buffer.o?N> all_o?N = (trace ? (new ArrayList<Buffer.o?N>()) : null);
  final ArrayList<Buffer.n?N> all_n?N = (trace ? (new ArrayList<Buffer.n?N>()) : null);
  final ArrayList<Buffer.x?N> all_x?N = (trace ? (new ArrayList<Buffer.x?N>()) : null);
  // !!!
  
  final int PAGE_SIZE;
  
  public BufferCentral(int page_size_bits)
  {
    if (!((3 <= page_size_bits) && (page_size_bits <= 30))) throw null;
    PAGE_SIZE = (1 << page_size_bits);
  }
  
  public BufferCentral()
  {
    this(12);
  }
  
  // !!! for 1 ?N Z B S I J F D // !!!
  /***
   * returns a new empty buffer from the buffer pool, creating a new
   * buffer if the buffer pool is exhausted.
   ***/
  public Buffer.x?N acquire?N()
  {
    return pool_x?N.acquire().initialize();
  }
  // !!!
  
  public void release()
  {
    // !!! for 1 ?N Z B S I J F D // !!!
    pool_page?N.cleanup();
    pool_link?N.cleanup();
    pool_o?N.cleanup();
    pool_n?N.cleanup();
    pool_x?N.cleanup();
    // !!!
  }
  
  private static final cB depiction_header = _("BufferCentral statistics follow.\n- low fraction means release() leak (higher is better).\n- denominator equals peak usage (lower is better).\n");
  
  private static final cB[] depictions =
    (new cB[]
      {
        // !!! for 1 ?N Z B S I J F D // !!!
        _("P?N="), _("/"),
        _(" L?N="), _("/"),
        _(" o?N="), _("/"),
        _(" n?N="), _("/"),
        _(" x?N="), _("/"),
        // !!!
        null
      });
  
  private int[] snapshot()
  {
    final int[] stats =
      (new int[]
        {
          // !!! for 1 ?N Z B S I J F D // !!!
          pool_page?N.stack.len(), pool_page?N.counter,
          pool_link?N.stack.len(), pool_link?N.counter,
          
          pool_o?N.stack.len(), pool_o?N.counter,
          pool_n?N.stack.len(), pool_n?N.counter,
          pool_x?N.stack.len(), pool_x?N.counter,
          // !!!
          0, 0
        });
    
    //for (int i = 0; i < stats.length; i += 2) {
    //stats[i] = stats[i+1] - stats[i];
    //}
    
    return stats;
  }
  
  private void depict0(Buffer.nB out, int[] snapshot)
  {
    out.aB_all(depiction_header);
    
    for (int i = 0; i < (depictions.length - 1); i++) {
      out.aB_all(depictions[i]);
      out.aB_all(enAsciiI(snapshot[i]));
      if ((i % 10) == 9) out.aB(((byte)('\n')));
    }
  }
  
  /***
   * appends an ASCII string representing this buffer central's
   * statistics (outstanding and total allocated objects of each kind)
   * to the given cursor.
   ***/
  public void depict(Buffer.nB out)
  {
    int[] snapshot = snapshot();
    depict0(out, snapshot);
  }
  
  /***
   * returns a byte buffer that is an ASCII string representing this
   * buffer central's statistics (outstanding and total allocated
   * objects of each kind). this information may be useful in
   * discovering and debugging memory leaks.
   ***/
  public Buffer.xB depict()
  {
    if (trace) {
      // !!! for 1 ?N Z B S I J F D // !!!
      for (Buffer.o?N elm : all_o?N) {
        // elm.trace.printStackTrace(System.out);
      }
      // !!!
      
      // !!! for 1 ?N Z B S I J F D // !!!
      for (Buffer.n?N elm : all_n?N) {
        // elm.trace.printStackTrace(System.out);
      }
      // !!!
      
      {
        try {
          PrintStream out = (new PrintStream(new BufferedOutputStream(new FileOutputStream("/tmp/BufferCentral.cap"))));
          
          // !!! for 1 ?N Z B S I J F D // !!!
          {
            int sum = 0;
            
            for (Buffer.x?N elm : all_x?N) {
              if (elm.fst != null) {
                int len = elm.length();
                
                if (len > 1000) {
                  out.println("Buffer.x?N elm=@" + elm.hashCode() + ", len=" + elm.length() + ", linkCount=" + elm.linkCount() + ", pageCount=" + elm.pageCount() + " allocated from " + elm.trace.getStackTrace()[4].toString());
                  out.println(elm.depiction(false));
                }
                
                sum += len;
              }
            }
            
            out.println("Buffer.x?N sum=" + sum);
          }
          // !!!
          
          out.close();
        } catch (Exception e) {
          // ignored
        }
      }
    }
    
    int[] snapshot = snapshot();
    Buffer.xB buffer = acquireB();
    Buffer.nB n = buffer.prepend();
    depict0(n, snapshot);
    n.release();
    return buffer;
  }
  
  public String depiction()
  {
    Buffer.xB depiction = depict();
    String retv = (new String(depiction.toNewArrayB()));
    depiction.release();
    return retv;
  }
}

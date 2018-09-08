/***
 * ZeroDelayQueue.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import static zs42.parts.Static.cast_unchecked;

public final class ZeroDelayQueue<E>
{
  final ConsObjectCache cons_cache;
  
  volatile Cons<Object, Object> wrln;
  volatile Cons<Object, Object> rdln;
  volatile Cons<Object, Object> gcln;
  
  volatile int wrnr;
  volatile int rdnr;
  /*    */ int gcnr;
  
  private String inspect(Cons<Object, Object> zzln)
  {
    StringBuilder out = (new StringBuilder());
    
    int limiter = 0;
    
    while ((zzln != null) && (limiter < 20)) {
      out.append(("" + zzln + "(" + zzln.getA() + ", " + zzln.getB() + ") "));
      zzln = cast_unchecked(((Cons<Object, Object>)(null)), (zzln.getA()));
      limiter++;
    }
    
    return out.toString();
  }
  
  private String inspect()
  {
    StringBuilder out = (new StringBuilder());
    
    out.append("wrnr=" + wrnr + ", rdnr=" + rdnr + ", gcnr=" + gcnr + "\n");
    out.append("wrln=" + inspect(wrln) + "\n");
    out.append("rdln=" + inspect(rdln) + "\n");
    out.append("gcln=" + inspect(gcln) + "\n");
    
    return out.toString();
  }
  
  public ZeroDelayQueue(ConsObjectCache input_cache)
  {
    this.cons_cache = input_cache;
    
    wrln = cons_cache.obtain(((Object)(null)), ((Object)(null)));
    rdln = wrln;
    gcln = rdln;
    
    wrnr = 0;
    rdnr = 0;
    gcnr = 0;
  }
  
  public ZeroDelayQueue()
  {
    this((new ConsObjectCache()));
  }
  
  public int size()
  {
    return (wrnr - rdnr);
  }
  
  public void push(E element)
  {
    //synchronized (ZeroDelayQueue.class) {
    //Log.log("" + this + ".push(" + element + ")");
    
    // garbage collect
    {
      final int rdnr_cached = rdnr;
      
      Cons<Object, Object> gcln_cached = gcln;
      int                  gcnr_cached = gcnr;
      {
        if (gcnr_cached != rdnr_cached) {
          while (gcnr_cached != rdnr_cached) {
            Cons<Object, Object> zzln_cached = gcln_cached;
            gcln_cached = cast_unchecked(((Cons<Object, Object>)(null)), (gcln_cached.getA()));
            cons_cache.refund(zzln_cached);
            gcnr_cached++;
          }
          
          gcln = gcln_cached;
          gcnr = gcnr_cached;
        }
      }
    }
    
    // append new link
    {
      Cons<Object, Object> wrln_cached = wrln;
      int                  wrnr_cached = wrnr;
      {
        if (true) {
          {
            Cons<Object, Object> zzln_cached = cons_cache.obtain(((Object)(null)), ((Object)(element)));
            wrln_cached.setA(zzln_cached);
            wrln_cached = zzln_cached;
            wrnr_cached++;
          }
          
          wrln = wrln_cached;
          wrnr = wrnr_cached;
        }
      }
    }
    
    //}
  }
  
  public E pull()
  {
    //synchronized (ZeroDelayQueue.class) {
    
    final int wrnr_cached = wrnr;
    
    Cons<Object, Object> rdln_cached = rdln;
    int                  rdnr_cached = rdnr;
    {
      if (rdnr_cached != wrnr_cached) {
        E retv = null;
        
        rdln_cached = cast_unchecked(((Cons<Object, Object>)(null)), (rdln_cached.getA()));
        retv = cast_unchecked(((E)(null)), (rdln_cached.rotB(null)));
        rdnr_cached++;
        
        rdln = rdln_cached;
        rdnr = rdnr_cached;
        
        //Log.log("" + this + ".pull() => " + retv);
        return retv;
      } else {
        //Log.log("" + this + ".pull() => null");
        return null;
      }
    }
    
    //}
  }
}

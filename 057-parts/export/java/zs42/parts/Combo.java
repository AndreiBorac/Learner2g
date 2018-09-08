/***
 * Combo.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import static zs42.parts.Static.cast_unchecked;

public final class Combo
{
  public static final int CAPACITY = 8;
  
  private static final Blazon<Void> BLAZON_LONGVALUE = (new Blazon<Void>());
  
  private boolean writing = true;
  
  private int wrptr = 0;
  private int rdptr = 0;
  
  private final Object [] signatures = (new Object [CAPACITY]);
  private final Object [] references = (new Object [CAPACITY]);
  private final long   [] longvalues = (new long   [CAPACITY]);
  
  public void reset()
  {
    for (int i = 0; i < wrptr; i++) {
      references[i] = null;
      longvalues[i] = 0;
    }
    
    writing = true;
    
    wrptr = 0;
    rdptr = 0;
  }
  
  private void push_common()
  {
    if (!(writing)) {
      throw (new RuntimeException("356b3b81d840be1fedecd8da213d3563"));
    }
    
    if (!(wrptr < CAPACITY)) {
      throw (new RuntimeException("0fb2ead061f943335fa89579cfed0701"));
    }
  }
  
  public <T> void push(Blazon<T> signature, T reference)
  {
    push_common();
    
    signatures[wrptr] = signature;
    references[wrptr] = reference;
    
    wrptr++;
  }
  
  public void push_long(long value)
  {
    push_common();
    
    signatures[wrptr] = BLAZON_LONGVALUE;
    longvalues[wrptr] = value;
    
    wrptr++;
  }
  
  public void tack()
  {
    if (!(writing)) {
      throw (new RuntimeException("2dec6d63d821ec5474b36d8b90d987b4"));
    }
    
    writing = false;
  }
  
  private void pull_common()
  {
    if (!(!writing)) {
      throw (new RuntimeException("5ec7db515f99c004869e26da62d6fa6f"));
    }
    
    if (!(rdptr < wrptr)) {
      throw (new RuntimeException("71388846bce21f6aed08be1e90bd0dea"));
    }
  }
  
  public <T> T pull(Blazon<T> signature)
  {
    pull_common();
    
    if (!(signatures[rdptr] == signature)) {
      throw (new RuntimeException("7f38788f46a547dcbb78e83720df03b9"));
    }
    
    return cast_unchecked(((T)(null)), references[rdptr++]);
  }
  
  public long pull_long()
  {
    pull_common();
    
    if (!(signatures[rdptr] == BLAZON_LONGVALUE)) {
      throw (new RuntimeException("5f5e22151a13e407a5ba294c4cc1b798"));
    }
    
    return longvalues[rdptr++];
  }
  
  public void turn()
  {
    if (!(!writing)) {
      throw (new RuntimeException("803edc3b0385e817fea5e043eab5c472"));
    }
    
    if (!(rdptr == wrptr)) {
      throw (new RuntimeException("744c568b2dff1141c9a94ccfa927b845"));
    }
    
    reset();
  }
  
  private static final ThreadLocal<ObjectCache<Combo>> tls =
    (new ThreadLocal<ObjectCache<Combo>>()
     {
       protected ObjectCache<Combo> initialValue()
       {
         return
         (new ObjectCache<Combo>
          ((new F0<Combo>()
            {
              public Combo invoke()
              {
                return (new Combo());
              }
            })));
       }
     });
  
  public static Combo obtain()
  {
    return tls.get().obtain();
  }
  
  public static void refund(Combo combo)
  {
    combo.reset();
    tls.get().refund(combo);
  }
}

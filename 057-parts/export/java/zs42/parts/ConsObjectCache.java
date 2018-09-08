/***
 * ConsObjectCache.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public final class ConsObjectCache extends Cons.Root
{
  public <A, B> Cons<A, B> obtain(A a, B b)
  {
    return Cons.obtain1(this, a, b);
  }
  
  public void refund(Cons<?, ?> cons)
  {
    Cons.refund1(this, cons);
  }
}

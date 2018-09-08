/***
 * Cons.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import static zs42.parts.Static.cast_unchecked;

public final class Cons<A, B>
{
  public static class Root
  {
    private Cons<Object, Object> cons;
  }
  
  private A a;
  private B b;
  
  /***
   * (package-private) constructor.
   ***/
  Cons()
  {
    // nothing to do
  }
  
  public A getA()
  {
    return a;
  }
  
  public B getB()
  {
    return b;
  }
  
  public void setA(A a)
  {
    this.a = a;
  }
  
  public void setB(B b)
  {
    this.b = b;
  }
  
  public A rotA(A a)
  {
    A p = this.a;
    this.a = a;
    return p;
  }
  
  public B rotB(B b)
  {
    B p = this.b;
    this.b = b;
    return p;
  }
  
  static Cons<Object, Object> obtain0(Root root, Object a, Object b)
  {
    Cons<Object, Object> cons;
    
    if ((cons = root.cons) != null) {
      root.cons = cast_unchecked(((Cons<Object, Object>)(null)), cons.a);
    } else {
      cons = (new Cons<Object, Object>());
    }
    
    cons.a = a;
    cons.b = b;
    
    return cons;
  }
  
  static void refund0(Root root, Cons<Object, Object> cons)
  {
    cons.a = root.cons;
    cons.b = null;
    
    root.cons = cons;
  }
  
  @SuppressWarnings("unchecked")
  static <A, B> Cons<A, B> obtain1(Root root, A a, B b)
  {
    return ((Cons<A, B>)(obtain0(root, a, b)));
  }
  
  @SuppressWarnings("unchecked")
  static void refund1(Root root, Cons<?, ?> cons)
  {
    refund0(root, ((Cons<Object, Object>)(cons)));
  }
  
  private static final ThreadLocal<Root> tlsr = (new ThreadLocal<Root>() { protected Root initialValue() { return (new Root()); } });
  
  static <A, B> Cons<A, B> obtain(A a, B b)
  {
    return obtain1(tlsr.get(), a, b);
  }
  
  static void refund(Cons<?, ?> cons)
  {
    refund1(tlsr.get(), cons);
  }
}

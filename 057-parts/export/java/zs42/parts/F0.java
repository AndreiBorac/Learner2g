/***
 * F0.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public abstract class F0<X>
{
  public abstract X invoke();
  
  public static final F0<Void> NULL = (new F0<Void>() { public Void invoke() { return null; } } );
  public static final F0<Boolean> TRUE = (new F0<Boolean>() { public Boolean invoke() { return Boolean.TRUE; } });
  public static final F0<Boolean> FALSE = (new F0<Boolean>() { public Boolean invoke() { return Boolean.FALSE; } });
}

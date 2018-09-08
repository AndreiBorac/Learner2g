/***
 * PartsStatic.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public final class PartsStatic
{
  public static final F0<Boolean> TRUE = (new F0<Boolean>() { public Boolean invoke() { return Boolean.TRUE; } });
  
  @SuppressWarnings("unchecked")
  public static <DST, SRC> DST cast_unchecked(DST ign, SRC obj)
  {
    return ((DST)(obj));
  }
  
  public static int nextBinaryPower(int inp, int min)
  {
    int out = min;
    
    while (out < inp) {
      out <<= 1;
    }
    
    return out;
  }
  
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private PartsStatic()
  {
    throw null;
  }
}

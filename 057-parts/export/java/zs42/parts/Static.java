/***
 * Static.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public class Static
{
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
  private Static()
  {
    throw null;
  }
}

/***
 * MicroTime.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public final class MicroTime
{
  static final long adjustment;
  
  static
  {
    long value = -1;
    
    for (int i = 0; i < 100; i++) {
      long ms = System.currentTimeMillis();
      long ns = System.nanoTime();
      
      long expected_ns = ms * 1000000;
      
      value = (expected_ns - ns);
    }
    
    if (value == -1) throw null;
    
    adjustment = value;
  }
  
  public static long now()
  {
    return ((System.nanoTime() + adjustment) / 1000);
  }
  
  private MicroTime() { throw null; }
}

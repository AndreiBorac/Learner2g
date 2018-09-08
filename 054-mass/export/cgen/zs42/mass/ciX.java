/***
 * ciX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class ci_N__G_ extends ri_N__G_
{
  _EZO_;
  
  public static final ciZ FALSE = new ciZ(false);
  public static final ciZ TRUE  = new ciZ(true);
  
  _LZO_;
  
  _ENO_;
  
  public static final ci_N_ ZERO = new ci_N_();
  
  _LNO_;
  
  _ELO_;
  
  public static final ci_N_ NULL = new ci_N_();
  
  public static <E> ciL<E> newNull()
  {
    return cast_unchecked(((ciL<E>)(null)), NULL);
  }
  
  _LLO_;
  
  _EZO_;
  
  public static ciZ getSingletonOf(boolean onoff)
  {
    if (onoff) {
      return TRUE;
    } else {
      return FALSE;
    }
  }
  
  _LZO_;
  
  public ci_N_()
  {
    // super();
  }
  
  public ci_N_(_E_ val)
  {
    super(val);
  }
}

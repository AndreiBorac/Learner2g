/***
 * wiX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public class wi_N__G_ extends ri_N__G_
{
  public wi_N_(_E_ val)
  {
    super(val);
  }
  
  public final void put(_E_ val)
  {
    super.val = val;
  }
  
  public final _E_ rot(_E_ val)
  {
    _E_ old = super.val;
    super.val = val;
    return old;
  }
}

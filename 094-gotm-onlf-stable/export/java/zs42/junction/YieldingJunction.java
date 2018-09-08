/***
 * YieldingJunction.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.junction;

public final class YieldingJunction extends Junction
{
  public static final YieldingJunction INSTANCE = new YieldingJunction();
  
  public static final class YieldingWaiter extends Waiter
  {
    static final YieldingWaiter INSTANCE = new YieldingWaiter();
    
    YieldingWaiter()
    {
      // package-private constructor
    }
    
    public boolean waitfor(boolean condition)
    {
      if (condition) {
        return false;
      } else {
        Thread.yield();
        return true;
      }
    }
  }
  
  public YieldingWaiter waiter()
  {
    return YieldingWaiter.INSTANCE;
  }
  
  public void changed()
  {
    // nothing to do
  }
}

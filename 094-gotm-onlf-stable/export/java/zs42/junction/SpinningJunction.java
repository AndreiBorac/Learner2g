/***
 * SpinningJunction.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.junction;

public final class SpinningJunction extends Junction
{
  public static final SpinningJunction INSTANCE = new SpinningJunction();
  
  public static final class SpinningWaiter extends Waiter
  {
    static final SpinningWaiter INSTANCE = new SpinningWaiter();
    
    SpinningWaiter()
    {
      // package-private contructor
    }
    
    public boolean waitfor(boolean condition)
    {
      return !condition;
    }
  }
  
  public SpinningWaiter waiter()
  {
    return SpinningWaiter.INSTANCE;
  }
  
  public void changed()
  {
    // nothing to do
  }
}

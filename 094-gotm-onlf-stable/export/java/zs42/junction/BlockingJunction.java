/***
 * BlockingJunction.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.junction;

public final class BlockingJunction extends Junction
{
  Object monitor = new Object();
  long change = 0;
  
  public static final class BlockingWaiter extends Waiter
  {
    BlockingJunction target;
    long shadow;
    
    BlockingWaiter(BlockingJunction target)
    {
      // package-private constructor
      
      synchronized (target.monitor) {
        this.target = target;
        this.shadow = target.change;
      }
    }
    
    public boolean waitfor(boolean condition)
    {
      if (condition) {
        return false;
      }
      
      synchronized (target.monitor) {
        if (shadow == target.change) {
          try {
            target.monitor.wait();
          } catch (InterruptedException e) {
            // ignored
          }
        }
        
        shadow = target.change;
        return true;
      }
    }
  }
  
  public BlockingWaiter waiter()
  {
    return new BlockingWaiter(this);
  }
  
  public void changed()
  {
    synchronized (monitor) {
      change = change + 1;
      monitor.notify();
    }
  }
}

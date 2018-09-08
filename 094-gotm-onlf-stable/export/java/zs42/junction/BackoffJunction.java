/***
 * BackoffJunction.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.junction;

/*
public final class BackoffJunction extends Junction
{
  volatile long change = 0;
  
  static 
  
  int yield_num;
  int sleep_min;
  int sleep_max;
  
  static final class BackoffWaiter extends Waiter
  {
    BackoffWaiter(BackoffJunction target)
    {
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
  
  public BackoffJunction(int yield_num, )
  {
    
  }
  
  public BackoffJunction()
  {
    
  }
  
  public BackoffWaiter waiter()
  {
    return new BackoffWaiter(this);
  }
  
  public BackoffWaiter waiter()
  {
    
  }
  
  public void changed()
  {
    change = change + 1;
  }
}
*/

/***
 * Junction.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.junction;

public abstract class Junction
{
  public static abstract class Waiter
  {
    public abstract boolean waitfor(boolean condition);
  }
  
  public abstract Waiter waiter();
  public abstract void changed();
}

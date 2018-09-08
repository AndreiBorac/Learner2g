/***
 * Handle.java
 * copyright (c) 2013 by andrei borac
 ***/

package zs42.parts;

public class Handle<T>
{
  T t;
  
  public Handle()
  {
  }
  
  public Handle(T t)
  {
    this.t = t;
  }
  
  public T get()
  {
    return t;
  }
  
  public void put(T t)
  {
    this.t = t;
  }
}

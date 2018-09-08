/***
 * InfiniteArray.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public class InfiniteArray<T>
{
  private final F0<T> creat;
  private final ArrayList<T> inner = (new ArrayList<T>());
  
  public InfiniteArray(F0<T> creat)
  {
    this.creat = creat;
  }
  
  public T get(int idx)
  {
    while (!(idx < inner.size())) {
      inner.add(creat.invoke());
    }
    
    return inner.get(idx);
  }
}

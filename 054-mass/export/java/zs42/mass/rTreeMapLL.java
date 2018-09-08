/***
 * rTreeMapLL.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public abstract class rTreeMapLL<K, V> extends RootObject
{
  final Proxy<K> proxy;
  
  /***
   * (package-private) constructor.
   ***/
  rTreeMapLL(Proxy<K> proxy)
  {
    this.proxy = proxy;
  }
  
  public abstract int size();
  public abstract V get(K key);
  public abstract int entries(K[] arrK, V[] arrV);
  
  public D2<K[], V[]> entries()
  {
    int len = size();
    
    K[] arrK = anewarray_unchecked(((K)(null)), len);
    V[] arrV = anewarray_unchecked(((V)(null)), len);
    
    entries(arrK, arrV);
    
    return new D2<K[], V[]>(arrK, arrV);
  }
  
  public boolean has(K key)
  {
    return (get(key) != null);
  }
}

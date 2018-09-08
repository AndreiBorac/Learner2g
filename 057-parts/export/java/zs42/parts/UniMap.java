/***
 * UniMap.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public class UniMap<K, V>
{
  final F1<V, K> creat;
  final HashMap<K, V> inner = (new HashMap<K, V>());
  
  public UniMap(F1<V, K> creat)
  {
    this.creat = creat;
  }
  
  public V access(K key)
  {
    V val = inner.get(key);
    
    if (val == null) {
      inner.put(key, val = creat.invoke(key));
    }
    
    return val;
  }
}

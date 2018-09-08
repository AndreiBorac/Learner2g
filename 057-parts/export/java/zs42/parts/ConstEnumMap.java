/***
 * ConstEnumMap.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import static zs42.parts.PartsStatic.*;

public final class ConstEnumMap<K extends Enum<K>, V>
{
  final V[] lookup;
  
  ConstEnumMap(K[] values, F1<V, K> mapper)
  {
    lookup = (V[])cast_unchecked(((V[])(null)), (new Object[values.length]));
    
    for (K value : values) {
      lookup[value.ordinal()] = mapper.invoke(value);
    }
  }
  
  V get(K key)
  {
    return lookup[key.ordinal()];
  }
}

/***
 * AsciiTreeMap.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public class AsciiTreeMap<V> extends TreeMap<String, V>
{
  public AsciiTreeMap()
  {
    super
      ((new Comparator<String>()
        {
          public int compare(String L, String R)
          {
            final int zL = L.length();
            final int zR = R.length();
            
            for (int i = 0, l = Math.min(zL, zR); i < l; i++) {
              final int cL = (((int)(L.charAt(i))) & 0xFFFF);
              final int cR = (((int)(R.charAt(i))) & 0xFFFF);
              
              if (cL < cR) return -1;
              if (cL > cR) return +1;
            }
            
            if (zL < zR) return -1;
            if (zL > zR) return +1;
            
            return 0;
          }
        }));
  }
  
  public NavigableMap<String, V> startsWithRange(String prefix)
  {
    return subMap(prefix, false, (prefix + "\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF"), true);
  }
}

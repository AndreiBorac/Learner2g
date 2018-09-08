/***
 * cTreeMapLL.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class cTreeMapLL<K, V> extends rTreeMapLL<K, V>
{
  final int arrL;
  final K[] arrK;
  final V[] arrV;
  
  public static final cTreeMapLL EMPTY = new cTreeMapLL();
  
  /***
   * package-private constructor.
   ***/
  cTreeMapLL()
  {
    super(null);
    arrL = 0;
    arrK = null;
    arrV = null;
  }
  
  /***
   * (package-private) constructor.
   ***/
  cTreeMapLL(Proxy<K> proxy, K[] arrK, V[] arrV)
  {
    super(proxy);
    
    this.arrL = eqI(arrK.length, arrV.length);
    this.arrK = arrK;
    this.arrV = arrV;
  }
  
  public static <K, V> cTreeMapLL<K, V> newEmpty(K ignoredK, V ignoredV)
  {
    return cast_unchecked(((cTreeMapLL<K, V>)(null)), EMPTY);
  }
  
  public static <K, V> cTreeMapLL<K, V> newCopyOf(rTreeMapLL<K, V> source)
  {
    D2<K[], V[]> arrmap = source.entries();
    return new cTreeMapLL<K, V>(source.proxy, arrmap.d1, arrmap.d2);
  }
  
  public static <K, V> cTreeMapLL<K, V> newLinkOfSortedPairs(Proxy<K> proxy, cL<K> arrK, cL<V> arrV)
  {
    return new cTreeMapLL<K, V>(proxy, arrK.arr, arrV.arr);
  }
  
  public static <K, V> cTreeMapLL<K, V> newCopyOfSortedPairs(Proxy<K> proxy, K[] arrK, V[] arrV)
  {
    return newLinkOfSortedPairs(proxy, cL.newCopyOf(arrK), cL.newCopyOf(arrV));
  }
  
  public int size()
  {
    return arrL;
  }
  
  public V get(K key)
  {
    int idx = sL.binarySearch(key, arrK, 0, arrL, proxy);
    
    if (!((0 <= idx) && (idx < arrL))) {
      return null;
    } else {
      if (proxy.compare(key, arrK[idx]) == 0) {
        return arrV[idx];
      } else {
        return null;
      }
    }
  }
  
  public int entries(K[] arrK, V[] arrV)
  {
    for (int i = 0; i < arrL; i++) {
      arrK[i] = this.arrK[i];
    }
    
    for (int i = 0; i < arrL; i++) {
      arrV[i] = this.arrV[i];
    }
    
    return arrL;
  }
}

/***
 * CyclicBufferL.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class CyclicBufferL<E> extends RootObject
{
  static final int DEFAULT_CAPACITY = 16;
  
  int off;
  int lim;
  E[] arr;
  
  /***
   * (package-private) constructor.
   ***/
  CyclicBufferL(int cap)
  {
    off = 0;
    lim = 0;
    arr = anewarray_unchecked(((E)(null)), pwI((cap > 0) ? cap : 1));
  }
  
  public static <E> CyclicBufferL<E> newEmpty(int cap)
  {
    return new CyclicBufferL<E>(cap);
  }
  
  public static <E> CyclicBufferL<E> newEmpty()
  {
    return newEmpty(DEFAULT_CAPACITY);
  }
  
  private void reallocate()
  {
    E[] old = arr;
    
    arr = anewarray_unchecked(((E)(null)), (arr.length << 1));
    
    for (int pos = off; pos != lim; pos++) {
      arr[pos & (arr.length - 1)] = old[pos & (old.length - 1)];
    }
  }
  
  public int len()
  {
    return lim - off;
  }
  
  private int idx(int pos)
  {
    if (!((0 <= pos) && (pos < len()))) throw null;
    return ((pos + off) & (arr.length - 1));
  }
  
  private E rot0(int idx, E val)
  {
    E old = arr[idx];
    arr[idx] = val;
    return old;
  }
  
  public E get(int pos)
  {
    return arr[idx(pos)];
  }
  
  public E rot(int pos, E val)
  {
    return rot0(idx(pos), val);
  }
  
  public void addHead(E elm)
  {
    if (len() >= arr.length) reallocate();
    arr[(--off) & (arr.length - 1)] = elm;
  }
  
  public void addTail(E elm)
  {
    if (len() >= arr.length) reallocate();
    arr[(lim++) & (arr.length - 1)] = elm;
  }
  
  public E rmvHead()
  {
    if (len() <= 0) throw null;
    return rot0(((off++) & (arr.length - 1)), null);
  }
  
  public E rmvTail()
  {
    if (len() <= 0) throw null;
    return rot0(((--lim) & (arr.length - 1)), null);
  }
  
  public boolean empty()
  {
    return (len() == 0);
  }
}

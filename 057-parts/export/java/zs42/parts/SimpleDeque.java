/***
 * SimpleDeque.java (renamed from LocalDeque.java)
 * copyright (c) 2011-2012 by andrei borac
 ***/

package zs42.parts;

public class SimpleDeque<E>
{
  public static final int MINIMUM_CAPACITY =  4;
  public static final int DEFAULT_CAPACITY = 16;
  
  int off = 0;
  int lim = 0;
  Object[] backing = (new Object[DEFAULT_CAPACITY]);
  
  private int aux(int idx) { return (idx & (backing.length - 1)); }
  @SuppressWarnings("unchecked") private E lea(int idx) { return ((E)(backing[aux(idx)])); }
  @SuppressWarnings("unchecked") private E rot(int idx, E inp) { idx = aux(idx); E out = ((E)(backing[idx])); backing[idx] = inp; return out; }
  
  public SimpleDeque()
  {
    // nothing to do
  }
  
  public SimpleDeque(int capacity)
  {
    int allocation = MINIMUM_CAPACITY;
    
    while (allocation < capacity) {
      allocation <<= 1;
    }
    
    backing = (new Object[allocation]);
  }
  
  public boolean isEmpty()
  {
    return (off == lim);
  }
  
  public int size()
  {
    return (lim - off);
  }
  
  private boolean isFull()
  {
    return ((lim - off) == backing.length);
  }
  
  private void grow()
  {
    Object[] previous = backing;
    backing = (new Object[(previous.length << 1)]);
    
    for (int pos = off; pos < lim; pos++) {
      backing[(pos & (backing.length - 1))] = previous[(pos & (previous.length - 1))];
    }
  }
  
  private void test()
  {
    if (!(off < lim)) throw null;
  }
  
  /* random access */
  
  public E get(int idx)
  {
    if (!(idx < size())) throw null;
    return lea(off + idx);
  }
  
  /* action */
  
  public void addFirst(E inp)
  {
    if (!isFull()) {
      rot(--off, inp);
    } else {
      bh_addFirst(inp);
    }
  }
  
  public void bh_addFirst(E inp)
  {
    grow();
    addFirst(inp);
  }
  
  public void addLast(E inp)
  {
    if (!isFull()) {
      rot(lim++, inp);
    } else {
      bh_addLast(inp);
    }
  }
  
  public void bh_addLast(E inp)
  {
    grow();
    addLast(inp);
  }
  
  public E removeFirst()
  {
    test();
    return rot(off++, null);
  }
  
  public E removeLast()
  {
    test();
    return rot(--lim, null);
  }
  
  /* required peek */
  
  public E getFirst()
  {
    test();
    return lea(off);
  }
  
  public E getLast()
  {
    test();
    return lea((lim - 1));
  }
  
  /* possible peek */
  
  public E peekFirst()
  {
    if (!isEmpty()) {
      return lea(off);
    } else {
      return null;
    }
  }
  
  public E peekLast()
  {
    if (!isEmpty()) {
      return lea((lim - 1));
    } else {
      return null;
    }
  }
  
  /* bulk operations */
  
  public void clear()
  {
    while (off < lim) {
      rot(off++, null);
    }
  }
  
  public void addAll(SimpleDeque<E> peer)
  {
    for (int i = 0, l = peer.size(); i < l; i++) {
      addLast(peer.get(i));
    }
  }
}

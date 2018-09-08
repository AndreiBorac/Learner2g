/***
 * ConcurrentCopyableQueue.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import static zs42.parts.PartsStatic.*;

public final class ConcurrentCopyableQueue<E>
{
  static final int MINIMUM_CAPACITY = 1;
  
  int head;
  int tail;
  int mask;
  E[] data;
  
  E refrot(int ipos, E elem)
  {
    E retv = data[ipos];
    data[ipos] = elem;
    return retv;
  }
  
  public ConcurrentCopyableQueue(int desired_capacity)
  {
    int capacity = nextBinaryPower(desired_capacity, MINIMUM_CAPACITY);
    
    // head = 0;
    // tail = 0;
    mask = capacity - 1;
    data = (E[])cast_unchecked(((E[])(null)), (new Object[capacity]));
  }
  
  /***
   * concurrency preconditions: the destination (<code>this</code>)
   * must be stable around the <code>copyStateFrom</code>
   * invokation. however, the source (<code>peer</code>) need not be.
   ***/
  public void copyStateFrom(ConcurrentCopyableQueue<E> peer)
  {
    clear();
    
    int peer_head = peer.head;
    int peer_tail = peer.tail;
    
    if ((peer_tail - peer_head) <= data.length) {
      while (peer_head != peer_tail) {
        enqueue(peer.data[++peer_head]);
      }
    }
  }
  
  public void clear()
  {
    while (tail > head) {
      refrot(++head, null);
    }
  }
  
  public int size()
  {
    return tail - head;
  }
  
  public void enqueue(E elem)
  {
    refrot(tail++, elem);
  }
  
  public E dequeue()
  {
    return refrot(++head, null);
  }
}

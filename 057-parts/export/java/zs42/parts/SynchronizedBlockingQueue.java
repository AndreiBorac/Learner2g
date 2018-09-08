/***
 * SynchronizedBlockingQueue.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.util.concurrent.LinkedBlockingQueue;

public class SynchronizedBlockingQueue<E>
{
  private final LinkedBlockingQueue<E> inner = (new LinkedBlockingQueue<E>());
  
  public int size()
  {
    return inner.size();
  }
  
  public boolean empty()
  {
    return inner.isEmpty();
  }
  
  public E pull()
  {
    try {
      return inner.take();
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public void push(E elm)
  {
    try {
      inner.put(elm);
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
}

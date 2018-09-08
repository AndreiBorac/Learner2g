/***
 * ObjectCache.java
 * copyright (c) 2011-2012 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public final class ObjectCache<E>
{
  private int allocated = 0;
  
  private final F0<E> create;
  private final F1<Void, E> retool;
  
  private final SimpleDeque<E> cache = (new SimpleDeque<E>());
  
  public ObjectCache(F0<E> create, F1<Void, E> retool)
  {
    this.create = create;
    this.retool = retool;
  }
  
  public ObjectCache(F0<E> create)
  {
    this(create, (new F1<Void, E>() { public Void invoke(E object) { return null; } }));
  }
  
  public E obtain()
  {
    if (!cache.isEmpty()) {
      return cache.removeFirst();
    } else {
      allocated++;
      return create.invoke();
    }
  }
  
  public void refund(E object)
  {
    if (object == null) throw null;
    
    retool.invoke(object);
    cache.addLast(object);
  }
  
  public String dumpStatistics()
  {
    return ("allocated = " + allocated + ", cached = " + cache.size());
  }
}

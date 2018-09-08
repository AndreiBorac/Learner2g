/***
 * ThreadLocalObjectCache.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public final class ThreadLocalObjectCache<E>
{
  final ThreadLocal<ObjectCache<E>> tls;
  
  public ThreadLocalObjectCache(final F0<E> create, final F1<Void, E> retool)
  {
    tls =
      (new ThreadLocal<ObjectCache<E>>()
       {
         public ObjectCache<E> initialValue()
         {
           return (new ObjectCache<E>(create, retool));
         }
       });
  }
  
  public ThreadLocalObjectCache(final F0<E> create)
  {
    tls =
      (new ThreadLocal<ObjectCache<E>>()
       {
         public ObjectCache<E> initialValue()
         {
           return (new ObjectCache<E>(create));
         }
       });
  }
  
  public E obtain()
  {
    return tls.get().obtain();
  }
  
  public void refund(E object)
  {
    tls.get().refund(object);
  }
  
  public String dumpStatistics()
  {
    return tls.get().dumpStatistics();
  }
}

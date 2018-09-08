/***
 * ByteArrayCache.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

public class ByteArrayCache
{
  private final UniMap<Integer, ObjectCache<byte[]>> cache =
    ((new UniMap<Integer, ObjectCache<byte[]>>
      ((new F1<ObjectCache<byte[]>, Integer>()
        {
          public ObjectCache<byte[]> invoke(final Integer length)
          {
            return
              ((new ObjectCache<byte[]>
                ((new F0<byte[]>()
                  {
                    public byte[] invoke()
                    {
                      return (new byte[length]);
                    }
                  }))));
          }
        }))));
  
  public byte[] obtain(int length)
  {
    return cache.access(length).obtain();
  }
  
  public void refund(byte[] buffer)
  {
    cache.access(buffer.length).refund(buffer);
  }
}

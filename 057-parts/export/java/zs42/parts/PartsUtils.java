/***
 * PartsUtils.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public final class PartsUtils
{
  public static String arr2str(byte[] arr)
  {
    char[] chr = (new char[arr.length]);
    
    for (int i = 0; i < chr.length; i++) {
      chr[i] = ((char)(arr[i] & 0xFF));
    }
    
    return (new String(chr));
  }
  
  public static byte[] str2arr(String str)
  {
    byte[] arr = (new byte[str.length()]);
    
    for (int i = 0; i < arr.length; i++) {
      arr[i] = ((byte)(str.charAt(i)));
    }
    
    return arr;
  }
  
  public static byte[] extract(byte[] buf, int off, int lim)
  {
    byte[] out = (new byte[(lim - off)]);
    
    int pos = 0;
    
    while (off < lim) {
      out[pos++] = buf[off++];
    }
    
    return out;
  }
  
  public static boolean starts_with(byte[] buf, int off, int lim, byte[] pat)
  {
    if ((lim - off) < pat.length) return false;
    
    for (int pos = 0; pos < pat.length; pos++) {
      if (buf[off++] != pat[pos]) {
        return false;
      }
    }
    
    return true;
  }
  
  /***
   * returns the lowest index in <code>buf</code> at which a match of
   * <code>pat</code> starts, or <code>-1</code> if there is no match.
   ***/
  public static int locate(byte[] buf, int off, int lim, byte[] pat)
  {
    if (pat.length == 0) return off;
    
    byte end = pat[pat.length - 1];
    
    for (int pos = (off + (pat.length - 1)); pos < lim; pos++) {
      if (buf[pos] == end) {
        boolean success = true;
        
        for (int j = 1; j < pat.length; j++) {
          if (buf[pos - j] != pat[(pat.length - 1) - j]) {
            success = false;
            break;
          }
        }
        
        if (success) {
          return (pos - (pat.length - 1));
        }
      }
    }
    
    return -1;
  }
  
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private PartsUtils()
  {
    throw null;
  }
}

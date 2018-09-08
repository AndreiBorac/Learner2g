/***
 * HexStr.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

public class HexStr
{
  static char nib2hex(int nib)
  {
    if (nib < 10) {
      return ((char)('0' + nib));
    } else {
      return ((char)('a' + (nib - 10)));
    }
  }
  
  static int hex2nib(char hex)
  {
    if (('0' <= hex) && (hex <= '9')) return (hex - '0' + 00);
    if (('A' <= hex) && (hex <= 'F')) return (hex - 'A' + 10);
    if (('a' <= hex) && (hex <= 'f')) return (hex - 'a' + 10);
    throw null;
  }
  
  static final String[] b2h;
  
  static
  {
    b2h = (new String[(1 << Byte.SIZE)]);
    
    for (int i = 0; i < b2h.length; i++) {
      b2h[i] = ("" + nib2hex((i >> 4) & 0x0F) + nib2hex(i & 0x0F));
    }
  }
  
  public static String bin2hex(byte[] bin)
  {
    StringBuilder hex = (new StringBuilder());
    
    for (int i = 0; i < bin.length; i++) {
      hex.append(b2h[(((int)(bin[i])) & 0xFF)]);
    }
    
    return hex.toString();
  }
  
  public static byte[] hex2bin(String hex)
  {
    byte[] bin = (new byte[(hex.length() >> 1)]);
    
    for (int src = 0, dst = 0; dst < bin.length; dst++) {
      bin[dst] = ((byte)((hex2nib(hex.charAt(src++)) << 4) | (hex2nib(hex.charAt(src++)) << 0)));
    }
    
    return bin;
  }
}

/***
 * HexCodec.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public class HexCodec extends RootObject
{
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private HexCodec()
  {
    throw null;
  }
  
  private static final byte[] bin2hex = new byte[]
    {
      ((byte)('0')), ((byte)('1')), ((byte)('2')), ((byte)('3')),
      ((byte)('4')), ((byte)('5')), ((byte)('6')), ((byte)('7')),
      ((byte)('8')), ((byte)('9')), ((byte)('a')), ((byte)('b')),
      ((byte)('c')), ((byte)('d')), ((byte)('e')), ((byte)('f'))
    };
  
  private static int hex2bin(int chr)
  {
    if (('0' <= chr) && (chr <= '9')) return (chr - '0'     );
    if (('A' <= chr) && (chr <= 'F')) return (chr - 'A' + 10);
    if (('a' <= chr) && (chr <= 'f')) return (chr - 'a' + 10);
    throw null;
  }
  
  public static void bin2hex(byte[] out, int out_off, byte[] inp, int inp_off, int inp_lim)
  {
    while (inp_off < inp_lim) {
      out[out_off++] = bin2hex[(inp[inp_off] >> 4) & 0xF];
      out[out_off++] = bin2hex[(inp[inp_off]     ) & 0xF];
      inp_off++;
    }
  }
  
  public static void hex2bin(byte[] out, int out_off, byte[] inp, int inp_off, int inp_lim)
  {
    if (((inp_lim - inp_off) & 1) != 0) {
      throw null;
    }
    
    while (inp_off < inp_lim) {
      out[out_off++] =
        ((byte)
         ((hex2bin(inp[inp_off++]) << 4) |
          (hex2bin(inp[inp_off++])     )));
    }
  }
  
  public static cB bin2hexC(rB inp)
  {
    byte[] out = (new byte[inp.len() << 1]);
    bin2hex(out, 0, inp.arr, inp.off(), inp.lim());
    return (new cB()).iniLinkOfUnchecked(out, 0, out.length);
  }
  
  public static vB bin2hexV(rB inp)
  {
    byte[] out = (new byte[inp.len() << 1]);
    bin2hex(out, 0, inp.arr, inp.off(), inp.lim());
    return (new vB()).iniLinkOfUnchecked(out, 0, out.length);
  }
  
  public static cB hex2binC(rB inp)
  {
    byte[] out = (new byte[inp.len() >> 1]);
    hex2bin(out, 0, inp.arr, inp.off(), inp.lim());
    return (new cB()).iniLinkOfUnchecked(out, 0, out.length);
  }
  
  public static vB hex2binV(rB inp)
  {
    byte[] out = (new byte[inp.len() >> 1]);
    hex2bin(out, 0, inp.arr, inp.off(), inp.lim());
    return (new vB()).iniLinkOfUnchecked(out, 0, out.length);
  }
}

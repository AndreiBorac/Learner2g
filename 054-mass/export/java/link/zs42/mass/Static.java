/***
 * Static.java
 * copyright (c) 2011 by andrei borac
 ***/

package link.zs42.mass;

public class Static extends RootObject
{
  private final static boolean trace;
  
  static {
    boolean retv;
    
    try {
      retv = Boolean.parseBoolean(System.getProperty("link.zs42.mass.Static.trace"));
    } catch (SecurityException e) {
      // probably running as applet; do not trace in that case
      retv = false;
    }
    
    trace = retv;
  }
  
  @SuppressWarnings("unchecked")
  public static <DST, SRC> DST cast_unchecked(DST ign, SRC obj)
  {
    return ((DST)(obj));
  }
  
  @SuppressWarnings("unchecked")
  public static <DST, SRC> DST[] cast_unchecked_array(DST ign, SRC obj)
  {
    return ((DST[])(obj));
  }
  
  public static Object[] anewarray(int len)
  {
    return new Object[len];
  }
  
  public static Object aload(Object arr, int idx)
  {
    return ((Object[])(arr))[idx];
  }
  
  public static void astore(Object arr, int idx, Object val)
  {
    ((Object[])(arr))[idx] = val;
  }
  
  public static Object[] join0()
  {
    return new Object[] { };
  }
  
  public static Object[] join1(Object o1)
  {
    return new Object[] { o1 };
  }
  
  public static Object[] join2(Object o1, Object o2)
  {
    return new Object[] { o1, o2 };
  }
  
  public static Object[] join3(Object o1, Object o2, Object o3)
  {
    return new Object[] { o1, o2, o3 };
  }
  
  public static Object[] join4(Object o1, Object o2, Object o3, Object o4)
  {
    return new Object[] { o1, o2, o3, o4 };
  }
  
  public static Object[] join5(Object o1, Object o2, Object o3, Object o4, Object o5)
  {
    return new Object[] { o1, o2, o3, o4, o5 };
  }
  
  public static Object[] join6(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6 };
  }
  
  public static Object[] join7(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7 };
  }
  
  public static Object[] join8(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7, o8 };
  }
  
  public static Object[] join9(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7, o8, o9 };
  }
  
  public static Object[] joinA(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7, o8, o9, oA };
  }
  
  public static Object[] joinB(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA, Object oB)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7, o8, o9, oA, oB };
  }
  
  public static Object[] joinC(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA, Object oB, Object oC)
  {
    return new Object[] { o1, o2, o3, o4, o5, o6, o7, o8, o9, oA, oB, oC };
  }
  
  public static byte[] _s2b(String val)
  {
    char[] inp = val.toCharArray();
    byte[] out = new byte[inp.length];
    
    for (int i = 0; i < out.length; i++) {
      out[i] = ((byte)(inp[i]));
    }
    
    return out;
  }
  
  public static String _b2s(byte[] inp)
  {
    char[] out = new char[inp.length];
    
    for (int i = 0; i < out.length; i++) {
      out[i] = ((char)(inp[i]));
    }
    
    return new String(out);
  }
  
  public static RuntimeException error()
  {
    throw new RuntimeException();
  }
  
  public static int floatToRawIntBits(float val)
  {
    return Float.floatToRawIntBits(val);
  }
  
  public static long doubleToRawLongBits(double val)
  {
    return Double.doubleToRawLongBits(val);
  }
  
  public static byte[] enAsciiZ(boolean val)
  {
    return Boolean.toString(val).getBytes();
  }
  
  public static boolean deAsciiZ(byte[] str)
  {
    return Boolean.parseBoolean(new String(str));
  }
  
  public static byte[] enAsciiB(byte val)
  {
    return Byte.toString(val).getBytes();
  }
  
  public static byte deAsciiB(byte[] str)
  {
    return Byte.parseByte(new String(str));
  }
  
  public static byte[] enAsciiS(short val)
  {
    return Short.toString(val).getBytes();
  }
  
  public static short deAsciiS(byte[] str)
  {
    return Short.parseShort(new String(str));
  }
  
  public static byte[] enAsciiI(int val)
  {
    return Integer.toString(val).getBytes();
  }
  
  public static int deAsciiI(byte[] str)
  {
    return Integer.parseInt(new String(str));
  }
  
  public static byte[] enAsciiJ(long val)
  {
    return Long.toString(val).getBytes();
  }
  
  public static long deAsciiJ(byte[] str)
  {
    return Long.parseLong(new String(str));
  }
  
  public static byte[] enAsciiF(float val)
  {
    return Float.toString(val).getBytes();
  }
  
  public static float deAsciiF(byte[] str)
  {
    return Float.parseFloat(new String(str));
  }
  
  public static byte[] enAsciiD(double val)
  {
    return Double.toString(val).getBytes();
  }
  
  public static double deAsciiD(byte[] str)
  {
    return Double.parseDouble(new String(str));
  }
  
  public static void trace(byte[] word)
  {
    if (trace) {
      System.err.print(_b2s(word));
    }
  }
  
  public static void trace(String word)
  {
    if (trace) {
      System.err.print(word);
    }
  }
  
  public static void traceln(byte[] line)
  {
    if (trace) {
      System.err.println(_b2s(line));
    }
  }
  
  public static void traceln(String line)
  {
    if (trace) {
      System.err.println(line);
    }
  }
  
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private Static()
  {
    throw null;
  }
}

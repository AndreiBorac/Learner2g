/***
 * Static.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

public class Static extends RootObject
{
  public static final int  valminI = 0x80000000;
  public static final int  valmaxI = 0x7FFFFFFF;
  public static final long valminJ = 0x8000000000000000L;
  public static final long valmaxJ = 0x7FFFFFFFFFFFFFFFL;
  
  public static <DST, SRC> DST cast_unchecked(DST ign, SRC obj)
  {
    return link.zs42.mass.Static.cast_unchecked(ign, obj);
  }
  
  public static <DST, SRC> DST[] cast_unchecked_array(DST ign, SRC obj)
  {
    return link.zs42.mass.Static.cast_unchecked_array(ign, obj);
  }
  
  public static Object[] anewarray(int len)
  {
    return link.zs42.mass.Static.anewarray(len);
  }
  
  public static <DST> DST[] anewarray_unchecked(DST ign, int len)
  {
    return cast_unchecked_array(ign, anewarray(len));
  }
  
  public static Object aload(Object arr, int idx)
  {
    return link.zs42.mass.Static.aload(arr, idx);
  }
  
  public static <DST> DST aload_unchecked(DST ign, Object[] arr, int idx)
  {
    return cast_unchecked(ign, link.zs42.mass.Static.aload(arr, idx));
  }
  
  public static <DST> DST[] aload_unchecked_array(DST ign, Object[] arr, int idx)
  {
    return cast_unchecked_array(ign, link.zs42.mass.Static.aload(arr, idx));
  }
  
  public static void astore(Object arr, int idx, Object val)
  {
    link.zs42.mass.Static.astore(arr, idx, val);
  }
  
  public static <E> E nullbomb(E obj)
  {
    if (obj == null) throw null;
    return obj;
  }
  
  public static void checkbounds(int off, int lim, int len)
  {
    if (!((0 <= off) && (off <= lim) && (lim <= len))) throw null;
  }
  
  public static int eqI(int A, int B)
  {
    if (A != B) throw null;
    return A;
  }
  
  public static long eqJ(long A, long B)
  {
    if (A != B) throw null;
    return A;
  }
  
  public static int asI(long x)
  {
    if (!((valminI <= x) && (x <= valmaxI))) throw null;
    return ((int)(x));
  }
  
  public static Object[] join0()
  {
    return link.zs42.mass.Static.join0();
  }
  
  public static Object[] join1(Object o1)
  {
    return link.zs42.mass.Static.join1(o1);
  }
  
  public static Object[] join2(Object o1, Object o2)
  {
    return link.zs42.mass.Static.join2(o1, o2);
  }
  
  public static Object[] join3(Object o1, Object o2, Object o3)
  {
    return link.zs42.mass.Static.join3(o1, o2, o3);
  }
  
  public static Object[] join4(Object o1, Object o2, Object o3, Object o4)
  {
    return link.zs42.mass.Static.join4(o1, o2, o3, o4);
  }
  
  public static Object[] join5(Object o1, Object o2, Object o3, Object o4, Object o5)
  {
    return link.zs42.mass.Static.join5(o1, o2, o3, o4, o5);
  }
  
  public static Object[] join6(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6)
  {
    return link.zs42.mass.Static.join6(o1, o2, o3, o4, o5, o6);
  }
  
  public static Object[] join7(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7)
  {
    return link.zs42.mass.Static.join7(o1, o2, o3, o4, o5, o6, o7);
  }
  
  public static Object[] join8(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8)
  {
    return link.zs42.mass.Static.join8(o1, o2, o3, o4, o5, o6, o7, o8);
  }
  
  public static Object[] join9(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9)
  {
    return link.zs42.mass.Static.join9(o1, o2, o3, o4, o5, o6, o7, o8, o9);
  }
  
  public static Object[] joinA(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA)
  {
    return link.zs42.mass.Static.joinA(o1, o2, o3, o4, o5, o6, o7, o8, o9, oA);
  }
  
  public static Object[] joinB(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA, Object oB)
  {
    return link.zs42.mass.Static.joinB(o1, o2, o3, o4, o5, o6, o7, o8, o9, oA, oB);
  }
  
  public static Object[] joinC(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object oA, Object oB, Object oC)
  {
    return link.zs42.mass.Static.joinC(o1, o2, o3, o4, o5, o6, o7, o8, o9, oA, oB, oC);
  }
  
  /***
   * returns the smallest power of two that is greater than or equal
   * to <code>x</code>. returns <code>0</code> if <code>x</code> is
   * not positive.
   ***/
  public static int pwI(int x)
  {
    if (x <= 0) {
      return 0;
    } else {
      if ((x & (x - 1)) == 0) {
        return x;
      } else {
        x |= x >>  1;
        x |= x >>  2;
        x |= x >>  4;
        x |= x >>  8;
        x |= x >> 16;
        return x + 1;
      }
    }
  }
  
  public static int floatToRawIntBits(float val)
  {
    return link.zs42.mass.Static.floatToRawIntBits(val);
  }
  
  public static long doubleToRawLongBits(double val)
  {
    return link.zs42.mass.Static.doubleToRawLongBits(val);
  }
  
  public static byte[] _s2b(String val)
  {
    return link.zs42.mass.Static._s2b(val);
  }
  
  public static String _b2s(byte[] val)
  {
    return link.zs42.mass.Static._b2s(val);
  }
  
  public static cB _(String val)
  {
    return cB.newCopyOf(_s2b(val));
  }
  
  public static String _(rB val)
  {
    return _b2s(val.all());
  }
  
  public static final cB enAsciiZ_true = _("true");
  public static final cB enAsciiZ_false = _("false");
  
  public static cB enAsciiZ(boolean val)
  {
    return val ? enAsciiZ_true : enAsciiZ_false;
  }
  
  public static boolean deAsciiZ(rB str)
  {
    return link.zs42.mass.Static.deAsciiZ(str.all());
  }
  
  public static cB enAsciiB(byte val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiB(val));
  }
  
  public static byte deAsciiB(rB str)
  {
    return link.zs42.mass.Static.deAsciiB(str.all());
  }
  
  public static cB enAsciiS(short val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiS(val));
  }
  
  public static short deAsciiS(rB str)
  {
    return link.zs42.mass.Static.deAsciiS(str.all());
  }
  
  public static cB enAsciiI(int val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiI(val));
  }
  
  public static int deAsciiI(rB str)
  {
    return link.zs42.mass.Static.deAsciiI(str.all());
  }
  
  public static cB enAsciiJ(long val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiJ(val));
  }
  
  public static long deAsciiJ(rB str)
  {
    return link.zs42.mass.Static.deAsciiJ(str.all());
  }
  
  public static cB enAsciiF(float val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiF(val));
  }
  
  public static float deAsciiF(rB str)
  {
    return link.zs42.mass.Static.deAsciiF(str.all());
  }
  
  public static cB enAsciiD(double val)
  {
    return cB.newCopyOf(link.zs42.mass.Static.enAsciiD(val));
  }
  
  public static double deAsciiD(rB str)
  {
    return link.zs42.mass.Static.deAsciiD(str.all());
  }
  
  public static void trace(rB word)
  {
    link.zs42.mass.Static.trace(word.all());
  }
  
  public static void trace(String word)
  {
    link.zs42.mass.Static.trace(word);
  }
  
  public static void traceln(rB line)
  {
    link.zs42.mass.Static.traceln(line.all());
  }
  
  public static void traceln(String word)
  {
    link.zs42.mass.Static.traceln(word);
  }
  
  /***
   * calculates the allocation size for backing arrays;
   * <code>cap</code> is a capacity hint, interpreted as follows:
   * 
   * <ul>
   * 
   * <li>non-negative <code>cap</code> acts as a lower bound for the
   * size.</li>
   * 
   * <li>negative <code>cap</code> acts as a shift amount for the
   * smallest power of two that is at least
   * <code>lim</code>. <code>cap == -1</code> means use the absolute
   * smallest possible power of two, <code>cap == -2</code> means use
   * twice the size that would be chosen by <code>cap == -1</code>,
   * and so on.</li>
   * 
   * </ul>
   * 
   * this method <i>always</i> returns a non-negative value,
   * regardless how crazy the arguments. however, sufficiently crazy
   * arguments can result in an unchecked exception rather than a
   * return value.
   ***/
  public static int hinterpret(int lim, int cap)
  {
    // adjust cap from a mere hint to the actual allocation size
    if (0 <= cap) {
      // positive capacity -> minimum length
      // promote the capacity so that the elements to be copied actually fit
      if (cap < lim) {
        cap = lim;
      }
    } else {
      // negative capacity -> power-of-two amplifier
      int sig = pwI(lim);
      int amp = (-cap) - 1;
      cap = sig << amp;
      if ((cap < 0) || ((cap >> amp) != sig)) throw null;
    }
    
    return cap;
  }
  
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private Static()
  {
    throw null;
  }
}

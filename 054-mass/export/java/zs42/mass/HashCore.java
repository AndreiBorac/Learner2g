/***
 * HashCore.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public abstract class HashCore<A> extends RootObject
{
  /***
   * (package-private) constructor.
   ***/
  HashCore()
  {
    // nothing to do
  }
  
  public abstract HashCore<A> duplicateState();
  
  public abstract void copyStateFrom(A src);
  
  public abstract long finish();
  public /*    */ int  finish(long[] out) { return finish(out, 0, out.length); }
  public abstract int  finish(long[] out, int off, int lim);
  
  public abstract void acceptZ(boolean val);
  public /*    */ void acceptZ(boolean[] arr) { acceptZ(arr, 0, arr.length); }
  public abstract void acceptZ(boolean[] arr, int off, int lim);
  
  public abstract void acceptB(byte val);
  public /*    */ void acceptB(byte[] arr) { acceptB(arr, 0, arr.length); }
  public abstract void acceptB(byte[] arr, int off, int lim);
  
  public abstract void acceptS(short val);
  public /*    */ void acceptS(short[] arr) { acceptS(arr, 0, arr.length); }
  public abstract void acceptS(short[] arr, int off, int lim);
  
  public abstract void acceptI(int val);
  public /*    */ void acceptI(int[] arr) { acceptI(arr, 0, arr.length); }
  public abstract void acceptI(int[] arr, int off, int lim);
  
  public abstract void acceptJ(long val);
  public /*    */ void acceptJ(long[] arr) { acceptJ(arr, 0, arr.length); }
  public abstract void acceptJ(long[] arr, int off, int lim);
  
  public void acceptF(float val) { acceptI(Float.floatToRawIntBits(val)); }
  public void acceptF(float[] arr) { acceptF(arr, 0, arr.length); }
  public void acceptF(float[] arr, int off, int lim) { for (int pos = off; pos < lim; pos++) { acceptI(floatToRawIntBits(arr[pos])); } }
  
  public void acceptD(double val) { acceptJ(Double.doubleToRawLongBits(val)); }
  public void acceptD(double[] arr) { acceptD(arr, 0, arr.length); }
  public void acceptD(double[] arr, int off, int lim) { for (int pos = off; pos < lim; pos++) { acceptJ(doubleToRawLongBits(arr[pos])); } }
  
  public <E> void acceptL(Proxy<E> proxy, E val) { proxy.compress(this, val); }
  public <E> void acceptL(Proxy<E> proxy, E[] arr) { acceptL(proxy, arr, 0, arr.length); }
  public <E> void acceptL(Proxy<E> proxy, E[] arr, int off, int lim) { proxy.compressMultiple(this, arr, off, lim); }
}

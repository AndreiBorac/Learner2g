/***
 * Proxy.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

public abstract class Proxy<T> extends RootObject
{
  /***
   * returns <code>x&lt;0</code>, <code>x==0</code> or
   * <code>x&gt;0</code> if <code>A</code> is less than, equal to, or
   * greater than <code>B</code>.
   ***/
  public abstract int compare(T a, T b);
  
  /***
   * places in <code>out</code> the results of comparing
   * <code>ref</code> to each object in the specified range of
   * <code>arr</code>.
   ***/
  public void compareMultiple(T ref, T[] arr, int off, int lim, int[] out)
  {
    int ptr = 0;
    
    while (off < lim) {
      out[ptr++] = compare(ref, arr[off++]);
    }
  }
  
  /***
   * returns the first index in the specified range of
   * <code>arr</code> that contains an object greater than or equal to
   * <code>ref</code>, or <code>lim</code> if none is found.
   ***/
  public int compareWhileLT(T ref, T[] arr, int off, int lim)
  {
    while ((off < lim) && (compare(ref, arr[off]) < 0)) off++;
    return off;
  }
  
  /***
   * updates the hash core <code>state</code> with the contents of
   * <code>obj</code>.
   ***/
  public abstract <A> void compress(HashCore<A> state, T obj);
  
  /***
   * updates the hash core <code>state</code> with the contents of
   * each object in the specified range of <code>arr</code>.
   ***/
  public <A> void compressMultiple(HashCore<A> state, T[] arr, int off, int lim)
  {
    while (off < lim) {
      compress(state, arr[off++]);
    }
  }
}

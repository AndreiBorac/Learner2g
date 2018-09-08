/***
 * vX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

/***
 * base class for all writable slices of type _N_ (L = object).
 ***/
public class v_N__G_ extends r_N__G_
{
  /***
   * assigns to an element of the collection by index. requires
   * <code>off &lt;= idx &lt; lim</code>. however, the current
   * implementation performs only the mandatory Java array bounds
   * check, so that accessing a slice out of the bounds succeeds as
   * long as the backing array is not overflowed.
   ***/
  public void put(int idx, _E_ val)
  {
    arr[idx] = val;
  }
  
  /***
   * assigns to an element of the collection by index and returns the
   * previous occupant. requires <code>off &lt;= idx &lt;
   * lim</code>. however, the current implementation performs only the
   * mandatory Java array bounds check, so that accessing a slice out
   * of the bounds succeeds as long as the backing array is not
   * overflowed.
   ***/
  public _E_ rot(int idx, _E_ val)
  {
    _E_ old = arr[idx];
    arr[idx] = val;
    return old;
  }
  
  /***
   * returns a reference to the backing array. modifications to the
   * returned array write through to this slice's contents and vice
   * versa.
   ***/
  public _E_[] arr()
  {
    return arr;
  }
  
  /***
   * equiv <code>s_N_.sort(arr(), off(), lim(), ...)</code>.
   ***/
  public final void sort(_ELO_ Proxy_G_ proxy _LLO_)
  {
    s_N_.sort(arr, off, lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ v_N__G_ newArray(int len)
  {
    v_N__G_ out = (new v_N__G_());
    out.iniEmpty(len, 0);
    out.lim = len;
    return out;
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ v_N__G_ newLinkOf(_E_[] arr)
  {
    return (new v_N__G_()).iniLinkOf(arr, 0, arr.length);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ v_N__G_ newLinkOf(_E_[] arr, int off, int lim)
  {
    return (new v_N__G_()).iniLinkOf(arr, off, lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ v_N__G_ newLinkOf(r_N__G_ src)
  {
    src = src.checkLinkable();
    return (new v_N__G_()).iniLinkOf(src.arr, src.off, src.lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ v_N__G_ newLinkOf(r_N__G_ src, int src_off, int src_lim)
  {
    src = src.checkLinkable();
    return (new v_N__G_()).iniLinkOf(src.arr, src_off, src_lim);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(_E_[] arr)
  {
    return (new v_N__G_()).iniCopyOf(arr, 0, arr.length, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(_E_[] arr, int off, int lim)
  {
    return (new v_N__G_()).iniCopyOf(arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(_E_[] arr, int off, int lim, int gap, int cap)
  {
    return (new v_N__G_()).iniCopyOf(arr, off, lim, gap, cap);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(r_N__G_ src)
  {
    return (new v_N__G_()).iniCopyOf(src.arr, src.off, src.lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(r_N__G_ src, int off, int lim)
  {
    return (new v_N__G_()).iniCopyOf(src.arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ v_N__G_ newCopyOf(r_N__G_ src, int off, int lim, int gap, int cap)
  {
    return (new v_N__G_()).iniCopyOf(src.arr, off, lim, gap, cap);
  }
  
  /***
   * (package-private) empty slice singleton object.
   ***/
  static final v_N__O_ empty = newArray(0);
  
  /***
   * returns a reference to the shared empty slice singleton object.
   ***/
  public static _G_ v_N__G_ getSharedEmpty()
  {
    return cast_unchecked(((v_N__G_)(null)), empty);
  }
  
  /***
   * proxy object/factory.
   ***/
  _EPO_;
  public static final Proxy<v_N_> proxy = cast_unchecked(((Proxy<v_N_>)(null)), r_N_.proxy);
  _LPO_;
  _ELO_;
  public static _G_ Proxy<v_N__G_> newProxyV(final Proxy<E> proxy) { return cast_unchecked(((Proxy<v_N__G_>)(null)), r_N_.newProxy(proxy)); }
  _LLO_;
  
  /***
   * (package-private) nullary constructor.
   ***/
  v_N_()
  {
    // nothing to do
  }
}

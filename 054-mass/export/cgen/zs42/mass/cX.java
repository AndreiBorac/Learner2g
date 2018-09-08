/***
 * cX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

/***
 * base class for all constant slices of type _N_ (L = object).
 ***/
public final class c_N__G_ extends r_N__G_
{
  /***
   * type variable.
   ***/
  _EPO_ public static final c_N_ TYPE = null _LPO_;
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ c_N__G_ newEmpty()
  {
    return (new c_N__G_()).iniEmpty(0, 0);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ c_N__G_ newLinkOf(c_N__G_ src)
  {
    return (new c_N__G_()).iniLinkOf(src.arr, src.off, src.lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ c_N__G_ newLinkOf(c_N__G_ src, int src_off, int src_lim)
  {
    return (new c_N__G_()).iniLinkOf(src.arr, src_off, src_lim);
  }
  
  /***
   * (package-private) initialize as linked to an existing backing
   * array, bypassing compile-time type checks. returns this.
   ***/
  synchronized <T extends r_N__G_> T iniLinkOfUnchecked(Object src, int off, int lim)
  {
    return super.iniLinkOfUnchecked(src, off, lim);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ c_N__G_ newCopyOf(_E_[] arr)
  {
    return (new c_N__G_()).iniCopyOf(arr, 0, arr.length, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ c_N__G_ newCopyOf(_E_[] arr, int off, int lim)
  {
    return (new c_N__G_()).iniCopyOf(arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ c_N__G_ newCopyOf(r_N__G_ src)
  {
    return (new c_N__G_()).iniCopyOf(src.arr, src.off, src.lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ c_N__G_ newCopyOf(r_N__G_ src, int off, int lim)
  {
    return (new c_N__G_()).iniCopyOf(src.arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new constant array wrapper as a copy of an existing
   * array, <b>or</b> returns the given array directly if its runtime
   * type is already <code>c_N_</code>.
   ***/
  public static _G_ c_N__G_ newSafeOf(r_N__G_ src)
  {
    if (src instanceof c_N_) {
      return cast_unchecked(((c_N__G_)(null)), src);
    } else {
      return newCopyOf(src);
    }
  }
  
  /***
   * (package-private) empty slice singleton object.
   ***/
  static final c_N__O_ empty = newEmpty();
  
  /***
   * returns a reference to the shared empty slice singleton object.
   ***/
  public static _G_ c_N__G_ getSharedEmpty()
  {
    return cast_unchecked(((c_N__G_)(null)), empty);
  }
  
  /***
   * proxy object/factory.
   ***/
  _EPO_;
  public static final Proxy<c_N_> proxy = cast_unchecked(((Proxy<c_N_>)(null)), r_N_.proxy);
  _LPO_;
  _ELO_;
  public static _G_ Proxy<c_N__G_> newProxyC(final Proxy<E> proxy) { return cast_unchecked(((Proxy<c_N__G_>)(null)), r_N_.newProxy(proxy)); }
  _LLO_;
  
  /***
   * (package-private) nullary constructor.
   ***/
  c_N_()
  {
    // nothing to do
  }
}

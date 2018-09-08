/***
 * wX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

/***
 * class invariants:
 * <code>off == 0</code>
 * <code>arr.length &gt;= 1</code>
 ***/
public class w_N__G_ extends v_N__G_
{
  /***
   * (package-private) throws an unchecked exception, since w slices
   * are not linkable.
   ***/
  r_N__G_ checkLinkable()
  {
    throw null;
  }
  
  /***
   * returns the capacity of the current backing array. an additional
   * <code>cap() - lim()</code> values may be appended before the
   * backing array will need to be reallocated.
   ***/
  public int cap()
  {
    return arr.length;
  }
  
  /***
   * changes the length of the slice, growing the backing array if
   * necessary. after a successful invocation, <code>lim</code>
   * becomes <code>req</code>. off+ safe.
   ***/
  public void sln(int req)
  {
    if (req < off) {
      throw null;
    }
    
    if (req < lim) {
      while (req < lim) {
        arr[--lim] = _DEFV_;
      }
    }
    
    if (req > lim) {
      if (req > arr.length) {
        resize(req);
      }
      
      lim = req;
    }
  }
  
  /***
   * appends a value to the slice. the backing array is grown if
   * necessary. returns this. off+ safe.
   ***/
  public w_N__G_ app(_E_ val)
  {
    if (lim >= arr.length) {
      resize(lim + 1);
    }
    
    arr[lim++] = val;
    
    return this;
  }
  
  /***
   * appends multiple values to the slice. the backing array is grown
   * if necessary. returns this. off+ safe.
   ***/
  public w_N__G_ app_all(_E_[] src_arr, int src_off, int src_lim)
  {
    checkbounds(src_off, src_lim, src_arr.length);
    
    int src_len = src_lim - src_off;
    resize(len() + src_len);
    
    for (int src_pos = src_off; src_pos < src_lim; src_pos++) {
      arr[lim++] = src_arr[src_pos];
    }
    
    return this;
  }
  
  /***
   * equiv <code>app_all(src.arr(), src.off(), src.lim())</code>.
   ***/
  public w_N__G_ app_all(r_N__G_ src)
  {
    return app_all(src.arr, src.off, src.lim);
  }
  
  /***
   * equiv <code>app_all(src_obj.arr, src_off, src_lim)</code>.
   ***/
  public w_N__G_ app_all(r_N__G_ src_obj, int src_off, int src_lim)
  {
    return app_all(src_obj.arr, src_off, src_lim);
  }
  
  /***
   * inserts <code>val</code> at index <code>idx</code>, shifting one
   * index higher all elements previously at or after index
   * <code>idx</code>. requires <code>((off &lt;= idx) &amp;&amp; (idx
   * &lt;= lim))</code>.
   ***/
  public void ins(int idx, _E_ val)
  {
    if (!((off <= idx) && (idx <= lim))) throw null;
    
    // expand
    app(_DEFV_);
    
    // shuffle
    {
      val = rot(idx++, val);
      
      while (idx < lim) {
        val = rot(idx++, val);
      }
    }
  }
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ w_N__G_ newEmpty()
  {
    return (new w_N__G_()).iniEmpty(DEFAULT_CAPACITY, 0);
  }
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ w_N__G_ newEmpty(int req, int cap)
  {
    return (new w_N__G_()).iniEmpty(req, cap);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ w_N__G_ newLinkOf(_E_[] arr)
  {
    return (new w_N__G_()).iniLinkOf(arr, 0, arr.length);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ w_N__G_ newLinkOf(_E_[] arr, int off, int lim)
  {
    return (new w_N__G_()).iniLinkOf(arr, off, lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ w_N__G_ newLinkOf(r_N__G_ src)
  {
    src = src.checkLinkable();
    return (new w_N__G_()).iniLinkOf(src.arr, src.off, src.lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ w_N__G_ newLinkOf(r_N__G_ src, int src_off, int src_lim)
  {
    src = src.checkLinkable();
    return (new w_N__G_()).iniLinkOf(src.arr, src_off, src_lim);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(_E_[] arr)
  {
    return (new w_N__G_()).iniCopyOf(arr, 0, arr.length, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(_E_[] arr, int off, int lim)
  {
    return (new w_N__G_()).iniCopyOf(arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(_E_[] arr, int off, int lim, int gap, int cap)
  {
    return (new w_N__G_()).iniCopyOf(arr, off, lim, gap, cap);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(r_N__G_ src)
  {
    return (new w_N__G_()).iniCopyOf(src.arr, src.off, src.lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(r_N__G_ src, int off, int lim)
  {
    return (new w_N__G_()).iniCopyOf(src.arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ w_N__G_ newCopyOf(r_N__G_ src, int off, int lim, int gap, int cap)
  {
    return (new w_N__G_()).iniCopyOf(src.arr, off, lim, gap, cap);
  }
  
  /***
   * proxy object/factory.
   ***/
  _EPO_;
  public static final Proxy<w_N_> proxy = cast_unchecked(((Proxy<w_N_>)(null)), r_N_.proxy);
  _LPO_;
  _ELO_;
  public static _G_ Proxy<w_N__G_> newProxyW(final Proxy<E> proxy) { return cast_unchecked(((Proxy<w_N__G_>)(null)), r_N_.newProxy(proxy)); }
  _LLO_;
  
  /***
   * (package-private) default array capacity.
   ***/
  static final int DEFAULT_CAPACITY = 8;
  
  /***
   * (package-private) nullary constructor.
   ***/
  w_N_()
  {
    // nothing to do
  }
  
  /***
   * (package-private) allocates a new backing array if necessary such
   * that capacity becomes at least <code>req</code>. off+ safe.
   ***/
  final void resize(int req)
  {
    if (req > arr.length) {
      int len = arr.length;
      if (len == 0) len = 1;
      do { len <<= 1; } while (len < req);
      
      _E_[] old = arr;
      allocate(len);
      
      for (int pos = off; pos < lim; pos++) {
        arr[pos] = old[pos];
      }
    }
  }
}

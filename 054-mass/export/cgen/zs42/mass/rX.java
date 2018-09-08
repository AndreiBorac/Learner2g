/***
 * rX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

/***
 * base class for all slices of type _N_ (L = object). a slice is an
 * arbitrary continguous range of a "backing" array. <code>r_N_</code>
 * is designed to limit mutation of the values of the slice held by
 * means of an <code>r_N_</code> object. however, the slice values
 * -may- be mutated by other means ... here is what the r/c/w types
 * mean:
 * 
 * <ul>
 * 
 * <li>an <code>r_N_</code> slice is possibly mutable, but <i>not
 * through the r_N_ object</i>. thus an <code>r_N_</code> reference
 * can be passed to an untrusted module with the assurance that the
 * untrusted module will not be able to change the slice
 * values. however, trusted code should not accept <code>r_N_</code>
 * references created by untrusted modules without carefully
 * considering the security impliciations, since the untrusted module
 * may be able to modify the slice values.</li>
 * 
 * <li>a <code>c_N_</code> slice is immutable. its values can never be
 * changed. thus a <code>c_N_</code> reference can be accepted from an
 * untrusted module and sanity-checked once with the assurance that
 * the untrusted module will not be able to change the slice values
 * after sanity-checks.</li>
 * 
 * <li>a <code>v_N_</code> slice is explicity mutable. references to
 * <code>v_N_</code> slices should rarely be shared across trust
 * boundaries. while <code>v_N_</code> slices allow changes to the
 * slice values, they do not allow changes to the slice delimiters
 * (<code>off</code> and <code>lim</code>).</li>
 * 
 * <li>a <code>w_N_</code> slice is also explicity mutable. references
 * to <code>w_N_</code> slices should rarely be shared across trust
 * boundaries. <code>w_N_</code> slices allow changes to the
 * <code>lim</code> delimiter (append to / shrink from : the end of
 * the slice). however, the <code>off</code> delimiter is always zero
 * in <code>w_N_</code> slices.
 * 
 * </ul>
 * 
 * bounds checking policy: each new object initialization must fail if
 * accessing any valid index would fail. additionally, new object
 * initialization must fail if <code>lim()</code> would exceed the
 * length of backing array. technically, this would otherwise be
 * permitted for empty ranges. bounds checks at object creation time
 * are a minimum standard. bounds checks may also be performed at any
 * other reasonable time. note that bounds checks are not required to
 * catch access to sublists that are outside the sublist but within
 * the backing array.
 ***/
public class r_N__G_ extends RootObject
{
  _E_[] arr;
  int   off;
  int   lim;
  
  /***
   * (package-private) returns <code>this</code> if the object is
   * "linkable" or throws an unchecked exception otherwise. only
   * <code>w</code> slices are not "linkable."
   ***/
  r_N__G_ checkLinkable()
  {
    return this;
  }
  
  /***
   * returns the offset of the first element.
   ***/
  public final int off()
  {
    return off;
  }
  
  /***
   * returns the offset of the last element plus one. <code>off() =
   * lim()</code> indicates an empty list.
   ***/
  public final int lim()
  {
    return lim;
  }
  
  /***
   * returns a slice value by index. requires <code>off &lt;= idx &lt;
   * lim</code>. however, the current implementation performs only the
   * mandatory Java array bounds check, so that accessing a slice out
   * of the bounds succeeds as long as the backing array is not
   * accessed out of bounds.
   ***/
  public final _E_ get(int idx)
  {
    return arr[idx];
  }
  
  /***
   * equiv <code>lim() - off()</code>.
   ***/
  public final int len()
  {
    return lim - off;
  }
  
  /***
   * equiv <code>((off() == 0) &amp;&amp; (lim() ==
   * arr.length))</code>. note that <code>arr.length</code> may differ
   * from <code>arr().length</code>, <code>all.length()</code> and
   * <code>dup.length()</code>.
   ***/
  public final boolean packed()
  {
    return ((off == 0) && (lim == arr.length));
  }
  
  /***
   * returns either the backing array directly or a clone of the
   * backing array. that is, it is unspecified whether changes to the
   * returned array affect write through to this slice's contents. in
   * any case, indices <code>[off..lim)</code> of the returned array
   * will correpond to indices <code>[off..lim)</code> of the backing
   * array. objects of type <code>rX</code> (rather than a subclass
   * that overrides this method) will return a clone from this method.
   ***/
  public _E_[] arr()
  {
    _E_[] out = new[lim]new;
    
    for (int pos = off; pos < lim; pos++) {
      out[pos] = arr[pos];
    }
    
    return out;
  }
  
  /***
   * returns either the backing array directly or a clone of the
   * backing array. that is, it is unspecified whether changes to the
   * returned array affect write through to this object's contents. in
   * any case, indices <code>[0..len)</code> of the returned array
   * will correpond to indices <code>[off..lim)</code> of the backing
   * array. objects of type <code>rX</code> (rather than a subclass
   * that overrides this method) will return a clone from this method.
   * 
   * as a performance enhancement, this method is guaranteed to invoke
   * <code>arr()</code> when <code>off</code> is zero. therefore,
   * subclasses that override <code>arr()</code> to return the backing
   * array directly also automatically avoid copying via
   * <code>all()</code> when <code>arr</code> is zero.
   ***/
  public _E_[] all()
  {
    // better chance of avoiding copy by using arr() if offset is zero
    if (off == 0) {
      return arr();
    }
    
    _E_[] out = new[lim]new;
    
    for (int pos = off; pos < lim; pos++) {
      out[pos] = arr[pos];
    }
    
    return out;
  }
  
  /***
   * returns a clone of the backing array. indices
   * <code>[0..len)</code> of the returned array will correpond to
   * indices <code>[off..lim)</code> of the backing array.
   ***/
  public _E_[] dup()
  {
    _E_[] out = new[len()]new;
    
    for (int i = 0, j = off; i < out.length; i++, j++) {
      out[i] = arr[j];
    }
    
    return out;
  }
  
  /***
   * equiv <code>s_N_.startsWith(this, pre, ...)</code>.
   ***/
  public final boolean startsWith(r_N__G_ pre _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return s_N_.startsWith(this, pre _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>s_N__G_.binarySearch(ref, this.arr, this.off,
   * this.lim, ...)</code>.
   ***/
  public final int binarySearch(_E_ ref _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return s_N_.binarySearch(ref, arr, off, lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ r_N__G_ newEmpty()
  {
    return (new r_N__G_()).iniEmpty(0, 0);
  }
  
  /***
   * creates a new empty slice.
   ***/
  public static _G_ r_N__G_ newEmpty(int req, int cap)
  {
    return (new r_N__G_()).iniEmpty(req, cap);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ r_N__G_ newLinkOf(_E_[] arr)
  {
    return (new r_N__G_()).iniLinkOf(arr, 0, arr.length);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ r_N__G_ newLinkOf(_E_[] arr, int off, int lim)
  {
    return (new r_N__G_()).iniLinkOf(arr, off, lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ r_N__G_ newLinkOf(r_N__G_ src)
  {
    src = src.checkLinkable();
    return (new r_N__G_()).iniLinkOf(src.arr, src.off, src.lim);
  }
  
  /***
   * creates a new slice as a link.
   ***/
  public static _G_ r_N__G_ newLinkOf(r_N__G_ src, int src_off, int src_lim)
  {
    src = src.checkLinkable();
    return (new r_N__G_()).iniLinkOf(src.arr, src_off, src_lim);
  }
  
  /***
   * creates a new slice as a link, without checking linkability.
   ***/
  public static _G_ r_N__G_ newVolatileLinkOf(r_N__G_ src)
  {
    return (new r_N__G_()).iniLinkOf(src.arr, src.off, src.lim);
  }
  
  /***
   * creates a new slice as a link, without checking linkability.
   ***/
  public static _G_ r_N__G_ newVolatileLinkOf(r_N__G_ src, int src_off, int src_lim)
  {
    return (new r_N__G_()).iniLinkOf(src.arr, src_off, src_lim);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(_E_[] arr)
  {
    return (new r_N__G_()).iniCopyOf(arr, 0, arr.length, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(_E_[] arr, int off, int lim)
  {
    return (new r_N__G_()).iniCopyOf(arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(_E_[] arr, int off, int lim, int gap, int cap)
  {
    return (new r_N__G_()).iniCopyOf(arr, off, lim, gap, cap);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(r_N__G_ src)
  {
    return (new r_N__G_()).iniCopyOf(src.arr, src.off, src.lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(r_N__G_ src, int off, int lim)
  {
    return (new r_N__G_()).iniCopyOf(src.arr, off, lim, 0, 0);
  }
  
  /***
   * creates a new slice as a copy.
   ***/
  public static _G_ r_N__G_ newCopyOf(r_N__G_ src, int off, int lim, int gap, int cap)
  {
    return (new r_N__G_()).iniCopyOf(src.arr, off, lim, gap, cap);
  }
  
  /***
   * (package-private) empty slice singleton object.
   ***/
  static final r_N__O_ empty = newEmpty();
  
  /***
   * returns a reference to the shared empty slice singleton object.
   ***/
  public static _G_ r_N__G_ getSharedEmpty()
  {
    return cast_unchecked(((r_N__G_)(null)), empty);
  }
  
  /***
   * proxy object/factory.
   ***/
  _EPO_ public static final Proxy<r_N_> proxy = new Proxy<r_N_>() _LPO_ _ELO_ public static _G_ Proxy<r_N__G_> newProxy(final Proxy<E> proxy) { return new Proxy<r_N__G_>() _LLO_
  {
    public int compare(r_N__G_ a, r_N__G_ b)
    {
      return s_N_.compare(a.arr, a.off, a.lim, b.arr, b.off, b.lim _ELO_ , proxy _LLO_);
    }
    
    public <A> void compress(HashCore<A> state, r_N__G_ obj)
    {
      state.accept_N_(_ELO_ proxy, _LLO_ obj.arr, obj.off, obj.lim);
    }
  }; _ELO_ } _LLO_
  
  /***
   * (package-private) nullary constructor.
   ***/
  r_N_()
  {
    // nothing to do
  }
  
  /***
   * (package-private) allocate a backing array, performing bounds
   * checks to ensure that the current array range fits in the backing
   * array.
   ***/
  final void allocate(int len)
  {
    checkbounds(off, lim, len);
    this.arr = new[len]new;
  }
  
  /***
   * (package-private) initialize as empty. returns this as any type.
   ***/
  final <T extends r_N__G_> T iniEmpty(int exp, int cap)
  {
    this.off = 0;
    this.lim = 0;
    allocate(hinterpret(exp, cap));
    return cast_unchecked(((T)(null)), this);
  }
  
  /***
   * (package-private) initialize as linked to an existing backing
   * array. returns this as any type.
   ***/
  final <T extends r_N__G_> T iniLinkOf(_E_[] src, int off, int lim)
  {
    checkbounds(off, lim, src.length);
    
    this.off = off;
    this.lim = lim;
    this.arr = src;
    
    return cast_unchecked(((T)(null)), this);
  }
  
  /***
   * (package-private) initialize as linked to an existing backing
   * array, bypassing compile-time type checks. returns this.
   ***/
  <T extends r_N__G_> T iniLinkOfUnchecked(Object src, int off, int lim)
  {
    return iniLinkOf
      (_EPO_ cast_unchecked(((_E_[])(null)), src), _LPO_
       _ELO_ cast_unchecked_array(((_E_)(null)), src), _LLO_
       off, lim);
  }
  
  /***
   * (package-private) initialize as a copy of a given range of an
   * existing array shifted by the given <code>gap</code> amount with
   * the given capacity hint. returns this.
   ***/
  final <T extends r_N__G_> T iniCopyOf(_E_[] src, int off, int lim, int gap, int cap)
  {
    checkbounds(off, lim, src.length);
    
    this.off = gap;
    this.lim = gap + (lim - off);
    allocate(hinterpret(this.lim, cap));
    
    for (int i = this.off, j = off; i < this.lim; i++, j++) {
      this.arr[i] = src[j];
    }
    
    return cast_unchecked(((T)(null)), this);
  }
}

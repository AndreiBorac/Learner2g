/***
 * sX.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class s_N_ extends RootObject
{
  /***
   * (private) constructor to prevent instantiation.
   ***/
  private s_N_()
  {
    throw null;
  }
  
  /***
   * STATIC
   ***/
  
  _ENO_;
  public static final _E_ ZERO = 0;
  _LNO_;
  
  /***
   * equiv <code>Static.enAscii_N_(val)</code>.
   ***/
  _EPO_;
  public static cB enAscii(_E_ val)
  {
    return Static.enAscii_N_(val);
  }
  _LPO_;
  
  /***
   * equiv <code>Static.deAscii_N_(val)</code>.
   ***/
  _EPO_;
  public static _E_ deAscii(rB str)
  {
    return Static.deAscii_N_(str);
  }
  _LPO_;
  
  /***
   * returns the lesser of the two inputs.
   ***/
  public static _G_ _E_ min(_E_ A, _E_ B  _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _DECL_;
    _TEST_;
    
    if (_ALTB_) {
      return A;
    } else {
      return B;
    }
  }
  
  /***
   * returns the greater of the two inputs.
   ***/
  public static _G_ _E_ max(_E_ A, _E_ B  _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _DECL_;
    _TEST_;
    
    if (_AGTB_) {
      return A;
    } else {
      return B;
    }
  }
  
  /***
   * returns the least element of the given array range. the array
   * range should contain at least one element.
   ***/
  public static _G_ _E_ min(_E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!((0 <= off) && (off < lim) && (lim <= arr.length))) throw null;
    
    _E_ A = arr[off];
    
    for (int i = off + 1; i < lim; i++) {
      _E_ B = arr[i];
      
      _DECL_;
      _TEST_;
      
      if (_AGTB_) {
        A = B;
      }
    }
    
    return A;
  }
  
  /***
   * equiv <code>min(arr, 0, arr.length, ...)</code>
   ***/
  public static _G_ _E_ min(_E_[] arr _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return min(arr, 0, arr.length _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>min(src.all(), proxy)</code>, except that the backing
   * array is accessed directly.
   ***/
  public static _G_ _E_ min(r_N__G_ src _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return min(src.arr, src.off, src.lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * returns the greatest element of the given array range. the array
   * range should have at least one element.
   ***/
  public static _G_ _E_ max(_E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!((0 <= off) && (off < lim) && (lim <= arr.length))) throw null;
    
    _E_ A = arr[off];
    
    for (int i = off + 1; i < lim; i++) {
      _E_ B = arr[i];
      
      _DECL_;
      _TEST_;
      
      if (_ALTB_) {
        A = B;
      }
    }
    
    return A;
  }
  /***
   * equiv <code>max(arr, 0, arr.length, ...)</code>
   ***/
  public static _G_ _E_ max(_E_[] arr _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return max(arr, 0, arr.length _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>max(src.all(), proxy)</code>, except that the backing
   * array is accessed directly.
   ***/
  public static _G_ _E_ max(r_N__G_ src _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return max(src.arr, src.off, src.lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * returns a new array containing <code>len</code> copies of
   * <code>elm</code>.
   ***/
  public static _G_ _E_[] rep(_E_ elm, int len)
  {
    _E_[] out = new[len]new;
    
    for (int i = 0; i < len; i++) {
      out[i] = elm;
    }
    
    return out;
  }
  
  /***
   * returns a new array containing the <code>lim - off</code>
   * elements with values between <code>off</codee> (inclusive) and
   * <code>lim</code> (exclusive).
   ***/
  _EIO_;
  public static _G_ _E_[] range0(long off, long lim)
  {
    int len = ((int)(lim - off));
    if (!(0 <= len)) throw null;
    
    _E_[] out = new[len]new;
    
    for (int i = 0; i < out.length; i++) {
      out[i] = ((_E_)(off++));
    }
    
    return out;
  }
  _LIO_;
  
  /***
   * equiv <code>cB.newCopyOf(range0(off, lim))</code>, except possibly more efficient.
   ***/
  _EIO_;
  public static _G_ c_N__G_ rangeC(long off, long lim)
  {
    _E_[] bak = range0(off, lim);
    return (new c_N__G_()).iniLinkOfUnchecked(bak, 0, bak.length);
  }
  _LIO_;
  
  /***
   * equiv <code>wB.newCopyOf(range0(off, lim))</code>, except possibly more efficient.
   ***/
  _EIO_;
  public static _G_ w_N__G_ rangeW(long off, long lim)
  {
    _E_[] bak = range0(off, lim);
    return (new w_N__G_()).iniLinkOfUnchecked(bak, 0, bak.length);
  }
  _LIO_;
  
  /***
   * (shallowly) copies <code>len</code> elements from the source
   * array to the destination array. returns <code>dst</code>.
   ***/
  public static _G_ _E_[] mov(_E_[] dst, _E_[] src, int len)
  {
    for (int i = 0; i < len; i++) {
      dst[i] = src[i];
    }
    
    return dst;
  }
  
  /***
   * (shallowly) copies <code>len</code> elements from the beginning
   * of the source array to the range of the destination array
   * starting at <code>off</code>. returns <code>dst</code>.
   ***/
  public static _G_ _E_[] mov(_E_[] dst, int dii, _E_[] src, int len)
  {
    for (int i = 0; i < len; i++) {
      dst[dii + i] = src[i];
    }
    
    return dst;
  }
  
  /***
   * (shallowly) copies <code>len</code> elements from the range of
   * the source array starting at <code>sii</code> to the beginning of
   * the destination array. returns <code>dst</code>.
   ***/
  public static _G_ _E_[] mov(_E_[] dst, _E_[] src, int sii, int len)
  {
    for (int i = 0; i < len; i++) {
      dst[i] = src[sii + i];
    }
    
    return dst;
  }
  
  /***
   * (shallowly) copies <code>len</code> elements from the range of
   * the source array starting at <code>sii</code> to the the range of
   * the destination array starting at <code>dii</code>. returns
   * <code>dst</code>.
   ***/
  public static _G_ _E_[] mov(_E_[] dst, int dii, _E_[] src, int sii, int len)
  {
    for (int i = 0; i < len; i++) {
      dst[dii + i] = src[sii + i];
    }
    
    return dst;
  }
  
  /***
   * (shallowly) duplicates the given array range.
   ***/
  public static _G_ _E_[] dup(_E_[] arr, int off, int lim)
  {
    if (!((0 <= off) && (off <= lim) && (lim <= arr.length))) throw null;
    int len = lim - off;
    return mov(new[len]new, arr, off, len);
  }
  
  /***
   * (shallowly) duplicates the given array.
   ***/
  public static _G_ _E_[] dup(_E_[] arr)
  {
    return mov(new[arr.length]new, arr, arr.length);
  }
  
  /***
   * (shallowly) "resizes" the given array. the returned array will
   * have the length <code>len</code>. if <code>len</code> exceeds
   * <code>arr.length</code>, the remaining elements will have the
   * default uninitialized values.
   ***/
  public static _G_ _E_[] dup(_E_[] arr, int len)
  {
    return mov(new[len]new, arr, sI.min(arr.length, len));
  }
  
  /***
   * returns <code>elms</code> (de-vararg).
   ***/
  public static _G_ _E_[] from(_E_... elms)
  {
    return elms;
  }
  
  /***
   * returns the combined length of all arrays in <code>arrs</code>.
   ***/
  public static _G_ int clen(_E_[]... arrs)
  {
    int len = 0;
    
    for (int i = 0; i < arrs.length; i++) {
      len += arrs[i].length;
    }
    
    return len;
  }
  
  /***
   * returns a single array containing all the elements of the arrays
   * in <code>arrs</code> combined.
   ***/
  public static _G_ _E_[] join(_E_[]... arrs)
  {
    int pos = 0;
    _E_[] out = new[clen(arrs)]new;
    
    for (_E_[] arr : arrs) {
      mov(out, pos, arr, arr.length); pos += arr.length;
    }
    
    return out;
  }
  
  /***
   * returns the combined length of all slices in <code>slices</code>.
   ***/
  public static _G_ int clen(r_N__G_... slices)
  {
    int len = 0;
    
    for (int i = 0; i < slices.length; i++) {
      len += slices[i].len();
    }
    
    return len;
  }
  
  /***
   * returns the combined length of all arrays in <code>arrs</code>.
   ***/
  public static _G_ int clen(rL<? extends r_N__G_> arrs)
  {
    int len = 0;
    
    for (int i = arrs.off(); i < arrs.lim(); i++) {
      len += arrs.get(i).len();
    }
    
    return len;
  }
  
  /***
   * returns a single array containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ _E_[] joinA(r_N__G_... slices)
  {
    int pos = 0;
    _E_[] out = new[clen(slices)]new;
    
    for (r_N__G_ slice : slices) {
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return out;
  }
  
  /***
   * returns a single array containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ _E_[] joinA(rL<? extends r_N__G_> slices)
  {
    int pos = 0;
    _E_[] out = new[clen(slices)]new;
    
    for (int i = slices.off(); i < slices.lim(); i++) {
      r_N__G_ slice = slices.get(i);
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return out;
  }
  
  /***
   * returns a single slice containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ c_N__G_ joinC(r_N__G_... slices)
  {
    int pos = 0;
    _E_[] out = new[clen(slices)]new;
    
    for (r_N__G_ slice : slices) {
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return (new c_N__G_()).iniLinkOf(out, 0, out.length);
  }
  
  /***
   * returns a single slice containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ c_N__G_ joinC(rL<? extends r_N__G_> slices)
  {
    int pos = 0;
    _E_[] out = new[clen(slices)]new;
    
    for (int i = slices.off(); i < slices.lim(); i++) {
      r_N__G_ slice = slices.get(i);
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return (new c_N__G_()).iniLinkOf(out, 0, out.length);
  }
  
  /***
   * returns a single slice containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ w_N__G_ joinW(r_N__G_... slices)
  {
    int pos = 0;
    _E_[] out = new[sI.max(clen(slices), 1)]new; // for w_N__G_, allocate at least one element (class invariant)
    
    for (r_N__G_ slice : slices) {
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return (new w_N__G_()).iniLinkOf(out, 0, out.length);
  }
  
  /***
   * returns a single slice containing all the elements of the slices
   * in <code>arrs</code> combined.
   ***/
  public static _G_ w_N__G_ joinW(rL<? extends r_N__G_> slices)
  {
    int pos = 0;
    _E_[] out = new[sI.max(clen(slices), 1)]new; // for w_N__G_, allocate at least one element (class invariant)
    
    for (int i = slices.off(); i < slices.lim(); i++) {
      r_N__G_ slice = slices.get(i);
      int slice_len = slice.len();
      mov(out, pos, slice.arr, slice.off, slice_len); pos += slice_len;
    }
    
    return (new w_N__G_()).iniLinkOf(out, 0, out.length);
  }
  
  /***
   * returns <code>join(arrs)</code>, terminating each array in
   * <code>arrs</code> with a copy of the elements in
   * <code>cap</code>.
   ***/
  public static _G_ _E_[] joinT(_E_[] cap, _E_[]... arrs)
  {
    int pos = 0;
    _E_[] out = new[clen(arrs) + (cap.length * arrs.length)]new;
    
    for (_E_[] arr : arrs) {
      mov(out, pos, arr, arr.length); pos += arr.length;
      mov(out, pos, cap, cap.length); pos += cap.length;
    }
    
    return out;
  }
  
  /***
   * returns <code>join(arrs)</code>, delimiting the components with a
   * copy of the elements in <code>sep</code>.
   ***/
  public static _G_ _E_[] joinS(_E_[] sep, _E_[]... arrs)
  {
    if (arrs.length == 0) {
      return new[0]new;
    }
    
    int pos = 0;
    _E_[] out = new[clen(arrs) + (sep.length * (arrs.length - 1))]new;
    
    // head
    {
      _E_[] arr = arrs[0];
      mov(out, pos, arr, arr.length); pos += arr.length;
    }
    
    // tail
    {
      for (int i = 1; i < arrs.length; i++) {
        _E_[] arr = arrs[i];
        
        mov(out, pos, sep, sep.length); pos += sep.length;
        mov(out, pos, arr, arr.length); pos += arr.length;
      }
    }
    
    return out;
  }
  
  /***
   * equiv <code>startsWith(src.arr(), src.off(), src.lim(),
   * pre.arr())</code>, except possibly more efficient as it accesses
   * the backing array directly. requires <code>pre</code> to be
   * "packed."
   ***/
  public static _G_ boolean startsWith(r_N__G_ src, r_N__G_ pre _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!pre.packed()) throw null;
    return startsWith(src.arr, src.off, src.lim, pre.arr _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>startsWith(src.arr(), off, lim, pre.arr())</code>,
   * except possibly more efficient as it accesses the backing array
   * directly. requires <code>pre</code> to be "packed."
   ***/
  public static _G_ boolean startsWith(r_N__G_ src, int off, int lim, r_N__G_ pre _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!pre.packed()) throw null;
    return startsWith(src.arr, off, lim, pre.arr _ELO_ , proxy _LLO_);
  }
  
  /***
   * returns true iff <code>src[off..lim)</code> starts with <code>pre</code>.
   ***/
  public static _G_ boolean startsWith(_E_[] src, int off, int lim, _E_[] pre _ELO_ , Proxy_G_ proxy _LLO_)
  {
    int len = lim - off;
    
    if (len < pre.length) {
      return false;
    }
    
    for (int i = 0; i < pre.length; i++) {
      _E_ A = src[off++];
      _E_ B = pre[i];
      
      _DECL_;
      _TEST_;
      
      if (!(_AEQB_)) {
        return false;
      }
    }
    
    return true;
  }
  
  /***
   * equiv <code>indexOf(src.arr(), src.off(), src.lim(),
   * fix.arr())</code>, except possibly more efficient as it accesses
   * the backing array directly. requires <code>fix</code> to be
   * "packed."
   ***/
  public static _G_ int indexOf(r_N__G_ src, r_N__G_ fix _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!fix.packed()) throw null;
    return indexOf(src.arr, src.off, src.lim, fix.arr _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>indexOf(src.arr(), off, lim, fix.arr())</code>,
   * except possibly more efficient as it accesses the backing array
   * directly. requires <code>fix</code> to be "packed."
   ***/
  public static _G_ int indexOf(r_N__G_ src, int off, int lim, r_N__G_ fix _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!fix.packed()) throw null;
    return indexOf(src.arr, off, lim, fix.arr _ELO_ , proxy _LLO_);
  }
  
  /***
   * returns the first index of <code>fix</code> in
   * <code>src[off..lim)</code> or -1 if not found. the returned index
   * is relative to the head of <code>src</code>, it is not relative
   * to <code>off</code>. that is, if <code>fix</code> is found, the
   * returned index will be a value in the range
   * <code>[off..lim)</code>.
   ***/
  public static _G_ int indexOf(_E_[] src, int off, int lim, _E_[] fix _ELO_ , Proxy_G_ proxy _LLO_)
  {
    while ((lim - off) >= fix.length) {
      if (startsWith(src, off, lim, fix _ELO_ , proxy _LLO_)) {
        return off;
      }
      
      off++;
    }
    
    return -1;
  }
  
  /***
   * enter methods for splitting strings into tokens
   ***/
  
  /***
   * equiv <code>splitCC(src.arr(), src.off(), src.lim(), sep,
   * valmaxI)</code>, except possibly more efficient. requires
   * <code>sep</code> to be "packed."
   ***/
  public static _G_ cL<c_N__G_> splitCC(r_N__G_ src, c_N__G_ sep _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return splitCC(src, src.off, src.lim, sep, valmaxI _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>splitCC(src.arr(), src.off(), src.lim(), sep,
   * max)</code>, except possibly more efficient. requires
   * <code>sep</code> to be "packed."
   ***/
  public static _G_ cL<c_N__G_> splitCC(r_N__G_ src, c_N__G_ sep, int max _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return splitCC(src, src.off, src.lim, sep, max _ELO_ , proxy _LLO_);
  }
  
  /***
   * requires <code>sep</code> to be "packed."
   ***/
  public static _G_ cL<c_N__G_> splitCC(r_N__G_ src, int off, int lim, c_N__G_ sep, int max _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!sep.packed()) throw null;
    
    int ctr = s_N_.split00(src.arr, off, lim, sep.arr, max, null, 0 _ELO_ , proxy _LLO_);
    
    Object[] bak = anewarray(ctr);
    
    for (int i = 0; i < bak.length; i++) {
      bak[i] = new c_N__G_();
    }
    
    cL<c_N__G_> out = new cL<c_N__G_>();
    out.iniLinkOfUnchecked(bak, 0, bak.length);
    s_N_.split00(src.arr, off, lim, sep.arr, max, cast_unchecked(((rL<r_N__G_>)(null)), out), 0 _ELO_ , proxy _LLO_);
    return out;
  }
  
  /***
   * equiv <code>splitMC(src.arr(), src.off(), src.lim(), sep,
   * valmaxI)</code>, except possibly more efficient. requires
   * <code>sep</code> to be "packed."
   ***/
  public static _G_ wL<c_N__G_> splitMC(r_N__G_ src, c_N__G_ sep _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return splitMC(src, 0, src.len(), sep, valmaxI _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>splitMC(src.arr(), src.off(), src.lim(), sep,
   * max)</code>, except possibly more efficient. requires
   * <code>sep</code> to be "packed."
   ***/
  public static _G_ wL<c_N__G_> splitMC(r_N__G_ src, c_N__G_ sep, int max _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return splitMC(src, 0, src.len(), sep, max _ELO_ , proxy _LLO_);
  }
  
  /***
   * requires <code>sep</code> to be "packed."
   ***/
  public static _G_ wL<c_N__G_> splitMC(r_N__G_ src, int off, int lim, c_N__G_ sep, int max _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!sep.packed()) throw null;
    
    int ctr = s_N_.split00(src.arr, off, lim, sep.arr, max, null, 0 _ELO_ , proxy _LLO_);
    
    Object[] bak = anewarray(ctr);
    
    for (int i = 0; i < bak.length; i++) {
      bak[i] = new c_N__G_();
    }
    
    wL<c_N__G_> out = new wL<c_N__G_>();
    out.iniLinkOfUnchecked(bak, 0, bak.length);
    s_N_.split00(src.arr, off, lim, sep.arr, max, cast_unchecked(((rL<r_N__G_>)(null)), out), 0 _ELO_ , proxy _LLO_);
    return out;
  }
  
  public static _G_ int split00(_E_[] src, int off, int lim, _E_[] sep, int max, rL<r_N__G_> out, int cap _ELO_ , Proxy_G_ proxy _LLO_)
  {
    int ctr = 0;
    
    int head = off;
    
    while (ctr < max) {
      while ((off < lim) && (src[off] != sep[0])) off++;
      if (!(off < lim)) break;
      
      if ((off + sep.length) <= lim) {
        boolean match = true;
        
        for (int j = 1; j < sep.length; j++) {
          _E_ A = src[off + j];
          _E_ B = sep[j];
          
          _DECL_;
          _TEST_;
          
          if (!(_AEQB_)) {
            match = false;
            break;
          }
        }
        
        if (match) {
          if (out != null) {
            out.get(ctr++).iniCopyOf(src, head, off, 0, cap);
          } else {
            ctr++;
          }
          
          off += sep.length;
          head = off;
        } else {
          off++;
        }
      }
    }
    
    if (head != lim) {
      if (out != null) {
        out.get(ctr++).iniCopyOf(src, head, lim, 0, cap);
      } else {
        ctr++;
      }
    }
    
    return ctr;
  }
  
  /***
   * leave methods for splitting strings into tokens
   ***/
  
  /***
   * zips a pair of array of the underlying type (objects only).
   ***/
  _ELO_;
  public static <A, B> D2<A, B>[] zip2(D2<A[], B[]> pair)
  {
    return zip2(cast_unchecked_array(((A)(null)), pair.d1), cast_unchecked_array(((B)(null)), pair.d2));
  }
  _LLO_;
  
  /***
   * zips a pair of array of the underlying type (objects only).
   ***/
  _ELO_;
  public static <A, B> D2<A, B>[] zip2(A[] arrA, B[] arrB)
  {
    int len = eqI(arrA.length, arrB.length);
    
    D2<A, B>[] out = cast_unchecked_array(((D2<A, B>)(null)), (new D2[len]));
    
    for (int i = 0; i < len; i++) {
      out[i] = new D2<A, B>(arrA[i], arrB[i]);
    }
    
    return out;
  }
  _LLO_;
  
  /***
   * unzips an array of pairs of the underlying type (objects only).
   ***/
  _ELO_;
  public static <A, B> D2<A[], B[]> unzip2(D2<A, B>[] arr)
  {
    A[] outA = anewarray_unchecked(((A)(null)), arr.length);
    B[] outB = anewarray_unchecked(((B)(null)), arr.length);
    
    for (int i = 0; i < arr.length; i++) {
      outA[i] = arr[i].d1;
      outB[i] = arr[i].d2;
    }
    
    return new D2<A[], B[]>(outA, outB);
  }
  _LLO_;
  
  /***
   * compares arrays of the underlying type.
   ***/
  public static _G_ int compare(_E_[] arrA, int offA, int limA, _E_[] arrB, int offB, int limB _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if (!((0 <= offA) && (offA <= limA) && (limA <= arrA.length))) throw null;
    if (!((0 <= offB) && (offB <= limB) && (limB <= arrB.length))) throw null;
    
    int lenA = limA - offA;
    int lenB = limB - offB;
    
    int len = sI.min(lenA, lenB);
    
    while (len-- > 0) {
      _E_ A = arrA[offA++];
      _E_ B = arrB[offB++];
      
      _DECL_;
      _TEST_;
      
      _EPO_;
      {
        if (_ALTB_) return -1;
        if (_AGTB_) return +1;
      }
      _LPO_;
      
      _ELO_;
      {
        if (AcmpB != 0) return AcmpB;
      }
      _LLO_;
    }
    
    if (lenA < lenB) return -1;
    if (lenA > lenB) return +1;
    
    return 0;
  }
  
  /***
   * equiv <code>compare(arrA, 0, arrA.length, arrB, 0, arrB.length, ...)</code>
   ***/
  public static _G_ int compare(_E_[] arrA, _E_[] arrB _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return compare(arrA, 0, arrA.length, arrB, 0, arrB.length _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>compare(arrA.arr(), arrA.off(), arrA.lim(), arrB.arr(), arrB.off(), arrB.lim(), ...)</code>, except possibly more efficient.
   ***/
  public static _G_ int compare(r_N__G_ arrA, r_N__G_ arrB _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return compare(arrA.arr, arrA.off, arrA.lim, arrB.arr, arrB.off, arrB.lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * equiv <code>compare(arrA.arr(), arrA_off, arrA_lim, arrB.arr(), arrB_off, arrB_lim, ...)</code>, except possibly more efficient.
   ***/
  public static _G_ int compare(r_N__G_ arrA, int arrA_off, int arrA_lim, r_N__G_ arrB, int arrB_off, int arrB_lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return compare(arrA.arr, arrA_off, arrA_lim, arrB.arr, arrB_off, arrB_lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * hashes arrays of the underlying type.
   ***/
  public static _EPO_ <A> _LPO_ _ELO_ <E, A> _LLO_ void compress(HashCore<A> state, _E_[] EE, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _EPO_;
    state.accept_N_(EE, off, lim);
    _LPO_;
    
    _ELO_;
    proxy.compressMultiple(state, EE, off, lim);
    _LLO_;
  }
  
  /***
   * hashes arrays of the underlying type.
   ***/
  public static _EPO_ <A> _LPO_ _ELO_ <E, A> _LLO_ void compress(HashCore<A> state, _E_[] EE _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _EPO_;
    state.accept_N_(EE);
    _LPO_;
    
    _ELO_;
    proxy.compressMultiple(state, EE, 0, EE.length);
    _LLO_;
  }
  
  /***
   * returns a new proxy object for comparing/hashing arrays of the
   * underlying type.
   ***/
  public static _G_ Proxy<_E_[]> newArrayProxy(_ELO_ final Proxy_G_ proxy _LLO_)
  {
    return
      (new Proxy<_E_[]>()
       {
         public int compare(_E_[] AA, _E_[] BB)
         {
           return s_N_.compare(AA, BB _ELO_ , proxy _LLO_);
         }
         
         public <A> void compress(HashCore<A> state, _E_[] EE)
         {
           s_N_.compress(state, EE _ELO_ , proxy _LLO_);
         }
         
         public <A> void compressMultiple(HashCore<A> state, _E_[][] arr, int off, int lim)
         {
           while (off < lim) {
             s_N_.compress(state, arr[off++] _ELO_ , proxy _LLO_);
           }
         }
       });
  }
  
  private static _G_ void sort_inner(_E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _E_ B = arr[off];
    
    // bone-basic quicksort that uses four regions:
    // [off..eq0) less-than region
    // [eq0..gt0) equal-to region
    // [gt0..ptr) greater-than region
    // [ptr..lim) unknown region
    
    int lt0 = off;
    int eq0 = off;
    int gt0 = off + 1;
    int ptr = off + 1;
    
    while (ptr < lim) {
      _E_ A = arr[ptr];
      
      _EPO_;
      final boolean test_leB = !(_AGTB_);
      final boolean test_ltB =  (_ALTB_);
      _LPO_;
      
      _ELO_;
      final int cmp = proxy.compare(A, B);
      final boolean test_leB = (cmp <= 0);
      final boolean test_ltB = (cmp <  0);
      _LLO_;
      
      if (test_leB) {
        // swap with gt0 and expand equal region (by incrementing gt0)
        arr[ptr] = arr[gt0];
        // arr[gt0] = A; // TO ELSE CLAUSE
        // gt0++; // TO BELOW IF
        
        if (test_ltB) {
          // swap with eq0 and expand less than region (by incrementing eq0)
          arr[gt0 /* -1 NOT AFTER INCREMENT TO BELOW IF */] = arr[eq0];
          arr[eq0] = A;
          eq0++;
        } else {
          arr[gt0] = A;
        }
        
        gt0++;
      }
      
      ptr++;
    }
    
    if ((eq0 - off) > 1) sort_inner(arr, off, eq0 _ELO_ , proxy _LLO_);
    if ((lim - gt0) > 1) sort_inner(arr, gt0, lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * sorts an array of the underlying type.
   ***/
  public static _G_ void sort(_E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if ((lim - off) > 1) {
      sort_inner(arr, off, lim _ELO_ , proxy _LLO_);
    }
  }
  
  /***
   * equiv <code>sort(arr, 0, arr.length, ...)</code>.
   ***/
  public static _G_ void sort(_E_[] arr _ELO_ , Proxy_G_ proxy _LLO_)
  {
    sort(arr, 0, arr.length _ELO_ , proxy _LLO_);
  }
  
  private static _G_ void vsort_inner(int[] loc, _E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    int loB = loc[off];
    _E_ B = arr[loB];
    
    // bone-basic quicksort that uses four regions:
    // [off..eq0) less-than region
    // [eq0..gt0) equal-to region
    // [gt0..ptr) greater-than region
    // [ptr..lim) unknown region
    
    int lt0 = off;
    int eq0 = off;
    int gt0 = off + 1;
    int ptr = off + 1;
    
    while (ptr < lim) {
      int loA = loc[ptr];
      _E_ A = arr[loA];
      
      _EPO_;
      final boolean test_leB = !(_AGTB_);
      final boolean test_ltB =  (_ALTB_);
      _LPO_;
      
      _ELO_;
      final int cmp = proxy.compare(A, B);
      final boolean test_leB = (cmp <= 0);
      final boolean test_ltB = (cmp <  0);
      _LLO_;
      
      if (test_leB) {
        // swap with gt0 and expand equal region (by incrementing gt0)
        loc[ptr] = loc[gt0];
        // arr[gt0] = A; // TO ELSE CLAUSE
        // gt0++; // TO BELOW IF
        
        if (test_ltB) {
          // swap with eq0 and expand less than region (by incrementing eq0)
          loc[gt0 /* -1 NOT AFTER INCREMENT TO BELOW IF */] = loc[eq0];
          loc[eq0] = loA;
          eq0++;
        } else {
          loc[gt0] = loA;
        }
        
        gt0++;
      }
      
      ptr++;
    }
    
    if ((eq0 - off) > 1) vsort_inner(loc, arr, off, eq0 _ELO_ , proxy _LLO_);
    if ((lim - gt0) > 1) vsort_inner(loc, arr, gt0, lim _ELO_ , proxy _LLO_);
  }
  
  /***
   * virtually sorts an array of the underlying type.
   ***/
  public static _G_ void vsort(int[] loc, _E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    if ((lim - off) > 1) {
      vsort_inner(loc, arr, off, lim _ELO_ , proxy _LLO_);
    }
  }
  
  /***
   * returns the index of the first element of the given range of
   * <code>arr</code> that is greater than equal to <code>ref</code>,
   * or <code>off-1</code> if all elements are greater than
   * <code>ref</code> or <code>lim</code> if all elements are smaller
   * than <code>ref</code>. operates under the assumption that
   * <code>arr</code> is sorted. if <code>off</code> is greater than
   * or equal to <code>lim</code>, then <code>lim</code> is returned.
   ***/
  public static _G_ int binarySearch(_E_ ref, _E_[] arr, int off, int lim _ELO_ , Proxy_G_ proxy _LLO_)
  {
    _E_ A = ref;
    
    while (lim > off) {
      int mid = (off + lim) >> 1;
      _E_ B = arr[mid];
      
      _DECL_;
      _TEST_;
      
      /****/ if (_AGTB_) {
        off = mid + 1;
      } else {
        lim = mid;
      }
    }
    
    return lim;
  }
  
  /***
   * equiv <code>binarySearch(ref, arr, 0, arr.length, ...)</code>.
   ***/
  public static _G_ int binarySearch(_E_ ref, _E_[] arr _ELO_ , Proxy_G_ proxy _LLO_)
  {
    return binarySearch(ref, arr, 0, arr.length _ELO_ , proxy _LLO_);
  }
  
  _EZO_;
  
  public static cZ compile_allow_table(int max, int... ranges)
  {
    boolean[] out = (new boolean[max]);
    
    for (int i = 0; i < ranges.length; i += 2) {
      for (int j = ranges[i+0]; j <= ranges[i+1]; j++) {
        out[j] = true;
      }
    }
    
    return cZ.newCopyOf(out);
  }
  
  _LZO_;
  
  _EIO_;
  
  public static boolean satisfies_allow_table(r_N_ input, rZ table)
  {
    boolean retv = true;
    
    for (int pos = input.off(); pos < input.lim(); pos++) {
      retv = (retv & table.get((int)(input.get(pos))));
    }
    
    return retv;
  }
  
  public static _E_[] map0(r_N_ input, r_N_ table)
  {
    int input_off = input.off();
    
    _E_[] out = new[input.len()]new;
    
    for (int i = 0; i < out.length; i++) {
      out[i] = table.get((int)(input.get(input_off++)));
    }
    
    return out;
  }
  
  public static c_N_ mapC(r_N_ input, r_N_ table)
  {
    _E_[] bak = map0(input, table);
    return (new c_N_()).iniLinkOfUnchecked(bak, 0, bak.length);
  }
  
  public static w_N_ mapW(r_N_ input, r_N_ table)
  {
    _E_[] bak = map0(input, table);
    return (new w_N_()).iniLinkOfUnchecked(bak, 0, bak.length);
  }
  
  _LIO_;
}

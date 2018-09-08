/***
 * optimizer.cpp
 * copyright (c) 2011 by andrei borac
 ***/

#define   likely(x) __builtin_expect((x),1)
#define unlikely(x) __builtin_expect((x),0)

#define ACQUIRE(what_jtype, what_utype, what_codename, what_SUFFIX)     \
  jboolean jiscopy##what_codename;                                      \
  what_jtype * arr##what_codename =                                     \
    (*jenv)->Get##what_utype##Array##what_SUFFIX(jenv, jarr##what_codename, &jiscopy##what_codename); \
  if (jiscopy##what_codename) {                                         \
    fprintf(stderr, "warning: pomber: JNI did array copy, which is suboptimal (did you try -DUSECRITICAL ?)\n"); \
  }

#define RELEASE(what_utype, what_codename, what_SUFFIX)                 \
  (*jenv)->Release##what_utype##Array##what_SUFFIX(jenv, jarr##what_codename, arr##what_codename, JNI_ABORT);

#include "../../temp/codegen/zs42_nats_codec_Optimizer.h"

// enter algorithm

#if 1

static inline int min(int a, int b)
{
  return ((a <= b) ? a : b);
}

static inline int max(int a, int b)
{
  return ((a >= b) ? a : b);
}

static inline int bit_length_32(int v)
{
  //if (v < 0) throw null;
  
  int x = 0;
  
  while ((v & ((1 << x) - 1)) != v) x++;
  
  return x;
}

static inline int encode_universal_length(int v)
{
  return (bit_length_32(v) << 1);
}

static inline int span_maximum_bit_length(jint*restrict blen, int off, int lim)
{
  //if (!(off < lim)) throw null;
  
  int maxlen = 0;
  
  for (int i = off; i < lim; i++) {
    maxlen = /* Math. */ max(maxlen, blen[i]);
  }
  
  return maxlen;
}

static inline int encode_span_length(int maxlen, int off, int lim)
{
  //if (!(off < lim)) throw null;
  
  int length = 0;
  
  length += encode_universal_length(maxlen);
  length += encode_universal_length((lim - off));
  length += ((lim - off) * maxlen);
  
  return length;
}

static inline void optimize(jint numv, jint*restrict blen, jint*restrict optc, jint*restrict optn, jint xlen)
{
  // first pass
  // dynamic programming
  // for speed, do not consider runs longer than xlen
  {
    int off = numv;
    
    while (off-- > 0) {
      int local_optc = (1 << 30); //Integer.MAX_VALUE;
      int local_optn = -1;
      
      int maxlen = 0;
      
      for (int lim = off + 1, end = /* Math. */ min(off + xlen, numv); lim <= end; lim++) {
        // try joining (inclusive) off..lim (exclusive)
        
        // including one more value
        maxlen = /* Math. */ max(maxlen, blen[lim - 1]);
        
        // calculate cost
        int cost = encode_span_length(maxlen, off, lim);
        if (lim < numv) cost += optc[lim];
        
        // update optimal solution
        if (cost < local_optc) {
          local_optc = cost;
          local_optn = (lim - off);
        }
      }
      
      //if (local_optn == -1) throw null;
      
      optc[off] = local_optc;
      optn[off] = local_optn;
    }
  }
  
  // second pass
  // merge adjacent runs of equal bit-length
  // on optimal path only, of course
  {
    int off = 0;
    
    if (off < numv) {
      int lim = off + optn[off];
      
      while (lim < numv) {
        int off2 = lim;
        int lim2 = off2 + optn[off2];
        
        if (span_maximum_bit_length(blen, off, lim) == span_maximum_bit_length(blen, off2, lim2)) {
          optn[off] += optn[off2];
        } else {
          off = off2;
        }
        
        lim = lim2;
      }
    }
  }
}

#else

static inline void optimize(jint numv, jint* blen, jint* optc, jint* optn, jint xlen)
{
}

#endif

// leave algorithm

JNIEXPORT void JNICALL Java_zs42_nats_codec_Optimizer_optimize
(JNIEnv * jenv, jclass jenc, jint numv, jintArray jarr_blen, jintArray jarr_optc, jintArray jarr_optn, jint xlen)
{
#ifdef USECRITICAL
  ACQUIRE(jint, Primitive, _blen, Critical);
  ACQUIRE(jint, Primitive, _optc, Critical);
  ACQUIRE(jint, Primitive, _optn, Critical);
#else
  ACQUIRE(jint, Int, _blen, Elements);
  ACQUIRE(jint, Int, _optc, Elements);
  ACQUIRE(jint, Int, _optn, Elements);
#endif
  
  optimize(numv, arr_blen, arr_optc, arr_optn, xlen);
  
#ifdef USECRITICAL
  RELEASE(Primitive, _blen, Critical);
  RELEASE(Primitive, _optc, Critical);
  RELEASE(Primitive, _optn, Critical);
#else
  RELEASE(Int, _blen, Elements);
  RELEASE(Int, _optc, Elements);
  RELEASE(Int, _optn, Elements);
#endif
}

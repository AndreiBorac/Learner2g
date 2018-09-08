/***
 * MurmurHashCore.java
 * copyright (c) 2011 by andrei borac
 * the hashing algorithm underlying this code is Austin Appleby's MurmurHash3
 ***/

package zs42.mass;

public final class MurmurHashCore extends HashCore<MurmurHashCore>
{
  long h1, h2, c1, c2, nb;
  
  private void mix(long k1, long k2)
  {
    k1 *= c1;
    k1  = (k1 << 23) | (k1 >>> (64 - 23));
    k1 *= c2;
    h1 ^= k1;
    h1 += h2;
    h2  = (h2 << 41) | (h2 >>> (64 - 41));
    k2 *= c2;
    k2  = (k2 << 23) | (k2 >>> (64 - 23));
    k2 *= c1;
    h2 ^= k2;
    h2 += h1;
    h1  = (3 * h1) + 0x52dce729;
    h2  = (3 * h2) + 0x38495ab5;
    c1  = (5 * c1) + 0x7b7d159c;
    c2  = (5 * c2) + 0x6bce6396;
    nb++;
  }
  
  private long fix(long k0)
  {
    k0 ^= k0 >>> 33;
    k0 *= 0xff51afd7ed558ccdL;
    k0 ^= k0 >>> 33;
    k0 *= 0xc4ceb9fe1a85ec53L;
    k0 ^= k0 >> 33;
    return k0;
  }
  
  private void fin()
  {
    h2 ^= nb;
    h1 += h2;
    h2 += h1;
    h1  = fix(h1);
    h2  = fix(h2);
    h1 += h2;
    h2 += h1;
  }
  
  public MurmurHashCore()
  {
    this(0x9368e53c2f6af274L, 0x586dcd208f7cd3fdL, 0x87c37b91114253d5L, 0x4cf5ad432745937fL, 0);
  }
  
  public MurmurHashCore(long h1, long h2, long c1, long c2, long nb)
  {
    this.h1 = h1;
    this.h2 = h2;
    this.c1 = c1;
    this.c2 = c2;
    this.nb = nb;
  }
  
  public MurmurHashCore duplicateState()
  {
    return new MurmurHashCore(h1, h2, c1, c2, nb);
  }
  
  public void copyStateFrom(MurmurHashCore src)
  {
    h1 = src.h1;
    h2 = src.h2;
    c1 = src.c1;
    c2 = src.c2;
    nb = src.nb;
  }
  
  public long finish()
  {
    fin();
    return h1;
  }
  
  public int finish(long[] out, int off, int lim)
  {
    int bas = off;
    
    if (off < lim) out[off++] = h1;
    if (off < lim) out[off++] = h2;
    
    return off - bas;
  }
  
  public void acceptZ(boolean val)
  {
    long k0 = val ? 1 : 0;
    mix(k0, k0);
  }
  
  public void acceptZ(boolean[] arr, int off, int lim)
  {
    // full blocks
    {
      while ((lim - off) > 128) {
        long k0 = 0;
        long k1 = 0;
        
        for (int i = 0; i < 8; i++) {
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 0
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 1
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 2
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 3
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 4
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 5
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 6
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0; // 7
        }
        
        for (int i = 0; i < 8; i++) {
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 0
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 1
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 2
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 3
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 4
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 5
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 6
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0; // 7
        }
        
        mix(k0, k1);
      }
    }
    
    // partial block
    {
      if (lim > off) {
        long k0 = 0;
        long k1 = 0;
        
        while ((lim - off) > 2) {
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0;
          k1 <<= 1; k1 |= arr[off++] ? 1 : 0;
        }
        
        if (lim > off) {
          k0 <<= 1; k0 |= arr[off++] ? 1 : 0;
        }
        
        mix(k0, k1);
      }
    }
  }
  
  public void acceptB(byte val)
  {
    long k0 = val;
    mix(k0, k0);
  }
  
  public void acceptB(byte[] arr, int off, int lim)
  {
    // full blocks
    {
      while ((lim - off) > 16) {
        long k0 = 0;
        long k1 = 0;
        
        for (int i = 0; i < 8; i++) {
          k0 <<= 8; k0 ^= arr[off++];
          k1 <<= 8; k1 ^= arr[off++];
        }
        
        mix(k0, k1);
      }
    }
    
    // partial block
    {
      if (lim > off) {
        long k0 = 0;
        long k1 = 0;
        
        while ((lim - off) > 2) {
          k0 <<= 8; k0 ^= arr[off++];
          k1 <<= 8; k1 ^= arr[off++];
        }
        
        if (lim > off) {
          k0 <<= 8; k0 ^= arr[off++];
        }
        
        mix(k0, k1);
      }
    }
  }
  
  public void acceptS(short val)
  {
    long k0 = val;
    mix(k0, k0);
  }
  
  public void acceptS(short[] arr, int off, int lim)
  {
    // full blocks
    {
      while ((lim - off) > 8) {
        long k0 = 0;
        long k1 = 0;
        
        for (int i = 0; i < 8; i++) {
          k0 <<= 16; k0 ^= arr[off++];
          k1 <<= 16; k1 ^= arr[off++];
        }
        
        mix(k0, k1);
      }
    }
    
    // partial block
    {
      if (lim > off) {
        long k0 = 0;
        long k1 = 0;
        
        while ((lim - off) > 2) {
          k0 <<= 16; k0 ^= arr[off++];
          k1 <<= 16; k1 ^= arr[off++];
        }
        
        if (lim > off) {
          k0 <<= 16; k0 ^= arr[off++];
        }
        
        mix(k0, k1);
      }
    }
  }
  
  public void acceptI(int val)
  {
    long k0 = val;
    mix(k0, k0);
  }
  
  public void acceptI(int[] arr, int off, int lim)
  {
    // full blocks
    {
      while ((lim - off) > 4) {
        long k0 = 0;
        long k1 = 0;
        
        for (int i = 0; i < 4; i++) {
          k0 <<= 32; k0 ^= arr[off++];
          k1 <<= 32; k1 ^= arr[off++];
        }
        
        mix(k0, k1);
      }
    }
    
    // partial block
    {
      long k0 = 0;
      long k1 = 0;
      
      while ((lim - off) > 2) {
        k0 <<= 32; k0 ^= arr[off++];
        k1 <<= 32; k1 ^= arr[off++];
      }
      
      if (lim > off) {
        k0 <<= 32; k0 ^= arr[off++];
      }
      
      mix(k0, k1);
    }
  }
  
  public void acceptJ(long val)
  {
    long k0 = val;
    mix(k0, k0);
  }
  
  public void acceptJ(long[] arr, int off, int lim)
  {
    // full blocks
    {
      while ((lim - off) > 2) {
        long k0 = arr[off++];
        long k1 = arr[off++];
        
        mix(k0, k1);
      }
    }
    
    // partial block
    {
      if (lim > off) {
        long k0 = 0;
        long k1 = 0;
        
        k0 = arr[off++];
        
        mix(k0, k1);
      }
    }
  }
}

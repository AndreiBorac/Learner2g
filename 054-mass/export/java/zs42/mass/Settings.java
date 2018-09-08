/***
 * Settings.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public final class Settings extends RootObject
{
  final cTreeMapLL<cB, cB> map;
  
  public Settings(rB... settings)
  {
    wTreeMapLL<cB, cB> tmp = wTreeMapLL.newEmpty(cB.proxy);
    
    for (int i = 0; i < settings.length; i += 2) {
      rB rkey = settings[i + 0];
      rB rval = settings[i + 1];
      
      if (rkey == null) throw null;
      if (rval == null) throw null;
      
      cB ckey = cB.newSafeOf(rkey);
      cB cval = cB.newSafeOf(rval);
      
      tmp.put(ckey, cval);
    }
    
    map = cTreeMapLL.newCopyOf(tmp);
  }
  
  public boolean has(cB key)
  {
    return (map.get(key) != null);
  }
  
  public cB get(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return val;
  }
  
  public cB get(cB key, cB def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return val;
  }
  
  public boolean getZ(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiZ(val);
  }
  
  public byte getB(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiB(val);
  }
  
  public short getS(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiS(val);
  }
  
  public int getI(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiI(val);
  }
  
  public long getJ(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiJ(val);
  }
  
  public float getF(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiF(val);
  }
  
  public double getD(cB key)
  {
    cB val = map.get(key);
    if (val == null) throw null;
    return deAsciiD(val);
  }
  
  public boolean getZ(cB key, boolean def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiZ(val);
  }
  
  public byte getB(cB key, byte def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiB(val);
  }
  
  public short getS(cB key, short def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiS(val);
  }
  
  public int getI(cB key, int def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiI(val);
  }
  
  public long getJ(cB key, long def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiJ(val);
  }
  
  public float getF(cB key, float def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiF(val);
  }
  
  public double getD(cB key, double def)
  {
    cB val = map.get(key);
    if (val == null) return def;
    return deAsciiD(val);
  }
}

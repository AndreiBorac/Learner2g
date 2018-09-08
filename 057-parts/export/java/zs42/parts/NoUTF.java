/***
 * NoUTF.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public class NoUTF
{
  public static class Filter
  {
    private final boolean[] allow_map;
    
    public Filter(boolean[] allow_map)
    {
      this.allow_map = allow_map;
    }
    
    public Filter(String specification)
    {
      this(processSpecification(specification));
    }
    
    protected static boolean[] processSpecification(String specification)
    {
      boolean[] allow_map = (new boolean[256]);
      
      for (int i = 0; i < specification.length(); i += 2) {
        int min = (((int)(specification.charAt(i+0))) & 0xFF);
        int max = (((int)(specification.charAt(i+1))) & 0xFF);
        
        for (int pos = min; pos <= max; pos++) {
          allow_map[pos] = true;
        }
      }
      
      return allow_map;
    }
    
    public boolean allowed(int val)
    {
      return allow_map[val];
    }
  }
  
  public static class ReplacementFilter extends Filter
  {
    public final byte replacement_byte;
    public final char replacement_char;
    
    public ReplacementFilter(boolean[] allow_map, String replacement)
    {
      super(allow_map);
      
      if (!(replacement.length() == 1)) throw null;
      
      this.replacement_char = replacement.charAt(0);
      this.replacement_byte = ((byte)(replacement_char));
    }
    
    public ReplacementFilter(String specification, String replacement)
    {
      this(processSpecification(specification), replacement);
    }
    
    public void decimate(byte[] bin, int off, int lim)
    {
      while (off < lim) {
        if (!allowed((((int)(bin[off])) & 0xFF))) {
          bin[off] = replacement_byte;
        }
        
        off++;
      }
    }
    
    public void decimate(byte[] bin)
    {
      decimate(bin, 0, bin.length);
    }
  }
  
  private static boolean[] newAllowAllMap()
  {
    boolean[] allow_map = (new boolean[256]);
    Arrays.fill(allow_map, true);
    return allow_map;
  }
  
  public static final ReplacementFilter FILTER_ALLOW_ALL = (new ReplacementFilter(newAllowAllMap(), " "));
  public static final ReplacementFilter FILTER_PRINTABLE = (new ReplacementFilter(" ~", "~"));
  
  public static String bin2str(byte[] bin, int off, int lim, ReplacementFilter filter)
  {
    char[] str = (new char[(lim - off)]);
    
    for (int i = 0; i < str.length; i++) {
      int src = (((int)(bin[(off + i)])) & 0xFF);
      str[i] = (filter.allowed(src) ? ((char)(src)) : filter.replacement_char);
    }
    
    return (new String(str));
  }
  
  public static String bin2str(byte[] bin, int off, int lim)
  {
    return bin2str(bin, off, lim, FILTER_ALLOW_ALL);
  }
  
  public static String bin2str(byte[] bin, ReplacementFilter filter)
  {
    return bin2str(bin, 0, bin.length, filter);
  }
  
  public static String bin2str(byte[] bin)
  {
    return bin2str(bin, 0, bin.length, FILTER_ALLOW_ALL);
  }
  
  public static byte[] str2bin(String str)
  {
    byte[] bin = (new byte[str.length()]);
    
    for (int i = 0; i < bin.length; i++) {
      bin[i] = ((byte)((int)(str.charAt(i))));
    }
    
    return bin;
  }
  
  private static final String[] ZEROL_STRING = (new String[0]);
  
  public static String[] tokenize(String string, Filter filter)
  {
    ArrayList<String> out = (new ArrayList<String>());
    StringBuilder tmp = (new StringBuilder());
    
    int pos = 0;
    int len = string.length();
    
    while (pos < len) {
      while ((pos < len) && filter.allowed(string.charAt(pos))) {
        pos++;
      }
      
      if (pos < len) {
        while ((pos < len) && (!filter.allowed(string.charAt(pos)))) {
          tmp.append(string.charAt(pos));
          pos++;
        }
        
        out.add(tmp.toString());
        tmp.setLength(0);
      }
    }
    
    return out.toArray(ZEROL_STRING);
  }
}

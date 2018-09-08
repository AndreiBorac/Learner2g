/***
 * Settings.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class Settings
{
  private final AsciiTreeMap<String> inner = (new AsciiTreeMap<String>());
  
  public void scan(String prefix, String fil)
  {
    Scanner.populate(prefix, inner, fil);
  }
  
  public void scan(String prefix, Reader inp)
  {
    Scanner.populate(prefix, inner, inp);
  }
  
  public void scan(String prefix, InputStream inp)
  {
    Scanner.populate(prefix, inner, inp);
  }
  
  public boolean has(String key)
  {
    return inner.containsKey(key);
  }
  
  public String get(String key)
  {
    String val = inner.get(key);
    
    if (val == null) {
      throw (new RuntimeException("setting not found for '" + key + "'"));
    }
    
    return val;
  }
  
  public String getdef(String key, String def)
  {
    if (has(key)) {
      return get(key);
    } else {
      return def;
    }
  }
  
  public String toString()
  {
    StringBuilder out = (new StringBuilder());
    
    out.append("Settings[");
    
    for (Map.Entry<String, String> entry : inner.entrySet()) {
      out.append((" ('" + entry.getKey() + "' => '" + entry.getValue() + "')"));
    }
    
    out.append(" ]");
    
    return out.toString();
  }
  
  public static class Scanner
  {
    private enum State { EK, LK, EV, LV };
    
    public static void populate(final String pre, final Map<String, String> map, InputSource inp)
    {
      try {
        DefaultHandler handler =
          (new DefaultHandler()
            {
              final ArrayDeque<String> p = (new ArrayDeque<String>());
              
              final StringBuilder k = (new StringBuilder());
              final StringBuilder v = (new StringBuilder());
              
              StringBuilder x = null;
              
              State state = State.LV;
              
              public void startElement(String ign1, String ign2, String name, Attributes attributes)
              {
                if (name.equals("p")) {
                  if (!(state == State.LV)) throw (new RuntimeException("illegal state in SettingsScanner"));
                  state = State.LV;
                  
                  p.addLast(attributes.getValue("d"));
                }
                
                if (name.equals("k")) {
                  if (!(state == State.LV)) throw (new RuntimeException("illegal state in SettingsScanner"));
                  state = State.EK;
                  
                  k.setLength(0);
                  
                  k.append(pre);
                  
                  for (String e : p) {
                    k.append(e);
                    k.append('.');
                  }
                  
                  x = k;
                }
                
                if (name.equals("v")) {
                  if (!(state == State.LK)) throw (new RuntimeException("illegal state in SettingsScanner"));
                  state = State.EV;
                  
                  v.setLength(0);
                  
                  x = v;
                }
              }
              
              public void characters(char[] buf, int pos, int len)
              {
                if ((state == State.EK) || (state == State.EV)) {
                  x.append(buf, pos, len);
                }
              }
              
              public void endElement(String ign1, String ign2, String name)
              {
                if (name.equals("p")) {
                  if (!(state == State.LV)) throw null;
                  
                  p.removeLast();
                }
                
                if (name.equals("k")) {
                  if (!(state == State.EK)) throw null;
                  state = State.LK;
                }
                
                if (name.equals("v")) {
                  if (!(state == State.EV)) throw null;
                  state = State.LV;
                  
                  map.put(k.toString(), v.toString());
                }
              }
            });
        
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(inp, handler);
      } catch (Exception e) {
        throw (new RuntimeException(e));
      }
    }
    
    public static void populate(final String pre, final Map<String, String> map, Reader inp)
    {
      populate(pre, map, (new InputSource(inp)));
    }
    
    public static void populate(final String pre, final Map<String, String> map, InputStream inp)
    {
      try {
        populate(pre, map, (new InputStreamReader(inp, "UTF-8")));
      } catch (IOException e) {
        throw (new RuntimeException(e));
      }
    }
    
    public static void populate(final String pre, final Map<String, String> map, final String fil)
    {
      try {
        populate(pre, map, (new FileInputStream(fil)));
      } catch (IOException e) {
        throw (new RuntimeException(e));
      }
    }
    
    public static void populate(final Map<String, String> map, final InputStream inp)
    {
      populate("", map, inp);
    }
    
    public static void populate(final Map<String, String> map, final String fil)
    {
      populate("", map, fil);
    }
  }
}

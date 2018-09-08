/***
 * ResourceTracker.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.util.*;

public class ResourceTracker
{
  private static final boolean debug = true;
  
  public static class Token { }
  
  static HashMap<Token, String> tokens = (new HashMap<Token, String>());
  
  public synchronized static Token acquire(String description)
  {
    Token token;
    
    tokens.put((token = (new Token())), (description = ("@" + MicroTime.now() + ":" + description)));
    
    if (debug) {
      Log.log("ResourceTracker allocated " + description);
    }
    
    return token;
  }
  
  public synchronized static void release(Token token)
  {
    String description;
    
    if ((description = tokens.remove(token)) == null) throw null;
    
    if (debug) {
      Log.log("ResourceTracker released " + description + ", remaining: " + list());
    }
  }
  
  public synchronized static int count()
  {
    return tokens.size();
  }
  
  public synchronized static String list()
  {
    StringBuilder out = (new StringBuilder());
    
    for (Map.Entry<Token, String> entry : tokens.entrySet()) {
      out.append(", ");
      out.append(entry.getValue());
    }
    
    if (out.length() >= 2) {
      out.delete(0, 2);
    }
    
    return out.toString();
  }
}

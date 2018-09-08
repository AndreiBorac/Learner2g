/***
 * Test.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.station2g;

import java.util.*;

public class Test
{
  static class TestLocate
  {
    private static int locate_simple(byte[] buf, int off, int lim, byte[] pat)
    {
      for (int pos = off; pos <= lim; pos++) {
        boolean success = true;
        
        for (int elm = 0; elm < pat.length; elm++) {
          if ((pos + elm) < lim) {
            if (buf[pos + elm] != pat[elm]) {
              success = false;
            }
          } else {
            success = false;
          }
        }
        
        if (success) {
          return pos;
        }
      }
      
      return -1;
    }
    
    static final Random random = (new Random());
    
    static void test(int n)
    {
      for (int i = 0; i < n; i++) {
        byte[] a = (new byte[random.nextInt(16)]);
        byte[] b = (new byte[random.nextInt(16)]);
        
        for (int j = 0; j < a.length; j++) { a[j] = ((byte)('0' + random.nextInt(4))); }
        for (int j = 0; j < b.length; j++) { b[j] = ((byte)('0' + random.nextInt(4))); }
        
        int lim = random.nextInt(a.length + 1);
        int off = random.nextInt(lim + 1);
        
        if (lim < off) {
          int tmp = off;
          off = lim;
          lim = off;
        }
        
        //System.err.println("a: '" + (new String(a)) + "', off=" + off + ", lim=" + lim + ", b: '" + (new String(b)) + "'");
        
        int r1 = Station2g.Server.locate(a, off, lim, b);
        int r2 = locate_simple(a, off, lim, b);
        
        if (r1 != r2) {
          //System.err.println("r1=" + r1 + ", r2=" + r2);
          throw null;
        }
      }
    }
    
    public static void main(String[] args)
    {
      for (int i = 0; i < 100; i++) {
        test(100000);
      }
    }
  }
}
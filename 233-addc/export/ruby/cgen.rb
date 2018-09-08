#!/usr/bin/env ruby
# copyright (c) 2012 by andrei borac

# encoding bits (mulaw 8)
B = 10;

# primordial interval width (mulaw 8)
W = 1.0 / (2 ** 4);

# 256 possible byte values
# let 0 be 0
# let [+1..+127] encode [+1..+RANGE_UPPER]
# let [-1..-128] encode [-1..-RANGE_LOWER]

RANGE_UPPER = 32767;
RANGE_LOWER = 32768;

IVALS_UPPER = ((1 << (B - 1)) - 1);
IVALS_LOWER = ((1 << (B - 1))    );

# e - end of primordial interval (inclusive)
# w - width of primordial interval
# a - width amplifier
# s - stop value
def genval(e, w, a, s)
  w, a, = [ w, a, ].map{ |x| x.to_f; };
  
  o = [];
  
  e = e; # end of previous interval (inclusive)
  
  while (e < s)
    c = e;
    w = a * w;
    e = e + [ 1, w.round ].max;
    o << e;
  end
  
  return o;
end

# e - end of primordial interval (inclusive)
# w - width of primordial interval
# s - stop value
# n - code limit
def getamp(e, w, s, n)
  a_lo = 1.0;
  a_hi = s.to_f;
  
  while ((a_hi - a_lo) > 0.000000001)
    a_md = ((a_lo + a_hi) / 2);
    
    if (genval(e, w, a_md, s).length > n)
      a_lo = a_md;
    else
      a_hi = a_md;
    end
  end
  
  return a_hi;
end

a_upper = getamp(1, W, RANGE_UPPER, IVALS_UPPER);
a_lower = getamp(0, W, RANGE_LOWER, IVALS_LOWER);

$stderr.puts("a_upper=#{a_upper.inspect}");
$stderr.puts("a_lower=#{a_lower.inspect}");

x_upper = genval(1, W, a_upper, RANGE_UPPER);
x_lower = genval(0, W, a_lower, RANGE_LOWER);

$stderr.puts("x_upper[#{x_upper.length}]=#{x_upper.inspect}");
$stderr.puts("x_lower[#{x_lower.length}]=#{x_lower.inspect}");

raise if (!(x_upper[-1] == RANGE_UPPER));
raise if (!(x_lower[-1] == RANGE_LOWER));

require("digest");

s_upper, s_lower, = [ x_upper, x_lower, ].map{ |i| Digest::SHA2.new.update(i.join(", ")); }

$stderr.puts(s_upper);
$stderr.puts(s_lower);

raise if (!(s_upper == "b66d5798cd7b2bf16959c4176574131fb5a9e2f4781d119caf1957eeaae425d9"));
raise if (!(s_lower == "7963d289f90920e877f6900ea1ac3a79b1c7393cfd902b1e764ad5774ee695f0"));

File.open("./export/cgen.out", "w"){ |f|
  [ a_upper, a_lower, x_upper, x_lower, ].each{ |x|
    f.puts(x.inspect);
  };
};

BOILERPLATE_SOURCE = <<"EOF"

package zs42.addc;

import java.util.Random;

public class Mulaw
{
  public static final int B = #{B.to_s};
  
  public static class Enlaw
  {
    static final int[] enlaw = (new int[(1 << Short.SIZE)]);
    
    // interpret a linear sample as unsigned
    static int adapt(int x)
    {
      return (x & ((1 << Short.SIZE) - 1));
    }
    
    static int basic_enlaw(int[] x_table, int x)
    {
      int i = 0;
      while (x > x_table[i++]);
      return i;
    }
    
    static int basic_enlaw(int x)
    {
      /****/ if (x > 0) {
        return (+basic_enlaw(x_upper, +x));
      } else if (x < 0) {
        return (-basic_enlaw(x_lower, -x));
      } else {
        return 0;
      }
    }
    
    static
    {
      for (int x = Short.MIN_VALUE; x <= Short.MAX_VALUE; x++) {
        enlaw[adapt(x)] = basic_enlaw(x);
      }
    }
    
    public static int enlaw(short x)
    {
      int i = adapt(x);
      return enlaw[i];
    }
  }
  
  public static class Unlaw
  {
    static final int[] unlaw_start = (new int[(1 << B)]);
    static final int[] unlaw_width = (new int[(1 << B)]);
    
    // interpret a mulaw code as unsigned
    static int adapt(int x)
    {
      return (x & ((1 << B) - 1));
    }
    
    static
    {
      unlaw_start[0] = 0;
      unlaw_width[0] = 1;
      
      for (int i = 1; i <= #{IVALS_UPPER}; i++) {
        unlaw_start[adapt(((short)(+i)))] = (unlaw_start[i-1] + unlaw_width[i-1]);
        unlaw_width[adapt(((short)(+i)))] = (x_upper[i-1] - unlaw_start[i]);
        
        /*
        if (i < 16) {
          System.err.println("unlaw_start[i=" + i + "]=" + unlaw_start[i]);
          System.err.println("unlaw_width[i=" + i + "]=" + unlaw_width[i]);
        }
        */
      }
      
      for (int i = 1; i <= #{IVALS_LOWER}; i++) {
        unlaw_start[adapt(((short)(-i)))] = -x_lower[i-1];
        unlaw_width[adapt(((short)(-i)))] = (unlaw_start[adapt(((short)(-(i-1))))] - unlaw_start[adapt(((short)(-i)))]);
        
        /*
        if (i < 16) {
          System.err.println("unlaw_start[-i=" + i + "]=" + unlaw_start[adapt(((short)(-i)))]);
          System.err.println("unlaw_width[-i=" + i + "]=" + unlaw_width[adapt(((short)(-i)))]);
        }
        */
      }
      
      /*
      for (int i = 0; i < (1 << B); i++) {
        System.err.println("unlaw_start[i=" + i + "]=" + unlaw_start[i]);
        System.err.println("unlaw_width[i=" + i + "]=" + unlaw_width[i]);
      }
      */
    }
    
    public static short unlaw(int x)
    {
      int i = adapt(x);
      return ((short)(unlaw_start[i]));
    }
    
    public static short unlaw(int x, boolean m)
    {
      int i = adapt(x);
      return ((short)(unlaw_start[i] + (unlaw_width[i] >> 1)));
    }
    
    public static short unlaw(int x, Random r)
    {
      int i = adapt(x);
      return ((short)(unlaw_start[i] + r.nextInt(unlaw_width[i])));
    }
  }
  
  <X/>;
}
EOF
;

BOILERPLATE_ENTER, BOILERPLATE_LEAVE, = BOILERPLATE_SOURCE.split("<X/>;");

File.open("java/zs42/addc/Mulaw.java", "w") { |f|
  f.puts(BOILERPLATE_ENTER);
  f.puts("  static final int[] x_upper = { #{x_upper.join(", ")} };");
  f.puts("  static final int[] x_lower = { #{x_lower.join(", ")} };");
  f.puts(BOILERPLATE_LEAVE);
};

$stderr.puts("+OK (cgen.rb)");

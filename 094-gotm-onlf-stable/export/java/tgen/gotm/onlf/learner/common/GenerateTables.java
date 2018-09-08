package tgen.gotm.onlf.learner.common;

import java.io.*;

public class GenerateTables
{
  public static byte[] enlaw = null;
  public static short[] unlaw = null;

  static void build_unlaw_table()
  {
    final double MU = 255.0;
    final double ONE_PLUS_MU = 1.0 + MU;
    final double INV_MU = 1.0 / MU;
    double y, sign, ay, fval;

    unlaw = new short[0x100];

    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
      y = 2.0 * ((double)i) / ONE_PLUS_MU;
      sign = (i < 0) ? -1.0 : 1.0;
      ay = Math.abs(y);
      fval = sign * INV_MU * (Math.pow(ONE_PLUS_MU, ay) - 1.0);

      // System.out.println("i y ay fval: " + i + " " + y + " " + ay + " " + fval); 

      unlaw[i & 0xFF] = (short)Math.round(((double)0x8000) * fval);
    }
  }

  static void build_enlaw_table()
  {
    enlaw = new byte[0x10000];

    boolean choose_below = true;
    for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      int idx_below = 0, idx_above = 0, idx = 0;
      double lse_below = Double.MAX_VALUE;
      double lse_above = Double.MAX_VALUE;
      double error = 0;
      for (int j = Byte.MIN_VALUE; j <= Byte.MAX_VALUE; j++) {
        int entry = unlaw[j & 0xFF];
        error = i - entry;
        double sq_error = error * error;
        if (error >= 0) {
          if (sq_error < lse_below) {
            idx_below = j;
            lse_below = sq_error;
          }
        } else {
          if (sq_error < lse_above) {
            idx_above = j;
            lse_above = sq_error;
          }
        }
      }
      if (lse_below < lse_above) {
        idx = (byte)(idx_below);
      } else if (lse_below == lse_above) {
        idx = (byte)(choose_below ? idx_below : idx_above);
        choose_below = !choose_below;
      } else {
        idx = (byte)(idx_above);
      }
      // DBG
      // System.out.format("p: %6d  ib: %4d  pb: %6d  eb: %8g  ia: %6d  pa: %6d  ea: %8g  id: %4d\n",
      //                   i,
      //                   idx_below, unlaw[idx_below & 0xFF], lse_below,
      //                   idx_above, unlaw[idx_above & 0xFF], lse_above,
      //                   idx);

      enlaw[i & 0xFFFF] = (byte)idx;
    }
  }

  static double eval_error_metric()
  {
    double error_metric = 0.0;
    for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      double error = unlaw[enlaw[i & 0xFFFF] & 0xFF] - i;
      error = error * error;
      error_metric += error;
    }
    return error_metric;
  }

  public static void main(String[] args)
  {
    try {
      build_unlaw_table();
      build_enlaw_table();

      // DBG
      // System.out.println("unlaw");
      // for (int i = 0; i < 0x100; i++)
      //   System.out.format("i: %3d  l: %4d  p: %6d\n", i, (byte)i, unlaw[i]);

      // System.out.println("enlaw");
      // for (int i = 0; i < 0x10000; i++)
      //   System.out.format("i: %5d  p: %6d  l: %4d\n", i, (short)i, enlaw[i]);

      // System.out.println();
      // System.out.println("error: " + eval_error_metric());

      //PrintStream stream = new PrintStream(new File("AudioCommonTables.java"));

      PrintStream stream = System.out;

      stream.println("// Automatically generated file. Do not edit.");
      stream.println();
      stream.println("package gotm.onlf.learner.common;");
      stream.println();
      stream.println("public class AudioCommonTables");
      stream.println("{");
      stream.println("public static byte[] enlaw = new byte[0x10000];");

      stream.println("public static final short[] unlaw = new short[] {");
      for (int i = 0; i < 0x100; i++) {
        stream.format(" %6d%s", unlaw[i], (i < 0xFF ? "," : "") + (i % 8 == 7 ? "\n" : ""));
      }
      stream.println("};");

      for (int i = 0; i < 8; i++) {
        String classname = "EnlawTableSlice" + i;
        stream.println("static class " + classname);
        stream.println("{");

        stream.println("static final byte[] slice = new byte[] {");
        int begin_idx = 0x2000 * i;
        int end_idx = 0x2000 * (i+1);
        for (int j = begin_idx; j < end_idx; j++) {
          stream.format(" %4d%s", enlaw[j], (j < end_idx - 1 ? "," : "") + (j % 16 == 15 ? "\n" : ""));
        }
        stream.println(" };");
        stream.println();
        stream.println("public static void fill()");
        stream.println("{");
        stream.println("for (int k = 0; k < slice.length; k++) {");
        stream.println("enlaw[k + " + begin_idx + "] = slice[k];");
        stream.println("} // for");
        stream.println();
        stream.println("} // fill");
        stream.println();
        
        stream.println("} // class" + classname);
        stream.println();
      }

      stream.println("static {");
      for (int i = 0; i < 8; i++) {
        stream.println("EnlawTableSlice" + i + ".fill();");
      }
      stream.println("}");
      stream.println();

      stream.println("}");
    } catch (Exception e) {
      // TODO
    }
  }
}

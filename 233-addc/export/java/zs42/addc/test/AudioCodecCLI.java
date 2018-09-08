/***
 * AudioCodecCLI.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.addc.test;

import zs42.addc.*;

import java.io.*;
import java.util.*;

public class AudioCodecCLI
{
  public static void main(String[] args) throws Exception
  {
    final Random random = (new Random());
    
    DataInputStream  dis = (new DataInputStream  (new FileInputStream  (args[1])));
    DataOutputStream dos = (new DataOutputStream (new FileOutputStream (args[2])));
    
    try {
      /****/ if (args[0].equals("debug")) {
        while (true) {
          short inp = dis.readShort();
          short law = ((short)(Mulaw.Enlaw.enlaw(inp)));
          short out = Mulaw.Unlaw.unlaw(law);
          double dif = ((out - inp) / (inp + 0.1));
          
          if (Math.abs(dif) > 0.05) {
            System.out.println("dif=" + ((out - inp) / (inp + 0.1)) + ", inp=" + inp + ", law=" + law + ", out=" + out);
          }
        }
      } else if (args[0].equals("samp1")) {
        /* resample by averaging */
        
        int width = Integer.parseInt(args[3]);
        
        while (true) {
          int sum = 0;
          
          for (int i = 0; i < width; i++) {
            sum += ((short)(dis.readShort()));
          }
          
          dos.writeShort((sum / width));
        }
      } else if (args[0].equals("samp2")) {
        /* resample by decimating */
        
        int width = Integer.parseInt(args[3]);
        
        while (true) {
          dos.writeShort(dis.readShort());
          
          for (int i = 1; i < width; i++) {
            dis.readShort();
          }
        }
      } else if (args[0].equals("enlaw")) {
        while (true) {
          dos.writeShort(Mulaw.Enlaw.enlaw(dis.readShort()));
        }
      } else if (args[0].equals("unlaw")) {
        while (true) {
          dos.writeShort(Mulaw.Unlaw.unlaw(dis.readShort()));
        }
      } else if (args[0].equals("melaw")) {
        while (true) {
          dos.writeShort(Mulaw.Unlaw.unlaw(dis.readShort(), true));
        }
      } else if (args[0].equals("delaw")) {
        while (true) {
          dos.writeShort(Mulaw.Unlaw.unlaw(dis.readShort(), random));
        }
      } else {
        throw null;
      }
    } catch (EOFException e) {
      dos.close();
    }
  }
}

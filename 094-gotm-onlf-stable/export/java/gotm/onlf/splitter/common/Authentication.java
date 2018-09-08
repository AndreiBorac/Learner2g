/***
 * Authentication.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.common;

import zs42.parts.*;

import static gotm.onlf.utilities.Utilities.SHAZ;
import static gotm.onlf.utilities.Utilities.join_bytes;
import static gotm.onlf.utilities.Utilities.csum_bytes;
import static gotm.onlf.utilities.Utilities.secure_random;
import static gotm.onlf.utilities.Utilities.test_equal_bytes;

public class Authentication
{
  public static void challenge(IncomingNetworkStream inp, OutgoingNetworkStream out, byte[] kmac)
  {
    inp.assure_turned();
    out.assure_turned();
    
    while (true) {
      byte[] send = secure_random(kmac);
      out.wB(send);
      out.writeback();
      
      byte[] recv = new byte[SHAZ];
      inp.readahead(SHAZ);
      inp.rB(recv);
      inp.turn();
      
      byte[] calc = csum_bytes(join_bytes(kmac, send));
      
      if (test_equal_bytes(recv, calc)) {
        out.wL(1);
        out.writeback();
        break;
      } else {
        out.wL(0);
        out.writeback();
        continue;
      }
    }
  }
  
  public static boolean response(IncomingNetworkStream inp, OutgoingNetworkStream out, byte[] kmac)
  {
    byte[] chal = new byte[SHAZ];
    inp.readahead(SHAZ);
    inp.rB(chal);
    inp.turn();
    
    byte[] calc = csum_bytes(join_bytes(kmac, chal));
    out.wB(calc);
    out.writeback();
    
    inp.readahead(8);
    long good = inp.rL();
    inp.turn();
    
    return (good == 1);
  }
}

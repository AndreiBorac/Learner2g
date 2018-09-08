/***
 * GroupirPacket.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.common;

import java.util.*;

import zs42.parts.*;

import static gotm.onlf.utilities.Utilities.*;

public class GroupirPacket
{
  public static final int DIMC = 4;
  public static final int DIMZ = DIMC * 2;
  public static final int ESCZ = ((1 << Short.SIZE) - 1);
  
  public int     mZ; // payload length, without padding
  public long[]  dL;
  public int[]   dI;
  public short[] dS;
  public byte[]  dB;
  
  public GroupirPacket(int iZ, long[] iL, int[] iI, short[] iS, byte[] iB)
  {
    mZ = iZ;
    dL = iL;
    dI = iI;
    dS = iS;
    dB = iB;
  }
  
  public GroupirPacket(long[] iL, int[] iI, short[] iS, byte[] iB)
  {
    this(measure(iL, iI, iS, iB), iL, iI, iS, iB);
  }
  
  public static int measure(long[] iL, int[] iI, short[] iS, byte[] iB)
  {
    int iZ = 0;
    
    iZ <<= 1; iZ += iL.length;
    iZ <<= 1; iZ += iI.length;
    iZ <<= 1; iZ += iS.length;
    iZ <<= 1; iZ += iB.length;
    
    return iZ;
  }
  
  public static GroupirPacket recv(IncomingNetworkStream inp, byte[] kmac, boolean always_trust)
  {
    final int ADNZ = 8;
    
    inp.readahead(((kmac == null) ? 0 : SHAZ) + DIMZ);
    
    byte[] cmac = new byte[SHAZ];
    if (kmac != null) inp.rB(cmac);
    
    int pos_enter_packet = inp.pos;
    
    int[] dims = new int[DIMC];
    
    // read basic dimensions
    {
      inp.readahead(DIMZ);
      
      for (int i = 0; i < DIMC; i++) {
        dims[i] = inp.rS();
      }
    }
    
    // read additional dimensions
    {
      int addn = 0;
      
      for (int i = 0; i < DIMC; i++) {
        if (dims[i] == ((1 << Short.SIZE) - 1)) {
          addn += ADNZ;
        }
      }
      
      inp.readahead(addn);
      
      for (int i = 0; i < DIMC; i++) {
        if (dims[i] == ESCZ) {
          dims[i] = (int)(inp.rL());
        }
      }
    }
    
    int mZ = 0;
    
    // calculate payload size
    {
      for (int i = 0; i < DIMC; i++) {
        mZ <<= 1;
        mZ += dims[i];
      }
    }
    
    inp.readahead((mZ + 0x7) & ~0x7);
    
    long[]  dL = new long  [dims[0]];
    int[]   dI = new int   [dims[1]];
    short[] dS = new short [dims[2]];
    byte[]  dB = new byte  [dims[3]];
    
    inp.rL(dL);
    inp.rI(dI);
    inp.rS(dS);
    inp.rB(dB);
    
    for (int i = 0; i < ((-mZ) & 0x7); i++) {
      inp.rB();
    }
    
    int pos_leave_packet = inp.pos;
    
    // check digest
    if (kmac != null) {
      byte[] csum = csum_bytes(inp.buf, pos_enter_packet, pos_leave_packet - pos_enter_packet);
      byte[] calc = csum_bytes(join_bytes(kmac, csum));
      
      if (!test_equal_bytes(cmac, calc)) {
        if (!always_trust) {
          throw (new BAD());
        }
      }
    }
    
    return (new GroupirPacket(mZ, dL, dI, dS, dB));
  }
  
  public static GroupirPacket recv(IncomingNetworkStream inp, byte[] kmac)
  {
    return recv(inp, kmac, false);
  }
  
  private static final byte[] zero_kmac = (new byte[SHAZ]);
  
  public static GroupirPacket recv_from_trusted_source(IncomingNetworkStream inp)
  {
    return recv(inp, zero_kmac, true);
  }
  
  public byte[] send(byte[] kmac)
  {
    DataQueue out = new DataQueue((mZ & ~0x7) + 512, throw_F0);
    
    byte[] cmac = new byte[SHAZ];
    if (kmac != null) out.wB(cmac); // incomplete!
    
    int pos_enter_packet = out.lim;
    
    int[] dims = new int[DIMC];
    
    dims[0] = dL.length;
    dims[1] = dI.length;
    dims[2] = dS.length;
    dims[3] = dB.length;
    
    for (int i = 0; i < DIMC; i++) {
      if (dims[i] < ESCZ) {
        out.wS(dims[i]);
      } else {
        out.wS(ESCZ);
      }
    }
    
    for (int i = 0; i < DIMC; i++) {
      if (dims[i] >= ESCZ) {
        out.wL(dims[i]);
      }
    }
    
    out.wL(dL);
    out.wI(dI);
    out.wS(dS);
    out.wB(dB);
    
    out.wB(cmac, 0, ((-mZ) & 0x7));
    
    int pos_leave_packet = out.lim;
    
    if (kmac != null) {
      byte[] csum = csum_bytes(out.buf, pos_enter_packet, pos_leave_packet - pos_enter_packet);
      byte[] calc = csum_bytes(join_bytes(kmac, csum));
      
      for (int i = 0; i < SHAZ; i++) {
        out.buf[i] = calc[i];
      }
    }
    
    byte[] retv = (new byte[pos_leave_packet]);
    for (int i = 0; i < retv.length; i++) retv[i] = out.buf[i];
    return retv;
  }
}

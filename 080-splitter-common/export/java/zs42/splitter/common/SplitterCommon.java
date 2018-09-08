/***
 * SplitterCommon.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.splitter.common;

import java.io.*;
import java.security.*;

import zs42.mass.*;
import zs42.buff.*;

public class SplitterCommon
{
  public static final int TRANSPORT_BUFFER_SIZE  = ((1 << 16));
  public static final int MAXIMUM_PACKET_PAYLOAD = ((1 << 16) - (1 << 9));
  
  public static class Utilities
  {
    static final long adjustment;
    
    static
    {
      long value = -1;
      
      for (int i = 0; i < 100; i++) {
        long ms = System.currentTimeMillis();
        long ns = System.nanoTime();
        
        long expected_ns = ms * 1000000;
        
        value = (expected_ns - ns);
      }
      
      if (value == -1) throw null;
      
      adjustment = value;
    }
    
    public static long microTime()
    {
      return ((System.nanoTime() + adjustment) / 1000);
    }
  }
  
  public static class Groupir
  {
    public static class Packet
    {
      public Buffer.xJ bufJ;
      public Buffer.xI bufI;
      public Buffer.xS bufS;
      public Buffer.xB bufB;
      
      public Packet(BufferCentral central, boolean wantJ, boolean wantI, boolean wantS, boolean wantB)
      {
        if (wantJ) this.bufJ = central.acquireJ();
        if (wantI) this.bufI = central.acquireI();
        if (wantS) this.bufS = central.acquireS();
        if (wantB) this.bufB = central.acquireB();
      }
      
      public void recv(PacketInputStream inp)
      {
        Buffer.nJ outJ_n = null;
        Buffer.nI outI_n = null;
        Buffer.nS outS_n = null;
        Buffer.nB outB_n = null;
        
        if (bufJ != null) outJ_n = bufJ.append();
        if (bufI != null) outI_n = bufI.append();
        if (bufS != null) outS_n = bufS.append();
        if (bufB != null) outB_n = bufB.append();
        
        inp.recv(outJ_n, outI_n, outS_n, outB_n);
        
        if (outJ_n != null) outJ_n.release();
        if (outI_n != null) outI_n.release();
        if (outS_n != null) outS_n.release();
        if (outB_n != null) outB_n.release();
      }
      
      public void send(PacketOutputStream out, boolean flush)
      {
        Buffer.oJ inpJ_o = null;
        Buffer.oI inpI_o = null;
        Buffer.oS inpS_o = null;
        Buffer.oB inpB_o = null;
        
        if (bufJ != null) inpJ_o = bufJ.iterate();
        if (bufI != null) inpI_o = bufI.iterate();
        if (bufS != null) inpS_o = bufS.iterate();
        if (bufB != null) inpB_o = bufB.iterate();
        
        out.send(inpJ_o, inpI_o, inpS_o, inpB_o, flush);
        
        if (inpJ_o != null) inpJ_o.release();
        if (inpI_o != null) inpI_o.release();
        if (inpS_o != null) inpS_o.release();
        if (inpB_o != null) inpB_o.release();
      }
      
      public void release()
      {
        if (bufJ != null) { bufJ.release(); bufJ = null; }
        if (bufI != null) { bufI.release(); bufI = null; }
        if (bufS != null) { bufS.release(); bufS = null; }
        if (bufB != null) { bufB.release(); bufB = null; }
      }
    }
    
    public static abstract class Authentication
    {
      public static byte[] load_key_file(String filename)
      {
        byte[] out = (new byte[ChecksumAssistant.KMAC.SHAZ]);
        
        try {
          DataInputStream inp = (new DataInputStream(new FileInputStream(filename)));
          inp.readFully(out);
          inp.close();
        } catch (IOException e) {
          throw (new RuntimeException(e));
        }
        
        return out;
      }
      
      public static PacketInputStream connect_user(BufferCentral central, InputStream raw_inp, OutputStream raw_out, byte[] user_pass)
      {
        ChecksumAssistant.KMAC kmac = (new ChecksumAssistant.KMAC(user_pass));
        PacketInputStream inp = (new PacketInputStream(central, kmac, raw_inp));
        
        {
          Buffer.xB chal = central.acquireB();
          
          // read challenge
          {
            Buffer.nB chal_n = chal.prepend();
            {
              inp.require(kmac.checksumLength());
              {
                Buffer.oB buffer_o = inp.buffer.iterate();
                {
                  Buffer.sB.alias(chal_n, buffer_o, kmac.checksumLength());
                }
                buffer_o.release();
              }
              inp.truncate(kmac.checksumLength());
            }
            chal_n.release();
          }
          
          Buffer.xB csum = central.acquireB();
          
          // calculate authenticator
          {
            Buffer.nB csum_n = csum.prepend();
            {
              Buffer.oB chal_o = chal.iterate();
              {
                kmac.calculateChecksum(central, csum_n, chal_o, kmac.checksumLength());
              }
              chal_o.release();
            }
            csum_n.release();
          }
          
          // supply authenticator
          {
            try {
              raw_out.write(csum.toNewArrayB());
              raw_out.flush();
            } catch (IOException e) {
              throw (new RuntimeException(e));
            }
          }
          
          csum.release();
          chal.release();
        }
        
        return inp;
      }
      
      public static PacketOutputStream connect_root(BufferCentral central, OutputStream raw_out, byte[] root_pass)
      {
        ChecksumAssistant.KMAC kmac = (new ChecksumAssistant.KMAC(root_pass));
        PacketOutputStream out = (new PacketOutputStream(central, kmac, raw_out));
        
        return out;
      }
    }
    
    public static abstract class ChecksumAssistant
    {
      public abstract int checksumLength();
      public abstract void calculateChecksum(BufferCentral central, Buffer.nB csum, Buffer.oB data, int data_length);
      public abstract boolean verifyChecksum(BufferCentral central, Buffer.oB csum, Buffer.oB data, int data_length);
      
      public static class NONE extends ChecksumAssistant
      {
        public NONE()
        {
          // nothing to do
        }
        
        public int checksumLength()
        {
          return 0;
        }
        
        public void calculateChecksum(BufferCentral central, Buffer.nB csum, Buffer.oB data, int data_length)
        {
          throw null;
        }
        
        public boolean verifyChecksum(BufferCentral central, Buffer.oB csum, Buffer.oB data, int data_length)
        {
          throw null;
        }
      }
      
      public static class SHA2 extends NONE
      {
        public static final int SHAZ = 256/8;
        final MessageDigest sha;
        
        {
          try {
            sha = MessageDigest.getInstance("SHA-256");
          } catch (NoSuchAlgorithmException e) {
            throw (new RuntimeException(e));
          }
        }
        
        public SHA2()
        {
          // nothing to do
        }
        
        public int checksumLength()
        {
          return SHAZ;
        }
        
        protected final boolean compare0(Buffer.oB csum_o, byte[] calc)
        {
          int diff = 0;
          
          for (int i = 0; i < SHAZ; i++) {
            diff |= (csum_o.rB() ^ calc[i]);
          }
          
          return (diff == 0);
        }
        
        protected byte[] calculateChecksum0(BufferCentral central, Buffer.oB data_o, int data_length)
        {
          if (data_length > data_o.remaining()) throw null;
          
          while (data_length > 0) {
            data_o.dma_enter();
            int off = data_o.dma_off();
            int len = data_o.dma_lim() - off;
            if (len > data_length) len = data_length;
            sha.update(data_o.dma_bak(), off, len);
            data_o.dma_leave(len);
            data_length -= len;
          }
          
          return sha.digest();
        }
        
        public void calculateChecksum(BufferCentral central, Buffer.nB csum, Buffer.oB data, int data_length)
        {
          Buffer.sB.copy(csum, calculateChecksum0(central, data, data_length), 0, SHAZ);
        }
        
        public boolean verifyChecksum(BufferCentral central, Buffer.oB csum, Buffer.oB data, int data_length)
        {
          return compare0(csum, calculateChecksum0(central, data, data_length));
        }
      }
      
      public static class KYES extends SHA2
      {
        public KYES()
        {
          // nothing to do
        }
        
        public int checksumLength()
        {
          return SHAZ;
        }
        
        public void calculateChecksum(BufferCentral central, Buffer.nB csum, Buffer.oB data, int data_length)
        {
          throw null;
        }
        
        public boolean verifyChecksum(BufferCentral central, Buffer.oB csum, Buffer.oB data, int data_length)
        {
          return true;
        }
      }
      
      public static class KMAC extends SHA2
      {
        byte[] kmac;
        
        public KMAC(byte[] kmac)
        {
          if (kmac.length != SHAZ) throw null;
          
          this.kmac = kmac;
        }
        
        protected byte[] calculateChecksum0(BufferCentral central, Buffer.oB data, int data_length)
        {
          byte[] calc;
          
          {
            Buffer.xB join = central.acquireB();
            
            {
              Buffer.nB join_n = join.prepend();
              {
                Buffer.sB.copy(join_n, kmac, 0, SHAZ);
                Buffer.sB.copy(join_n, super.calculateChecksum0(central, data, data_length), 0, SHAZ);
              }
              join_n.release();
              join_n = null;
            }
            
            {
              Buffer.oB join_o = join.iterate();
              calc = super.calculateChecksum0(central, join_o, join_o.remaining());
              join_o.release();
              join_o = null;
            }
            
            join.release();
            join = null;
          }
          
          return calc;
        }
      }
    }
    
    public static class PacketInputStream
    {
      final BufferCentral central;
      final ChecksumAssistant assistant;
      final InputStream inp;
      
      Buffer.xB buffer;
      
      public PacketInputStream(BufferCentral central, ChecksumAssistant assistant, InputStream inp)
      {
        this.central = central;
        this.assistant = assistant;
        this.inp = inp;
        this.buffer = central.acquireB();
      }
      
      /***
       * reads until the buffer contains at least <code>amount</code>
       * unprocessed bytes.
       ***/
      void require(int amount)
      {
        int buffer_length = buffer.length();
        
        if (amount > buffer_length) {
          amount -= buffer_length;
          
          Buffer.nB n = buffer.append();
          {
            while (amount > 0) {
              int got;
              
              n.dma_enter();
              {
                try {
                  got = inp.read(n.dma_bak(), n.dma_off(), (n.dma_lim() - n.dma_off()));
                  if (got <= 0) throw (new EOFException());
                } catch (IOException e) {
                  throw (new RuntimeException(e));
                }
              }
              n.dma_leave(got);
              
              amount -= got;
            }
          }
          n.release();
          n = null;
        }
      }
      
      /***
       * set <code>buffer</code> to a new buffer that excludes the
       * first <code>buffer_offset</code> bytes from the buffer.
       ***/
      void truncate(int buffer_offset)
      {
        Buffer.xB buffer_prime = central.acquireB();
        
        {
          Buffer.nB buffer_prime_n = buffer_prime.prepend();
          {
            Buffer.oB buffer_o = buffer.iterate();
            buffer_o.skip(buffer_offset);
            Buffer.sB.alias(buffer_prime_n, buffer_o, buffer_o.remaining());
            buffer_o.release();
            buffer_o = null;
          }
          buffer_prime_n.release();
          buffer_prime_n = null;
        }
        
        buffer.release();
        buffer = null;
        
        buffer = buffer_prime;
      }
      
      /***
       * receives a packet. an argument may be null if there are not
       * any primitives of that type in the packet.
       ***/
      public void recv(Buffer.nJ outJ_n, Buffer.nI outI_n, Buffer.nS outS_n, Buffer.nB outB_n)
      {
        int buffer_offset = 0;
        
        Buffer.xB csum = null;
        
        if (assistant.checksumLength() > 0) {
          csum = central.acquireB();
          
          require(buffer_offset + assistant.checksumLength());
          {
            Buffer.oB buffer_o = buffer.iterate();
            buffer_o.skip(buffer_offset);
            {
              Buffer.nB csum_n = csum.prepend();
              Buffer.sB.copy(csum_n, buffer_o, assistant.checksumLength());
              csum_n.release();
              csum_n = null;
            }
            buffer_o.release();
            buffer_o = null;
          }
          buffer_offset += assistant.checksumLength();
        }
        
        int buffer_offset_enter_packet = buffer_offset;
        
        int nrB;
        int nrS;
        int nrI;
        int nrJ;
        
        // read size descritors
        {
          final int AMTZ = 4 * 2;
          
          require(buffer_offset + AMTZ);
          
          Buffer.xS desc = central.acquireS();
          
          {
            Buffer.nS desc_n = desc.prepend();
            {
              Buffer.oB buffer_o = buffer.iterate();
              buffer_o.skip(buffer_offset);
              Buffer.sS.decode(desc_n, buffer_o, AMTZ/2);
              buffer_offset += AMTZ;
              buffer_o.release();
              buffer_o = null;
            }
            desc_n.release();
            desc_n = null;
          }
          
          {
            Buffer.oS desc_o = desc.iterate();
            {
              nrJ = (desc_o.rS() & 0xFFFF);
              nrI = (desc_o.rS() & 0xFFFF);
              nrS = (desc_o.rS() & 0xFFFF);
              nrB = (desc_o.rS() & 0xFFFF);
            }
            desc_o.release();
            desc_o = null;
          }
          
          desc.release();
          desc = null;
        }
        
        // read extended size descriptors
        {
          if ((nrJ | nrI | nrS | nrB) == 0xFFFF) {
            int nre = 0;
            
            if (nrJ == 0xFFFF) nre++;
            if (nrI == 0xFFFF) nre++;
            if (nrS == 0xFFFF) nre++;
            if (nrB == 0xFFFF) nre++;
            
            final int AMTZ = nre * 8;
            
            require(buffer_offset + AMTZ);
            
            Buffer.xJ desc = central.acquireJ();
            
            {
              Buffer.nJ desc_n = desc.prepend();
              {
                Buffer.oB buffer_o = buffer.iterate();
                buffer_o.skip(buffer_offset);
                Buffer.sJ.decode(desc_n, buffer_o, AMTZ/8);
                buffer_offset += AMTZ;
                buffer_o.release();
                buffer_o = null;
              }
              desc_n.release();
              desc_n = null;
            }
            
            {
              Buffer.oJ desc_o = desc.iterate();
              {
                if (nrJ == 0xFFFF) nrJ = ((int)(desc_o.rJ()));
                if (nrI == 0xFFFF) nrI = ((int)(desc_o.rJ()));
                if (nrS == 0xFFFF) nrS = ((int)(desc_o.rJ()));
                if (nrB == 0xFFFF) nrB = ((int)(desc_o.rJ()));
              }
              desc_o.release();
              desc_o = null;
            }
            
            desc.release();
            desc = null;
          }
        }
        
        // read payload data
        {
          int AMTZ = ((((((((nrJ << 1) + nrI) << 1) + nrS) << 1) + nrB) + 0x7) & ~0x7);
          
          require(buffer_offset + AMTZ);
          
          {
            Buffer.oB buffer_o = buffer.iterate();
            buffer_o.skip(buffer_offset);
            {
              // compound primitives must copy
              if (nrJ > 0) Buffer.sJ.decode(outJ_n, buffer_o, nrJ);
              if (nrI > 0) Buffer.sI.decode(outI_n, buffer_o, nrI);
              if (nrS > 0) Buffer.sS.decode(outS_n, buffer_o, nrS);
              
              // bytes get aliased
              if (nrB > 0) Buffer.sB.alias(outB_n, buffer_o, nrB);
            }
            buffer_offset += AMTZ;
            buffer_o.release();
            buffer_o = null;
          }
        }
        
        int buffer_offset_leave_packet = buffer_offset;
        
        // verify checksum
        {
          if (false) {
            try {
              System.out.println("read-size nrJ=" + nrJ + ", nrI=" + nrI + ", nrS=" + nrS + ", nrB=" + nrB);
              System.out.println("read-side packet depict (sans checksum) (enter_packet=" + buffer_offset_enter_packet + ", leave_packet=" + buffer_offset_leave_packet + "):");
              System.out.write(buffer.depict().toNewArrayB());
              System.out.println();
            } catch (IOException e) {
              throw (new RuntimeException(e));
            }
          }
          
          if (assistant.checksumLength() > 0) {
            Buffer.oB buffer_o = buffer.iterate();
            buffer_o.skip(buffer_offset_enter_packet);
            {
              Buffer.oB csum_o = csum.iterate();
              if (!assistant.verifyChecksum(central, csum_o, buffer_o, (buffer_offset_leave_packet - buffer_offset_enter_packet))) throw null;
              csum_o.release();
              csum_o = null;
            }
            buffer_o.release();
            buffer_o = null;
            
            csum.release();
            csum = null;
          }
        }
        
        // truncate buffer
        truncate(buffer_offset);
        buffer_offset = 0; // never read, but still ...
      }
      
      /***
       * releases the input buffer and closes the input stream.
       ***/
      public void close()
      {
        buffer.release();
        buffer = null;
        
        try {
          inp.close();
        } catch (IOException e) {
          throw (new RuntimeException(e));
        }
      }
    }
    
    public static class PacketOutputStream
    {
      final BufferCentral central;
      final ChecksumAssistant assistant;
      final OutputStream out;
      
      public PacketOutputStream(BufferCentral central, ChecksumAssistant assistant, OutputStream out)
      {
        this.central = central;
        this.assistant = assistant;
        this.out = out;
      }
      
      /***
       * sends a packet. any argument may be null (resulting in no
       * primitives of that type being transmitted in the packet).
       ***/
      public void send(Buffer.oJ inpJ_o, Buffer.oI inpI_o, Buffer.oS inpS_o, Buffer.oB inpB_o, boolean flush)
      {
        Buffer.xB packet = central.acquireB();
        Buffer.oB packet_o = packet.iterate();
        
        Buffer.nB csum_o = packet_o.insert();
        Buffer.nB desc_o = packet_o.insert();
        Buffer.nB data_o = packet_o.insert();
        
        int nrJ = ((inpJ_o != null) ? (inpJ_o.remaining()) : 0);
        int nrI = ((inpI_o != null) ? (inpI_o.remaining()) : 0);
        int nrS = ((inpS_o != null) ? (inpS_o.remaining()) : 0);
        int nrB = ((inpB_o != null) ? (inpB_o.remaining()) : 0);
        
        // write size descriptors
        {
          Buffer.xS temp = central.acquireS();
          {
            Buffer.nS temp_n = temp.prepend();
            {
              temp_n.aS(((short)((nrJ < 0xFFFF) ? (nrJ) : (0xFFFF))));
              temp_n.aS(((short)((nrI < 0xFFFF) ? (nrI) : (0xFFFF))));
              temp_n.aS(((short)((nrS < 0xFFFF) ? (nrS) : (0xFFFF))));
              temp_n.aS(((short)((nrB < 0xFFFF) ? (nrB) : (0xFFFF))));
            }
            temp_n.release();
            temp_n = null;
            
            Buffer.oS temp_o = temp.iterate();
            {
              Buffer.sS.encode(desc_o, temp_o, temp_o.remaining());
            }
            temp_o.release();
            temp_o = null;
          }
          temp.release();
          temp = null;
        }
        
        // write extended size descriptors
        {
          if ((nrJ | nrI | nrS | nrB) >= 0xFFFF) {
            Buffer.xJ temp = central.acquireJ();
            {
              Buffer.nJ temp_n = temp.prepend();
              {
                if (nrJ >= 0xFFFF) temp_n.aJ(nrJ);
                if (nrI >= 0xFFFF) temp_n.aJ(nrI);
                if (nrS >= 0xFFFF) temp_n.aJ(nrS);
                if (nrB >= 0xFFFF) temp_n.aJ(nrB);
              }
              temp_n.release();
              temp_n = null;
              
              Buffer.oJ temp_o = temp.iterate();
              {
                Buffer.sJ.encode(desc_o, temp_o, temp_o.remaining());
              }
              temp_o.release();
              temp_o = null;
            }
            temp.release();
            temp = null;
          }
        }
        
        // flush size descriptors
        desc_o.release();
        desc_o = null;
        
        // compound primitives must copy
        Buffer.sJ.encode(data_o, inpJ_o, nrJ);
        Buffer.sI.encode(data_o, inpI_o, nrI);
        Buffer.sS.encode(data_o, inpS_o, nrS);
        
        // bytes get aliased
        {
          if (nrB > 0) {
            Buffer.sB.alias(data_o, inpB_o, nrB);
          }
        }
        
        // write padding
        {
          int payload_length = ((((((nrJ << 1) + nrI) << 1) + nrS) << 1) + nrB);
          
          int padding = ((-payload_length) & 0x7);
          
          for (int len = ((-payload_length) & 0x7); len > 0; len--) {
            data_o.aB(((byte)(0)));
          }
        }
        
        // flush data
        data_o.release();
        data_o = null;
        
        packet_o.release();
        packet_o = null;
        
        // write checksum
        {
          {
            Buffer.oB payload_o = packet.iterate();
            
            if (false) {
              try {
                System.out.println("write-side nrJ=" + nrJ + ", nrI=" + nrI + ", nrS=" + nrS + ", nrB=" + nrB);
                System.out.println("write-side packet depict (sans checksum):");
                System.out.write(packet.depict().toNewArrayB());
                System.out.println();
              } catch (IOException e) {
                throw (new RuntimeException(e));
              }
            }
            
            assistant.calculateChecksum(central, csum_o, payload_o, payload_o.remaining());
            payload_o.release();
            payload_o = null;
          }
        }
        
        // flush checksum
        csum_o.release();
        csum_o = null;
        
        // write packet to output stream
        {
          packet_o = packet.iterate();
          
          int rem = packet_o.remaining();
          
          while (rem > 0) {
            packet_o.dma_enter();
            
            int off = packet_o.dma_off();
            int len = packet_o.dma_lim() - off;
            
            if (len > rem) len = rem;
            
            try {
              out.write(packet_o.dma_bak(), off, len);
              if (flush) out.flush();
            } catch (IOException e) {
              throw (new RuntimeException(e));
            }
            
            packet_o.dma_leave(len);
            
            rem -= len;
          }
          
          packet_o.release();
          packet_o = null;
        }
        
        packet.release();
        packet = null;
      }
    }
  }
}

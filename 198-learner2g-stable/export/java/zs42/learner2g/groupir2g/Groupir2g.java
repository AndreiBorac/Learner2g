/***
 * Groupir2g.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.groupir2g;

import zs42.mass.*;
import zs42.buff.*;

import zs42.parts.*;

import zs42.splitter.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Groupir2g
{
  public static enum StreamDisposition
  {
    /***
     * Disposition <code>INSTANT</code> means that stream's packets
     * should be executed immediately upon receipt (out-of-band).
     ***/
    INSTANT
      {
        public boolean isRepeatable() { return false; }
      },
    
    /***
     * Disposition <code>PRIMARY</code> means that the stream
     * determines the playback rate (i.e., audio).
     ***/
    PRIMARY
      {
        public boolean isRepeatable() { return true; }
      },
    
    /***
     * Disposition <code>TRAILER</code> means that the stream is to
     * be synchronized to the primary stream.
     ***/
    TRAILER
      {
        public boolean isRepeatable() { return true; }
      };
    
    public abstract boolean isRepeatable();
  }
  
  public static enum StreamIdentifier
  {
    COMMAND
      {
        public StreamDisposition getDisposition() { return StreamDisposition.INSTANT; }
      },
    AUDIO
      {
        public StreamDisposition getDisposition() { return StreamDisposition.PRIMARY; }
      },
    NWED_EVENT
      {
        public StreamDisposition getDisposition() { return StreamDisposition.TRAILER; }
      },
    ETCH_JARFILE_FRAGMENT
      {
        public StreamDisposition getDisposition() { return StreamDisposition.INSTANT; }
      },
    ETCH_EVENT_BUNDLE
      {
        public StreamDisposition getDisposition() { return StreamDisposition.TRAILER; }
      },
    FROB_CODE
      {
        public StreamDisposition getDisposition() { return StreamDisposition.TRAILER; }
      };
    
    public abstract StreamDisposition getDisposition();
  }
  
  public static class Packet
  {
    /** groupir payload data (long).  */
    public Buffer.xJ dJ;
    /** groupir payload data (int).   */
    public Buffer.xI dI;
    /** groupir payload data (short). */
    public Buffer.xS dS;
    /** groupir payload data (byte).  */
    public Buffer.xB dB;
    
    public void release()
    {
      if (dJ != null) { dJ.release(); dJ = null; }
      if (dI != null) { dI.release(); dI = null; }
      if (dS != null) { dS.release(); dS = null; }
      if (dB != null) { dB.release(); dB = null; }
    }
  }
  
  /***
   * set <code>buffer</code> to a new buffer that excludes the
   * first <code>buffer_offset</code> bytes from the buffer.
   * 
   * code here is identical to
   * SplitterCommon::PacketInputStream::truncate; bug fixes should be
   * propagated to both copies.
   ***/
  static Buffer.xB truncate(BufferCentral central, Buffer.xB buffer, int buffer_offset)
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
    buffer.pack();
    
    return buffer;
  }
  
  public static class SteamRoller
  {
    boolean initialized = false;
    
    Buffer.xB buff = null;
    Buffer.nB head = null;
    Buffer.oB tail = null;
    
    public void release()
    {
      initialized = false;
      
      if (head != null) { head.release(); head = null; }
      if (tail != null) { tail.release(); tail = null; }
      if (buff != null) { buff.release(); buff = null; }
    }
  }
  
  public static Buffer.xB decode(BufferCentral central, SplitterCommon.Groupir.ChecksumAssistant assistant, Buffer.xB buffer, SimpleDeque<Packet> output_queue, SteamRoller steam_roller)
  {
    Buffer.xJ outJ = central.acquireJ();
    Buffer.xI outI = central.acquireI();
    Buffer.xS outS = central.acquireS();
    Buffer.xB outB = central.acquireB();
    
    Buffer.nJ outJ_n = outJ.prepend();
    Buffer.nI outI_n = outI.prepend();
    Buffer.nS outS_n = outS.prepend();
    Buffer.nB outB_n = outB.prepend();
    
    while (true) {
      int buffer_offset = 0;
      int buffer_length = buffer.length();
      
      /***
       * receive one packet. code here is analogous to
       * SplitterCommon::PacketInputStream::recv; bug fixes should be
       * propagated to both versions.
       ***/
      {
        Buffer.xB csum = null;
        
        if (assistant.checksumLength() > 0) {
          csum = central.acquireB();
          
          if (!(buffer_length >= (buffer_offset + assistant.checksumLength()))) { if (csum != null) { csum.release(); csum = null; } break; }
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
          
          if (!(buffer_length >= (buffer_offset + AMTZ))) { if (csum != null) { csum.release(); csum = null; } break; }
          
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
            
            if (!(buffer_length >= (buffer_offset + AMTZ))) { if (csum != null) { csum.release(); csum = null; } break; }
            
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
          
          if (!(buffer_length >= (buffer_offset + AMTZ))) { if (csum != null) { csum.release(); csum = null; } break; }
          
          {
            Buffer.oB buffer_o = buffer.iterate();
            buffer_o.skip(buffer_offset);
            {
              // compound primitives must copy
              if (nrJ > 0) Buffer.sJ.decode(outJ_n, buffer_o, nrJ);
              if (nrI > 0) Buffer.sI.decode(outI_n, buffer_o, nrI);
              if (nrS > 0) Buffer.sS.decode(outS_n, buffer_o, nrS);
              
              // bytes get aliased (OLD)
              //if (nrB > 0) Buffer.sB.alias(outB_n, buffer_o, nrB);
              
              // now with steam roller
              {
                if (!steam_roller.initialized) {
                  steam_roller.buff = central.acquireB();
                  steam_roller.head = steam_roller.buff.prepend();
                  steam_roller.tail = steam_roller.buff.iterate();
                  steam_roller.initialized = true;
                }
                
                Buffer.sB.copy(steam_roller.head, buffer_o, nrB);
                steam_roller.head.commit();
                Buffer.sB.alias(outB_n, steam_roller.tail, nrB);
                
                //Log.log("steam_roller: " + steam_roller.buff.depiction(false));
              }
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
      }
      
      // one packet has been received; cycle the chamber
      {
        Packet packet = (new Packet());
        
        outJ_n.release();
        outI_n.release();
        outS_n.release();
        outB_n.release();
        
        if (outJ.length() == 0) { outJ.release(); outJ = null; } else { outJ.pack(); }
        if (outI.length() == 0) { outI.release(); outI = null; } else { outI.pack(); }
        if (outS.length() == 0) { outS.release(); outS = null; } else { outS.pack(); }
        if (outB.length() == 0) { outB.release(); outB = null; } else { outB.pack(); }
        
        packet.dJ = outJ;
        packet.dI = outI;
        packet.dS = outS;
        packet.dB = outB;
        
        output_queue.addLast(packet);
        
        outJ = central.acquireJ();
        outI = central.acquireI();
        outS = central.acquireS();
        outB = central.acquireB();
        
        outJ_n = outJ.prepend();
        outI_n = outI.prepend();
        outS_n = outS.prepend();
        outB_n = outB.prepend();
        
        buffer = truncate(central, buffer, buffer_offset);
        buffer_offset = 0; // never read, but still ...
      }
    }
    
    outJ_n.release();
    outI_n.release();
    outS_n.release();
    outB_n.release();
    
    outJ.release();
    outI.release();
    outS.release();
    outB.release();
    
    return buffer;
  }
  
  public static class WriteQueue
  {
    static class Packet
    {
      Buffer.xJ xJ;
      Buffer.xI xI;
      Buffer.xS xS;
      Buffer.xB xB;
      
      Buffer.oJ oJ;
      Buffer.oI oI;
      Buffer.oS oS;
      Buffer.oB oB;
      
      LinkedBlockingQueue<Packet> reverse;
    }
    
    static final ThreadLocalObjectCache<Packet> tls_cache = (new ThreadLocalObjectCache<Packet>((new zs42.parts.F0<Packet>() { public Packet invoke() { return (new Packet()); } })));
    
    static final ThreadLocal<LinkedBlockingQueue<Packet>> tls_reverse =
      (new ThreadLocal<LinkedBlockingQueue<Packet>>()
       {
         protected LinkedBlockingQueue<Packet> initialValue()
         {
           return (new LinkedBlockingQueue<Packet>());
         }
       });
    
    final LinkedBlockingQueue<Packet> forward = (new LinkedBlockingQueue<Packet>());
    
    public WriteQueue(final SplitterCommon.Groupir.ChecksumAssistant aux, final OutputStream out)
    {
      (new Thread()
        {
          public void run()
          {
            try {
              final BufferCentral local_central = (new BufferCentral(9));
              
              final ByteArrayOutputStream local_capture = (new ByteArrayOutputStream());
              
              final SplitterCommon.Groupir.PacketOutputStream local_output_stream = (new SplitterCommon.Groupir.PacketOutputStream(local_central, aux, local_capture));
              
              final SplitterCommon.Groupir.Packet local_packet  = (new SplitterCommon.Groupir.Packet(local_central, false, false, false, false));
              
              while (true) {
                Packet packet = forward.take();
                
                if (packet.oJ != null) { Buffer.sJ.copy((local_packet.bufJ = local_central.acquireJ()), packet.oJ); }
                if (packet.oI != null) { Buffer.sI.copy((local_packet.bufI = local_central.acquireI()), packet.oI); }
                if (packet.oS != null) { Buffer.sS.copy((local_packet.bufS = local_central.acquireS()), packet.oS); }
                if (packet.oB != null) { Buffer.sB.copy((local_packet.bufB = local_central.acquireB()), packet.oB); }
                
                local_packet.send(local_output_stream, true);
                
                local_packet.release();
                
                packet.reverse.add(packet);
                
                local_capture.writeTo(out);
                out.flush();
                
                local_capture.reset();
              }
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        }).start();
    }
    
    /***
     * cleanup policy: the passed buffers are not released by this
     * method, but they may be released by the caller as soon as the
     * method returns.
     ***/
    public void push(BufferCentral central, Buffer.xJ xJ, Buffer.xI xI, Buffer.xS xS, Buffer.xB xB)
    {
      //Log.log("WriteQueue::push(xJ.length()=" + ((xJ == null) ? -1 : xJ.length()) + ", xI.length()=" + ((xI == null) ? -1 : xI.length()) + ", xS.length()=" + ((xS == null) ? -1 : xS.length()) + ", xB.length()=" + ((xB == null) ? -1 : xB.length()) + ")");
      
      final LinkedBlockingQueue<Packet> reverse = tls_reverse.get();
      
      // release buffers on reverse stream
      {
        while (!reverse.isEmpty()) {
          Packet packet = reverse.remove();
          
          if (packet.oJ != null) { packet.oJ.release(); packet.oJ = null; }
          if (packet.oI != null) { packet.oI.release(); packet.oI = null; }
          if (packet.oS != null) { packet.oS.release(); packet.oS = null; }
          if (packet.oB != null) { packet.oB.release(); packet.oB = null; }
          
          if (packet.xJ != null) { packet.xJ.release(); packet.xJ = null; }
          if (packet.xI != null) { packet.xI.release(); packet.xI = null; }
          if (packet.xS != null) { packet.xS.release(); packet.xS = null; }
          if (packet.xB != null) { packet.xB.release(); packet.xB = null; }
          
          packet.reverse = null;
          
          tls_cache.refund(packet);
        }
      }
      
      // append packet to forward stream
      {
        Packet packet = tls_cache.obtain();
        
        if (xJ != null) { packet.xJ = central.acquireJ(); Buffer.sJ.alias(packet.xJ, xJ); packet.oJ = packet.xJ.iterate(); }
        if (xI != null) { packet.xI = central.acquireI(); Buffer.sI.alias(packet.xI, xI); packet.oI = packet.xI.iterate(); }
        if (xS != null) { packet.xS = central.acquireS(); Buffer.sS.alias(packet.xS, xS); packet.oS = packet.xS.iterate(); }
        if (xB != null) { packet.xB = central.acquireB(); Buffer.sB.alias(packet.xB, xB); packet.oB = packet.xB.iterate(); }
        
        packet.reverse = reverse;
        
        forward.add(packet);
      }
    }
  }
}

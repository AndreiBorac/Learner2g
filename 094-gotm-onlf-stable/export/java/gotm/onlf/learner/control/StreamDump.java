/***
 * StreamDump.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.learner.control;

import java.io.*;
import java.net.*;
import java.security.*;

import java.util.*;
import java.util.jar.*;

import java.awt.*;
import java.awt.image.*;

import zs42.mass.*;
import zs42.buff.*;
import zs42.parts.*;
import zs42.nats.codec.*;
import zs42.pixels.codec.*;
import zs42.nwed.*;
import gotm.etch.*;

import gotm.onlf.splitter.common.*;
import gotm.onlf.learner.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class StreamDump
{
  private static final BufferCentral central = (new BufferCentral());
  
  static abstract class PayloadHandler
  {
    abstract void handlePayload(GroupirPacket groupir, long rebased_source_tc);
    
    void flushPayload()
    {
      // nop by default
    }
  }
  
  static abstract class VideoPayloadHandler extends PayloadHandler
  {
    final DataOutputStream out;
    
    final int FRAME_TIME_US = 40000; // -EXACTLY- 25fps
    
    final int H = Integer.getInteger("gotm.onlf.learner.control.StreamDump.H");
    final int W = Integer.getInteger("gotm.onlf.learner.control.StreamDump.W");
    
    final byte[] aY = (new byte[H * W]);
    final byte[] rC = (new byte[(H * W) >> 2]);
    final byte[] bC = (new byte[(H * W) >> 2]);
    
    final PixelsCodec.Dimensions  dimensions  = (new PixelsCodec.Dimensions(H, W));
    final PixelsCodec.Framebuffer framebuffer = (new PixelsCodec.Framebuffer(dimensions));
    
    VideoPayloadHandler(DataOutputStream out)
    {
      this.out = out;
    }
    
    byte clamp_16_235(double fp)
    {
      int x = ((int)(Math.round(fp)));
      
      if (x <  16) return ((byte)( 16));
      if (x > 235) return ((byte)(235));
      
      return ((byte)(x));
    }
    
    byte clamp_16_239(double fp)
    {
      int x = ((int)(Math.round(fp)));
      
      if (x <  16) return ((byte)( 16));
      if (x > 239) return ((byte)(239));
      
      return ((byte)(x));
    }
    
    void punch_pixel_i420(int z, int y, int x)
    {
      int[] xRGB = framebuffer.getRaster();
      
      int r = ((xRGB[z] >> 0x10) & 0xFF);
      int g = ((xRGB[z] >> 0x08) & 0xFF);
      int b = ((xRGB[z] >> 0x00) & 0xFF);
      
      aY[z] = clamp_16_235((+0.299 * r) + (+0.587 * g) + (+0.114 * b));
      
      if (((y & 1) | (x & 1)) == 0) {
        bC[z >> 2] = clamp_16_239((-0.169 * r) + (-0.331 * g) + (+0.500 * b) + 128);
        rC[z >> 2] = clamp_16_239((+0.500 * r) + (-0.419 * g) + (-0.081 * b) + 128);
      }
    }
    
    void eframe_i420(boolean cached)
    {
      if (!cached) {
        int[] xRGB = framebuffer.getRaster();
        
        {
          int z = 0;
          
          for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
              int r = ((xRGB[z] >> 0x10) & 0xFF);
              int g = ((xRGB[z] >> 0x08) & 0xFF);
              int b = ((xRGB[z] >> 0x00) & 0xFF);
              
              aY[z] = clamp_16_235((+0.299 * r) + (+0.587 * g) + (+0.114 * b));
              
              z++;
            }
          }
        }
        
        {
          int o = 0;
          int z = 0;
          
          for (int y = 0; y < H; y += 2) {
            for (int x = 0; x < W; x += 2) {
              int r = ((xRGB[z] >> 0x10) & 0xFF);
              int g = ((xRGB[z] >> 0x08) & 0xFF);
              int b = ((xRGB[z] >> 0x00) & 0xFF);
              
              bC[o] = clamp_16_239((-0.169 * r) + (-0.331 * g) + (+0.500 * b) + 128);
              rC[o] = clamp_16_239((+0.500 * r) + (-0.419 * g) + (-0.081 * b) + 128);
              
              o++;
              
              z += 2;
            }
            
            z += W;
          }
        }
      }
      
      try {
        out.write(aY);
        out.write(bC);
        out.write(rC);
        
        out.flush();
      } catch (Exception e) {
        throw (new RuntimeException(e));
      }
    }
    
    void eframe(boolean cached)
    {
      //eframe_rgb(cached);
      eframe_i420(cached);
    }
    
    private int prior_hashcode = 0;
    
    void eframe_autocache()
    {
      int current_hashcode = Arrays.hashCode(framebuffer.getRaster());
      eframe((current_hashcode == prior_hashcode));
      prior_hashcode = current_hashcode;
    }
  }
  
  public static void main(String[] args) throws Exception
  {
    int nr = 0;
    
    final int ARGI_MODE   = nr++;
    final int ARGI_INPUT  = nr++;
    final int ARGI_OUTPUT = nr++;
    
    final IncomingNetworkStream inp = (new IncomingNetworkStream((new FileInputStream(args[ARGI_INPUT])), TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
    final DataOutputStream out = (new DataOutputStream((new FileOutputStream(args[ARGI_OUTPUT]))));
    
    PayloadHandler payload_handler;
    
    /****/ if (args[ARGI_MODE].equals("times")) {
      payload_handler =
        ((new PayloadHandler()
          {
            void handlePayload(GroupirPacket groupir, long rebased_source_tc)
            {
              long proper_source_tc = decodeProperSourceTimecode(groupir);
              
              if (proper_source_tc > 0) {
                try {
                  out.write(("" + proper_source_tc + "\n").getBytes("UTF-8"));
                  out.flush();
                  System.exit(0);
                } catch (Exception e) {
                  throw (new RuntimeException(e));
                }
              }
            }
          }));
    } else if (args[ARGI_MODE].equals("audio")) {
      payload_handler =
        (new PayloadHandler()
         {
           void handlePayload(GroupirPacket groupir, long rebased_source_tc)
           {
             try {
               byte[] payload = groupir.dB;
               out.write(AudioCommon.Format.F_2_S_EL.adaptSignedPCM(AudioCommon.unlaw(payload, 0, payload.length)));
             } catch (Exception e) {
               throw (new RuntimeException(e));
             }
           }
         });
    } else if (args[ARGI_MODE].equals("video-pixels")) {
      payload_handler =
        (new PayloadHandler()
         {
           final int FRAME_TIME_US = 40000; // -EXACTLY- 25fps
           
           final int H = Integer.getInteger("gotm.onlf.learner.control.StreamDump.H");
           final int W = Integer.getInteger("gotm.onlf.learner.control.StreamDump.W");
           
           final PixelsCodec.Dimensions  dimensions  = (new PixelsCodec.Dimensions(H, W));
           final PixelsCodec.Framebuffer framebuffer = (new PixelsCodec.Framebuffer(dimensions));
           
           final wiL<vB> aux = (new wiL<vB>(null));
           
           void eframe_rgb(boolean cached)
           {
             try {
               framebuffer.sendStream(out, PixelsCodec.PixelOrder.RGB_888, aux);
             } catch (Exception e) {
               throw (new RuntimeException(e));
             }
           }
           
           final byte[] aY = (new byte[H * W]);
           final byte[] rC = (new byte[(H * W) >> 2]);
           final byte[] bC = (new byte[(H * W) >> 2]);
           
           byte clamp_16_235(double fp)
           {
             int x = ((int)(Math.round(fp)));
             
             if (x <  16) return ((byte)( 16));
             if (x > 235) return ((byte)(235));
             
             return ((byte)(x));
           }
           
           byte clamp_16_239(double fp)
           {
             int x = ((int)(Math.round(fp)));
             
             if (x <  16) return ((byte)( 16));
             if (x > 239) return ((byte)(239));
             
             return ((byte)(x));
           }
           
           void punch_pixel_i420(int z, int y, int x)
           {
             int[] xRGB = framebuffer.getRaster();
             
             int r = ((xRGB[z] >> 0x10) & 0xFF);
             int g = ((xRGB[z] >> 0x08) & 0xFF);
             int b = ((xRGB[z] >> 0x00) & 0xFF);
             
             aY[z] = clamp_16_235((+0.299 * r) + (+0.587 * g) + (+0.114 * b));
             
             if (((y & 1) | (x & 1)) == 0) {
               bC[z >> 2] = clamp_16_239((-0.169 * r) + (-0.331 * g) + (+0.500 * b) + 128);
               rC[z >> 2] = clamp_16_239((+0.500 * r) + (-0.419 * g) + (-0.081 * b) + 128);
             }
           }
           
           void eframe_i420(boolean cached)
           {
             if (!cached) {
               int[] xRGB = framebuffer.getRaster();
               
               {
                 int z = 0;
                 
                 for (int y = 0; y < H; y++) {
                   for (int x = 0; x < W; x++) {
                     int r = ((xRGB[z] >> 0x10) & 0xFF);
                     int g = ((xRGB[z] >> 0x08) & 0xFF);
                     int b = ((xRGB[z] >> 0x00) & 0xFF);
                     
                     aY[z] = clamp_16_235((+0.299 * r) + (+0.587 * g) + (+0.114 * b));
                     
                     z++;
                   }
                 }
               }
               
               {
                 int o = 0;
                 int z = 0;
                 
                 for (int y = 0; y < H; y += 2) {
                   for (int x = 0; x < W; x += 2) {
                     int r = ((xRGB[z] >> 0x10) & 0xFF);
                     int g = ((xRGB[z] >> 0x08) & 0xFF);
                     int b = ((xRGB[z] >> 0x00) & 0xFF);
                     
                     bC[o] = clamp_16_239((-0.169 * r) + (-0.331 * g) + (+0.500 * b) + 128);
                     rC[o] = clamp_16_239((+0.500 * r) + (-0.419 * g) + (-0.081 * b) + 128);
                     
                     o++;
                     
                     z += 2;
                   }
                   
                   z += W;
                 }
               }
             }
             
             try {
               out.write(aY);
               out.write(bC);
               out.write(rC);
               
               out.flush();
             } catch (Exception e) {
               throw (new RuntimeException(e));
             }
           }
           
           void eframe(boolean cached)
           {
             //eframe_rgb(cached);
             eframe_i420(cached);
           }
           
           long playback_tc;
           
           void handlePayload(GroupirPacket groupir, long rebased_source_tc)
           {
             final byte[] payload = groupir.dB;
             
             boolean cached = false;
             
             // repeat previous frame
             {
               while (playback_tc < rebased_source_tc) {
                 eframe(cached); cached = true;
                 playback_tc += FRAME_TIME_US;
               }
             }
             
             // decode !
             {
               Buffer.xB crm = central.acquireB();
               
               {
                 Buffer.nB crm_n = crm.append();
                 Buffer.sB.copy(crm_n, payload, 0, payload.length);
                 crm_n.release();
               }
               
               Buffer.xI mod = central.acquireI();
               
               PixelsCodec._003.decode(central, framebuffer, crm, framebuffer, dimensions, mod);
               
               {
                 Buffer.oI mod_o = mod.iterate();
                 int num = mod_o.remaining();
                 
                 while (num > 0) {
                   int z = mod_o.rI(); num--;
                   int s = mod_o.rI(); num--;
                   
                   int y = (z / dimensions.W);
                   int x = (z % dimensions.W);
                   
                   while (z < s) {
                     punch_pixel_i420(z, y, x);
                     
                     z++;
                   }
                 }
                 
                 mod_o.release();
               }
               
               mod.release();
               crm.release();
             }
           }
         });
    } else if (args[ARGI_MODE].equals("video-etch")) {
      payload_handler =
        (new VideoPayloadHandler(out)
          {
            final ByteArrayOutputStream etch_jarfile_data = (new ByteArrayOutputStream());
            
            final Etch etch = (new Etch(H, W, null, -1));
            final Etch.OrganicInputEvent.DecodeCreator decoder = (new Etch.OrganicInputEvent.DecodeCreator());
            
            void handlePayload(GroupirPacket groupir, long rebased_source_tc)
            {
              final byte[] payload = groupir.dB;
              
              final int complexity = groupir.dI[PACKET_METADATA_COMPLEXITY];
              
              if (groupir.dI[PACKET_METADATA_STREAM_ID] == PACKET_METADATA_STREAM_ID_ETCH_JARFILE) {
                etch_jarfile_data.write(payload, 0, payload.length);
                
                if (etch_jarfile_data.size() == complexity) {
                  try {
                    JarInputStream inp = (new JarInputStream(new ByteArrayInputStream(etch_jarfile_data.toByteArray())));
                    
                    JarEntry entry;
                    
                    while ((entry = inp.getNextJarEntry()) != null) {
                      if (entry.getName().endsWith(".png")) {
                        byte[] png = SimpleIO.slurp(inp);
                        etch.appendBackdrop(png);
                      }
                    }
                  } catch (IOException e) {
                    throw (new RuntimeException(e));
                  }
                }
              } else {
                Buffer.xB crm = central.acquireB();
                Buffer.xI tmp = central.acquireI();
                
                {
                  Buffer.nB crm_n = crm.append();
                  Buffer.sB.copy(crm_n, payload, 0, payload.length);
                  crm_n.release();
                }
                
                NaturalNumberCodec.decode(central, tmp, crm);
                
                Buffer.oI tmp_o = tmp.iterate();
                decoder.setInputSource(tmp_o, rebased_source_tc);
                
                for (int i = 0; i < complexity; i++) {
                  Etch.OrganicInputEvent event = decoder.decode();
                  etch.submit(event);
                }
                
                tmp.release();
                crm.release();
              }
            }
            
            private final BufferedImage canvas = (new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB));
            private final Graphics2D canvas_graphics = canvas.createGraphics();
            
            private void render(long timecode)
            {
              etch.paintMicroTime(canvas_graphics, timecode);
              canvas.getRGB(0, 0, W, H, framebuffer.getRaster(), 0, W);
            }
            
            void flushPayload()
            {
              long playback_tc = 0;
              
              while (etch.hasPendingOrganic()) {
                //System.err.println("playback_tc=" + playback_tc + " (first queued event at " + etch.getPendingOrganicMicroTime() + ")");
                render(playback_tc);
                eframe_autocache();
                playback_tc += FRAME_TIME_US;
              }
            }
          });
    } else {
      throw null;
    }
    
    /*
    final zs42.mass.F1<Long, GroupirPacket> timestamp_rebaser_001 =
      (new zs42.mass.F1<Long, GroupirPacket>()
       {
         long proper_source_tc = (1L << 48);
         int last_known_source_tc = 0;
         
         boolean unknown_base_source_tc = true;
         long base_source_tc = -1;
         
         public Long invoke(GroupirPacket groupir)
         {
           final int source_tc = groupir.dI[PACKET_METADATA_SOURCE_TC];
           
           long proper_source_tc = ((((long)(source_hi_tc)) << 32) | (((long)(source_lo_tc)) & 0xFFFFFFFF));
           
           proper_source_tc += (source_tc - last_known_source_tc); last_known_source_tc = source_tc;
           
           if (unknown_base_source_tc) {
             unknown_base_source_tc = false;
             base_source_tc = proper_source_tc;
           }
           
           return (proper_source_tc - base_source_tc);
         }
       });
    */
    
    final zs42.mass.F1<Long, GroupirPacket> timestamp_rebaser_002 =
      (new zs42.mass.F1<Long, GroupirPacket>()
       {
         boolean unknown_base_source_tc = true;
         long base_source_tc = -1;
         
         public Long invoke(GroupirPacket groupir)
         {
           long proper_source_tc = decodeProperSourceTimecode(groupir);
           
           //System.out.println("hi = " + (((long)(groupir.dI[PACKET_METADATA_SOURCE_TC_HI])) & 0x00000000FFFFFFFFL) + ", lo = " + (((long)(groupir.dI[PACKET_METADATA_SOURCE_TC_LO])) & 0x00000000FFFFFFFFL));
           
           //System.err.println("proper_source_tc = " + proper_source_tc);
           
           long rebased_source_tc;
           
           if (unknown_base_source_tc) {
             if (proper_source_tc > 0) {
               unknown_base_source_tc = false;
               base_source_tc = proper_source_tc;
             }
           }
           
           if (unknown_base_source_tc) {
             rebased_source_tc = 0;
           } else {
             rebased_source_tc = (proper_source_tc - base_source_tc);
           }
           
           //System.out.println("rebased_source_tc = " + rebased_source_tc);
           
           return rebased_source_tc;
         }
       });
    
    final zs42.mass.F1<Long, GroupirPacket> timestamp_rebaser = timestamp_rebaser_002;
    
    while (true) {
      GroupirPacket groupir;
      
      inp.turn();
      
      try {
        groupir = GroupirPacket.recv_from_trusted_source(inp);
      } catch (Throwable e) {
        e.printStackTrace(System.err);
        break;
      }
      
      long rebased_source_tc = timestamp_rebaser.invoke(groupir);
      
      payload_handler.handlePayload(groupir, rebased_source_tc);
    }
    
    payload_handler.flushPayload();
    
    try {
      out.flush();
      out.close();
    } catch (Throwable e) {
      e.printStackTrace(System.err);
    }
    
    System.out.println("+OK (gotm.onlf.learner.control.StreamDump)");
    
    System.exit(0);
  }
}

/***
 * EtchTeacher.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.etch.teacher;

import gotm.etch.*;

import zs42.mass.*;
import zs42.buff.*;

import zs42.parts.*;

import zs42.nats.codec.*;

import zs42.splitter.common.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.concurrent.*;

import javax.swing.*;

public class EtchTeacher
{
  /***
   * display fps
   ***/
  public static final int UPDATE_INTERVAL = 25;
  
  /***
   * collect a quarter-second of events at a time.
   ***/
  public static final int EVENT_COLLECTION_INTERVAL_MS = 250;
  
  /***
   * longest event description is currently 20 bytes (give or take nat
   * encoding compression/overhead). so 64 events certainly can't
   * break the bank.
   ***/
  public static final int EVENT_BATCH_LIMIT = 64;
  
  static RuntimeException fatal(Throwable e)
  {
    Log.log(e);
    
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (Throwable e2) {
        // ignored
      }
    }
  }
  
  static void trace(Throwable e)
  {
    Log.log(e);
  }
  
  public static void main(String[] args)
  {
    Log.loopHandleEventsBackground(System.err, true);
    
    try {
      int nr = 0;
      
      final int ARGI_H                          = nr++;
      final int ARGI_W                          = nr++;
      final int ARGI_BACKDROP_JARFILE           = nr++;
      final int ARGI_BACKDROP_JARFILE_SIZE      = nr++;
      final int ARGI_SPLITTER_HOST              = nr++;
      final int ARGI_SPLITTER_PORT              = nr++;
      final int ARGI_SPLITTER_ROOT_PASS_FILE    = nr++;
      final int ARGI_SPLITTER_STREAM_ID_JARFILE = nr++;
      final int ARGI_SPLITTER_STREAM_ID_EVENTS  = nr++;
      final int ARGI_EXPECTED_LENGTH            = nr++;
      
      for (int i = 0; i < args.length; i++) {
        Log.log("args[" + i + "]='" + args[i] + "'");
      }
      
      if (args.length != ARGI_EXPECTED_LENGTH) {
        for (String line : (new String[] { "usage:", "  java gotm.etch.teacher.EtchTeacher (H) (W) (backdrop jarfile) (backdrop jarfile size) (splitter host) (splitter port) (splitter root pass file) (splitter stream id jarfile) (splitter stream id events)"})) {
          System.out.println(line);
        }
        
        fatal(null);
        throw null;
      }
      
      final String splitter_host              = args[ARGI_SPLITTER_HOST];
      final int    splitter_port              = Integer.parseInt(args[ARGI_SPLITTER_PORT]);
      final int    splitter_stream_id_jarfile = Integer.parseInt(args[ARGI_SPLITTER_STREAM_ID_JARFILE]);
      final int    splitter_stream_id_events  = Integer.parseInt(args[ARGI_SPLITTER_STREAM_ID_EVENTS]);
      
      final BufferCentral central = (new BufferCentral(9));
      
      Log.log("connecting to host='" + splitter_host + "', port=" + splitter_port);
      
      final SplitterCommon.Groupir.PacketOutputStream out =
        (SplitterCommon.Groupir.Authentication.connect_root
         (central,
          (NetworkingUtilities.connect(splitter_host, splitter_port).getOutputStream()),
          SplitterCommon.Groupir.Authentication.load_key_file(args[ARGI_SPLITTER_ROOT_PASS_FILE])));
      
      Log.log("connected !");
      
      Log.log("uploading jarfile ...");
      
      // upload jarfile
      {
        DataInputStream inp =
          (new DataInputStream
           (new FileInputStream
            (args[ARGI_BACKDROP_JARFILE])));
        
        byte[] buf = (new byte[Integer.parseInt(args[ARGI_BACKDROP_JARFILE_SIZE])]);
        
        inp.readFully(buf);
        
        int off = 0;
        
        while (off < buf.length) {
          SplitterCommon.Groupir.Packet groupir = (new SplitterCommon.Groupir.Packet(central, false, true, false, true));
          
          {
            Buffer.nI metadata_n = groupir.bufI.prepend();
            metadata_n.aI(0);                          // capture timecode
            metadata_n.aI(0);                          // capture timecode
            metadata_n.aI(splitter_stream_id_jarfile); // stream id
            metadata_n.aI(buf.length);                 // complexity
            metadata_n.release();
          }
          
          {
            Buffer.nB payload_n = groupir.bufB.prepend();
            
            int amt = Math.min((buf.length - off), 8192);
            Buffer.sB.copy(payload_n, buf, off, (off + amt));
            off += amt;
            
            payload_n.release();
          }
          
          Log.log("sending packet ...");
          groupir.send(out, true);
          Log.log("sent packet");
        }
      }
      
      Log.log("uploaded jarfile ...");
      
      final LinkedBlockingQueue<Etch.OrganicInputEvent> capture = (new LinkedBlockingQueue<Etch.OrganicInputEvent>());
      final Etch etch = (new Etch(Integer.parseInt(args[ARGI_H]), Integer.parseInt(args[ARGI_W]), capture, UPDATE_INTERVAL));
      
      final LinkedBlockingQueue<Etch.OrganicInputEvent> transmit = (new LinkedBlockingQueue<Etch.OrganicInputEvent>());
      
      Log.log("launching user interface ...");
      
      // launch user interface
      {
        JFrame frame = (new JFrame("Network Visiplate"));
        
        frame.getContentPane().add(etch.getInterfaceElement());
        frame.pack();
        frame.setVisible(true);
        
        (new Thread()
          {
            public void run()
            {
              try {
                while (true) {
                  Etch.OrganicInputEvent event = capture.take();
                  etch.submit(event);
                  transmit.put(event);
                }
              } catch (Throwable e) {
                fatal(e);
              }
            }
          }).start();
      }
      
      Log.log("loading jarfile locally ...");
      
      // load jarfile locally
      {
        JarInputStream inp =
          (new JarInputStream
           (new BufferedInputStream
            (new FileInputStream
             (args[ARGI_BACKDROP_JARFILE]))));
        
        JarEntry entry;
        
        while ((entry = inp.getNextJarEntry()) != null) {
          if (entry.getName().endsWith(".png")) {
            byte[] png = SimpleIO.slurp(inp);
            etch.appendBackdrop(png);
          }
        }
      }
      
      Log.log("starting transmit updates thread ...");
      
      // transmit updates
      {
        (new Thread()
          {
            public void run()
            {
              try {
                Etch.OrganicInputEvent.EncodeVisitor encoder = (new Etch.OrganicInputEvent.EncodeVisitor());
                
                ArrayList<Etch.OrganicInputEvent> round = (new ArrayList<Etch.OrganicInputEvent>());
                
                final wiL<int[][]> aux = (new wiL<int[][]>(null));
                
                while (true) {
                  {
                    Etch.OrganicInputEvent event;
                    
                    sleep(EVENT_COLLECTION_INTERVAL_MS);
                    
                    round.clear();
                    
                    while ((round.size() < EVENT_BATCH_LIMIT) && (event = transmit.poll()) != null) {
                      round.add(event);
                    }
                  }
                  
                  if (round.size() > 0) {
                    long lktc = round.get(0).ustc;
                    
                    SplitterCommon.Groupir.Packet groupir = (new SplitterCommon.Groupir.Packet(central, false, true, false, true));
                    
                    {
                      Buffer.nI metadata_n = groupir.bufI.prepend();
                      metadata_n.aI(((int)(lktc >> 32)));       // capture timecode
                      metadata_n.aI(((int)(lktc      )));       // capture timecode
                      metadata_n.aI(splitter_stream_id_events); // stream id
                      metadata_n.aI(round.size());              // complexity
                      metadata_n.release();
                    }
                    
                    {
                      Buffer.xI tmp = central.acquireI();
                      
                      {
                        Buffer.nI tmp_n = tmp.prepend();
                        
                        encoder.setOutputTarget(tmp_n, lktc);
                        
                        for (Etch.OrganicInputEvent event : round) {
                          event.accept(encoder);
                        }
                        
                        tmp_n.release();
                      }
                      
                      NaturalNumberCodec.enable_native_code(true);
                      NaturalNumberCodec.encode(central, tmp, groupir.bufB, aux, 32);
                      
                      tmp.release();
                    }
                    
                    Log.log("sending groupir packet");
                    groupir.send(out, true);
                  }
                }
              } catch (Throwable e) {
                fatal(e);
              }
            }
          }).start();
      }
    } catch (Throwable e) {
      fatal(e);
    }
    
    Log.log("leaving main ...");
  }
}

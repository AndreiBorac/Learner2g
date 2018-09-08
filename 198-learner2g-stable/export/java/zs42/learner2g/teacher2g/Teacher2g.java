/***
 * Teacher2g.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.teacher2g;

import zs42.learner2g.groupir2g.*;

import gotm.onlf.splitter.common.*;
import gotm.onlf.learner.common.*;

import zs42.mass.*;
import zs42.buff.*;

import zs42.nats.codec.*;

import zs42.addc.*;

import zs42.parts.*;

import zs42.splitter.common.*;

import gotm.etch.*;
import gotm.etch.teacher.*;

import zs42.nwed.*;

import java.io.*;
import java.nio.charset.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.jar.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import static gotm.onlf.utilities.Utilities.*;
import static gotm.onlf.splitter.common.Constants.*;

import zs42.parts.F0; // disambiguation (ugh)
import zs42.parts.F1; // disambiguation (ugh)
import zs42.parts.F2; // disambiguation (ugh)

import zs42.parts.Settings; // disambiguation (ugh)

public class Teacher2g
{
  static final int TIMEOUT_MS = 5000;
  
  static final Charset UTF_8;
  
  static
  {
    try {
      UTF_8 = Charset.forName("UTF-8");
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
  }
  
  static final NoUTF.Filter FILTER_TOKENIZE = (new NoUTF.Filter("\u0000\u0020//"));
  
  static Throwable fatal(Throwable e)
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
  
  static class Globals
  {
    final Settings settings;
    final Groupir2g.WriteQueue write_queue;
    
    volatile boolean mute_enabled = false;
    
    volatile int signal_power_estimate = 0;
    
    Globals(Settings settings, Groupir2g.WriteQueue write_queue)
    {
      this.settings = settings;
      this.write_queue = write_queue;
    }
  }
  
  static void launch_user_interface(final Globals globals)
  {
    
  }
  
  /***
   * ENTER AUDIO TEACHER
   ***/
  
  static void writeOldGroupirTo(BufferCentral central, GroupirPacket groupir, Groupir2g.WriteQueue write_queue)
  {
    Buffer.xJ xJ = null;
    Buffer.xI xI = null;
    Buffer.xS xS = null;
    Buffer.xB xB = null;
    
    if ((groupir.dL != null) && (groupir.dL.length > 0)) { xJ = central.acquireJ(); Buffer.sJ.copy(xJ, groupir.dL); }
    if ((groupir.dI != null) && (groupir.dI.length > 0)) { xI = central.acquireI(); Buffer.sI.copy(xI, groupir.dI); }
    if ((groupir.dS != null) && (groupir.dS.length > 0)) { xS = central.acquireS(); Buffer.sS.copy(xS, groupir.dS); }
    if ((groupir.dB != null) && (groupir.dB.length > 0)) { xB = central.acquireB(); Buffer.sB.copy(xB, groupir.dB); }
    
    write_queue.push(central, xJ, xI, xS, xB);
    
    if (xJ != null) xJ.release();
    if (xI != null) xI.release();
    if (xS != null) xS.release();
    if (xB != null) xB.release();
  }
  
  static void writeSplitterCommonGroupirTo(BufferCentral central, SplitterCommon.Groupir.Packet groupir, Groupir2g.WriteQueue write_queue)
  {
    write_queue.push(central, groupir.bufJ, groupir.bufI, groupir.bufS, groupir.bufB);
  }
  
  static int avg(short[] pcm)
  {
    int sum = 0;
    
    for (short val : pcm) {
      sum += Math.abs(val);
    }
    
    sum /= pcm.length;
    
    return sum;
  }
  
  static int max(short[] pcm)
  {
    int max = 0;
    
    for (short val : pcm) {
      max = Math.max(max, Math.abs(val));
    }
    
    return max;
  }
  
  static void launch_audio_teacher(final Globals globals) throws Exception
  {
    final BufferCentral central = (new BufferCentral(9));
    
    final wiL<int[][]> aux = (new wiL<int[][]>(null));
    
    final int stream_id = Integer.parseInt(globals.settings.get("teacher.uplink.splitter.stream.audio"));
    
    final int kHz = Integer.parseInt(globals.settings.get("teacher.capture.audio.kHz"));
    final String regexp = globals.settings.get("teacher.capture.audio.matcher");
    final Pattern pattern = Pattern.compile(regexp);
    
    if (!((kHz % 8000) == 0)) throw null;
    
    final LinkedBlockingQueue<short[]> capture = (new LinkedBlockingQueue<short[]>());
    
    (new Thread()
      {
        public void run()
        {
          try {
            DataOutputStream dos = (new DataOutputStream(new FileOutputStream("utc" + System.currentTimeMillis() + "ms")));
            
            for (int i = 0; true; i++) {
              short[] buf = capture.take();
              
              for (short val : buf) {
                dos.writeShort(val);
              }
              
              if ((i & 0x3) == 0) {
                dos.flush();
              }
            }
          } catch (Throwable e) {
            Log.log(e);
          }
        }
      }).start();
    
    AudioCommon.Format.rate = kHz;
    
    final int multiplier = (kHz / 8000);
    
    AudioReader.launch
      ((new F1<AudioCommon.Target, AudioCommon.Target[]>()
        {
          public AudioCommon.Target invoke(AudioCommon.Target[] targets)
          {
            for (AudioCommon.Target target : targets) {
              Log.log("target: '" + target.getLabel() + "'");
            }
            
            for (AudioCommon.Target target : targets) {
              Matcher matcher = pattern.matcher(target.getLabel());
              
              if (matcher.matches()) {
                Log.log("label='" + target.getLabel() + "' matches pattern '" + regexp + "', returning!");
                return target;
              } else {
                Log.log("label='" + target.getLabel() + "' does not match pattern '" + regexp + "'");
              }
            }
            
            return null;
          }
        }),
       null,
       2048 * multiplier, /* approximately 1/4 second packet size */
       (new AudioReader.Callback()
         {
           short[] down(short[] inp)
           {
             int pos = 0;
             
             short[] out = (new short[(inp.length / multiplier)]);
             
             for (int i = 0; i < out.length; i++) {
               int sum = 0;
               
               for (int j = 0; j < multiplier; j++) {
                 sum += inp[pos++];
               }
               
               out[i] = ((short)(sum / multiplier));
             }
             
             return out;
           }
           
           public void gotBatch(AudioCommon.Format format, byte[] buf, int off, int lim)
           {
             Log.log("gotBatch(off=" + off + ", lim=" + lim + ")");
             
             try {
               // enter old gunk
               
               final long now = microTime();
               
               int metadata[] = (new int[PACKET_METADATA_LENGTH]);
               encodeProperSourceTimecode(metadata, now);
               metadata[PACKET_METADATA_STREAM_ID] = stream_id;
               metadata[PACKET_METADATA_COMPLEXITY] = 3; /* new (!) */
               
               int dataz = lim - off;
               byte[] data = new byte[dataz];
               for (int i = 0; i < dataz; i++)
                 data[i] = buf[i + off];
               short[] signed_pcm = format.adaptArbitraryPCM(data);
               
               capture.add(signed_pcm);
               signed_pcm = down(signed_pcm);
               
               //low_pass_filter(signed_pcm);
               //amplify_filter_15(signed_pcm);
               Log.log("=========================> AVG " + avg(signed_pcm) + ", MAX " + max(signed_pcm));
               
               byte[] encoded;
               
               {
                 Buffer.xB buf_x = central.acquireB();
                 Buffer.xI dif_x = central.acquireI();
                 
                 {
                   Buffer.nI dif_n = dif_x.prepend();
                   
                   int mem = 0;
                   
                   for (int i = 0; i < signed_pcm.length; i++) {
                     int val = Mulaw.Enlaw.enlaw(signed_pcm[i]);
                     
                     int dif = (((val - mem) << (32 - Mulaw.B)) >> (32 - Mulaw.B));
                     
                     int cdw;
                     
                     if (dif >= 0) {
                       cdw = (((+dif    ) << 1)    );
                     } else {
                       cdw = (((-dif - 1) << 1) + 1);
                     }
                     
                     dif_n.aI(cdw);
                     
                     // check that this mess is reversible
                     {
                       int fid;
                       
                       if ((cdw & 1) == 0) {
                         fid = +((cdw >> 1)    );
                       } else {
                         fid = -((cdw >> 1) + 1);
                       }
                       
                       if (fid != dif) throw null;
                       
                       int lav = (((mem + fid) << (32 - Mulaw.B)) >> (32 - Mulaw.B));
                       
                       if (lav != val) throw null;
                     }
                     
                     mem = val;
                   }
                   
                   dif_n.release();
                 }
                 
                 NaturalNumberCodec.enable_native_code(true);
                 NaturalNumberCodec.encode(central, dif_x, buf_x, aux, 32);
                 
                 encoded = buf_x.toNewArrayB();
                 
                 dif_x.release();
                 buf_x.release();
               }
               
               GroupirPacket groupir = (new GroupirPacket(emptyL, metadata, emptyS, encoded));
               
               // leave old gunk
               
               writeOldGroupirTo(central, groupir, globals.write_queue);
             } catch (Exception e) {
               throw (new RuntimeException(e));
             }
           }
         }));
  }
  
  /***
   * LEAVE AUDIO TEACHER
   ***/
  
  /***
   * ENTER FROB TEACHER
   ***/
  
  static void launch_frob_teacher(final Globals globals) throws Exception
  {
    final String organization = globals.settings.get("teacher.uplink.organization");
    final String channel      = globals.settings.get("teacher.instance.channel");
    final String credential   = globals.settings.get("teacher.uplink.channel." + channel + ".credential");
    
    final String http_host = globals.settings.get("teacher.uplink.http.host");
    final String http_path = globals.settings.get("teacher.uplink.http.path");
    
    // frob control
    {
      final int splitter_stream_id_frob = Integer.parseInt(globals.settings.get("teacher.uplink.splitter.stream.frob"));
      
      final BufferCentral central = (new BufferCentral(9));
      
      JFrame frame = (new JFrame("Network Frobber"));
      
      for (int j = 0; j < 9; j++) {
        final int i = j;
        
        JButton button = (new JButton(("" + i)));
        
        button.addActionListener
          ((new ActionListener()
            {
              public void actionPerformed(ActionEvent e)
              {
                SplitterCommon.Groupir.Packet groupir = (new SplitterCommon.Groupir.Packet(central, false, true, false, false));
                
                {
                  final long now = microTime();
                  
                  Buffer.nI metadata_n = groupir.bufI.prepend();
                  metadata_n.aI(((int)(now >> 32)));      // capture timecode
                  metadata_n.aI(((int)(now      )));      // capture timecode
                  metadata_n.aI(splitter_stream_id_frob); // stream id
                  metadata_n.aI(i);                       // versatile value
                  metadata_n.release();
                }
                
                writeSplitterCommonGroupirTo(central, groupir, globals.write_queue);
                
                groupir.release();
              }
            }));
        
        frame.getContentPane().setLayout((new FlowLayout()));
        frame.getContentPane().add(button);
        frame.pack();
        frame.setVisible(true);
      }
    }
    
    // feedback window
    {
      final JTextArea textarea = ConvenientUI.launchScrollingTextArea("Network Feedback");
      
      (new Thread()
        {
          public void run()
          {
            try {
              int marker = 0;
              final ArrayList<String> messages = (new ArrayList<String>());
              
              final HttpARQ http = (new HttpARQ());
              
              while (true) {
                final ArrayList<String> update_messages = (new ArrayList<String>());
                
                // fetch
                {
                  http.clearBuffer();
                  http.fetchURL(("http://" + http_host + http_path + "pull-mesg/" + organization + "/" + channel + "/" + credential + "/" + marker + "/" + Math.max(0, Math.abs(System.nanoTime()))));
                  
                  for (String line : NoUTF.tokenize(NoUTF.bin2str(http.getBuffer()), FILTER_TOKENIZE)) {
                    marker++;
                    
                    String str = NoUTF.bin2str(HexStr.hex2bin(line), NoUTF.FILTER_PRINTABLE);
                    
                    {
                      final String prefix = "msg: ";
                      
                      if (str.startsWith(prefix)) {
                        update_messages.add(str.substring(prefix.length()));
                      }
                    }
                  }
                }
                
                for (String message : update_messages) {
                  messages.add(message);
                }
                
                SwingUtilities.invokeLater
                  (new Runnable()
                    {
                      public void run()
                      {
                        for (String message : update_messages) {
                          textarea.append(message);
                          textarea.append("\n");
                        }
                      }
                    });
              }
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        }).start();
    }
  }
  
  /***
   * LEAVE FROB TEACHER
   ***/
  
  /***
   * ENTER ETCH TEACHER
   ***/
  
  static void launch_etch_teacher(final Globals globals) throws Exception
  {
    final BufferCentral central = (new BufferCentral(9));
    
    final String organization = globals.settings.get("teacher.uplink.organization");
    final String channel      = globals.settings.get("teacher.instance.channel");
    final String credential   = globals.settings.get("teacher.uplink.channel." + channel + ".credential");
    
    final String http_host = globals.settings.get("teacher.uplink.http.host");
    final String http_path = globals.settings.get("teacher.uplink.http.path");
    
    final boolean resumed = (globals.settings.get("teacher.session.resumed").equals("true"));
    
    final String filepath_jarfile = globals.settings.get("teacher.instance.etch.jarfile");
    
    final int splitter_stream_id_jarfile = Integer.parseInt(globals.settings.get("teacher.uplink.splitter.stream.etch.jarfile"));
    final int splitter_stream_id_events  = Integer.parseInt(globals.settings.get("teacher.uplink.splitter.stream.etch.events"));
    
    final int ETCH_H = Integer.parseInt(globals.settings.get("teacher.session.H"));
    final int ETCH_W = Integer.parseInt(globals.settings.get("teacher.session.W"));
    
    final LinkedBlockingQueue<Etch.OrganicInputEvent> capture  = (new LinkedBlockingQueue<Etch.OrganicInputEvent>());
    final LinkedBlockingQueue<Etch.OrganicInputEvent> transmit = (new LinkedBlockingQueue<Etch.OrganicInputEvent>());
    
    final Etch etch = (new Etch(ETCH_H, ETCH_W, capture, EtchTeacher.UPDATE_INTERVAL));
    
    if (!resumed) {
      Log.log("uploading jarfile ...");
      {
        // upload jarfile
        {
          DataInputStream inp =
            (new DataInputStream
             (new FileInputStream
              (filepath_jarfile)));
          
          byte[] buf = SimpleIO.slurp(inp);
          
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
            
            writeSplitterCommonGroupirTo(central, groupir, globals.write_queue);
            
            groupir.release();
          }
        }
      }
      Log.log("uploaded jarfile ...");
    } else {
      final long backpost = (MicroTime.now() - 1000000); /* 1 second in the past */
      
      Log.log("downloading event history ...");
      {
        final HttpARQ http = (new HttpARQ());
        
        http.clearBuffer();
        http.fetchURL(("http://" + http_host + http_path + "pull/" + organization + "/" + channel + "/" + credential + "/ENTER/" + (1 << 30) + "/" + Math.max(0, Math.abs(System.nanoTime())) + "/etch-event-history"));
        
        byte[] buf = http.getBuffer();
        int    off = http.getOffset();
        int    amt = http.getLength();
        
        int next_marker_length;
        
        // extract next marker length (last byte)
        {
          if (amt < 1) throw null;
          next_marker_length = (((int)(buf[off + amt - 1])) & 0xFF);
          amt -= 1;
        }
        
        // extract next marker (trailing bytes)
        {
          if (amt < next_marker_length) throw null;
          amt -= next_marker_length;
        }
        
        {
          Buffer.xB buffer = central.acquireB();
          {
            Buffer.nB buffer_n = buffer.prepend();
            Buffer.sB.copy(buffer_n, buf, off, amt);
            {
              Groupir2g.SteamRoller roller = (new Groupir2g.SteamRoller());
              SimpleDeque<Groupir2g.Packet> queue = (new SimpleDeque<Groupir2g.Packet>());
              Groupir2g.decode(central, (new SplitterCommon.Groupir.ChecksumAssistant.SHA2()), buffer, queue, roller);
              {
                final Etch.OrganicInputEvent.DecodeCreator decoder = (new Etch.OrganicInputEvent.DecodeCreator());
                
                final Etch.OrganicInputEvent.DecodeCreator.TimecodeAdjuster adjuster =
                        (new Etch.OrganicInputEvent.DecodeCreator.TimecodeAdjuster()
                          {
                            public long invoke(long ustc)
                            {
                              return backpost;
                            }
                          });
                
                while (!(queue.isEmpty())) {
                  Groupir2g.Packet packet = queue.removeFirst();
                  {
                    Buffer.oI oI = packet.dI.iterate();
                    
                    // skip timecode
                    oI.rI();
                    oI.rI();
                    
                    if ((oI.rI() == splitter_stream_id_events)) {
                      final int versatile = oI.rI();
                      
                      // populate event list
                      {
                        Buffer.xI tmp = central.acquireI();
                        
                        NaturalNumberCodec.decode(central, tmp, packet.dB);
                        
                        Buffer.oI tmp_o = tmp.iterate();
                        
                        decoder.setInputSource(tmp_o, 0);
                        
                        for (int i = 0; i < versatile; i++) {
                          Etch.OrganicInputEvent event = decoder.decode(adjuster);
                          Log.log("event history: etch.decode: now=" + MicroTime.now() + ", event.ustc=" + event.ustc);
                          etch.submit(event);
                        }
                        
                        tmp_o.release();
                        
                        tmp.release();
                      }
                    }
                    
                    oI.release();
                  }
                  packet.release();
                }
              }
              roller.release();
            }
            buffer_n.release();
            buffer_n = null;
          }
          buffer.release();
          buffer = null;
        }
      }
      Log.log("downloaded event history");
      
      // submit a synthetic close event, to prevent continuing an extant curve after recovery
      etch.submit((new Etch.CloseTabletInputEvent(backpost, 0, 0)));
      // place the same in the event queue, to keep both sides in sync
      transmit.put((new Etch.CloseTabletInputEvent(MicroTime.now(), 0, 0)));
      
      try {
        Thread.sleep(500);
      } catch (Throwable e) {
        Log.log(e);
      }
    }
    
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
           (filepath_jarfile))));
      
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
                  
                  sleep(EtchTeacher.EVENT_COLLECTION_INTERVAL_MS);
                  
                  round.clear();
                  
                  while ((round.size() < EtchTeacher.EVENT_BATCH_LIMIT) && (event = transmit.poll()) != null) {
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
                  
                  writeSplitterCommonGroupirTo(central, groupir, globals.write_queue);
                  
                  groupir.release();
                }
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
    }
  }

  /***
   * LEAVE ETCH TEACHER
   ***/
  
  /***
   * ENTER NWED TEACHER
   ***/
  
  static class NwedTeacherContainer
  {
    static class Event
    {
      final long tc;
      final int  pl;
      
      Event(boolean magic, char typed)
      {
        this.tc = SplitterCommon.Utilities.microTime();
        this.pl = ((magic ? (1 << 31) : 0) | (((int)(typed)) & 0xFF));
      }
    }
    
    static void launch_nwed_teacher(final Globals globals) throws Exception
    {
      final BufferCentral central = (new BufferCentral(9));
      
      final int splitter_srid = Integer.parseInt(globals.settings.get("teacher.uplink.splitter.stream.nwed"));
      final String virthome = globals.settings.get("teacher.capture.nwed.virthome");
      
      final NetworkEditor editor = (new NetworkEditor());
      
      final LinkedBlockingQueue<Event> queue = (new LinkedBlockingQueue<Event>());
      
      final F2<Void, Boolean, Character> onNetworkEditorEvent =
        (new F2<Void, Boolean, Character>()
         {
           public Void invoke(Boolean magic, Character typed)
           {
             // prepare event for accurate timecode
             Event event = (new Event(magic, typed));
             
             // process locally
             {
               if (magic) {
                 editor.onMagic(typed);
               } else {
                 editor.onGlyph(typed);
               }
             }
             
             // enqueue event
             {
               try {
                 queue.put(event);
               } catch (InterruptedException e) {
                 throw (new RuntimeException(e));
               }
             }
             
             return null;
           }
         });
      
      (new Thread()
        {
          public void run()
          {
            try {
              final ArrayList<Event> local = (new ArrayList<Event>());
              
              while (true) {
                final Event first;
                
                local.add((first = queue.take()));
                Thread.sleep(100);
                queue.drainTo(local, 99);
                
                if (local.size() == 1) {
                  SplitterCommon.Groupir.Packet packet = (new SplitterCommon.Groupir.Packet(central, false, true, false, false));
                  
                  {
                    Buffer.nI metadata_n = packet.bufI.prepend();
                    metadata_n.aI(((int)(first.tc >> 32))); // timecode
                    metadata_n.aI(((int)(first.tc      ))); // timecode
                    metadata_n.aI(splitter_srid);           // stream id
                    metadata_n.aI(first.pl);                // versatile value
                    metadata_n.release();
                    metadata_n = null;
                  }
                  
                  writeSplitterCommonGroupirTo(central, packet, globals.write_queue);
                  
                  packet.release();
                } else {
                  SplitterCommon.Groupir.Packet packet = (new SplitterCommon.Groupir.Packet(central, false, true, false, true));
                  
                  {
                    Buffer.nI metadata_n = packet.bufI.prepend();
                    metadata_n.aI(((int)(first.tc >> 32))); // timecode
                    metadata_n.aI(((int)(first.tc      ))); // timecode
                    metadata_n.aI(splitter_srid);           // stream id
                    metadata_n.aI(-1);                      // versatile value
                    metadata_n.release();
                    metadata_n = null;
                  }
                  
                  {
                    Buffer.nB payload_n = packet.bufB.prepend();
                    
                    for (Event event : local) {
                      boolean magic = ((event.pl & (1 << 31)) != 0);
                      payload_n.aB(((byte)((magic ? 0x80 : 0) | ((int)((byte)(event.pl))))));
                    }
                    
                    payload_n.release();
                    payload_n = null;
                  }
                  
                  writeSplitterCommonGroupirTo(central, packet, globals.write_queue);
                  
                  packet.release();
                }
                
                local.clear();
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
      
      Toolkit.getDefaultToolkit().getSystemEventQueue().push
        (new java.awt.EventQueue()
          {
            protected void dispatchEvent(AWTEvent event)
            {
              if (event instanceof KeyEvent) {
                KeyEvent e = ((KeyEvent)(event));
                
                if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_TAB)) {
                  onNetworkEditorEvent.invoke(true, 't');
                  e.consume();
                  return;
                }
              }
              
              super.dispatchEvent(event);
            }
          });
      
      editor.addKeyListener
        ((new KeyListener()
          {
            public void keyPressed(KeyEvent event)
            {
              try {
                switch (event.getKeyCode()) {
                case KeyEvent.VK_UP:         onNetworkEditorEvent.invoke(true, '^'); break;
                case KeyEvent.VK_DOWN:       onNetworkEditorEvent.invoke(true, '_'); break;
                case KeyEvent.VK_LEFT:       onNetworkEditorEvent.invoke(true, '<'); break;
                case KeyEvent.VK_RIGHT:      onNetworkEditorEvent.invoke(true, '>'); break;
                case KeyEvent.VK_TAB:        onNetworkEditorEvent.invoke(true, 't'); break;
                case KeyEvent.VK_ENTER:      onNetworkEditorEvent.invoke(true, 'n'); break;
                case KeyEvent.VK_BACK_SPACE: onNetworkEditorEvent.invoke(true, 'b'); break;
                case KeyEvent.VK_DELETE:     onNetworkEditorEvent.invoke(true, 'b'); break;
                case KeyEvent.VK_ESCAPE:     onNetworkEditorEvent.invoke(true, 'e'); break;
                  
                case KeyEvent.VK_P:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '^'); break;
                case KeyEvent.VK_N:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '_'); break;
                case KeyEvent.VK_B:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '<'); break;
                case KeyEvent.VK_F:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '>'); break;
                case KeyEvent.VK_D:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, 'd'); break;
                case KeyEvent.VK_K:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, 'k'); break;
                  
                case KeyEvent.VK_Q:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, 'q'); break;
                  
                case KeyEvent.VK_1:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '1'); break;
                case KeyEvent.VK_2:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '2'); break;
                case KeyEvent.VK_3:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '3'); break;
                case KeyEvent.VK_4:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '4'); break;
                case KeyEvent.VK_5:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '5'); break;
                case KeyEvent.VK_6:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '6'); break;
                case KeyEvent.VK_7:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '7'); break;
                case KeyEvent.VK_8:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '8'); break;
                case KeyEvent.VK_9:          if (event.isControlDown()) onNetworkEditorEvent.invoke(true, '9'); break;
                  
                  // local-only events
                  
                case KeyEvent.VK_S:
                  {
                    if (event.isControlDown()) {
                      BufferedWriter out = (new BufferedWriter(new FileWriter((virthome + "/nwedvirt" + editor.getCurrentBufferIndex()))));
                      out.write(editor.getCurrentBufferBytes());
                      out.close();
                    }
                    
                    break;
                  }
                  
                  
                case KeyEvent.VK_L:
                  {
                    if (event.isControlDown()) {
                      BufferedReader inp = (new BufferedReader(new FileReader((virthome + "/nwedvirt" + editor.getCurrentBufferIndex()))));
                      
                      {
                        String line;
                        
                        for (int i = 0, l = editor.getCurrentBufferLineCount(); i < l; i++) {
                          onNetworkEditorEvent.invoke(true, 'k');
                        }
                        
                        while ((line = inp.readLine()) != null) {
                          for (char x : line.toCharArray()) {
                            onNetworkEditorEvent.invoke(false, x);
                          }
                          
                          onNetworkEditorEvent.invoke(true, 'n');
                        }
                      }
                      
                      inp.close();
                    }
                    
                    break;
                  }
                }
              } catch (Throwable e) {
                fatal(e);
              }
            }
            
            public void keyReleased(KeyEvent event)
            {
              // nothing to do
            }
            
            public void keyTyped(KeyEvent event)
            {
              try {
                char x;
                
                if ((event.getModifiers() & (~InputEvent.SHIFT_MASK)) == 0) {
                  if ((x = event.getKeyChar()) != KeyEvent.CHAR_UNDEFINED) {
                    if ((' ' <= x) && (x <= '~')) {
                      onNetworkEditorEvent.invoke(false, x);
                    }
                  }
                }
              } catch (Throwable e) {
                fatal(e);
              }
            }
          }));
      
      final JFrame frame = (new JFrame("Network Editor"));
      
      frame.getContentPane().add(editor.getScrollPane());
      frame.setSize(800, 600);
      frame.setVisible(true);
    }
  }
  
  /***
   * LEAVE NWED TEACHER
   ***/
  
  /***
   * for now, Teacher2g is not an applet; it only works as a regular
   * java application.
   ***/
  public static void main(String[] args)
  {
    Log.loopHandleEventsBackground(System.err, true);
    
    try {
      final Settings settings = (new Settings());
      
      // load (possibly multiple) settings files
      {
        for (String argi : args) {
          settings.scan("", argi);
        }
      }
      
      final String organization = settings.get("teacher.uplink.organization");
      final String channel      = settings.get("teacher.instance.channel");
      final String credential   = settings.get("teacher.uplink.channel." + channel + ".credential");
      
      final String http_host = settings.get("teacher.uplink.http.host");
      final String http_path = settings.get("teacher.uplink.http.path");
      
      final String splitter_host = settings.get("teacher.uplink.splitter.host");
      final int    splitter_port = Integer.parseInt(settings.get("teacher.uplink.splitter.port"));
      
      if (!settings.has("teacher.session.authenticated")) {
        final byte[] teacher_session_bytes;
        
        // launch channel
        {
          String url = ("http://" + http_host + http_path + "teacher-login/" + organization + "/" + channel + "/" + credential + "/simple/" + Math.max(0, Math.abs(System.nanoTime())));
          
          HttpURLConnection con = ((HttpURLConnection)((new URL(url)).openConnection()));
          
          con.setRequestMethod("GET");
          con.setDoInput(true);
          
          con.setConnectTimeout(TIMEOUT_MS);
          con.setReadTimeout(TIMEOUT_MS);
          
          con.connect();
          
          InputStream inp = con.getInputStream();
          teacher_session_bytes = SimpleIO.slurp(inp);
          inp.close();
        }
        
        // verify sanity of session data
        {
          settings.scan("", (new ByteArrayInputStream(teacher_session_bytes)));
          
          for (String key : (new String[] { "authenticated", "KMAC", "H", "W" })) {
            settings.get(("teacher.session." + key));
          }
        }
        
        // save session data
        {
          FileOutputStream fos = (new FileOutputStream("session.xml"));
          fos.write(teacher_session_bytes);
          fos.close();
        }
        
        // set non-resumed, just this once ...
        {
          FileOutputStream fos = (new FileOutputStream("resumed.xml"));
          fos.write(NoUTF.str2bin("<s><k>teacher.session.resumed</k><v>false</v></s>"));
          fos.close();
        }
      }
      
      if (!settings.has("teacher.session.calling")) {
        // TODO: here it may be good to tell the server to "estoppel"
        // new data on existing splitter connections as we are about
        // to take over and we can't handle crosstalk from un-killed
        // zombie sessions or un-pushed tcp buffers that may
        // eventually be flushed
        
        // launch separate VMs for different capture tools
        {
          if (!(settings.get("system.command.stoppel").equals("false"))) throw null;
          
          final String header = settings.get("system.command.self.header");
          final String footer = settings.get("system.command.self.footer");
          
          ArrayList<String> prefix = (new ArrayList<String>());
          ArrayList<String> suffix = (new ArrayList<String>());
          
          for (String section : NoUTF.tokenize(settings.get("system.command.self.prefix.sections"), FILTER_TOKENIZE)) {
            prefix.add(settings.get("system.command.self.prefix.section." + section));
          }
          
          for (String section : NoUTF.tokenize(settings.get("system.command.self.suffix.sections"), FILTER_TOKENIZE)) {
            suffix.add(settings.get("system.command.self.suffix.section." + section));
          }
          
          for (String callee : (new String[] { "audio", "frob", "etch", "nwed" })) {
            ArrayList<String> command = (new ArrayList<String>());
            
            command.addAll(prefix);
            command.add(("CALLING=" + callee + "; " + header + (" ./session.xml ./calling-" + callee + ".xml ") + footer));
            command.addAll(suffix);
            
            {
              System.err.println("ENTER PARAMETER BLOCK");
              
              for (String param : command) {
                System.err.println("param: '" + param + "'");
              }
              
              System.err.println("LEAVE PARAMETER BLOCK");
            }
            
            Runtime.getRuntime().exec(command.toArray((new String[0])));
          }
        }
      } else {
        final String calling = settings.get("teacher.session.calling");
        
        final byte[] teacher_kmac = HexStr.hex2bin(settings.get("teacher.session.KMAC"));
        
        Log.log("will use KMAC '" + HexStr.bin2hex(teacher_kmac) + "'");
        
        final Groupir2g.WriteQueue write_queue =
          (new Groupir2g.WriteQueue
           ((new SplitterCommon.Groupir.ChecksumAssistant.KMAC(teacher_kmac)),
            (NetworkingUtilities.connect(splitter_host, splitter_port).getOutputStream())));
        
        final Globals globals = (new Globals(settings, write_queue));
        
        Log.log("calling=" + calling);
        
        /****/ if (calling.equals("audio")) {
          launch_audio_teacher(globals);
        } else if (calling.equals("frob")) {
          launch_frob_teacher(globals);
        } else if (calling.equals("etch")) {
          launch_etch_teacher(globals);
        } else if (calling.equals("nwed")) {
          NwedTeacherContainer.launch_nwed_teacher(globals);
        } else {
          throw null;
        }
      }
    } catch (Throwable e) {
      Log.log(e);
    }
    
    /* if there was a non-fatal exception, give it time to print */
    try {
      Thread.sleep(500);
    } catch (Throwable e) {
      // gah
    }
  }
}

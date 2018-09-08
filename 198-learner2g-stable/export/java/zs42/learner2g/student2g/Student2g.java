/***
 * Student2g.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.student2g;

import zs42.learner2g.lantern2g.ny4j.bind.*;

import zs42.learner2g.groupir2g.*;

import zs42.mass.*;
import zs42.buff.*;
import zs42.ny4j.*;

import zs42.nats.codec.*;

import zs42.au2g.*;
import zs42.au2g.ny4j.bind.*;

import zs42.addc.*;

import gotm.etch.*;

import zs42.parts.*;

import zs42.splitter.common.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import static zs42.parts.Static.cast_unchecked;
import static zs42.parts.Log.T;

// disambiguation (ugh)
import zs42.mass.F0;
import zs42.mass.F2;
import zs42.mass.Nothing;

public class Student2g extends java.applet.Applet
{
  public static final int NETWORK_BUFFER_BYTES = 65536;
  
  public static final int STATION_USER_INTERFACE_LATENCY_MS = 128;
  
  public static final long ONE_SECOND_NS = 1000000000L;
  
  public static final long WATCHDOG_TIMEOUT_NS = (10 * ONE_SECOND_NS); /* 10 seconds */
  
  static final Charset UTF_8;
  
  static
  {
    try {
      UTF_8 = Charset.forName("UTF-8");
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
  }
  
  static String defaulting(String val, String def)
  {
    return ((val != null) ? val : def);
  }
  
  static boolean enabled(String setting)
  {
    return ((setting != null) && (setting.equals("true") || setting.equals("TRUE") || setting.toUpperCase().equals("TRUE")));
  }
  
  static final AtomicBoolean debugWindowTriggerable = (new AtomicBoolean(true));
  
  static void enableDebugWindow()
  {
    if (debugWindowTriggerable.getAndSet(false)) {
      // enable debug window
      SwingUtilities.invokeLater
        ((new Runnable()
          {
            public void run()
            {
              final JTextArea textarea = (new JTextArea());
              textarea.setEditable(false);
              textarea.setFont((new Font(Font.MONOSPACED, Font.BOLD, 14)));
              
              final JScrollPane scrollpane = (new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
              scrollpane.setViewportBorder((new javax.swing.border.LineBorder(Color.WHITE, 5)));
              
              final JFrame frame = (new JFrame("Debug Log"));
              frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
              frame.getContentPane().add(scrollpane);
              
              frame.setSize(800, 600);
              frame.setVisible(true);
              
              (new javax.swing.Timer
               (100,
                (new ActionListener()
                  {
                    public void actionPerformed(ActionEvent e)
                    {
                      Log.handleEventsNonblocking
                        (new Log.EventHandler()
                          {
                            void L(String line)
                            {
                              textarea.append(line);
                              textarea.append("\n");
                            }
                            
                            void dumpException(Throwable exception)
                            {
                              L(exception.toString());
                              
                              for (StackTraceElement element : exception.getStackTrace()) {
                                L("    " + element.toString());
                              }
                            }
                            
                            public void onMessage(long ustc, Thread thread, String message)
                            {
                              L("@" + ustc + ": " + thread + ": " + message);
                            }
                            
                            public void onException(long ustc, Thread thread, Throwable exception)
                            {
                              L("@" + ustc + ": " + thread + ": (enter exception)");
                              dumpException(exception);
                              L("(leave exception)");
                            }
                            
                            public void onAnnotatedException(long ustc, Thread thread, String message, Throwable exception)
                            {
                              L("@" + ustc + ": " + thread + ": " + message + " (enter exception)");
                              dumpException(exception);
                              L("(leave exception)");
                            }
                            
                            public void onAbortException(long ustc, Thread thread, Throwable exception)
                            {
                              /*
                                aborting isn't supported -- just log it
                              */
                              
                              onException(ustc, thread, exception);
                            }
                          });
                    }
                  }))).start();
            }
          }));
    }
  }
  
  static abstract class NetworkDescriptor
  {
    abstract boolean isRealtime();
    abstract String[] getOriginDescriptor();
    
    int getNetworkBufferCount() { return (isRealtime() ? 2 : 8); }
    int getAudioBufferCount() { return 4; }
  }
  
  static class Instance
  {
    final JNylus.Manager manager = (new JNylus.ExponentialBackoffManager(16, 16, 128));
    final JNylus.Station station_entry = manager.newStation();
    
    final BufferCentral central = (new BufferCentral(9));
    final ByteArrayCache byteArrayCache = (new ByteArrayCache());
    
    final HashMap<String, String> settings;
    final String student_name;
    final String pseudo_auth;
    final boolean support_rewind;
    final boolean enforce_rewind;
    final NetworkDescriptor network_descriptor;
    final SplitterCommon.Groupir.ChecksumAssistant checksum_assistant;
    
    final Etch.OrganicInputEvent.DecodeCreator decoder = (new Etch.OrganicInputEvent.DecodeCreator());
    
    final AudioReplayAgent          agent_audio_replay;
    final AudioMixerEnumeratorAgent agent_mixer_enumerator;
    final UserInterfaceAgent        agent_user_interface;
    
    NetworkEndpointAgent agent_network_endpoint;
    NetworkIncomingAgent agent_network_incoming;
    NetworkOutgoingAgent agent_network_outgoing;
    
    int outstanding_spinoff = 0;
    
    /***
     * <code>Packet</code>s are basically groupir payload data, out of
     * which a number of fields have been pulled out and cached for
     * efficiency reasons.
     ***/
    static class Packet
    {
      /***
       * stream identifier.
       ***/
      Groupir2g.StreamIdentifier streamIdentifier;
      
      /***
       * microsecond timecode.
       ***/
      long ustc;
      
      /***
       * "versatile" value.
       ***/
      int versatile;
      
      /***
       * groupir packet data.
       ***/
      //Groupir.Packet payload; // fields other than bytes are not used much, so ...
      
      /***
       * groupir packet byte data.
       ***/
      Buffer.xB payload;
    }
    
    static byte a2u(int a)
    {
      return ((byte)(a + 128));
    }
    
    static final Random getAudioPacketSamplesRandom = (new Random());
    
    Buffer.xS getAudioPacketSamples(Packet input)
    {
      Buffer.xS output_samples = central.acquireS();
      
      Buffer.nS output_samples_n = output_samples.prepend();
      {
        switch (input.versatile) {
        case 0: /* 8-bit mulaw */
          {
            Buffer.oB input_payload_o = input.payload.iterate();
            {
              int rem = input_payload_o.remaining();
              
              if (!(rem == Audio2g.SAMPLE_SIZE_RECORD_NR)) throw null;
              
              while (rem-- > 0) {
                output_samples_n.aS(Unlaw.unlaw[(((int)(input_payload_o.rB())) & 0xFF)]);
              }
            }
            input_payload_o.release();
            
            break;
          }
          
        case 1: /* 8-bit mulaw differential nats (v1) */
          {
            Buffer.xI dif = central.acquireI();
            
            NaturalNumberCodec.decode(central, dif, input.payload);
            
            Buffer.oI dif_o = dif.iterate();
            
            int dif_len = dif_o.remaining();
            
            if (!(dif_len == Audio2g.SAMPLE_SIZE_RECORD_NR)) throw null;
            
            int mem = 0;
            
            while (dif_len-- > 0) {
              int cdw = dif_o.rI();
              
              int v;
              
              if ((cdw & 1) == 0) {
                v = (mem += (cdw >> 1));
              } else {
                v = (mem -= ((cdw >> 1) + 1));
              }
              
              output_samples_n.aS(Unlaw.unlaw[(v & 0xFF)]);
            }
            
            dif_o.release();
            
            dif.release();
            
            break;
          }
          
        case 2: /* 8-bit mulaw differential nats (v2) */
          {
            Buffer.xI dif_x = central.acquireI();
            
            NaturalNumberCodec.decode(central, dif_x, input.payload);
            
            Buffer.oI dif_o = dif_x.iterate();
            
            int dif_len = dif_o.remaining();
            
            if (!(dif_len == Audio2g.SAMPLE_SIZE_RECORD_NR)) throw null;
            
            int mem = 0;
            
            while (dif_len-- > 0) {
              int cdw = dif_o.rI();
              
              int dif;
              
              if ((cdw & 1) == 0) {
                dif = +((cdw >> 1)    );
              } else {
                dif = -((cdw >> 1) + 1);
              }
              
              int val = ((mem + dif) & 0xFF);
              
              output_samples_n.aS(Unlaw.unlaw[(a2u(val) & 0xFF)]);
              
              mem = val;
            }
            
            dif_o.release();
            
            dif_x.release();
            
            break;
          }
          
        case 3: /* 10-bit mulaw addc differential nats */
          {
            Buffer.xI dif_x = central.acquireI();
            
            NaturalNumberCodec.decode(central, dif_x, input.payload);
            
            Buffer.oI dif_o = dif_x.iterate();
            
            int dif_len = dif_o.remaining();
            
            if (!(dif_len == Audio2g.SAMPLE_SIZE_RECORD_NR)) throw null;
            
            int mem = 0;
            
            while (dif_len-- > 0) {
              int cdw = dif_o.rI();
              
              int fid;
              
              if ((cdw & 1) == 0) {
                fid = +((cdw >> 1)    );
              } else {
                fid = -((cdw >> 1) + 1);
              }
              
              int lav = (((mem + fid) << (32 - Mulaw.B)) >> (32 - Mulaw.B));
              
              output_samples_n.aS(Mulaw.Unlaw.unlaw(lav, getAudioPacketSamplesRandom));
              
              mem = lav;
            }
            
            dif_o.release();
            
            dif_x.release();
            
            break;
          }
        }
      }
      output_samples_n.release();
      
      if (!(output_samples.length() == Audio2g.SAMPLE_SIZE_RECORD_NR)) throw null;
      
      return output_samples;
    }
    
    static enum VolumeHeuristic
    {
      SILENT
        {
          boolean actionable() { return true; }
        },
      INDETERMINATE
        {
          boolean actionable() { return false; }
        },
      TALKING
        {
          boolean actionable() { return true; }
        };
    }
    
    VolumeHeuristic calculateVolumeHeuristic(Packet input)
    {
      if (!(input.streamIdentifier.getDisposition() == Groupir2g.StreamDisposition.PRIMARY)) throw null;
      
      int len;
      int sum = 0;
      
      {
        Buffer.xS input_samples = getAudioPacketSamples(input);
        Buffer.oS input_samples_o = input_samples.iterate();
        
        int rem = len = input_samples_o.remaining();
        
        while (rem-- > 0) {
          int val = input_samples_o.rS();
          sum += (val * val);
        }
        
        input_samples_o.release();
        input_samples.release();
      }
      
      if (len > 0) {
        sum /= len;
      }
      
      /****/ if (sum < silence_threshold) {
        return VolumeHeuristic.SILENT;
      } else {
        return VolumeHeuristic.TALKING;
      }
    }
    
    /***
     * ENTER STATE VARIABLES
     ***/
    
    volatile boolean shutdown = false;
    volatile boolean shutdown_completed = false;
    
    cL<Audio2g.MixerTarget> detected_mixers = cL.getSharedEmpty();
    
    Audio2g.MixerTarget selected_mixer_target = Audio2g.AudioMixerEnumerator.getSystemDefaultMixerTarget(SourceDataLine.class);
    Audio2g.MixerFormat selected_mixer_format = selected_mixer_target.getDefaultFormat();
    
    Audio2g.Effects.AdjustableVolumeFilter filter_volume = (new Audio2g.Effects.AdjustableVolumeFilter(central, calculateVolumeMultiplier(0.5)));
    Audio2g.Effects.Filter filter =
      (new F0<Audio2g.Effects.Filter>()
       {
         public Audio2g.Effects.Filter invoke()
         {
           Audio2g.Effects.Filter filter =
             (new Audio2g.Effects.ChainFilter
              (central,
               (new Audio2g.Effects.LanczosUpscaleFilter(central, 128, 6)), filter_volume));
           
           filter.reset();
           
           return filter;
         }
       }).invoke();
    
    long watchdog_last_triggered = System.nanoTime();
    boolean watchdog_alarm_active = true;
    
    // the "tail" buffer contains bytes most recently obtained from the connection
    Buffer.xB buffer_tail = central.acquireB();
    
    final SimpleDeque<Groupir2g.Packet> pending_unclassified = (new SimpleDeque<Groupir2g.Packet>());
    
    // history is preallocated to fit two streams for thirty minutes of continuous quarter-second packets
    // this prevents potentially costly reallocation from interfering with realtime processing
    // history is populated only if "support_rewind" is true, to facilitate rewinding on audio device change
    // only PRIMARY and TRAILER packets are included in history; INSTANT not
    final SimpleDeque<Packet> history = (new SimpleDeque<Packet>(2 * 30 * 60 * 4));
    
    final SimpleDeque<Packet> pending_instant = (new SimpleDeque<Packet>());
    final SimpleDeque<Packet> pending_primary = (new SimpleDeque<Packet>());
    final SimpleDeque<Packet> pending_trailer = (new SimpleDeque<Packet>());
    
    Buffer.xB buffer_etch_jarfile   = central.acquireB();
    Buffer.nB buffer_etch_jarfile_n = buffer_etch_jarfile.prepend();
    int       buffer_etch_jarfile_z = 0;
    
    Object pseudo_cookie = (new Object());
    Object rewind_cookie = (new Object());
    
    int silence_threshold = 16;
    
    boolean skip_silence = false;
    boolean previous_packet_silence = false;
    
    // in realtime mode, silence skipping is triggered by an excess of primary packets
    int pending_primary_limit = 24; /* 6 seconds */
    
    /***
     * LEAVE STATE VARIABLES
     ***/
    
    double calculateVolumeMultiplier(double sliderFraction)
    {
      double multiplier;
      
      /****/ if (sliderFraction < 0.4) {
        multiplier = (2 * sliderFraction);
      } else if (sliderFraction < 0.6) {
        multiplier = 1.0;
      } else {
        multiplier = (2 * (2 * (sliderFraction - 0.5)));
      }
      
      return multiplier;
    }
    
    void onVolumeLevel(int val, int max)
    {
      max = Math.max(1, max);
      val = Math.max(0, Math.min(val, max));
      
      filter_volume.setMultiplier(calculateVolumeMultiplier((((double)(val)) / ((double)(max)))));
    }
    
    /***
     * places an incoming packet in the correct pending_whatever
     * stream.
     ***/
    void stagePending(Packet packet)
    {
      switch (packet.streamIdentifier.getDisposition()) {
      case INSTANT: pending_instant.addLast(packet); break;
      case PRIMARY: pending_primary.addLast(packet); break;
      case TRAILER: pending_trailer.addLast(packet); break;
      default: throw null;
      }
    }
    
    /***
     * processes "instant" packets.
     ***/
    void processInstant()
    {
      while (!(pending_instant.isEmpty())) {
        Packet packet = pending_instant.removeFirst();
        
        switch (packet.streamIdentifier) {
        case COMMAND:
          {
            if (packet.payload != null) {
              try {
                String[] tokens = NoUTF.bin2str(packet.payload.toNewArrayB()).split("\\s+");
                
                if ((tokens[0].equals("*") || pseudo_auth.startsWith(tokens[0]))) {
                  if (tokens[1].equals("setppl")) {
                    pending_primary_limit = Integer.parseInt(tokens[2]);
                  }
                  
                  if (tokens[1].equals("setsil")) {
                    silence_threshold = Integer.parseInt(tokens[2]);
                  }
                }
              } catch (Throwable e) {
                Log.log(e);
              }
            }
            
            break;
          }
          
        case ETCH_JARFILE_FRAGMENT:
          {
            // copy bytes
            {
              Buffer.oB payload_o = packet.payload.iterate();
              Buffer.sB.copy(buffer_etch_jarfile_n, payload_o, packet.payload.length());
              payload_o.release();
              buffer_etch_jarfile_z += packet.payload.length();
            }
            
            // enough?
            {
              if (buffer_etch_jarfile_z == packet.versatile) {
                buffer_etch_jarfile_n.release();
                buffer_etch_jarfile_n = null;
                
                ArrayList<Etch.InputEvent> events = (new ArrayList<Etch.InputEvent>());
                
                // populate backdrop loaded event list
                {
                  try {
                    JarInputStream inp = (new JarInputStream(new ByteArrayInputStream(buffer_etch_jarfile.toNewArrayB())));
                    JarEntry entry;
                    
                    int index = 0;
                    
                    while ((entry = inp.getNextJarEntry()) != null) {
                      if (entry.getName().endsWith(".png")) {
                        byte[] png = SimpleIO.slurp(inp);
                        events.add((new Etch.BackdropLoadedInputEvent(index++, png)));
                      }
                    }
                  } catch (IOException e) {
                    throw (new RuntimeException(e));
                  }
                }
                
                // submit backdrop loaded event list
                agent_user_interface.doSubmitEtchEvents(events);
              }
            }
            
            break;
          }
          
        default:
          {
            // ignore it
            break;
          }
        }
        
        // packet not linked from history, so release buffer here
        // TODO: why do nwed events get here? shouldn't they be in the history? they are replayable ... (ideally, if the network editor can be reset ... ugh)
        // UPDATE: nwed events do not get here anymore ...
        {
          if (packet.payload != null) {
            packet.payload.release();
          } else {
            Log.log("processInstant() w/o payload: packet.streamIdentifier=" + packet.streamIdentifier);
          }
        }
      }
    }
    
    final Audio2g.Packet postAudioPacketSpecimen = (new Audio2g.Packet());
    final Buffer.xS postAudioSilenceSamples =
      (new F0<Buffer.xS>()
       {
         public Buffer.xS invoke()
         {
           Buffer.xS samples = central.acquireS();
           
           {
             Buffer.nS samples_n = samples.prepend();
             
             int rem = Audio2g.SAMPLE_SIZE_RECORD_NR;
             
             while (rem-- > 0) {
               samples_n.aS(((short)(0)));
             }
             
             samples_n.release();
           }
           
           return samples;
         }
       }).invoke();
    
    long report_counter = 0;
    
    int pps_min = Integer.MAX_VALUE;
    int pps_max = Integer.MIN_VALUE;
    
    void pps_reset() {
      pps_min = Integer.MAX_VALUE;
      pps_max = Integer.MIN_VALUE;
    }
    
    { pps_reset(); }
    
    /***
     * post an audio packet to the AudioWriter.
     ***/
    void dequeuePrimaryPacket()
    {
      // gather and report queue length statistics
      {
        pps_min = Math.min(pps_min, pending_primary.size());
        pps_max = Math.max(pps_max, pending_primary.size());
        
        if (((report_counter++) & 0xF) == 0) {
          if (agent_network_outgoing != null) {
            String message = ("dbg: " + pseudo_auth + ": pps: min: " + pps_min + " max: " + pps_max);
            byte[] encoding = NoUTF.str2bin(message);
            agent_network_outgoing.doSend(encoding, 0, encoding.length);
            Log.log(message);
          }
          
          pps_reset();
        }
      }
      
      boolean real = false;
      long ustc = 0;
      
      Buffer.xS samples = central.acquireS();
      
      if (!(pending_primary.isEmpty())) {
        real = true;
        
        {
          Packet input = pending_primary.removeFirst();
          
          if (skip_silence || (network_descriptor.isRealtime() && (pending_primary.size() > pending_primary_limit))) {
            int skipped = 0;
            
            if (previous_packet_silence) {
              VolumeHeuristic input_vh = null;
              
              while ((!pending_primary.isEmpty()) && ((input_vh = calculateVolumeHeuristic(input)) == VolumeHeuristic.SILENT) && (pending_primary.size() > pending_primary_limit)) {
                if ((!support_rewind) && (input.payload != null)) {
                  input.payload.release();
                }
                
                input = pending_primary.removeFirst();
                skipped++;
              }
              
              previous_packet_silence = (input_vh == VolumeHeuristic.SILENT);
            } else {
              previous_packet_silence = (calculateVolumeHeuristic(input) == VolumeHeuristic.SILENT);
            }
            
            if (skipped > 0) {
              String message = ("dbg: " + pseudo_auth + ": cut: " + skipped);
              byte[] encoding = NoUTF.str2bin(message);
              agent_network_outgoing.doSend(encoding, 0, encoding.length);
              Log.log(message);
            }
          } else {
            previous_packet_silence = false;
          }
          
          ustc = input.ustc;
          
          Buffer.xS input_samples = getAudioPacketSamples(input);
          Buffer.sS.copy(samples, input_samples);
          input_samples.release();
          
          if ((!support_rewind) && (input.payload != null)) {
            input.payload.release();
          }
        }
      } else {
        Buffer.sS.copy(samples, postAudioSilenceSamples);
      }
      
      byte[] replay_buffer = byteArrayCache.obtain(Audio2g.AudioReplay.calculateBufferSize(selected_mixer_format));
      
      {
        Audio2g.Packet packet = postAudioPacketSpecimen;
        
        packet.addChannelBuffer(samples);
        
        Audio2g.Effects.filter(central, filter, packet);
        
        for (int i = 1; i < selected_mixer_format.getChannelCount(); i++) {
          packet.addChannelBuffer(packet.getChannelBuffer(0));
        }
        
        Audio2g.MixerFormat.encodeRawBytes(packet, replay_buffer, 0, selected_mixer_format);
        
        for (int i = 1; i < selected_mixer_format.getChannelCount(); i++) {
          packet.popChannelBuffer(false);
        }
        
        packet.reset(true);
      }
      
      int expected_duration_us = ((int)((((long)(Audio2g.SAMPLE_SIZE_REPLAY_NR)) * 1000000L) / ((long)(Audio2g.SAMPLE_RATE_REPLAY_HZ))));
      agent_audio_replay.doPutPacket(replay_buffer, expected_duration_us, ustc, (real ? rewind_cookie : pseudo_cookie));
    }
    
    void handleFrobCode(int versatile)
    {
      switch (versatile) {
      case 0:
        {
          agent_user_interface.doRaiseEtch();
          break;
        }
        
      case 1:
        {
          agent_user_interface.doRaiseNwed();
          break;
        }
      }
    }
    
    final long AGGREGATE_BEHIND_US = 1000000;
    final long QUEUE_AHEAD_US      = 1000000;
    final long ETCH_EVENT_DELAY_US =  375000;
    
    final Synchronizer synchronizer = (new Synchronizer.ConvergingSynchronizer());
    
    void dequeueTrailerPackets(long replay_timestamp, long packet_timestamp, Object cookie)
    {
      // fastskip
      if (replay_timestamp == 0) return;
      if (cookie != rewind_cookie) return;
      
      //Log.log("dequeueTrailerPackets(" + replay_timestamp + ", " + packet_timestamp + ")");
      
      synchronizer.notify(replay_timestamp, packet_timestamp);
      
      final long now = MicroTime.now();
      
      final ArrayList<Integer> nwed_events = (new ArrayList<Integer>());
      
      if (!(pending_trailer.isEmpty())) {
        if ((replay_timestamp + (pending_trailer.peekFirst().ustc - packet_timestamp)) < (now - AGGREGATE_BEHIND_US)) {
          ArrayList<Buffer.oB> payloads = (new ArrayList<Buffer.oB>());
          
          /***
           * queue up to 2*QUEUE_AHEAD_US in bulk because
           * (1) it minimizes the possibility of crosstalk w/ organic queue and
           * (2) it should result in less jumpy start as the system is warming up
           ***/
          while ((!(pending_trailer.isEmpty())) && ((replay_timestamp + (pending_trailer.peekFirst().ustc - packet_timestamp)) < (now + (2 * QUEUE_AHEAD_US)))) {
            Packet packet = pending_trailer.removeFirst();
            
            /****/ if (packet.streamIdentifier == Groupir2g.StreamIdentifier.ETCH_EVENT_BUNDLE) {
              payloads.add(packet.payload.iterate());
            } else if (packet.streamIdentifier == Groupir2g.StreamIdentifier.NWED_EVENT) {
              if (packet.versatile != -1) {
                nwed_events.add(packet.versatile);
              } else {
                for (byte encoding : packet.payload.toNewArrayB()) {
                  boolean magic = ((encoding & 0x80) != 0);
                  nwed_events.add(((magic ? (1 << 31) : 0) | (((int)(encoding)) & 0x7F)));
                }
              }
            } else if (packet.streamIdentifier == Groupir2g.StreamIdentifier.FROB_CODE) {
              handleFrobCode(packet.versatile);
            }
            
            // can't do this here due to packet.payload.iterate() for ETCH_EVENT_BUNDLE
            //if ((!support_rewind) && (packet.payload != null)) {
            //  packet.payload.release();
            //}
          }
          
          //Log.log("submitting " + payloads.size() + " etch event(s) (in bulk)");
          
          agent_user_interface.doSubmitEtchEventBundle((new Etch.EventBundleInputEvent(payloads)));
        } else {
          ArrayList<Etch.InputEvent> events = (new ArrayList<Etch.InputEvent>());
          
          while (((!(pending_trailer.isEmpty())) && ((replay_timestamp + (pending_trailer.peekFirst().ustc - packet_timestamp)) < (now + QUEUE_AHEAD_US)))) {
            Packet packet = pending_trailer.removeFirst();
            
            if (packet.streamIdentifier == Groupir2g.StreamIdentifier.ETCH_EVENT_BUNDLE) {
              // populate event list
              {
                Buffer.xI tmp = central.acquireI();
                
                NaturalNumberCodec.decode(central, tmp, packet.payload);
                
                Buffer.oI tmp_o = tmp.iterate();
                
                decoder.setInputSource(tmp_o, packet.ustc);
                
                Etch.OrganicInputEvent.DecodeCreator.TimecodeAdjuster adjuster =
                  (new Etch.OrganicInputEvent.DecodeCreator.TimecodeAdjuster()
                    {
                      public long invoke(long ustc)
                      {
                        return (synchronizer.tsconv((now + (2 * QUEUE_AHEAD_US)), ustc, true) + ETCH_EVENT_DELAY_US);
                      }
                    });
                
                for (int i = 0; i < packet.versatile; i++) {
                  Etch.OrganicInputEvent event = decoder.decode(adjuster);
                  //Log.log("etch.decode: now=" + MicroTime.now() + ", event.ustc=" + event.ustc);
                  events.add(event);
                }
                
                tmp_o.release();
                
                tmp.release();
              }
            } else if (packet.streamIdentifier == Groupir2g.StreamIdentifier.NWED_EVENT) {
              if (packet.versatile != -1) {
                nwed_events.add(packet.versatile);
              } else {
                for (byte encoding : packet.payload.toNewArrayB()) {
                  boolean magic = ((encoding & 0x80) != 0);
                  nwed_events.add(((magic ? (1 << 31) : 0) | (((int)(encoding)) & 0x7F)));
                }
              }
            } else if (packet.streamIdentifier == Groupir2g.StreamIdentifier.FROB_CODE) {
              handleFrobCode(packet.versatile);
            }
            
            if ((!support_rewind) && (packet.payload != null)) {
              packet.payload.release();
            }
          }
          
          // submit etch event list
          if (events.size() > 0) {
            //Log.log("submitting " + events.size() + " etch event(s)");
            agent_user_interface.doSubmitEtchEvents(events);
          }
        }
        
        // submit nwed event list
        agent_user_interface.doSubmitNwedEvents(nwed_events);
      }
    }
    
    final Groupir2g.SteamRoller steam_roller = (new Groupir2g.SteamRoller());
    
    void processTail()
    {
      {
        buffer_tail = Groupir2g.decode(central, checksum_assistant, buffer_tail, pending_unclassified, steam_roller);
        
        if (!support_rewind) {
          steam_roller.release();
        }
      }
      
      while (!(pending_unclassified.isEmpty())) {
        Packet packet = (new Packet());
        
        // populate packet fields from groupir packet
        {
          Groupir2g.Packet groupir_packet = pending_unclassified.removeFirst();
          
          Buffer.oI oI = groupir_packet.dI.iterate();
          
          {
            int source_tc_hi = oI.rI();
            int source_tc_lo = oI.rI();
            packet.ustc = ((((long)(source_tc_hi)) << 32) | (((long)(source_tc_lo)) & 0x00000000FFFFFFFFL));
          }
          
          {
            int stream_id = oI.rI();
            
            switch (stream_id) {
            case 0: packet.streamIdentifier = Groupir2g.StreamIdentifier.COMMAND;               break;
            case 1: packet.streamIdentifier = Groupir2g.StreamIdentifier.AUDIO;                 break;
            case 6: packet.streamIdentifier = Groupir2g.StreamIdentifier.NWED_EVENT;            break;
            case 7: packet.streamIdentifier = Groupir2g.StreamIdentifier.ETCH_JARFILE_FRAGMENT; break;
            case 8: packet.streamIdentifier = Groupir2g.StreamIdentifier.ETCH_EVENT_BUNDLE;     break;
            case 9: packet.streamIdentifier = Groupir2g.StreamIdentifier.FROB_CODE;             break;
            default: /* ignore the packet (below) */
            }
            
            if (packet.streamIdentifier == null) {
              Log.log("ignoring packet with strange stream identifier " + stream_id);
              oI.release();
              continue; // ignore the packet (WARNING: THIS IS PROBABLY A MEMORY LEAK)
            }
          }
          
          if (oI.remaining() > 0) {
            packet.versatile = oI.rI();
          }
          
          oI.release();
          
          // now, discard dS/dI/dJ forever ...
          if (groupir_packet.dS != null) { groupir_packet.dS.release(); groupir_packet.dS = null; }
          if (groupir_packet.dI != null) { groupir_packet.dI.release(); groupir_packet.dI = null; }
          if (groupir_packet.dJ != null) { groupir_packet.dJ.release(); groupir_packet.dJ = null; }
          
          // and link dB into the packet structure
          packet.payload = groupir_packet.dB;
        }
        
        if (false) {
          Log.log("ustc=" + packet.ustc + ", streamIdentifier=" + packet.streamIdentifier + ", versatile=" + packet.versatile + ", payload.length()=" + ((packet.payload == null) ? "(null)" : packet.payload.length()));
        }
        
        // possibly place the packet in the history stream
        {
          if (support_rewind) {
            if (packet.streamIdentifier.getDisposition().isRepeatable()) {
              history.addLast(packet);
            }
          }
        }
        
        stagePending(packet);
      }
      
      processInstant();
    }
    
    void actuallyRewind()
    {
      rewind_cookie = (new Object());
      
      while (!(pending_primary.isEmpty())) pending_primary.removeFirst();
      while (!(pending_trailer.isEmpty())) pending_trailer.removeFirst();
      
      for (int i = 0, l = history.size(); i < l; i++) {
        stagePending(history.get(i));
      }
      
      ArrayList<Etch.InputEvent> events = (new ArrayList<Etch.InputEvent>());
      events.add((new Etch.SoftResetInputEvent()));
      agent_user_interface.doSubmitEtchEvents(events);
    }
    
    void backgroundProcessingEverySecond()
    {
      // TODO: what's this for? I'm sure it was something important, but a comment should explain this kind of bizarre code
      agent_user_interface.doSubmitEtchEventBundle(null);
      
      // watchdog maintenance
      {
        if (network_descriptor.isRealtime()) {
          long latency = (System.nanoTime() - watchdog_last_triggered);
          
          if (latency < WATCHDOG_TIMEOUT_NS) {
            if (watchdog_alarm_active) {
              agent_user_interface.doReplaceWatchdogStatus("connection OK");
              watchdog_alarm_active = false;
            }
          } else {
            latency /= ONE_SECOND_NS;
            agent_user_interface.doReplaceWatchdogStatus(("NOTHING RECEIVED FOR " + latency + "s (but still trying ...)"));
            watchdog_alarm_active = true;
          }
        }
      }
    }
    
    long background_processing_last_second = System.nanoTime();
    
    void backgroundProcessing()
    {
      long now = System.nanoTime();
      
      if (now > (background_processing_last_second + 1000000000)) {
        backgroundProcessingEverySecond();
        background_processing_last_second = now;
      }
    }
    
    Instance(HashMap<String, String> settings)
    {
      this.settings = settings;
      
      student_name = defaulting(settings.get("STUDENT_NAME"), "null");
      pseudo_auth  = defaulting(settings.get("PSEUDO_AUTH"),  "null");
      
      support_rewind = enabled(settings.get("SUPPORT_REWIND"));
      enforce_rewind = enabled(settings.get("ENFORCE_REWIND"));
      
      {
        String origin = settings.get("ORIGIN");
        
        /****/ if (origin.equals("splitter")) {
          network_descriptor =
            (new NetworkDescriptor()
              {
                boolean isRealtime()
                {
                  return true;
                }
                
                String[] getOriginDescriptor()
                {
                  return (new String[] { "E", "TCP4", Instance.this.settings.get("SPLITTER_HOST"), Instance.this.settings.get("SPLITTER_PORT") });
                }
              });
        } else if (origin.equals("cooked")) {
          network_descriptor =
            (new NetworkDescriptor()
              {
                boolean isRealtime()
                {
                  return false;
                }
                
                String[] getOriginDescriptor()
                {
                  return (new String[] { "E", "HTTP", Instance.this.settings.get("COOKED_HOST"), Instance.this.settings.get("COOKED_PATH") });
                }
              });
        } else if (origin.equals("pseudo")) {
          network_descriptor =
            (new NetworkDescriptor()
              {
                boolean isRealtime()
                {
                  return true;
                }
                
                String[] getOriginDescriptor()
                {
                  return (new String[] { "P", "PSDO", Instance.this.settings.get("PSEUDO_HOST"), Instance.this.settings.get("PSEUDO_PATH"), Instance.this.settings.get("PSEUDO_NAME"), pseudo_auth });
                }
              });
        } else {
          throw null;
        }
      }
      
      {
        String mode = settings.get("ASSISTANT");
        
        /****/ if (mode.equals("NONE")) {
          checksum_assistant = (new SplitterCommon.Groupir.ChecksumAssistant.NONE());
        } else if (mode.equals("KYES")) {
          checksum_assistant = (new SplitterCommon.Groupir.ChecksumAssistant.KYES());
        } else if (mode.equals("SHA2")) {
          checksum_assistant = (new SplitterCommon.Groupir.ChecksumAssistant.SHA2());
        } else if (mode.equals("KMAC")) {
          checksum_assistant = (new SplitterCommon.Groupir.ChecksumAssistant.KMAC(HexStr.hex2bin(settings.get("ASSISTANT_KEY"))));
        } else {
          throw null;
        }
      }
      
      {
        final JNylus.Station station_audio_replay     = manager.newStation();
        final JNylus.Station station_mixer_enumerator = manager.newStation();
        final JNylus.Station station_user_interface   = manager.newStation();
        final JNylus.Station station_network_endpoint = manager.newStation();
        final JNylus.Station station_network_incoming = manager.newStation();
        final JNylus.Station station_network_outgoing = manager.newStation();
        
        final JNylus.Channel station_entry_channel_audio_replay     = station_entry.newChannel();
        final JNylus.Channel station_entry_channel_mixer_enumerator = station_entry.newChannel();
        final JNylus.Channel station_entry_channel_user_interface   = station_entry.newChannel();
        final JNylus.Channel station_entry_channel_network_endpoint = station_entry.newChannel();
        final JNylus.Channel station_entry_channel_network_incoming = station_entry.newChannel();
        final JNylus.Channel station_entry_channel_network_outgoing = station_entry.newChannel();
        
        final JNylus.Channel station_audio_replay_channel_entry     = station_audio_replay.newChannel();
        final JNylus.Channel station_mixer_enumerator_channel_entry = station_mixer_enumerator.newChannel();
        final JNylus.Channel station_user_interface_channel_entry   = station_user_interface.newChannel();
        final JNylus.Channel station_network_endpoint_channel_entry = station_network_endpoint.newChannel();
        final JNylus.Channel station_network_incoming_channel_entry = station_network_incoming.newChannel();
        final JNylus.Channel station_network_outgoing_channel_entry = station_network_outgoing.newChannel();
        
        final JNylus.Linkage linkage_entry_audio            = (new JNylus.Linkage(station_entry, station_audio_replay,     station_entry_channel_audio_replay,     station_audio_replay_channel_entry));
        final JNylus.Linkage linkage_entry_mixer_enumerator = (new JNylus.Linkage(station_entry, station_mixer_enumerator, station_entry_channel_mixer_enumerator, station_mixer_enumerator_channel_entry));
        final JNylus.Linkage linkage_entry_user_interface   = (new JNylus.Linkage(station_entry, station_user_interface,   station_entry_channel_user_interface,   station_user_interface_channel_entry));
        final JNylus.Linkage linkage_entry_network_endpoint = (new JNylus.Linkage(station_entry, station_network_endpoint, station_entry_channel_network_endpoint, station_network_endpoint_channel_entry));
        final JNylus.Linkage linkage_entry_network_incoming = (new JNylus.Linkage(station_entry, station_network_incoming, station_entry_channel_network_incoming, station_network_incoming_channel_entry));
        final JNylus.Linkage linkage_entry_network_outgoing = (new JNylus.Linkage(station_entry, station_network_outgoing, station_entry_channel_network_outgoing, station_network_outgoing_channel_entry));
        
        final AudioReplayYield yield_audio_replay =
          (new AudioReplayYield(linkage_entry_audio.reverse())
            {
              protected void onInitialize(Audio2g.MixerTarget mixer_target, Audio2g.MixerFormat mixer_format)
              {
              }
              
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onPutPacket(byte[] buffer, long expected_duration_us, long packet_timestamp_us, Object cookie, boolean success, long replay_timestamp_us)
              {
                byteArrayCache.refund(buffer);
                dequeuePrimaryPacket();
                dequeueTrailerPackets(replay_timestamp_us, packet_timestamp_us, cookie);
              }
            });
        
        final AudioMixerEnumeratorYield yield_mixer_enumerator =
          (new AudioMixerEnumeratorYield(linkage_entry_mixer_enumerator.reverse())
            {
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onProbeMixers(Class<? extends DataLine> line_class, Audio2g.AudioMixerEnumerator.ProbeMethod probe_method, int sampleRateHertz, cL<Audio2g.MixerTarget> mixers)
              {
                detected_mixers = mixers;
                
                ArrayList<String> mixerChoices = (new ArrayList<String>());
                
                for (int pos = mixers.off(); pos < mixers.lim(); pos++) {
                  mixerChoices.add(mixers.get(pos).getLabel());
                }
                
                agent_user_interface.doReplaceMixerChoices(mixerChoices);
              }
            });
        
        final UserInterfaceYield yield_user_interface =
          (new UserInterfaceYield(linkage_entry_user_interface.reverse())
            {
              protected void onInitialize(HashMap<String, String> settings, boolean realtime, F2<Nothing, Integer, Integer> onVolumeLevel)
              {
              }
              
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onReplaceWatchdogStatus(String status)
              {
              }
              
              protected void onReplaceMixerChoices(ArrayList<String> choices)
              {
              }
              
              protected void onResetFormatChoices()
              {
              }
              
              protected void onRaiseEtch()
              {
              }
              
              protected void onRaiseNwed()
              {
              }
              
              protected void onSubmitEtchEvents(ArrayList<Etch.InputEvent> events)
              {
              }
              
              protected void onSubmitEtchEventBundle(Etch.EventBundleInputEvent event)
              {
                for (Buffer.oB payload_o : event.payloads) {
                  payload_o.release();
                }
                
                event.payloads.clear();
              }
              
              protected void onSubmitNwedEvents(ArrayList<Integer> events)
              {
              }
              
              protected void onMixerSelect(int index)
              {
                index -= 1;
                
                if ((0 <= index) && (index < detected_mixers.len())) {
                  selected_mixer_target = detected_mixers.get(detected_mixers.off() + index);
                } else {
                  selected_mixer_target = Audio2g.AudioMixerEnumerator.getSystemDefaultMixerTarget(SourceDataLine.class);
                }
                
                selected_mixer_format = selected_mixer_target.getDefaultFormat();
                
                agent_user_interface.doResetFormatChoices();
                
                agent_audio_replay.doInitialize(selected_mixer_target, selected_mixer_format);
                
                if (enforce_rewind) {
                  actuallyRewind();
                }
              }
              
              protected void onFormatSelect(int index)
              {
                cL<Audio2g.MixerFormat> formats = Audio2g.MixerFormat.sensible_writer;
                
                if ((0 <= index) && (index < formats.len())) {
                  selected_mixer_format = formats.get(formats.off() + index);
                  
                  agent_audio_replay.doInitialize(selected_mixer_target, selected_mixer_format);
                }
              }
              
              protected void onVolumeLevel(int val, int max)
              {
                Instance.this.onVolumeLevel(val, max);
              }
              
              protected void onStaticCheckbox(boolean checked)
              {
                Log.log("onStaticCheckbox(checked=" + checked + ")");
              }
              
              protected void onSilentCheckbox(boolean checked)
              {
                Log.log("onSilentCheckbox(checked=" + checked + ")");
              }
              
              protected void onRewind()
              {
                if (support_rewind) {
                  actuallyRewind();
                }
              }
              
              protected void onSetSkipSilence(boolean inp_skip_silence)
              {
                skip_silence = inp_skip_silence;
              }
              
              int feedback_counter = 0;
              
              protected void onFeedback(String line)
              {
                Log.log("onFeedback(line='" + line + "')");
                
                if (line.equals("$$$log")) {
                  enableDebugWindow();
                }
                
                if (line.startsWith("$$$fwd")) {
                  try {
                    int amt = Integer.parseInt(line.substring(("$$$fwd".length())));
                    
                    while (amt-- > 0) {
                      if (!(pending_primary.isEmpty())) {
                        pending_primary.removeFirst();
                      }
                    }
                  } catch (NumberFormatException e) {
                    Log.log(e);
                  }
                }
                
                if (agent_network_outgoing != null) {
                  String message = ("msg: " + student_name + ": " + (feedback_counter += 1) + ": " + line);
                  byte[] encoding = NoUTF.str2bin(message);
                  agent_network_outgoing.doSend(encoding, 0, encoding.length);
                  Log.log(message);
                }
              }
              
              protected void onFrameClose()
              {
                shutdown = true;
              }
            });
        
        final NetworkEndpointYield yield_network_endpoint =
          (new NetworkEndpointYield(linkage_entry_network_endpoint.reverse())
            {
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onInitialize(String[] origin, JNylus.Linkage linkage_entry_incoming, JNylus.Linkage linkage_entry_outgoing, NetworkIncomingYield yield_incoming, NetworkOutgoingYield yield_outgoing, boolean success, NetworkIncomingAgent agent_network_incoming_supplied, NetworkOutgoingAgent agent_network_outgoing_supplied)
              {
                agent_network_incoming = agent_network_incoming_supplied;
                agent_network_outgoing = agent_network_outgoing_supplied;
                
                for (int i = 0; i < network_descriptor.getNetworkBufferCount(); i++) {
                  byte[] buf = (new byte[NETWORK_BUFFER_BYTES]);
                  agent_network_incoming.doRecv(buf, 0, buf.length);
                }
                
                {
                  String message = ("msg: " + student_name + ": (joined)");
                  byte[] encoding = NoUTF.str2bin(message);
                  agent_network_outgoing.doSend(encoding, 0, encoding.length);
                  Log.log(message);
                }
              }
              
              protected void onClose()
              {
              }
            });
        
        final NetworkIncomingYield yield_network_incoming =
          (new NetworkIncomingYield(linkage_entry_network_incoming.reverse())
            {
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onShutdownIncoming()
              {
              }
              
              protected void onRecv(byte[] buf, int off, int lim, int amt)
              {
                if (amt != 0) {
                  watchdog_last_triggered = System.nanoTime();
                  
                  // append obtained bytes to tail buffer
                  {
                    Buffer.nB buffer_tail_n = buffer_tail.append();
                    Buffer.sB.copy(buffer_tail_n, buf, off, (off + amt));
                    buffer_tail_n.release();
                  }
                  
                  processTail();
                  
                  agent_network_incoming.doRecv(buf, 0, buf.length);
                } else {
                  Log.log("onRecv(amt=" + amt + ")");
                }
              }
            });
        
        final NetworkOutgoingYield yield_network_outgoing =
          (new NetworkOutgoingYield(linkage_entry_network_outgoing.reverse())
            {
              protected void onShutdown()
              {
                outstanding_spinoff--;
              }
              
              protected void onShutdownOutgoing()
              {
              }
              
              protected void onSend(byte[] buf, int off, int lim, int amt)
              {
                /* amt<(lim-off) implies broken pipe */
              }
            });
        
        agent_audio_replay     = Audio2g.AudioReplay          .launch(linkage_entry_audio,            yield_audio_replay);
        agent_mixer_enumerator = Audio2g.AudioMixerEnumerator .launch(linkage_entry_mixer_enumerator, yield_mixer_enumerator);
        agent_user_interface   = UserInterface                .launch(linkage_entry_user_interface,   yield_user_interface);
        
        agent_network_endpoint = NetworkConnection.launch(linkage_entry_network_endpoint, yield_network_endpoint);
        
        JNylus.spinOff(station_audio_replay);     outstanding_spinoff++;
        JNylus.spinOff(station_mixer_enumerator); outstanding_spinoff++;
        JNylus.spinOff(station_network_endpoint); outstanding_spinoff++;
        JNylus.spinOff(station_network_incoming); outstanding_spinoff++;
        JNylus.spinOff(station_network_outgoing); outstanding_spinoff++;
        
        javax.swing.SwingUtilities.invokeLater
          ((new Runnable()
            {
              public void run()
              {
                try {
                  station_user_interface.registerOwnership();
                  
                  final javax.swing.Timer timer = (new javax.swing.Timer(STATION_USER_INTERFACE_LATENCY_MS, null));
                  final ResourceTracker.Token token_timer = ResourceTracker.acquire("Student2g::Instance::SwingUtilities::Runnable(timer=" + timer + ")");
                  
                  timer.addActionListener
                    (new ActionListener()
                      {
                        public void actionPerformed(ActionEvent e)
                        {
                          if (station_user_interface.getTerminated()) {
                            timer.removeActionListener(this);
                            timer.stop();
                            ResourceTracker.release(token_timer);
                          } else {
                            station_user_interface.once();
                          }
                        }
                      });
                  
                  timer.start();
                } catch (Throwable e) {
                  Log.log(e);
                }
              }
            })); outstanding_spinoff++;
        
        station_entry.registerOwnership();
        
        agent_audio_replay.doInitialize(Audio2g.AudioMixerEnumerator.getSystemDefaultMixerTarget(SourceDataLine.class), Audio2g.MixerFormat.sensible_writer.get(0));
        
        for (int i = 0; i < network_descriptor.getAudioBufferCount(); i++) {
          dequeuePrimaryPacket();
        }
        
        agent_mixer_enumerator.doProbeMixers(SourceDataLine.class, Audio2g.AudioMixerEnumerator.ProbeMethod.GuessYesProbeMethod, Audio2g.SAMPLE_RATE_REPLAY_HZ);
        agent_user_interface.doInitialize(settings, network_descriptor.isRealtime(), (new F2<Nothing, Integer, Integer>() { public Nothing invoke(Integer val, Integer max) { Instance.this.onVolumeLevel(val, max); return null; } }));
        agent_network_endpoint.doInitialize(network_descriptor.getOriginDescriptor(), linkage_entry_network_incoming, linkage_entry_network_outgoing, yield_network_incoming, yield_network_outgoing);
      }
    }
    
    void init()
    {
      // already been
    }
    
    void fini()
    {
      agent_audio_replay.doInitialize(null, null);
      agent_audio_replay.doShutdown();
      
      agent_mixer_enumerator.doShutdown();
      
      agent_user_interface.doShutdown();
      
      if (agent_network_incoming != null) { agent_network_incoming.doShutdownIncoming(); agent_network_incoming.doShutdown(); } else { outstanding_spinoff--; }
      if (agent_network_outgoing != null) { agent_network_outgoing.doShutdownOutgoing(); agent_network_outgoing.doShutdown(); } else { outstanding_spinoff--; }
      
      if (agent_network_endpoint != null) { agent_network_endpoint.doShutdown(); } else { outstanding_spinoff--; }
    }
    
    long once_last_depiction = System.nanoTime();
    
    void once()
    {
      station_entry.once();
      backgroundProcessing();
      station_entry.hang();
      
      if (false) {
        long now = System.nanoTime();
        
        if ((now - once_last_depiction) > 5000000000L) { /* every 5 seconds */
          Log.log(central.depiction());
          once_last_depiction = now;
        }
      }
    }
    
    void loop()
    {
      while (!shutdown) {
        once();
      }
      
      fini();
      
      Log.log("waiting for outstanding spinoff");
      
      while (outstanding_spinoff > 0) {
        once();
      }
      
      Log.log("outstanding_spinoff == 0");
      
      {
        for (Object queue_shadow : (support_rewind ? (new Object[] { history, pending_instant }) : (new Object[] { pending_instant, pending_primary, pending_trailer }))) {
          SimpleDeque<Packet> queue = cast_unchecked(((SimpleDeque<Packet>)(null)), queue_shadow);
          
          for (int i = 0, l = queue.size(); i < l; i++) {
            Packet packet = queue.removeFirst();
            
            if (packet.payload != null) {
              packet.payload.release();
            }
          }
        }
      }
      
      buffer_tail.release();
      buffer_tail = null;
      
      if (buffer_etch_jarfile_n != null) {
        buffer_etch_jarfile_n.release();
        buffer_etch_jarfile_n = null;
      }
      
      buffer_etch_jarfile.release();
      buffer_etch_jarfile = null;
      
      steam_roller.release();
      
      Log.log(central.depiction());
      
      central.release();
    }
    
    void run()
    {
      init();
      loop();
      
      shutdown_completed = true;
    }
  }
  
  static ApplicationLauncher.ApplicationClient createApplicationLauncherClient(final HashMap<String, String> settings)
  {
    return
      (new ApplicationLauncher.ApplicationClient()
        {
          volatile Instance instance;
          boolean shutdown_acknowledged;
          
          protected void start()
          {
            System.gc();
            
            (new Thread()
              {
                public void run()
                {
                  try {
                    instance = (new Instance(settings));
                    instance.run();
                  } catch (Throwable e) {
                    Log.log(e);
                  }
                }
              }).start();
            
            shutdown_acknowledged = false;
          }
          
          protected boolean started()
          {
            return (instance != null);
          }
          
          protected void stop()
          {
            instance.shutdown = true;
          }
          
          protected boolean stopped()
          {
            if (!shutdown_acknowledged) {
              if (instance.shutdown_completed) {
                instance = null;
                shutdown_acknowledged = true;
              }
            }
            
            return shutdown_acknowledged;
          }
        });
  }
  
  ApplicationLauncher launcher;
  
  public void start()
  {
    if (enabled(getParameter("SETTING_USE_128_BIT_VECTORS"))) {
      enableDebugWindow();
    }
    
    if (launcher == null) {
      final HashMap<String, String> settings = (new HashMap<String, String>());
      
      // probe settings (applet version)
      {
        for (String key : getParameter("SETTINGS").split("\\s")) {
          String val = getParameter(("SETTING_" + key));
          settings.put(key, val);
          Log.log("settings.put(key='" + key + "', val='" + val + "')");
        }
      }
      
      launcher = (new ApplicationLauncher(createApplicationLauncherClient(settings), this));
    }
    
    launcher.start();
  }
  
  public void stop()
  {
    launcher.stop();
  }
  
  public void destroy()
  {
    launcher.stop();
  }
  
  public static void main(String[] args)
  {
    final HashMap<String, String> settings = (new HashMap<String, String>());
    
    // probe settings (command-line version)
    {
      for (int i = 0; i < args.length; i += 2) {
        String key = args[i + 0];
        String val = args[i + 1];
        
        settings.put(key, val);
      }
    }
    
    if (enabled(settings.get("USE_128_BIT_VECTORS"))) {
      Log.loopHandleEventsBackground(System.err, true);
    }
    
    final ApplicationLauncher launcher = (new ApplicationLauncher(createApplicationLauncherClient(settings), null));
    launcher.start();
  }
}

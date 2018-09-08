/***
 * CommandLineInterface.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.teacher;

import java.net.*;

import java.util.*;

import java.util.regex.*;

import java.util.concurrent.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

import static gotm.onlf.learner.common.AudioCommon.*;

import gotm.onlf.learner.common.*;

// use the sound reader component to build a command line interface for it,
// connect (tcp) to a splitter and send packets to it.
// make the callback from the soundreader enqueue (sync-queue) packets
// make a thread that dequeues packets and sends them
// cli: regexp identifying the mixer; optional format (+'default"); host-port for the server (HOST_ROOT, PORT_ROOT; root key file (PASS_ROOT); batch size
// need key to authenticate every packet; just need to pass the passwd to groupir
// GroupirPacket.send(..., pass_root, ....) returning the byte[] that must be send to splitter

public class CommandLineInterface
{
  static final LinkedBlockingQueue<GroupirPacket> queue = (new LinkedBlockingQueue<GroupirPacket>());
  
  /*
  static double lpf_rate = 0.05;
  static double lpf_mean = 0;
  
  static double dither()
  {
    return (8 * Math.random());
  }
  
  static void low_pass_filter(short[] arr)
  {
    for (int i = 0; i < arr.length; i++) {
      lpf_mean += lpf_rate * (((double)(arr[i])) - lpf_mean + dither());
      int lpf_int = ((int)(Math.round(lpf_mean)));
      arr[i] = ((short)(lpf_int));
      if (lpf_int >= Short.MAX_VALUE) { System.err.println("!"); arr[i] = Short.MAX_VALUE-1; }
      if (lpf_int <= Short.MIN_VALUE) { System.err.println("!"); arr[i] = Short.MIN_VALUE+1; }
    }
  }
  */
  
  static final Random random = (new Random());
  
  static void amplify_filter_2(short[] arr)
  {
    int rem = 0;
    int raw = 0;
    
    for (int i = 0; i < arr.length; i++) {
      if (rem == 0) {
        rem = 32;
        raw = random.nextInt();
      }
      
      arr[i] = ((short)((((int)(arr[i])) << 1) | (raw & 1)));
      
      rem--;
      raw >>= 1;
    }
  }
  
  static void amplify_filter_15(short[] arr)
  {
    int rem = 0;
    int raw = 0;
    
    for (int i = 0; i < arr.length; i++) {
      if (rem == 0) {
        rem = 32;
        raw = random.nextInt();
      }
      
      arr[i] = ((short)(arr[i] + (arr[i] >> 1)));
      
      rem--;
      raw >>= 1;
    }
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
  
  public static void main(String[] args)
  {
    final int ARGI_MIXER      = 0;
    final int ARGI_FORMAT     = 1;
    final int ARGI_HOST_ROOT  = 2;
    final int ARGI_PORT_ROOT  = 3;
    final int ARGI_STREAM_ID  = 4;
    final int ARGI_PASS_ROOT  = 5;
    final int ARGI_BATCH_SIZE = 6;
    
    final boolean duppkt = (System.getenv("DUPPKT") != null);
    
    log("duppkt " + (duppkt ? "enabled" : "disabled"));
    
    try {
      log("connecting to splitter");
      
      final String mixer_regex = args[ARGI_MIXER];
      final byte[] pass_root = read_file_bytes(args[ARGI_PASS_ROOT], new byte[SHAZ]);
      final int stream_id = Integer.parseInt(args[ARGI_STREAM_ID]);
      final int batchz = Integer.parseInt(args[ARGI_BATCH_SIZE]);
      
      log("host: " + args[ARGI_HOST_ROOT] + " port: " + Integer.parseInt(args[ARGI_PORT_ROOT]));
      
      Socket socket = NetworkingUtilities.connect(args[ARGI_HOST_ROOT], Integer.parseInt(args[ARGI_PORT_ROOT]));
      final OutgoingNetworkStream out = new OutgoingNetworkStream(socket.getOutputStream(), TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1);

      log("connected");

      Format format = null;

      if (!args[ARGI_FORMAT].equals("default")) {
        for (Format f : Format.values()) {
          if (f.getLabel().equals(args[ARGI_FORMAT])) {
            format = f;
            break;
          }
        }
        if (format == null) {
          log("invalid format: " + args[ARGI_FORMAT]);
          fatal(new RuntimeException());
        }
      }

      log("launching the audio writer in a new thread");
        
      AudioReader.Control control = AudioReader.launch
        (new F1<Target, Target[]>()
         {
           public Target invoke(Target[] targets)
           {
             {
               log("enter all target print");
               
               for (Target target : targets) {
                 log("target: '" + target.getLabel() + "'");
               }
               
               log("leave all target print");
             }
             
             Pattern pattern = Pattern.compile(mixer_regex);
             // DBG
             //log("matching targets to regex: " + mixer_regex);
             //
             for (Target target : targets) {
               Matcher matcher = pattern.matcher(target.getLabel());
               // DBG
               log("trying to match " + target.getLabel());
               //
              if (matcher.matches()) {
                // DBG
                log("found " + target.getLabel() + " , returning");
                //
                return target;
               }
             }
             return null;
           }
         },
         format,
         batchz,
         new AudioReader.Callback()
         {
           public void gotBatch(Format format, byte[] buf, int off, int lim)
           {
             try {
               final long now = microTime();
               
               int metadata[] = (new int[PACKET_METADATA_LENGTH]);
               encodeProperSourceTimecode(metadata, now);
               metadata[PACKET_METADATA_STREAM_ID] = stream_id;
               
               int dataz = lim - off;
               byte[] data = new byte[dataz];
               for (int i = 0; i < dataz; i++)
                 data[i] = buf[i + off];
               short[] signed_pcm = format.adaptArbitraryPCM(data);
               //low_pass_filter(signed_pcm);
               //amplify_filter_15(signed_pcm);
               log("=========================> AVG " + avg(signed_pcm) + ", MAX " + max(signed_pcm));
               byte[] enlawed = enlaw(signed_pcm, 0, signed_pcm.length);

               GroupirPacket groupir = (new GroupirPacket(emptyL, metadata, emptyS, enlawed));

               queue.put(groupir);
               
               /*
               if (duppkt) { // duplicate packets (for debugging)
                 int other_metadata[] = (new int[PACKET_METADATA_LENGTH]);
                 System.arraycopy(metadata, 0, other_metadata, 0, metadata.length);
                 other_metadata[PACKET_METADATA_SOURCE_TC] = ((int)(now) + 128000);
                 groupir = (new GroupirPacket(emptyL, other_metadata, emptyS, enlawed));
                 queue.put(groupir);
               }
               */
             } catch (InterruptedException e) {
               log("" + e);
               throw new RuntimeException(e);
             }
           }
         });

      log("launched");

      log("starting a new thread for sending packets to the splitter");

      (new Thread()
          {
            private int spe(byte[] arr)
            {
              {
                int spe = 0;
                
                for (int i = 0; i < arr.length; i++) {
                  spe = Math.max(Math.abs(arr[i]), spe);
                }
                
                return spe;
              }
            }
            
            public void run()
            {
              try {
                long previous_source_tc = 0;
                
                while (true) {
                  // DBG
                  //if ((x++ & 0xFF) == 0) System.err.println("CLI queue size: " + queue.size());
                  //
                  
                  GroupirPacket groupir = queue.take();
                  // DBG
                  //log("CLI: took packet from queue, sending " + queue.size());
                  //
                  
                  final long source_tc = decodeProperSourceTimecode(groupir);
                  log("source_tc=" + source_tc + " (delta=" + (source_tc - previous_source_tc) + ", samples=" + groupir.dB.length + ") (spe " + spe(groupir.dB) + ")");
                  previous_source_tc = source_tc;
                  
                  byte[] encoded = groupir.send(pass_root);
                  out.wB(encoded);
                  out.writeback();
                }
              } catch (Exception e) {
                log("sending thread failure", e);
              }
            }
          }).start();

      log("sending thread started");

    } catch (Exception e) {
      fatal(e);
    }
  }
}

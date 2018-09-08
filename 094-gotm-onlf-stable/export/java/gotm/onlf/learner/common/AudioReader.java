/***
 * AudioReader.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.common;

import javax.sound.sampled.*;

import static gotm.onlf.utilities.Utilities.*;

import zs42.parts.*;

public class AudioReader extends AudioCommon
{
  /***
   * SPECIFICATION
   ***/
  
  /***
   * methods of the following interface may be invoked from the
   * context of any thread. they do not block.
   ***/
  public static abstract class Control
  {
    /***
     * signals the audio reader thread to stop.
     ***/
    abstract void putFinish();
  }
  
  /***
   * methods of the following interface will be invoked in the context
   * of the specialized audio reader thread. the caller is not
   * responsible for performing any synchronization.
   ***/
  public interface Callback
  {
    /***
     * called to report a new batch of samples. depending on the
     * desired format, there may be one or two or four bytes of data
     * per sample. the callee must <b>quickly</b> copy the data; the
     * passed buffer remains owned by the caller and may be reused.
     ***/
    public void gotBatch(Format format, byte[] buf, int off, int lim);
  }
  
  /***
   * launches a new thread to read a sound source selected by the
   * given selector in the given format (or null for default) in
   * blocks of size batchz (in samples). returns a control object.
   ***/
  public static Control launch(F1<Target, Target[]> select, Format format, int batchz, Callback callback) { return launch_inner(select, format, batchz, callback); }
  
  /***
   * IMPLEMENTATION
   ***/
  
  static Format[] get_formats(Format format)
  {
    Format[] formats;
    
    if (format != null) {
      formats = new Format[1];
      formats[0] = format;
    } else {
      formats = Format.sensible_reader;
    }
    
    return formats;
  }

  static Control launch_inner(F1<Target, Target[]> select, Format chosen_format, final int batchz, final Callback callback)
  {
    final MixerTarget target = (MixerTarget)select.invoke(detect_mixers(TargetDataLine.class));

    // determine the format and open the audio line

    Format tbd_format = null;
    TargetDataLine tbd_line = null;

    if (target == null) {
      // no mixer found. trying the sensible formats directly on AudioSystem
      for (Format f : Format.sensible_reader) {
        DataLine.Info dli = new DataLine.Info(TargetDataLine.class, f.getAudioFormat());
        try {
          tbd_line = (TargetDataLine)AudioSystem.getLine(dli);
          tbd_format = f;
          break;
        } catch (LineUnavailableException e) {
          continue;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      tbd_format = chosen_format != null ? chosen_format : target.formats[0];
      try {
        DataLine.Info dli = new DataLine.Info(TargetDataLine.class, tbd_format.getAudioFormat());
        tbd_line = (TargetDataLine)AudioSystem.getMixer(target.mixer).getLine(dli);
        log("audio reader: line: " + target.mixer + " " + dli + " " + AudioSystem.getMixer(target.mixer).isLineSupported(dli));
      } catch (LineUnavailableException e) {
        log("" + e);
        throw new RuntimeException(e);
      }
    }

    final Format format = tbd_format;
    final TargetDataLine line = tbd_line;
    AudioFormat audio_format = format.getAudioFormat();
    final int bufz = audio_format.getChannels() * (audio_format.getSampleSizeInBits() >> 3) * batchz;
    final byte[] buf = new byte[bufz];

    try {
      line.open(audio_format, bufz);
    } catch (LineUnavailableException e) {
      log("" + e);
      // DBG
      if (target != null && target.mixer != null) {
        log("target.mixer info: " + target.mixer.toString());
      }
      if (format != null) {
        log("format label: " + format.getLabel());
      }
      if (line != null && line.getLineInfo() != null) {
        log("line info: " + line.getLineInfo().toString());
      }
      //
      
      throw new RuntimeException(e);
    }

    return (new Control()
      {
        volatile boolean running = true;
        
        public void loop()
        {
          line.start();
                    
          while (running) { 
            int amt;
            // DBG
            // long startTime = System.nanoTime();
            //
            if ((amt = line.read(buf, 0, bufz)) != bufz) {
              log("short read (" + amt + " / " + bufz + ")");
              System.exit(1);
            }
            // DBG
              // log("AudioReader: read " + batchz + " samples from target " + target.getLabel().substring(0, 16) + " in format " + format.getLabel() + " (" + bufz + " bytes) in " + (System.nanoTime() - startTime) / 1000000 + " ms");
            //
//              System.err.println("AudioReader: packet: off: " + packet.off + "lim: " + packet.lim);
//              for (int i = 0; i < packet.lim-packet.off; i += 256) {
//                System.err.print(" " + packet.arr[packet.off + i]);
//              }
//              System.err.println();

            callback.gotBatch(format, buf, 0, bufz);
          }
          line.stop();
        }
        
        void putFinish()
        {
          running = false;
        }
        
        { (new Thread() { public void run() { loop(); } }).start(); }
      });
  }
}

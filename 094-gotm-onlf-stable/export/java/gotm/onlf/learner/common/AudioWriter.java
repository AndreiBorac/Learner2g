/***
 * AudioWriter.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

/***
 * The AudioWriter interfaces with the Java Sound API. It allows the
 * creation of a thread that continuously queries data from an
 * abstract packet source and feeds the data into a mixer. The mixer
 * selection can be changed at any time.
 ***/

package gotm.onlf.learner.common;

import java.util.concurrent.atomic.*;
import javax.sound.sampled.*;

import zs42.parts.F0;
import zs42.parts.F1;

import static gotm.onlf.utilities.Utilities.*;

public class AudioWriter extends AudioCommon
{
  /***
   * SPECIFICATION
   ***/
  
  public static class Packet
  {
    // flush_thing is true iff 'thing' setting has changed since last audio packet
    // for the first packet, flush_target and flush_format are guaranteed to be "true"
    public boolean flush_target;
    public boolean flush_format;
    public boolean flush_volume;
    
    // settings
    // 
    // any setting that might change the packet format (target,
    // format) does not take effect until the next getPacket() when
    // the new format can be passed as an argument
    public Target target; // null means "default"
    public Format format; // null means "default"
    public double volume;
    
    // expected duration
    public long millis;
    
    // samples (payload)
    public byte[] arr;
    public int    off;
    public int    lim;
  }

  /***
   * methods of the following interface may be invoked from the
   * context of any thread. they do not block.
   ***/
  public static abstract class Control
  {
    public abstract Target[] getTargets();
    public abstract int      stop(F1<Void, Integer> dec);
  }
  
  /***
   * methods of the following interface will be invoked in the context
   * of the specialized audio feeder thread. the caller is not
   * responsible for performing any synchronization.
   ***/
  public interface Callback
  {
    /***
     * should <b>quickly</b> return more audio data, or
     * <code>null</code> to indicate that the audio source is
     * exhausted and the audio system should be shut down. passes the
     * format in effect.
     ***/
    Packet getPacket(Format format);
  }
  
  /***
   * launches a new thread to feed the given audio source to the audio
   * system until the audio source is exhausted. returns the
   * discovered sound targets.
   ***/
  public static Control launch(Callback callback) { return launch_inner(callback); }
  
  /***
   * IMPLEMENTATION
   ***/
  
  static class LineKeeper
  {
    static final int AUDIO_BUFFER_SAMPLE_COUNT = 2048;
    static final byte[] silence = (new byte[AUDIO_BUFFER_SAMPLE_COUNT]);
    
    final MixerTarget default_target; // the system default mixer, never null
    
    Format format = AudioCommon.Format.sensible_writer[0];
    
    SourceDataLine line = null;
    FloatControl volume_control = null;
    FloatControl master_gain_control = null;
    boolean never_written = true;
    boolean using_default_volume = true;
    
    LineKeeper(MixerTarget default_target)
    {
      this.default_target = default_target;
    }
    
    boolean open_internal(MixerTarget target, final Format format)
    {
      boolean success = false;
      
      this.format = format;
      
      line = target.getSourceDataLine(format);
      if (line == null) { close(); return false; }
      
      Boolean good = softfail
        (new SoftFail<Boolean>()
         {
           public Boolean invoke() throws LineUnavailableException
           {
             line.open(format.getAudioFormat(), AUDIO_BUFFER_SAMPLE_COUNT);
             line.start();
             return true;
           }
         });
      
      if (good == null) { close(); return false; }
      
      if (line.isControlSupported(FloatControl.Type.VOLUME)) {
        volume_control = ((FloatControl)(line.getControl(FloatControl.Type.VOLUME)));
      }
      
      if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
        master_gain_control = ((FloatControl)(line.getControl(FloatControl.Type.MASTER_GAIN)));
      }
      
      never_written = true;
      using_default_volume = true;
      
      return true;
    }
    
    boolean open(MixerTarget target, Format desired_format)
    {
      if (!((line == null) && (volume_control == null) && (master_gain_control == null))) {
        throw null; // insist on clean close
      }
      
      if (target == null) {
        target = default_target;
      }
      
      // if a format was specified, try it first
      {
        if (desired_format != null) {
          if (open_internal(target, desired_format)) {
            return true;
          }
        }
      }
      
      // either format was null, or it didn't work, so try all now ...
      {
        for (Format format : target.formats) {
          if (open_internal(target, format)) {
            return true;
          }
        }
        
        // baaaaaaad!
        return false;
      }
    }
    
    void close()
    {
      // shutting down a line poses a special problem ... stale data
      // can remain in the buffer during a format change for the same
      // sound card, which results in really loud static spanning the
      // whole dynamic range (as the previously pseudorandom low byte
      // becomes the high byte) ... this could also be a bug in this
      // app's code rather than the system (?)
      // 
      // sleeping either before or after the flush/stop/close does
      // nothing towards solving the problem
      // 
      // it seems to be a bug in this app's code, because the new
      // never-written mechanism suppresses it. so somehow sendData()
      // is fed one bad packet at the outset? HOW???
      // 
      // update: this has since been fixed. I forget how, but it was a
      // bug in the app code.
      
      if (line != null) {
        final SourceDataLine fptr_line = line;
        
        line.write(silence, 0, silence.length);
        
        softfail((new SoftFail<Void>() { public Void invoke() { fptr_line.flush(); return null; } }));
        softfail((new SoftFail<Void>() { public Void invoke() { fptr_line.stop();  return null; } }));
        softfail((new SoftFail<Void>() { public Void invoke() { fptr_line.close(); return null; } }));
        
        line = null;
      }
      
      volume_control = null;
      master_gain_control = null;
    }
    
    Format getCurrentFormat()
    {
      return format;
    }
    
    boolean usingDefaultVolume()
    {
      return using_default_volume;
    }
    
    void setVolume(double volume)
    {
      if (volume_control != null) {
        volume_control.setValue(((float)(volume)));
        using_default_volume = false;
      }
      
      if (master_gain_control != null) {
        final double LOG_LIN_TO_DB_MULT = (20.0 / Math.log(10.0));
        master_gain_control.setValue(((float)(Math.round(LOG_LIN_TO_DB_MULT * Math.log(volume)))));
        using_default_volume = false;
      }
    }
    
    long ms_prior = System.currentTimeMillis();
    
    void sendData(byte[] arr, int off, int lim, long millis)
    {
      if (never_written) {
        never_written = false;
        return; // don't write the packet
      }
      
      int len = (lim - off);
      
      if (line != null) {
        line.write(arr, off, len);
        long ms_leave = System.currentTimeMillis();
        long ms_lapse = ms_leave - ms_prior;
        ms_prior = ms_leave;
        
        log("ms_lapse=" + ms_lapse + " vs millis=" + millis);
        
        // if the output lapse is at least half the expected lapse, we
        // give it the benefit of doubt and clear millis to continue
        // writing at maximum speed ... otherwise, we sleep the
        // "remainder" and knock this remainder off the prior
        if (ms_lapse > (millis >> 1)) {
          millis = 0;
        } else {
          millis -= ms_lapse;
        }
      }
      
      if (millis > 0) {
        try {
          Thread.sleep(millis);
        } catch (InterruptedException e) {
          throw (new RuntimeException(e));
        }
        
        ms_prior = System.currentTimeMillis();
      }
    }
  }
  
  // launch_inner() has to call getPacket() after the line is open as
  // we cannot precog the winning format
  
  static Control launch_inner(final Callback callback)
  {
    final MixerTarget[] targets = detect_mixers(SourceDataLine.class);
    final AtomicBoolean shut_down = new AtomicBoolean(false);
    final AtomicReference<F1<Void, Integer>> decrement_threads = new AtomicReference<F1<Void, Integer>>();
    
    (new Thread()
      {
        public void run()
        {
          try {
            boolean using_default_volume = true;
            final LineKeeper lkpr = (new LineKeeper(targets[0]));
            
            Packet packet;
            
            while ((packet = callback.getPacket(lkpr.getCurrentFormat())) != null) {
              if ((packet.flush_target) || (packet.flush_format) || (packet.flush_volume && (packet.volume == -1) && (!lkpr.usingDefaultVolume()))) {
                log("audio writer: closing and reopening the line");
                
                lkpr.close();
                lkpr.open(((MixerTarget)(packet.target)), packet.format);
                
                if (packet.format != lkpr.getCurrentFormat()) {
                  // drop one packet (format mismatch) ... no big deal
                  // since the line change lost time anyways
                  log("audio writer: dropping one packet");
                  continue;
                }
              }
              
              if (packet.flush_volume && (packet.volume != -1)) {
                lkpr.setVolume(packet.volume);
              }
              
              if (packet.format != lkpr.getCurrentFormat()) {
                throw null;
              }
              
              lkpr.sendData(packet.arr, packet.off, packet.lim, packet.millis);
              
              if (shut_down.get()) {
                log("shutting down the audio writer");
                lkpr.close();
                decrement_threads.get().invoke(1);
                break;
              }
            }
            
            log("exited the audio writer packet loop, closing line");
            
            lkpr.close();
          } catch (Throwable e) {
            log(e);
            throw null;
          }
          
          log("exiting the audio writer");
        }
      }).start();
    
    return (new Control()
      {
        public Target[] getTargets()
        {
          return targets;
        }
        
        public int stop(F1<Void, Integer> dec)
        {
          decrement_threads.set(dec);
          shut_down.set(true);
          return 1;
        }
    });
  }
  
  /*
  static Target[] launch_inner(final Callback callback)
  {
    final Target[] targets = detect_mixers(SourceDataLine.class);
    
    log("starting writing thread");
    (new Thread()
      {
        public void run()
        {
          final int AUDIO_BUFFER_SAMPLE_COUNT = 2048;
          final double LOG_LIN_TO_DB_MULT = 20.0 / Math.log(10.0);
          
          Packet packet;
          
          if (targets.length <= 0) {
            throw new BAD("Found no mixers for playback.");
          }
          
          // start with the default mixer and format
          MixerTarget target = ((MixerTarget)(targets[0]));
          Format format = target.preferred;
          AudioFormat af = format.getAudioFormat();
          SourceDataLine line = null;
          FloatControl volume_control = null;
          FloatControl master_gain_control = null;
          
          try {
            line = ((SourceDataLine)(AudioSystem.getMixer(target.mixer).getLine(target.line)));
            line.open(af, AUDIO_BUFFER_SAMPLE_COUNT);
          } catch (Exception e) {
            line = null; // because open() may have been the cause
          }
          
          boolean using_default_volume = true;
          
          while ((packet = callback.getPacket(format)) != null) {
            boolean force_reopen = false;
            
            if (packet.flush_target) {
              force_reopen = true;
              
              /***** if (packet.target != null) {
                target = (MixerTarget)(packet.target);
              } else if (targets.length > 0) {
                target = (MixerTarget)(targets[0]);
              } else {
                target = null;
              }
            }

            if (packet.flush_format) {
              force_reopen = true;
              
              /***** if (packet.format != null) {
                format = packet.format;
              } else if (target != null) {
                format = target.preferred;
              } else {
                format = null; // all targets have a preferred format; the format is moot if there is no target
              }
            }
            
            if (!force_reopen && !using_default_volume && (packet.volume < 0)) {
              force_reopen = true;
              using_default_volume = true;
            }
            
            boolean force_flush_volume = false;
            
            if (force_reopen) {
              force_flush_volume = true;
              
              if (line != null) {
                line.stop();
                line.close();
                line = null;
              }
              
              if (target != null) {
                try {
                  line = (SourceDataLine)(AudioSystem.getMixer(target.mixer).getLine(target.line));
                  af = format.getAudioFormat();
                  line.open(af, AUDIO_BUFFER_SAMPLE_COUNT * (af.getSampleSizeInBits() >> 3));
                } catch (Exception e) {
                  log("" + e);
                  line = null; // because open() may have been the cause
                }
              }
              
              volume_control = null;
              master_gain_control = null;
              
              try {
                if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                  volume_control = (FloatControl)line.getControl(FloatControl.Type.VOLUME);
                }
              } catch (Exception e) {
                log("" + e);
              }
              
              try {
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                  master_gain_control = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
                }
              } catch (Exception e) {
                log("" + e);
              }
              
              line.start();
            }
            
            if (line != null) {
              if ((packet.flush_volume || force_flush_volume) && packet.volume >= 0.0f) {
                using_default_volume = false;
                if (volume_control != null) {
                  volume_control.setValue((float)(packet.volume));
                } else if (master_gain_control != null) {
                  float new_value = (float)Math.round(LOG_LIN_TO_DB_MULT * Math.log(packet.volume));
                  master_gain_control.setValue(new_value);
                }
              }
              
              line.write(packet.arr, packet.off, packet.lim - packet.off);
            } else {
              try {
                sleep(packet.millis);
              } catch (Exception e) {
                log("" + e);
                // ignored
              }
            }
          }
          
          if (line != null) {
            line.stop();
            line.close();
            line = null;
          }
        }
      }).start();
    
    return targets;
  }
  */
}

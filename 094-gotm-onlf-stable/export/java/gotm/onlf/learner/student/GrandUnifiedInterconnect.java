/***
 * GrandUnifiedInterconnect.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.learner.student;

import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import zs42.parts.F0;
import zs42.parts.F1;
import zs42.parts.OutgoingNetworkStream;

import zs42.pixels.codec.*;

import gotm.onlf.splitter.common.*;
import gotm.onlf.learner.common.*;
import gotm.onlf.utilities.*;

import static gotm.onlf.utilities.Utilities.*;

import static gotm.onlf.splitter.common.Constants.*;
import static gotm.onlf.learner.common.Constants.GrandUnifiedInterconnect.*;

public class GrandUnifiedInterconnect implements UserInterface.Callback, SplitterClient.Callback, AudioWriter.Callback
{
  static abstract class AudioStateVisitor<R>
  {
    abstract R onSilent();
    abstract R onStatic();
    abstract R onNormal();
  }
  
  static class CopycatAudioStateVisitor<R> extends AudioStateVisitor<R>
  {
    AudioStateVisitor<R> inner;
    
    CopycatAudioStateVisitor(AudioStateVisitor<R> inner)
    {
      this.inner = inner;
    }
    
    R onSilent() { return inner.onSilent(); }
    R onStatic() { return inner.onStatic(); }
    R onNormal() { return inner.onNormal(); }
  }
  
  static enum AudioState
  {
    /***
     * as a desired state: indicates that the user has selected mute
     * enable.
     * 
     * as an actual state: indicates that the last packet written to
     * audio_instant ramped down or was a silence packet, allowing a
     * mode change. has highest precedence over other states.
     ***/
    SILENT { <R> R visit(AudioStateVisitor<R> visitor) { return visitor.onSilent(); } },
    
    /***
     * as a desired state: indicates that the user has selected static
     * enable but not mute enable.
     * 
     * indicates that the last packet written to audio_instant was a
     * static packet.
     ***/
    STATIC { <R> R visit(AudioStateVisitor<R> visitor) { return visitor.onStatic(); } },
    
    /***
     * as a desired state: indicates that the user has selected neither
     * mute enable nor static enable.
     * 
     * indicates that the last packet written to audio_instant was
     * just a normal speech packet.
     ***/
    NORMAL { <R> R visit(AudioStateVisitor<R> visitor) { return visitor.onNormal(); } };
    
    /***
     * returns visitor.onState(), where the invoked function depends
     * on the audio state.
     ***/
    abstract <R> R visit(AudioStateVisitor<R> visitor);
    
    /***
     * returns the corresponding desired state from control statuses.
     ***/
    static AudioState getWantedFromControls(boolean silent_enable, boolean static_enable)
    {
      if (silent_enable) return SILENT;
      if (static_enable) return STATIC;
      return NORMAL;
    }
  }
  
  static class MediaPacket
  {
    final long source_tc;
    final long client_tc;
    
    MediaPacket(long source_tc, long client_tc)
    {
      this.source_tc = source_tc;
      this.client_tc = client_tc;
    }
  }
  
  static final class AudioPacket extends MediaPacket
  {
    static final AudioPacket SILENCE = (new AudioPacket(0, 0, (new short[CLIENT_PACKET_SAMPLE_COUNT])));
    
    final short[] signedPCM;
    
    AudioPacket(long source_tc, long client_tc, short[] signedPCM)
    {
      super(source_tc, client_tc);
      this.signedPCM = signedPCM;
    }
  }
  
  static final class VideoPacket extends MediaPacket
  {
    final int complexity;
    final byte[] codec_payload;
    
    VideoPacket(long source_tc, long client_tc, int complexity, byte[] codec_payload)
    {
      super(source_tc, client_tc);
      this.complexity = complexity;
      this.codec_payload = codec_payload;
    }
  }
  
  static final class TypedPacket extends MediaPacket
  {
    final boolean magic;
    final char    typed;
    
    TypedPacket(long source_tc, long client_tc, int encoded)
    {
      super(source_tc, client_tc);
      magic = ((encoded >>> 31) != 0);
      typed = ((char)(encoded & 0xFF));
    }
  }
  
  static abstract class Synchronizer
  {
    final long audio_delay_us;
    
    private boolean pure_video_mode = true;
    private boolean pure_video_init = false;
    private long    pure_video_base;
    private long    pure_video_lock;
    
    /***
     * a "timelock" establishes a correspondence between a source
     * timecode and a target (local) timecode, and specifies a ratio
     * for the target time clock tick length relative to the local
     * processor clock tick length.
     ***/
    protected long   lock_source_time;
    protected long   lock_target_time;
    protected double lock_target_rate; // 0.9 = fast, 1.0 = same, 1.1 = slow
    
    Synchronizer(long audio_delay_us)
    {
      this.audio_delay_us = audio_delay_us;
    }
    
    /***
     * implementations should update <code>lock_target_time</code>,
     * <code>lock_source_time</code> and <code>lock_target_rate</code>
     * according to their unique strategy.
     ***/
    abstract void notifyAudioPacket0(long now, long source_audio_tc);
    
    /***
     * implementations should update <code>lock_target_time</code>,
     * <code>lock_source_time</code> and <code>lock_target_rate</code>
     * according to their unique strategy.
     ***/
    abstract void notifyVideoPacket0(long now, long source_video_tc);
    
    /***
     * notifies the synchronizer that an audio packet with the given
     * source timecode (microseconds) is just about to be written to
     * the sound card. <code>now</code> should be the result of a
     * <i>recent</i> call to <code>microTime()</code>.
     ***/
    synchronized void notifyAudioPacket(long now, long source_audio_tc)
    {
      pure_video_mode = false;
      notifyAudioPacket0(now, source_audio_tc);
    }
    
    /***
     * returns the time difference (microseconds) between
     * <code>now</code> and the time at which the video frame (packet)
     * with the given source timecode (microseconds) should be
     * displayed. a positive return value indicates that the frame
     * should ideally be played back in that many microseconds. a
     * negative return value indicates that the frame should ideally
     * have been played back that many microseconds in the past (and
     * should probably be displayed immediately). a zero return value
     * is also possible if the calculated playback time is exactly
     * equal to <code>now</code>. <code>now</code> should be the
     * result of a <i>recent</i> call to <code>microTime()</code>.
     * 
     * the result of making calls to <code>getVideoLapse()</code>
     * before any calls to <code>notifyAudioPacket</code> is defined:
     * the first frame results in a zero return value, and subsequent
     * frames are played back at the normal rate of their timecodes.
     ***/
    synchronized long queryVideoLapse(long now, long source_video_tc)
    {
      if (pure_video_mode) {
        // pure video mode: no need to invoke strategy code, since
        // there is only one strategy ...
        {
          if (!pure_video_init) {
            pure_video_init = true;
            pure_video_base = source_video_tc;
            pure_video_lock = now;
          }
          
          return ((source_video_tc - pure_video_base) + (now - pure_video_lock));
        }
      } else {
        // mixed mode: let the strategy know about the video packet
        // (it should already have seen an audio packet first) and
        // skew time according to the values written in lock_* by the
        // strategy
        {
          notifyVideoPacket0(now, source_video_tc);
          
          long target_video_tc = source_video_tc + (lock_target_time - lock_source_time);
          target_video_tc += ((long)(((double)(target_video_tc - lock_target_time)) * lock_target_rate));
          target_video_tc -= audio_delay_us; // TODO: is this the right sign?
          
          return target_video_tc - now;
        }
      }
    }
  }
  
  static class PrimitiveSynchronizer extends Synchronizer
  {
    PrimitiveSynchronizer(long audio_delay_us)
    {
      super(audio_delay_us);
    }
    
    void notifyAudioPacket0(long now, long source_audio_tc)
    {
      lock_source_time = source_audio_tc;
      lock_target_time = now;
      lock_target_rate = 1.0;
    }
    
    void notifyVideoPacket0(long now, long source_video_tc)
    {
      // nothing to do
    }
  }
  
  static class QueueAdjuster
  {
    static final double[] ramp = (new double[AUDIO_FEATHER_WIN]);
    
    LocalDeque<AudioPacket> queue;
    int min;
    int max;
    int samplec = 0;
    int capacity = 0;
    int refl_off;
    short[] samples;
    float[] spe;
    ArrayList<Integer> silence_runs = new ArrayList<Integer>();
    
    static {
      for (int i = 0; i < AUDIO_FEATHER_WIN; i++) {
        ramp[i] = (double)(i) / AUDIO_FEATHER_WIN;
      }
    }
    
    QueueAdjuster(LocalDeque<AudioPacket> queue)
    {
      this.queue = queue;
    }
    
    void adjust(int min, int max)
    {
      int packetc = queue.size();
      
      if (packetc < max) return;
      
      long enter = microTime();
      
      this.min = min;
      this.max = max;
      
      // read samples
      
      samplec = packetc * CLIENT_PACKET_SAMPLE_COUNT;
      int target_packetc = min;
      int target_samplec = target_packetc * CLIENT_PACKET_SAMPLE_COUNT;
      
      if (samples == null || samplec > capacity) {
        samples = (new short[samplec]);
        spe = (new float[samplec]);
        capacity = samplec;
      }
      
      long begin_source_tc = queue.getFirst().source_tc;
      long end_source_tc = queue.getLast().source_tc + US_PER_PACKET;
      long rate_source_tc = (long)(((double)end_source_tc - (double)begin_source_tc + 0.5) / target_packetc);
      //long begin_server_tc = queue.getFirst().server_tc;
      //long end_server_tc = queue.getLast().server_tc + US_PER_PACKET;
      //long rate_server_tc = (long)(((double)end_server_tc - (double)begin_server_tc + 0.5) / target_packetc);
      
      {
        int k = 0;
        
        for (int i = 0, l = queue.size(); i < l; i++) {
          AudioPacket p = queue.get(i);
          
          for (int j = 0; j < CLIENT_PACKET_SAMPLE_COUNT; j++) {
            samples[k++] = p.signedPCM[j];
          }
        }
      }
      
      //dbg
      log ("adjust: read samples");
      
      // compute spe
      
      float max_spe;
      
      {
        int off = AUDIO_FEATHER_WIN;
        int lim = samplec - AUDIO_FEATHER_WIN;
        int i = off;
        float val = 0; // running value of spe
        
        for (int j = i - AUDIO_SPE_WIN_HSZ; j <= i + AUDIO_SPE_WIN_HSZ; j++) {
          val += Math.abs(samples[j]);
        }
        
        max_spe = spe[i] = val;
        
        int old_idx = AUDIO_FEATHER_WIN - AUDIO_SPE_WIN_HSZ;
        int new_idx = AUDIO_FEATHER_WIN + AUDIO_SPE_WIN_HSZ + 1;
        
        for (i = off + 1; i < lim; i++) {
          val += (Math.abs((float)samples[new_idx++]) - Math.abs((float)samples[old_idx++]));
          spe[i] = val;
          if (val > max_spe) {
            max_spe = val;
          }
        }
        
        for (i = 0; i < AUDIO_FEATHER_WIN; i++) spe[i] = max_spe;
        for (i = samplec - AUDIO_FEATHER_WIN; i < samplec; i++) spe[i] = max_spe;
      }
      
      //dbg
      log("adjust: computed spe");
      //
      
      int new_samplec = 0;
      {
        float low = 0, high = max_spe, mid = 0;
        final float min_interval = 0.00001f * max_spe;
        
        while ((high - low) > min_interval) {
          
          mid = 0.5f * (low + high);
          silence_runs.clear();
          new_samplec = samplec;
          
          for (int off = 0, lim; off < samplec; ) {
            // determine start of run
            if (spe[off] > mid) {
              off++;
              continue;
            }
            // determine end of run
            for (lim = off + 1; lim < samplec && spe[lim] <= mid; lim++) ;
            // ignore runs that are too short
            if (lim - off > 2 * AUDIO_FEATHER_WIN) {
              silence_runs.add(off);
              silence_runs.add(lim);
              new_samplec -= ((lim - off) - AUDIO_FEATHER_WIN);
            }
            off = lim;
          }
          
          //dbg
          log("adjust: target_samplec low high mid new_samplec " + target_samplec + " " + low + " " + high + " " + mid + " " + new_samplec + " " + (target_samplec - new_samplec));
          //
          
          if (new_samplec > target_samplec) {
            low = mid;
          } else {
            high = mid;
          }
        }
      }
      
      // correct the number samples using the first silence run -- and possibly the subsequent one(s) if it is not possible to use the first run. 'deficit' can be either positive or negative.
      
      int deficit = target_samplec - new_samplec;
      
      while (deficit != 0) {
        for (int k = 0; deficit != 0 && k < silence_runs.size(); k += 2) {
          int off = silence_runs.get(k), lim = silence_runs.get(k+1);
          int slack = lim - off - 2 * AUDIO_FEATHER_WIN;
          int correction = ((deficit < slack) ? deficit : slack);
          int correction0 = correction / 2;
          int constraint_correction0 = - (off - AUDIO_FEATHER_WIN);
          if (correction0 < constraint_correction0) correction0 = constraint_correction0;
          silence_runs.set(k, off + correction0);
          silence_runs.set(k+1, lim - (correction - correction0));
          deficit -= correction;
        }
      }
      
      //dbg
      // {
      //   int total = samplec;
      //   StringBuffer out = new StringBuffer();
      //   for (int k = 0; k < silence_runs.size(); k += 2) {
      //     out.append(" " + silence_runs.get(k) + " " + silence_runs.get(k+1));
      //     total -= (silence_runs.get(k+1) - silence_runs.get(k) - AUDIO_FEATHER_WIN);
      //   }
      //   log("adjust: total size runs " + total + " " + silence_runs.size() + " " + out);
      // }
      // log("st: samplec mid smallc target_samplec target_packetc " + samplec + " " + mid + " " + smallc + " " + target_samplec + " " + target_packetc);
      //
      
      // write samples
      
      // move samples to the beginning of the sample array
      {
        int off = 0, lim = 0, j = 0;
        for (int k = 0; k < silence_runs.size(); k += 2) {
          off = silence_runs.get(k);
          // copy large spe samples over to the begining of the array
          if (k > 0) {
            for (int i = lim; i < off; i++) {
              samples[j++] = samples[i];
            }
          } else {
            j = off;
          }
          
          // feather the start and end of the small spe run
          lim = silence_runs.get(k+1);
          off = lim - AUDIO_FEATHER_WIN;
          for (int i = off; i < lim; i++) {
            samples[j] = (short)((1.0 - ramp[i - off]) * samples[j] + ramp[i - off] * samples[i]);
            j++;
          }
        }
        for (int i = lim; i < samplec; i++) {
          samples[j++] = samples[i];
        }
        if (j != target_samplec) {
          log("adjust: ASSERTION FAILED: j == target_samplec. j: " + j + " target_samplec: " + target_samplec);
        }
      }
      
      int removec = packetc - target_packetc;
      
      while (removec-- > 0) queue.removeLast();
      
      if (queue.size() != target_packetc) throw null;
      
      long source_tc = begin_source_tc; //, server_tc = begin_server_tc;
      {
        int k = 0;
        
        LocalDeque<AudioPacket> queue_prime = (new LocalDeque<AudioPacket>());
        
        for (int i = 0, l = queue.size(); i < l; i++) {
          AudioPacket packet = queue.get(i);
          
          queue_prime.addLast((new AudioPacket(source_tc, packet.client_tc, packet.signedPCM)));
          
          for (int j = 0; j < CLIENT_PACKET_SAMPLE_COUNT; j++) {
            packet.signedPCM[j] = samples[k++];
          }
        }
        
        queue.clear();
        queue.addAll(queue_prime);
        
        /* old code that does not respect final
        for (AudioPacket p : queue) {
          p.source_tc = source_tc;
          p.server_tc = server_tc;
          
          for (int j = 0; j < CLIENT_PACKET_SAMPLE_COUNT; j++) {
            p.signedPCM[j] = samples[k++];
          }
          
          source_tc += rate_source_tc;
          server_tc += rate_server_tc;
        }
        */
      }
      
      //dbg
      {
        // StringBuffer out = (new StringBuffer());
        
        // for (AudioPacket p : queue) {
        //   out.append(" " + trim(p.source_tc));
        // }
        // log("adjust: final source_tcs" + out);
      }
      //
      
      log("adjusted the reserve queue. init size: " + packetc + " final size: " + queue.size() + " time: " + (microTime() - enter));
    }
  }
  
  // **********************
  // * ENTER STATE FIELDS *
  // **********************
  
  // random number generator
  Random random = (new Random());
  
  // control objects
  AudioWriter.Control    audio_writer_control = null;
  SplitterClient.Control splitter_client_control = null;
  UserInterface.Control  user_interface_control = null;
  
  // stop condition
  boolean stop_condition = false;
  
  // GUI state
  boolean luser_flush_target = true;
  boolean luser_flush_format = true;
  boolean luser_flush_volume = false;
  AudioCommon.Target luser_target = null;
  AudioCommon.Format luser_format = null;
  double luser_volume = -1f;
  boolean luser_silent_enable = false;
  boolean luser_static_enable = false;
  
  // audio receive packet queue
  LocalDeque<AudioPacket> audio_receive_queue = new LocalDeque<AudioPacket>();
  
  // audio reserve packet queue
  LocalDeque<AudioPacket> audio_reserve_queue = new LocalDeque<AudioPacket>();
  
  // adjuster for the audio reserve packet queue
  QueueAdjuster audio_reserve_queue_adjuster = (new QueueAdjuster(audio_reserve_queue));
  
  // audio writer spliced state (true iff packets in audio_*_queue do not directly follow written packets)
  boolean audio_spliced = false;
  
  // audio writer output state (as of the most recent assignment to audio_instant)
  AudioState audio_output_state = AudioState.SILENT;
  
  // audio writer recover buffer (null when depleted)
  AudioPacket audio_recover = null;
  
  // audio writer instant buffer (null when depleted)
  AudioPacket audio_instant = null;

  // number of interrupts caused by audio queue underflows or overflows
  int audio_underflow_count = 0;
  int audio_overflow_count = 0;
  
  // video receive packet queue
  LocalDeque<VideoPacket> video_queue = (new LocalDeque<VideoPacket>());
  
  // queue of video packets for incomplete updates
  LocalDeque<VideoPacket> incomplete_update_queue = (new LocalDeque<VideoPacket>());
  
  // typed character packet queue
  LocalDeque<TypedPacket> typed_queue = (new LocalDeque<TypedPacket>());
  
  // etch packet queue
  LocalDeque<UserInterface.EtchPacket> etch_queue = (new LocalDeque<UserInterface.EtchPacket>());
  
  // A/V sync variables
  final Synchronizer synchronizer = (new PrimitiveSynchronizer(0));
  /*
  long av_sync_audio_tc = 0; // timecode of audio packet most recently issued to AudioWriter
  long av_sync_prev_frame_tc = 0; // local time at which the previous packet was issued
  long av_sync_frame_tc = 0; // local time at which that packet was issued
  double av_sync_frame_tc_drift = 0; // correction frame_tc over the duration of the next audio packet, per us
  long av_sync_au_delay = 0;
  double av_sync_avg_delta = 0;
  */
  
  long instance_id;
  
  // **********************
  // * LEAVE STATE FIELDS *
  // **********************
  
  static final double[] ramp01 = (new double[CLIENT_PACKET_SAMPLE_COUNT]);
  static final double[] ramp10 = (new double[CLIENT_PACKET_SAMPLE_COUNT]);
  
  static {
    int k = RAMP_SAMPLE_COUNT;
    
    for (int i = 0; i < k; i++) {
      ramp01[i] = 0.0;
    }
    
    for (int i = k, l = CLIENT_PACKET_SAMPLE_COUNT; i < l; i++) {
      ramp01[i] = (((double)(i - k)) / (l - k));
    }
    
    for (int i = 0, l = CLIENT_PACKET_SAMPLE_COUNT; i < l; i++) {
      ramp10[i] = ramp01[l - i - 1];
    }
  }
  
  AudioPacket rampPacket(AudioPacket packet, double[] ramp)
  {
    short[] signedPCM = packet.signedPCM;
    
    for (int i = 0, l = signedPCM.length; i < l; i++) {
      signedPCM[i] = ((short)(ramp[i] * signedPCM[i]));
    }
    
    return packet;
  }
  
  AudioPacket rampPacket01(AudioPacket packet)
  {
    return rampPacket(packet, ramp01);
  }
  
  AudioPacket rampPacket10(AudioPacket packet)
  {
    return rampPacket(packet, ramp10);
  }
  
  AudioPacket generateStaticPacket()
  {
    int range_mask = (1 << (AUDIO_STATIC_AMPLOG + 1)) - 1;
    int adjustment = range_mask >> 1;
    
    short[] signedPCM = (new short[CLIENT_PACKET_SAMPLE_COUNT]);
    
    for (int i = 0; i < signedPCM.length; i++) {
      signedPCM[i] = ((short)((random.nextInt() & range_mask) - adjustment));
    }
    
    AudioPacket packet = (new AudioPacket(0, 0, signedPCM));
    
    return packet;
  }
  
  AudioStateVisitor<Boolean> sourceCanCommence =
    (new AudioStateVisitor<Boolean>()
     {
       Boolean onSilent() { return true; }
       Boolean onStatic() { return true; }
       
       Boolean onNormal()
       {
         return (audio_reserve_queue.size() >= AUDIO_RESERVE_QUEUE_MIN);
       }
     });
  
  AudioStateVisitor<Boolean> sourceCanContinue =
    (new AudioStateVisitor<Boolean>()
     {
       Boolean onSilent() { return true; }
       Boolean onStatic() { return true; }
       
       Boolean onNormal()
       {
         return (audio_reserve_queue.size() > 0) && !audio_spliced;
       }
     });
  
  // the main function here is to prime an audio source's recovery
  // mechanism; should be invoked after sourceCanCommence() returns
  // true on a desired audio source
  AudioStateVisitor<Void> sourceActuallyCommence =
    (new AudioStateVisitor<Void>()
     {
       Void onSilent() { return null; }
       Void onStatic() { return null; }
       
       Void onNormal()
       {
         audio_spliced = false;
         audio_recover = audio_reserve_queue.removeFirst();
         return null;
       }
     });
  
  // dequeue an ordinary "non-fading" packet; this must fail rather
  // than returning the last "recover" packet (which must be used for
  // fading)
  AudioStateVisitor<AudioPacket> sourceGetOrdinary =
    (new AudioStateVisitor<AudioPacket>()
     {
       AudioPacket onSilent() { return AudioPacket.SILENCE; }
       AudioPacket onStatic() { return generateStaticPacket(); }
       
       AudioPacket onNormal()
       {
         if (audio_spliced) {
           return null;
         }
         
         if (audio_reserve_queue.size() > 0) {
           AudioPacket retv = audio_recover;
           audio_recover = audio_reserve_queue.removeFirst();
           return retv;
         } else {
           return null;
         }
       }
     });
  
  // dequeue a "fading" packet, possibly the last available packet in
  // the stream
  AudioStateVisitor<AudioPacket> sourceGetRecovery =
    (new CopycatAudioStateVisitor<AudioPacket>(sourceGetOrdinary)
     {
       AudioPacket onNormal()
       {
         AudioPacket retv = audio_recover;
         
         if (audio_reserve_queue.size() > 0) {
           audio_recover = audio_reserve_queue.removeFirst();
         } else {
           audio_recover = null;
         }
         
         return retv;
       }
     });
  
  void fillAudioWriterInstantBuffer()
  {
    // check that it's empty
    if (audio_instant != null) throw null;
    
    AudioState audio_desired_state = AudioState.getWantedFromControls(luser_silent_enable, luser_static_enable);
    
    if (audio_desired_state != audio_output_state) {
      if (audio_output_state != AudioState.SILENT) {
        audio_instant = rampPacket10(audio_output_state.visit(sourceGetRecovery));
        audio_output_state = AudioState.SILENT;
      } else {
        if (audio_desired_state.visit(sourceCanCommence)) {
          audio_desired_state.visit(sourceActuallyCommence);
          audio_instant = rampPacket01(audio_desired_state.visit(sourceGetOrdinary));
          audio_output_state = audio_desired_state;
        } else {
          audio_instant = AudioPacket.SILENCE;
        }
      }
    } else {
      if (audio_desired_state.visit(sourceCanContinue)) {
        audio_instant = audio_desired_state.visit(sourceGetOrdinary);
      } else {
        audio_instant = rampPacket10(audio_desired_state.visit(sourceGetRecovery));
        audio_output_state = AudioState.SILENT;
        
        if (audio_spliced) {
          audio_overflow_count++;
        } else {
          audio_underflow_count++;
        }
      }
    }
  }
  
  void settleReceiveQueue()
  {
    boolean modified_reserve_queue = false;
    
    if (audio_receive_queue.size() > 0) {
      int server_packet_sample_count = audio_receive_queue.peekFirst().signedPCM.length;
      
      while ((server_packet_sample_count * audio_receive_queue.size()) >= CLIENT_PACKET_SAMPLE_COUNT) {
        int client_packet_pointer = 0;
        short[] client_packet = (new short[CLIENT_PACKET_SAMPLE_COUNT]);
        
        long source_tc = audio_receive_queue.peekFirst().source_tc;
        
        while (client_packet_pointer < client_packet.length) {
          System.arraycopy(audio_receive_queue.removeFirst().signedPCM, 0, client_packet, client_packet_pointer, server_packet_sample_count);
          client_packet_pointer += server_packet_sample_count;
        }
        
        audio_reserve_queue.addLast((new AudioPacket(source_tc, 0, client_packet)));
        
        modified_reserve_queue = true;
      }
    }
    
    if (modified_reserve_queue) {
      settleReserveQueue();
    }
  }
  
  void settleReserveQueue()
  {
    if (audio_reserve_queue.size() > AUDIO_RESERVE_QUEUE_MAX) {
      log("audio reserve queue overflow");
      
      if (AUDIO_CONTENT_SENSITIVE_TRUNCATION) {
        audio_reserve_queue_adjuster.adjust(AUDIO_RESERVE_QUEUE_TRC, AUDIO_RESERVE_QUEUE_MAX);
      } else {
        audio_spliced = true;
        
        while (audio_reserve_queue.size() > AUDIO_RESERVE_QUEUE_TRC) {
          audio_reserve_queue.removeFirst();
        }
      }
    }
    
    log("audio reserve queue length: " + audio_reserve_queue.size());
  }
  
  static byte[] stringToBytes(String s)
  {
    byte[] b = (new BigInteger("01" + s, 16)).toByteArray();
    byte[] o = (new byte[b.length - 1]);
    for (int i = 0; i < o.length; i++) { o[i] = b[i+1]; }
    return o;
  }
  
  // return stream function factories
  
  void transmitString(OutgoingNetworkStream out, byte code, String text)
  {
    byte[] conv;
    
    try {
      conv = text.getBytes("UTF-8");
    } catch (Exception e) {
      throw (new BAD(e));
    }
    
    out.wI(1 + conv.length);
    out.wB(code);
    out.wB(conv);
  }
  
  F1<Void, OutgoingNetworkStream> sendUserFeedback(final String text)
  {
    return
      (new F1<Void, OutgoingNetworkStream>()
       {
         public Void invoke(OutgoingNetworkStream out)
         {
           transmitString(out, RET_FEEDBACK, text);
           return null;
         }
       });
  }

  F1<Void, OutgoingNetworkStream> sendCommandReply(final String text)
  {
    return
      (new F1<Void, OutgoingNetworkStream>()
       {
         public Void invoke(OutgoingNetworkStream out)
         {
           transmitString(out, RET_COMMAND_REPLY, text);
           return null;
         }
       });
  }
  
  F1<Void, OutgoingNetworkStream> sendQueueSizes(final int sizeA, final int sizeV, final int undrA, final int overA)
  {
    return
      (new F1<Void, OutgoingNetworkStream>()
       {
         public Void invoke(OutgoingNetworkStream out)
         {
           out.wI(1 + 4 + 4 + 4 + 4);
           out.wB(RET_QUEUE_SIZES);
           out.wI(sizeA);
           out.wI(sizeV);
           out.wI(undrA);
           out.wI(overA);
           
           return null;
         }
       });
  }
  
  // command processing
  
  String processCommand(String request)
  {
    String[] words = (new String[] { "", "", "", "", "" });
    
    {
      String[] split = request.split("\\s");
      for (int i = 0; i < split.length; i++) words[i] = split[i];
    }
    
    if (!(("" + instance_id).startsWith(words[0]))) {
      log("request ignored '" + request + "' due to instance id mismatch");
      return null;
    }
    
    String reply = "command not understood";
    
    /****/ if (words[1].equals("arst")) {
      AUDIO_RESERVE_QUEUE_MIN = DEFAULT_AUDIO_RESERVE_QUEUE_MIN;
      AUDIO_RESERVE_QUEUE_TRC = DEFAULT_AUDIO_RESERVE_QUEUE_TRC;
      AUDIO_RESERVE_QUEUE_MAX = DEFAULT_AUDIO_RESERVE_QUEUE_MAX;
      
      AUDIO_CONTENT_SENSITIVE_TRUNCATION = DEFAULT_AUDIO_CONTENT_SENSITIVE_TRUNCATION;
      
      reply = "audio queue parameters reset";
    } else if (words[1].equals("aset")) {
      AUDIO_RESERVE_QUEUE_MIN = Integer.parseInt(words[2]);
      AUDIO_RESERVE_QUEUE_TRC = Integer.parseInt(words[3]);
      AUDIO_RESERVE_QUEUE_MAX = Integer.parseInt(words[4]);
      
      reply = "audio queue parameters set (MIN=" + AUDIO_RESERVE_QUEUE_MIN + ", TRC=" + AUDIO_RESERVE_QUEUE_TRC + ", MAX=" + AUDIO_RESERVE_QUEUE_MAX + ")";
    } else if (words[1].equals("aqld")) {
      AUDIO_RESERVE_QUEUE_MAX = Integer.MAX_VALUE;
      
      reply = "audio queue limit disabled";
    } else if (words[1].equals("acst")) {
      AUDIO_CONTENT_SENSITIVE_TRUNCATION = true;
      
      reply = "audio queue smartness enabled";
    }
    
    return reply;
  }
  
  synchronized F0<Void> initialize(PixelsCodec.Dimensions dim, String host, String port, String user_pass, String want_bits, String splitter_instance_id, int ups, long start_time, HashMap<String, String> settings)
  {
    log("launching AudioWriter");
    
    AudioCommon.Target[] targets;
    
    audio_writer_control = AudioWriter.launch(this);
    
    log("launching SplitterClient");
    
    byte[] user_pass_bytes = stringToBytes(user_pass);
    
    if (user_pass_bytes.length != 32) throw (new RuntimeException());
    
    instance_id = Long.parseLong(splitter_instance_id);
    
    {
      String cookedPath = settings.get("COOKED_PATH");
      
      if (cookedPath != null) {
        splitter_client_control = SplitterClient.launch_recording(this, "http://" + host + "/" + cookedPath);
        AUDIO_RESERVE_QUEUE_MAX = Integer.MAX_VALUE;
      } else {
        splitter_client_control = SplitterClient.launch(this, host, Integer.parseInt(port), user_pass_bytes, instance_id);
      }
    }
    
    splitter_client_control.putStreamSelectVector(Long.parseLong(want_bits));
    
    log("launching UserInterface");
    
    user_interface_control = UserInterface.launch(this, audio_writer_control.getTargets(), dim, ups, start_time, settings);
    
    return
      (new F0<Void>()
       {
         public Void invoke()
         {
           final AtomicInteger rem = (new AtomicInteger(0));
           
           final F1<Void, Integer> dec =
             (new F1<Void, Integer>()
              {
                public Void invoke(Integer amt)
                {
                  rem.addAndGet(-amt);
                  return null;
                }
              });
           
           rem.addAndGet(splitter_client_control.stop(dec));
           rem.addAndGet(audio_writer_control.stop(dec));
           rem.addAndGet(user_interface_control.stop(dec));
           
           while (rem.get() > 0) {
             try {
               Thread.sleep(SHUTDOWN_NAP_MS);
             } catch (InterruptedException e) {
               throw (new RuntimeException(e));
             }
           }
           
           return null;
         }
       });
  }
  
  // USER INTERFACE CALLBACKS
  
  public synchronized String gotCommand(String request)
  {
    return processCommand("" + instance_id + " " + request);
  }
  
  public synchronized void gotMute(boolean enabled)
  {
    luser_silent_enable = enabled;
  }
  
  public synchronized void gotStatic(boolean enabled)
  {
    luser_static_enable = enabled;
  }
  
  public synchronized void gotTarget(AudioCommon.Target target)
  {
    luser_flush_target = true;
    luser_target = target;
  }
  
  public synchronized void gotFormat(AudioCommon.Format format)
  {
    luser_flush_format = true;
    luser_format = format;
  }
  
  public synchronized void gotVolume(double volume)
  {
    luser_flush_volume = true;
    luser_volume = volume;
  }
  
  public synchronized void gotUserFeedback(String line)
  {
    user_interface_control.putLine("FEEDBACK " + line + "\n");
    splitter_client_control.enqueueOutgoingSnippet(sendUserFeedback(line));
  }
  
  public synchronized void gotSystemFeedback(String line)
  {
    user_interface_control.putLine("SYSTEM " + line + "\n");
  }
  
  public synchronized UserInterface.Packet getPacket()
  {
    final long now = microTime();
    
    final int INCOMPLETE_MASK = (1 << 31);
    
    // transfer incomplete packets to the incomplete queue
    {
      while ((!video_queue.isEmpty()) && ((video_queue.peekFirst().complexity & INCOMPLETE_MASK) != 0)) {
        incomplete_update_queue.addLast(video_queue.removeFirst());
      }
    }
    
    // if the video queue is consequently empty, then there are no
    // packets to display at this time
    {
      if (video_queue.isEmpty()) {
        return null;
      }
    }
    
    VideoPacket finale = video_queue.peekFirst();
    long video_lapse = synchronizer.queryVideoLapse(now, finale.source_tc);
    
    // we are headed towards a chain of updates culminating in a
    // complete update, so check that it is to be displayed at this
    // time
    {
      if (video_lapse > 0) {
        return null; // no packet to display at this time
      }
    }
    
    // if there is any packet in the incomplete queue, return it
    {
      if (!(incomplete_update_queue.isEmpty())) {
        return (new UserInterface.Packet(false, true, incomplete_update_queue.removeFirst().codec_payload));
      }
    }
    
    // return the "finale" packet from the head of the video queue (a
    // complete update for sure)
    {
      video_queue.removeFirst(); // since we're returning it for sure
      boolean suppress = ((video_lapse < -MAXIMUM_VIDEO_LAPSE_US) || (finale.complexity > MAXIMUM_IMMEDIATE_FRAME_COMPLEXITY));
      return (new UserInterface.Packet(true, suppress, finale.codec_payload));
    }
  }
  
  public synchronized UserInterface.TypedPacket getTypedPacket()
  {
    TypedPacket tp = typed_queue.peekFirst();
    
    if (tp == null) return null;
    
    long now = microTime();
    
    long typed_lapse;
    
    if ((typed_lapse = synchronizer.queryVideoLapse(now, tp.source_tc)) > 0) {
      return null; // no packet to display at this time
    }
    
    tp = typed_queue.removeFirst();
    
    return (new UserInterface.TypedPacket(tp.magic, tp.typed));
  }
  
  public synchronized UserInterface.EtchPacket getEtchPacket()
  {
    UserInterface.EtchPacket ep = etch_queue.peekFirst();
    
    if (ep == null) return null;
    
    long now = microTime();
    
    long etch_lapse;
    
    if ((etch_lapse = synchronizer.queryVideoLapse(now, ep.ustc)) > ETCH_QUEUE_AHEAD_US) {
      return null; // do not release yet
    }
    
    if (etch_queue.removeFirst() != ep) throw null;
    
    return ep.reclock(now + etch_lapse + ETCH_EVENT_DELAY_US);
  }
  
  // SPLITTER CLIENT CALLBACKS
  
  public synchronized void gotPacket(long source_tc, long client_tc, int stream_id, GroupirPacket groupir)
  {
    switch (stream_id) {
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_COMMAND:
      {
        final String request = (new String(groupir.dB));
        final String reply = processCommand(request);
        
        //user_interface_control.putLine("ADMIN (" + request + ") => (" + reply + ")\n");
        
        log("request: '" + request + "'");
        
        if (reply != null) {
          log("reply: '" + reply + "'");
          splitter_client_control.enqueueOutgoingSnippet(sendCommandReply(reply));
        }
        
        break;
      }
      
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_AUDIO:
      {
        audio_receive_queue.addLast((new AudioPacket(source_tc, client_tc, AudioCommon.unlaw(groupir.dB, 0, groupir.dB.length))));
        settleReceiveQueue();
        
        // report
        splitter_client_control.enqueueOutgoingSnippet(sendQueueSizes(audio_reserve_queue.size(), video_queue.size(), audio_underflow_count, audio_overflow_count));
        
        break;
      }
      
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_VIDEO_4:
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_VIDEO_5:
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_VIDEO_6:
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_VIDEO_7:
      {
        final int complexity = groupir.dI[PACKET_METADATA_COMPLEXITY];
        
        video_queue.addLast((new VideoPacket(source_tc, client_tc, complexity, groupir.dB)));
        
        // report
        //splitter_client_control.enqueueOutgoingSnippet(sendQueueSizes(audio_reserve_queue.size(), video_queue.size(), audio_underflow_count, audio_overflow_count));
        
        break;
      }
      
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_NWED:
      {
        final int encoded = groupir.dI[PACKET_METADATA_ENCODED];
        
        typed_queue.addLast((new TypedPacket(source_tc, client_tc, encoded)));
        
        break;
      }
      
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_ETCH_JARFILE:
      {
        user_interface_control.gotEtchJarfilePacket(groupir.dI[PACKET_METADATA_COMPLEXITY], groupir.dB);
        break;
      }
      
    case gotm.onlf.splitter.common.Constants.PACKET_METADATA_STREAM_ID_ETCH_EVENTS:
      {
        etch_queue.addLast((new UserInterface.EtchPacket(source_tc, groupir.dI[PACKET_METADATA_COMPLEXITY], groupir.dB)));
        break;
      }
      
    default:
      throw (new RuntimeException());
    }
  }
  
  public synchronized void gotStopCondition()
  {
    stop_condition = true;
  }
  
  // AUDIO WRITER CALLBACKS
  
  public synchronized AudioWriter.Packet getPacket(AudioCommon.Format format)
  {
    AudioWriter.Packet output = (new AudioWriter.Packet());
    
    output.flush_target = luser_flush_target; luser_flush_target = false;
    output.flush_format = luser_flush_format; luser_flush_format = false;
    output.flush_volume = luser_flush_volume; luser_flush_volume = false;
    
    output.target = luser_target;
    output.format = luser_format;
    output.volume = luser_volume;
    
    output.millis = CLIENT_PACKET_SAMPLE_COUNT / SAMPLES_PER_MS;
    
    fillAudioWriterInstantBuffer();
    output.arr = format.adaptSignedPCM(audio_instant.signedPCM);
    output.off = 0;
    output.lim = output.arr.length;
    
    // note the packet format on the packet for double-check
    if (!output.flush_format) {
      output.format = format;
    }
    
    // adjust a/v sync variables
    if (audio_output_state == AudioState.NORMAL) {
      synchronizer.notifyAudioPacket(microTime(), audio_instant.source_tc);
      
      /*
      av_sync_prev_frame_tc = av_sync_frame_tc;
      
      av_sync_audio_tc = audio_instant.source_tc;
      
      long now = microTime();
      long delta_frame_tc = now - av_sync_prev_frame_tc;
      
      if (-AV_SYNC_SMOOTHING_MAX_DELTA_US < delta_frame_tc && delta_frame_tc < AV_SYNC_SMOOTHING_MAX_DELTA_US) {
      
        // smooth out fluctuations in (frame_tc - audio_tc)
        long delta = now - av_sync_audio_tc;
        
        av_sync_avg_delta = AV_SYNC_SMOOTHING_ALPHA * ((double)delta - av_sync_avg_delta) + av_sync_avg_delta;
        av_sync_frame_tc = av_sync_audio_tc + Math.round(av_sync_avg_delta);
        av_sync_frame_tc_drift = (double)(av_sync_frame_tc - av_sync_prev_frame_tc) / (double)AV_SYNC_IDEAL_ELAPSED_US - 1.0;
      } else {
        av_sync_frame_tc = now;
        av_sync_frame_tc_drift = 0;
      }
      */
    }
    
    audio_instant = null;
    
    return output;
  }
  
  /***
   * launches a new instance of the application. the returned handler can be invoked
   * to shut down all components gracefully.
   ***/
  static F0<Void> launch(PixelsCodec.Dimensions dim, String splitter_host, String splitter_port, String splitter_user_pass, String splitter_want_bits, String splitter_instance_id, int ups, long start_time, HashMap<String, String> settings)
  {
    final GrandUnifiedInterconnect gui = (new GrandUnifiedInterconnect());
    
    return gui.initialize(dim, splitter_host, splitter_port, splitter_user_pass, splitter_want_bits, splitter_instance_id, ups, start_time, settings);
  }
  
  public static void main(String args[])
  {
    final long start_time = System.currentTimeMillis();
    
    try {
      int nr = 0;
      
      final int ARGI_H                       = nr++;
      final int ARGI_W                       = nr++;
      final int ARGI_SPLITTER_HOST           = nr++;
      final int ARGI_SPLITTER_PORT           = nr++;
      final int ARGI_SPLITTER_USER_PASS_FILE = nr++;
      final int ARGI_SPLITTER_WANT_BITS      = nr++;
      final int ARGI_SPLITTER_INSTANCE_ID    = nr++;
      final int ARGI_UPS                     = nr++;
      final int ARGI_EXPECTED_LENGTH         = nr++;
      final int ARGI_ETCH_H                  = nr++;
      final int ARGI_ETCH_W                  = nr++;
      final int ARGI_ETCH_UPS                = nr++;

      for (int i = 0; i < args.length; i++) {
        log("args[" + i + "]='" + args[i] + "'");
      }
      
      if (args.length != ARGI_EXPECTED_LENGTH) {
        for (String line : (new String[] { "usage:", "  java gotm.onlf.lerner.student.GrnadUnifiedInterconnect (H) (W) (splitter host) (splitter port) (splitter user pass file) (splitter want bits) (updates per second)"})) {
          log(line);
        }
        
        System.exit(1);
        throw null;
      }
      
      final PixelsCodec.Dimensions dim =
        (new PixelsCodec.Dimensions
         (Integer.parseInt(args[ARGI_H]), Integer.parseInt(args[ARGI_W])));
      
      final int ups = Integer.parseInt(args[ARGI_UPS]);
      
      HashMap<String, String> settings = (new HashMap<String, String>());
      
      settings.put("ETCH_H", args[ARGI_ETCH_H]);
      settings.put("ETCH_W", args[ARGI_ETCH_W]);
      settings.put("ETCH_UPS", args[ARGI_ETCH_UPS]);
      
      launch(dim, args[ARGI_SPLITTER_HOST], args[ARGI_SPLITTER_PORT], args[ARGI_SPLITTER_USER_PASS_FILE], args[ARGI_SPLITTER_WANT_BITS], args[ARGI_SPLITTER_INSTANCE_ID], ups, start_time, settings);
    } catch (Throwable e) {
      fatal(e);
    }
  }
}

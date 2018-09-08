/***
 * Audio2g.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.au2g;

import zs42.au2g.ny4j.bind.*;

import zs42.mass.*;
import zs42.buff.*;
import zs42.ny4j.*;

import zs42.parts.Log;
import zs42.parts.MicroTime;
import zs42.parts.ByteArrayCache;

import java.util.*;
import java.util.concurrent.*;

import javax.sound.sampled.*;

public class Audio2g
{
  // N.B. it is assumed that SAMPLE_RATE_REPLAY_HZ is an exact multiple of SAMPLE_RATE_RECORD_HZ
  public static final int SAMPLE_RATE_RECORD_HZ = 8000;
  public static final int SAMPLE_RATE_REPLAY_HZ = 48000;
  
  public static final int SAMPLE_SIZE_RECORD_NR = 2048;
  public static final int SAMPLE_SIZE_REPLAY_NR = ((SAMPLE_RATE_REPLAY_HZ / SAMPLE_RATE_RECORD_HZ) * SAMPLE_SIZE_RECORD_NR);
  
  public static volatile double record_lapse_avg;
  public static volatile double record_lapse_max;
  
  public static volatile double replay_lapse_avg;
  public static volatile double replay_lapse_max;
  
  /****************************************************************************
   * FORMAT/MIXER MODEL
   ****************************************************************************/
  
  public static enum MixerFormat
  {
    F_1_U_EL
      {
        public String getLabel() { return "1/U/EL"; }
        public int getChannelCount() { return 1; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return false; }
        public boolean isLittleEndian() { return true; }
      },
    
    F_1_U_BE
      {
        public String getLabel() { return "1/U/BE"; }
        public int getChannelCount() { return 1; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return false; }
        public boolean isLittleEndian() { return false; }
      },
    
    F_1_S_EL
      {
        public String getLabel() { return "1/S/EL"; }
        public int getChannelCount() { return 1; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return true; }
        public boolean isLittleEndian() { return true; }
      },
    
    F_1_S_BE
      {
        public String getLabel() { return "1/S/BE"; }
        public int getChannelCount() { return 1; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return true; }
        public boolean isLittleEndian() { return false; }
      },
    
    F_2_U_EL
      {
        public String getLabel() { return "2/U/EL"; }
        public int getChannelCount() { return 2; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return false; }
        public boolean isLittleEndian() { return true; }
      },
    
    F_2_U_BE
      {
        public String getLabel() { return "2/U/BE"; }
        public int getChannelCount() { return 2; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return false; }
        public boolean isLittleEndian() { return false; }
      },
    
    F_2_S_EL
      {
        public String getLabel() { return "2/S/EL"; }
        public int getChannelCount() { return 2; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return true; }
        public boolean isLittleEndian() { return true; }
      },
    
    F_2_S_BE
      {
        public String getLabel() { return "2/S/BE"; }
        public int getChannelCount() { return 2; }
        public int getSampleSize() { return 2; }
        public boolean isSignedComplement() { return true; }
        public boolean isLittleEndian() { return false; }
      };
    
    public static final MixerFormat[] zerol = (new MixerFormat[0]);
    
    public static final cL<MixerFormat> sensible_reader =
      cL.newCopyOf
      ((new MixerFormat[]
        {
          F_1_S_EL, F_1_S_BE, F_1_U_EL, F_1_U_BE,
          F_2_S_EL, F_2_S_BE, F_2_U_EL, F_2_U_BE
        }));
    
    public static final cL<MixerFormat> sensible_writer =
      cL.newCopyOf
      ((new MixerFormat[]
        {
          F_2_S_EL, F_2_S_BE, F_2_U_EL, F_2_U_BE,
          F_1_S_EL, F_1_S_BE, F_1_U_EL, F_1_U_BE
        }));
    
    public abstract String getLabel();
    public abstract int getChannelCount();
    public abstract int getSampleSize();
    public abstract boolean isSignedComplement();
    public abstract boolean isLittleEndian();
    
    public AudioFormat getAudioFormat(final int sampleRateHertz)
    {
      return (new AudioFormat(sampleRateHertz, (getSampleSize() << 3), getChannelCount(), isSignedComplement(), !isLittleEndian()));
    }
    
    private static int decodeHelperEL(Buffer.nS wrc, byte[] buf, int off)
    {
      wrc.aS
        ((short)
         (((buf[off++] & 0xFF)     ) |
          ((buf[off++] & 0xFF) << 8)));
      
      return off;
    }
    
    private static int decodeHelperBE(Buffer.nS wrc, byte[] buf, int off)
    {
      wrc.aS
        ((short)
         (((buf[off++] & 0xFF) << 8) |
          ((buf[off++] & 0xFF)     )));
      
      return off;
    }
    
    private static int encodeHelperEL(Buffer.oS rdc, byte[] buf, int off)
    {
      short val = rdc.rS();
      
      buf[off++] = ((byte)(val     ));
      buf[off++] = ((byte)(val >> 8));
      
      return off;
    }
    
    private static int encodeHelperBE(Buffer.oS rdc, byte[] buf, int off)
    {
      short val = rdc.rS();
      
      buf[off++] = ((byte)(val >> 8));
      buf[off++] = ((byte)(val     ));
      
      return off;
    }
    
    private static void vectorConstantAddition(Packet packet, int adjust)
    {
      for (int i = 0; i < packet.getChannelCount(); i++) {
        Buffer.oS ovwr = packet.getChannelBuffer(i).iterate();
        
        int rem = ovwr.remaining();
        
        while (rem-- > 0) {
          ovwr.wS(((short)(ovwr.peekS() + adjust)));
        }
        
        ovwr.release();
      }
    }
    
    private static final int C_S2U = (1 << 16);
    private static final int C_U2S = (- C_S2U);
    
    public static void decodeRawBytes(Packet packet, byte[] buf, int off, int lim, MixerFormat format, BufferCentral central)
    {
      if (!(format.getSampleSize() == 2)) throw null;
      
      switch (format.getChannelCount()) {
      case 1:
        {
          Buffer.xS buf0 = central.acquireS();
          
          Buffer.nS wrc0 = buf0.prepend();
          
          if (format.isLittleEndian()) {
            while (off < lim) {
              off = decodeHelperEL(wrc0, buf, off);
            }
          } else {
            while (off < lim) {
              off = decodeHelperBE(wrc0, buf, off);
            }
          }
          
          wrc0.release();
          
          packet.addChannelBuffer(buf0);
          
          break;
        }
        
      case 2:
        {
          Buffer.xS buf0 = central.acquireS();
          Buffer.xS buf1 = central.acquireS();
          
          Buffer.nS wrc0 = buf0.prepend();
          Buffer.nS wrc1 = buf1.prepend();
          
          if (format.isLittleEndian()) {
            while (off < lim) {
              off = decodeHelperEL(wrc0, buf, off);
              off = decodeHelperEL(wrc1, buf, off);
            }
          } else {
            while (off < lim) {
              off = decodeHelperBE(wrc0, buf, off);
              off = decodeHelperBE(wrc1, buf, off);
            }
          }
          
          wrc0.release();
          wrc1.release();
          
          packet.addChannelBuffer(buf0);
          packet.addChannelBuffer(buf1);
          
          break;
        }
        
      default:
        throw null;
      }
      
      if (!(off == lim)) throw null;
      
      if (!(format.isSignedComplement())) {
        vectorConstantAddition(packet, C_U2S);
      }
    }
    
    public static int encodeRawBytes(Packet packet, byte[] buf, int off, MixerFormat format)
    {
      if (!(format.isSignedComplement())) {
        vectorConstantAddition(packet, C_S2U);
      }
      
      int rem = packet.getSampleCount();
      
      if (!(format.getSampleSize() == 2)) throw null;
      
      if (!(packet.getChannelCount() == format.getChannelCount())) throw null;
      
      switch (format.getChannelCount()) {
      case 1:
        {
          Buffer.oS rdc0 = packet.getChannelBuffer(0).iterate();
          
          if (format.isLittleEndian()) {
            while (rem-- > 0) {
              off = encodeHelperEL(rdc0, buf, off);
            }
          } else {
            while (rem-- > 0) {
              off = encodeHelperBE(rdc0, buf, off);
            }
          }
          
          rdc0.release();
          
          break;
        }
        
      case 2:
        {
          Buffer.oS rdc0 = packet.getChannelBuffer(0).iterate();
          Buffer.oS rdc1 = packet.getChannelBuffer(1).iterate();
          
          if (format.isLittleEndian()) {
            while (rem-- > 0) {
              off = encodeHelperEL(rdc0, buf, off);
              off = encodeHelperEL(rdc1, buf, off);
            }
          } else {
            while (rem-- > 0) {
              off = encodeHelperBE(rdc0, buf, off);
              off = encodeHelperBE(rdc1, buf, off);
            }
          }
          
          rdc0.release();
          rdc1.release();
          
          break;
        }
        
      default:
        throw null;
      }
      
      if (!(format.isSignedComplement())) {
        vectorConstantAddition(packet, C_U2S);
      }
      
      return off;
    }
  }
  
  public static abstract class MixerTarget
  {
    public static final MixerTarget[] zerol = (new MixerTarget[0]);
    
    public abstract String      getLabel();
    public abstract MixerFormat getDefaultFormat();
    
    abstract AudioSystemMixerTarget getAudioSystemMixerTarget();
  }
  
  /****************************************************************************
   * PACKET MODEL
   ****************************************************************************/
  
  /***
   * A packet is a unit of sound. It may contain an arbitrary number
   * of samples. Each sample is always encoded a signed 16-bit "short"
   * value, and the sample rate must be 48khz. The only allowed
   * variation is in the number of channels, which may be an arbitrary
   * positive integer (though is most normally one or two). All of the
   * channel buffers returned by <code>getChannelBuffer()</code> must
   * have lengths equal to the value returned by
   * <code>getSampleCount()</code>.
   ***/
  public static final class Packet
  {
    public static final int MAXIMUM_CHANNEL_COUNT = 2;
    
    private boolean initialized;
    
    private boolean discontinuity;
    private long micro_time;
    
    private int sample_count;
    private int channel_count;
    
    private final Buffer.xS[] channel_buffers = (new Buffer.xS[MAXIMUM_CHANNEL_COUNT]);
    
    public Packet()
    {
      channel_count = 0;
      reset(true);
    }
    
    public boolean getInitialized()
    {
      return initialized;
    }
    
    public boolean getDiscontinuity()
    {
      return discontinuity;
    }
    
    public long getMicroTime()
    {
      return micro_time;
    }
    
    public int getChannelCount()
    {
      return channel_count;
    }
    
    public int getSampleCount()
    {
      return sample_count;
    }
    
    public Buffer.xS getChannelBuffer(int index)
    {
      return channel_buffers[index];
    }
    
    public void reset(boolean release)
    {
      setDiscontinuity(false);
      setMicroTime(0);
      
      while (this.channel_count > 0) {
        popChannelBuffer(release);
      }
      
      this.initialized = false;
    }
    
    public void copyMetadataFrom(Packet peer)
    {
      this.initialized = peer.initialized;
      this.discontinuity = peer.discontinuity;
      this.micro_time = peer.micro_time;
      this.sample_count = peer.sample_count;
    }
    
    public void setDiscontinuity(boolean discontinuity)
    {
      this.initialized = true;
      
      this.discontinuity = discontinuity;
    }
    
    public void setMicroTime(long micro_time)
    {
      this.initialized = true;
      
      this.micro_time = micro_time;
    }
    
    public void setMicroTimeNow()
    {
      setMicroTime(MicroTime.now());
    }
    
    public void addChannelBuffer(Buffer.xS channel_buffer)
    {
      this.initialized = true;
      
      if (channel_count > 0) {
        if (!(channel_buffer.length() == sample_count)) throw null;
      } else {
        sample_count = channel_buffer.length();
      }
      
      this.channel_buffers[this.channel_count++] = channel_buffer;
    }
    
    public void popChannelBuffer(boolean release)
    {
      this.initialized = true;
      
      if (!(channel_count > 0)) throw null;
      
      this.channel_count--;
      
      if (release) {
        this.channel_buffers[channel_count].release();
      }
      
      this.channel_buffers[channel_count] = null;
      
      if (channel_count == 0) {
        sample_count = 0;
      }
    }
  }
  
  static abstract class SoftFail<T>
  {
    abstract T invoke() throws LineUnavailableException;
  }
  
  /***
   * returns <code>proc.invoke()</code>, or <code>null</code> in case
   * of an audio-related failure.
   ***/
  static <T> T softfail(SoftFail<T> proc)
  {
    final String excuse = "the following exception is being IGNORED, because it is probably just audio system crazyness";
    
    try {
      return proc.invoke();
    } catch (LineUnavailableException e) {
      Log.log(excuse, e);
    } catch (IllegalArgumentException e) {
      Log.log(excuse, e);
    } catch (IllegalStateException e) {
      Log.log(excuse, e);
    } catch (SecurityException e) {
      Log.log(excuse, e);
    }
    
    return null;
  }
  
  static class AudioSystemMixerTarget extends MixerTarget
  {
    public static final AudioSystemMixerTarget[] zerol = (new AudioSystemMixerTarget[0]);
    
    private final String             label;
    private final Mixer.Info         mixer; // null for default mixer
    private final Class<?>      line_class;
    private final cL<MixerFormat>  formats; // formats in fallback order
    
    AudioSystemMixerTarget(Mixer.Info m, Class<?> l, cL<MixerFormat> f)
    {
      label = ((m != null) ? (m.toString()) : ("(default)"));
      mixer = m;
      line_class = l;
      formats = f;
    }
    
    AudioSystemMixerTarget getAudioSystemMixerTarget()
    {
      return this;
    }
    
    public String getLabel()
    {
      return label;
    }
    
    public MixerFormat getDefaultFormat()
    {
      return formats.get(0);
    }
    
    SourceDataLine getSourceDataLine(final AudioFormat format)
    {
      return softfail
        ((new SoftFail<SourceDataLine>()
          {
            public SourceDataLine invoke() throws LineUnavailableException
            {
              return AudioSystem.getSourceDataLine(format, mixer);
            }
          }));
    }
    
    TargetDataLine getTargetDataLine(final AudioFormat format)
    {
      return softfail
        ((new SoftFail<TargetDataLine>()
          {
            public TargetDataLine invoke() throws LineUnavailableException
            {
              return AudioSystem.getTargetDataLine(format, mixer);
            }
          }));
    }
  }
  
  /****************************************************************************
   * FORMAT/MIXER PROBING
   ****************************************************************************/
  
  public static class AudioMixerEnumerator
  {
    public static final int TUNNELLING_LATENCY = 250; /* ms */
    
    public static abstract class ProbeMethod
    {
      abstract boolean workable(Mixer mixer, DataLine.Info linfo);
      
      public static ProbeMethod AccurateProbeMethod =
        (new ProbeMethod()
          {
            boolean workable(final Mixer mixer, final DataLine.Info linfo)
            {
              final Line    line = softfail((new SoftFail<Line>    () { public Line    invoke() throws LineUnavailableException {               return mixer.getLine(linfo); } }));
              final Boolean good = softfail((new SoftFail<Boolean> () { public Boolean invoke() throws LineUnavailableException { line.open();  return true;                 } }));
              /*                */ softfail((new SoftFail<Void>    () { public Void    invoke() throws LineUnavailableException { line.close(); return null;                 } }));
              
              return (good == Boolean.TRUE);
            }
          });
      
      public static ProbeMethod GullibleProbeMethod =
        (new ProbeMethod()
          {
            boolean workable(final Mixer mixer, final DataLine.Info linfo)
            {
              Boolean good =
                (softfail
                 ((new SoftFail<Boolean>()
                   {
                     public Boolean invoke() throws LineUnavailableException
                     {
                       Line.Info[] matching = null;
                       
                       /****/ if (linfo.getLineClass() == TargetDataLine.class) {
                         matching = mixer.getTargetLineInfo(linfo);
                       } else if (linfo.getLineClass() == SourceDataLine.class) {
                         matching = mixer.getSourceLineInfo(linfo);
                       } else {
                         // some other kind of line; leave matching as null
                       }
                       
                       return ((matching != null) && (matching.length > 0));
                     }
                   })));
              
              return (good == Boolean.TRUE);
            }
          });
      
      public static ProbeMethod GuessYesProbeMethod =
        (new ProbeMethod()
          {
            boolean workable(Mixer mixer, DataLine.Info linfo)
            {
              return true; // sure, why not?
            }
          });
    }
    
    static cL<MixerFormat> getSensibleFormats(Class<? extends DataLine> line_class)
    {
      /****/ if (line_class == TargetDataLine.class) {
        return MixerFormat.sensible_reader;
      } else if (line_class == SourceDataLine.class) {
        return MixerFormat.sensible_writer;
      } else {
        throw (new RuntimeException("invalid line class"));
      }
    }
    
    public static MixerTarget getSystemDefaultMixerTarget(Class<? extends DataLine> line_class)
    {
      final cL<MixerFormat> sensible_formats = getSensibleFormats(line_class);
      
      return (new AudioSystemMixerTarget(null, line_class, sensible_formats));
    }
    
    /***
     * this method probes for available mixers; it should generally be
     * called once during application startup. it may return an empty
     * array if mixers cannot be enumerated.
     ***/
    public static cL<MixerTarget> probeMixers(Class<? extends DataLine> line_class, final ProbeMethod probe_method, final int sampleRateHertz)
    {
      final cL<MixerFormat> sensible_formats = getSensibleFormats(line_class);
      
      final ArrayList<MixerTarget> targets = (new ArrayList<MixerTarget>());
      
      try {
        for (Mixer.Info mixer_info : AudioSystem.getMixerInfo()) {
          try {
            Mixer mixer = AudioSystem.getMixer(mixer_info);
            
            for (int pos1 = sensible_formats.off(); pos1 < sensible_formats.lim(); pos1++) {
              try {
                MixerFormat format = sensible_formats.get(pos1);
                
                DataLine.Info linfo = (new DataLine.Info(line_class, format.getAudioFormat(sampleRateHertz)));
                
                if (probe_method.workable(mixer, linfo)) {
                  ArrayList<MixerFormat> fallback_order = (new ArrayList<MixerFormat>());
                  
                  fallback_order.add(format);
                  
                  for (int pos2 = sensible_formats.off(); pos2 < sensible_formats.lim(); pos2++) {
                    MixerFormat backup = sensible_formats.get(pos2);
                    
                    if (backup != format) {
                      fallback_order.add(backup);
                    }
                  }
                  
                  targets.add((new AudioSystemMixerTarget(mixer_info, line_class, cL.newCopyOf(fallback_order.toArray(MixerFormat.zerol)))));
                  
                  break;
                }
              } catch (Throwable e) {
                Log.log(e); // try to continue ...
              }
            }
          } catch (Throwable e) {
            Log.log(e); // try to continue
          }
        }
      } catch (Throwable e) {
        Log.log(e); // try to continue ...
      }
      
      return cL.newCopyOf(targets.toArray(MixerTarget.zerol));
    }
    
    public static AudioMixerEnumeratorAgent launch(final JNylus.Linkage linkage, final AudioMixerEnumeratorYield yield)
    {
      return
        (new AudioMixerEnumeratorAgent(linkage)
          {
            protected void onProbeMixers(Class<? extends DataLine> line_class, ProbeMethod probe_method, int sampleRateHertz)
            {
              yield.doProbeMixers(line_class, probe_method, sampleRateHertz, probeMixers(line_class, probe_method, sampleRateHertz));
            }
            
            protected void onShutdown()
            {
              yield.doShutdown();
              
              linkage.station_callee.setTerminated();
            }
          });
    }
  }
  
  /****************************************************************************
   * LOW-LEVEL AUDIO DRIVERS
   ****************************************************************************/
  
  static class AudioState
  {
    final int SAMPLE_RATE_HZ;
    final int SAMPLE_SIZE_NR;
    
    AudioState(int SAMPLE_RATE_HZ, int SAMPLE_SIZE_NR)
    {
      this.SAMPLE_RATE_HZ = SAMPLE_RATE_HZ;
      this.SAMPLE_SIZE_NR = SAMPLE_SIZE_NR;
    }
    
    static int calculateBufferSize(MixerFormat format, int sample_size_nr)
    {
      return (format.getSampleSize() * format.getChannelCount() * sample_size_nr);
    }
    
    int calculateBufferSize(MixerFormat format)
    {
      return calculateBufferSize(format, SAMPLE_SIZE_NR);
    }
    
    static int calculatePermissibleLatency(int sample_rate_hz, int sample_size_nr)
    {
      return (((1000 * sample_size_nr) / sample_rate_hz) / 4);
    }
    
    int calculatePermissibleLatency()
    {
      return calculatePermissibleLatency(SAMPLE_RATE_HZ, SAMPLE_SIZE_NR);
    }
    
    void closeLine(final DataLine line)
    {
      if (line != null) {
        softfail
          ((new SoftFail<Void>()
            {
              public Void invoke() throws LineUnavailableException
              {
                line.stop();
                return null;
              }
            }));
        
        softfail
          ((new SoftFail<Void>()
            {
              public Void invoke() throws LineUnavailableException
              {
                line.close();
                return null;
              }
            }));
      }
    }
    
    static enum DataLineFlavor
    {
      RECORD
        {
          DataLine getDataLine(AudioSystemMixerTarget target, AudioFormat format) { return target.getTargetDataLine(format); }
          void openLine(DataLine line, AudioFormat format, int buffer_size) throws LineUnavailableException { ((TargetDataLine)(line)).open(format, buffer_size); }
        },
      REPLAY
        {
          DataLine getDataLine(AudioSystemMixerTarget target, AudioFormat format) { return target.getSourceDataLine(format); }
          void openLine(DataLine line, AudioFormat format, int buffer_size) throws LineUnavailableException { ((SourceDataLine)(line)).open(format, buffer_size); }
        };
      
      abstract DataLine getDataLine(AudioSystemMixerTarget target, AudioFormat format);
      abstract void openLine(DataLine line, AudioFormat format, int buffer_size) throws LineUnavailableException;
    };
    
    DataLine reopenLine(final DataLineFlavor flavor, final MixerTarget target, final MixerFormat format)
    {
      final DataLine[] line = (new DataLine[1]);
      
      if ((target != null) && (format != null)) {
        Log.log("will attempt to open and start line");
        
        Boolean success =
          (softfail
           ((new SoftFail<Boolean>()
             {
               public Boolean invoke() throws LineUnavailableException
               {
                 final AudioFormat audio_format = format.getAudioFormat(SAMPLE_RATE_HZ);
                 
                 Log.log("obtaining flavor '" + flavor.name() + "' line for '" + target.getLabel() + "'='" + target.getAudioSystemMixerTarget().mixer + "'/'" + format.getLabel() + "'='" + audio_format + "'");
                 
                 line[0] = flavor.getDataLine(target.getAudioSystemMixerTarget(), audio_format);
                 
                 if (line[0] == null) {
                   Log.log("could not obtain line");
                   return null;
                 } else {
                   Log.log("obtained line '" + line[0].toString() + "'");
                 }
                 
                 Log.log("opening line");
                 int requested_buffer_size = calculateBufferSize(format);
                 flavor.openLine(line[0], audio_format, requested_buffer_size);
                 Log.log("opened line (buffer size " + line[0].getBufferSize() + " / " + requested_buffer_size + ")");
                 
                 Log.log("starting line");
                 line[0].start();
                 Log.log("started line");
                 
                 return Boolean.TRUE;
               }
             })));
        
        if (success == Boolean.TRUE) {
          return line[0];
        } else {
          Log.log("failed to open and start line");
          closeLine(line[0]);
          return null;
        }
      } else {
        return null;
      }
    }
  }
  
  /***
   * AudioRecord is the audio record driver. It basically reads
   * samples from a <code>TargetDataLine</code>. It supports changing
   * the underlying target line on the fly. It does not perform format
   * conversion.
   ***/
  public static final class AudioRecord
  {
    public static int calculateBufferSize(MixerFormat format)
    {
      return AudioState.calculateBufferSize(format, SAMPLE_SIZE_RECORD_NR);
    }
    
    static final class State extends AudioState
    {
      /***
       * the mixer target that is currently being read from, or
       * <code>null</code> if the audio reader is off.
       ***/
      MixerTarget target = null;
      
      /***
       * the format that is currently being read in, or
       * <code>null</code> if the audio reader is off.
       ***/
      MixerFormat format = null;
      
      /***
       * the <code>TargetDataLine</code> currently being read from, or
       * <code>null</code> if the audio reader is off.
       ***/
      TargetDataLine line = null;
      
      State()
      {
        super(SAMPLE_RATE_RECORD_HZ, SAMPLE_SIZE_RECORD_NR);
      }
      
      void closeLine()
      {
        closeLine(line);
        line = null;
      }
      
      void reopenLine()
      {
        closeLine();
        line = ((TargetDataLine)(reopenLine(DataLineFlavor.RECORD, target, format)));
      }
    }
    
    public static AudioRecordAgent launch(final JNylus.Linkage linkage, final AudioRecordYield yield)
    {
      final State state = (new State());
      
      return
        (new AudioRecordAgent(linkage)
          {
            protected void onInitialize(MixerTarget desired_target, MixerFormat desired_format)
            {
              if ((state.target != desired_target) || (state.format != desired_format)) {
                state.target = desired_target;
                state.format = desired_format;
                
                state.reopenLine();
              }
              
              yield.doInitialize(desired_target, desired_format);
            }
            
            protected void onShutdown()
            {
              state.target = null;
              state.format = null;
              
              state.reopenLine();
              
              yield.doShutdown();
              
              linkage.station_callee.setTerminated();
            }
            
            protected void onGetPacket(byte[] buffer)
            {
              // lapse tracking
              long prior = 0;
              long total = 0;
              long denom = 0;
              long worst = 0;
              
              boolean success = false;
              
              final long enter_us;
              final long leave_us;
              
              enter_us = MicroTime.now();
              
              if (state.line != null) {
                // lapse tracking
                {
                  if (prior != 0) {
                    long lapse = (enter_us - prior);
                    
                    total += lapse;
                    denom += 1;
                    
                    if (denom > 10) worst = Math.max(worst, lapse);
                    
                    record_lapse_avg = (((double)(total)) / ((double)(denom)));
                    record_lapse_max = ((double)(worst));
                  }
                }
                
                int amt = state.line.read(buffer, 0, buffer.length);
                success = (amt == buffer.length);
                
                leave_us = MicroTime.now();
                
                // lapse tracking
                {
                  prior = leave_us;
                }
              } else {
                leave_us = enter_us;
              }
              
              yield.doGetPacket(buffer, success, leave_us, state.format);
            }
          });
    }
  }
  
  /***
   * AudioReplay is the audio playback driver. It basically writes
   * samples to a <code>SourceDataLine</code> and performs format
   * conversion. It supports changing the underlying target line on
   * the fly. It does not perform format conversion. It does not
   * handle signal discontinuity.
   ***/
  public static class AudioReplay
  {
    public static int calculateBufferSize(MixerFormat format)
    {
      return AudioState.calculateBufferSize(format, SAMPLE_SIZE_REPLAY_NR);
    }
    
    static final class State extends AudioState
    {
      /***
       * the mixer target that is currently being written to, or
       * <code>null</code> if the audio writer is off.
       ***/
      MixerTarget target = null;
      
      /***
       * the format that is currently being written in, or
       * <code>null</code> if the audio writer is off.
       ***/
      MixerFormat format = null;
      
      /***
       * the <code>SourceDataLine</code> currently being written to,
       * or <code>null</code> if the audio writer is off.
       ***/
      SourceDataLine line = null;
      
      State()
      {
        super(SAMPLE_RATE_REPLAY_HZ, SAMPLE_SIZE_REPLAY_NR);
      }
      
      void closeLine()
      {
        closeLine(line);
        line = null;
      }
      
      void reopenLine()
      {
        closeLine();
        line = ((SourceDataLine)(reopenLine(DataLineFlavor.REPLAY, target, format)));
      }
    }
    
    public static AudioReplayAgent launch(final JNylus.Linkage linkage, final AudioReplayYield yield)
    {
      final State state = (new State());
      
      return
        (new AudioReplayAgent(linkage)
          {
            protected void onInitialize(MixerTarget desired_target, MixerFormat desired_format)
            {
              if ((state.target != desired_target) || (state.format != desired_format)) {
                state.target = desired_target;
                state.format = desired_format;
                
                state.reopenLine();
              }
              
              yield.doInitialize(desired_target, desired_format);
            }
            
            protected void onShutdown()
            {
              state.target = null;
              state.format = null;
              
              state.reopenLine();
              
              yield.doShutdown();
              
              linkage.station_callee.setTerminated();
            }
            
            // lapse tracking
            long prior = 0;
            long total = 0;
            long denom = 0;
            long worst = 0;
            
            protected void onPutPacket(byte[] buffer, long expected_duration_us, long packet_timestamp_us, Object cookie)
            {
              boolean success = false;
              
              long enter_us;
              long leave_us;
              
              enter_us = MicroTime.now();
              
              // lapse tracking
              {
                if (prior != 0) {
                  long lapse = (enter_us - prior);
                  
                  total += lapse;
                  denom += 1;
                  
                  if (denom > 10) worst = Math.max(worst, lapse);
                  
                  replay_lapse_avg = (((double)(total)) / ((double)(denom)));
                  replay_lapse_max = ((double)(worst));
                  //Log.log("lapse " + lapse + " (avg " + (((double)(total)) / ((double)(denom))) + ", max " + worst + ")");
                }
              }
              
              {
                if (state.line != null) {
                  int amt = state.line.write(buffer, 0, buffer.length);
                  success = (amt == buffer.length);
                }
              }
              
              leave_us = MicroTime.now();
              
              // napping
              {
                final long observed_duration_us = (leave_us - prior);
                
                if ((0 <= observed_duration_us) && (observed_duration_us < expected_duration_us)) {
                  try {
                    long nap_ms = ((expected_duration_us - observed_duration_us) / 1000);
                    Thread.sleep(nap_ms);
                  } catch (InterruptedException e) {
                    Log.log(e);
                  }
                  
                  leave_us = MicroTime.now();
                }
              }
              
              // lapse tracking
              {
                prior = leave_us;
              }
              
              yield.doPutPacket(buffer, expected_duration_us, packet_timestamp_us, cookie, success, leave_us);
            }
          });
    }
  }
  
  /****************************************************************************
   * EFFECTS
   ****************************************************************************/
  
  public static class Effects
  {
    static double D(int x)
    {
      return ((double)(x));
    }
    
    static double nsinc(double x)
    {
      if ((-0.000001 < x) && (x < +0.000001)) {
        return 1;
      } else {
        double px = (Math.PI * x);
        return (Math.sin(px) / px);
      }
    }
    
    public static abstract class Filter
    {
      protected final BufferCentral central;
      
      boolean primed = false;
      
      public Filter(BufferCentral central)
      {
        this.central = central;
      }
      
      /***
       * resets the filter state. the filter should subseqently
       * operate as if there were no preceeding calls to
       * <code>filter(...)</code>. this method should be invoked when
       * the signal is discontinuous (i.e., forward/backward
       * jumps). it is <i>required</i> that this method be invoked
       * before the first call to <code>filter(...)</code>.
       ***/
      public void reset()
      {
        resetExtendsFilter();
        primed = true;
      }
      
      protected abstract void resetExtendsFilter();
      
      /***
       * applies the operation of the filter to the samples contained
       * in the given input buffer, which is then
       * released. frequently, the number of samples is not
       * conserved. many filters require a window. in the interest of
       * maximum signal quality, such filters will cache a number of
       * samples from invocation to invocation which means that the
       * signal near the end of the output buffer may not be
       * representative of the singal near the end of the input
       * buffer. instead, the beginning of the next output buffer will
       * be representative of the signal near the end of the previous
       * input buffer. filters that change the audio speed may remove
       * and insert samples. note that filters operate on floats which
       * are to contain signed samples generally not exceeding the
       * magnitude of signed "short" values. application of filters
       * may generate slight overflow conditions, which are later
       * resolved by clamping.
       ***/
      public Buffer.xF filter(Buffer.xF inp)
      {
        if (!(primed)) throw null;
        
        Buffer.xF out_x = central.acquireF();
        Buffer.nF out_n = out_x.prepend();
        
        Buffer.oF inp_o = inp.iterate();
        
        {
          filterExtendsFilter(out_n, inp_o);
        }
        
        inp_o.release();
        out_n.release();
        
        inp.release();
        
        return out_x;
      }
      
      protected abstract void filterExtendsFilter(Buffer.nF out, Buffer.oF inp);
    }
    
    public static final class IdentityFilter extends Filter
    {
      public IdentityFilter(BufferCentral central)
      {
        super(central);
      }
      
      protected void resetExtendsFilter()
      {
        // nothing to do
      }
      
      protected void filterExtendsFilter(Buffer.nF out, Buffer.oF inp)
      {
        int rem = inp.remaining();
        
        while (rem-- > 0) {
          out.aF(inp.rF());
        }
      }
    }
    
    public static final class ChainFilter extends Filter
    {
      final Filter[] filters;
      
      public ChainFilter(BufferCentral central, final Filter... filters)
      {
        super(central);
        this.filters = filters.clone();
      }
      
      protected final void resetExtendsFilter()
      {
        for (Filter filter : filters) {
          filter.reset();
        }
      }
      
      protected final void filterExtendsFilter(Buffer.nF out, Buffer.oF inp)
      {
        {
          Buffer.xF tmp_src = central.acquireF();
          {
            // transfer input into tmp_src
            {
              Buffer.nF tmp_src_n = tmp_src.prepend();
              {
                Buffer.sF.copy(tmp_src_n, inp, inp.remaining());
              }
              tmp_src_n.release();
            }
            
            // apply effects, each time leaving result in tmp_src
            {
              for (Filter filter : filters) {
                tmp_src = filter.filter(tmp_src);
              }
            }
            
            // transfer tmp_src into output
            {
              Buffer.oF tmp_src_o = tmp_src.iterate();
              Buffer.sF.copy(out, tmp_src_o, tmp_src_o.remaining());
              tmp_src_o.release();
            }
          }
          tmp_src.release();
        }
      }
    }
    
    public static abstract class AbstractVolumeFilter extends Filter
    {
      public AbstractVolumeFilter(BufferCentral central)
      {
        super(central);
      }
      
      protected final void resetExtendsFilter()
      {
        // nothing to do
      }
      
      protected final void filterExtendsFilter(Buffer.nF out, Buffer.oF inp)
      {
        double multiplier = determineMultiplier();
        
        int rem = inp.remaining();
        
        if (!((0.99 <= multiplier) && (multiplier < 1.01))) {
          while (rem-- > 0) {
            out.aF(((float)(inp.rF() * multiplier)));
          }
        } else {
          while (rem-- > 0) {
            out.aF(inp.rF());
          }
        }
      }
      
      protected abstract double determineMultiplier();
    }
    
    public static final class VolumeFilter extends AbstractVolumeFilter
    {
      final double multiplier;
      
      public VolumeFilter(BufferCentral central, double multiplier)
      {
        super(central);
        this.multiplier = multiplier;
      }
      
      protected double determineMultiplier()
      {
        return multiplier;
      }
    }
    
    public static final class AdjustableVolumeFilter extends AbstractVolumeFilter
    {
      volatile double multiplier;
      
      public AdjustableVolumeFilter(BufferCentral central, double multiplier)
      {
        super(central);
        this.multiplier = multiplier;
      }
      
      public void setMultiplier(double multiplier)
      {
        this.multiplier = multiplier;
      }
      
      protected double determineMultiplier()
      {
        return multiplier;
      }
    }
    
    public static class UpscaleFilter extends Filter
    {
      /***
       * the window size.
       ***/
      final int anchor;
      
      /***
       * the scale-up factor. usually 8, for the 8khz -&gt; 48khz
       * conversion.
       ***/
      final int factor;
      
      /***
       * window buffer. the central window sample is always at index
       * <code>(anchor/2)</code>. it would not make sense to use a
       * circular buffer since a linear pass over the window is
       * required anyways for each output sample.
       ***/
      final float[] window;
      
      /***
       * coefficient matrix.
       ***/
      final float[][] coefficients;
      
      public UpscaleFilter(BufferCentral central, int anchor, int factor, float[][] coefficients)
      {
        super(central);
        
        if (!((anchor & 1) == 0)) throw null;
        if (!(factor > 1)) throw null;
        
        this.anchor = anchor;
        this.factor = factor;
        
        this.window = (new float[anchor]);
        this.coefficients = coefficients;
        
        resetExtendsFilter();
      }
      
      protected final void resetExtendsFilter()
      {
        for (int i = 0; i < window.length; i++) {
          window[i] = 0;
        }
      }
      
      protected final void filterExtendsFilter(Buffer.nF out, Buffer.oF inp)
      {
        int rem = inp.remaining();
        
        while (rem-- > 0) {
          for (int src = 1; src < anchor; src++) {
            window[src - 1] = window[src];
          }
          
          window[(anchor - 1)] = inp.rF();
          
          /***
           * offset - how far past the central window sample the next
           * output sample is. sensible values range from 0 through
           * factor-1, inclusive.
           ***/
          for (int offset = 0; offset < factor; offset++) {
            float sum = 0;
            
            for (int slider = 0; slider < anchor; slider++) {
              sum += coefficients[offset][slider] * window[slider];
            }
            
            out.aF(sum);
          }
        }
      }
    }
    
    public static final class LanczosUpscaleFilter extends UpscaleFilter
    {
      static double determine_weight(int anchor, double distance)
      {
        return nsinc(distance) * nsinc((distance / D(anchor / 2)));
      }
      
      static float[][] determine_coefficients(int anchor, int factor)
      {
        if (!((anchor > 1) && (factor > 1))) throw null;
        if (!((anchor & 1) == 0)) throw null;
        
        float[][] coefficients = (new float[factor][anchor]);
        
        for (int offset = 0; offset < factor; offset++) {
          for (int slider = 0; slider < anchor; slider++) {
            int leader = (anchor / 2) - 1;
            
            double coordinate = (D(leader) + (D(offset) / D(factor)));
            double distance = Math.abs(coordinate - D(slider));
            
            //Log.log("distance[offset=" + offset + "][slider=" + slider + "] = " + distance);
            
            coefficients[offset][slider] = ((float)(determine_weight(anchor, distance)));
            
            //Log.log("coefficients[offset=" + offset + "][slider=" + slider + "] = " + coefficients[offset][slider]);
          }
        }
        
        return coefficients;
      }
      
      public LanczosUpscaleFilter(BufferCentral central, int anchor, int factor)
      {
        super(central, anchor, factor, determine_coefficients(anchor, factor));
      }
    }
    
    /***
     * applies the effects of the given filter to
     * <code>packet</code>, which must contain a single channel.
     ***/
    public static void filter(BufferCentral central, Filter filter, Audio2g.Packet packet)
    {
      if (!(packet.getChannelCount() == 1)) throw null;
      
      if (packet.getDiscontinuity()) filter.reset();
      
      Buffer.xF samples = central.acquireF();
      {
        long enter_load = System.nanoTime();
        
        // fill samples from packet channel
        {
          Buffer.nF samples_n = samples.prepend();
          Buffer.oS channel_o = packet.getChannelBuffer(0).iterate();
          {
            int rem = channel_o.remaining();
            
            while (rem-- > 0) {
              samples_n.aF(((float)(channel_o.rS())));
            }
          }
          channel_o.release();
          samples_n.release();
        }
        
        long leave_load = System.nanoTime();
        
        // release packet channel
        packet.popChannelBuffer(true);
        
        long enter_main = System.nanoTime();
        
        // apply filter to samples
        samples = filter.filter(samples);
        
        long leave_main = System.nanoTime();
        
        long enter_save = System.nanoTime();
        
        // fill packet channel from samples
        {
          Buffer.xS channel = central.acquireS();
          {
            Buffer.nS channel_n = channel.prepend();
            Buffer.oF samples_o = samples.iterate();
            {
              int rem = samples_o.remaining();
              
              int clipped = 0;
              
              while (rem-- > 0) {
                int value = Math.round(samples_o.rF());
                
                if (value < Short.MIN_VALUE) {
                  value = Short.MIN_VALUE;
                  clipped++;
                }
                
                if (value > Short.MAX_VALUE) {
                  value = Short.MAX_VALUE;
                  clipped++;
                }
                
                channel_n.aS(((short)(value)));
              }
              
              //Log.log("clipped=" + clipped);
            }
            samples_o.release();
            channel_n.release();
          }
          packet.addChannelBuffer(channel);
        }
        
        long leave_save = System.nanoTime();
        
        if (false) {
          Log.log("load: " + (leave_load - enter_load) + ", main: " + (leave_main - enter_main) + ", save: " + (leave_save - enter_save));
        }
      }
      samples.release();
    }
  }
}

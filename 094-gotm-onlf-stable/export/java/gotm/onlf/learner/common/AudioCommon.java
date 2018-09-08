/***
 * AudioCommon.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.common;

import java.util.*;
import javax.sound.sampled.*;

import zs42.mass.*;

import gotm.onlf.learner.common.AudioCommonTables;

import static gotm.onlf.learner.common.Constants.AudioCommon.Format.*;
import static gotm.onlf.utilities.Utilities.*;

public class AudioCommon
{
  /***
   * methods of the following class may be invoked in the context of
   * any thread.
   ***/
  public static abstract class Target
  {
    public abstract String getLabel();
    public abstract Format getDefaultFormat();
  }
  
  public static enum Format
  {
    F_1_U_EL
      {
        public String getLabel() { return "1/U/EL"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 1, false, false)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, false, false, true); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, false, false, true); }
      },
    
    F_1_U_BE
      {
        public String getLabel() { return "1/U/BE"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 1, false, true)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, false, false, false); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, false, false, false); }
      },
    
    F_1_S_EL
      {
        public String getLabel() { return "1/S/EL"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 1, true, false)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, false, true, true); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, false, true, true); }
      },
    
    F_1_S_BE
      {
        public String getLabel() { return "1/S/BE"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 1, true, true)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, false, true, false); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, false, true, false); }
      },
    
    F_2_U_EL
      {
        public String getLabel() { return "2/U/EL"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 2, false, false)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, true, false, true); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, true, false, true); }
      },
    
    F_2_U_BE
      {
        public String getLabel() { return "2/U/BE"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 2, false, true)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, true, false, false); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, true, false, false); }
      },
    
    F_2_S_EL
      {
        public String getLabel() { return "2/S/EL"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 2, true, false)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, true, true, true); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, true, true, true); }
      },
    
    F_2_S_BE
      {
        public String getLabel() { return "2/S/BE"; }
        public AudioFormat getAudioFormat() { return (new AudioFormat(rate, 16, 2,  true,  true)); }
        public byte[] adaptSignedPCM(short[] signedPCM) { return adaptSignedPCM(signedPCM, true, true, false); }
        public short[] adaptArbitraryPCM(byte[] arbitraryPCM) { return adaptArbitraryPCM(arbitraryPCM, true, true, false); }
      };
    
    public static /* final (!!!) */ float rate = RATE;
    
    public static final Format[] sensible_reader =
      {
        F_1_S_EL, F_1_S_BE, F_1_U_EL, F_1_U_BE,
        F_2_S_EL, F_2_S_BE, F_2_U_EL, F_2_U_BE
      };
    
    public static final Format[] sensible_writer =
      {
        F_2_S_EL, F_2_S_BE, F_2_U_EL, F_2_U_BE,
        F_1_S_EL, F_1_S_BE, F_1_U_EL, F_1_U_BE
      };
    
    public abstract String getLabel();
    public abstract AudioFormat getAudioFormat();
    public abstract byte[] adaptSignedPCM(short[] signedPCM);
    public abstract short[] adaptArbitraryPCM(byte[] arbitraryPCM);
    
    static byte[] adaptSignedPCM(short[] signedPCM, boolean stereo, boolean signed, boolean little)
    {
      int shla = stereo ? 2 : 1;
      int ctrp = signed ? 0 : ((1 << 16) - 1);
      int shr0 = little ? 0 : 8;
      int shr1 = little ? 8 : 0;
      
      byte[] out = new byte[(signedPCM.length << 1) << (stereo ? 1 : 0)];
      
      for (int i = 0; i < signedPCM.length; i++) {
        out[(i << shla)    ] = ((byte)((ctrp + signedPCM[i]) >> shr0));
        out[(i << shla) + 1] = ((byte)((ctrp + signedPCM[i]) >> shr1));
      }
      
      if (stereo) {
        for (int i = 0; i < signedPCM.length; i++) {
          out[(i << shla) + 2] = out[(i << shla)    ];
          out[(i << shla) + 3] = out[(i << shla) + 1];
        }
      }
      
      return out;
    }
    
    static short[] adaptArbitraryPCM(byte[] arbitraryPCM, boolean stereo, boolean signed, boolean little)
    {
      int divy = stereo ? 2 : 1;
      int ctrp = signed ? 0 : ((1 << 16) - 1);
      int shl0 = little ? 0 : 8;
      int shl1 = little ? 8 : 0;
      
      short[] out = new short[arbitraryPCM.length >> divy];
      
      for (int i = 0; i < out.length; i++) {
        out[i] =
          ((short)
           (((arbitraryPCM[(i << divy)    ] & 0xFF) << shl0) +
            ((arbitraryPCM[(i << divy) + 1] & 0xFF) << shl1) +
            (- ctrp)));
      }
      
      return out;
    }
  }
  
  public static byte[] enlaw(short[] inp, int off, int lim)
  {
    byte[] out = new byte[lim - off];
    
    for (int i = 0; i < out.length; i++) {
      out[i] = AudioCommonTables.enlaw[inp[off + i] & 0xFFFF];
    }
    
    return out;
  }
  
  public static short[] unlaw(byte[] inp, int off, int lim)
  {
    short[] out = new short[lim - off];
    
    for (int i = 0; i < out.length; i++) {
      out[i] = AudioCommonTables.unlaw[inp[off + i] & 0xFF];
    }
    
    return out;
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
      log("the following exception is being IGNORED", e);
    } catch (IllegalArgumentException e) {
      log("the following exception is being IGNORED", e);
    } catch (IllegalStateException e) {
      log("the following exception is being IGNORED", e);
    } catch (SecurityException e) {
      log("the following exception is being IGNORED", e);
    }
    
    return null;
  }
  
  public static class MixerTarget extends Target
  {
    String        label;
    Mixer.Info    mixer; // null for default mixer
    Class<?> line_class;
    Format[]    formats; // formats in fallback order
    
    MixerTarget(Mixer.Info m, Class<?> l, Format[] f)
    {
      label = ((m != null) ? (m.toString()) : ("(default)"));
      mixer = m;
      line_class = l;
      formats = f;
    }
    
    public String getLabel()
    {
      return label;
    }
    
    public Format getDefaultFormat()
    {
      return formats[0];
    }
    
    SourceDataLine getSourceDataLine(final Format format)
    {
      return softfail
        ((new SoftFail<SourceDataLine>()
          {
            public SourceDataLine invoke() throws LineUnavailableException
            {
              return AudioSystem.getSourceDataLine(format.getAudioFormat(), mixer);
            }
          }));
    }
    
    TargetDataLine getTargetDataLine(final Format format)
    {
      return softfail
        ((new SoftFail<TargetDataLine>()
          {
            public TargetDataLine invoke() throws LineUnavailableException
            {
              return AudioSystem.getTargetDataLine(format.getAudioFormat(), mixer);
            }
          }));
    }
  }
  
  static abstract class ProbeMethod
  {
    abstract boolean workable(Mixer mixer, DataLine.Info linfo);
  }
  
  static class AccurateProbeMethod extends ProbeMethod
  {
    boolean workable(Mixer mixer, DataLine.Info linfo)
    {
      try {
        Line line = mixer.getLine(linfo);
        line.open();
        line.close();
        return true;
      } catch (LineUnavailableException e) {
        // ignored
      } catch (IllegalArgumentException e) {
        // ignored
      } catch (SecurityException e) {
        // ignored
      }
      
      return false;
    }
  }
  
  static class GullibleProbeMethod extends ProbeMethod
  {
    boolean workable(Mixer mixer, DataLine.Info linfo)
    {
      try {
        Line.Info[] matching;
        
        /****/ if (linfo.getLineClass() == TargetDataLine.class) {
          matching = mixer.getTargetLineInfo(linfo);
        } else if (linfo.getLineClass() == SourceDataLine.class) {
          matching = mixer.getSourceLineInfo(linfo);
        } else {
          throw null;
        }
        
        return ((matching != null) && (matching.length > 0));
      } catch (IllegalArgumentException e) {
        // ignored
      } catch (SecurityException e) {
        // ignored
      }
      
      return false;
    }
  }
  
  private static final boolean use_probing = false;
  private static final ProbeMethod probe_method = (new GullibleProbeMethod());
  
  /***
   * this method probes for available mixers; it should generally be
   * called once during application startup. this method always
   * returns an array containing at least one entry (for the system
   * default mixer). furthermore, the entry for the system default
   * mixer is always the first entry.
   ***/
  public static MixerTarget[] detect_mixers(Class<?> line_class)
  {
    Format[] sensible_formats;
    
    /****/ if (line_class == TargetDataLine.class) {
      sensible_formats = Format.sensible_reader;
    } else if (line_class == SourceDataLine.class) {
      sensible_formats = Format.sensible_writer;
    } else {
      throw (new BAD("invalid line class"));
    }
    
    ArrayList<MixerTarget> targets = (new ArrayList<MixerTarget>());
    
    targets.add((new MixerTarget(null, line_class, sensible_formats)));
    
    if (use_probing) {
      for (Mixer.Info mixer_info : AudioSystem.getMixerInfo()) {
        Mixer mixer = AudioSystem.getMixer(mixer_info);
        
        for (Format format : sensible_formats) {
          DataLine.Info linfo = (new DataLine.Info(line_class, format.getAudioFormat()));
          
          if (probe_method.workable(mixer, linfo)) {
            ArrayList<Format> fallback_order = (new ArrayList<Format>());
            fallback_order.add(format);
            fallback_order.addAll(Arrays.asList(sensible_formats));
            
            targets.add((new MixerTarget(mixer_info, line_class, fallback_order.toArray((new Format[0])))));
            break;
          }
        }
        
        mixer.close();
      }
    } else {
      for (Mixer.Info mixer_info : AudioSystem.getMixerInfo()) {
        targets.add((new MixerTarget(mixer_info, line_class, sensible_formats)));
      }
    }
    
    return ((MixerTarget[])(targets.toArray(new MixerTarget[0])));
  }
}

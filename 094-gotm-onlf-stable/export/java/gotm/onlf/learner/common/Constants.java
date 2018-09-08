/***
 * Constants.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.learner.common;

public class Constants
{
  public static class SplitterClient
  {
    public static final int CONNECTION_TIMEOUT_MS = 5000;
  }
  
  public static class AudioCommon
  {
    public static class Format
    {
      public static final int SAMPLES_PER_MS = 48;
      public static final float RATE = ((float)(SAMPLES_PER_MS * 1000));
    }
  }
  
  public static class UserInterface
  {
    public static final int LOG_TILE_SIZE = 4;  // 16x16 pixel tiles for the remote desktop
    public static final int HALF_TILE_BUFFER_SIZE = 2;  // 5x5 tile buffer
    public static final int TILE_SIZE = (1 << 4);
    public static final int TILE_BUFFER_SIZE = (2 * HALF_TILE_BUFFER_SIZE + 1);
    public static final int TILE_BUFFER_PIXEL_COUNT = (TILE_BUFFER_SIZE * TILE_BUFFER_SIZE * TILE_SIZE * TILE_SIZE);
    public static final int MAXIMUM_UPDATE_STALL_MS = 200;  // point five seconds
  }
  
  public static class GrandUnifiedInterconnect
  {
    public static final int  INSTANCE_ID_HEX_DIGIT_COUNT = ((Long.SIZE /* bits */) / 4);
    public static final int  SAMPLES_PER_MS = AudioCommon.Format.SAMPLES_PER_MS;
    public static final int  CLIENT_PACKET_SAMPLE_COUNT = 2048;
    public static final int  LOG_CLIENT_PACKET_SAMPLE_COUNT = 11;
    public static final long US_PER_PACKET = CLIENT_PACKET_SAMPLE_COUNT * 1000 / SAMPLES_PER_MS;
    public static final int  RAMP_SAMPLE_COUNT = ((CLIENT_PACKET_SAMPLE_COUNT * 7) / 8);
    public static final int  EVENT_QUEUE_BACKLOG = 64;
    public static final int  MAXIMUM_IMMEDIATE_FRAME_COMPLEXITY = UserInterface.TILE_BUFFER_PIXEL_COUNT;
    
    /***
     * dynamic note: on a connection that periodically hangs for a
     * short time, things will be OK if that time is less than
     * MAX-TRC. however, if the time is more than MAX-TRC, catch-up
     * will be triggered which sets the system up for another
     * interruption at the next hang.
     * 
     * optimistic settings:   (1.0 sec, 2.0 sec, 4.0 sec) = ( 4,  8, 16)
     * conservative settings: (2.0 sec, 4.0 sec, 8.0 sec) = ( 8, 16, 32)
     * 
     * since audio settings can now be adjusted at runtime, things run
     * optimistic by default.
     ***/
    
    public static final boolean DEFAULT_AUDIO_CONTENT_SENSITIVE_TRUNCATION = false;
    
    public static final int DEFAULT_AUDIO_RESERVE_QUEUE_MIN = 1*4; // ~1 seconds (can't start with less than this buffered)
    public static final int DEFAULT_AUDIO_RESERVE_QUEUE_TRC = 2*4; // ~2 seconds (truncate to this after exceeding max)
    public static final int DEFAULT_AUDIO_RESERVE_QUEUE_MAX = 4*4; // ~4 seconds (must catch up if more than this buffered)
    
    public static volatile boolean AUDIO_CONTENT_SENSITIVE_TRUNCATION = DEFAULT_AUDIO_CONTENT_SENSITIVE_TRUNCATION;
    
    public static volatile int AUDIO_RESERVE_QUEUE_MIN = DEFAULT_AUDIO_RESERVE_QUEUE_MIN; // can't start with less than this buffered
    public static volatile int AUDIO_RESERVE_QUEUE_TRC = DEFAULT_AUDIO_RESERVE_QUEUE_TRC; // truncate to this after exceeding max
    public static volatile int AUDIO_RESERVE_QUEUE_MAX = DEFAULT_AUDIO_RESERVE_QUEUE_MAX; // must catch up if more than this buffered
    
    public static final int AUDIO_FEATHER_WIN = 33; // size of the feathering window
    public static final int AUDIO_SPE_WIN_HSZ = 16; // window size for the computation of the signal power estimate. (2 * hsz + 1) must be <= feather win
    
    public static final int AUDIO_STATIC_AMPLOG = 10;
    
    public static final int SHUTDOWN_NAP_MS = 100;
    
    public static final long   AV_SYNC_IDEAL_ELAPSED_US = 1000 * (CLIENT_PACKET_SAMPLE_COUNT / SAMPLES_PER_MS);
    public static final long   AV_SYNC_SMOOTHING_MAX_DELTA_US = 2 * AV_SYNC_IDEAL_ELAPSED_US;
    public static final double AV_SYNC_SMOOTHING_ALPHA = 0.1;
    public static final long   MAXIMUM_VIDEO_LAPSE_US = 250000;
    
    public static final long ETCH_QUEUE_AHEAD_US = 500000;
    public static final long ETCH_EVENT_DELAY_US = 375000;
  }
}

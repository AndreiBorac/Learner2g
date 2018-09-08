/***
 * Constants.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.common;

import gotm.onlf.utilities.Utilities;

public class Constants
{
  public static final int TRANSPORT_BUFFER_SIZE = 65536;
  public static final int SESSION_PACKETS_MAXIMUM = (1 << 20);
  public static final int STREAM_STOP = 64;
  public static final int MAXIMUM_FEEDBACK_PACKET_LENGTH = 512;
  public static final int MAXIMUM_FEEDBACK_OUTPUT_LENGTH = (8 << 20);
  
  public static final int PACKET_METADATA_SOURCE_TC_HI  = 0;
  public static final int PACKET_METADATA_SOURCE_TC_LO  = 1;
  public static final int PACKET_METADATA_STREAM_ID     = 2;
  public static final int PACKET_METADATA_COMPLEXITY    = 3;
  public static final int PACKET_METADATA_ENCODED       = 3; // OVERLAPPED
  public static final int PACKET_METADATA_LENGTH        = 4;
  
  public static final int PACKET_METADATA_STREAM_ID_COMMAND       = 0; // [  1]
  public static final int PACKET_METADATA_STREAM_ID_AUDIO         = 1; // [  2]
  public static final int PACKET_METADATA_STREAM_ID_NWED          = 6; // [ 64] network editor = variable low-ish bitrate
  public static final int PACKET_METADATA_STREAM_ID_ETCH_JARFILE  = 7; // [128] network visiplate backdrops jarfile
  public static final int PACKET_METADATA_STREAM_ID_ETCH_EVENTS   = 8; // [256] network visiplate events
  public static final int PACKET_METADATA_STREAM_ID_FROB_EVENTS   = 9; // [???] frob events
  public static final int PACKET_METADATA_STREAM_ID_VIDEO_4       = 2; // [ 4] [455]   8 bytes/ms = 7.813 kBps = 0.061 mbit/sec
  public static final int PACKET_METADATA_STREAM_ID_VIDEO_5       = 3; // [ 8] [459]  24 bytes/ms = 23.44 kBps = 0.183 mbit/sec
  public static final int PACKET_METADATA_STREAM_ID_VIDEO_6       = 4; // [16] [467]  56 bytes/ms = 54.69 kBps = 0.427 mbit/sec
  public static final int PACKET_METADATA_STREAM_ID_VIDEO_7       = 5; // [32] [483] 120 bytes/ms = 117.2 kBps = 0.916 mbit/sec
  
  // return stream tokens
  public static final byte RET_RESERVED         = ((byte)(0)); // reserved by splitter (for matching instances to IP addresses)
  public static final byte RET_FEEDBACK         = ((byte)(1)); // user's feedback (bytes... text)
  public static final byte RET_COMMAND_REPLY    = ((byte)(2)); // administrator command reply (bytes... text)
  public static final byte RET_PACKET_RECEIVED  = ((byte)(3)); // packet received notification (int stream_id, int source_tc, int client_tc)
  public static final byte RET_QUEUE_SIZES      = ((byte)(4)); // audio reserve queue size (int size), video queue size (int size)
  
  public static void encodeProperSourceTimecode(int[] metadata, long now)
  {
    metadata[PACKET_METADATA_SOURCE_TC_HI] = ((int)(now >> 32));
    metadata[PACKET_METADATA_SOURCE_TC_LO] = ((int)(now      ));
  }
  
  public static void encodeProperSourceTimecode(int[] metadata)
  {
    encodeProperSourceTimecode(metadata, Utilities.microTime());
  }
  
  public static void encodeProperSourceTimecode(GroupirPacket groupir, long now)
  {
    encodeProperSourceTimecode(groupir.dI, now);
  }
  
  public static void encodeProperSourceTimecode(GroupirPacket groupir)
  {
    encodeProperSourceTimecode(groupir.dI);
  }
  
  public static long decodeProperSourceTimecode(GroupirPacket groupir)
  {
    final int source_tc_hi = groupir.dI[PACKET_METADATA_SOURCE_TC_HI];
    final int source_tc_lo = groupir.dI[PACKET_METADATA_SOURCE_TC_LO];
    
    long proper_source_tc = ((((long)(source_tc_hi)) << 32) | (((long)(source_tc_lo)) & 0x00000000FFFFFFFFL));
    
    return proper_source_tc;
  }
}

/***
 * Admin.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.control;

import java.io.*;
import java.net.*;
import java.util.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class Admin
{
  public static void main(String[] args)
  {
    int nr = 0;
    
    final int ARGI_HOST_ROOT   = nr++;
    final int ARGI_PORT_ROOT   = nr++;
    final int ARGI_PASS_ROOT   = nr++;
    final int ARGI_COMMAND     = nr++;
    
    log("connecting to splitter");
    
    final byte[] pass_root = read_file_bytes(args[ARGI_PASS_ROOT], (new byte[SHAZ]));
    
    try {
      Socket socket = (new Socket(args[ARGI_HOST_ROOT], Integer.parseInt(args[ARGI_PORT_ROOT])));
      final OutgoingNetworkStream out = (new OutgoingNetworkStream(socket.getOutputStream(), TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
      
      log("connected");
      
      // arst
      // aqld
      // acst
      
      final ArrayList<String> commands = (new ArrayList<String>());
      
      if (args[ARGI_COMMAND].equals("-")) {
        BufferedReader inp = (new BufferedReader(new InputStreamReader(System.in)));
        
        String line;
        
        while ((line = inp.readLine()) != null) {
          commands.add(line);
        }
      } else {
        commands.add(args[ARGI_COMMAND]);
      }
      
      for (String command : commands) {
        int metadata[] = (new int[PACKET_METADATA_LENGTH]);
        encodeProperSourceTimecode(metadata);
        metadata[PACKET_METADATA_STREAM_ID] = PACKET_METADATA_STREAM_ID_COMMAND;
        
        byte[] data = command.getBytes("UTF-8");
        GroupirPacket groupir = (new GroupirPacket(emptyL, metadata, emptyS, data));
        
        byte[] encoded = groupir.send(pass_root);
        out.wB(encoded);
        
        out.writeback();
      }
      
      socket.close();
    } catch (Exception e) {
      throw (new BAD(e));
    }
  }
}

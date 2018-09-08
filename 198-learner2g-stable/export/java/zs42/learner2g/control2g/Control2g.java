/***
 * Control2g.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.control2g;

import zs42.learner2g.groupir2g.*;

import zs42.splitter.common.*;

import zs42.buff.*;

import zs42.parts.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.*;

public class Control2g
{
  static final NoUTF.Filter FILTER_TOKENIZE = (new NoUTF.Filter("\u0000\u0020//"));
  static final NoUTF.Filter FILTER_TOKENIZE_WITH_TILDE = (new NoUTF.Filter("\u0000\u0020//~~"));
  
  public static void main(String[] args) throws Exception
  {
    Log.loopHandleEventsBackground(System.err, true);
    
    final Settings settings = (new Settings());
    
    // load (possibly multiple) settings files
    {
      for (String argi : args) {
        settings.scan("", argi);
      }
    }
    
    final String http_host = settings.get("control.uplink.http.host");
    final String http_path = settings.get("control.uplink.http.path");
    
    final String splitter_host = settings.get("control.uplink.splitter.host");
    final String splitter_port = settings.get("control.uplink.splitter.port");
    
    final String organization = settings.get("control.organization");
    final String channel      = settings.get("control.channel");
    
    final String credential = settings.get("control.credential");
    
    final int splitter_stream_id_command = Integer.parseInt(settings.get("control.uplink.splitter.stream.command"));
    
    final byte[] teacher_kmac;
    
    {
      HttpARQ http = (new HttpARQ());
      
      http.clearBuffer();
      http.fetchURL(("http://" + http_host + http_path + "control-session/" + organization + "/" + channel + "/" + credential + "/" + Math.max(0, Math.abs(System.nanoTime()))));
      
      byte[] data = http.getBuffer();
      
      System.out.println("<response>" + NoUTF.bin2str(data) + "</response>");
      
      settings.scan("", (new ByteArrayInputStream(data)));
      
      settings.get("teacher.session.authenticated");
      
      teacher_kmac = HexStr.hex2bin(settings.get("teacher.session.KMAC"));
    }
    
    final LinkedBlockingQueue<String> commands = (new LinkedBlockingQueue<String>());
    
    ConvenientUI.launchTextField
      ("Network Commander",
       (new F1<Void, String>()
        {
          public Void invoke(String line)
          {
            commands.add(line);
            return null;
          }
        }));
    
    Log.startLoggedThread
      ((new Log.LoggedThread()
        {
          final BufferCentral central = (new BufferCentral(9));
          
          final Groupir2g.WriteQueue write_queue =
            (new Groupir2g.WriteQueue
             ((new SplitterCommon.Groupir.ChecksumAssistant.KMAC(teacher_kmac)),
              (NetworkingUtilities.connect(splitter_host, Integer.parseInt(splitter_port)).getOutputStream())));
          
          protected void run() throws Exception
          {
            final HttpARQ http = (new HttpARQ());
            
            while (true) {
              String command = commands.take();
              
              Buffer.xI xI = central.acquireI();
              
              {
                final long now = SplitterCommon.Utilities.microTime();
                
                Buffer.nI nI = xI.prepend();
                nI.aI(((int)(now >> 32)));         // capture timecode
                nI.aI(((int)(now      )));         // capture timecode
                nI.aI(splitter_stream_id_command); // stream id
                // nI.aI(0);                       // no versatile value
                nI.release();
              }
              
              Buffer.xB xB = central.acquireB();
              
              Buffer.sB.copy(xB, NoUTF.str2bin(command));
              
              write_queue.push(central, null, xI, null, xB);
              
              xI.release();
              xB.release();
            }
          }
        }));
    
    Log.startLoggedThread
      ((new Log.LoggedThread()
        {
          final JTextArea textarea = ConvenientUI.launchScrollingTextArea("Network Oversight");
          
          protected void run() throws Exception
          {
            final ArrayList<String> messages = (new ArrayList<String>());
            
            final HttpARQ http = (new HttpARQ());
            
            while (true) {
              final ArrayList<String> update_messages = (new ArrayList<String>());
              
              // fetch
              {
                http.clearBuffer();
                http.fetchURL(("http://" + http_host + http_path + "pull-mesg/" + organization + "/" + channel + "/" + credential + "/" + messages.size() + "/" + Math.max(0, Math.abs(System.nanoTime()))));
                
                for (String line : NoUTF.tokenize(NoUTF.bin2str(http.getBuffer(), NoUTF.FILTER_PRINTABLE), FILTER_TOKENIZE_WITH_TILDE)) {
                  update_messages.add(NoUTF.bin2str(HexStr.hex2bin(line), NoUTF.FILTER_PRINTABLE));
                }
              }
              
              for (String message : update_messages) {
                messages.add(message);
              }
              
              SwingUtilities.invokeLater
                ((new Runnable()
                  {
                    public void run()
                    {
                      for (String message : update_messages) {
                        textarea.append(message);
                        textarea.append("\n");
                      }
                    }
                  }));
            }
          }
        }));
    
    
  }
}

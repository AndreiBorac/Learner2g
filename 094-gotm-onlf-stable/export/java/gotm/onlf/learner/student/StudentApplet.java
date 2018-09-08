package gotm.onlf.learner.student;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.applet.*;

import zs42.parts.F0;
import zs42.parts.F1;
import zs42.pixels.codec.*;
import gotm.onlf.utilities.*;
import gotm.onlf.learner.student.*;

import static gotm.onlf.utilities.Utilities.*;

public class StudentApplet extends Applet
{
  F0<Void> shut_down_func = null;
  
  // password list
  String password_list[] = (new String[] { "swordfish" });
  
  private void startup_interconnect(long start_time)
  {
    final HashMap<String, String> settings = (new HashMap<String, String>());
    
    {
      for (String setting : getParameter("SETTINGS").split("\\s")) {
        settings.put(setting, getParameter("SETTING_" + setting));
      }
    }
    
    final int H = Integer.parseInt(settings.get("DESKTOP_HEIGHT"));
    final int W = Integer.parseInt(settings.get("DESKTOP_WIDTH"));
    
    final String splitter_host        = settings.get("SPLITTER_HOST");
    final String splitter_port        = settings.get("SPLITTER_PORT");
    final String splitter_user_pass   = settings.get("SPLITTER_USER_PASS");
    final String splitter_want_bits   = settings.get("SPLITTER_WANT_BITS");
    final String splitter_instance_id = settings.get("SPLITTER_INSTANCE_ID");
    
    final int ups = Integer.parseInt(settings.get("DESKTOP_UPDATES_PER_SEC"));
    
    final PixelsCodec.Dimensions dim = (new PixelsCodec.Dimensions(H, W));
    
    log("launching GUI: host: " + splitter_host + " port: " + splitter_port + " pass: " + splitter_user_pass + " want bits: " + splitter_want_bits + " ups: " + ups);
    
    shut_down_func = GrandUnifiedInterconnect.launch(dim, splitter_host, splitter_port, splitter_user_pass, splitter_want_bits, splitter_instance_id, ups, start_time, settings);
    
    setBackground((new Color(0, 0, 0)));
  }

  private void shutdown_interconnect()
  {
    if (shut_down_func != null) {
      shut_down_func.invoke();
      shut_down_func = null;
    }
  }
  
  public synchronized void init()
  {
    final long start_time = System.currentTimeMillis();
    
    if (getParameter("SETTING_USE_128_BIT_VECTORS") != null) {
      Utilities.logging_target = System.err;
    }
    
    startup_interconnect(start_time);
  }
  
  public synchronized void destroy()
  {
    shutdown_interconnect();
  }
  
  public void paint(Graphics g)
  {
    g.setColor((new Color(255, 255, 255)));
    g.setFont((new Font(Font.MONOSPACED, Font.PLAIN, 16)));
    
    // TODO: use FontMetrics to get this to look good
    // it should have the text each line centered horizontally
    // equal spacing between the lines
    // the whole paragraph centered on the screen
    {
      int y = 20;
      
      for (String line : (new String[] { "STUDENT", "APPLET", "LOADED" })) {
        g.drawString(line, 0, y);
        y += 16;
      }
    }
  }
}

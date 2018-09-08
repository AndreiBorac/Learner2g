/***
 * ApplicationLauncher.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.student2g;

import zs42.parts.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class ApplicationLauncher
{
  public static abstract class ApplicationClient
  {
    protected abstract void start();
    protected abstract boolean started();
    
    protected abstract void stop();
    protected abstract boolean stopped();
  }
  
  private static enum Status
  {
    STARTING
      {
        boolean getRunning   () { return true; }
        boolean getStable    () { return false; }
        String  getAction    () { return "Stop"; }
        String  getDepiction () { return "starting ..."; }
      },
    STARTED
      {
        boolean getRunning   () { return true; }
        boolean getStable    () { return true; }
        String  getAction    () { return "Stop"; }
        String  getDepiction () { return "currently running."; }
      },
    STOPPING
      {
        boolean getRunning   () { return false; }
        boolean getStable    () { return false; }
        String  getAction    () { return "Start"; }
        String  getDepiction () { return "stopping ..."; }
      },
    STOPPED
      {
        boolean getRunning   () { return false; }
        boolean getStable    () { return true; }
        String  getAction    () { return "Start"; }
        String  getDepiction () { return "currently stopped."; }
      };
    
    abstract boolean getRunning   ();
    abstract boolean getStable    ();
    abstract String  getAction    ();
    abstract String  getDepiction ();
  }
  
  private final ApplicationClient client;
  
  private Status status = Status.STOPPED;
  private Status priori = null;
  
  private JPanel  parlor;
  private JLabel  billet;
  private JButton button;
  
  public ApplicationLauncher(ApplicationClient client, final Container container)
  {
    this.client = client;
    
    SwingUtilities.invokeLater
      ((new Runnable()
        {
          public void run()
          {
            try {
              onInitialize(container);
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        }));
  }
  
  public void start()
  {
    SwingUtilities.invokeLater
      ((new Runnable()
        {
          public void run()
          {
            try {
              onStartRequest();
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        }));
  }
  
  void stop()
  {
    SwingUtilities.invokeLater
      ((new Runnable()
        {
          public void run()
          {
            try {
              onStopRequest();
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        }));
  }
  
  private static Component edgeX()
  {
    return Box.createRigidArea((new Dimension(5, 0)));
  }
  
  private static Component edgeY()
  {
    return Box.createRigidArea((new Dimension(0, 5)));
  }
  
  private void onInitialize(Container container)
  {
    /* automatically generated frame feature is intended mainly for debugging */
    if (container == null) {
      JFrame frame = (new JFrame("ApplicationLauncher"));
      
      frame.setSize(500, 100);
      
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      
      frame.setVisible(true);
      
      container = frame.getContentPane();
    }
    
    parlor = (new JPanel());
    billet = (new JLabel());
    button = (new JButton());
    
    {
      parlor.setLayout((new BoxLayout(parlor, BoxLayout.Y_AXIS)));
      
      parlor.add(Box.createVerticalGlue());
      
      {
        JPanel panel_status = (new JPanel());
        
        panel_status.setLayout((new BoxLayout(panel_status, BoxLayout.X_AXIS)));
        panel_status.add(Box.createHorizontalGlue());
        panel_status.add(billet);
        panel_status.add(Box.createHorizontalGlue());
        
        parlor.add(panel_status);
      }
      
      parlor.add(Box.createVerticalGlue());
      
      {
        JPanel panel_button = (new JPanel());
        
        panel_button.setLayout((new BoxLayout(panel_button, BoxLayout.X_AXIS)));
        panel_button.add(Box.createHorizontalGlue());
        panel_button.add(button);
        panel_button.add(Box.createHorizontalGlue());
        
        parlor.add(panel_button);
      }
      
      parlor.add(Box.createVerticalGlue());
      
      container.setLayout((new CardLayout()));
      container.add(parlor, "parlor");
    }
    
    button.addActionListener
      ((new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            onButton();
          }
        }));
    
    (new javax.swing.Timer
     (500,
      (new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            onTimer();
          }
        }))).start();
  }
  
  private void onTimer()
  {
    if (status == Status.STARTING) {
      if (client.started()) {
        status = Status.STARTED;
      }
    }
    
    if (status == Status.STOPPING) {
      if (client.stopped()) {
        status = Status.STOPPED;
      }
    }
    
    if (status != priori) {
      billet.setText(("The application is " + status.getDepiction()));
      button.setText((status.getAction() + " Application"));
      button.setEnabled(status.getStable());
      
      billet.invalidate();
      button.invalidate();
      parlor.invalidate();
      
      parlor.validate();
      parlor.repaint();
      
      priori = status;
    }
  }
  
  private void onStartRequest()
  {
    if (status != Status.STOPPED) return;
    
    status = Status.STARTING;
    
    client.start();
    
    onTimer();
  }
  
  private void onStopRequest()
  {
    if (status != Status.STARTED) return;
    
    status = Status.STOPPING;
    
    client.stop();
    
    onTimer();
  }
  
  private void onButton()
  {
    if (status == Status.STOPPED) onStartRequest();
    if (status == Status.STARTED) onStopRequest();
  }
}

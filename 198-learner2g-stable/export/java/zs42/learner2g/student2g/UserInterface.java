/***
 * UserInterface.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.student2g;

import zs42.learner2g.lantern2g.ny4j.bind.*;

import zs42.mass.*;
import zs42.ny4j.*;

import zs42.au2g.*;

import gotm.etch.*;
import zs42.nwed.*;

import zs42.parts.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import static zs42.parts.Log.T;

// disambiguate (ugh)
import zs42.mass.F0;
import zs42.mass.F2;
import zs42.mass.Nothing;

public class UserInterface
{
  /***
   * ENTER HELPERS
   ***/
  
  static abstract class DimensionModifier
  {
    DimensionModifier next;
    
    DimensionModifier(DimensionModifier next)
    {
      this.next = next;
    }
    
    void modifyDimensions(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize)
    {
      if (next != null) next.modifyDimensions(minimumSize, preferredSize, maximumSize);
      modifyDimensionsInner(minimumSize, preferredSize, maximumSize);
    }
    
    abstract void modifyDimensionsInner(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize);
  }
  
  static class ForcePreferredHeightDM extends DimensionModifier
  {
    ForcePreferredHeightDM(DimensionModifier next)
    {
      super(next);
    }
    
    void modifyDimensionsInner(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize)
    {
      minimumSize.height = preferredSize.height;
      maximumSize.height = preferredSize.height;
    }
  }
  
  static class ForcePreferredWidthDM extends DimensionModifier
  {
    ForcePreferredWidthDM(DimensionModifier next)
    {
      super(next);
    }
    
    void modifyDimensionsInner(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize)
    {
      minimumSize.width = preferredSize.width;
      maximumSize.width = preferredSize.width;
    }
  }
  
  static class ImposeMaximumWidthDM extends DimensionModifier
  {
    int width;
    
    ImposeMaximumWidthDM(int width, DimensionModifier next)
    {
      super(next);
      this.width = width;
    }
    
    void modifyDimensionsInner(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize)
    {
      minimumSize.width   = Math.min(minimumSize.width,   width);
      preferredSize.width = Math.min(preferredSize.width, width);
      maximumSize.width   = Math.min(maximumSize.width,   width);
    }
  }
  
  static class JTextFieldDM extends JTextField
  {
    DimensionModifier dm;
    
    JTextFieldDM(DimensionModifier dm)
    {
      this.dm = dm;
    }
    
    public Dimension getMinimumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return minimumSize;
    }
    
    public Dimension getPreferredSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return preferredSize;
    }
    
    public Dimension getMaximumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return maximumSize;
    }
  }
  
  static class JComboBoxDM extends JComboBox
  {
    DimensionModifier dm;
    
    JComboBoxDM(DimensionModifier dm)
    {
      this.dm = dm;
    }
    
    public Dimension getMinimumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return minimumSize;
    }
    
    public Dimension getPreferredSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return preferredSize;
    }
    
    public Dimension getMaximumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return maximumSize;
    }
  }
  
  static class JSliderDM extends JSlider
  {
    DimensionModifier dm;
    
    JSliderDM(int min, int max, int val, DimensionModifier dm)
    {
      super(min, max, val);
      this.dm = dm;
    }
    
    public Dimension getMinimumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return minimumSize;
    }
    
    public Dimension getPreferredSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return preferredSize;
    }
    
    public Dimension getMaximumSize()
    {
      Dimension minimumSize   = super.getMinimumSize();
      Dimension preferredSize = super.getPreferredSize();
      Dimension maximumSize   = super.getMaximumSize();
      
      dm.modifyDimensions(minimumSize, preferredSize, maximumSize);
      
      return maximumSize;
    }
  }
  
  static Component edgeX()
  {
    return Box.createRigidArea((new Dimension(5, 0)));
  }
  
  static Component edgeY()
  {
    return Box.createRigidArea((new Dimension(0, 5)));
  }
  
  /***
   * LEAVE HELPERS
   ***/
  
  public static final int MIXER_CHOOSER_MAXIMUM_WIDTH = 512;
  public static final int VOLUME_SLIDER_MAXIMUM = 1024;
  public static final int VOLUME_SLIDER_INITIAL = (VOLUME_SLIDER_MAXIMUM / 2);
  
  public static final String[] MIXER_CHOOSER_PREFIX_ITEMS = (new String[] { /* "(reload sound device list)", */ "(default java sound device)" });
  public static final int MIXER_CHOOSER_DEFAULT_ITEM = 0;
  
  public static final String HANDLE_ETCH = "etch";
  public static final String HANDLE_NWED = "nwed";
  
  public static UserInterfaceAgent launch(final JNylus.Linkage linkage, final UserInterfaceYield yield)
  {
    return
      (new UserInterfaceAgent(linkage)
        {
          final ArrayList<F0> detachments = (new ArrayList<F0>());
          
          Etch          etch;
          NetworkEditor nwed;
          
          boolean uiMixerChooserEventSuppression = false;
          boolean uiFormatChooserEventSuppression = false;
          JComboBox uiMixerChooser;
          JComboBox uiFormatChooser;
          JLabel uiWatchdogLabel;
          JPanel uiClientArea;
          CardLayout uiClientAreaLayout;
          JFrame uiFrame;
          
          protected void onInitialize(final HashMap<String, String> settings, final boolean realtime, final F2<Nothing, Integer, Integer> doVolumeLevel)
          {
            try {
              JPanel dY = (new JPanel());
              dY.setLayout((new BoxLayout(dY, BoxLayout.Y_AXIS)));
              dY.add(edgeY());
              
              // audio settings row
              {
                final JSlider uiVolumeSlider;
                
                /* OLD
                   final JCheckBox uiStaticCheckbox;
                   final JCheckBox uiSilentCheckbox;
                */
                
                JPanel dX = (new JPanel());
                dX.setLayout((new BoxLayout(dX, BoxLayout.X_AXIS)));
                dX.add(edgeX());
                
                dX.add((new JLabel("Device:")));
                dX.add(edgeX());
                
                dX.add((uiMixerChooser = (new JComboBoxDM(((new ImposeMaximumWidthDM(MIXER_CHOOSER_MAXIMUM_WIDTH, (new ForcePreferredHeightDM(null)))))))));
                dX.add(edgeX());
                
                for (String item : MIXER_CHOOSER_PREFIX_ITEMS) {
                  uiMixerChooser.addItem(item);
                }
                
                uiMixerChooser.setSelectedIndex(MIXER_CHOOSER_DEFAULT_ITEM);
                
                uiMixerChooser.addItemListener
                  (new ItemListener()
                    {
                      final ItemListener self = this;
                      { detachments.add((new F0() { public Void invoke() { uiMixerChooser.removeItemListener(self); return null; } })); }
                      
                      public void itemStateChanged(ItemEvent event)
                      {
                        if (!uiMixerChooserEventSuppression) {
                          if (event.getStateChange() == ItemEvent.SELECTED) {
                            yield.doMixerSelect(uiMixerChooser.getSelectedIndex());
                          }
                        }
                      }
                    });
                
                dX.add((new JLabel("Format:")));
                dX.add(edgeX());
                
                dX.add((uiFormatChooser = (new JComboBoxDM((new ForcePreferredHeightDM((new ForcePreferredWidthDM(null))))))));
                dX.add(edgeX());
                
                {
                  cL<Audio2g.MixerFormat> formats = Audio2g.MixerFormat.sensible_writer;
                  
                  for (int pos = formats.off(); pos < formats.lim(); pos++) {
                    uiFormatChooser.addItem(formats.get(pos).getLabel());
                  }
                }
                
                uiFormatChooser.addItemListener
                  (new ItemListener()
                    {
                      final ItemListener self = this;
                      { detachments.add((new F0() { public Void invoke() { uiFormatChooser.removeItemListener(self); return null; } })); }
                      
                      public void itemStateChanged(ItemEvent event)
                      {
                        if (!uiFormatChooserEventSuppression) {
                          if (event.getStateChange() == ItemEvent.SELECTED) {
                            yield.doFormatSelect(uiFormatChooser.getSelectedIndex());
                          }
                        }
                      }
                    });
                
                dX.add((new JLabel("Volume:")));
                dX.add(edgeX());
                
                dX.add((uiVolumeSlider = (new JSliderDM(0, VOLUME_SLIDER_MAXIMUM, VOLUME_SLIDER_INITIAL, (new ForcePreferredHeightDM(null))))));
                dX.add(edgeX());
                
                uiVolumeSlider.addChangeListener
                  (new ChangeListener()
                    {
                      final ChangeListener self = this;
                      { detachments.add((new F0() { public Void invoke() { uiVolumeSlider.removeChangeListener(self); return null; } })); }
                      
                      public void stateChanged(ChangeEvent e)
                      {
                        if (!(uiVolumeSlider.getValueIsAdjusting())) {
                          doVolumeLevel.invoke(uiVolumeSlider.getValue(), VOLUME_SLIDER_MAXIMUM);
                        }
                      }
                    });
                
                if (realtime)
                  {
                    dX.add((uiWatchdogLabel = (new JLabel("connection status unknown"))));
                    dX.add(edgeX());
                    
                    /* OLD
                    dX.add((uiStaticCheckbox = (new JCheckBox("Static"))));
                    dX.add(edgeX());
                    
                    uiStaticCheckbox.addActionListener
                      (new ActionListener()
                        {
                          final ActionListener self = this;
                          { detachments.add((new F0() { public Void invoke() { uiStaticCheckbox.removeActionListener(self); return null; } })); }
                          
                          public void actionPerformed(ActionEvent e)
                          {
                            yield.doStaticCheckbox(uiStaticCheckbox.isSelected());
                          }
                        });
                    
                    dX.add((uiSilentCheckbox = (new JCheckBox("Silent"))));
                    dX.add(edgeX());
                    
                    uiSilentCheckbox.addActionListener
                      (new ActionListener()
                        {
                          final ActionListener self = this;
                          { detachments.add((new F0() { public Void invoke() { uiSilentCheckbox.removeActionListener(self); return null; } })); }
                          
                          public void actionPerformed(ActionEvent e)
                          {
                            yield.doSilentCheckbox(uiSilentCheckbox.isSelected());
                          }
                        });
                    */
                  }
                else
                  {
                    {
                      final JButton button = (new JButton("Rewind"));
                      
                      button.addActionListener
                        (new ActionListener()
                          {
                            final ActionListener self = this;
                            { detachments.add((new F0() { public Void invoke() { button.removeActionListener(self); return null; } } )); }
                            
                            public void actionPerformed(ActionEvent event)
                            {
                              yield.doRewind();
                            }
                          });
                      
                      dX.add(button);
                      dX.add(edgeX());
                    }
                    
                    {
                      final JToggleButton button = (new JToggleButton("Skip Silence"));
                      
                      button.addActionListener
                        (new ActionListener()
                          {
                            final ActionListener self = this;
                            { detachments.add((new F0() { public Void invoke() { button.removeActionListener(self); return null; } } )); }
                            
                            public void actionPerformed(ActionEvent event)
                            {
                              if (button.isSelected()) {
                                yield.doSetSkipSilence(true);
                              } else {
                                yield.doSetSkipSilence(false);
                              }
                            }
                          });
                      
                      dX.add(button);
                      dX.add(edgeX());
                    }
                  }
                
                dY.add(dX);
                dY.add(edgeY());
              }
              
              // feedback row
              {
                JPanel dX = (new JPanel());
                dX.setLayout((new BoxLayout(dX, BoxLayout.X_AXIS)));
                dX.add(edgeX());
                
                dX.add((new JLabel("Message:")));
                dX.add(edgeX());
                {
                  final JTextFieldDM textfield = (new JTextFieldDM((new ForcePreferredHeightDM(null))));
                  
                  textfield.addKeyListener
                    (new KeyAdapter()
                      {
                        final KeyListener self = this;
                        { detachments.add((new F0() { public Void invoke() { textfield.removeKeyListener(self); return null; } } )); }
                        
                        public void keyTyped(KeyEvent event)
                        {
                          if (event.getKeyChar() == '\n') {
                            String contents = textfield.getText();
                            textfield.setText("");
                            yield.doFeedback(contents);
                          }
                        }
                      });
                  
                  dX.add(textfield);
                  dX.add(edgeX());
                }
                
                {
                  for (final String preset : (new String[] { "A", "B", "C", "D", "E", "YES/TRUE", "NO/FALSE" })) {
                    final JButton button = (new JButton(preset));
                    
                    button.addActionListener
                      (new ActionListener()
                        {
                          final ActionListener self = this;
                          { detachments.add((new F0() { public Void invoke() { button.removeActionListener(self); return null; } } )); }
                          
                          public void actionPerformed(ActionEvent e) {
                            yield.doFeedback(preset);
                          }
                        });
                    
                    dX.add(button);
                    dX.add(edgeX());
                  }
                }
                
                dY.add(dX);
                dY.add(edgeY());
              }
              
              // component row
              {
                JPanel dX = (new JPanel());
                dX.setLayout((new BoxLayout(dX, BoxLayout.X_AXIS)));
                
                dX.add(edgeX());
                
                dX.add((uiClientArea = (new JPanel())));
                dX.add(edgeX());
                
                {
                  uiClientArea.setLayout((uiClientAreaLayout = (new CardLayout())));
                  
                  int etchH = Integer.parseInt(settings.get("ETCH_H"));
                  int etchW = Integer.parseInt(settings.get("ETCH_W"));
                  
                  etch = (new Etch(etchH, etchW, null, (1000 / Integer.parseInt(settings.get("ETCH_UPS")))));
                  uiClientArea.add(etch.getInterfaceElement(), HANDLE_ETCH);
                  
                  nwed = (new NetworkEditor());
                  JScrollPane nwedScrollPane = nwed.getScrollPane();
                  nwedScrollPane.setPreferredSize((new Dimension(etchW, etchH)));
                  uiClientArea.add(nwedScrollPane, HANDLE_NWED);
                }
                
                dY.add(dX);
                dY.add(edgeY());
              }
              
              uiFrame = (new JFrame("Student's Applet"));
              uiFrame.getContentPane().add(dY);
              uiFrame.pack();
              
              uiFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
              
              uiFrame.addWindowListener
                ((new WindowAdapter()
                  {
                    final WindowListener self = this;
                    { detachments.add((new F0() { public Void invoke() { uiFrame.removeWindowListener(self); return null; } } )); }
                    
                    public void windowClosing(WindowEvent e)
                    {
                      yield.doFrameClose();
                    }
                  }));
              
              uiFrame.setVisible(true);
            } catch (Throwable e) {
              Log.log(e);
            }
            
            yield.doInitialize(settings, realtime, doVolumeLevel);
          }
          
          protected void onShutdown()
          {
            etch.cleanup();
            
            etch = null;
            nwed = null;
            
            // detachments from UI
            {
              for (F0 action : detachments) {
                action.invoke();
              }
              
              detachments.clear();
            }
            
            uiFrame.getContentPane().removeAll();
            
            uiFrame.dispose();
            
            yield.doShutdown();
            
            linkage.station_callee.setTerminated();
          }
          
          protected void onReplaceWatchdogStatus(String status)
          {
            uiWatchdogLabel.setText(status);
            
            yield.doReplaceWatchdogStatus(status);
          }
          
          protected void onReplaceMixerChoices(ArrayList<String> labels)
          {
            {
              uiMixerChooserEventSuppression = true;
              
              uiMixerChooser.removeAllItems();
              
              for (String item : MIXER_CHOOSER_PREFIX_ITEMS) {
                uiMixerChooser.addItem(item);
              }
              
              for (String item : labels) {
                uiMixerChooser.addItem(item);
              }
              
              uiMixerChooser.setSelectedIndex(MIXER_CHOOSER_DEFAULT_ITEM);
              
              uiMixerChooserEventSuppression = false;
            }
            
            yield.doReplaceMixerChoices(labels);
          }
          
          protected void onResetFormatChoices()
          {
            {
              uiFormatChooserEventSuppression = true;
              
              uiFormatChooser.setSelectedIndex(0);
              
              uiFormatChooserEventSuppression = false;
            }
            
            yield.doResetFormatChoices();
          }
          
          protected void onRaiseEtch()
          {
            uiClientAreaLayout.show(uiClientArea, HANDLE_ETCH);
            
            yield.doRaiseEtch();
          }
          
          protected void onRaiseNwed()
          {
            uiClientAreaLayout.show(uiClientArea, HANDLE_NWED);
            
            yield.doRaiseNwed();
          }
          
          protected void onSubmitEtchEvents(ArrayList<Etch.InputEvent> events)
          {
            {
              for (Etch.InputEvent event : events) {
                etch.submitGeneric(event);
              }
              
              events.clear();
            }
            
            yield.doSubmitEtchEvents(events);
          }
          
          protected void onSubmitEtchEventBundle(Etch.EventBundleInputEvent event)
          {
            if (event != null) {
              etch.submitSynthetic(event);
            } else {
              event = etch.obtainReturnBundle();
              
              if (event != null) {
                yield.doSubmitEtchEventBundle(event);
              }
            }
          }
          
          protected void onSubmitNwedEvents(ArrayList<Integer> events)
          {
            {
              for (Integer event : events) {
                nwed.onEvent(event);
              }
              
              events.clear();
            }
            
            yield.doSubmitNwedEvents(events);
          }
        });
  }
}

/***
 * MixerPanel.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.teacher;

import static gotm.onlf.utilities.Utilities.log;

import zs42.parts.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class MixerPanel
{
  static class GenUII
  {
    static class ComponentMap
    {
      final TreeMap<String, Component> map = (new TreeMap<String, Component>());
      
      Component get(String key)
      {
        Component ret = map.get(key);
        if (ret == null) throw null;
        return ret;
      }
      
      ComponentMap put(String key, Component val)
      {
        map.put(key, val);
        return this;
      }
    }
    
    private static Queue<String> enterMotes(String specification)
    {
      ArrayDeque<String> out = (new ArrayDeque<String>());
      
      StringTokenizer tok = (new StringTokenizer(specification));
      
      while (tok.hasMoreTokens()) {
        out.add(tok.nextToken());
      }
      
      return out;
    }
    
    private static GroupLayout.Group interpretLayoutSpecification(ComponentMap components, String right_channel, String wrong_channel, GroupLayout layout, GroupLayout.Group group, Queue<String> motes, String closing)
    {
      while (!motes.isEmpty()) {
        String mote = motes.remove();
        
        if (mote.equals(right_channel)) {
          continue;
        }
        
        if (mote.equals(wrong_channel)) {
          while (!motes.isEmpty() && !motes.remove().equals(right_channel)) {
            // loop
          }
          
          // the very next mote will be of the correct channel, unless
          // it is another mode change ...
          continue;
        }
        
        boolean resizable = true;
        boolean anchorBaselineToTop = false;
        GroupLayout.Alignment alignment = GroupLayout.Alignment.LEADING;
        
        if ((!motes.isEmpty()) && (motes.peek().equals("!"))) { motes.remove(); resizable = false; }
        if ((!motes.isEmpty()) && (motes.peek().equals("^"))) { motes.remove(); anchorBaselineToTop = true; }
        
        for (GroupLayout.Alignment possible_alignment : GroupLayout.Alignment.values()) {
          if ((!motes.isEmpty()) && (motes.peek().equals(possible_alignment.name()))) {
            motes.remove();
            alignment = possible_alignment;
          }
        }
        
        /****/ if (mote.equals("{")) {
          group.addGroup(interpretLayoutSpecification(components, right_channel, wrong_channel, layout, layout.createBaselineGroup(resizable, anchorBaselineToTop), motes, "}"));
        } else if (mote.equals("[")) {
          group.addGroup(interpretLayoutSpecification(components, right_channel, wrong_channel, layout, layout.createParallelGroup(alignment, resizable), motes, "]"));
        } else if (mote.equals("(")) {
          group.addGroup(interpretLayoutSpecification(components, right_channel, wrong_channel, layout, layout.createSequentialGroup(), motes, ")"));
        } else if (mote.equals("i")) {
          group.addComponent(components.get(motes.remove()));
        } else if (mote.equals("g")) {
          group.addGap(Integer.parseInt(motes.remove()));
        } else if ((closing != null) && (mote.equals(closing))) {
          return group;
        } else {
          log("unexpected mote: " + mote);
          throw null; // unexpected mote
        }
      }
      
      if (closing != null) {
        throw null; // unexpected end-of-stream
      }
      
      return group;
    }
    
    static Component createFormattedPanel(ComponentMap components, String specification, boolean auto_create_gaps, boolean auto_create_container_gaps, String name, boolean swing)
    {
      // if enabled, log layout formatting
      if (false) {
        StringBuilder out = (new StringBuilder());
        
        out.append("UI: createFormattedPanel(specification='" + specification + "'):");
        
        for (Map.Entry<String, Component> component : components.map.entrySet()) {
          out.append(" '" + component.getKey() + "' => '" + component.getValue().getClass().getName() + "@" + component.getValue().hashCode() + "'");
        }
        
        out.append(")");
        
        log(out.toString());
      }
      
      Container panel = (swing ? (new JPanel()) : (new Panel()));
      GroupLayout layout = (new GroupLayout(panel));
      
      if (name != null) {
        panel.setName(name);
      }
      
      panel.setLayout(layout);
      
      layout.setAutoCreateGaps(auto_create_gaps);
      layout.setAutoCreateContainerGaps(auto_create_container_gaps);
      
      layout.setHorizontalGroup (interpretLayoutSpecification(components, "H:", "V:", layout, layout.createSequentialGroup(), enterMotes(specification), null));
      layout.setVerticalGroup   (interpretLayoutSpecification(components, "V:", "H:", layout, layout.createSequentialGroup(), enterMotes(specification), null));
      
      return panel;
    }
  }
  
  static class LayoutSpec
  {
    String H;
    String V;
    
    LayoutSpec() { }
    
    LayoutSpec(String h, String v) { H = h; V = v; }

    public String toString()
    {
      return "H: " + H + " V: " + V;
    }
  }

  static class LabelBatch
  {
    private static final int LABEL_HEIGHT_FIX = 10;
    
    final JLabel label_audio = createLabel("Audio");
    final JLabel label_video = createLabel("Video");

    private JLabel createLabel(String label_text)
    {
      JLabel out = (new JLabel(label_text, SwingConstants.CENTER));
      Dimension size = out.getMinimumSize();
      size = (new Dimension(size.width, size.height + LABEL_HEIGHT_FIX));
      out.setMinimumSize(size);
      return out;
    }
  }

  double SD = 5;
  final boolean[] audio_off = (new boolean[] { false });
  final boolean[] video_off = (new boolean[] { false });
  double audio_level = 50;
  double video_level = 50;
  
  static Component makePanel(final JProgressBar audio_bar, final JProgressBar video_bar, final F1<Void, Boolean> onAudioButtonPress, final F1<Void, Boolean> onVideoButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());

    final LabelBatch batch = (new LabelBatch());
    
    cm.put("al", batch.label_audio);
    cm.put("vl", batch.label_video);
    
    cm.put("ap", audio_bar);
    cm.put("vp", video_bar);

    final JRadioButton audio_off_button = (new JRadioButton());

    audio_off_button.addItemListener
      (new ItemListener()
        {
          public void itemStateChanged(ItemEvent e)
          {
            onAudioButtonPress.invoke(e.getStateChange() == ItemEvent.SELECTED);
          }
        });

    final JRadioButton video_off_button = (new JRadioButton());

    video_off_button.addItemListener
      (new ItemListener()
        {
          public void itemStateChanged(ItemEvent e)
          {
            onVideoButtonPress.invoke(e.getStateChange() == ItemEvent.SELECTED);
          }
        });

    cm.put("ab", audio_off_button);
    cm.put("vb", video_off_button);

    LayoutSpec spec = (new LayoutSpec("( [ CENTER i al i ap i ab ] [ CENTER i vl i vp i vb ] )", "( [ i al i vl ] [ i ap i vp ] [ i ab i vb ] )"));
    
    return (GenUII.createFormattedPanel(cm, spec.toString(), true, true, "Mixer", true));
  }
  
  void launch()
  {
    final F1<Void, Boolean> onAudioButtonPress =
      (new F1<Void, Boolean>()
       {
         public Void invoke(Boolean new_state)
         {
           audio_off[0] = new_state;
           return null;
         }
       });

    final F1<Void, Boolean> onVideoButtonPress =
      (new F1<Void, Boolean>()
       {
         public Void invoke(Boolean new_state)
         {
           video_off[0] = new_state;
           return null;
         }
       });

    final JProgressBar audio_bar = (new JProgressBar(SwingConstants.VERTICAL));
    final JProgressBar video_bar = (new JProgressBar(SwingConstants.VERTICAL));

    Component panel = makePanel(audio_bar, video_bar, onAudioButtonPress, onVideoButtonPress);

    final JFrame frame = new JFrame("Mixer Panel");

    frame.add(panel);
    frame.pack();
    frame.setVisible(true);

    Random rand_gen = (new Random());

    while (true) {

      double delta = SD * rand_gen.nextGaussian();

      if (audio_off[0] && delta > 0) delta = 0;
      audio_level += delta;
      audio_level = Math.max(audio_level,   0);
      //audio_level = Math.min(audio_level, 100);

      delta = SD * rand_gen.nextGaussian();
      if (video_off[0] && delta > 0) delta = 0;
      video_level += delta;
      video_level = Math.max(video_level,   0);
      //video_level = Math.min(video_level, 100);

      audio_bar.setValue((int)Math.min(audio_level, 100));
      video_bar.setValue((int)Math.min(video_level, 100));

      try {
        Thread.sleep(50);
      } catch(Exception e) {
      }
    }
  }

  public static void main(String args[])
  {
    (new MixerPanel()).launch();
  }
}

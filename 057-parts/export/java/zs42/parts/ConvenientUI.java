/***
 * ConvenientUI.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class ConvenientUI
{
  public static void launchTextField(final String title, final F1<Void, String> action)
  {
    try {
      SwingUtilities.invokeAndWait
        ((new Runnable()
          {
            public void run()
            {
              final JTextField textfield = (new JTextField());
              
              final JFrame frame = (new JFrame(title));
              frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
              frame.getContentPane().add(textfield);
              
              frame.setSize(500, 50);
              frame.setVisible(true);
              
              textfield.addKeyListener
                ((new KeyAdapter()
                  {
                    public void keyPressed(KeyEvent e)
                    {
                      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        String text = textfield.getText();
                        action.invoke(text);
                        textfield.setText("");
                      }
                    }
                  }));
            }
          }));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static JTextArea launchScrollingTextArea(final String title)
  {
    final JTextArea[] textarea_shadow = (new JTextArea[1]);
    
    try {
      SwingUtilities.invokeAndWait
        ((new Runnable()
          {
            public void run()
            {
              final JTextArea textarea = (new JTextArea());
              textarea.setEditable(false);
              textarea.setFont((new Font(Font.MONOSPACED, Font.BOLD, 14)));
              
              final JScrollPane scrollpane = (new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
              scrollpane.setViewportBorder((new javax.swing.border.LineBorder(Color.WHITE, 5)));
              
              final JFrame frame = (new JFrame(title));
              frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
              frame.getContentPane().add(scrollpane);
              
              frame.setSize(800, 600);
              frame.setVisible(true);
              
              textarea_shadow[0] = textarea;
            }
          }));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
    
    return textarea_shadow[0];
  }
}

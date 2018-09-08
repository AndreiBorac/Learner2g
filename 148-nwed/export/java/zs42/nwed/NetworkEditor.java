/***
 * NetworkEditor.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.nwed;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

/***
 * Despite the name, NetworkEditor has no built-in networking
 * capabilities. It has a replay-based architecture, however, which is
 * the main thing.
 ***/
public class NetworkEditor
{
  static final int BEYOND_H = 2400;
  static final int BEYOND_W = 3200;
  
  static boolean isPlain(char x)
  {
    return ((('0' <= x) && (x <= '9')) || (('A' <= x) && (x <= 'Z')) || (('a' <= x) && (x <= 'z')));
  }
  
  static boolean isSymbol(char x)
  {
    return (('!' <= x) && (x <= '~') && !isPlain(x));
  }
  
  static int nrocc(String line, char x)
  {
    int retv = 0;
    
    for (int i = 0, l = line.length(); i < l; i++) {
      if (line.charAt(i) == x) {
        retv++;
      }
    }
    
    return retv;
  }
  
  /***
   * return the "net indent change" introduced by the given
   * string. the net indent level may be negative, positive or
   * zero.
   ***/
  static int netdent(String line)
  {
    return ((nrocc(line, '(') + nrocc(line, '[') + nrocc(line, '{')) - (nrocc(line, ')') + nrocc(line, ']') + nrocc(line, '}')));
  }
  
  /***
   * returns the "minimum indent level" of the given string. the
   * minimum indent level is always less than or equal to zero.
   ***/
  static int mindent(String line, String indent_incrementors, String indent_decrementors)
  {
    int lowest = 0;
    int current = 0;
    
    for (int i = 0, l = line.length(); i < l; i++) {
      if (indent_incrementors.indexOf(line.charAt(i)) >= 0) current++;
      if (indent_decrementors.indexOf(line.charAt(i)) >= 0) current--;
      
      lowest = Math.min(lowest, current);
    }
    
    return lowest;
  }
  
  static String repeat(char x, int n)
  {
    char[] out = (new char[n]);
    for (int i = 0; i < out.length; i++) out[i] = x;
    return (new String(out));
  }
  
  static String rjustify(int lineno)
  {
    String out = "" + lineno;
    while (out.length() < 3) out = "0" + out;
    return out;
  }
  
  static class Context
  {
    HashSet<String> keywords = (new HashSet<String>(Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while")));
    
    String indent_incrementors = "{[(";
    String indent_decrementors = ")]}";
  }
  
  static class Buffer
  {
    final Context context;
    
    final ArrayList<String> lines = (new ArrayList<String>(Arrays.asList("")));
    
    boolean syntax_enabled = true;
    
    int cursor_row = 0; // zero-based line number that the cursor is on
    int cursor_col = 0; // zero-based character number that the cursor is at on the cursor's line
    
    Buffer(Context context)
    {
      this.context = context;
    }
    
    void movedown()
    {
      cursor_row++;
      while (!(cursor_row < lines.size())) lines.add("");
    }
    
    void snapleft()
    {
      cursor_col = Math.min(cursor_col, lines.get(cursor_row).length());
    }
    
    void onMagic(char x)
    {
      if (x == '^') {
        if (cursor_row > 0) {
          cursor_row--;
          snapleft();
        } else {
          // nothing to do
        }
      }
      
      if (x == '_') {
        movedown();
        snapleft();
      }
      
      if (x == '<') {
        if (cursor_col > 0) {
          cursor_col--;
        } else {
          if (cursor_row > 0) {
            cursor_row--;
            cursor_col = Integer.MAX_VALUE;
            snapleft();
          } else {
            // nothing to do
          }
        }
      }
      
      if (x == '>') {
        cursor_col++;
        
        if (cursor_col > lines.get(cursor_row).length()) {
          movedown();
          cursor_col = 0;
        }
      }
      
      if (x == 'q') {
        syntax_enabled = (!syntax_enabled);
      }
      
      if (x == 't') {
        // indent everything
        {
          int level = 0;
          
          for (int i = 0, l = lines.size(); i < l; i++) {
            String line = lines.get(i);
            
            int effective = level + mindent(line, context.indent_incrementors, context.indent_decrementors);
            line = ((effective > 0) ? (repeat(' ', (effective << 1))) : ("")) + line.trim();
            
            level += netdent(line);
            
            lines.set(i, line);
          }
        }
        
        // move to the end of the current line
        cursor_col = Integer.MAX_VALUE;
        snapleft();
      }
      
      if (x == 'n') {
        String line = lines.get(cursor_row);
        
        if (line.length() < cursor_col) {
          lines.add((cursor_row + 1), "");
        } else {
          lines.add((cursor_row + 1), line.substring(cursor_col));
          lines.set(cursor_row, line.substring(0, cursor_col));
        }
        
        cursor_row++;
        cursor_col = 0;
      }
      
      if (x == 'b') {
        if (cursor_col > 0) {
          String line = lines.get(cursor_row);
          
          if (line.length() < cursor_col) {
            // nothing to do
          } else {
            lines.set(cursor_row, line.substring(0, cursor_col - 1) + line.substring(cursor_col));
          }
          
          cursor_col--;
        } else {
          if (cursor_row > 0) {
            cursor_row--;
            cursor_col = lines.get(cursor_row).length();
            lines.set(cursor_row, lines.get(cursor_row) + lines.get((cursor_row + 1)));
            lines.remove((cursor_row + 1));
          } else {
            // nothing to do
          }
        }
      }
      
      // 'd' means delete next character
      if (x == 'd') {
        onMagic('>');
        onMagic('b');
      }
      
      // 'k' means delete current line
      if (x == 'k') {
        if (lines.size() > 1) {
          lines.remove(cursor_row);
          
          if (!(cursor_row < lines.size())) {
            cursor_row = (lines.size() - 1);
          }
          
          if (cursor_col > lines.get(cursor_row).length()) {
            cursor_col = lines.get(cursor_row).length();
          }
        } else {
          // we must be on the first line, so ...
          lines.set(0, "");
          cursor_row = 0;
        }
      }
      
      // validate
      {
        if (!((0 <= cursor_row) && (cursor_row < lines.size()))) throw null;
        if (!((0 <= cursor_col) && (cursor_col <= lines.get(cursor_row).length()))) throw null;
      }
    }
    
    void onGlyph(char x)
    {
      String line = lines.get(cursor_row);
      
      if (line.length() < cursor_col) {
        line = line + x;
      } else {
        line = line.substring(0, cursor_col) + x + line.substring(cursor_col);
        cursor_col++;
      }
      
      lines.set(cursor_row, line);
    }
  }
  
  static class Transcriber
  {
    final DefaultStyledDocument document;
    
    final Style style_normal; // style for normal text
    final Style style_margin; // style for margin (line numbers)
    final Style style_extent; // style for extent (right margin indicator)
    final Style style_lankey; // style for language keyword
    final Style style_symbol; // style for symbols (paranthesis, brackets, operators)
    final Style style_cursor; // style for special cursor symbol
    
    HashSet<String> keywords = (new HashSet<String>(Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while")));
    
    Transcriber()
    {
      // create document
      
      document = (new DefaultStyledDocument());
      
      // elaborate styles
      
      // normal
      
      style_normal = document.addStyle(null, StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE));
      
      StyleConstants.setFontFamily(style_normal, Font.MONOSPACED);
      StyleConstants.setFontSize(style_normal, 14);
      
      // margin
      
      style_margin = document.addStyle(null, style_normal);
      
      StyleConstants.setBold(style_margin, true);
      StyleConstants.setForeground(style_margin, (new Color(0, 0, 255)));
      
      // extent
      
      style_extent = document.addStyle(null, style_normal);
      
      StyleConstants.setBold(style_extent, true);
      StyleConstants.setForeground(style_extent, (new Color(127, 127, 127)));
      
      // lankey
      
      style_lankey = document.addStyle(null, style_normal);
      
      StyleConstants.setBold(style_lankey, true);
      StyleConstants.setForeground(style_lankey, (new Color(255, 0, 0)));
      
      // symbol
      
      style_symbol = document.addStyle(null, style_normal);
      
      StyleConstants.setBold(style_symbol, true);
      
      // cursor
      
      style_cursor = document.addStyle(null, style_normal);
      
      StyleConstants.setBold(style_cursor, true);
      StyleConstants.setForeground(style_cursor, (new Color(255, 255, 255)));
      StyleConstants.setBackground(style_cursor, (new Color(0, 0, 0)));
    }
    
    int length()
    {
      return document.getLength();
    }
    
    void remove()
    {
      try {
        document.remove(0, document.getLength());
      } catch (BadLocationException e) {
        throw (new RuntimeException(e));
      }
    }
    
    void append(String text, Style style)
    {
      try {
        document.insertString(document.getLength(), text, style);
      } catch (BadLocationException e) {
        throw (new RuntimeException(e));
      }
    }
    
    void append_plain(Context context, String line)
    {
      append(line, style_normal);
    }
    
    void append_syntax(Context context, String line)
    {
      while (line.length() > 0) {
        char first = line.charAt(0);
        
        int   pflen = 0;
        Style style;
        
        /****/ if (isPlain(first)) {
          while ((pflen < line.length()) && (isPlain(line.charAt(pflen)))) pflen++;
          style = context.keywords.contains(line.substring(0, pflen)) ? style_lankey : style_normal;
        } else if (isSymbol(first)) {
          while ((pflen < line.length()) && (isSymbol(line.charAt(pflen)))) pflen++;
          style = style_symbol;
        } else {
          pflen = 1;
          style = style_normal;
        }
        
        append(line.substring(0, pflen), style);
        line = line.substring(pflen);
      }
    }
    
    /***
     * truncates the current document contents and appends new text
     * regions representing the state of the given buffer. returns the
     * new caret position.
     ***/
    int acquire(Context context, Buffer buffer)
    {
      remove();
      
      int retv = 0;
      
      for (int i = 0, l = buffer.lines.size(); i < l; i++) {
        String line = buffer.lines.get(i);
        
        if (buffer.syntax_enabled) {
          append("[" + rjustify((i + 1)) + "]", style_margin);
          append(" ", style_normal);
          
          append_syntax(context, line);
          
          append(".", style_extent);
          append("\n", style_extent);
        } else {
          append_plain(context, (("[" + rjustify((i + 1)) + "] ") + line + ".\n"));
        }
        
        if (i == buffer.cursor_row) {
          int lhs = length();
          append(("|---|-" + repeat('-', buffer.cursor_col) + "^"), style_cursor);
          int rhs = length();
          
          append(" ", style_normal);
          append("\n", style_normal);
          
          retv = ((buffer.cursor_col < 16) ? lhs : rhs);
        }
      }
      
      return retv;
    }
  }
  
  final Context context = (new Context());
  
  int bufferi = 0;
  final Buffer[] buffers = (new Buffer[10]);
  
  final Transcriber transcriber;
  
  final JTextPane   visual;
  final JScrollPane scroll;
  
  public NetworkEditor()
  {
    for (int i = 0; i < buffers.length; i++) {
      buffers[i] = (new Buffer(context));
    }
    
    transcriber = (new Transcriber());
    
    visual = (new JTextPane(transcriber.document));
    
    visual.setEditable(false);
    
    final Dimension beyond = (new Dimension(BEYOND_W, BEYOND_H));
    
    final JPanel canvas = (new JPanel((new FlowLayout(FlowLayout.CENTER, 0, 0))));
    
    visual.setSize(beyond);
    visual.setMinimumSize(beyond);
    visual.setPreferredSize(beyond);
    
    canvas.setSize(beyond);
    canvas.setMinimumSize(beyond);
    canvas.setPreferredSize(beyond);
    
    canvas.add(visual);
    
    scroll = (new JScrollPane(canvas));
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    // seed display
    onMagic('1');
  }
  
  public JScrollPane getScrollPane()
  {
    return scroll;
  }
  
  public void addKeyListener(KeyListener listener)
  {
    visual.addKeyListener(listener);
  }
  
  public void setLanguageKeywords(HashSet<String> keywords)
  {
    context.keywords = (new HashSet<String>(keywords));
  }
  
  public void setIndentIncrementors(String indent_incrementors)
  {
    context.indent_incrementors = indent_incrementors;
  }
  
  public void setIndentDecrementors(String indent_decrementors)
  {
    context.indent_decrementors = indent_decrementors;
  }
  
  void update()
  {
    visual.setCaretPosition(transcriber.acquire(context, buffers[bufferi]));
  }
  
  public void onMagic(char x)
  {
    if (('0' <= x) && (x <= '9')) {
      bufferi = (x - '0');
    } else {
      buffers[bufferi].onMagic(x);
    }
    
    update();
  }
  
  public void onGlyph(char x)
  {
    buffers[bufferi].onGlyph(x);
    update();
  }
  
  public void onEvent(int encoded)
  {
    boolean magic = ((encoded >>> 31) != 0);
    char    typed = ((char)(encoded & 0xFF));
    
    if (magic) {
      onMagic(typed);
    } else {
      onGlyph(typed);
    }
  }
  
  public int getCurrentBufferIndex()
  {
    return bufferi;
  }
  
  public String getCurrentBufferBytes()
  {
    StringBuilder out = (new StringBuilder());
    
    for (String line : buffers[bufferi].lines) {
      out.append(line);
      out.append('\n');
    }
    
    return out.toString();
  }
  
  public int getCurrentBufferLineCount()
  {
    return buffers[bufferi].lines.size();
  }
}

/***
 * UserInterface.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.onlf.learner.student;

import gotm.onlf.learner.common.*;

import gotm.onlf.utilities.*;
import gotm.onlf.utilities.Utilities.*;

import zs42.buff.*;
import zs42.parts.*;
import zs42.pixels.codec.*;
import zs42.nwed.*;
import gotm.etch.*;

import zs42.nats.codec.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

import java.io.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import static gotm.onlf.utilities.Utilities.log;
import static gotm.onlf.utilities.Utilities.microTime;

import static gotm.onlf.learner.common.Constants.UserInterface.*;

public class UserInterface
{
  /***
   * SPECIFICATION
   ***/
  
  /***
   * methods of the following interface may be invoked from the
   * context of any thread. they do not block.
   ***/
  static abstract class Control
  {
    /***
     * display instructor feedback line
     ***/
    abstract void putLine(String line);
    abstract int  stop(F1<Void, Integer> dec);
    
    abstract void gotEtchJarfilePacket(int size, byte[] data);
  }
  
  static class Packet
  {
    // complete=true: a desktop update is complete at the end of this packet.
    // a redraw may happen only after decoding a complete packet
    final boolean complete;

    // suppressed=true: the packet is delayed or it has high complexity
    // and thus it will not be decoded by the awt thread.
    // Instead, it will be sent to a background decoding thread.
    final boolean suppressed;

    final byte[] codec_payload;

    Packet(boolean complete, boolean suppressed, byte[] codec_payload) {
      this.complete = complete;
      this.suppressed = suppressed;
      this.codec_payload = codec_payload;
    }
  }
  
  static class TypedPacket
  {
    final boolean magic;
    final char    typed;
    
    TypedPacket(boolean magic, char typed)
    {
      this.magic = magic;
      this.typed = typed;
    }
  }
  
  static class EtchPacket
  {
    final long   ustc;
    final int    size;
    final byte[] data;
    
    EtchPacket(long ustc, int size, byte[] data)
    {
      this.ustc = ustc;
      this.size = size;
      this.data = data;
    }
    
    EtchPacket reclock(long ustc)
    {
      return (new EtchPacket(ustc, size, data));
    }
  }
  
  /***
   * methods of the following interface will be invoked in the context
   * of the specialized user interaction thread.
   ***/
  interface Callback
  {
    String gotCommand(String request);
    void gotMute(boolean enabled);
    void gotStatic(boolean enabled);
    void gotTarget(AudioCommon.Target target); // use null for "default"
    void gotFormat(AudioCommon.Format format); // use null for "default"
    void gotVolume(double volume);
    void gotUserFeedback(String line);
    Packet getPacket();
    TypedPacket getTypedPacket();
    EtchPacket getEtchPacket();
  }
  
  /***
   * launches a new thread (or possibly delegates thread creation to a
   * GUI package's event dispatch thread) to handle user interaction
   * with a graphical user interface.
   ***/
  static Control launch(Callback callback, AudioCommon.Target[] targets, PixelsCodec.Dimensions dims, int ups, long start_time, HashMap<String, String> settings)
  {
    return (new UserInterface(start_time)).launch_inner(callback, targets, dims, ups, settings);
  }
  
  /***
   * IMPLEMENTATION
   ***/
  
  static class LabelBatch
  {
    private static final int LABEL_HEIGHT_FIX = 10;
    
    final JLabel label_target   = createLabel("Mixer:");
    final JLabel label_format   = createLabel("Format:");
    final JLabel label_volume   = createLabel("Volume:");
    final JLabel label_static   = createLabel("Static?");
    final JLabel label_mute     = createLabel("Mute?");
    final JLabel label_history  = createLabel("History:");
    final JLabel label_name     = createLabel("Your Name:");
    final JLabel label_answer   = createLabel("Answer:");
    final JLabel label_feedback = createLabel("Message:");
    final JLabel label_command  = createLabel("Command:");
    final JLabel label_password = createLabel("Type Password:");
    final JLabel label_old_java = createLabel("You are running an outdated version of Java. Please upgrade to Java 1.6 or newer for an improved user interface.");
    
    private JLabel createLabel(String label_text)
    {
      JLabel out = (new JLabel(label_text, SwingConstants.RIGHT));
      Dimension size = out.getMinimumSize();
      size = (new Dimension(size.width, size.height + LABEL_HEIGHT_FIX));
      out.setMinimumSize(size);
      out.setName(out.getText());
      return out;
    }
  }
  
  static abstract class TiledCanvas
  {
    protected final PixelsCodec.Dimensions dims;
    
    private PixelsCodec.Framebuffer framebuffer;
    
    static int min(int A, int B) { return ((A <= B) ? A : B); }
    
    public PixelsCodec.Framebuffer getFramebuffer()
    {
      return framebuffer;
    }
    
    public TiledCanvas(PixelsCodec.Dimensions dims)
    {
      this.dims = dims;
      
      framebuffer = (new PixelsCodec.Framebuffer(dims));
    }
    
    public PixelsCodec.Dimensions getDims() {
      return dims;
    }
    
    /***
     * returns the pixel value array. format is 24-bit RGB.
     ***/
    public final int[] getRaster()
    {
      return framebuffer.getRaster();
    }
    
    /***
     * marks pixels with indices off (inclusive) through lim (exclusive)
     * as dirty (i.e., needing redraw).
     ***/
    public abstract void setDirty(int off, int lim);
    
    /***
     * marks pixels with coordinates in the given rectange as dirty
     * (i.e., needing redraw).
     ***/
    public abstract void setDirty(int x, int y, int w, int h);
    
    /***
     * forces all pixels to the dirty state. equiv <code>setDirty(0,
     * getRaster().length)</code>. subclasses are encouraged to
     * override this method and supply a more efficient
     * implementation.
     ***/
    public void setDirtyAll()
    {
      setDirty(0, dims.HxW);
    }
    
    /***
     * redraws all currently dirty pixels to the given graphics
     * context, and resets them to the clean state. after calling this
     * method, all pixels will be in the clean state. <code>my</code>
     * and <code>mx</code> specify the position of the mouse
     * cursor. the mouse cursor is drawn without affecting the
     * framebuffer (i.e., the contents of xRGB will be the same before
     * and after this call and will not contain the cursor shape).
     ***/
    public abstract void redraw(Graphics g, int my, int mx, long deadline_ms);
  }
  
  static class UnbufferedTiledCanvas extends TiledCanvas
  {
    final boolean []             cmap;
    final int     []             tile;
    
    final int tbits;
    final int tsize; // (1 << tbits)
    final int tmask; // (tsize - 1)
    final int tspan; // (dims.W >> tbits) // number of tiles to span the screen width
    
    final BufferedImage cache;
    
    static int min(int A, int B) { return ((A <= B) ? A : B); }
    
    private int getTileForPixel(int idx)
    {
      return tile[idx];
    }
    
    private int getTileForPixel(int y, int x)
    {
      y >>= tbits;
      x >>= tbits;
      
      return ((tspan * y) + x);
    }
    
    /***
     * constructor. <code>param_tbits</code> specifies the base-2
     * logarithm of the tile side length. use 4 for 16x16, 5 for 32x32
     * or 6 for 64x64. the tile side length must divide both the canvas
     * height and the canvas width.
     ***/
    public UnbufferedTiledCanvas(PixelsCodec.Dimensions dims, int param_tbits)
    {
      super(dims);
      
      if (param_tbits < 2) throw null;
      
      tbits = param_tbits;
      tsize = (1 << tbits);
      tmask = tsize - 1;
      
      if ((dims.H & tmask) != 0) throw null;
      if ((dims.W & tmask) != 0) throw null;
      
      tspan = dims.W >> tbits;
      
      cmap = (new boolean[((dims.HxW >> tbits) >> tbits)]);
      tile = (new int[dims.HxW]);
      
      {
        int acc_H = 0;
        
        for (int y = 0; y < dims.H; y++) {
          for (int x = 0; x < dims.W; x++) {
            tile[acc_H + x] = getTileForPixel(y, x);
          }
          
          acc_H += dims.W;
        }
      }
      
      cache = (new BufferedImage(tsize, tsize, BufferedImage.TYPE_INT_RGB));
    }
    
    /***
     * marks pixels with indices off (inclusive) through lim (exclusive)
     * as dirty (needs redraw).
     ***/
    public void setDirty(int off, int lim)
    {
      int scanline_lim = (off / dims.W + 1) * dims.W;

      while (off < lim) {

        int min = getTileForPixel(off + 0);
        int iter_lim = min(scanline_lim, lim);
        int max = getTileForPixel(iter_lim - 1);

        for (int i = min; i <= max; i++) {
          cmap[i] = true;
        }

        off = iter_lim;
        scanline_lim += dims.W;
      }
    }
    
    /***
     * marks pixels with coordinates in the given rectange as dirty
     * (i.e., needing redraw).
     ***/
    public void setDirty(int x, int y, int w, int h)
    {
      int off_x = x;
      int lim_x = x + w;
      int off_y = y;
      int lim_y = y + h;

      // move back in y to the first row of the top tiles
      off_y = off_y & ~tmask;

      int acc_H = dims.W * off_y;

      for (int pos_y = off_y; pos_y < lim_y; pos_y += tsize) {
        setDirty(acc_H + off_x, acc_H + lim_x);
        acc_H += tsize * dims.W;
      }
    }
    
    /***
     * forces all pixels to the dirty state.
     ***/
    public void setDirtyAll()
    {
      for (int i = 0; i < cmap.length; i++) {
        cmap[i] = true;
      }
    }
    
    /***
     * redraws all currently dirty pixels to the given graphics context,
     * and resets them to the clean state. after calling this method,
     * all pixels will be in the clean state.
     ***/
    public void redraw(Graphics g, int my, int mx, long deadline_ms)
    {
      for (int i = 0; i < cmap.length; i++) {
        if (System.currentTimeMillis() > deadline_ms) break;
        
        if (cmap[i]) {
          cmap[i] = false;
          
          int first_tile_in_row = (i / tspan) * tspan;
          int off = ((first_tile_in_row << tbits) + i - first_tile_in_row) << tbits;
          
          cache.setRGB(0, 0, tsize, tsize, getFramebuffer().getRaster(), off, dims.W);
          
          int off_y = (i / tspan) << tbits;
          int off_x = (i % tspan) << tbits;
          
          g.drawImage(cache, off_x, off_y, null);
        }
      }
    }
  }
  
  static class SquareBufferedTiledCanvas extends UnbufferedTiledCanvas
  {
    final int bsize;
    final BufferedImage cache;
    
    public SquareBufferedTiledCanvas(PixelsCodec.Dimensions dims, int param_tbits, int param_k)
    {
      super(dims, param_tbits);
      
      bsize = ((2 * param_k) + 1);
      
      cache = (new BufferedImage((bsize << tbits), (bsize << tbits), BufferedImage.TYPE_INT_RGB));
    }
    
    /***
     * redraws all currently dirty pixels to the given graphics context,
     * and resets them to the clean state. after calling this method,
     * all pixels will be in the clean state.
     ***/
    public void redraw(Graphics g, int my, int mx, long deadline_ms)
    {
      // compute bounding box
      int lim_y = (dims.H >> tbits), lim_x = tspan;
      int min_y = lim_y, min_x = lim_x, max_y = 0, max_x = 0;
      
      for (int i = 0; i < cmap.length; i++) {
        if (cmap[i]) {
          int y = (i / tspan);
          int x = i - y * tspan;
          
          if (y < min_y) min_y = y;
          if (x < min_x) min_x = x;
          if (y + 1 > max_y) max_y = y + 1;
          if (x + 1 > max_x) max_x = x + 1;
        }
      }
      
      if (min_y >= max_y || min_x >= max_x) return;
      
      if (min_y + bsize < max_y || min_x + bsize < max_x) {
        log("redraw unbuffered: min: " + min_x + " " + min_y + " max: " + max_x + " " + max_y);
        super.redraw(g, my, mx, deadline_ms);
        return;
      } else {
        log("redraw buffered");
      }
      
      for (int y = min_y; y < max_y; y++) {
        for (int x = min_x; x < max_x; x++) {
          cmap[y * tspan + x] = false;
        }
      }
      
      int off_x = (min_x << tbits);
      lim_x = (max_x << tbits);
      int off_y = (min_y << tbits);
      lim_y = (max_y << tbits);
      int off = off_y * dims.W + off_x;
      
      cache.setRGB(0, 0, (lim_x - off_x), (lim_y - off_y), getFramebuffer().getRaster(), off, dims.W);
      g.drawImage(cache, off_x, off_y, lim_x, lim_y, 0, 0, lim_x - off_x, lim_y - off_y, null);
    }
  }

  static class RemoteDesktop extends Canvas
  {
    final Utilities.RateTracer trace_decode  = (new Utilities.RateTracer("decode"));
    final Utilities.RateTracer trace_display = (new Utilities.RateTracer("display"));
    
    final TiledCanvas tiles;
    final BufferCentral central;
    final Callback callback;
    final long update_interval_ms;
    final long start_time;
    
    long next_update_time_ms;
    volatile boolean was_scrolled = false;
    volatile long last_scroll_ms;
    
    // decoding thread
    final LinkedBlockingQueue<Packet> deferred = (new LinkedBlockingQueue<Packet>());
    final LinkedBlockingQueue<Packet> returned = (new LinkedBlockingQueue<Packet>());

    // Number of outstanding packets sent by update() to the decoding thread and not returned yet.
    // To be modified only by the awt thread.
    int outstanding_decode_count = 0;
    
    void notifyScrolled()
    {
      was_scrolled = true;
      last_scroll_ms = System.currentTimeMillis();
    }
    
    boolean wasScrolled()
    {
      return was_scrolled;
    }
    
    void setWasScrolled(boolean ws)
    {
      was_scrolled = ws;
    }
    
    long getLastScrollMs()
    {
      return last_scroll_ms;
    }
    
    void setDirtyAll()
    {
      tiles.setDirtyAll();
    }
    
    boolean decode_packet_complete = false;
    
    void decode_packet(Packet packet)
    {
      Buffer.xB crm = central.acquireB();
      
      {
        Buffer.nB crm_n = crm.append();
        Buffer.sB.copy(crm_n, packet.codec_payload, 0, packet.codec_payload.length);
        crm_n.release();
      }
      
      Buffer.xI list = central.acquireI();
      
      //dbg
      long now = microTime();
      long start_decode_time = now;
      //
      
      PixelsCodec._003.decode(central, tiles.getFramebuffer(), crm, tiles.getFramebuffer(), tiles.getDims(), list);
      
      
      //dbg
      now = microTime();
      log("decode_packet: after decode " + (now - start_decode_time));
      //
      
      trace_decode.trigger();
      
      crm.release();
      
      Buffer.oI list_o = list.iterate();
      int num = list_o.remaining();
      
      while (num > 0) {
        int off = list_o.rI(); num--;
        int lim = list_o.rI(); num--;
        
        tiles.setDirty(off, lim);
      }
      
      list_o.release();
      list.release();
      
      decode_packet_complete = packet.complete;
    }

    RemoteDesktop(PixelsCodec.Dimensions dims, final int ups, Callback callback, long start_time)
    {
      tiles = (new SquareBufferedTiledCanvas(dims, LOG_TILE_SIZE, HALF_TILE_BUFFER_SIZE));
      central = (new BufferCentral());
      this.callback = callback;
      update_interval_ms = 1000 / ups;
      next_update_time_ms = System.currentTimeMillis();
      this.start_time = start_time;
      
      setSize(new Dimension(dims.W, dims.H));
      setMinimumSize(new Dimension(dims.W, dims.H));
      setPreferredSize(new Dimension(dims.W, dims.H));
      setMaximumSize(new Dimension(dims.W, dims.H));
      
      // decoding thread
      (new Thread()
        {
          public void run()
          {
            while (true) {
              try {
                Packet packet = deferred.take();
                decode_packet(packet);
                returned.put(packet);
              } catch (Exception e) {
                log(e);
              }
            }
          }
        }).start();
      
      // background update thread forces decoding even when component
      // not visible so as not to have video packets accumulating in
      // the receive buffer
      if (true) {
        (new Thread()
          {
            public void run()
            {
              try {
                while (true) {
                  update(null);
                  sleep(1000);
                }
              } catch (Exception e) {
                log(e);
                throw null;
              }
            }
          }).start();
      }
      
      if (true) {
        (new Thread()
          {
            public void run()
            {
              try {
                while (true) {
                  if (RemoteDesktop.this.wasScrolled()) {
                    long now_ms = System.currentTimeMillis();
                    long lsm = RemoteDesktop.this.getLastScrollMs();
                    if (now_ms - lsm > SCROLL_FIX_LATENCY) {
                      tiles.setDirtyAll();
                      repaint();
                      RemoteDesktop.this.setWasScrolled(false);
                      log("scroll detected, dirtied all tiles " + now_ms);
                    }
                  }
                  sleep(1000);
                }
              } catch (Exception e) {
                log(e);
                throw null;
              }
            }
          }).start();
      }
      
      if (true) {
        (new Thread()
          {
            public void run()
            {
              try {
                final long interval = 1000 / ups;
                
                while (true) {
                  repaint();
                  sleep(interval);
                  //log("component_info: getBounds()=" + getBounds().toString() + ", getHeight()=" + getHeight() + ", getWidth()=" + getWidth() + ", getIgnoreRepaint()=" + getIgnoreRepaint() + ", getMinimumSize()=" + getMinimumSize() + ", getMaximumSize()=" + getMaximumSize() + ", getName()=" + getName() + ", getSize()=" + getSize() + ", getX()=" + getX() + ", getY()=" + getY() + ", hasFocus()=" + hasFocus() + ", isDisplayable()=" + isDisplayable() + ", isDoubleBuffered()=" + isDoubleBuffered() + ", isEnabled()=" + isEnabled() + ", isFocusable()=" + isFocusable() + ", isShowing()=" + isShowing() + ", isValid()=" + isValid() + ", isVisible()=" + isVisible());
                }
              } catch (Exception e) {
                log(e);
                throw null;
              }
            }
          }).start();
      }
    }
    
    public void paint(Graphics g)
    {
      log("entering paint: g: " + g + " getClipBounds():" + g.getClipBounds().toString());
      g = g.create();
      g.clipRect(0, 0, tiles.dims.W, tiles.dims.H);
      log(" clipped to " + g.getClipBounds().toString());
      Rectangle r = g.getClipBounds();
      tiles.setDirty(r.x, r.y, r.width, r.height);
      update(g);
    }
    
    long last_full_update_ms = System.currentTimeMillis();
    
    private void update_give_up(Packet packet)
    {
      try {
        if (packet != null) {
          deferred.put(packet);
          outstanding_decode_count++;
        }
      
        while ((packet = callback.getPacket()) != null) {
          deferred.put(packet);
          outstanding_decode_count++;
        }
      } catch (InterruptedException e) {
        throw (new RuntimeException(e));
      }
    }
    
    private void update_give_up()
    {
      update_give_up(null);
    }
    
    public synchronized void update(Graphics g)
    {
      long time_enter = System.currentTimeMillis();
      long deadline_ms = time_enter + MAXIMUM_UPDATE_STALL_MS;
      
      // manage decode thread
      {
        try {
          // accept returned packets
          while (returned.peek() != null) {
            Packet p = returned.take();
            outstanding_decode_count--;
          }
          
          if (outstanding_decode_count != 0) {
            // we can't go on since there is a decode backlog (and
            // therefore we do not own the buffer)
            update_give_up();
            return;
          }
        } catch (InterruptedException e) {
          throw (new RuntimeException(e));
        }
      }
      
      // decode unsuppressed packets while staying within a time
      // budget (if we encounter a suppressed packet, queue it and
      // give up) ... we hope to burn through all remaining packets
      // and finally get to display
      {
        Packet packet;
        
        while ((packet = callback.getPacket()) != null) {
          if (packet.suppressed) {
            update_give_up(packet);
            return;
          }
          
          decode_packet(packet);
          
          if (System.currentTimeMillis() > deadline_ms) {
            return; // we'll finish this picnic later
          }
        }
      }
      
      // win!
      {
        if ((g != null) && decode_packet_complete) {
          tiles.redraw(g, 0, 0, deadline_ms);
          
          //dbg
          // {
          //   PixelsCodec.Dimensions dims = tiles.getDims();

          //   my += 8;
          //   mx += 13;
          //   while (my >= dims.H) my -= dims.H;
          //   while (mx >= dims.W) mx -= dims.W;
          // }
          //
          //cursor_canvas.redraw(g, my, mx, deadline_ms);
          //
          
          trace_display.trigger();
        }
      }
    }
  }
  
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
    
    private static LocalDeque<String> enterMotes(String specification)
    {
      LocalDeque<String> out = (new LocalDeque<String>());
      
      StringTokenizer tok = (new StringTokenizer(specification));
      
      while (tok.hasMoreTokens()) {
        out.addLast(tok.nextToken());
      }
      
      return out;
    }
    
    private static GroupLayout.Group interpretLayoutSpecification(ComponentMap components, String right_channel, String wrong_channel, GroupLayout layout, GroupLayout.Group group, LocalDeque<String> motes, String closing)
    {
      while (!motes.isEmpty()) {
        String mote = motes.removeFirst();
        
        if (mote.equals(right_channel)) {
          continue;
        }
        
        if (mote.equals(wrong_channel)) {
          while (!motes.isEmpty() && !motes.removeFirst().equals(right_channel)) {
            // loop
          }
          
          // the very next mote will be of the correct channel, unless
          // it is another mode change ...
          continue;
        }
        
        boolean resizable = true;
        boolean anchorBaselineToTop = false;
        GroupLayout.Alignment alignment = GroupLayout.Alignment.LEADING;
        
        if ((!motes.isEmpty()) && (motes.peekFirst().equals("!"))) { motes.removeFirst(); resizable = false; }
        if ((!motes.isEmpty()) && (motes.peekFirst().equals("^"))) { motes.removeFirst(); anchorBaselineToTop = true; }
        
        for (GroupLayout.Alignment possible_alignment : GroupLayout.Alignment.values()) {
          if ((!motes.isEmpty()) && (motes.peekFirst().equals(possible_alignment.name()))) {
            motes.removeFirst();
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
          group.addComponent(components.get(motes.removeFirst()));
        } else if (mote.equals("g")) {
          group.addGap(Integer.parseInt(motes.removeFirst()));
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
  
  static class PopOutFrame extends JFrame
  {
    PopOutFrame(F1<Void, String> onEnter, F1<Void, String> onButtonPress)
    {
      add(makeQuickStripePanel(POP_OUT_COLC, onEnter, onButtonPress));
      pack();
      setVisible(true);
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
  
  // UserInterface mem
  static final int SETTINGS_HISTORY_ROWC =  40;
  static final int SETTINGS_FEEDBACK_ROWC =  4;
  static final int SETTINGS_FEEDBACK_COLC = 64;
  static final int SETTINGS_NAME_ROWC = 1;
  static final int SETTINGS_NAME_COLC = 32;
  static final int POP_OUT_ROWC = 1;
  static final int POP_OUT_COLC = 32;
  static final int QUICKBAR_FEEDBACK_ROWC =  1;
  static final int QUICKBAR_FEEDBACK_COLC = 8;
  static final int PASSWORD_COLC = 32;
  static final long SCROLL_FIX_LATENCY = 500;
  
  final AtomicReference<String> name_string = (new AtomicReference<String>());
  final AtomicReference<F1<Void, String>> onPutLine =
    (new AtomicReference<F1<Void, String>>
     (new F1<Void, String>() { public Void invoke(String ignored) { return null; } }));
  final AtomicReference<Long> last_scroll_ms = (new AtomicReference<Long>(new Long(-1)));
  final long start_time;
  Dimension last_viewport_size = (new Dimension(0, 0));

  UserInterface(long start_time)
  {
    this.start_time = start_time;
  }
  
  static JComboBox createChoiceBox(ArrayList<String> items, final F1<Void, Integer> onChoose, boolean with_default)
  {
    final JComboBox box = new JComboBox();
    
    if (with_default) {
      box.addItem("default");
    }
    
    for (String item : items) {
      box.addItem(item);
    }
    
    box.setMaximumRowCount(box.getItemCount());
    
    box.addItemListener
      (new ItemListener()
        {
          public void itemStateChanged(ItemEvent event)
          {
            if (event.getStateChange() == ItemEvent.SELECTED) {
              onChoose.invoke(box.getSelectedIndex());
            }
          }
        });
    
    return box;
  }
  
  static LayoutSpec makeSettingsGroup(GenUII.ComponentMap cm, final Callback callback, final AudioCommon.Target[] targets, final F1<Void, String> onReleaseKeyInName, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress, final F1<Void, String> onPopoutButtonPress, final AtomicReference<F1<Void, String>> onPutLine)
  {
    final JComboBox[] target_box = (new JComboBox[1]);
    final JComboBox[] format_box = (new JComboBox[1]);
    final JComboBox[] volume_box = (new JComboBox[1]);
    
    final LabelBatch batch = (new LabelBatch());
    
    final F0<Void> onTargetChange =
      (new F0<Void>()
       {
         public Void invoke()
         {
           format_box[0].setSelectedIndex(0);
           volume_box[0].setSelectedIndex(0);
           return null;
         }
       });
    
    // mixer label
    
    cm.put("ml", batch.label_target);
    
    // mixer choice box
    
    {
      ArrayList<String> items = (new ArrayList<String>());
      
      for (AudioCommon.Target target : targets) {
        items.add(target.getLabel());
      }
      
      target_box[0] =
        (createChoiceBox
         (items,
          ((new F1<Void, Integer>() {
              public Void invoke(Integer index)
              {
                callback.gotTarget(targets[index]);
                onTargetChange.invoke();
                return null;
              }
            })),
          false));
    }
    
    cm.put("m", target_box[0]);
    
    // format label
    
    cm.put("fl", batch.label_format);
    
    // format choice box
    
    {
      ArrayList<String> items = (new ArrayList<String>());
      
      for (AudioCommon.Format format : AudioCommon.Format.sensible_reader) {
        items.add(format.getLabel());
      }
      
      format_box[0] = createChoiceBox
        (items,
         ((new F1<Void, Integer>() {
             public Void invoke(Integer index)
             {
               callback.gotFormat(index == 0 ? null : AudioCommon.Format.sensible_reader[index-1]);
               return null;
             }
           })),
         true);
    }
    
    cm.put("f", format_box[0]);
    
    // volume label
    
    cm.put("vl", batch.label_volume);
    
    {
      // volume choice box
      
      final int DISPLAY_INCREMENT = 10;
      final int DISPLAY_COUNT = 11;
      final double VALUE_SCALE = 1.0 / ((DISPLAY_COUNT - 1) * DISPLAY_INCREMENT);
      
      ArrayList<String> items = new ArrayList<String>();
      
      for (int i = 0; i < DISPLAY_COUNT; i++) {
        items.add("" + (i * DISPLAY_INCREMENT) + " %");
      }
      
      volume_box[0] =
        createChoiceBox
        (items,
         (new F1<Void, Integer>()
      {
        public Void invoke(Integer index)
        {
          if (index == 0) {
            callback.gotVolume(-1.0);
          } else {
            callback.gotVolume((index-1) * DISPLAY_INCREMENT * VALUE_SCALE);
          }
          return null;
        }
      }),
         true);
    }
    
    cm.put("v", volume_box[0]);
    
    // static label
    
    cm.put("sl", batch.label_static);
    
    // static check box
    
    final JCheckBox static_box = new JCheckBox("", false);
    
    static_box.addItemListener
      (new ItemListener()
        {
          public void itemStateChanged(ItemEvent ie)
          {
            if (ie.getStateChange() == ItemEvent.SELECTED) {
              callback.gotStatic(true);
            } else if (ie.getStateChange() == ItemEvent.DESELECTED) {
              callback.gotStatic(false);
            } else {
              // unexpected; ignored
            }
          }
        });
    cm.put("s", static_box);
    
    // mute label
    
    cm.put("mul", batch.label_mute);
    
    // mute check box
    
    final JCheckBox mute_box = new JCheckBox("", false);
    
    mute_box.addItemListener
      (new ItemListener()
        {
          public void itemStateChanged(ItemEvent ie)
          {
            if (ie.getStateChange() == ItemEvent.SELECTED) {
              callback.gotMute(true);
            } else if (ie.getStateChange() == ItemEvent.DESELECTED) {
              callback.gotMute(false);
            } else {
              // unexpected; ignored
            }
          }
        });
    
    cm.put("mu", mute_box);
    
    // history label
    
    cm.put("hl", batch.label_history);
    
    // history text field
    
    final JTextArea history_text_area = (new JTextArea(SETTINGS_HISTORY_ROWC, SETTINGS_FEEDBACK_COLC));
    
    final JScrollPane history_scroll_pane = (new JScrollPane(history_text_area));
    
    onPutLine.set
      (new F1<Void, String>()
       {
         public Void invoke(String line)
         {
           history_text_area.append(line);
           return null;
         }
       });
    
    cm.put("h", history_scroll_pane);
    
    // name label
    
    cm.put("nl", batch.label_name);
    
    // name text field
    
    final JTextField namefield = (new JTextField(SETTINGS_NAME_COLC));
    
    namefield.addKeyListener
      (new KeyAdapter()
        {
          public void keyReleased(KeyEvent event)
          {
            onReleaseKeyInName.invoke(namefield.getText());
          }
        });
    
    cm.put("n", namefield);
    
    LayoutSpec answer_buttons_spec = makeAnswerButtonGroup(cm, (new String[] { "A", "B", "C", "D", "E", "YES/TRUE", "NO/FALSE" }), onAnswerButtonPress);
    
    // pop out button
    
    final JButton popout_button = (new JButton("Pop Out"));
    
    popout_button.addActionListener
      (new ActionListener()
        {
          public void actionPerformed(ActionEvent event)
          {
            onPopoutButtonPress.invoke("");
          }
        });
    
    cm.put("pob", popout_button);
    
    // feedback label
    
    cm.put("fel", batch.label_feedback);
    
    // feedback
    final JTextField textfield = (new JTextField(SETTINGS_FEEDBACK_COLC));
    
    textfield.addKeyListener
      (new KeyAdapter()
        {
          public void keyTyped(KeyEvent event)
          {
            if (event.getKeyChar() == '\n') {
              String contents = textfield.getText();
              textfield.setText("");
              onEnter.invoke(contents);
            }
          }
        });

    cm.put("fe", textfield);
    
    // command label

    cm.put("cl", batch.label_command);

    // command

    final JTextField command_field = (new JTextField(SETTINGS_FEEDBACK_COLC));
    
    command_field.addKeyListener
      (new KeyAdapter()
        {
          public void keyTyped(KeyEvent event)
          {
            if (event.getKeyChar() == '\n') {
              final String request = command_field.getText();
              command_field.setText("");
              
              // command processing
              
              final String reply = callback.gotCommand(request);
              
              if (reply != null) {
                onPutLine.get().invoke(reply + "\n");
              }
            }
          }
        });
    
    cm.put("c", command_field);
    
    return
      (new LayoutSpec
       ("( [ TRAILING i ml i fl i hl i nl i fel i cl ] [ LEADING i m ( i f i vl i v i sl i s i mul i mu ) i h ( i n " + answer_buttons_spec.H + " i pob ) i fe i c ] )",
        "( [ BASELINE i ml i m ] [ BASELINE i fl i f i vl i v i sl i s i mul i mu ] [ BASELINE i hl i h ] [ BASELINE i nl i n " + answer_buttons_spec.V + " i pob ] [ BASELINE i fel i fe ] [ BASELINE i cl i c ] )"));
  }
  
  static LayoutSpec makeAnswerButtonGroup(GenUII.ComponentMap cm, final String[] originalButtonLabels, final F1<Void, String> onButtonPress)
  {
    final String[] buttonLabels = (new String[originalButtonLabels.length]);
    for (int i = 0; i < buttonLabels.length; i++) buttonLabels[i] = originalButtonLabels[i];
    
    LayoutSpec ls = (new LayoutSpec());
    
    ls.H = "(";
    ls.V = "[";
    
    for (int i = 0; i < buttonLabels.length; i++) {
      final String buttonLabel = buttonLabels[i];
      
      JButton button = (new JButton(buttonLabel));
      
      button.addActionListener
        (new ActionListener()
          {
            public void actionPerformed(ActionEvent event)
            {
              onButtonPress.invoke(buttonLabel);
            }
          });

      String key = "b" + buttonLabel;
      cm.put(key, button);
      ls.H += (" i " + key);
      ls.V += (" i " + key);
    }
    
    ls.H += " )";
    ls.V += " ]";
    
    return ls;
  }
  
  static LayoutSpec makeQuickStripeGroup(GenUII.ComponentMap cm, int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    final LabelBatch batch = (new LabelBatch());
    
    cm.put("fl", batch.label_feedback);
    
    final JTextField textfield = (new JTextField(textfield_columns));
      
    textfield.addKeyListener
      (new KeyAdapter()
        {
          public void keyTyped(KeyEvent event)
          {
            if (event.getKeyChar() == '\n') {
              String contents = textfield.getText();
              textfield.setText("");
              onEnter.invoke(contents);
            }
          }
        });
    cm.put("ft", textfield);
    
    LayoutSpec answer_buttons_spec = makeAnswerButtonGroup(cm, (new String[] { "A", "B", "C", "D", "E", "YES/TRUE", "NO/FALSE" }), onAnswerButtonPress);
    
    return (new LayoutSpec("( i fl i ft " + answer_buttons_spec.H + " )",
                           "[ CENTER i fl i ft " + answer_buttons_spec.V + " ]"));
  }
  
  static Component makePasswordPanel(final F1<Void, char[]> onEnter)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());

    final LabelBatch batch = (new LabelBatch());
    
    cm.put("pl", batch.label_password);
    
    final JPasswordField password_field = (new JPasswordField(PASSWORD_COLC));
    
    password_field.addKeyListener
      (new KeyAdapter()
        {
          public void keyTyped(KeyEvent event)
          {
            if (event.getKeyChar() == '\n') {
              char contents[] = password_field.getPassword();
              onEnter.invoke(contents);
              password_field.setText("");
            }
          }
        });

    cm.put("pt", password_field);
    
    LayoutSpec spec = (new LayoutSpec("( i pl i pt )", "[ CENTER i pl i pt ]"));
    
    return (GenUII.createFormattedPanel(cm, spec.toString(), true, true, "Password", true));
  }
  
  static Component makeSettingsPanel(final Callback callback, final AudioCommon.Target[] targets, final F1<Void, String> onReleaseKeyInName, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress, final F1<Void, String> onPopoutButtonPress, final AtomicReference<F1<Void, String>> onPutLine)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeSettingsGroup(cm, callback, targets, onReleaseKeyInName, onEnter, onAnswerButtonPress, onPopoutButtonPress, onPutLine);
    
    return (GenUII.createFormattedPanel(cm, spec.toString(), true, true, "Settings", true));
  }
  
  static Component makeAnswerButtonPanel(final String[] originalButtonLabels, final F1<Void, String> onButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeAnswerButtonGroup(cm, originalButtonLabels, onButtonPress);

    return (GenUII.createFormattedPanel(cm, spec.toString(), true, true, null, true));
  }
  
  static Component makeQuickStripePanel(int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeQuickStripeGroup(cm, textfield_columns, onEnter, onAnswerButtonPress);
    
    return (GenUII.createFormattedPanel(cm, spec.toString(), true, true, "Remote Desktop", true));
  }
  
  static Component makeRemoteDesktopPanel(final RemoteDesktop remote_desktop, final ScrollPane scroll_pane, int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeQuickStripeGroup(cm, textfield_columns, onEnter, onAnswerButtonPress);
    
    Component quick_stripe_panel = GenUII.createFormattedPanel(cm, "H: " + spec.H + " V: " + spec.V, true, true, null, true);
    
    quick_stripe_panel.setMaximumSize((new Dimension(quick_stripe_panel.getMaximumSize().width, quick_stripe_panel.getMinimumSize().height)));

    scroll_pane.setMinimumSize(new Dimension(100, 100));
    
    scroll_pane.add(remote_desktop);
    scroll_pane.addMouseListener
      (new MouseListener()
        {
          public void mouseClicked(MouseEvent e) {}
          public void mouseEntered(MouseEvent e) {}
          public void mouseExited(MouseEvent e) {}
          public void mousePressed(MouseEvent e) {}
          
          public void mouseReleased(MouseEvent e)
          {
            log(""+e+" " + System.currentTimeMillis());
            remote_desktop.notifyScrolled();
          }
        });
    
    cm = (new GenUII.ComponentMap());
    
    cm.put("qsp", quick_stripe_panel);
    cm.put("rd", scroll_pane);
    
    return (GenUII.createFormattedPanel(cm, "H: ( [ i qsp i rd ] ) V: ( ( i qsp i rd ) )", false, false, "Remote Desktop", false));
  }
  
  static Component makeNetworkEditorPanel(final JComponent nwed_pane, int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeQuickStripeGroup(cm, textfield_columns, onEnter, onAnswerButtonPress);
    
    Component quick_stripe_panel = GenUII.createFormattedPanel(cm, "H: " + spec.H + " V: " + spec.V, true, true, null, true);
    
    quick_stripe_panel.setMaximumSize((new Dimension(quick_stripe_panel.getMaximumSize().width, quick_stripe_panel.getMinimumSize().height)));
    
    cm = (new GenUII.ComponentMap());
    
    cm.put("qsp", quick_stripe_panel);
    cm.put("ne", nwed_pane);
    
    return (GenUII.createFormattedPanel(cm, "H: [ i qsp i ne ] V: ( i qsp i ne )", false, false, "Network Editor", true));
  }
  
  static Component makeVisiplatePanel(final JComponent etch_pane, int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec spec = makeQuickStripeGroup(cm, textfield_columns, onEnter, onAnswerButtonPress);
    
    Component quick_stripe_panel = GenUII.createFormattedPanel(cm, "H: " + spec.H + " V: " + spec.V, true, true, null, true);
    
    quick_stripe_panel.setMaximumSize((new Dimension(quick_stripe_panel.getMaximumSize().width, quick_stripe_panel.getMinimumSize().height)));
    
    cm = (new GenUII.ComponentMap());
    
    cm.put("qsp", quick_stripe_panel);
    cm.put("et", etch_pane);
    
    return (GenUII.createFormattedPanel(cm, "H: [ i qsp i et ] V: ( i qsp i et )", false, false, "Network Visiplate", true));
  }
  
  static Component makeConsolidatedRemoteDesktopPanel(RemoteDesktop remote_desktop, int textfield_columns, final F1<Void, String> onEnter, final F1<Void, String> onAnswerButtonPress)
  {
    GenUII.ComponentMap cm = (new GenUII.ComponentMap());
    
    LayoutSpec qss = makeQuickStripeGroup(cm, textfield_columns, onEnter, onAnswerButtonPress);
    
    ScrollPane scroll_pane = (new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS));
    
    scroll_pane.add(remote_desktop);
    
    cm.put("rd", scroll_pane);
    
    return (GenUII.createFormattedPanel(cm, "H: ( [ " + qss.H + " i rd ] ) V: ( ( " + qss.V + " i rd ) )", false, false, "Remote Desktop", false));
  }
  
  static StringBuffer reportSizes(StringBuffer res, Component c, int depth)
  {
    for (int i = 0; i < depth; i++) res.append(" ");
    res.append(c.getClass() + " minSize " + c.getMinimumSize() + "\n");
    
    if (c instanceof Container) {
      for (Component d : ((Container) c).getComponents()) {
        reportSizes(res, d, depth + 1);
      }
    }
    return res;
  }
  
  static void adjustScrollPaneSize(final ScrollPane scroll_pane, final Dimension rdsz)
  {
    // Dimension spsz = scroll_pane.getSize();
    // Dimension vpsz = scroll_pane.getViewportSize();
    // Dimension prefsz = (new Dimension(rdsz.width + spsz.width - vpsz.width, rdsz.height + spsz.height - vpsz.height));

    Dimension prefsz = (new Dimension(rdsz.width + 2 * scroll_pane.getVScrollbarWidth(), rdsz.height + 2 * scroll_pane.getHScrollbarHeight()));

    scroll_pane.setPreferredSize(prefsz);
    
    log("set scroll pane size to " + (rdsz.width + 2 * scroll_pane.getVScrollbarWidth()) + " " +  (rdsz.height + 2 * scroll_pane.getHScrollbarHeight()));
  }
  
  boolean adjustFrameSize(final JFrame frame, final ScrollPane scroll_pane, final Dimension rdsz)
  {
    boolean adjust = false;
    
    Dimension vpsz = scroll_pane.getViewportSize();    //adjustFrameSize(frame, scroll_pane, rdsz);
    
    if (vpsz.equals(last_viewport_size)) return false;
    
    last_viewport_size.setSize(vpsz);
    
    Dimension frsz = (new Dimension(frame.getSize()));
    
    if (vpsz.width > rdsz.width) {
      frsz.width -= vpsz.width - rdsz.width;
      adjust = true;
    }
    
    if (vpsz.height > rdsz.height) {
      frsz.height -= vpsz.height - rdsz.height;
      adjust = true;
    }
    
    if (adjust) {
      frame.setSize(frsz);
    }
    
    return adjust;
  }
  
// 'targets' may be zero-length. In that case there is only a single 'default' menu item
  Control launch_inner(final Callback callback, final AudioCommon.Target[] targets, PixelsCodec.Dimensions dims, final int ups, HashMap<String, String> settings)
  {
    // event handlers
    
    final F1<Void, String> onEnterOrAnswerButtonPress =
      (new F1<Void, String>()
       {
         public Void invoke(String text)
         {
           callback.gotUserFeedback(name_string.get() + ": " + text);
           return null;
         }
       });
    
    final F1<Void, String> onPopoutButtonPress =
      (new F1<Void, String>()
       {
         public Void invoke(String text)
         {
           new PopOutFrame(onEnterOrAnswerButtonPress, onEnterOrAnswerButtonPress);
           return null;
         }
       });
    
    final F1<Void, String> onReleaseKeyInName =
      (new F1<Void, String>()
       {
         public Void invoke(String text)
         {
           name_string.set(text);
           return null;
         }
       });
    
    JTabbedPane tabbed_pane = new JTabbedPane();
    
    Component settings_panel = makeSettingsPanel(callback, targets, onReleaseKeyInName, onEnterOrAnswerButtonPress, onEnterOrAnswerButtonPress, onPopoutButtonPress, onPutLine);
    
    tabbed_pane.add(settings_panel);
    
    final RemoteDesktop remote_desktop;
    final Dimension rdsz;
    final ScrollPane scroll_pane;
    final Component remote_desktop_panel;
    
    {
      RemoteDesktop remote_desktop_shadow = null;
      Dimension rdsz_shadow = null;
      ScrollPane scroll_pane_shadow = null;
      Component remote_desktop_panel_shadow = null;
      
      // add Remote Desktop and Network Editor paths, but only if not in cooked mode
      if (settings.get("COOKED_PATH") == null) {
        remote_desktop_shadow = (new RemoteDesktop(dims, ups, callback, start_time));
        rdsz_shadow = remote_desktop_shadow.getSize();
        
        scroll_pane_shadow = (new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS));
        
        adjustScrollPaneSize(scroll_pane_shadow, rdsz_shadow);
        
        remote_desktop_panel_shadow = makeRemoteDesktopPanel(remote_desktop_shadow, scroll_pane_shadow, QUICKBAR_FEEDBACK_COLC, onEnterOrAnswerButtonPress, onEnterOrAnswerButtonPress);
        
        tabbed_pane.add(remote_desktop_panel_shadow);
        
        final NetworkEditor network_editor = (new NetworkEditor());
        
        final ActionListener onTimer =
          (new ActionListener()
            {
              public void actionPerformed(ActionEvent event)
              {
                TypedPacket packet;
                
                if ((packet = callback.getTypedPacket()) != null) {
                  if (packet.magic) {
                    network_editor.onMagic(packet.typed);
                  } else {
                    network_editor.onGlyph(packet.typed);
                  }
                }
              }
            });
        (new javax.swing.Timer(1000 / Integer.parseInt(settings.get("NWED_UPS")), onTimer)).start();
        
        final JScrollPane nwed_pane = network_editor.getScrollPane();
        
        final Component network_editor_panel = makeNetworkEditorPanel(nwed_pane, QUICKBAR_FEEDBACK_COLC, onEnterOrAnswerButtonPress, onEnterOrAnswerButtonPress);
        
        tabbed_pane.add(network_editor_panel);
      }
      
      remote_desktop = remote_desktop_shadow;
      rdsz = rdsz_shadow;
      scroll_pane = scroll_pane_shadow;
      remote_desktop_panel = remote_desktop_panel_shadow;
    }
    
    final Etch etch = (new Etch(Integer.parseInt(settings.get("ETCH_H")), Integer.parseInt(settings.get("ETCH_W")), null, 1000 / Integer.parseInt(settings.get("ETCH_UPS"))));
    
    (new Thread()
      {
        public void run()
        {
          try {
            BufferCentral central = (new BufferCentral());
            
            Etch.OrganicInputEvent.DecodeCreator decoder = (new Etch.OrganicInputEvent.DecodeCreator());
            
            while (true) {
              EtchPacket packet;
              
              while ((packet = callback.getEtchPacket()) == null) sleep(125);
              
              Buffer.xB crm = central.acquireB();
              Buffer.xI tmp = central.acquireI();
              
              {
                Buffer.nB crm_n = crm.append();
                Buffer.sB.copy(crm_n, packet.data, 0, packet.data.length);
                crm_n.release();
              }
              
              NaturalNumberCodec.decode(central, tmp, crm);
              
              Buffer.oI tmp_o = tmp.iterate();
              decoder.setInputSource(tmp_o, packet.ustc);
              
              for (int i = 0; i < packet.size; i++) {
                Etch.OrganicInputEvent event = decoder.decode();
                log("etch.decode: now=" + microTime() + ", event.ustc=" + event.ustc);
                etch.submit(event);
              }
              
              tmp.release();
              crm.release();
            }
          } catch (Throwable e) {
            log(e);
          }
        }
      }).start();
    
    final JComponent etch_pane = etch.getInterfaceElement();
    
    final Component visiplate_panel = makeVisiplatePanel(etch_pane, QUICKBAR_FEEDBACK_COLC, onEnterOrAnswerButtonPress, onEnterOrAnswerButtonPress);
    
    tabbed_pane.add(visiplate_panel);
    
    final JFrame frame = new JFrame("Student's Applet");
    
    frame.add(tabbed_pane);
    
    frame.pack();
    
    frame.setVisible(true);
    
    if (remote_desktop != null) {
      frame.addComponentListener
        (new ComponentListener()
          {
            long last_resize_ms = System.currentTimeMillis();
            
            public void componentResized(ComponentEvent e)
            {
              log("componentResized: ComponentEvent: +" + e);
              
              if (UserInterface.this.adjustFrameSize(frame, scroll_pane, rdsz)) {
                remote_desktop.setDirtyAll();
                remote_desktop.repaint();
              }
            }
            
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
          });
    }
    
    if ((remote_desktop != null) && false) {
      (new Thread()
        {
          public void run()
          {
            try {
              while (true) {
                log("frame minSize " + frame.getMinimumSize());
                StringBuffer res = (new StringBuffer(""));
                log(reportSizes(res, remote_desktop_panel, 0).toString());
                sleep(1000);
              }
            } catch (Exception e) {
              log(e);
              throw null;
            }
          }
        }).start();
    }
    
    return (new Control()
      {
        void putLine(String line)
        {
          onPutLine.get().invoke(line);
        }

        int stop(F1<Void, Integer> dec)
        {
          frame.dispose();
          return 0;
        }
        
        final ByteArrayOutputStream etch_jarfile_data = (new ByteArrayOutputStream());
        
        synchronized void gotEtchJarfilePacket(int size, byte[] data)
        {
          if (data.length > 0) {
            try {
              etch_jarfile_data.write(data, 0, data.length);
              
              log("etch_jarfile_progress: " + etch_jarfile_data.size() + " / " + size);
              
              if (etch_jarfile_data.size() == size) {
                JarInputStream inp = (new JarInputStream(new ByteArrayInputStream(etch_jarfile_data.toByteArray())));
                
                JarEntry entry;
                
                while ((entry = inp.getNextJarEntry()) != null) {
                  if (entry.getName().endsWith(".png")) {
                    byte[] png = SimpleIO.slurp(inp);
                    etch.appendBackdrop(png);
                  }
                }
              }
            } catch (Exception e) {
              throw (new RuntimeException(e));
            }
          }
        }
    });
  }
}

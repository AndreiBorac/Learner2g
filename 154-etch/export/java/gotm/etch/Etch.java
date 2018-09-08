/***
 * Etch.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.etch;

import zs42.buff.*;

import zs42.nats.codec.*;

import zs42.parts.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;

import javax.swing.*;

import javax.imageio.*;
import javax.imageio.stream.*;

public class Etch
{
  /***
   * "mode selection" constants; i.e., fixed "palettes" that index a
   * very small subset of all possible settings for a given variable.
   ***/
  static class ModeSelect
  {
    // mono color palette
    static final Color BLACK    = (new Color(  0,   0,   0));
    static final Color WHITE    = (new Color(255, 255, 255));
    
    // tame color palette
    static final Color RUST     = (new Color(139,  31,  31));
    static final Color LEAF     = (new Color(  0, 108,   0));
    static final Color TEAL     = (new Color( 14,  78, 121));
    static final Color VIOLET   = (new Color( 74,  42, 150));
    static final Color FUCHSIA  = (new Color(127,  19, 127));
    
    // high color palette
    static final Color ORANGE   = (new Color(254, 171,  16));
    static final Color GREEN    = (new Color( 97, 251,  13));
    static final Color CYAN     = (new Color( 12, 250, 250));
    
    // width palette
    static final double W1 = 1.5;
    static final double W2 = 2.5;
    static final double W3 = 3.5;
    
    static final java.util.List<Character> color_palette_hotkeys =
      (Collections.unmodifiableList
       (Arrays.asList
        ('b', 'w', 'r', 'l', 't', 'v', 'f', 'o', 'g', 'c')));
    
    static final java.util.List<Color> color_palette =
      (Collections.unmodifiableList
       (Arrays.asList
        (BLACK, WHITE, RUST, LEAF, TEAL, VIOLET, FUCHSIA, ORANGE, GREEN, CYAN)));
    
    static final java.util.List<Character> width_palette_hotkeys =
      (Collections.unmodifiableList
       (Arrays.asList
        ('1', '2', '3')));
    
    static final java.util.List<Float> width_palette_float =
      (Collections.unmodifiableList
       (Arrays.asList
        (((float)(W1)), ((float)(W2)), ((float)(W3)))));
    
    static final java.util.List<Double> width_palette_double =
      (Collections.unmodifiableList
       (Arrays.asList
        (((double)(W1)), ((double)(W2)), ((double)(W3)))));
    
    static final java.util.List<BasicStroke> width_palette_stroke = (new ArrayList<BasicStroke>());
    
    static {
      for (float width : width_palette_float) {
        width_palette_stroke.add((new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
      }
    }
    
    static final int DEFAULT_BRUSH_COLOR_INDEX = 0; /* = BLACK */
    static final int DEFAULT_BRUSH_WIDTH_INDEX = 1; /* = 4.0   */
    
    static final int DEFAULT_AUTOMARGIN_COLOR_INDEX = 0; /* = BLACK */
    static final int DEFAULT_BACKGROUND_COLOR_INDEX = 1; /* = WHITE */
    
    static final int DEFAULT_ERASE_COLOR_INDEX = 1; /* = WHITE */
  }
  
  /***
   * client actions:
   * 
   * S - set the brush attributes (color and width). the brush
   * attributes are global (as opposed to per-screen).
   * 
   * M - move the cursor to the given point. the cursor location is
   * global (as opposed to per-screen).
   * 
   * F - if there is not an open path in the current screen, open a
   * new (empty) path in the current screen, with the current
   * brush attributes.
   * 
   * C - if there is an open path in the current screen, close it.
   * 
   * A - if there is an open path in the current screen, append the
   * current cursor location to it.
   * 
   * X - remove the closed path with the given index
   * 
   * V - switch to the given screen.
   * 
   * event semantics:
   * 
   * backdrop: C, then V.
   * 
   * brush: S.
   * 
   * point: M, then A.
   * 
   * erase: X.
   * 
   * first: M, then F, then A.
   * 
   * close: M, then C. (don't append the close point to the path; this allows for an easy clean reset)
   ***/
  public static abstract class InputEvent
  {
    public static abstract class Visitor
    {
      // basic::synthetic::SoftReset
      // basic::synthetic::EventBundle
      // basic::synthetic::BackdropLoaded
      // basic::synthetic::BlankBackdropLoaded
      // basic::synthetic::EraseClick
      // basic::organic::global::BackdropChange
      // basic::organic::global::InsertBlankBackdrop
      // basic::organic::global::BrushAttributes
      // basic::organic::screen::ErasePath
      // basic::organic::screen::tablet::Point
      // basic::organic::screen::tablet::First
      // basic::organic::screen::tablet::Close
      
      // *
      public void handleBasicInputEvent (BasicInputEvent event) { throw null; /* unhandled event */ }
      
      // basic::*
      public void handleSyntheticInputEvent (SyntheticInputEvent event) { handleBasicInputEvent(event); }
      public void handleOrganicInputEvent   (OrganicInputEvent   event) { handleBasicInputEvent(event); }
      
      // basic::synthetic::+
      public void handleSoftResetInputEvent      (SoftResetInputEvent      event) { handleSyntheticInputEvent(event); }
      public void handleEventBundleInputEvent    (EventBundleInputEvent    event) { handleSyntheticInputEvent(event); }
      public void handleBackdropLoadedInputEvent (BackdropLoadedInputEvent event) { handleSyntheticInputEvent(event); }
      public void handleEraseClickInputEvent     (EraseClickInputEvent     event) { handleSyntheticInputEvent(event); }
      
      // basic::organic::*
      public void handleGlobalInputEvent (GlobalInputEvent event) { handleOrganicInputEvent(event); }
      public void handleScreenInputEvent (ScreenInputEvent event) { handleOrganicInputEvent(event); }
      
      // basic::global::+
      public void handleBackdropChangeInputEvent  (BackdropChangeInputEvent  event) { handleGlobalInputEvent(event); }
      public void handleBrushAttributesInputEvent (BrushAttributesInputEvent event) { handleGlobalInputEvent(event); }
      
      // basic::screen::+
      public void handleErasePathInputEvent (ErasePathInputEvent event) { handleScreenInputEvent(event); }
      
      // basic::screen::*
      public void handleTabletInputEvent (TabletInputEvent event) { handleScreenInputEvent(event); }
      
      // basic::screen::tablet::+
      public void handlePointTabletInputEvent (PointTabletInputEvent event) { handleTabletInputEvent(event); }
      public void handleFirstTabletInputEvent (FirstTabletInputEvent event) { handleTabletInputEvent(event); }
      public void handleCloseTabletInputEvent (CloseTabletInputEvent event) { handleTabletInputEvent(event); }
    }
    
    /***
     * (package-private) nullary constructor.
     ***/
    InputEvent()
    {
      // nothing to do
    }
    
    public abstract void accept(Visitor visitor);
  }
  
  public static abstract class BasicInputEvent extends InputEvent
  {
    /***
     * (package-private) constructor.
     ***/
    BasicInputEvent()
    {
      super();
    }
  }
  
  public static abstract class SyntheticInputEvent extends BasicInputEvent
  {
    /***
     * (package-private) constructor.
     ***/
    SyntheticInputEvent()
    {
      super();
    }
  }
  
  public static abstract class OrganicInputEvent extends BasicInputEvent
  {
    public final long ustc;
    
    /***
     * (package-private) constructor. ustc - client-side microsecond
     * timecode at which event should be considered effected
     ***/
    OrganicInputEvent(long ustc)
    {
      super();
      
      this.ustc = ustc;
    }
    
    public abstract OrganicInputEvent reclock(long ustc);
    
    public static final class EncodeVisitor extends InputEvent.Visitor
    {
      private Buffer.nI out;
      
      private long lktc;
      
      public void setOutputTarget(Buffer.nI out, long lktc)
      {
        this.out = out;
        
        this.lktc = lktc;
      }
      
      private void emitTimeCode(long ustc)
      {
        if (ustc > lktc) {
          out.aI(((int)(ustc - lktc)));
          lktc = ustc;
        } else {
          out.aI(0);
        }
      }
      
      private void emitTimeCode(OrganicInputEvent event)
      {
        emitTimeCode(event.ustc);
      }
      
      public void handleBackdropChangeInputEvent(BackdropChangeInputEvent event)
      {
        out.aI(BackdropChangeInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(event.index);
      }
      
      public void handleBrushAttributesInputEvent(BrushAttributesInputEvent event)
      {
        out.aI(BrushAttributesInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(event.color_index);
        out.aI(event.width_index);
      }
      
      public void handleErasePathInputEvent(ErasePathInputEvent event)
      {
        out.aI(ErasePathInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(event.index);
      }
      
      public void handlePointTabletInputEvent(PointTabletInputEvent event)
      {
        out.aI(PointTabletInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(((int)(Math.round(event.y))));
        out.aI(((int)(Math.round(event.x))));
      }
      
      public void handleFirstTabletInputEvent(FirstTabletInputEvent event)
      {
        out.aI(FirstTabletInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(((int)(Math.round(event.y))));
        out.aI(((int)(Math.round(event.x))));
      }
      
      public void handleCloseTabletInputEvent(CloseTabletInputEvent event)
      {
        out.aI(CloseTabletInputEvent.SERIAL);
        emitTimeCode(event);
        out.aI(((int)(Math.round(event.y))));
        out.aI(((int)(Math.round(event.x))));
      }
    }
    
    public static final class DecodeCreator
    {
      private Buffer.oI inp;
      
      private long lktc;
      
      public void setInputSource(Buffer.oI inp, long lktc)
      {
        this.inp = inp;
        
        this.lktc = lktc;
      }
      
      public int remaining()
      {
        return inp.remaining();
      }
      
      private long scanTimeCode()
      {
        lktc += (((long)(inp.rI())) & 0x00000000FFFFFFFFL);
        return lktc;
      }
      
      public OrganicInputEvent decode()
      {
        switch (inp.rI()) {
        case BackdropChangeInputEvent.SERIAL:
          return (new BackdropChangeInputEvent(scanTimeCode(), inp.rI()));
          
        case BrushAttributesInputEvent.SERIAL:
          return (new BrushAttributesInputEvent(scanTimeCode(), inp.rI(), inp.rI()));
          
        case ErasePathInputEvent.SERIAL:
          return (new ErasePathInputEvent(scanTimeCode(), inp.rI()));
          
        case PointTabletInputEvent.SERIAL:
          return (new PointTabletInputEvent(scanTimeCode(), inp.rI(), inp.rI()));
          
        case FirstTabletInputEvent.SERIAL:
          return (new FirstTabletInputEvent(scanTimeCode(), inp.rI(), inp.rI()));
          
        case CloseTabletInputEvent.SERIAL:
          return (new CloseTabletInputEvent(scanTimeCode(), inp.rI(), inp.rI()));
          
        default:
          throw null;
        }
      }
      
      public static abstract class TimecodeAdjuster
      {
        public abstract long invoke(long ustc);
      }
      
      public OrganicInputEvent decode(TimecodeAdjuster adjuster)
      {
        switch (inp.rI()) {
        case BackdropChangeInputEvent.SERIAL:
          return (new BackdropChangeInputEvent(adjuster.invoke(scanTimeCode()), inp.rI()));
          
        case BrushAttributesInputEvent.SERIAL:
          return (new BrushAttributesInputEvent(adjuster.invoke(scanTimeCode()), inp.rI(), inp.rI()));
          
        case ErasePathInputEvent.SERIAL:
          return (new ErasePathInputEvent(adjuster.invoke(scanTimeCode()), inp.rI()));
          
        case PointTabletInputEvent.SERIAL:
          return (new PointTabletInputEvent(adjuster.invoke(scanTimeCode()), inp.rI(), inp.rI()));
          
        case FirstTabletInputEvent.SERIAL:
          return (new FirstTabletInputEvent(adjuster.invoke(scanTimeCode()), inp.rI(), inp.rI()));
          
        case CloseTabletInputEvent.SERIAL:
          return (new CloseTabletInputEvent(adjuster.invoke(scanTimeCode()), inp.rI(), inp.rI()));
          
        default:
          throw null;
        }
      }
    }
  }
  
  public static abstract class GlobalInputEvent extends OrganicInputEvent
  {
    /***
     * (package-private) constructor.
     ***/
    GlobalInputEvent(long ustc)
    {
      super(ustc);
    }
  }
  
  public static abstract class ScreenInputEvent extends OrganicInputEvent
  {
    /***
     * (package-private) constructor.
     ***/
    ScreenInputEvent(long ustc)
    {
      super(ustc);
    }
  }
  
  public static abstract class TabletInputEvent extends ScreenInputEvent
  {
    public final double y;
    public final double x;
    
    private double reasonableCoordinate(double c)
    {
      if (c < 0.1)     c = 0.0;
      if (c > 65534.9) c = 65535.0;
      
      return c;
    }
    
    /***
     * (package-private) constructor. y, x - the location of the event
     ***/
    TabletInputEvent(long ustc, double y, double x)
    {
      super(ustc);
      
      this.y = reasonableCoordinate(y);
      this.x = reasonableCoordinate(x);
    }
  }
  
  public static final class SoftResetInputEvent extends SyntheticInputEvent
  {
    public SoftResetInputEvent()
    {
      super();
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleSoftResetInputEvent(this);
    }
  }
  
  public static final class EventBundleInputEvent extends SyntheticInputEvent
  {
    public final ArrayList<Buffer.oB> payloads;
    
    public EventBundleInputEvent(ArrayList<Buffer.oB> payloads)
    {
      super();
      this.payloads = payloads;
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleEventBundleInputEvent(this);
    }
  }
  
  public static final class BackdropLoadedInputEvent extends SyntheticInputEvent
  {
    public final int index;
    public final byte[] backdrop;
    
    /***
     * index - number of backdrop whose image has loaded. backdrop -
     * png-encoded image bytes. the <code>encoded</code> array should
     * not subsequently be modified by anyone.
     ***/
    public BackdropLoadedInputEvent(int index, byte[] backdrop)
    {
      super();
      
      this.index = index;
      this.backdrop = backdrop;
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleBackdropLoadedInputEvent(this);
    }
  }
  
  public static final class EraseClickInputEvent extends SyntheticInputEvent
  {
    public final long ustc;
    
    public final double y;
    public final double x;
    
    public final double r;
    
    public final LinkedBlockingQueue<OrganicInputEvent> capture;
    
    /***
     * ustc - mouse click timecode, y - click coordinate, x - click
     * coordinate, r - eraser "radius", capture - queue.
     ***/
    public EraseClickInputEvent(long ustc, double y, double x, double r, LinkedBlockingQueue<OrganicInputEvent> capture)
    {
      super();
      
      this.ustc = ustc;
      
      this.y = y;
      this.x = x;
      
      this.r = r;
      
      this.capture = capture;
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleEraseClickInputEvent(this);
    }
  }
  
  public static final class BackdropChangeInputEvent extends GlobalInputEvent
  {
    public static final int SERIAL = 1;
    
    public final int index;
    
    /***
     * index - number of backdrop to switch to.
     ***/
    public BackdropChangeInputEvent(long ustc, int index)
    {
      super(ustc);
      
      this.index = index;
    }
    
    public BackdropChangeInputEvent reclock(long ustc)
    {
      return (new BackdropChangeInputEvent(ustc, index));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleBackdropChangeInputEvent(this);
    }
    
    public String toString()
    {
      return "BackdropChangeInputEvent(ustc=" + ustc + ", index=" + index + ")";
    }
  }
  
  public static final class BrushAttributesInputEvent extends GlobalInputEvent
  {
    public static final int SERIAL = 2;
    
    public final int color_index;
    public final int width_index;
    
    /***
     * index - number of backdrop to switch to. this should also swap
     * the path set with that mainted for the target backdrop. except:
     * any currently-being-drawn path should continue drawing on
     * whichever backdrop it commenced drawing on (this should not
     * happen in real usage).
     ***/
    public BrushAttributesInputEvent(long ustc, int color_index, int width_index)
    {
      super(ustc);
      
      this.color_index = color_index;
      this.width_index = width_index;
    }
    
    public BrushAttributesInputEvent reclock(long ustc)
    {
      return (new BrushAttributesInputEvent(ustc, color_index, width_index));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleBrushAttributesInputEvent(this);
    }
    
    public String toString()
    {
      return "BrushAttributesInputEvent(ustc=" + ustc + ", color_index=" + color_index + ", width_index=" + width_index + ")";
    }
  }
  
  public static final class ErasePathInputEvent extends ScreenInputEvent
  {
    public static final int SERIAL = 3;
    
    public final int index;
    
    /***
     * index - index of the close path to be erased
     ***/
    public ErasePathInputEvent(long ustc, int index)
    {
      super(ustc);
      
      this.index = index;
    }
    
    public ErasePathInputEvent reclock(long ustc)
    {
      return (new ErasePathInputEvent(ustc, index));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleErasePathInputEvent(this);
    }
    
    public String toString()
    {
      return "ErasePathInputEvent(ustc=" + ustc + ", index=" + index + ")";
    }
  }
  
  public static final class PointTabletInputEvent extends TabletInputEvent
  {
    public static final int SERIAL = 4;
    
    public PointTabletInputEvent(long ustc, double y, double x)
    {
      super(ustc, y, x);
    }
    
    public PointTabletInputEvent reclock(long ustc)
    {
      return (new PointTabletInputEvent(ustc, y, x));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handlePointTabletInputEvent(this);
    }
    
    public String toString()
    {
      return "PointTabletInputEvent(ustc=" + ustc + ", y=" + y + ", x=" + x + ")";
    }
  }
  
  public static final class FirstTabletInputEvent extends TabletInputEvent
  {
    public static final int SERIAL = 5;
    
    /***
     * c - color
     * w - stroke width
     ***/
    public FirstTabletInputEvent(long ustc, double y, double x)
    {
      super(ustc, y, x);
    }
    
    public FirstTabletInputEvent reclock(long ustc)
    {
      return (new FirstTabletInputEvent(ustc, y, x));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleFirstTabletInputEvent(this);
    }
    
    public String toString()
    {
      return "FirstTabletInputEvent(ustc=" + ustc + ", y=" + y + ", x=" + x + ")";
    }
  }
  
  public static final class CloseTabletInputEvent extends TabletInputEvent
  {
    public static final int SERIAL = 6;
    
    public CloseTabletInputEvent(long ustc, double y, double x)
    {
      super(ustc, y, x);
    }
    
    public CloseTabletInputEvent reclock(long ustc)
    {
      return (new CloseTabletInputEvent(ustc, y, x));
    }
    
    public void accept(InputEvent.Visitor visitor)
    {
      visitor.handleCloseTabletInputEvent(this);
    }
    
    public String toString()
    {
      return "CloseTabletInputEvent(ustc=" + ustc + ", y=" + y + ", x=" + x + ")";
    }
  }
  
  static class Global
  {
    /***
     * enter global state variables
     ***/
    
    // brush attributes
    int color_index;
    int width_index;
    
    // current screen index
    int screen_index;
    int screen_index_maximum;
    
    // cursor location
    double y;
    double x;
    
    /***
     * leave global state variables
     ***/
    
    // for capture
    boolean capturing;
    
    // for optimization
    long redraw_timestamp = 0;
    long redraw_requested = 0;
    long redraw_ratcheted  = 0;
    long redraw_scheduled = 0;
    long redraw_confirmed = 0;
    int  redraw_iterative = 0;
    boolean rendered_image_write_enable = true;
    BufferedImage rendered_image;
    BufferedImage decoded_backdrop = null;
    byte[] decoded_backdrop_source = null;
    
    // for intermediate-point interpolation
    long prev_event_ustc;
    
    Global(int H, int W)
    {
      reset(H, W);
    }
    
    void reset(int H, int W)
    {
      color_index = ModeSelect.DEFAULT_BRUSH_COLOR_INDEX;
      width_index = ModeSelect.DEFAULT_BRUSH_WIDTH_INDEX;
      
      screen_index = 0;
      screen_index_maximum = 0;
      
      y = 0;
      x = 0;
      
      capturing = false;
      
      rendered_image = (new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB));
      
      decoded_backdrop = null;
      decoded_backdrop_source = null;
      
      prev_event_ustc = MicroTime.now();
    }
  }
  
  static class ClosedPath
  {
    final int color_index;
    final int width_index;
    
    final GeneralPath shape;

    ClosedPath(int color_index, int width_index, GeneralPath shape)
    {
      this.color_index = color_index;
      this.width_index = width_index;
      
      this.shape = shape;
    }
  }
  
  static class Screen
  {
    /***
     * enter screen state variables
     ***/
    
    // closed paths data
    final ArrayList<ClosedPath> closed_paths = (new ArrayList<ClosedPath>());
    
    // open path data
    boolean                         open_path_exists;
    int                             open_path_color_index;
    int                             open_path_width_index;
    final ArrayList<Point2D.Double> open_path_shape       = (new ArrayList<Point2D.Double>());
    
    /***
     * leave screen state variables
     ***/
    
    // fixed backdrop image (png encoded)
    byte[] backdrop = null;
    
    Screen()
    {
      reset();
    }
    
    void reset()
    {
      closed_paths.clear();
      
      open_path_exists = false;
      open_path_color_index = -1;
      open_path_width_index = -1;
      open_path_shape.clear();
      
      //backdrop = null;
    }
  }
  
  class Compositor extends JComponent
  {
    final int H, W;
    // for average hertz
    final long beginning_of_time = System.nanoTime();
    int update_count = 0;
    
    // for instantanous hertz
    long prev_paint_exited = System.nanoTime();
    
    Compositor(int H, int W)
    {
      this.H = H;
      this.W = W;
      
      {
        Dimension extent = (new Dimension(W, H));
        
        setSize(extent);
        setMinimumSize(extent);
        setPreferredSize(extent);
      }
      
      setOpaque(true);
      
      if (globals.capturing) {
        (new Runnable()
          {
            public void run() {
              final int H = 14-1;
              final int W = 14-1;
              
              BufferedImage image = (new BufferedImage(H, W, BufferedImage.TYPE_INT_ARGB));
              
              {
                for (int y = 0; y < H; y++) {
                  for (int x = 0; x < W; x++) {
                    image.setRGB(x, y, 0x00000000);
                  }
                }
                
                {
                  for (int x = 0; x < W; x++) {
                    image.setRGB(x,     0, 0xFF000000);
                    image.setRGB(x, (H-1), 0xFF000000);
                  }
                  
                  for (int y = 0; y < H; y++) {
                    image.setRGB(    0, y, 0xFF000000);
                    image.setRGB((W-1), y, 0xFF000000);
                  }
                }
              }
              
              setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, (new Point(((W >> 1) + 0), ((H >> 1) + 0))), "romahair"));
              //setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
          }).run();
      }
    }
    
    private static final int REDRAW_MINIMUM_INTERVAL_US   = 500000; /* 1/2 sec */
    private static final int REDRAW_ITERATION_DEADLINE_US =   8000; /* half of 1/60 seconds, the highest expected framerate */
    
    private Graphics2D incrementalGraphicsCache = null;
    
    public void paintMicroTime(Graphics g, long now)
    {
      processEvents(now);
      
      final Screen screen = screens.get(globals.screen_index);
      
      if (globals.redraw_confirmed < globals.redraw_requested) {
        if (globals.redraw_ratcheted < globals.redraw_requested) {
          globals.redraw_timestamp = (now + REDRAW_MINIMUM_INTERVAL_US);
          globals.redraw_ratcheted = globals.redraw_requested;
        } else {
          if (now > globals.redraw_timestamp) {
            if (globals.redraw_scheduled != globals.redraw_requested) {
              globals.redraw_scheduled = globals.redraw_requested;
              globals.redraw_iterative = 0;
              
              if (incrementalGraphicsCache != null) {
                incrementalGraphicsCache.dispose();
              }
              
              incrementalGraphicsCache = globals.rendered_image.createGraphics();
              
              //Log.log("lock!");
            }
            
            long deadline = (now + REDRAW_ITERATION_DEADLINE_US);
            
            int redraw_iterative_shadow = globals.redraw_iterative;
            
            {
              Graphics2D g2d = incrementalGraphicsCache;
              
              decodeBackdrop();
              
              if (globals.redraw_iterative == 0) {
                g2d.setColor(ModeSelect.color_palette.get(ModeSelect.DEFAULT_BACKGROUND_COLOR_INDEX));
                g2d.fillRect(0, 0, W, H);
                
                if (globals.decoded_backdrop != null) {
                  g2d.drawImage(globals.decoded_backdrop, 0, 0, null);
                }
              }
              
              while (globals.redraw_iterative < screen.closed_paths.size()) {
                ClosedPath path = screen.closed_paths.get(globals.redraw_iterative++);
                drawClosedPath(g2d, path);
                
                if (MicroTime.now() > deadline) {
                  break;
                }
              }
              
              if (globals.redraw_iterative == screen.closed_paths.size()) {
                globals.redraw_confirmed = globals.redraw_scheduled;
                
                incrementalGraphicsCache.dispose();
                incrementalGraphicsCache = null;
              }
            }
            
            long current = MicroTime.now();
            
            /*
            if (current > deadline) {
              Log.log("deadline exceeded by " + (current - deadline) + " (did " + (globals.redraw_iterative - redraw_iterative_shadow) + ")");
            }
            */
            
            // don't redraw again for as long as we redrew this iteration
            globals.redraw_timestamp = (current + (current - now));
          }
        }
        
        return;
      }
      
      Graphics2D g2d = ((Graphics2D)(g.create()));
      
      int aH;
      int aW;
      
      int mT;
      int mB;
      int mL;
      int mR;
      
      // calculate "apparent" dimensions (for centering) and margins
      {
        aH = Math.max(getHeight(), H);
        aW = Math.max(getWidth(),  W);
        
        mT = ((aH - H) >> 1);
        mB = aH - mT;
        
        mL = ((aW - W) >> 1);
        mR = aW - mL;
        
        // except force top-left if we're doing event capture
        if (globals.capturing) {
          mB += mT;
          mT = 0;
          
          mR += mL;
          mL = 0;
        }
      }
      
      // fulfill opacity guarantee before anything else
      {
        g2d.setColor(ModeSelect.color_palette.get(ModeSelect.DEFAULT_AUTOMARGIN_COLOR_INDEX));
        g2d.fillRect(0, 0, getWidth(), getHeight());
      }
      
      // allocate "centered" graphics context
      Graphics2D g2d2 = ((Graphics2D)(g2d.create(mL, mT, W, H)));
      
      // draw backdrop plus closed curves
      //g2d.drawImage(globals.rendered_image, mL, mT, null);
      g2d2.drawImage(globals.rendered_image, 0, 0, null);
      
      Point2D.Double interpolated = (new Point2D.Double(globals.x, globals.y));
      
      // interpolate, if possible (otherwise stick with the last known cursor position)
      {
        OrganicInputEvent event = pending_organic.peek();
        
        if (event instanceof TabletInputEvent) {
          TabletInputEvent tablet = ((TabletInputEvent)(event));
          
          double delta = 1e-3;
          
          final int maximum_interpolation_interval_us = 500000; // 0.5 seconds
          
          if ((globals.prev_event_ustc < (now - delta)) && ((now + delta) < tablet.ustc) && (tablet.ustc < (now + maximum_interpolation_interval_us))) {
            double t = (((double)(tablet.ustc - now)) / ((double)(tablet.ustc - globals.prev_event_ustc)));
            
            double interpolated_y = ((t * globals.y) + ((1.0 - t) * tablet.y));
            double interpolated_x = ((t * globals.x) + ((1.0 - t) * tablet.x));
            
            interpolated = (new Point2D.Double(interpolated_x, interpolated_y));
          }
        }
      }
      
      // draw the open path
      {
        // enable anti-aliasing
        g2d2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (screen.open_path_shape.size() > 0) {
          screen.open_path_shape.add(interpolated);
          
          {
            GeneralPath path = completeCRSpline(screen.open_path_shape);
            
            if (path != null) {
              g2d2.setColor(ModeSelect.color_palette.get(screen.open_path_color_index));
              g2d2.setStroke((new BasicStroke(ModeSelect.width_palette_float.get(screen.open_path_width_index), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)));
              g2d2.draw(path);
            }
          }
          
          screen.open_path_shape.remove(screen.open_path_shape.size() - 1);
        }
      }
      
      // draw cursor
      {
        double cursor_x = interpolated.x;
        double cursor_y = interpolated.y;
        
        if (globals.capturing) {
          cursor_x = 16;
          cursor_y = 16;
        }
        
        double outer_radius = 8.0;
        double inner_radius = 4.0;
        
        g2d2.setColor(ModeSelect.BLACK);
        g2d2.fill((new Ellipse2D.Double(cursor_x - outer_radius, cursor_y - outer_radius, 2 * outer_radius, 2 * outer_radius)));
        
        g2d2.setColor(ModeSelect.color_palette.get(globals.color_index));
        g2d2.fill((new Ellipse2D.Double(cursor_x - inner_radius, cursor_y - inner_radius, 2 * inner_radius, 2 * inner_radius)));
      }
      
      // for consistency, blank out margins
      /*
      {
        g2d.setColor(ModeSelect.color_palette.get(ModeSelect.DEFAULT_AUTOMARGIN_COLOR_INDEX));
        g2d.fillRect(       0,        0, aW, mT); // top    band
        g2d.fillRect(       0, (mT + H), aW, mB); // bottom band
        g2d.fillRect(       0,        0, mL, aH); // left   band
        g2d.fillRect((mL + W),        0, mR, aW); // right  band
      }
      */
      
      g2d2.dispose();
      g2d.dispose();
      
      /*
      long exited = System.nanoTime();
      long elapsed = exited - prev_paint_exited;
      prev_paint_exited = exited;
      double instant_hz = 1e9 / elapsed;
      update_count++;
      double average_hz = 1e9 * update_count / (exited - beginning_of_time);
      */
    }
    
    public void paint(Graphics g)
    {
      try {
        paintMicroTime(g, MicroTime.now());
      } catch (Throwable e) {
        Log.log(e);
      }
    }
  }
  
  /***
   * actual Etch class body begins here
   ***/
  
  private static final double DISTINCT_POINT_TOLERANCE = 0.5;
  private static final double POINT_DUPLICATION_OFFSET = 0.1;
  
  private final ArrayList<F0> detachments = (new ArrayList<F0>());
  
  private final int H, W;
  
  private final Global globals;
  private final InfiniteArray<Screen> screens = (new InfiniteArray<Screen>((new F0<Screen>() { public Screen invoke() { return (new Screen()); } })));
  
  private final ConcurrentLinkedQueue<SyntheticInputEvent> pending_synthetic = (new ConcurrentLinkedQueue<SyntheticInputEvent> ());
  private final ConcurrentLinkedQueue<OrganicInputEvent>   pending_organic   = (new ConcurrentLinkedQueue<OrganicInputEvent>   ());
  
  private final ConcurrentLinkedQueue<Etch.EventBundleInputEvent> pending_return_bundle = (new ConcurrentLinkedQueue<Etch.EventBundleInputEvent>());
  
  private final Compositor compositor;
  private final JComponent interfaceElement;
  private final javax.swing.Timer repaintTimer;
  
  static void drawClosedPath(Graphics2D g2d, ClosedPath path)
  {
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor(ModeSelect.color_palette.get(path.color_index));
    g2d.setStroke(ModeSelect.width_palette_stroke.get(path.width_index));
    g2d.draw(path.shape);
  }
  
  final InputEvent.Visitor visitor =
    (new InputEvent.Visitor()
      {
        private void _S(int color_index, int width_index)
        {
          globals.color_index = color_index;
          globals.width_index = width_index;
        }
        
        private void _M(double y, double x)
        {
          globals.y = y;
          globals.x = x;
        }
        
        private void _F()
        {
          Screen screen = screens.get(globals.screen_index);
          
          if (!(screen.open_path_exists)) {
            screen.open_path_exists = true;
            screen.open_path_color_index = globals.color_index;
            screen.open_path_width_index = globals.width_index;
          }
        }
        
        private void _C()
        {
          Screen screen = screens.get(globals.screen_index);
          
          if (screen.open_path_exists) {
            {
              GeneralPath shape = completeCRSpline(screen.open_path_shape);
              
              if (shape != null) {
                ClosedPath path = (new ClosedPath(screen.open_path_color_index, screen.open_path_width_index, shape));
                screen.closed_paths.add(path);
                
                if (globals.rendered_image_write_enable) {
                  Graphics2D g2d = globals.rendered_image.createGraphics();
                  drawClosedPath(g2d, path);
                  g2d.dispose();
                }
              }
            }
            
            screen.open_path_exists = false;
            screen.open_path_shape.clear();
          }
        }
        
        private void _A()
        {
          Screen screen = screens.get(globals.screen_index);
          
          if (screen.open_path_exists) {
            screen.open_path_shape.add((new Point2D.Double(globals.x, globals.y)));
          }
        }
        
        private void _X(int index)
        {
          Screen screen = screens.get(globals.screen_index);
          
          if ((0 <= index) && (index < screen.closed_paths.size())) {
            screen.closed_paths.remove(index);
          }
        }
        
        private void _V(int index)
        {
          globals.screen_index = index;
          globals.screen_index_maximum = Math.max(globals.screen_index_maximum, index);
          
          globals.redraw_requested++;
        }
        
        public void handleSoftResetInputEvent(SoftResetInputEvent event)
        {
          pending_organic.clear();
          
          for (int i = 0; i <= globals.screen_index_maximum; i++) {
            screens.get(i).reset();
          }
          
          globals.reset(H, W);
          
          _V(0); // synthetic
        }
        
        private final BufferCentral central = (new BufferCentral(9));
        
        public void handleEventBundleInputEvent(EventBundleInputEvent event)
        {
          globals.rendered_image_write_enable = false;
          
          // purge organic queue
          {
            OrganicInputEvent single_event;
            
            while ((single_event = pending_organic.poll()) != null) {
              single_event.accept(visitor);
              globals.prev_event_ustc = single_event.ustc;
            }
          }
          
          // work through payloads
          {
            Etch.OrganicInputEvent.DecodeCreator decoder = (new Etch.OrganicInputEvent.DecodeCreator());
            
            for (Buffer.oB payload_o : event.payloads) {
              Buffer.xI tmp = central.acquireI();
              
              NaturalNumberCodec.decode_remote_source(central, tmp, payload_o);
              
              Buffer.oI tmp_o = tmp.iterate();
              
              decoder.setInputSource(tmp_o, 0);
              
              while (decoder.remaining() > 0) {
                decoder.decode().accept(visitor);
              }
            }
          }
          
          // return the event
          pending_return_bundle.add(event);
          
          globals.rendered_image_write_enable = true;
          
          _V(globals.screen_index); // synthetic; injected here in order to recalculate rendered image
        }
        
        public void handleBackdropLoadedInputEvent(BackdropLoadedInputEvent event)
        {
          screens.get(event.index).backdrop = event.backdrop;
          
          if (globals.screen_index == event.index) {
            _V(globals.screen_index); // synthetic; injected here in order to recalculate rendered image
          }
        }
        
        public void handleEraseClickInputEvent(EraseClickInputEvent event)
        {
          Rectangle2D.Double eraser = (new Rectangle2D.Double(event.x - event.r, event.y - event.r, (2 * event.r), (2 * event.r)));
          
          Screen screen = screens.get(globals.screen_index);
          
          boolean erased = false;
          
          for (int i = 0; i < screen.closed_paths.size(); i++) {
            GeneralPath path = screen.closed_paths.get(i).shape;
            
            if (path.intersects(eraser)) {
              event.capture.add((new ErasePathInputEvent(event.ustc, i)));
              _X(i);
              erased = true;
              i--; // reprocess same index (different path)
            }
          }
          
          if (erased) {
            _V(globals.screen_index); // synthetic; injected here in order to recalculate rendered image
          }
        }
        
        public void handleBackdropChangeInputEvent(BackdropChangeInputEvent event)
        {
          _C();
          _V(event.index);
        }
        
        public void handleBrushAttributesInputEvent(BrushAttributesInputEvent event)
        {
          _S(event.color_index, event.width_index);
        }
        
        public void handleErasePathInputEvent(ErasePathInputEvent event)
        {
          if (!globals.capturing) {
            _X(event.index);
            _V(globals.screen_index); // synthetic; injected here in order to recalculate rendered image
          }
        }
        
        public void handlePointTabletInputEvent(PointTabletInputEvent event)
        {
          _M(event.y, event.x);
          _A();
        }
        
        public void handleFirstTabletInputEvent(FirstTabletInputEvent event)
        {
          _M(event.y, event.x);
          _F();
          _A();
        }
        
        public void handleCloseTabletInputEvent(CloseTabletInputEvent event)
        {
          _M(event.y, event.x);
          _C();
        }
      });
  
  public Etch(final int H, final int W, final LinkedBlockingQueue<OrganicInputEvent> capture, int default_update_interval)
  {
    this.H = H;
    this.W = W;
    
    globals = (new Global(H, W));
    
    globals.capturing = (capture != null);
    
    pending_organic.add((new BackdropChangeInputEvent(0, 0)));
    
    compositor = (new Compositor(H, W));
    
    interfaceElement =
      (new JScrollPane
       (compositor,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
    
    interfaceElement.setLayout
      (new ScrollPaneLayout()
        {
          public void layoutContainer(Container parent)
          {
            super.layoutContainer(parent);
            
            JScrollPane scroll = ((JScrollPane)(parent));
            JViewport   window = scroll.getViewport();
            Component   client = window.getView();
            
            Dimension framing = (new Dimension(window.getSize()));
            
            framing.width  = Math.max(framing.width,  client.getMinimumSize().width );
            framing.height = Math.max(framing.height, client.getMinimumSize().height);
            
            if (!(client.getSize().equals(framing))) {
              client.setSize(framing);
            }
          }
        });
    
    if (default_update_interval > 0) {
      repaintTimer = (new javax.swing.Timer(default_update_interval, null));
      
      repaintTimer.addActionListener
        (new ActionListener()
          {
            ActionListener self = this;
            { detachments.add((new F0() { public Void invoke() { repaintTimer.removeActionListener(self); return null; } })); }
            
            public void actionPerformed(ActionEvent event)
            {
              compositor.repaint();
            }
          });
    } else {
      repaintTimer = null;
    }
    
    if (repaintTimer != null) {
      repaintTimer.start();
    }
    
    if (capture != null) {
      class Generator
      {
        private static final double eraser_radius = 4.0;
        
        boolean in_erase_mode = false;
        
        int color_index = ModeSelect.DEFAULT_BRUSH_COLOR_INDEX;
        int width_index = ModeSelect.DEFAULT_BRUSH_WIDTH_INDEX;
        
        void attach(Compositor compositor)
        {
          compositor.setFocusable(true);
          
          compositor.addKeyListener
            ((new KeyAdapter()
              {
                public void keyTyped(KeyEvent event)
                {
                  long now = MicroTime.now();
                  
                  {
                    int idx;
                    
                    if ((idx = ModeSelect.color_palette_hotkeys.indexOf(event.getKeyChar())) >= 0) {
                      color_index = idx;
                      
                      if (!in_erase_mode) {
                        capture.add((new BrushAttributesInputEvent(now, color_index, width_index)));
                      }
                    }
                  }
                  
                  {
                    int idx;
                    
                    if ((idx = ModeSelect.width_palette_hotkeys.indexOf(event.getKeyChar())) >= 0) {
                      width_index = idx;
                      
                      if (!in_erase_mode) {
                        capture.add((new BrushAttributesInputEvent(now, color_index, width_index)));
                      }
                    }
                  }
                  
                  switch (event.getKeyChar()) {
                  case 'p': // previous page
                    {
                      int i = (globals.screen_index - 1);
                      if (i < 0) i = 0;
                      capture.add((new BackdropChangeInputEvent(now, i)));
                      break;
                    }
                    
                  case 'n': // next page
                    {
                      int i = (globals.screen_index + 1);
                      capture.add((new BackdropChangeInputEvent(now, i)));
                      break;
                    }
                    
                  case 'e': // erase toggle
                    {
                      if (in_erase_mode) {
                        in_erase_mode = false;
                        capture.add((new BrushAttributesInputEvent(now, color_index, width_index)));
                      } else {
                        in_erase_mode = true;
                        capture.add((new BrushAttributesInputEvent(now, ModeSelect.DEFAULT_ERASE_COLOR_INDEX, width_index)));
                      }
                      
                      break;
                    }
                  }
                }
              }));
          
          compositor.addMouseListener
            ((new MouseAdapter()
              {
                void normalModeMousePressed(MouseEvent event)
                {
                  capture.add((new FirstTabletInputEvent(MicroTime.now(), event.getY(), event.getX())));
                }
                
                void eraseModeMousePressed(MouseEvent event)
                {
                  pending_synthetic.add((new EraseClickInputEvent(MicroTime.now(), event.getY(), event.getX(), eraser_radius, capture)));
                }
                
                public void mousePressed(MouseEvent event)
                {
                  if (in_erase_mode) {
                    eraseModeMousePressed(event);
                  } else {
                    normalModeMousePressed(event);
                  }
                }
                
                public void mouseReleased(MouseEvent event)
                {
                  capture.add((new CloseTabletInputEvent(MicroTime.now(), event.getY(), event.getX())));
                }
              }));
          
          compositor.addMouseMotionListener
            ((new MouseAdapter()
              {
                public void mouseDragged(MouseEvent event)
                {
                  mouseMoved(event);
                  
                  if (in_erase_mode) {
                    pending_synthetic.add((new EraseClickInputEvent(MicroTime.now(), event.getY(), event.getX(), eraser_radius, capture)));
                  }
                }
                
                public void mouseMoved(MouseEvent event)
                {
                  capture.add((new PointTabletInputEvent(MicroTime.now(), event.getY(), event.getX())));
                }
              }));
        }
      }
      
      (new Generator()).attach(compositor);
    }
  }
  
  private static final float F(double x)
  {
    return ((float)(x));
  }
  
  private GeneralPath completeCRSpline(ArrayList<Point2D.Double> points)
  {
    // culling requires at least one point
    {
      if (points.size() < 1) {
        return null;
      }
    }
    
    // cull points
    {
      ArrayList<Point2D.Double> culled = (new ArrayList<Point2D.Double>());
      
      Point2D.Double prev = (points.get(0));
      culled.add(prev);
      
      for (Point2D.Double curr : points) {
        if (Math.max(Math.abs(curr.x - prev.x), Math.abs(curr.y - prev.y)) > DISTINCT_POINT_TOLERANCE) {
          culled.add(curr);
          prev = curr;
        }
      }
      
      points = culled;
    }
    
    // splining requires at least two points
    {
      if (points.size() < 2) {
        Point2D.Double prev = points.get(0);
        points.add((new Point2D.Double(prev.x + POINT_DUPLICATION_OFFSET, prev.y + POINT_DUPLICATION_OFFSET)));
      }
    }
    
    // perform splining
    {
      GeneralPath path = (new GeneralPath());
      
      int pointc = points.size();

      if (pointc == 2) {
        Point2D.Double p0, p1;
        
        p0 = points.get(0);
        p1 = points.get(1);
        
        path.moveTo(F(p0.x), F(p0.y));
        path.lineTo(F(p1.x), F(p1.y));
      } else {
        Point2D.Double p0, p1, p2, p3;
        
        // first polynomial segment
        p1 = points.get(0);
        p2 = points.get(1);
        p3 = points.get(2);
        path.moveTo(F(p1.x), F(p1.y));
        path.curveTo(F(p1.x + (p2.x - p1.x) / 3.0), F(p1.y + (p2.y - p1.y) / 3.0),
                     F(p2.x - (p3.x - p1.x) / 6.0), F(p2.y - (p3.y - p1.y) / 6.0),
                     F(p2.x),                       F(p2.y));
        
        // intermediate polynomial segments
        for (int i = 1; i < pointc - 2; i++) {
          p0 = p1;
          p1 = p2;
          p2 = p3;
          p3 = points.get(i + 2);
          
          path.curveTo(F(p1.x + (p2.x - p0.x) / 6.0), F(p1.y + (p2.y - p0.y) / 6.0),
                       F(p2.x - (p3.x - p1.x) / 6.0), F(p2.y - (p3.y - p1.y) / 6.0),
                       F(p2.x),                       F(p2.y));
        }
        
        // last polynomial segment
        p0 = points.get(pointc-3);
        p1 = points.get(pointc-2);
        p2 = points.get(pointc-1);
        path.curveTo(F(p1.x + (p2.x - p0.x) / 6.0), F(p1.y + (p2.y - p0.y) / 6.0),
                     F(p2.x - (p2.x - p1.x) / 3.0), F(p2.y - (p2.y - p1.y) / 3.0),
                     F(p2.x),                       F(p2.y));
      }
      
      return path;
    }
  }
  
  public JComponent getInterfaceElement()
  {
    return interfaceElement;
  }
  
  public void setUpdateInterval(int updateInterval)
  {
    if (repaintTimer != null) {
      repaintTimer.setDelay(updateInterval);
    }
  }
  
  public void submit(OrganicInputEvent event)
  {
    pending_organic.add(event);
  }
  
  public void submitSynthetic(SyntheticInputEvent event)
  {
    pending_synthetic.add(event);
  }
  
  public Etch.EventBundleInputEvent obtainReturnBundle()
  {
    return pending_return_bundle.poll();
  }
  
  public void submitGeneric(InputEvent event)
  {
    /****/ if (event instanceof OrganicInputEvent) {
      pending_organic.add(((OrganicInputEvent)(event)));
    } else if (event instanceof SyntheticInputEvent) {
      pending_synthetic.add(((SyntheticInputEvent)(event)));
    } else {
      throw null;
    }
  }
  
  public boolean hasPendingOrganic()
  {
    return (!(pending_organic.isEmpty()));
  }
  
  public long getPendingOrganicMicroTime()
  {
    OrganicInputEvent event = pending_organic.peek();
    
    if (event != null) {
      return event.ustc;
    } else {
      return -1;
    }
  }
  
  public void paintMicroTime(Graphics g, long now)
  {
    compositor.paintMicroTime(g, now);
  }
  
  private AtomicInteger acceptBackdropScreenIndex = (new AtomicInteger(0));
  
  public void appendBackdrop(byte[] backdrop)
  {
    pending_synthetic.add((new BackdropLoadedInputEvent(acceptBackdropScreenIndex.getAndIncrement(), backdrop)));
  }
  
  void processEvents(long now)
  {
    {
      SyntheticInputEvent event;
      
      // process at most one synthetic event
      while ((event = pending_synthetic.poll()) != null) {
        event.accept(visitor);
      }
    }
    
    {
      OrganicInputEvent event;
      
      while ((event = pending_organic.peek()) != null) {
        if (event.ustc > now) break;
        if (pending_organic.poll() != event) throw null;
        event.accept(visitor);
        globals.prev_event_ustc = event.ustc;
      }
      
      /*
      if (event == null) {
        Log.log("ETCH RAN DRY!");
      }
      */
    }
  }
  
  void decodeBackdrop()
  {
    final byte[] backdrop_source = screens.get(globals.screen_index).backdrop;
    
    if (globals.decoded_backdrop_source != backdrop_source) {
      if (backdrop_source == null) {
        globals.decoded_backdrop = null;
      } else {
        try {
          Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
          ImageReader reader = readers.next();
          ImageInputStream iis = ImageIO.createImageInputStream((new ByteArrayInputStream(backdrop_source)));
          reader.setInput(iis, true);
          globals.decoded_backdrop = reader.read(0);
        } catch (Exception e) {
          throw (new RuntimeException(e));
        }
      }
      
      globals.decoded_backdrop_source = backdrop_source;
    }
  }
  
  public void cleanup()
  {
    repaintTimer.stop();
    
    for (F0 action : detachments) {
      action.invoke();
    }
    
    detachments.clear();
  }
}

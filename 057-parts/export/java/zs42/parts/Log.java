/***
 * Log.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.PrintStream;

import java.util.concurrent.LinkedBlockingQueue;

public class Log
{
  public static abstract class EventHandler
  {
    public abstract void onMessage(long ustc, Thread thread, String message);
    public abstract void onException(long ustc, Thread thread, Throwable exception);
    public abstract void onAnnotatedException(long ustc, Thread thread, String message, Throwable exception);
    public abstract void onAbortException(long ustc, Thread thread, Throwable exception);
  }
  
  static abstract class Event
  {
    long ustc;
    
    Event()
    {
      ustc = MicroTime.now();
    }
    
    abstract void fire(EventHandler handler);
  }
  
  static final class MessageEvent extends Event
  {
    final Thread thread;
    final String message;
    
    MessageEvent(Thread thread, String message)
    {
      this.thread = thread;
      this.message = message;
    }
    
    void fire(EventHandler handler)
    {
      handler.onMessage(ustc, thread, message);
    }
  }
  
  static class ExceptionEvent extends Event
  {
    final Thread thread;
    final Throwable exception;
    
    ExceptionEvent(Thread thread, Throwable exception)
    {
      this.thread = thread;
      this.exception = exception;
    }
    
    void fire(EventHandler handler)
    {
      handler.onException(ustc, thread, exception);
    }
  }
  
  static class AnnotatedExceptionEvent extends Event
  {
    final Thread thread;
    final String message;
    final Throwable exception;
    
    AnnotatedExceptionEvent(Thread thread, String message, Throwable exception)
    {
      this.thread = thread;
      this.message = message;
      this.exception = exception;
    }
    
    void fire(EventHandler handler)
    {
      handler.onAnnotatedException(ustc, thread, message, exception);
    }
  }
  
  static class AbortExceptionEvent extends Event
  {
    final Thread thread;
    final Throwable exception;
    
    AbortExceptionEvent(Thread thread, Throwable exception)
    {
      this.thread = thread;
      this.exception = exception;
    }
    
    void fire(EventHandler handler)
    {
      handler.onAbortException(ustc, thread, exception);
    }
  }
  
  private static final LinkedBlockingQueue<Event> events = (new LinkedBlockingQueue<Event>());
  
  /***
   * trace location; for debugging only.
   ***/
  public static void T()
  {
    StackTraceElement elm = (((new Throwable()).getStackTrace())[1]);
    log("" + elm.getFileName() + ":" + elm.getLineNumber() + " (" + elm.getClassName() + "." + elm.getMethodName() + ") @ " + System.currentTimeMillis());
  }
  
  public static void log(String message)
  {
    try {
      events.put((new MessageEvent(Thread.currentThread(), message)));
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static void log(Throwable exception)
  {
    try {
      events.put((new ExceptionEvent(Thread.currentThread(), exception)));
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static void log(String message, Throwable exception)
  {
    try {
      events.put((new AnnotatedExceptionEvent(Thread.currentThread(), message, exception)));
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static RuntimeException fatal(Throwable exception)
  {
    try {
      events.put((new AbortExceptionEvent(Thread.currentThread(), exception)));
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
    
    /* hang */
    while (true);
  }
  
  public static final F1<Void, Throwable> BOMB = (new F1<Void, Throwable>() { public Void invoke(Throwable e) { fatal(e); throw null; } });
  
  public static abstract class LoggedThread
  {
    protected abstract void run() throws Exception;
  }
  
  private static void startLoggedThread(final LoggedThread thread, final boolean daemon)
  {
    (new java.lang.Thread()
      {
        { if (daemon) { setDaemon(true); } }
        
        public void run()
        {
          try {
            thread.run();
          } catch (Throwable e) {
            Log.log(e);
          }
        }
      }).start();
  }
  
  public static void startLoggedThread(LoggedThread thread)
  {
    startLoggedThread(thread, false);
  }
  
  public static void startLoggedDaemonThread(LoggedThread thread)
  {
    startLoggedThread(thread, true);
  }
  
  public static void handleEventsNonblocking(EventHandler handler)
  {
    Event event;
    
    while ((event = events.poll()) != null) {
      event.fire(handler);
    }
  }
  
  public static void loopHandleEvents(EventHandler handler)
  {
    try {
      while (true) {
        events.take().fire(handler);
      }
    } catch (InterruptedException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static void loopHandleEventsBackground(final PrintStream out, final boolean flush)
  {
    startLoggedDaemonThread
      ((new LoggedThread()
        {
          protected void run() throws Exception
          {
            loopHandleEvents
              ((new EventHandler()
                {
                  public void onMessage(long ustc, Thread thread, String message)
                  {
                    synchronized (out) {
                      out.println("@" + ustc + ": " + thread + ": " + message);
                      if (flush) out.flush();
                    }
                  }
                  
                  public void onException(long ustc, Thread thread, Throwable exception)
                  {
                    synchronized (out) {
                      out.println("@" + ustc + ": " + thread + ": (enter exception)");
                      exception.printStackTrace(out);
                      out.println("(leave exception)");
                      if (flush) out.flush();
                    }
                  }
                  
                  public void onAnnotatedException(long ustc, Thread thread, String message, Throwable exception)
                  {
                    synchronized (out) {
                      out.println("@" + ustc + ": " + thread + ": " + message + " (enter exception)");
                      exception.printStackTrace(out);
                      out.println("(leave exception)");
                      if (flush) out.flush();
                    }
                  }
                  
                  public void onAbortException(long ustc, Thread thread, Throwable exception)
                  {
                    synchronized (out) {
                      out.println("@" + ustc + ": " + thread + ": abort! (enter exception)");
                      exception.printStackTrace(out);
                      out.println("(leave exception)");
                      if (flush) out.flush();
                    }
                    
                    out.flush();
                    System.exit(1);
                    while (true);
                  }
                }));
          }
        }));
  }
}

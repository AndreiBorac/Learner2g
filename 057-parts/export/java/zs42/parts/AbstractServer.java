/***
 * AbstractServer.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.net.*;

import java.util.concurrent.*;

public abstract class AbstractServer<Context>
{
  public static final int DEFAULT_LISTEN_BACKLOG  = 128;
  public static final int DEFAULT_INITIAL_THREADS =  16;
  
  protected abstract void handle(Context context, Socket client) throws Exception;
  
  private static <Context> void require(final F0<? extends AbstractServer<Context>> factory, final Context context, final int port, final int backlog, final int initial_threads, final F0<Boolean> should_accept, final F1<Void, Throwable> caught_server_start, final F1<Void, Throwable> caught_server_run, final F1<Void, Throwable> caught_client_run, final SimpleDeque<SynchronousQueue<Socket>> threadports, int amount)
  {
    synchronized (threadports) {
      while (threadports.size() < amount) {
        final SynchronousQueue<Socket> threadport = (new SynchronousQueue<Socket>());
        
        (new Thread()
          {
            public void run() {
              while (true) {
                try {
                  Socket client = threadport.take();
                  
                  try {
                    factory.invoke().handle(context, client);
                  } catch (Throwable e) {
                    caught_client_run.invoke(e);
                  } finally {
                    try { client.close(); } catch (Throwable e) { }
                  }
                  
                  synchronized (threadports) {
                    threadports.addLast(threadport);
                  }
                } catch (Throwable e) {
                  caught_server_run.invoke(e);
                }
              }
            }
          }).start();
        
        threadports.addLast(threadport);
      }
    }
  }
  
  public static <Context> void launch(final F0<? extends AbstractServer<Context>> factory, final Context context, final int port, final int backlog, final int initial_threads, final F0<Boolean> should_accept, final F1<Void, Throwable> caught_server_start, final F1<Void, Throwable> caught_server_run, final F1<Void, Throwable> caught_client_run) {
    try {
      (new Thread() {
          public void run() {
            try {
              //System.err.println("AbstractServer::launch: preparing to listen on port " + port);
              
              ServerSocket server = (new ServerSocket(port, backlog));
              
              // used in a FIFO manner to minimize the chances that any stack's useful pages are swapped out
              final SimpleDeque<SynchronousQueue<Socket>> threadports = (new SimpleDeque<SynchronousQueue<Socket>>());
              
              // kick off the requestest number of "initial threads" -- though additional threads are created on demand if needed
              require(factory, context, port, backlog, initial_threads, should_accept, caught_server_start, caught_server_run, caught_client_run, threadports, initial_threads);
              
              //System.err.println("AbstractServer::launch: listening on port " + port);
              
              while (should_accept.invoke()) {
                final Socket client = server.accept();
                
                synchronized (threadports) {
                  require(factory, context, port, backlog, initial_threads, should_accept, caught_server_start, caught_server_run, caught_client_run, threadports, 1);
                  threadports.removeFirst().put(client);
                }
              }
              
              server.close();
            } catch (Throwable e) {
              caught_server_run.invoke(e);
            }
          }
        }).start();
    } catch (Throwable e) {
      caught_server_start.invoke(e);
    }
  }
  
  public static <Context> void launch(final F0<? extends AbstractServer<Context>> factory, final Context context, final int port, final F0<Boolean> should_accept, final F1<Void, Throwable> caught_server_start, final F1<Void, Throwable> caught_server_run, final F1<Void, Throwable> caught_client_run) {
    launch(factory, context, port, DEFAULT_LISTEN_BACKLOG, DEFAULT_INITIAL_THREADS, should_accept, caught_server_start, caught_server_run, caught_client_run);
  }
  
  static class BasicAbstractServer extends AbstractServer<Void>
  {
    final F1<Void, Socket> handler;
    
    BasicAbstractServer(F1<Void, Socket> handler)
    {
      this.handler = handler;
    }
    
    protected final void handle(Void context, Socket client)
    {
      handler.invoke(client);
    }
  }
  
  public static void launch(final int port, F1<Void, Socket> handler)
  {
    final BasicAbstractServer server = (new BasicAbstractServer(handler));
    
    AbstractServer.launch((new F0<AbstractServer<Void>>() { public AbstractServer<Void> invoke() { return server; } }), null, port, DEFAULT_LISTEN_BACKLOG, DEFAULT_INITIAL_THREADS, PartsStatic.TRUE, Log.BOMB, Log.BOMB, Log.BOMB);
  }
}

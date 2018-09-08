/***
 * SplitterClient.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.learner.student;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.BAD;
import static gotm.onlf.utilities.Utilities.log;
import static gotm.onlf.utilities.Utilities.throw_F0;
import static gotm.onlf.utilities.Utilities.throw_F1;
import static gotm.onlf.utilities.Utilities.microTime;

import static gotm.onlf.learner.common.Constants.SplitterClient.*;

class SplitterClient
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
    abstract void putStreamSelectVector(long vector);
    abstract void enqueueOutgoingSnippet(F1<Void, OutgoingNetworkStream> proc);
    abstract int  stop(F1<Void, Integer> dec);
  }
  
  /***
   * methods of the following interface will be invoked in the context
   * of the specialized networking thread.
   ***/
  interface Callback
  {
    void gotSystemFeedback(String line);
    void gotPacket(long source_tc, long client_tc, int stream_id, GroupirPacket groupir);
    void gotStopCondition();
  }
  
  static Control launch(Callback callback, String host, int port, byte[] pass_user, long instance_id) { return launch_inner(callback, host, port, pass_user, instance_id); }
  static Control launch_recording(Callback callback, String URL) { return launch_recording_inner(callback, URL); }
  
  /***
   * IMPLEMENTATION (STREAMING)
   ***/
  
  static Control launch_inner(final Callback callback, final String host, final int port, final byte[] pass_user, final long instance_id)
  {
    final AtomicBoolean shut_down = new AtomicBoolean(false);
    final AtomicReference<F1<Void, Integer>> decrement_threads = new AtomicReference<F1<Void, Integer>>();

    return (new Control()
      {
        IncomingNetworkStream inp = (new IncomingNetworkStream(null, TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
        OutgoingNetworkStream out = (new OutgoingNetworkStream(null, TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
        
        LinkedBlockingQueue<F1<Void, OutgoingNetworkStream>> write_buffer = (new LinkedBlockingQueue<F1<Void, OutgoingNetworkStream>>());
        
        void putStreamSelectVector(final long vector)
        {
          enqueueOutgoingSnippet
            (new F1<Void, OutgoingNetworkStream>()
             {
               public Void invoke(OutgoingNetworkStream out)
               {
                 out.wI(-1);
                 out.wL(vector);
                 out.wL(0x3); // hardcoded transient vector (skips initial commands and audio)
                 out.wL(0); // hardcoded skip no packets (beyond transients)
                 
                 return null;
               }
             });
        }
        
        void enqueueOutgoingSnippet(F1<Void, OutgoingNetworkStream> proc)
        {
          try {
            write_buffer.put(proc);
          } catch (Exception e) {
            log(e);
            throw (new RuntimeException(e));
          }
        }
        
        int stop(F1<Void, Integer> dec)
        {
          decrement_threads.set(dec);
          shut_down.set(true);
          
          // post a write object to ensure that the thread wakes up to
          // see the shutdown signal
          try {
            write_buffer.put
              ((new F1<Void, OutgoingNetworkStream>()
                {
                  public Void invoke(OutgoingNetworkStream out)
                  {
                    // nothing to do
                    return null;
                  }
                }
              ));
          } catch (Exception e) {
            throw (new RuntimeException(e));
          }
          
          // don't know if the reading thread will be blocked on read at shutdown time,
          // so here accounting for only the writing thread
          return 1;
        }
        
        void loop()
        {
          try {
            Socket socket = null;
            
            log("connecting to host '" + host + "', port " + port);
            
            {
              while (true) {
                callback.gotSystemFeedback("trying to connect (timeout " + CONNECTION_TIMEOUT_MS + "ms) ...");
                
                try {
                  socket = (new Socket());
                  socket.connect((new InetSocketAddress(host, port)), CONNECTION_TIMEOUT_MS);
                  break;
                } catch (IOException e) {
                  // ignored; retry
                }
                
                long leave_time = microTime() + CONNECTION_TIMEOUT_MS;
                
                while (microTime() < leave_time) {
                  try {
                    Thread.sleep(CONNECTION_TIMEOUT_MS);
                  } catch (InterruptedException e) {
                    // ignored
                  }
                }
              }
            }
            
            callback.gotSystemFeedback("connected!");
            
            inp.src = socket.getInputStream();
            out.dst = socket.getOutputStream();
            
            // solve challenge
            {
              log("authenticating");
              if (!Authentication.response(inp, out, pass_user)) { log("authentication failure"); throw (new BAD("failed authentication")); }
              log("authenticated");
            }
            
            // send instance id
            {
              out.wL(instance_id);
              out.writeback();
            }
            
            // unlock writes by creating write thread
            {
              Thread ignored =
                (new Thread()
                  {
                    public void run()
                    {
                      try {
                        while (true) {
                          // batch write
                          {
                            F1<Void, OutgoingNetworkStream> apply = write_buffer.take();
                            
                            do {
                              apply.invoke(out);
                              
                              if (out.highWaterReached()) {
                                out.writeback();
                              }
                            } while ((apply = write_buffer.poll()) != null);
                            
                            out.writeback();
                          }
                          
                          // pause
                          {
                            try {
                              sleep(250);
                            } catch (InterruptedException e) {
                              // ignored
                            }
                          }
                          
                          // done?
                          {
                            if (shut_down.get()) {
                              log("exiting the writer thread of the splitter client");
                              decrement_threads.get().invoke(1);
                              break;
                            }
                          }
                        }
                      } catch (Exception e) {
                        // TODO
                      }
                    }
                    
                    { start(); }
                  });
            }
            
            log("enter receive loop");
            
            while (true) {
              // turn network buffer
              inp.turn();
              
              // receive packet
              GroupirPacket groupir = GroupirPacket.recv(inp, pass_user);
              
              final long client_tc = microTime();
              
              // make sure the packet has the required metadata
              if (groupir.dI.length < PACKET_METADATA_LENGTH) throw (new BAD("illegal packet"));
              
              // extract metadata
              final long source_tc = decodeProperSourceTimecode(groupir);
              final int  stream_id = groupir.dI[PACKET_METADATA_STREAM_ID];
              
              // enqueue packet receipt confirmation (for audio packets only)
              if (stream_id == PACKET_METADATA_STREAM_ID_AUDIO) {
                enqueueOutgoingSnippet
                  (new F1<Void, OutgoingNetworkStream>()
                   {
                     public Void invoke(OutgoingNetworkStream out)
                     {
                       out.wI(1 + 4 + 4 + 4);
                       out.wB(RET_PACKET_RECEIVED);
                       out.wI(stream_id);
                       out.wI(((int)(source_tc)));
                       out.wI(((int)(microTime())));
                       
                       return null;
                     }
                   });
              }
              
              // check for stop condition
              if (stream_id == STREAM_STOP) {
                break;
              }
              
              // check thread termination condition
              if (shut_down.get()) {
                break;
              }
              
              callback.gotPacket(source_tc, client_tc, stream_id, groupir);
            }
            
            callback.gotStopCondition();
            
            log("leave receive loop");
            
            log("closing down");
            socket.close();
            log("closed down");
          } catch (Exception e) {
            log("splitter client side exception", e);
          } finally {
            callback.gotStopCondition();
          }
        }
        
        { (new Thread() { public void run() { loop(); } }).start(); }
      });
  }
  
  /***
   * IMPLEMENTATION (RECORDING)
   ***/
  
  static Control launch_recording_inner(final Callback callback, final String httpUrl)
  {
    return
      (new Control()
        {
          IncomingNetworkStream inp = (new IncomingNetworkStream(null, TRANSPORT_BUFFER_SIZE, throw_F0, throw_F1));
          
          void putStreamSelectVector(final long vector)
          {
            // ignored
          }
          
          void enqueueOutgoingSnippet(F1<Void, OutgoingNetworkStream> proc)
          {
            // ignored
          }
          
          int stop(F1<Void, Integer> dec)
          {
            return 0; // TODO: is this right?
          }
          
          void loop()
          {
            try {
              log("enter connecting");
              
              URL url = (new URL(httpUrl));
              HttpURLConnection connection = ((HttpURLConnection)((new URL(httpUrl)).openConnection()));
              connection.setRequestMethod("GET");
              connection.setDoInput(true);
              connection.connect();
              
              inp.src = connection.getInputStream();
              
              log("enter receive loop");
              
              while (true) {
                // turn network buffer
                inp.turn();
                
                // receive packet
                GroupirPacket groupir = GroupirPacket.recv_from_trusted_source(inp);
                
                final long client_tc = microTime();
                
                // make sure the packet has the required metadata
                if (groupir.dI.length < PACKET_METADATA_LENGTH) throw (new BAD("illegal packet"));
                
                // extract metadata
                final long source_tc = decodeProperSourceTimecode(groupir);
                final int  stream_id = groupir.dI[PACKET_METADATA_STREAM_ID];
                
                // enqueue packet receipt confirmation (for audio packets only)
                if (stream_id == PACKET_METADATA_STREAM_ID_AUDIO) {
                  enqueueOutgoingSnippet
                    (new F1<Void, OutgoingNetworkStream>()
                     {
                       public Void invoke(OutgoingNetworkStream out)
                       {
                         out.wI(1 + 4 + 4 + 4);
                         out.wB(RET_PACKET_RECEIVED);
                         out.wI(stream_id);
                         out.wI(((int)(source_tc)));
                         out.wI(((int)(microTime())));
                         
                         return null;
                       }
                     });
                }
                
                // check for stop condition
                if (stream_id == STREAM_STOP) {
                  break;
                }
                
                callback.gotPacket(source_tc, client_tc, stream_id, groupir);
              }
              
              callback.gotStopCondition();
              
              log("leave receive loop");
              
              log("closing down");
              
              connection.disconnect();
              
              log("closed down");
            } catch (Exception e) {
              log("splitter client side exception", e);
            } finally {
              callback.gotStopCondition();
            }
          }
          
          { (new Thread() { public void run() { loop(); } }).start(); }
        });
  }
}

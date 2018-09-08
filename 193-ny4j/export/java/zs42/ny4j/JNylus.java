/***
 * JNylus.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.ny4j;

import zs42.parts.*;

import static zs42.parts.Static.cast_unchecked;

import java.util.*;

public class JNylus
{
  /***
   * ABSTRACT INTERFACE
   ***/
  
  public static abstract class Manager
  {
    /***
     * Returns a new <code>Station</code> (initially unregistered).
     ***/
    public abstract Station newStation();
  }
  
  /***
   * A <code>Station</code> object may be used to send and post
   * messages. Only the owner thread may invoke methods of a
   * <code>Station</code> object.
   ***/
  public static abstract class Station
  {
    public abstract void registerOwnership();
    public abstract void assertOwnership();
    
    volatile boolean terminated;
    
    public boolean getTerminated()
    {
      return terminated;
    }
    
    public void setTerminated()
    {
      assertOwnership();
      terminated = true;
    }
    
    public abstract boolean getStarvationProtection();
    public abstract void    setStarvationProtection(boolean enabled);
    
    /***
     * Returns a new <code>Channel</code>, with lower priority than
     * (i.e., yielding to) any previously created channels.
     ***/
    public abstract Channel newChannel();
    
    /***
     * Adds the given <code>Runnable</code> to the execution queue of
     * the given <code>Channel</code>. Usually, the execution queue
     * will belong to a different thread. In any case, it must descend
     * from the same <code>Manager</code> object. A thread can message
     * itself to achieve prioritization of tasks.
     ***/
    public abstract void post(Channel channel, Runnable runnable);
    
    /***
     * If a message is available, processes it. Returns the number of
     * messages processed (0 or 1).
     ***/
    public abstract int trig();
    
    /***
     * Performs one "round" of message processing. Returns the number
     * of messages processed.
     ***/
    public abstract int once();
    
    /***
     * Same as <code>once()</code>, but blocks until at least one
     * message has been processed.
     ***/
    public abstract int hang();
  }
  
  /***
   * A <code>Channel</code> object represents a prioritized message
   * target.
   ***/
  public static abstract class Channel
  {
  }
  
  /***
   * A <code>Linkage</code> object represents a communication bridge
   * between a pair of channels.
   ***/
  public static final class Linkage
  {
    public final JNylus.Station station_caller;
    public final JNylus.Station station_callee;
    public final JNylus.Channel channel_caller;
    public final JNylus.Channel channel_callee;
    
    public Linkage(final JNylus.Station station_caller, final JNylus.Station station_callee, final JNylus.Channel channel_caller, final JNylus.Channel channel_callee)
    {
      this.station_caller = station_caller;
      this.station_callee = station_callee;
      this.channel_caller = channel_caller;
      this.channel_callee = channel_callee;
    }
    
    public Linkage reverse()
    {
      return (new Linkage(station_callee, station_caller, channel_callee, channel_caller));
    }
  }
  
  /***
   * IMPLEMENTATIONS
   ***/
  
  public static class ExponentialBackoffManager extends Manager
  {
    private final int station_limit;
    private final int minimum_wait_ms;
    private final int maximum_wait_ms;
    
    private volatile int station_count = 0;
    
    private synchronized int allocate_station_id()
    {
      if (station_count >= station_limit) throw null;
      return station_count++;
    }
    
    public ExponentialBackoffManager(int station_limit, int minimum_wait_ms, int maximum_wait_ms)
    {
      if (!((0 <= station_limit) && (station_limit < 256))) throw null;
      if (!((0 <= minimum_wait_ms) && (minimum_wait_ms < 10000))) throw null;
      if (!((0 <= maximum_wait_ms) && (maximum_wait_ms < 10000))) throw null;
      
      this.station_limit = station_limit;
      this.minimum_wait_ms = minimum_wait_ms;
      this.maximum_wait_ms = maximum_wait_ms;
    }
    
    public Station newStation()
    {
      final int station_id = allocate_station_id();
      
      return
        (new Station()
          {
            class StationChannel extends Channel
            {
              final Station enclosing;
              final ZeroDelayQueue<Runnable>[] inbound = cast_unchecked(((ZeroDelayQueue<Runnable>[])(null)), (new ZeroDelayQueue[station_limit]));
              
              StationChannel(Station enclosing)
              {
                this.enclosing = enclosing;
                
                final ConsObjectCache cons_cache = (new ConsObjectCache());
                
                for (int i = 0; i < inbound.length; i++) {
                  inbound[i] = (new ZeroDelayQueue<Runnable>(cons_cache));
                }
              }
            }
            
            Thread owner = null;
            boolean starvation_protection = false;
            final ArrayList<StationChannel> channels = (new ArrayList<StationChannel>());
            
            public void registerOwnership()
            {
              if (owner != null) throw null;
              owner = Thread.currentThread();
            }
            
            public void assertOwnership()
            {
              if (Thread.currentThread() != owner) throw null;
            }
            
            private void assertOwnershipPartial()
            {
              if (owner != null) assertOwnership();
            }
            
            public boolean getStarvationProtection()
            {
              assertOwnershipPartial();
              
              {
                return starvation_protection;
              }
            }
            
            public void setStarvationProtection(boolean enabled)
            {
              assertOwnershipPartial();
              
              {
                starvation_protection = enabled;
              }
            }
            
            public Channel newChannel()
            {
              assertOwnershipPartial();
              
              {
                StationChannel channel = (new StationChannel(this));
                channels.add(channel);
                return channel;
              }
            }
            
            public void post(Channel channel, Runnable runnable)
            {
              assertOwnership();
              
              {
                StationChannel peer = ((StationChannel)(channel));
                
                if (!peer.enclosing.getTerminated()) {
                  peer.inbound[station_id].push(runnable);
                }
              }
            }
            
            public int trig()
            {
              assertOwnership();
              
              {
                for (StationChannel channel : channels) {
                  for (ZeroDelayQueue<Runnable> queue : channel.inbound) {
                    Runnable entry = queue.pull();
                    
                    if (entry != null) {
                      if (!getTerminated()) {
                        entry.run();
                        return 1;
                      } else {
                        return 0;
                      }
                    }
                  }
                }
                
                return 0;
              }
            }
            
            public int once()
            {
              assertOwnership();
              
              {
                int cntr = 0;
                
                {
                  int last = -1;
                  
                  while (cntr != last) {
                    last = cntr;
                    cntr += trig();
                  }
                }
                
                return cntr;
              }
            }
            
            public int hang()
            {
              assertOwnership();
              
              {
                int cntr = 0;
                
                {
                  int interval = minimum_wait_ms;
                  
                  while (!getTerminated()) {
                    cntr += once();
                    if (cntr > 0) return cntr;
                    
                    try {
                      if (interval > 0) {
                        Thread.sleep(interval);
                      } else {
                        Thread.yield();
                      }
                      
                      // ratchet interval
                      interval = Math.min(maximum_wait_ms, Math.max(1, (interval << 1)));
                    } catch (InterruptedException e) {
                      throw (new RuntimeException(e));
                    }
                  }
                  
                  once();
                  return cntr;
                }
              }
            }
          });
    }
  }
  
  public static void spinOff(final Station station)
  {
    Runnable runnable =
      (new Runnable()
        {
          public void run()
          {
            try {
              {
                ResourceTracker.Token token_thread = ResourceTracker.acquire("JNylus::spinOff(station=" + station + ", thread=" + Thread.currentThread() + ")");
                
                station.registerOwnership();
                
                while (!(station.getTerminated())) {
                  station.hang();
                }
                
                station.once();
                
                ResourceTracker.release(token_thread);
              }
            } catch (Throwable e) {
              Log.log(e);
            }
          }
        });
    
    (new Thread(runnable)).start();
  }
}

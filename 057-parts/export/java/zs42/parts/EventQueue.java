/***
 * EventQueue.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.util.concurrent.atomic.*;

import static zs42.parts.PartsStatic.*;

public class EventQueue<M extends EventQueue.AbstractStateMachine<M, E>, E>
{
  static final int MINIMUM_EVENT_BACKLOG = 1;
  
  public abstract static class AbstractStateMachine<M extends EventQueue.AbstractStateMachine<M, E>, E>
  {
    volatile int version;
    
    protected abstract void loadInitialState();
    protected abstract void copyStateFrom(M other);
    protected abstract void processEvent(E event);
  }
  
  volatile boolean running = true;
  
  AtomicInteger           eventi = new AtomicInteger();
  int                     eventm; // events.size() - 1
  AtomicReferenceArray<E> events;
  
  volatile M curr;
  
  public void stop()
  {
    running = false;
  }
  
  private boolean isVersionValid(int version)
  {
    return ((version & 1) == 0); // even versions are valid; odd are not
  }
  
  private void yield()
  {
    Thread.yield();
  }
  
  public EventQueue(int event_backlog_hint, final F0<M> factory, final F1<Void, Throwable> handler)
  {
    // allocate event queue
    {
      int event_backlog = nextBinaryPower(event_backlog_hint, MINIMUM_EVENT_BACKLOG);
      eventm = event_backlog - 1;
      events = new AtomicReferenceArray<E>(event_backlog);
    }
    
    // allocate state machine and initialize it
    {
      M temp = factory.invoke();
      temp.loadInitialState();
      temp.version = 0; // write barrier!
      curr = temp;
    }
    
    (new Thread() {
        public void run() {
          try {
            // pointer that tracks the event queue
            int eventi = 0;
            
            // allocate state machine peer
            M peer = factory.invoke();
            
            while (running) {
              E event;
              
              // invalidate the peer state machine and update it to the current state machine
              peer.version = curr.version + 1; // curr.version is valid, so peer.version is not valid
              peer.copyStateFrom(curr);
              
              // update the peer state machine to new events since the "current" state machine
              while ((event = events.get(eventi & eventm)) != null) {
                events.set(eventi & eventm, null);
                eventi++;
                
                peer.processEvent(event);
              }
              
              // validate the peer state machine and swap it in as the "current" state machine
              peer.version = peer.version + 1; // peer.version was invalid, so not peer.version is valid
              peer = swap(peer);
              
              // TODO: in the future, use interrupts so as not to waste cycles
              yield();
            }
          } catch (Throwable e) {
            handler.invoke(e);
          }
        }
      }).start();
  }
  
  private M swap(M succ)
  {
    M retv;
    retv = curr;
    curr = succ;
    return retv;
  }
  
  public void copyStateTo(M target)
  {
    int enter_version;
    int leave_version;
    
    do {
      M cached;
      
      do {
        cached = curr;
        enter_version = cached.version;
      } while (!isVersionValid(enter_version));
      
      target.copyStateFrom(target);
      
      leave_version = cached.version;
    } while (enter_version != leave_version);
  }
  
  public void enqueueEvent(E event)
  {
    events.set(eventi.getAndIncrement() & eventm, event);
  }
}

/***
 * EventQueue2.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

public class EventQueue2<M extends EventQueue.AbstractStateMachine<M, E>, E>
{
  public abstract static class AbstractStateMachine<M extends EventQueue.AbstractStateMachine<M, E>, E>
  {
    volatile int version;
    
    protected abstract void loadInitialState();
    protected abstract void copyStateFrom(M other);
    protected abstract <R> R processEvent(E event);
  }
  
  
}

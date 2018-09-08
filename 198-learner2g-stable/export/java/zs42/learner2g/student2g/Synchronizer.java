/***
 * Synchronizer.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.student2g;

import zs42.parts.*;

public abstract class Synchronizer
{
  public abstract void notify(long local_lock, long other_lock);
  public abstract long tsconv(long limit_time, long other_time, boolean commit);
  
  public static final class ConvergingSynchronizer extends Synchronizer
  {
    long offset_estimator = 0; // local - other
    
    long recent_local = 0;
    long recent_other = 0;
    
    final long ONE_SECOND_US = 1000000L;
    final int MOVING_AVERAGE_SHAMT = 4;
    
    public void notify(long local_lock, long other_lock)
    {
      long offset_observed = (local_lock - other_lock);
      
      if (offset_observed > offset_estimator) {
        offset_estimator = offset_observed;
        //Log.log("ratchet: " + offset_estimator);
      }
      
      if (offset_observed < (offset_estimator - ONE_SECOND_US)) {
        offset_estimator = offset_observed;
        //Log.log("breakout: " + offset_estimator);
      }
    }
    
    final int INCREMENTAL_CORRECTION_SHAMT = 4;
    
    public long tsconv(long limit_time, long other_time, boolean commit)
    {
      long local_time;
      
      long local_time_reckoning = (recent_local + (other_time - recent_other));
      long local_time_ideal = other_time + offset_estimator;
      
      if (Math.abs(local_time_reckoning - local_time_ideal) > ONE_SECOND_US) {
        // too large a gap to bridge incrementally; just force it ...
        local_time = local_time_ideal;
      } else {
        long maximum_adjustment = (Math.abs(other_time - recent_other) >> INCREMENTAL_CORRECTION_SHAMT);
        
        //Log.log("local_time_ideal=" + local_time_ideal + ", local_time_reckoning=" + local_time_reckoning + ", maximum_adjustment=" + maximum_adjustment);
        
        if (Math.abs(local_time_reckoning - local_time_ideal) < maximum_adjustment) {
          local_time = local_time_ideal;
          //Log.log("ideal!");
        } else {
          if (local_time_reckoning < local_time_ideal) {
            local_time = local_time_reckoning + maximum_adjustment;
          } else {
            local_time = local_time_reckoning - maximum_adjustment;
          }
        }
      }
      
      // planning too far in the future should be prohibited
      if (local_time > limit_time) {
        local_time = limit_time;
      }
      
      if (commit) {
        recent_local = local_time;
        recent_other = other_time;
      }
      
      return local_time;
    }
  }
}

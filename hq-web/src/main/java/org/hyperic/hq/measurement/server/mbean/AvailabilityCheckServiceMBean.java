/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.measurement.server.mbean;

/**
 * MBean interface.
 */
public interface AvailabilityCheckServiceMBean {

   /**
    * 
    */
  void hitWithDate(java.util.Date lDate) ;

   /**
    * This method ignores the date which is passed in, the reason for this is that we have seen instances where the JBoss Timer service produces an invalid date which is in the past. Since AvailabilityCheckService is very time sensitive this is not acceptable. Therefore we use System.currentTimeMillis() and ignore the date which is passed in.
    */
  void hit(java.util.Date lDate) ;

   /**
    * Get the interval for how often this mbean is called
    */
  long getInterval() ;

   /**
    * Set the interval for how often this mbean is called
    */
  void setInterval(long interval) ;

   /**
    * Get the wait for how long after service starts to backfill
    */
  long getWait() ;

   /**
    * Set the wait for how long after service starts to backfill
    */
  void setWait(long wait) ;

  void init() ;

  void start() throws java.lang.Exception;

  void stop() ;

  void destroy() ;

}

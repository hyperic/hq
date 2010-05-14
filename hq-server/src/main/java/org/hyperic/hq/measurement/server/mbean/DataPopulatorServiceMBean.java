/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.measurement.server.mbean;

/**
 * MBean interface.
 */
public interface DataPopulatorServiceMBean {

  void stop() ;

  void start() ;

  void populate() throws java.lang.Exception;

  void populate(long max) throws java.lang.Exception;

}

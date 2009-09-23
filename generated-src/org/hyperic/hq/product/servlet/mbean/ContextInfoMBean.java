/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.product.servlet.mbean;

/**
 * MBean interface.
 */
public interface ContextInfoMBean {

  int getAvailable() ;

  long getTotalTime() ;

  int getMinTime() ;

  int getMaxTime() ;

  int getAvgTime() ;

  int getBytesSent() ;

  int getBytesReceived() ;

  int getRequestCount() ;

  int getErrorCount() ;

  int getSessionsCreated() ;

  int getSessionsDestroyed() ;

  java.lang.String getDocBase() ;

  java.lang.String getContextName() ;

  java.lang.String getResponseTimeLogDir() ;

  void setResponseTimeLogDir(java.lang.String logDir) ;

}

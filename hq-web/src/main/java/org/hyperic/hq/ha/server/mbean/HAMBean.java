package org.hyperic.hq.ha.server.mbean;

public interface HAMBean {
   
      void startSingleton() ;

      void stopSingleton(java.lang.String gracefulShutdown) ;

}


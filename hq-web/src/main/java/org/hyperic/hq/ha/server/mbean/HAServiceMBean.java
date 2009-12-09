package org.hyperic.hq.ha.server.mbean;

public interface HAServiceMBean {
   
      void startSingleton() ;

      void stopSingleton(java.lang.String gracefulShutdown) ;

}


package org.hyperic.hq.application;

import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.util.Runnee;

public interface HibernateLogManager {
    /**
     * Run the specified runner in the passed interceptor chain.  Only
     * calls within the local thread are processed by the chain, and when
     * this method returns the chain will be restored. 
     */
    void log(HibernateInterceptorChain chain, Runnee r)
        throws Exception;
}

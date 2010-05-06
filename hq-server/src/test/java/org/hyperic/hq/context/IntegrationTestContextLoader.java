package org.hyperic.hq.context;

import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.GenericXmlContextLoader;

public class IntegrationTestContextLoader
    extends GenericXmlContextLoader {

    @Override
    protected void customizeContext(GenericApplicationContext context) {
        Bootstrap.appContext = context;
        ConcurrentStatsCollector.setEnabled(false);
        System.setProperty("org.hyperic.sigar.path","/Users/jhickey/Desktop/sigar-1.6.4-libs");
    }

}

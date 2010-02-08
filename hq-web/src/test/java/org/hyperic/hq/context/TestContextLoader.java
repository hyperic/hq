package org.hyperic.hq.context;

import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.GenericXmlContextLoader;

public class TestContextLoader
    extends GenericXmlContextLoader {

    @Override
    protected void customizeContext(GenericApplicationContext context) {
        Bootstrap.appContext = context;
        ConcurrentStatsCollector.setEnabled(false);
    }

}

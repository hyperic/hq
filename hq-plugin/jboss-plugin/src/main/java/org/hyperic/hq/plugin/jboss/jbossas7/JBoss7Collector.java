package org.hyperic.hq.plugin.jboss.jbossas7;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss.jbossas7.objects.ThreadsInfo;
import org.hyperic.hq.product.PluginException;

public class JBoss7Collector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(JBoss7Collector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        try {
            setAvailability(true);
            ThreadsInfo th = admin.getThreadsInfo();
            setValue("thread-count", th.getThreadCount());
            setValue("peak-thread-count", th.getPeakThreadCount());
            setValue("total-started-thread-count", th.getTotalStartedThreadCount());
            setValue("daemon-thread-count", th.getDaemonThreadCount());
            setValue("current-thread-cpu-time", th.getCurrentThreadCpuTime());
            setValue("current-thread-user-time", th.getCurrentThreadUserTime());
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}

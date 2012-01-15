package org.hyperic.hq.agent.stats;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.stats.AbstractStatsWriter;

public class AgentStatsWriter extends AbstractStatsWriter {
    private static final String PREFIX = "agentstats";
    private ScheduledThreadPoolExecutor executor;
    private AgentConfig config;

    public AgentStatsWriter(AgentConfig config) {
        super(AgentStatsCollector.getInstance(), Retention.MONTHLY, PREFIX);
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.config = config;
    }

    protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long millis) {
        return executor.scheduleWithFixedDelay(task, millis, millis, TimeUnit.MILLISECONDS);
    }

    protected String getAndSetupBasedir() {
        File logDir = config.getLogDir();
        File statsDir = new File(logDir, PREFIX);
        statsDir.mkdir();
        return statsDir.getAbsolutePath();
    }

    public void stopWriter() {
        executor.shutdown();
    }

}

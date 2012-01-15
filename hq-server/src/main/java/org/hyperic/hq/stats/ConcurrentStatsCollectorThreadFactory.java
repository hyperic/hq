package org.hyperic.hq.stats;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentStatsCollectorThreadFactory implements ThreadFactory {
    private final String namePrefix = "hqstats-sampler-";
    private final AtomicInteger threadNumber = new AtomicInteger(0);

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
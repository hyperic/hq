/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.agent.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.stats.AgentStatsCollector;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorExecutor;
import org.hyperic.hq.product.PluginManager;

public class CollectorThread implements Runnable {

    private static Log log =
        LogFactory.getLog(CollectorThread.class.getName());

    //interval to check collectors
    private static final int DEFAULT_INTERVAL = 1 * 1000 * 60;

    private static final String COLLECTOR_THREAD_METRIC_COLLECTED_TIME =
        AgentStatsCollector.COLLECTOR_THREAD_METRIC_COLLECTED_TIME;

    private Thread thread = null;
    private static CollectorThread instance = null;
    private final AtomicBoolean shouldDie = new AtomicBoolean(false);
    private long interval = DEFAULT_INTERVAL;
    private Properties props;
    private AgentStatsCollector statsCollector;

    public static synchronized CollectorThread getInstance(PluginManager manager) {
        if (instance == null) {
            instance = new CollectorThread();
            instance.props = manager.getProperties();
        }
        return instance;
    }

    public static synchronized void shutdownInstance() {
        if (instance != null) {
            instance.doStop();
            instance = null;
        }
    }

    public synchronized void doStart() {
        if (this.thread != null) {
            return;
        }
        statsCollector = AgentStatsCollector.getInstance();
        statsCollector.register(COLLECTOR_THREAD_METRIC_COLLECTED_TIME);

        String interval = System.getProperty("exec.interval");
        if (interval != null) {
            this.interval = Integer.parseInt(interval) * 1000;
        }

        this.thread = new Thread(this, "CollectorThread");
        this.thread.setDaemon(true);
        this.thread.start();

        log.info(this.thread.getName() + " started");
    }

    public synchronized void doStop() {
        if (this.thread == null) {
            return;
        }
        die();
        this.thread.interrupt();
        log.info(this.thread.getName() + " stopped");
        this.thread = null;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getInterval() {
        return this.interval;
    }

    public void run() {
        CollectorExecutor executor = new CollectorExecutor(this.props);
        log.debug("Created ThreadPoolExecutor: " +
                  "corePoolSize=" + executor.getCorePoolSize() + ", " +
                  "maxPoolSize=" + executor.getMaximumPoolSize());
        while (!shouldDie.get()) {
            final Collection<Collector> collectorsToExecute = Collector.getCollectorsToExecute();
            for (final Collector collector : collectorsToExecute) {
                if (executor.isPoolable() && collector.isPoolable()) {
                    executor.execute(getProxy(collector));
                } else {
                    collector.run();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("CompletedTaskCount=" + executor.getCompletedTaskCount() + ", " +
                          "ActiveCount=" + executor.getActiveCount() + ", " +
                          "TaskCount=" + executor.getTaskCount() + ", " +
                          "PoolSize=" + executor.getPoolSize());
            }
            try {
                Thread.sleep(this.interval);
            } catch (InterruptedException e) {
            }
        }
        executor.shutdown();
    }

    /** proxy used to intercept in order to create stats */
    private Runnable getProxy(final Collector collector) {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ((null == args || args.length == 0) && method.getName().equals("run")) {
                    final long start = now();
                    Object rtn = method.invoke(collector, args);
                    final long duration = now() - start;
                    statsCollector.addStat(duration, COLLECTOR_THREAD_METRIC_COLLECTED_TIME);
                    return rtn;
                } else {
                    return method.invoke(collector, args);
                }
            }
        };
        return (Runnable) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {Runnable.class}, handler);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public void die() {
        this.shouldDie.set(true);
    }
}

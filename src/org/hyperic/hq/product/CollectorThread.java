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

package org.hyperic.hq.product;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CollectorThread implements Runnable {

    private static Log log =
        LogFactory.getLog(CollectorThread.class.getName());

    //interval to check collectors
    private static final int DEFAULT_INTERVAL = 1 * 1000 * 60;

    private Thread thread = null;
    private static CollectorThread instance = null;
    private boolean shouldDie = false;
    private long interval = DEFAULT_INTERVAL;
    private Properties props;

    static synchronized CollectorThread getInstance(PluginManager manager) {
        if (instance == null) {
            instance = new CollectorThread();
            instance.props = manager.getProperties();
        }

        return instance;
    }

    static synchronized void shutdownInstance() {
        if (instance != null) {
            instance.doStop();
            instance = null;
        }
    }

    public synchronized void doStart() {
        if (this.thread != null) {
            return;
        }

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

        while (!shouldDie) {
            Collector.check(executor);

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

    public void die() {
        this.shouldDie = true;
    }
}

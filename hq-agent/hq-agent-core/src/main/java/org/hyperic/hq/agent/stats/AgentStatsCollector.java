/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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

package org.hyperic.hq.agent.stats;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.hyperic.hq.stats.AbstractStatsCollector;

public class AgentStatsCollector extends AbstractStatsCollector {

    private ScheduledThreadPoolExecutor executor;
    private static final AgentStatsCollector instance = new AgentStatsCollector();
    public static final String SCHEDULE_THREAD_METRICS_COLLECTED_TIME = "SCHEDULE_THREAD_METRICS_COLLECTED_TIME";
    public static final String SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED = "SCHEDULE_THREAD_METRIC_TASKS_SUBMITTED";
    public static final String SCHEDULE_THREAD_METRIC_COLLECT_FAILED  = "SCHEDULE_THREAD_METRIC_COLLECT_FAILED";
    public static final String COLLECTOR_THREAD_METRIC_COLLECTED_TIME = "COLLECTOR_THREAD_METRIC_COLLECTED_TIME";
    public static final String SENDER_THREAD_SEND_NUM_METRICS = "SENDER_THREAD_NUM_SEND_METRICS";
    public static final String SENDER_THREAD_SEND_METRICS_TIME = "SENDER_THREAD_SEND_METRICS_TIME";
    public static final String DISK_LIST_DISK_ITERATOR_REMOVE_TIME = "DISK_LIST_DISK_ITERATOR_REMOVE_TIME";
    public static final String DISK_LIST_READ_RECORD_TIME = "DISK_LIST_READ_RECORD_TIME";
    public static final String DISK_LIST_ADD_TO_LIST_TIME = "DISK_LIST_ADD_TO_LIST_TIME";
    public static final String DISK_LIST_DELETE_ALL_RECORDS_TIME = "DISK_LIST_DELETE_ALL_RECORDS_TIME";
    public static final String DISK_LIST_KEYVALS_FLUSH_TIME = "DISK_LIST_KEYVALS_FLUSH_TIME";

    private AgentStatsCollector() {
        super(getMBeanServer());
        this.executor = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
            private final AtomicLong num = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                final Thread rtn = new Thread(r, "agentstats-" + num.getAndIncrement());
                rtn.setDaemon(true);
                return rtn;
            }
        });
    }
    
    private static MBeanServer getMBeanServer() {
        final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        return (servers.isEmpty()) ? null : servers.get(0);
    }

    protected ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long millis) {
        return executor.scheduleAtFixedRate(runnable, millis, millis, TimeUnit.MILLISECONDS);
    }
    
    public static AgentStatsCollector getInstance() {
        return instance;
    }

}

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

package org.hyperic.hq.stats;

import java.util.concurrent.ScheduledFuture;

import javax.annotation.PreDestroy;
import javax.management.MBeanServer;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.util.stats.StatCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ConcurrentStatsCollector extends AbstractStatsCollector {
    public static final String FIRE_ALERT_TIME = "FIRE_ALERT_TIME",
    						   EVENT_PROCESSING_TIME = "EVENT_PROCESSING_TIME", 
    						   EHCACHE_TOTAL_OBJECTS = "EHCACHE_TOTAL_OBJECTS",
    						   CONCURRENT_STATS_COLLECTOR = "CONCURRENT_STATS_COLLECTOR",
    						   RUNTIME_PLATFORM_AND_SERVER_MERGER = "RUNTIME_PLATFORM_AND_SERVER_MERGER", 
    						   AVAIL_MANAGER_METRICS_INSERTED = "AVAIL_MANAGER_METRICS_INSERTED",
    						   DATA_MANAGER_INSERT_TIME = "DATA_MANAGER_INSERT_TIME", 
                               DATA_MANAGER_RETRIES_TIME = "DATA_MANAGER_RETRIES_TIME",
    						   JMS_TOPIC_PUBLISH_TIME = "JMS_TOPIC_PUBLISH_TIME", 
    						   METRIC_DATA_COMPRESS_TIME = "METRIC_DATA_COMPRESS_TIME",
    						   DB_ANALYZE_TIME = "DB_ANALYZE_TIME", 
    						   PURGE_EVENT_LOGS_TIME = "PURGE_EVENT_LOGS_TIME",
    						   PURGE_MEASUREMENTS_TIME = "PURGE_MEASUREMENTS_TIME", 
    						   MEASUREMENT_SCHEDULE_TIME = "MEASUREMENT_SCHEDULE_TIME",
    						   SEND_ALERT_TIME = "SEND_ALERT_TIME",
    						   ZEVENT_QUEUE_SIZE = "ZEVENT_QUEUE_SIZE",
    						   TRIGGER_INIT_TIME = "TRIGGER_INIT_TIME", 
    						   FIRED_ALERT_TIME = "FIRED_ALERT_TIME",
    						   SCHEDULE_QUEUE_SIZE = "SCHEDULE_QUEUE_SIZE",
    						   UNSCHEDULE_QUEUE_SIZE = "UNSCHEDULE_QUEUE_SIZE",
    						   ESCALATION_EXECUTE_STATE_TIME = "ESCALATION_EXECUTE_STATE_TIME",
    						   JDBC_HQ_DS_MAX_ACTIVE = "JDBC_HQ_DS_MAX_ACTIVE", 
    						   JDBC_HQ_DS_IN_USE = "JDBC_HQ_DS_IN_USE",
                               AVAIL_BACKFILLER_TIME = "AVAIL_BACKFILLER_TIME",
                               AGENT_PLUGIN_TRANSFER = "AGENT_PLUGIN_TRANSFER",
                               AGENT_PLUGIN_REMOVE = "AGENT_PLUGIN_REMOVE",
                               AGENT_SYNC_JOB_QUEUE_ADDS = "AGENT_SYNC_JOB_QUEUE_ADDS",
                               AVAIL_BACKFILLER_NUMPLATFORMS = "AVAIL_BACKFILLER_NUMPLATFORMS",
                               AGENT_PLUGIN_SYNC_PENDING_RESTARTS = "AGENT_PLUGIN_SYNC_PENDING_RESTARTS",
                               CMD_PING = "LATHER_" + CommandInfo.CMD_PING.toUpperCase(),
                               CMD_MEASUREMENT_SEND_REPORT = "LATHER_" + CommandInfo.CMD_MEASUREMENT_SEND_REPORT.toUpperCase(),
                               CMD_MEASUREMENT_GET_CONFIGS = "LATHER_" + CommandInfo.CMD_MEASUREMENT_GET_CONFIGS.toUpperCase(),
                               CMD_REGISTER_AGENT = "LATHER_" + CommandInfo.CMD_REGISTER_AGENT.toUpperCase(),
                               CMD_UPDATE_AGENT = "LATHER_" + CommandInfo.CMD_UPDATE_AGENT.toUpperCase(),
                               CMD_AI_SEND_REPORT = "LATHER_" + CommandInfo.CMD_AI_SEND_REPORT.toUpperCase(),
                               CMD_AI_SEND_RUNTIME_REPORT = "LATHER_" + CommandInfo.CMD_AI_SEND_RUNTIME_REPORT.toUpperCase(),
                               CMD_TRACK_SEND_LOG = "LATHER_" + CommandInfo.CMD_TRACK_SEND_LOG.toUpperCase(),
                               CMD_TRACK_SEND_CONFIG_CHANGE = "LATHER_" + CommandInfo.CMD_TRACK_SEND_CONFIG_CHANGE.toUpperCase(),
                               CMD_CONTROL_GET_PLUGIN_CONFIG = "LATHER_" + CommandInfo.CMD_CONTROL_GET_PLUGIN_CONFIG.toUpperCase(),
                               CMD_CONTROL_SEND_COMMAND_RESULT = "LATHER_" + CommandInfo.CMD_CONTROL_SEND_COMMAND_RESULT.toUpperCase(),
                               CMD_PLUGIN_SEND_REPORT = "LATHER_" + CommandInfo.CMD_PLUGIN_SEND_REPORT.toUpperCase(),
                               CMD_USERISVALID = "LATHER_" + CommandInfo.CMD_USERISVALID.toUpperCase(),
                               AGENT_PLUGIN_SYNC_RESTARTS = "AGENT_PLUGIN_SYNC_RESTARTS",
                               LATHER_RUN_COMMAND_TIME = "LATHER_RUN_COMMAND_TIME",
                               AGENT_SYNCHRONIZER_QUEUE_SIZE = "AGENT_SYNCHRONIZER_QUEUE_SIZE",
                               LATHER_REMOTE_EXCEPTION = "LATHER_REMOTE_EXCEPTION",
                               ZEVENT_REGISTERED_BUFFER_SIZE = "ZEVENT_REGISTERED_BUFFER_SIZE",
                               METRIC_NOTIFICATION_FILTERING_TIME = "METRIC_NOTIFICATION_FILTERING_TIME",
                               INVENTORY_NOTIFICATION_FILTERING_TIME = "INVENTORY_NOTIFICATION_FILTERING_TIME",
                               NOTIFICATIONS_PUBLISHED_TO_ENDPOINT = "NOTIFICATIONS_PUBLISHED_TO_ENDPOINT",
                               NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME = "NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME",
                               NOTIFICATION_TOTAL_QUEUE_SIZE = "NOTIFICATION_TOTAL_QUEUE_SIZE",
                               CMD_TOPN_SEND_REPORT = "LATHER_" + CommandInfo.CMD_TOPN_SEND_REPORT.toUpperCase(),
                               DYNAMIC_GROUP_PROCESS_EVENTS_TIME = "DYNAMIC_GROUP_PROCESS_EVENTS_TIME",
                               POLICY_MANAGER_PROCESS_EVENTS_TIME = "POLICY_MANAGER_PROCESS_EVENTS_TIME";
    private TaskScheduler taskScheduler;

    @Override
    protected ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long millis) {
        return taskScheduler.scheduleAtFixedRate(runnable, millis);
    }

    @Autowired
    public ConcurrentStatsCollector(MBeanServer mBeanServer,@Value("#{concurrentStatsScheduler}")TaskScheduler taskScheduler) {
        super(mBeanServer);
        this.taskScheduler = taskScheduler;
        registerInternalStats();
    }

    private final void registerInternalStats() {
        register(new StatCollector() {
            public String getId() {
                return EHCACHE_TOTAL_OBJECTS;
            }
            public long getVal() {
                long rtn = 0l;
                String[] caches = CacheManager.getInstance().getCacheNames();
                for (int i = 0; i < caches.length; i++) {
                    rtn += CacheManager.getInstance().getCache(caches[i]).getStatistics().getObjectCount();
                }
                return rtn;
            }
        });
        register(new StatSampler(new MBeanCollector(
            "TOMCAT_THREAD_POOL_QUEUE_SIZE", "Catalina:type=Executor,name=tomcatThreadPool",
            "queueSize", false)));
        register(new StatSampler(new MBeanCollector(
            "TOMCAT_THREAD_POOL_ACTIVE_COUNT", "Catalina:type=Executor,name=tomcatThreadPool",
            "activeCount", false)));
        register(new MBeanCollector(
            "HIBERNATE_2ND_LEVEL_CACHE_HITS", "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheHitCount", true));
        register(new MBeanCollector(
            "HIBERNATE_2ND_LEVEL_CACHE_MISSES", "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheMissCount", true));
        register(new MBeanCollector(
            "HIBERNATE_QUERY_CACHE_MISSES", "Hibernate:type=statistics,application=hq",
            "QueryCacheMissCount", true));
        register(new MBeanCollector(
            "HIBERNATE_QUERY_CACHE_HITS", "Hibernate:type=statistics,application=hq",
            "QueryCacheHitCount", true));
        register(new MBeanCollector(
            "ZEVENTS_PROCESSED", "hyperic.jmx:name=HQInternal", "ZeventsProcessed", true));
        register(new MBeanCollector(
            "ZEVENT_QUEUE_SIZE", "hyperic.jmx:name=HQInternal", "ZeventQueueSize", false));
        register(new MBeanCollector(
            "PLATFORM_COUNT", "hyperic.jmx:name=HQInternal", "PlatformCount", false));
        register(new MBeanCollector(
            "JMS_EVENT_TOPIC",
            "org.apache.activemq:BrokerName=localhost,Type=Topic,Destination=topic/eventsTopic",
            "QueueSize", true));
        register(new MBeanCollector(JDBC_HQ_DS_MAX_ACTIVE,
            "hyperic.jmx:type=DataSource,name=tomcat.jdbc", "MaxActive", false));
        register(new StatSampler(new MBeanCollector(JDBC_HQ_DS_IN_USE,
            "hyperic.jmx:type=DataSource,name=tomcat.jdbc", "Active", false)));
        register(CONCURRENT_STATS_COLLECTOR);
    }
    
    @PreDestroy
    @Override 
    public final void destory() { 
        super.destory() ; 
    }//EOM 

}

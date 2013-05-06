/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.filtering.ResourceDestinationEvaluator;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.Transformer;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class EndpointQueue {
    private final Log log = LogFactory.getLog(EndpointQueue.class);
// XXX should make this configurable in some way
    private static final int QUEUE_LIMIT = 100000;
    private static final long TASK_INTERVAL = 30000;
    private static final String NOTIFICATIONS_PUBLISHED_TO_ENDPOINT = ConcurrentStatsCollector.NOTIFICATIONS_PUBLISHED_TO_ENDPOINT;
    private static final String NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME = ConcurrentStatsCollector.NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME;
    private static final String NOTIFICATION_TOTAL_QUEUE_SIZE = ConcurrentStatsCollector.NOTIFICATION_TOTAL_QUEUE_SIZE;
// XXX should make this configurable in some way
    private static final int BATCH_SIZE = 25000;
    @Autowired
    private ThreadPoolTaskScheduler notificationExecutor;
    @Autowired
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    protected final static long EXPIRATION_DURATION = 10*60*1000;
    MetricDestinationEvaluator metricEvaluator;
    ResourceDestinationEvaluator resourceEvaluator;
    
    private final Map<String, AccumulatedRegistrationData> registrationData = new HashMap<String, AccumulatedRegistrationData>();
    private final AtomicInteger numConsumers = new AtomicInteger(0);

    public void register(NotificationEndpoint endpoint, Transformer<InternalNotificationReport, String> transformer) {
        register(endpoint,null,transformer);
    }
    
    public int getNumConsumers() {
        return numConsumers.get();
    }
    
    @PostConstruct
    public void init() {
        metricEvaluator = (MetricDestinationEvaluator) Bootstrap.getBean("metricDestinationEvaluator");
        resourceEvaluator = (ResourceDestinationEvaluator) Bootstrap.getBean("resourceDestinationEvaluator");
        concurrentStatsCollector.register(NOTIFICATIONS_PUBLISHED_TO_ENDPOINT);
        concurrentStatsCollector.register(NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME);
        concurrentStatsCollector.register(new StatCollector() {
            public long getVal() throws StatUnreachableException {
                synchronized (registrationData) {
                    long rtn = 0;
                    for (final AccumulatedRegistrationData data : registrationData.values()) {
                        rtn += data.getAccumulatedNotificationsQueue().size();
                    }
                    return rtn;
                }
            }
            public String getId() {
                return NOTIFICATION_TOTAL_QUEUE_SIZE;
            }
        });
    }

    public void register(NotificationEndpoint endpoint, InternalResourceDetailsType resourceDetailsType,
                         Transformer<InternalNotificationReport, String> transformer) {
        final boolean debug = log.isDebugEnabled();
        final AccumulatedRegistrationData data =
            new AccumulatedRegistrationData(endpoint, QUEUE_LIMIT, resourceDetailsType, registrationData);
        synchronized (registrationData) {
            if (registrationData.containsKey(endpoint.getRegistrationId())) {
                if (debug) log.debug("can not register endpoint=" + endpoint + " twice");
                return;
            }
            final String regId = endpoint.getRegistrationId();
            registrationData.put(regId, data);
            numConsumers.incrementAndGet();
            schedule(endpoint, data, transformer);
        }
        if (log.isDebugEnabled()) {
            log.debug("new notification registration=" + endpoint);
        }
    }
    
    private void schedule(final NotificationEndpoint endpoint, final AccumulatedRegistrationData data,
                          final Transformer<InternalNotificationReport, String> transformer) {
        if (!endpoint.canPublish()) {
            return;
        }
        final Runnable task = new Runnable() {
            protected long firstFailure=Long.MAX_VALUE;
            protected boolean isFailedLastPostage = false;
            public void run() {
                int size = 0;
                long totalTime = 0;
                try {
                    final String registrationId = endpoint.getRegistrationId();
                    InternalNotificationReport report = null;
                    final long start = System.currentTimeMillis();
                    final Collection<InternalAndExternalNotificationReports> messages =
                            new ArrayList<InternalAndExternalNotificationReports>();
                    List<InternalNotificationReport> reports = new ArrayList<InternalNotificationReport>();
                    while (report == null || !report.getNotifications().isEmpty()) {
                        report = poll(registrationId, BATCH_SIZE);
                        reports.add(report);
                        final String toPublish = transformer.transform(report);
                        messages.add(new InternalAndExternalNotificationReports(report,toPublish));
                        size += report.getNotifications().size();
                    }
                    List<InternalNotificationReport> failedReports = new ArrayList<InternalNotificationReport>();
                    EndpointStatus batchPostingStatus = endpoint.publishMessagesInBatch(messages,failedReports);
                    checkResultStatus(batchPostingStatus, failedReports);
                    totalTime = System.currentTimeMillis() - start;
                } catch (Throwable t) {
                    log.error(t, t);
                } finally {
                    concurrentStatsCollector.addStat(size, NOTIFICATIONS_PUBLISHED_TO_ENDPOINT);
                    concurrentStatsCollector.addStat(totalTime, NOTIFICATIONS_PUBLISHED_TO_ENDPOINT_TIME);
                }
            }
            private void checkResultStatus(EndpointStatus batchPostingStatus,
                                           List<InternalNotificationReport> failedReports) {
                if (!batchPostingStatus.isEmpty()) {
                    // retry the reports which were failed to be published 
                    for(InternalNotificationReport failedReport:failedReports) {
                        @SuppressWarnings("unchecked")
                        List<BaseNotification> failedNotifications = (List<BaseNotification>) failedReport.getNotifications();
                        Map<NotificationEndpoint, Collection<BaseNotification>> map = new HashMap<NotificationEndpoint, Collection<BaseNotification>>();
                        map.put(endpoint, failedNotifications);
                        publishAsync(map);
                    }
                    data.merge(batchPostingStatus);
                    // if the last try was a failure, it means that problem sending notifications
                    // to the endpoint has happened before finishing the whole messages transmission
                    if (batchPostingStatus.getLast().isSuccessful()) {
                        this.isFailedLastPostage=false;
                    } else {
                        if (!this.isFailedLastPostage) {
                            this.isFailedLastPostage=true;
                            this.firstFailure = batchPostingStatus.getLast().getTime();
                        }
                        if (System.currentTimeMillis() - this.firstFailure >= EXPIRATION_DURATION) {
                            unregister(endpoint.getRegistrationId());
                            metricEvaluator.unregisterAll(endpoint);
                            resourceEvaluator.unregisterAll(endpoint);
                        }
                    }
                }
            }
        };
        final Date start = new Date(System.currentTimeMillis() + TASK_INTERVAL);
        ScheduledFuture<?> schedule = notificationExecutor.scheduleWithFixedDelay(task, start, TASK_INTERVAL);
        data.setSchedule(schedule);
    }

    public NotificationEndpoint unregister(String regID) {
        AccumulatedRegistrationData data = null;
        synchronized (registrationData) {
            // don't delete the data, we want to be able to access the endpoint by registrationId
            data = registrationData.get(regID);
            if (data == null || !data.isValid()) {
                if(log.isDebugEnabled()){
                log.debug((data == null ? "No queue assigned" : "Queue is already invalid") + " for regId: " +
                        regID);
                }
                return null;
            }
            numConsumers.decrementAndGet();
            data.markInvalid();
            data.clear();
            final ScheduledFuture<?> schedule = data.getSchedule();
            if (schedule != null) {
                schedule.cancel(true);
            }
            if (log.isDebugEnabled()) {
                log.debug("Removing the queue assigned for regId: " + regID);
            }
        }
        return data.getNotificationEndpoint();
    }

    public InternalNotificationReport poll(String registrationId) {
        return poll(registrationId,Integer.MAX_VALUE);
    }

    public InternalNotificationReport poll(String registrationId, int maxSize) {
        final InternalNotificationReport rtn = new InternalNotificationReport();
        final List<BaseNotification> notifications = new ArrayList<BaseNotification>();
        synchronized (registrationData) {
            AccumulatedRegistrationData data = registrationData.get(registrationId);
            if (data == null || !data.isValid()) {
                return rtn;
            }
            data.drainTo(notifications, maxSize);
            rtn.setNotifications(notifications);
            rtn.setResourceDetailsType(data.getResourceContentType());
            return rtn;
        }
    }
    
    public <T extends BaseNotification> void publishAsync(Map<NotificationEndpoint, Collection<T>> map) {
        synchronized (registrationData) {
            for (final Entry<NotificationEndpoint, Collection<T>> entry : map.entrySet()) {
                final NotificationEndpoint endpoint = entry.getKey();
                final Collection<T> list = entry.getValue();
                final AccumulatedRegistrationData data = registrationData.get(endpoint.getRegistrationId());
                if (data != null) {
                    data.addAll(list);
                }
            }
        }
    }
    
    public NotificationEndpoint getEndpoint(String registrationID) {
        if (registrationID == null) {
            return null;
        }
        synchronized (registrationData) {
            AccumulatedRegistrationData data = registrationData.get(registrationID);
            if (data != null) {
                return data.getNotificationEndpoint();
            }
        }
        return null;
    }
    
    public EndpointAndRegStatus getEndpointAndRegStatus(String registrationID) {
        synchronized (registrationData) {
            AccumulatedRegistrationData ard = this.registrationData.get(registrationID);
            if (ard!=null) {
                RegistrationStatus regStat = new RegistrationStatus();
                regStat.setCreationTime(ard.getCreationTime());
                regStat.setValid(ard.isValid());
                EndpointStatus endpointStatus = null;
                EndpointStatus ardStatus = ard.getEndpointStatus();
                if (ardStatus!=null) {
                    endpointStatus = new EndpointStatus(ardStatus);
                }
                return new EndpointAndRegStatus(endpointStatus,regStat);
            } else {
                this.log.error("there is no AccumulatedRegistrationData for registration " + registrationID);
            }
        }
        return null;
    }
    
    public static class EndpointAndRegStatus {
        protected EndpointStatus endpointStatus;
        protected RegistrationStatus regStat;
        public EndpointAndRegStatus(EndpointStatus endpointStatus, RegistrationStatus regStat) {
            super();
            this.endpointStatus = endpointStatus;
            this.regStat = regStat;
        }
        public EndpointStatus getEndpointStatus() {
            return endpointStatus;
        }
        public RegistrationStatus getRegStatus() {
            return regStat;
        }
    }
}
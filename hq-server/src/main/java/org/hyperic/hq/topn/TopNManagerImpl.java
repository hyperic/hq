package org.hyperic.hq.topn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.server.session.TopNSchedule;
import org.hyperic.hq.measurement.server.session.TopNScheduleDAO;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@SuppressWarnings("restriction")
@Component
public class TopNManagerImpl implements ZeventListener<ResourceZevent>, TopNManager {

    public final static String TOPN_DEFAULT_INTERVAL = "TOPN_DEFAULT_INTERVAL";
    private final Log log = LogFactory.getLog(TopNManagerImpl.class);
    private final ZeventEnqueuer zEventManager;
    private final PlatformManager platformManager;
    private final MeasurementCommandsClientFactory measurementCommandsClientFactory;
    private final ServerConfigManager serverConfigManager;
    private final TopNScheduleDAO topNScheduleDAO;

    @Autowired
    public TopNManagerImpl(ZeventEnqueuer zEventManager, PlatformManager platformManager,
            MeasurementCommandsClientFactory measurementCommandsClientFactory,
            ServerConfigManager serverConfigManager, TopNScheduleDAO topNScheduleDAO) {
        this.zEventManager = zEventManager;
        this.platformManager = platformManager;
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
        this.serverConfigManager = serverConfigManager;
        this.topNScheduleDAO = topNScheduleDAO;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<? extends Zevent>> listenEvents = new HashSet<Class<? extends Zevent>>();
        listenEvents.add(ResourceCreatedZevent.class);
        listenEvents.add(ResourceUpdatedZevent.class);
        listenEvents.add(ResourceRefreshZevent.class);
        zEventManager.addBufferedListener(listenEvents, this);
    }

    @Transactional
    public void scheduleOrUpdateTopNCollection(int resourceId, int intervalInMinutes) {
        TopNSchedule schedule = null;
        Platform platform = platformManager.getPlatformByResourceId(resourceId);
        if (null == (schedule = topNScheduleDAO.get(resourceId))) {
            schedule = createTopNSchedule(platform,
                    Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_DEFAULT_INTERVAL)));
        } else {
            schedule.setEnabled(true);
            schedule.setIntervalInMinutes(intervalInMinutes);
            schedule.setLastUpdated(System.currentTimeMillis());
            scheduleTopNCollection(platform, schedule);
        }
        topNScheduleDAO.save(schedule);
    }

    @Transactional
    public void unscheduleTopNCollection(int resourceId) {
        TopNSchedule schedule = null;
        if (null == (schedule = topNScheduleDAO.get(resourceId))) {
            log.error("Cannot unschedule TopN collection for resource '" + resourceId + "', "
                    + "no such scheduling exists");
        }
        Platform platform = platformManager.getPlatformByResourceId(resourceId);
        MeasurementCommandsClient client = measurementCommandsClientFactory.getClient(platform.getAgent());
        try {
            client.unscheduleTopn();
            schedule.setEnabled(false);
            topNScheduleDAO.save(schedule);
        } catch (AgentRemoteException ex) {
            log.error("Error while unscheduling TopN", ex);
        } catch (AgentConnectionException ex) {
            log.error("Error while unscheduling TopN", ex);
        }

    }

    @Transactional
    public void deleteTopNCollection(int resourceId) {
        TopNSchedule schedule = null;
        if (null != (schedule = topNScheduleDAO.get(resourceId))) {
            topNScheduleDAO.remove(schedule);
        }
    }

    public void processEvents(List<ResourceZevent> list) {
        for (final ResourceZevent e : list) {
            final AppdefEntityID id = e.getAppdefEntityID();
            if (!id.isPlatform()) {
                return;
            }
            try {
                SessionManager.runInSession(new SessionRunner() {
                    public void run() throws Exception {
                        Platform platform = platformManager.getPlatformById(id.getId());
                        TopNSchedule schedule = null;
                        if (!platform.getPlatformType().getPlugin().equalsIgnoreCase("system")) {
                            return;
                        }
                        // TopN is supported only for agents with version >=
                        // 5.8.0
                        if (0 > platform.getAgent().getVersion().compareTo("5.8.0")) {
                            return;
                        }
                        if (e instanceof ResourceCreatedZevent) {
                            schedule = createTopNSchedule(platform,
                                    Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_DEFAULT_INTERVAL)));
                            topNScheduleDAO.save(schedule);
                            log.info("Scheduling TopN collection for platform '" + platform.getResource().getId() + "'");
                            scheduleTopNCollection(platform, schedule);
                        } else if (e instanceof ResourceRefreshZevent) {
                            schedule = topNScheduleDAO.get(platform.getResource().getId());
                            if (null == schedule) {
                                schedule = createTopNSchedule(platform,
                                        Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_DEFAULT_INTERVAL)));
                                topNScheduleDAO.save(schedule);
                            } else {
                                if (!schedule.isEnabled()) {
                                    return;
                                }
                                log.info("Rescheduling TopN collection for platform '" + platform.getResource().getId()
                                        + "'");
                            }
                            scheduleTopNCollection(platform, schedule);
                        } 

                    }

                    public String getName() {
                        return "ResponseTimeRefreshListener";
                    }
                });
            } catch (Exception ex) {
                throw new SystemException("Could not perform licensing update for new platforms", ex);
            }

        }

    }

    private void scheduleTopNCollection(Platform platform, TopNSchedule schedule) {
        MeasurementCommandsClient client = measurementCommandsClientFactory.getClient(platform.getAgent());
        try {
            client.scheduleTopn(new ScheduleTopn_args(schedule.getIntervalInMinutes(), null));
        } catch (AgentRemoteException ex) {
            log.error("Error while scheduling TopN", ex);
        } catch (AgentConnectionException ex) {
            log.error("Error while scheduling TopN", ex);
        }
    }

    private TopNSchedule createTopNSchedule(Platform platform, int interval) {
        TopNSchedule schedule = new TopNSchedule();
        schedule.setIntervalInMinutes(interval);
        schedule.setLastUpdated(System.currentTimeMillis());
        schedule.setResourceId(platform.getResource().getId());
        schedule.setEnabled(true);
        return schedule;
    }

    @Override
    public String toString() {
        return "ResponseTimeRefreshListener";
    }
}

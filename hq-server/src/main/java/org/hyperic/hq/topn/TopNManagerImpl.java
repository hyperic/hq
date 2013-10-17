/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2013], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.topn;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.data.TopNConfigurationProperties;
import org.hyperic.hq.measurement.server.session.TopNSchedule;
import org.hyperic.hq.measurement.server.session.TopNScheduleDAO;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.xerial.snappy.Snappy;


@Component
public class TopNManagerImpl implements ZeventListener<ResourceZevent>, TopNManager {

    public final static String TOPN_DEFAULT_INTERVAL = "TOPN_DEFAULT_INTERVAL";
    public final static String TOPN_NUMBER_OF_PROCESSES = "TOPN_NUMBER_OF_PROCESSES";
    private final Log log = LogFactory.getLog(TopNManagerImpl.class);
    private final ZeventEnqueuer zEventManager;
    private final PlatformManager platformManager;
    private final MeasurementCommandsClientFactory measurementCommandsClientFactory;
    private final ServerConfigManager serverConfigManager;
    private final AuthzSubjectManager authzSubjectManager;
    private final ConfigManager configManager;
    private final TopNScheduleDAO topNScheduleDao;

    @Autowired
    public TopNManagerImpl(ZeventEnqueuer zEventManager, PlatformManager platformManager,
            MeasurementCommandsClientFactory measurementCommandsClientFactory, ServerConfigManager serverConfigManager,
            AuthzSubjectManager authzSubjectManager, ConfigManager configManager, TopNScheduleDAO topNScheduleDao) {
        this.zEventManager = zEventManager;
        this.platformManager = platformManager;
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
        this.serverConfigManager = serverConfigManager;
        this.authzSubjectManager = authzSubjectManager;
        this.configManager = configManager;
        this.topNScheduleDao = topNScheduleDao;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<? extends Zevent>> listenEvents = new HashSet<Class<? extends Zevent>>();
        listenEvents.add(ResourceCreatedZevent.class);
        listenEvents.add(ResourceUpdatedZevent.class);
        listenEvents.add(ResourceRefreshZevent.class);
        listenEvents.add(ResourceDeletedZevent.class);
        zEventManager.addBufferedListener(listenEvents, this);
    }

    @Transactional
    public void scheduleTopNCollection(AppdefEntityID id, ConfigResponse config) {
        log.info("Rescheduling TopN collection for platform '" + id.getId()
                + "', since scheduling parameters has changed");
        Platform platform = platformManager.getPlatformById(id.getId());
        if(!isTopNSupported(platform)) {
            return;
        }

        updateScheduleObject(config, platform.getResource().getId());

        if (!config.getValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName()).equalsIgnoreCase("true")) {
            unscheduleTopNCollection(platform.getResource().getId(), config);
            return;
        }
        scheduleTopNCollection(platform, config);
    }

    public int getNumberOfProcessesToShowForPlatform(int resourceId) {
        Platform platform = platformManager.getPlatformByResourceId(resourceId);
        ConfigResponse config;
        try {
            config = configManager.getMergedConfigResponse(authzSubjectManager.getOverlordPojo(),
                    ProductPlugin.TYPE_MEASUREMENT, platform.getEntityId(), true);
        } catch (Exception e) {
            return -1;
        }
        return Integer.valueOf(config.getValue(TopNConfigurationProperties.TOPN_NUMBER_OF_PROCESSES.getName()));
    }

    @Transactional
    public void scheduleTopNCollection(int resourceId, int intervalInMinutes, int numberOfProcesses) {
        ConfigResponse config;
        Platform platform = platformManager.getPlatformByResourceId(resourceId);
        if(!isTopNSupported(platform)) {
            return;
        }
        try {
            config = configManager.getMergedConfigResponse(authzSubjectManager.getOverlordPojo(),
                    ProductPlugin.TYPE_MEASUREMENT, platform.getEntityId(), true);
        } catch (Exception e) {
            return;
        }

        configureTopNSchedule(platform, config, intervalInMinutes, numberOfProcesses);
        scheduleTopNCollection(platform, config);
    }

    @Transactional
    public void updateGlobalTopNInterval(int intervalInMinutes) {
        log.info("Updating Top Processes interval in minutes for all platforms to '" + intervalInMinutes + "'");
        try {
            Properties newProps = serverConfigManager.getConfig();
            newProps.put(TOPN_DEFAULT_INTERVAL, String.valueOf(intervalInMinutes));
            serverConfigManager.setConfig(authzSubjectManager.getOverlordPojo(), newProps);
        } catch (ApplicationException e) {
            log.error("Error updating TopN interval", e);
            return;
        } catch (ConfigPropertyException e) {
            log.error("Error updating TopN interval", e);
            return;
        }
        for (TopNSchedule schedule : topNScheduleDao.findAll()) {
            scheduleTopNCollection(schedule.getResourceId(), intervalInMinutes, schedule.getNumberOfProcesses());
        }
    }

    @Transactional
    public void updateGlobalTopNNumberOfProcesses(int numberOfProcesses) {
        log.info("Updating number of processes to collect for all platforms to '" + numberOfProcesses + "'");
        try {
            Properties newProps = serverConfigManager.getConfig();
            newProps.put(TOPN_NUMBER_OF_PROCESSES, String.valueOf(numberOfProcesses));
            serverConfigManager.setConfig(authzSubjectManager.getOverlordPojo(), newProps);
        } catch (ApplicationException e) {
            log.error("Error updating TopN interval", e);
            return;
        } catch (ConfigPropertyException e) {
            log.error("Error updating TopN interval", e);
            return;
        }
        for (TopNSchedule schedule : topNScheduleDao.findAll()) {
            scheduleTopNCollection(schedule.getResourceId(), schedule.getIntervalInMinutes(), numberOfProcesses);
        }

    }

    @Transactional
    public void unscheduleGlobalTopNCollection() {
        log.info("Disabling Top Processes collection for all platforms");
        try {
            Properties newProps = serverConfigManager.getConfig();
            newProps.put(TOPN_DEFAULT_INTERVAL, "0");
            serverConfigManager.setConfig(authzSubjectManager.getOverlordPojo(), newProps);
        } catch (ApplicationException e) {
            log.error("Error unscheduling TopN collection", e);
            return;
        } catch (ConfigPropertyException e) {
            log.error("Error unscheduling TopN collection", e);
            return;
        }
        for (TopNSchedule schedule : topNScheduleDao.findAll()) {
            if (schedule.isEnabled()) {
                unscheduleTopNCollection(schedule.getResourceId());
            }
        }

    }

    @Transactional
    public void unscheduleTopNCollection(int resourceId) {
        unscheduleTopNCollection(resourceId, null);
    }

    @Transactional
    public void unscheduleTopNCollection(int resourceId, ConfigResponse config) {
        log.info("Unscheduling Top Processes collection for resource '" + resourceId + "'");
        Platform platform = platformManager.getPlatformByResourceId(resourceId);
        if (null == config) {
            try {
                config = configManager.getMergedConfigResponse(authzSubjectManager.getOverlordPojo(),
                        ProductPlugin.TYPE_MEASUREMENT, platform.getEntityId(), true);
            } catch (Exception e) {
                return;
            }
        }
        MeasurementCommandsClient client = measurementCommandsClientFactory.getClient(platform.getAgent());

        // Update ConfigResponse
        config.setValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName(), false);
        persistConfigResponse(platform, config);

        // Update the TopNSchedule object
        TopNSchedule scheduleData = topNScheduleDao.get(resourceId);
        scheduleData.setEnabled(false);
        scheduleData.setLastUpdated(System.currentTimeMillis());
        topNScheduleDao.save(scheduleData);

        // Send the unschedule command to the client
        try {
            client.unscheduleTopn();
        } catch (AgentRemoteException ex) {
            log.error("Error while unscheduling TopN", ex);
        } catch (AgentConnectionException ex) {
            log.error("Error while unscheduling TopN", ex);
        }
    }

    private void persistConfigResponse(Platform platform, ConfigResponse config) {
        try {
            configManager.setConfigResponse(authzSubjectManager.getOverlordPojo(), platform.getEntityId(), config,
                    ProductPlugin.TYPE_MEASUREMENT, false, false);
        } catch (Exception e) {

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

                        if (e instanceof ResourceDeletedZevent) {
                            if(null != e.getResourceId()) {
                                TopNSchedule schedule = topNScheduleDao.get(e.getResourceId());
                                if (null != schedule) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Deleting TopNSchedule entry with id - '" + e.getResourceId() + "'");
                                    }
                                    topNScheduleDao.remove(schedule);
                                    topNScheduleDao.getSession().flush();
                                }
                            }
                            return;
                        }

                        Platform platform = platformManager.getPlatformById(id.getId());
                        if(!isTopNSupported(platform)) {
                            return;
                        }
                        if (Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_DEFAULT_INTERVAL)) <= 0) {
                            if (log.isDebugEnabled()) {
                                log.debug("TopN collection is disabled, not scheduling for platform '"
                                        + platform.getFqdn() + "'");
                            }
                            return;
                        }

                        ConfigResponse config = configManager.getMergedConfigResponse(
                                authzSubjectManager.getOverlordPojo(), ProductPlugin.TYPE_MEASUREMENT, id, true);


                        if (e instanceof ResourceCreatedZevent) {
                            configureDefaultTopNSchedule(platform, config);
                            log.info("Scheduling TopN collection for platform '" + platform.getResource().getId() + "'");
                            scheduleTopNCollection(platform, config);
                        }

                        else if (e instanceof ResourceUpdatedZevent) {
                            TopNSchedule schedule = topNScheduleDao.get(platform.getResource().getId());
                            if (null == schedule) {
                                configureDefaultTopNSchedule(platform, config);
                                log.info("Scheduling TopN collection for platform '" + platform.getResource().getId()
                                        + "'");
                                scheduleTopNCollection(platform, config);
                            } else {
                                int interval = Integer.valueOf(config
                                        .getValue(TopNConfigurationProperties.TOPN_COLLECTION_INTERVAL_IN_MINUTES
                                                .getName()));
                                int numberOfProcesses = Integer.valueOf(config
                                        .getValue(TopNConfigurationProperties.TOPN_NUMBER_OF_PROCESSES.getName()));
                                boolean enabled = Boolean.valueOf(config
                                        .getValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName()));
                                if ((interval != schedule.getIntervalInMinutes())
                                        || (numberOfProcesses != schedule.getNumberOfProcesses())
                                        || (enabled != schedule.isEnabled())) {

                                    topNScheduleDao.getSession().clear();
                                    scheduleTopNCollection(id, config);
                                }
                            }
                        }

                        else if (e instanceof ResourceRefreshZevent) {
                            TopNSchedule schedule = topNScheduleDao.get(platform.getResource().getId());
                            if ((null != schedule) && !schedule.isEnabled()) {
                                return;
                            }
                            log.info("Rescheduling TopN collection for platform '" + platform.getResource().getId()
                                    + "'");

                            if (null == schedule) {
                                configureDefaultTopNSchedule(platform, config);
                            }

                            scheduleTopNCollection(platform, config);
                        }

                    }

                    public String getName() {
                        return "TopNEventsListener";
                    }
                });
            } catch (Exception ex) {
                log.error(ex, ex);
                return;
            }

        }

    }

    private void configureDefaultTopNSchedule(Platform platform, ConfigResponse config) {
        if (log.isDebugEnabled()) {
            log.debug("Configuring ConfigResponse with default Top Processes parameters for platform '"
                    + platform.getName() + "'");
        }
        configureTopNSchedule(platform, config,
                Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_DEFAULT_INTERVAL)),
                Integer.valueOf(serverConfigManager.getPropertyValue(TOPN_NUMBER_OF_PROCESSES)));
    }

    private boolean isTopNSupported(Platform platform){
        if (null == platform) {
            return false;
        }
        if (!agentVersionValid(platform)) {
            return false;
        }
        if (!platform.getPlatformType().getPlugin().equalsIgnoreCase("system")) {
            return false;
        }
        return true;
    }

    private boolean agentVersionValid(Platform platform) {
        // TODO:Revisit this
        // TopN is supported only for agents with version >=
        // 5.8.0
        if (0 > platform.getAgent().getVersion().compareTo("5.8.0")) {
            if (log.isDebugEnabled()) {
                log.debug("The agent running on platform '" + platform.getName()
                        + "' version is older than 5.8, not schedulig Top Processes collection");
            }
            return false;
        }
        return true;
    }

    private void scheduleTopNCollection(Platform platform, ConfigResponse config) {
        if (!config.getValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName()).equalsIgnoreCase("true")) {
            return;
        }
        int interval = Integer.valueOf(config.getValue(TopNConfigurationProperties.TOPN_COLLECTION_INTERVAL_IN_MINUTES
                .getName()));
        int numberOfProcesses = Integer.valueOf(config.getValue(TopNConfigurationProperties.TOPN_NUMBER_OF_PROCESSES
                .getName()));

        MeasurementCommandsClient client = measurementCommandsClientFactory.getClient(platform.getAgent());
        try {
            client.scheduleTopn(new ScheduleTopn_args(interval, null, numberOfProcesses));
        } catch (AgentRemoteException ex) {
            log.error("Error while scheduling TopN", ex);
        } catch (AgentConnectionException ex) {
            log.error("Error while scheduling TopN", ex);
        }
    }

    private void configureTopNSchedule(Platform platform, ConfigResponse config, int interval, int numberOfProcesses) {
        config.setValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName(), true);
        config.setValue(TopNConfigurationProperties.TOPN_COLLECTION_INTERVAL_IN_MINUTES.getName(), interval);
        config.setValue(TopNConfigurationProperties.TOPN_NUMBER_OF_PROCESSES.getName(), numberOfProcesses);
        persistConfigResponse(platform, config);
        updateScheduleObject(platform.getResource().getId(), true, interval, numberOfProcesses);
    }

    private void updateScheduleObject(ConfigResponse config, int resourceId) {
        int interval = Integer.valueOf(config.getValue(TopNConfigurationProperties.TOPN_COLLECTION_INTERVAL_IN_MINUTES
                .getName()));
        int numberOfProcesses = Integer.valueOf(config.getValue(TopNConfigurationProperties.TOPN_NUMBER_OF_PROCESSES
                .getName()));
        boolean enabled = Boolean
                .valueOf(config.getValue(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName()));
        updateScheduleObject(resourceId, enabled, interval, numberOfProcesses);

    }

    private void updateScheduleObject(int resourceId, boolean enabled, int interval, int numberOfProcesses) {
        if (log.isDebugEnabled()) {
            log.debug("Updating 'TopNSchedule' object with the following attributes - resourceId='" + resourceId
                    + ", enabled='" + enabled + "', interval='" + interval + "', numberOfProcesses='"
                    + numberOfProcesses + "'");

        }
        TopNSchedule schedule = new TopNSchedule();
        schedule.setResourceId(resourceId);
        schedule.setLastUpdated(System.currentTimeMillis());
        schedule.setIntervalInMinutes(interval);
        schedule.setNumberOfProcesses(numberOfProcesses);
        schedule.setEnabled(enabled);
        topNScheduleDao.save(schedule);
    }


    public byte[] compressData(final byte[] data) {
        try {
            return Snappy.compress(data);
        } catch (final IOException e) {
            log.error("Unable to compress TopN data " + e.getClass().getSimpleName(), e);
            return data;
        }
    }

    public byte[] uncompressData(final byte[] data) {
        try {
            return Snappy.uncompress(data);
        } catch (final IOException e) {
            log.error("Unable to uncompress TopN data " + e.getClass().getSimpleName(), e);
            return data;
        }
    }
}
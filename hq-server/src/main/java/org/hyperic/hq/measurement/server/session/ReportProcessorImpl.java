/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.ValueList;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.ReportProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.hq.plugin.system.TopReport;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class ReportProcessorImpl implements ReportProcessor {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);

    private static final long MINUTE = MeasurementConstants.MINUTE;
    private static final long PRIORITY_OFFSET = MINUTE * 3;

    private final MeasurementManager measurementManager;
    private final PlatformManager platformManager;
    private final ServerManager serverManager;
    private final ServiceManager serviceManager;
    private final SRNManager srnManager;
    private final ReportStatsCollector reportStatsCollector;
    private final MeasurementInserterHolder measurementInserterManager;
    private final AgentManager agentManager;
    private final ZeventEnqueuer zEventManager;
    private final TopNManager topNManager;

    private final ResourceManager resourceManager;

    private final AgentScheduleSynchronizer agentScheduleSynchronizer;

    @Autowired
    public ReportProcessorImpl(MeasurementManager measurementManager,
                               PlatformManager platformManager, ServerManager serverManager,
                               ServiceManager serviceManager, SRNManager srnManager,
                               ReportStatsCollector reportStatsCollector,
                               MeasurementInserterHolder measurementInserterManager,
                               AgentManager agentManager, ZeventEnqueuer zEventManager,
            AgentScheduleSynchronizer agentScheduleSynchronizer, TopNManager topNManager,
            ResourceManager resourceManager) {
        this.measurementManager = measurementManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.srnManager = srnManager;
        this.reportStatsCollector = reportStatsCollector;
        this.measurementInserterManager = measurementInserterManager;
        this.agentManager = agentManager;
        this.zEventManager = zEventManager;
        this.agentScheduleSynchronizer = agentScheduleSynchronizer;
        this.topNManager = topNManager;
        this.resourceManager = resourceManager;
    }
    
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(SrnCheckerZevent.class, new SrnCheckerZeventListener());
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void addPoint(List<DataPoint> points, List<DataPoint> priorityPts, Measurement m,
                          MetricValue[] vals) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        for (MetricValue val : vals) {
            final long now = TimingVoodoo.roundDownTime(now(), MINUTE);
            try {
                // this is just to check if the metricvalue is valid
                // will throw a NumberFormatException if there is a problem
                new BigDecimal(val.getValue());
                DataPoint dataPoint = new DataPoint(m.getId(), val);
                if ((priorityPts != null) && isPriority(now, dataPoint.getTimestamp())) {
                    priorityPts.add(dataPoint);
                } else {
                    points.add(dataPoint);
                }
                if (debug) {
                    watch.markTimeBegin("getTemplate");
                }             
                if (debug && m.getTemplate().isAvailability()) {
                    log.debug("availability -> " + dataPoint);
                }
                if (debug) {
                    watch.markTimeEnd("getTemplate");
                }
            } catch (NumberFormatException e) {
                log.warn("Unable to insert: " + e.getMessage() + ", metric id=" + m);
            }
        }
        if (debug) {
            log.debug(watch);
        }
    }

    private final boolean isPriority(long timestamp, long metricTimestamp) {
        if (metricTimestamp >= (timestamp - PRIORITY_OFFSET)) {
            return true;
        }
        return false;
    }

    protected final void addData(List<DataPoint> points, List<DataPoint> priorityPts, Measurement m, MetricValue[] dpts) {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        if (debug) {
            watch.markTimeBegin("getInterval");
        }
        long interval = m.getInterval();
        if (debug) {
            watch.markTimeEnd("getInterval");
        }

        // Safeguard against an anomaly
        if (interval <= 0) {
            log.warn("Measurement had bogus interval[" + interval + "]: " + m);
            interval = 60 * 1000;
        }

        // Each datapoint corresponds to a set of measurement
        // values for that cycle.
        MetricValue[] passThroughs = new MetricValue[dpts.length];

        for (int i = 0; i < dpts.length; i++) {
            // Save data point to DB.
            if (debug) {
                watch.markTimeBegin("getTimestamp");
            }
            long retrieval = dpts[i].getTimestamp();
            if (debug) {
                watch.markTimeEnd("getTimestamp");
            }
            if (debug) {
                watch.markTimeBegin("roundDownTime");
            }
            long adjust = TimingVoodoo.roundDownTime(retrieval, interval);
            if (debug) {
                watch.markTimeEnd("roundDownTime");
            }

            // Create new Measurement data point with the adjusted time
            if (debug) {
                watch.markTimeBegin("new MetricValue");
            }
            MetricValue modified = new MetricValue(dpts[i].getValue(), adjust);
            if (debug) {
                watch.markTimeEnd("new MetricValue");
            }
            passThroughs[i] = modified;
        }
        if (debug) {
            watch.markTimeBegin("addPoint");
        }
        addPoint(points, priorityPts, m, passThroughs);
        if (debug) {
            watch.markTimeEnd("addPoint");
        }
        if (debug) {
            log.debug(watch);
        }
    }

    /**
     * Method which takes data from the agent (or elsewhere) and throws it into
     * the DataManager, doing the right things with all the derived measurements
     */
    public void handleMeasurementReport(MeasurementReport report) throws DataInserterException {
        final DSNList[] dsnLists = report.getClientIdList();
        final String agentToken = report.getAgentToken();

        final List<DataPoint> dataPoints = new ArrayList<DataPoint>(dsnLists.length);
        final List<DataPoint> availPoints = new ArrayList<DataPoint>(dsnLists.length);
        final List<DataPoint> priorityAvailPts = new ArrayList<DataPoint>(dsnLists.length);

        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final Set<AppdefEntityID> toUnschedule = new HashSet<AppdefEntityID>();
        Agent agent = null;
        try {
            agent = agentManager.getAgent(agentToken);
        } catch (AgentNotFoundException e) {
            log.error(e,e);
            return;
        }
        if (agent == null) {
            log.error("agent associated with token=" + agentToken + " is null, ignoring report");
            return;
        }
        final Platform platform = platformManager.getPlatformByAgentId(agent.getId());
        final Resource platformRes = (platform == null) ? null : platform.getResource();

        /**
         * idea here is that if an agent checks in don't wait for its platform
         * availability to come in. We know that the platform is up if the agent
         * checks in.
         */
        boolean setPlatformAvail = true;
        final Map<Integer, Boolean> alreadyChecked = new HashMap<Integer, Boolean>();
        final Map<Integer, Measurement> measMap = new HashMap<Integer, Measurement>();
        for (final DSNList dsnList : dsnLists) {
            final Integer mid = new Integer(dsnList.getClientId());
            if (debug) {
                watch.markTimeBegin("getMeasurement");
            }
            final Measurement m = getMeasurement(mid, measMap);
            if (debug) {
                watch.markTimeEnd("getMeasurement");
            }

            // Can't do much if we can't look up the derived measurement
            // If the measurement is enabled, we just throw away their data
            // instead of trying to throw it into the backfill. This is
            // because we don't know the interval to normalize those old
            // points for. This is still a problem for people who change their
            // collection period, but the instances should be low.
            if ((m == null) || !m.isEnabled()) {
                continue;
            }
            // Need to check if resource was asynchronously deleted (type == null)
            final Resource res = m.getResource();
            if ((res == null) || res.isInAsyncDeleteState()) {
                if (debug) {
                    log.debug("dropping metricId=" + m.getId() + " since resource is in async delete state");
                }
                continue;
            }
            if ((platformRes == null) || platformRes.getId().equals(res.getId())) {
                setPlatformAvail = false;
            }
            
            if (debug) {
                watch.markTimeBegin("resMatchesAgent");
            }
            // TODO reosurceMatchesAgent() and the call to getAgent() can be
            // consolidated, the agent match can be checked by getting the agent
            // for the instanceID from the resource
            Boolean match = alreadyChecked.get(res.getId());
            if (match == null) {
                match = resourceMatchesAgent(res, agent);
                alreadyChecked.put(res.getId(), match);
            }
            if (!match) {
                String ipAddr = agent.getAddress();
                String portString = agent.getPort().toString();
                if (debug) {
                    log.debug("measurement (id=" + m.getId() + ", name=" +
                              m.getTemplate().getName() + ") was sent to the " +
                              "HQ server from agent (agentToken=" + agentToken + ", name=" +
                              ipAddr + ", port=" + portString + ")" +
                              " but resource (id=" + res.getId() + ", name=" +
                              res.getName() + ") is not associated " +
                              " with that agent.  Dropping measurement.");
                    watch.markTimeEnd("resMatchesAgent");
                }
                toUnschedule.add(AppdefUtil.newAppdefEntityId(res));
                continue;
            }
            if (debug) {
                watch.markTimeEnd("resMatchesAgent");
            }
            
            final boolean isAvail = m.getTemplate().isAvailability();
            final ValueList[] valLists = dsnList.getDsns();
            if (debug) {
                watch.markTimeBegin("addData");
            }
            for (ValueList valList : valLists) {
                final MetricValue[] vals = valList.getValues();
                if (isAvail) {
                    addData(availPoints, priorityAvailPts, m, vals);
                } else {
                    addData(dataPoints, null, m, vals);
                }
            }
            if (debug) {
                watch.markTimeEnd("addData");
            }
        }
        //Since we are sending the Availability data and the Metrics data in 2 different batches
        //in agents with version >= 5.0, if this is an availability batch there is no need
        //to call the BatchAggregateDataInserter (Jira issue [HHQ-5566]) and if the BatchAggregateDataInserter
        //queue is available we will still be able to process the availability data
        if (!dataPoints.isEmpty()) {
            DataInserter<DataPoint> d = measurementInserterManager.getDataInserter();
            if (debug) {
                watch.markTimeBegin("sendMetricDataToDB");
            }
            sendMetricDataToDB(d, dataPoints, false);
            if (debug) {
                watch.markTimeEnd("sendMetricDataToDB");
            }
        }
        //Since we are sending the Availability data and the Metrics data in 2 different batches
        //in agents with version >= 5.0, if this is a measurements batch there is no need
        //to call the SynchronousAvailDataInserter (Jira issue [HHQ-5566])
        if (!availPoints.isEmpty() || !priorityAvailPts.isEmpty()) {
            DataInserter<DataPoint> a = measurementInserterManager.getAvailDataInserter();
            if (debug) {
                watch.markTimeBegin("sendAvailDataToDB");
            }
            sendMetricDataToDB(a, availPoints, false);
            sendMetricDataToDB(a, priorityAvailPts, true);
            if (debug) {
                watch.markTimeEnd("sendAvailDataToDB");
            }
        }

        // need to process these in background queue since I don't want cache misses to backup
        // report processor since it runs in several threads.  Better to backup one thread with
        // db queries
        zEventManager.enqueueEventAfterCommit(new SrnCheckerZevent(report.getSRNList(), toUnschedule, agentToken));

    }

    public void handleTopNReport(List<TopReport> reports, String agentToken) throws DataInserterException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        DataInserter<TopNData> d = measurementInserterManager.getTopNInserter();
        List<TopNData> topNs = new LinkedList<TopNData>();
        Agent agent = null;
        try {
            agent = agentManager.getAgent(agentToken);
        } catch (AgentNotFoundException e) {
            log.warn("Recieved a Top Process report from an unknown agent with token '" + agentToken
                    + "' , ignoring the report");
            return;
        }
        if (agent == null) {
            log.error("agent associated with token=" + agentToken + " is null, ignoring report");
            return;
        }
        final Platform platform = platformManager.getPlatformByAgentId(agent.getId());
        final Resource platformRes = (platform == null) ? null : platform.getResource();
        if (platformRes == null) {
            return;
        }
        int resourceId = platformRes.getId();
        for (TopReport report : reports) {
            Date minute = DateUtils.truncate(new Date(report.getCreateTime()), Calendar.MINUTE);
            try {
                TopNData topNData = new TopNData(resourceId, minute, report.toSerializedForm());
                topNs.add(topNData);
            } catch (IOException e) {
                log.error("Error serializing TopN data: " + e, e);
            }
        }

        if (debug) { watch.markTimeBegin("insertTopNToDB"); }
        try {
            d.insertData(topNs);
        } catch (InterruptedException e) {
            throw new SystemException("Interrupted while attempting to insert topN data", e);
        }
        if (debug) { watch.markTimeEnd("insertTopNToDB"); }
    }

    protected Measurement getMeasurement(Integer mid, Map<Integer, Measurement> measMap) {
        Measurement measurement = measMap.get(mid);
        if (measurement == null) {
            measurement = measurementManager.getMeasurement(mid);
            measMap.put(mid, measurement);
        }
        return measurement;
    }

    /**
     * checks if the agentToken matches resource's agentToken
     */
    protected boolean resourceMatchesAgent(Resource resource, Agent agent) {
        if ((resource == null) || resource.isInAsyncDeleteState()) {
            return false;
        }
        final Integer resType = resource.getResourceType().getId();
        final Integer aeid = resource.getInstanceId();
        try {
            if (resType.equals(AuthzConstants.authzPlatform)) {
                Platform p = platformManager.findPlatformById(aeid);
                Resource r = p.getResource();
                if ((r == null) || r.isInAsyncDeleteState()) {
                    return false;
                }
                Agent a = p.getAgent();
                if (a == null) {
                    return false;
                }
                return a.getId().equals(agent.getId());
            } else if (resType.equals(AuthzConstants.authzServer)) {
                Server server = serverManager.findServerById(aeid);
                Resource r = server.getResource();
                if ((r == null) || r.isInAsyncDeleteState()) {
                    return false;
                }
                Platform p = server.getPlatform();
                if (p == null) {
                    return false;
                }
                r = p.getResource();
                if ((r == null) || r.isInAsyncDeleteState()) {
                    return false;
                }
                Agent a = p.getAgent();
                if (a == null) {
                    return false;
                }
                return a.getId().equals(agent.getId());
            } else if (resType.equals(AuthzConstants.authzService)) {
               Service service =serviceManager.findServiceById(aeid);
               Resource r = service.getResource();
               if ((r == null) || r.isInAsyncDeleteState()) {
                   return false;
               }
               Server server = service.getServer();
               if (server == null) {
                   return false;
               }
               r = server.getResource();
               if ((r == null) || r.isInAsyncDeleteState()) {
                   return false;
               }
               Platform p = server.getPlatform();
               if (p == null) {
                   return false;
               }
               r = p.getResource();
               if ((r == null) || r.isInAsyncDeleteState()) {
                   return false;
               }
               Agent a = p.getAgent();
               if (a == null) {
                   return false;
               }
               return a.getId().equals(agent.getId());
            }
        } catch (PlatformNotFoundException e) {
            log.warn("Platform not found Id=" + aeid);
        } catch (ServerNotFoundException e) {
            log.warn("Server not found Id=" + aeid);
        } catch (ServiceNotFoundException e) {
            log.warn("Service not found Id=" + aeid);
        }
        return false;
    }

    /**
     * Sends the actual data to the DB.
     */
    private void sendMetricDataToDB(DataInserter<DataPoint> d, List<DataPoint> dataPoints, boolean isPriority)
        throws DataInserterException {
        if (dataPoints.size() <= 0) {
            return;
        }
        try {
            d.insertData(dataPoints, isPriority);
            int size = dataPoints.size();
            long ts = now();
            reportStatsCollector.getCollector().add(size, ts);
        } catch (InterruptedException e) {
            throw new SystemException("Interrupted while attempting to " + "insert data");
        }
    }
    

    

    private class SrnCheckerZevent extends Zevent {
        private final String agentToken;
        private final Set<AppdefEntityID> toUnschedule;
        private final SRN[] srnList;
        @SuppressWarnings("serial")
        public SrnCheckerZevent(SRN[] srnList, Set<AppdefEntityID> toUnschedule, String agentToken) {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
            this.srnList = srnList;
            this.toUnschedule = toUnschedule;
            this.agentToken = agentToken;
        }
        public String getAgentToken() {
            return agentToken;
        }
        public Set<AppdefEntityID> getToUnschedule() {
            return toUnschedule;
        }
        public SRN[] getSrnList() {
            return srnList;
        }
    }
    
    private class SrnCheckerZeventListener implements ZeventListener<SrnCheckerZevent> {
        private static final long ONE_HOUR = MeasurementConstants.HOUR;
        private final Map<AppdefEntityID, Long> lastCheck = new HashMap<AppdefEntityID, Long>();
        private final Map<AppdefEntityID, Long> lastUnscheduleCheck = new HashMap<AppdefEntityID, Long>();
        public void processEvents(List<SrnCheckerZevent> events) {
            for (final SrnCheckerZevent z : events) {
                final Collection<AppdefEntityID> list = new ArrayList<AppdefEntityID>();
                final Collection<SRN> srns = new ArrayList<SRN>();
                final long now = now();
                for (final SRN srn : z.getSrnList()) {
                    final AppdefEntityID aeid = srn.getEntity();
                    final long last = getLastCheck(aeid, lastCheck);
                    if ((last + ONE_HOUR) < now) {
                        list.add(aeid);
                        srns.add(srn);
                        lastCheck.put(aeid, now);
                    }
                }
                final Collection<AppdefEntityID> toUnschedule = new ArrayList<AppdefEntityID>();
                for (final AppdefEntityID aeid : z.getToUnschedule()) {
                    final long last = getLastCheck(aeid, lastUnscheduleCheck);
                    if ((last + ONE_HOUR) < now) {
                        list.add(aeid);
                        lastUnscheduleCheck.put(aeid, now);
                    }
                }
                agentScheduleSynchronizer.unschedule(z.getAgentToken(), toUnschedule);
                agentScheduleSynchronizer.unscheduleNonEntities(z.getAgentToken(), list);
                srnManager.rescheduleOutOfSyncSrns(srns, false);
            }
        }
        private long getLastCheck(AppdefEntityID aeid, Map<AppdefEntityID, Long> lastCheck) {
            final Long last = lastCheck.get(aeid);
            return (last == null) ? 0l : last;
        }
    }

}

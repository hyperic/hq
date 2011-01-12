/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.agent.client.AgentMonitor;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Transactional
public class MeasurementProcessorImpl implements MeasurementProcessor {
    private static final String LOG_CTX = MeasurementProcessorImpl.class.getName();
    private final Log log = LogFactory.getLog(LOG_CTX);

    private AgentManager agentManager;
    private AvailabilityManager availManager;
    private MeasurementManager measurementManager;
    private SRNManager srnManager;
    private AgentMonitor agentMonitor;
    private MeasurementCommandsClientFactory measurementCommandsClientFactory;
    private ResourceManager resourceManager;
    private ZeventEnqueuer zEventManager;
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    @Autowired
    public MeasurementProcessorImpl(AgentManager agentManager, MeasurementManager measurementManager,
                                    SRNManager srnManager,
                                    AgentMonitor agentMonitor,
                                    MeasurementCommandsClientFactory measurementCommandsClientFactory, 
                                    ResourceManager resourceManager, AvailabilityManager availManager,
                                    ZeventEnqueuer zEventManager, ConcurrentStatsCollector concurrentStatsCollector) {

        this.agentManager = agentManager;
        this.measurementManager = measurementManager;
        this.srnManager = srnManager;
        this.agentMonitor = agentMonitor;
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
        this.resourceManager = resourceManager;
        this.availManager = availManager;
        this.zEventManager = zEventManager;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void initStatsCollector() {
    	concurrentStatsCollector.register(ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
    }

    /**
     * Ping the agent to make sure it's up
     */
    public boolean ping(Agent a) throws PermissionException {
        return agentMonitor.ping(a);
    }
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * 
     */
    public void scheduleHierarchyAfterCommit(Collection<Resource> resources) {
        if (resources.isEmpty()) {
            return;
        }
       
        final Set<AppdefEntityID> aeids = new HashSet<AppdefEntityID>(resources.size()+1);
        for (final Resource resource : resources ) {
            if (resource == null || resource.isInAsyncDeleteState()) {
                continue;
            }
            final Set<Resource> children= resource.getChildren(true);
            aeids.add(AppdefUtil.newAppdefEntityId(resource));
            for (Resource child: children ) { 
                if (child == null || child.isInAsyncDeleteState()) {
                    continue;
                }
                aeids.add(AppdefUtil.newAppdefEntityId(child));
            }
        }
        if (!aeids.isEmpty()) {
            final AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(aeids);
            zEventManager.enqueueEventAfterCommit(event);
        }
    }
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * 
     */
    public void scheduleHierarchyAfterCommit(Resource resource) {
        scheduleHierarchyAfterCommit(Collections.singletonList(resource));
    }

    /**
     * @param aeids {@link List} of {@link AppdefEntityID}
     */
    public void scheduleSynchronous(List<AppdefEntityID> aeids) {
        try {
            Map<Integer, Collection<AppdefEntityID>> agents = getAgentMap(aeids);
            for (Map.Entry<Integer, Collection<AppdefEntityID>> entry : agents.entrySet()) {
                Agent agent = agentManager.findAgent(entry.getKey());
                Collection<AppdefEntityID> entityIds = entry.getValue();
                scheduleEnabled(agent, entityIds);
            }
        } catch (Exception e) {
            log.error("Exception scheduling [" + aeids + "]: " + e.getMessage(), e);
        }
    }

    /**
     * @return Map of {@link Agent} to {@link List<AppdefEntityID>}
     */
    private Map<Integer, Collection<AppdefEntityID>> getAgentMap(Collection<AppdefEntityID> aeids) {
        return agentManager.getAgentMap(aeids);
    }

    /**
     * @param eids List<AppdefEntityID>
     */
    //TODO was propagation not supported - any benefit to suspending tx?
    @Transactional(readOnly=true)
    public void scheduleEnabled(Agent agent, Collection<AppdefEntityID> eids) throws MonitorAgentException {
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        if (debug) watch.markTimeBegin("findEnabledMeasurements");
        Map<Integer,List<Measurement>> measMap = measurementManager.findEnabledMeasurements(eids);
        if (debug) watch.markTimeEnd("findEnabledMeasurements");
        
        // availability measurements in scheduled downtime are disabled, but not unscheduled.
        // need to schedule these measurements for "new" agents.
        if (debug) watch.markTimeBegin("getAvailMeasurementsInDowntime");
        Map<Integer, Measurement> downtimeMeasMap = availManager.getAvailMeasurementsInDowntime(eids);
        if (debug) watch.markTimeEnd("getAvailMeasurementsInDowntime");
                
        for (Map.Entry<Integer, Measurement> entry : downtimeMeasMap.entrySet()) {
            Integer resourceId = entry.getKey();
            List<Measurement> measurements = measMap.get(resourceId);
            if (measurements == null) {
                measurements = new ArrayList<Measurement>();
                measMap.put(resourceId, measurements);
            }
            measurements.add(entry.getValue());
        }
        
        // Want to batch this operation.  There is something funky with the agent where it
        // appears to slow down drastically when too many measurements are scheduled at
        // once.  I believe (not 100% sure) this is due to the agent socket listener
        // not being multi-threaded and processing the scheduled measurements one-by-one while
        // reading the socket. Once that is enhanced it should be fine to remove the batching here.
        final int batchSize = 100;
        final List<AppdefEntityID> aeids = new ArrayList<AppdefEntityID>(eids);
        for (int i=0; i<aeids.size(); i+=batchSize) {
            final int end = Math.min(i+batchSize, aeids.size());
            if (debug) watch.markTimeBegin("scheduleMeasurements[" + end + "]");
            scheduleMeasurements(agent, measMap, aeids.subList(i, end));
            if (debug) watch.markTimeEnd("scheduleMeasurements[" + end + "]");
        }
        if (debug) {
            log.debug("scheduleEnabled: " + watch
                        + ", { Size: [appdefEntity=" + eids.size()
                        + "] [availMeasurementsInDowntime=" + downtimeMeasMap.size() 
                        + "] }");
        }
    }
    
    private void scheduleMeasurements(Agent agent, Map<Integer,List<Measurement>> measMap,
                                      Collection<AppdefEntityID> eids)
    throws MonitorAgentException {
        final boolean debug = log.isDebugEnabled();
        final Map<SRN,List<Measurement>> schedMap = new HashMap<SRN,List<Measurement>>();
      
        MeasurementCommandsClient client = null;
      
        final StringBuilder debugBuf = new StringBuilder();
        try {
            client = measurementCommandsClientFactory.getClient(agent);
           
            for (AppdefEntityID eid : eids ) {
                final long begin = now();
                int srnNumber = srnManager.incrementSrn(eid, Long.MAX_VALUE);
                SRN srn = new SRN(eid, srnNumber);
                Resource r = resourceManager.findResource(eid);
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                List<Measurement> measurements = measMap.get(r.getId());
                if (measurements == null) {
                    continue;
                }
                schedMap.put(srn, measurements);
                try {
                    if (debug) {
                        debugBuf.append("scheduling mids=")
                                .append(measurements)
                                .append(" for aeid=")
                                .append(eid)
                                .append("\n");
                    }
                    Measurement[] array = (Measurement[])measurements.toArray(new Measurement[0]);
                    agentMonitor.schedule(client, srn, array);
                    concurrentStatsCollector.addStat((now()-begin), ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
                } catch (AgentConnectionException e) {
                    final String emsg = "Error reported by agent @ "
                        + agent.connectionString() 
                        +  ": " + e.getMessage();
                    log.warn(emsg);
                    throw new MonitorAgentException(e.getMessage(), e);
                } catch (AgentRemoteException e) {
                    final String emsg = "Error reported by agent @ "
                        + agent.connectionString() 
                        +  ": " + e.getMessage();
                    log.warn(emsg);
                    throw new MonitorAgentException(emsg, e);
                }
            }
        } finally {
            if (debug) log.debug(debugBuf);
            if (client != null) {
                try {
                    client.closeConnection();
                } catch (AgentRemoteException e) {
                    log.error(e);
                }
            }
        }
    }

    private final long now() {
        return System.currentTimeMillis();
    }

    private void unschedule(Agent a, Collection<AppdefEntityID> entIds) throws MeasurementUnscheduleException,
        MonitorAgentException {
        if (log.isDebugEnabled()) {
            log.debug("unschedule agentId=" + a.getId() + ", numOfResources=" + entIds.size());
        }
        for (AppdefEntityID entId: entIds) {
            try {
                srnManager.removeSrn(entId);
            } catch (EntityNotFoundException e) {
                // Ok to ignore, this is the first time scheduling metrics
                // for this resource.
            }
        }
        List<AppdefEntityID> tmp = new ArrayList<AppdefEntityID>(entIds);
        agentMonitor.unschedule(a, tmp.toArray(new AppdefEntityID[0]));
    }

    /**
     * Unschedule metrics of multiple appdef entities
     * @param agentToken the entity whose agent will be contacted for the
     *        unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    @Transactional(readOnly=true)
    public void unschedule(String agentToken, Collection<AppdefEntityID> entIds) throws MeasurementUnscheduleException {
        try {
            // Get the agent from agent token
            Agent a = agentManager.getAgent(agentToken);
            unschedule(a, entIds);
        } catch (MonitorAgentException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        } catch (AgentNotFoundException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        }
    }

    /**
     * Unschedule metrics of multiple appdef entities
     * @param agentEnt the entity whose agent will be contacted for the
     *        unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    @Transactional(readOnly=true)
    public void unschedule(AppdefEntityID agentEnt, AppdefEntityID[] entIds) throws MeasurementUnscheduleException {
        try {
            // Get the agent IP and Port from server ID
            Agent a = agentManager.getAgent(agentEnt);
            unschedule(a, Arrays.asList(entIds));
        } catch (MonitorAgentException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        } catch (AgentNotFoundException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        }
    }

    /**
     * Unschedule measurements
     * @param aeids List of {@link AppdefEntityID}
     * @throws MeasurementUnscheduleException if an error occurs
     */
    @Transactional(readOnly=true)
    public void unschedule(Collection<AppdefEntityID> aeids) throws MeasurementUnscheduleException {
        Map<Integer, Collection<AppdefEntityID>> agents = getAgentMap(aeids);
        for (Map.Entry<Integer, Collection<AppdefEntityID>> entry : agents.entrySet()) {
            Agent agent = agentManager.findAgent((Integer) entry.getKey());
            Collection<AppdefEntityID> eids = entry.getValue();
            unschedule(agent.getAgentToken(), eids);
        }
    }

}

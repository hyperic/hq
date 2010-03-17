/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.agent.client.AgentMonitor;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
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
    private MeasurementManager measurementManager;
    private AuthzSubjectManager authzSubjectManager;
    private SRNManager srnManager;
    private AgentMonitor agentMonitor;
    private MeasurementCommandsClientFactory measurementCommandsClientFactory;

    @Autowired
    public MeasurementProcessorImpl(AgentManager agentManager, MeasurementManager measurementManager,
                                    AuthzSubjectManager authzSubjectManager, SRNManager srnManager,
                                    AgentMonitor agentMonitor,
                                    MeasurementCommandsClientFactory measurementCommandsClientFactory) {

        this.agentManager = agentManager;
        this.measurementManager = measurementManager;
        this.authzSubjectManager = authzSubjectManager;
        this.srnManager = srnManager;
        this.agentMonitor = agentMonitor;
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
    }
    
    @PostConstruct
    public void initStatsCollector() {
        ConcurrentStatsCollector.getInstance().register(ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
    }

    /**
     * Ping the agent to make sure it's up
     */
    public boolean ping(Agent a) throws PermissionException {
        return agentMonitor.ping(a);
    }

    /**
     * @param aeids {@link List} of {@link AppdefEntityID}
     */
    public void scheduleSynchronous(List<AppdefEntityID> aeids) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        try {
            Map<Integer, List<AppdefEntityID>> agents = getAgentMap(aeids);
            for (Map.Entry<Integer, List<AppdefEntityID>> entry : agents.entrySet()) {
                Agent agent = agentManager.findAgent(entry.getKey());
                List<AppdefEntityID> entityIds = entry.getValue();
                if (debug) {
                    watch.markTimeBegin("scheduleEnabled");
                }
                scheduleEnabled(agent, entityIds);
                if (debug) {
                    watch.markTimeEnd("scheduleEnabled");
                }
            }
        } catch (Exception e) {
            log.error("Exception scheduling [" + aeids + "]: " + e.getMessage(), e);
        }
        if (debug) {
            log.debug(watch);
        }
    }

    /**
     * @return Map of {@link Agent} to {@link List<AppdefEntityID>}
     */
    private Map<Integer, List<AppdefEntityID>> getAgentMap(List<AppdefEntityID> aeids) {
        Map<Integer, List<AppdefEntityID>> rtn = new HashMap<Integer, List<AppdefEntityID>>(aeids.size());
        for (AppdefEntityID eid : aeids) {
            Integer agentId;
            try {
                List<AppdefEntityID> tmp;
                agentId = agentManager.getAgent(eid).getId();
                if (null == (tmp = (List<AppdefEntityID>) rtn.get(agentId))) {
                    tmp = new ArrayList<AppdefEntityID>();
                    rtn.put(agentId, tmp);
                }
                tmp.add(eid);
            } catch (AgentNotFoundException e) {
                log.warn(e.getMessage());
            }
        }
        return rtn;
    }

    /**
     * @param eids List<AppdefEntityID>
     */
    public void scheduleEnabled(Agent agent, List<AppdefEntityID> eids) throws MonitorAgentException {
        final Map<SRN, List<Measurement>> schedMap = new HashMap<SRN, List<Measurement>>();

        MeasurementCommandsClient client = null;
        try {
            final ConcurrentStatsCollector stats = ConcurrentStatsCollector.getInstance();
            client = measurementCommandsClientFactory.getClient(agent);

            final AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
            for (AppdefEntityID eid : eids) {
                final long begin = now();
                List<Measurement> measurements = measurementManager.findEnabledMeasurements(overlord, eid, null);
                int srnNumber = srnManager.incrementSrn(eid, Long.MAX_VALUE);
                SRN srn = new SRN(eid, srnNumber);
                schedMap.put(srn, measurements);
                try {
                    Measurement[] meas = (Measurement[]) measurements.toArray(new Measurement[0]);
                    agentMonitor.schedule(client, srn, meas);
                    stats.addStat((now() - begin), ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
                } catch (AgentConnectionException e) {
                    final String emsg = "Error reported by agent @ " + agent.connectionString() + ": " + e.getMessage();
                    log.warn(emsg);
                    throw new MonitorAgentException(e.getMessage(), e);
                } catch (AgentRemoteException e) {
                    final String emsg = "Error reported by agent @ " + agent.connectionString() + ": " + e.getMessage();
                    log.warn(emsg);
                    throw new MonitorAgentException(emsg, e);
                }
            }
        } finally {
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

    private void unschedule(Agent a, AppdefEntityID[] entIds) throws MeasurementUnscheduleException,
        MonitorAgentException {

        for (int i = 0; i < entIds.length; i++) {
            try {
                srnManager.removeSrn(entIds[i]);
            } catch (ObjectNotFoundException e) {
                // Ok to ignore, this is the first time scheduling metrics
                // for this resource.
            }
        }
        agentMonitor.unschedule(a, entIds);
    }

    /**
     * Unschedule metrics of multiple appdef entities
     * @param agentToken the entity whose agent will be contacted for the
     *        unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    public void unschedule(String agentToken, AppdefEntityID[] entIds) throws MeasurementUnscheduleException {
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
    public void unschedule(AppdefEntityID agentEnt, AppdefEntityID[] entIds) throws MeasurementUnscheduleException {
        try {
            // Get the agent IP and Port from server ID
            Agent a = agentManager.getAgent(agentEnt);
            unschedule(a, entIds);
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
    public void unschedule(List<AppdefEntityID> aeids) throws MeasurementUnscheduleException {
        Map<Integer, List<AppdefEntityID>> agents = getAgentMap(aeids);
        for (Map.Entry<Integer, List<AppdefEntityID>> entry : agents.entrySet()) {
            Agent agent = agentManager.findAgent((Integer) entry.getKey());
            List<AppdefEntityID> eids = entry.getValue();
            unschedule(agent.getAgentToken(), eids.toArray(new AppdefEntityID[0]));
        }
    }

}

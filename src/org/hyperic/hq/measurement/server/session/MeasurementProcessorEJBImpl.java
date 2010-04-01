/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.agent.client.AgentMonitor;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.stats.ConcurrentStatsCollector;
import org.hyperic.util.timer.StopWatch;

/**
 * @ejb:bean name="MeasurementProcessor"
 *      jndi-name="ejb/measurement/MeasurementProcessor"
 *      local-jndi-name="LocalMeasurementProcessor"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class MeasurementProcessorEJBImpl 
    extends SessionEJB 
    implements SessionBean 
{
    private final String logCtx = MeasurementProcessorEJBImpl.class.getName();
    private Log log = LogFactory.getLog(logCtx);

    /**
     * Ping the agent to make sure it's up
     * @ejb:interface-method
     */
    public boolean ping(Agent a)
        throws PermissionException {

        AgentMonitor monitor = new AgentMonitor();
        return monitor.ping(a);
    }
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * @ejb:interface-method
     */
    public void scheduleHierarchyAfterCommit(Collection resources) {
        if (resources.isEmpty()) {
            return;
        }
        final ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        final ArrayList aeids = new ArrayList(resources.size()+1);
        for (final Iterator xx=resources.iterator(); xx.hasNext(); ) {
            final Resource resource = (Resource) xx.next();
            if (resource == null || resource.isInAsyncDeleteState()) {
                continue;
            }
            final Collection edges = rMan.findResourceEdges(rMan.getContainmentRelation(), resource);
            aeids.ensureCapacity(aeids.size()+edges.size()+1);
            aeids.add(new AppdefEntityID(resource));
            for (final Iterator it=edges.iterator(); it.hasNext(); ) {
                final ResourceEdge e = (ResourceEdge) it.next();
                final Resource r = e.getTo();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                aeids.add(new AppdefEntityID(e.getTo()));
            }
        }
        if (!aeids.isEmpty()) {
            final AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(aeids);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }
    }
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * @ejb:interface-method
     */
    public void scheduleHierarchyAfterCommit(Resource resource) {
        scheduleHierarchyAfterCommit(Collections.singletonList(resource));
    }

    /**
     * @param aeids {@link List} of {@link AppdefEntityID}
     * @ejb:transaction type="NotSupported"
     * @ejb:interface-method
     */
    public void scheduleSynchronous(List aeids) {
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        try {
            Map agents = getAgentMap(aeids);
            for (Iterator it=agents.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Agent agent = aMan.findAgent((Integer)entry.getKey());
                Collection entityIds = (Collection)entry.getValue();
                scheduleEnabled(agent, entityIds);
            }
        } catch(Exception e) {
            log.error("Exception scheduling [" + aeids + "]: " + e.getMessage(), e);
        }
    }
    
    /**
     * @return Map of {@link Agent} to {@link List<AppdefEntityID>}
     */
    private Map getAgentMap(Collection aeids) {
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        return aMan.getAgentMap(aeids);
    }

    /**
     * @param eids List<AppdefEntityID>
     * @ejb:interface-method
     */
    public void scheduleEnabled(Agent agent, Collection eids)
    throws MonitorAgentException {
        final MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        if (debug) watch.markTimeBegin("findEnabledMeasurements");
        Map measMap = mMan.findEnabledMeasurements(eids);
        if (debug) watch.markTimeEnd("findEnabledMeasurements");
        // Want to batch this operation.  There is something funky with the agent where it
        // appears to slow down drastically when too many measurements are scheduled at
        // once.  I believe (not 100% sure) this is due to the agent socket listener
        // not being multi-threaded and processing the scheduled measurements one-by-one while
        // reading the socket. Once that is enhanced it should be fine to remove the batching here.
        final int batchSize = 100;
        final List aeids = new ArrayList(eids);
        for (int i=0; i<aeids.size(); i+=batchSize) {
            final int end = Math.min(i+batchSize, aeids.size());
            if (debug) watch.markTimeBegin("scheduleMeasurements");
            scheduleMeasurements(agent, measMap, aeids.subList(i, end));
            if (debug) watch.markTimeEnd("scheduleMeasurements");
        }
        if (debug) log.debug(watch);
    }

    private void scheduleMeasurements(Agent agent, Map measMap, Collection eids)
    throws MonitorAgentException {
        final boolean debug = log.isDebugEnabled();
        final Map schedMap = new HashMap();
        final AgentMonitor monitor = new AgentMonitor();
        MeasurementCommandsClient client = null;
        final SRNManagerLocal srnMan = getSRNManager();
        final StringBuilder debugBuf = new StringBuilder();
        try {
            final ConcurrentStatsCollector stats = ConcurrentStatsCollector.getInstance();
            client = MeasurementCommandsClientFactory.getInstance().getClient(agent);
            ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
            for (Iterator it=eids.iterator(); it.hasNext(); ) {
                final long begin = now();
                AppdefEntityID eid = (AppdefEntityID)it.next();
                int srnNumber = srnMan.incrementSrn(eid, Long.MAX_VALUE);
                SRN srn = new SRN(eid, srnNumber);
                Resource r = rMan.findResource(eid);
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                List measurements = (List) measMap.get(r.getId());
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
                    monitor.schedule(client, srn, array);
                    stats.addStat((now()-begin), ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
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

    private void unschedule(Agent a, Collection entIds)
        throws MeasurementUnscheduleException, MonitorAgentException {
        
        if (log.isDebugEnabled()) {
            log.debug("unschedule agentId=" + a.getId()
                            + ", numOfResources=" + entIds.size());
        }
        
        SRNManagerLocal srnManager = getSRNManager();
        for (Iterator it=entIds.iterator(); it.hasNext(); ) {
            try {
                AppdefEntityID entId = (AppdefEntityID) it.next();
                srnManager.removeSrn(entId);
            } catch (ObjectNotFoundException e) {
                // Ok to ignore, this is the first time scheduling metrics
                // for this resource.
            }
        }

        AgentMonitor monitor = new AgentMonitor();
        List tmp = new ArrayList(entIds);
        monitor.unschedule(a, (AppdefEntityID[])tmp.toArray(new AppdefEntityID[0]));
    }
    
    /** Unschedule metrics of multiple appdef entities
     * @ejb:interface-method
     * @param agentToken the entity whose agent will be contacted for the
     * unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    public void unschedule(String agentToken, Collection entIds)
        throws MeasurementUnscheduleException {
        try {
            // Get the agent from agent token
            Agent a = getAgent(agentToken);
            unschedule(a, entIds);
        } catch (MonitorAgentException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        }
    }
    
    /** Unschedule metrics of multiple appdef entities
     * @ejb:interface-method
     * @param agentEnt the entity whose agent will be contacted for the unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    public void unschedule(AppdefEntityID agentEnt, AppdefEntityID[] entIds)
        throws MeasurementUnscheduleException {
        try {
            // Get the agent IP and Port from server ID
            Agent a = getAgent(agentEnt);
            unschedule(a, Arrays.asList(entIds));
        } catch (MonitorAgentException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        }
    }

    /** Unschedule measurements
     * @param aeids List of {@link AppdefEntityID}
     * @throws MeasurementUnscheduleException if an error occurs
     * @ejb:interface-method
     */
    public void unschedule(Collection aeids)
        throws MeasurementUnscheduleException {
        Map agents;
        agents = getAgentMap(aeids);
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        for (Iterator it=agents.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Agent agent = aMan.findAgent((Integer)entry.getKey());
            Collection eids = (Collection)entry.getValue();
            unschedule(agent.getAgentToken(), eids);
        }
    }

    public static MeasurementProcessorLocal getOne() {
        try {
            return MeasurementProcessorUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}

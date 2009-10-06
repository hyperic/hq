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
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
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
     * @param aeids {@link List} of {@link AppdefEntityID}
     * @ejb:interface-method
     */
    public void scheduleSynchronous(List aeids) {
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        try {
            Map agents = getAgentMap(aeids);
            for (Iterator it=agents.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Agent agent = aMan.findAgent((Integer)entry.getKey());
                List entityIds = (List)entry.getValue();
                if (debug) watch.markTimeBegin("scheduleEnabled");
                scheduleEnabled(agent, entityIds);
                if (debug) watch.markTimeEnd("scheduleEnabled");
            }
        } catch(Exception e) {
            log.error("Exception scheduling [" + aeids + "]: " + e.getMessage(), e);
        }
        if (debug) {
            log.debug(watch);
        }
    }
    
    /**
     * @return Map of {@link Agent} to {@link List<AppdefEntityID>}
     */
    private Map getAgentMap(List aeids) {
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        Map rtn = new HashMap(aeids.size());
        List tmp;
        for (Iterator it=aeids.iterator(); it.hasNext(); ) {
            AppdefEntityID eid = (AppdefEntityID)it.next();
            Integer agentId;
            try {
                agentId = aMan.getAgent(eid).getId();
                if (null == (tmp = (List)rtn.get(agentId))) {
                    tmp = new ArrayList();
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
     * @ejb:interface-method
     */
    public void scheduleEnabled(Agent agent, List eids)
        throws MonitorAgentException
    {
        final SRNManagerLocal srnMan = getSRNManager();
        final MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        final AuthzSubject overlord =
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        final Map schedMap = new HashMap();
        AgentMonitor monitor = new AgentMonitor();
        MeasurementCommandsClient client = null;
        try {
            final ConcurrentStatsCollector stats =
                ConcurrentStatsCollector.getInstance();
            client = MeasurementCommandsClientFactory.getInstance()
                                                     .getClient(agent);
            for (Iterator it=eids.iterator(); it.hasNext(); ) {
                final long begin = now();
                AppdefEntityID eid = (AppdefEntityID)it.next();
                List measurements =
                    mMan.findEnabledMeasurements(overlord, eid, null);
                int srnNumber = srnMan.incrementSrn(eid, Long.MAX_VALUE);
                SRN srn = new SRN(eid, srnNumber);
                schedMap.put(srn, measurements);
                try {
                    Measurement[] meas =
                        (Measurement[])measurements.toArray(new Measurement[0]);
                    monitor.schedule(client, srn, meas);
                    stats.addStat((now()-begin),
                        ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
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

    private void unschedule(Agent a, AppdefEntityID[] entIds)
        throws MeasurementUnscheduleException, MonitorAgentException {
        SRNManagerLocal srnManager = getSRNManager();
        for (int i = 0; i < entIds.length; i++) {
            try {
                srnManager.removeSrn(entIds[i]);
            } catch (ObjectNotFoundException e) {
                // Ok to ignore, this is the first time scheduling metrics
                // for this resource.
            }
        }

        AgentMonitor monitor = new AgentMonitor();
        monitor.unschedule(a, entIds);
    }
    
    /** Unschedule metrics of multiple appdef entities
     * @ejb:interface-method
     * @param agentToken the entity whose agent will be contacted for the
     * unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     * @throws MeasurementUnscheduleException if an error occurs
     */
    public void unschedule(String agentToken, AppdefEntityID[] entIds)
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
            unschedule(a, entIds);
        } catch (MonitorAgentException e) {
            log.warn("Error unscheduling metrics: " + e.getMessage());
        }
    }

    /** Unschedule measurements
     * @param aeids List of {@link AppdefEntityID}
     * @throws MeasurementUnscheduleException if an error occurs
     * @ejb:interface-method
     */
    public void unschedule(List aeids)
        throws MeasurementUnscheduleException {
        Map agents;
        agents = getAgentMap(aeids);
        AgentManagerLocal aMan = AgentManagerEJBImpl.getOne();
        for (Iterator it=agents.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Agent agent = aMan.findAgent((Integer)entry.getKey());
            List eids = (List)entry.getValue();
            unschedule(agent.getAgentToken(),
                       (AppdefEntityID[])eids.toArray(new AppdefEntityID[0]));
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

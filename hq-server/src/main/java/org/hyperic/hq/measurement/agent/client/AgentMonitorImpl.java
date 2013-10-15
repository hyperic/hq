/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.measurement.agent.client;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.collection.ExpireMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * The AgentMonitor is a wrapper around the MeasurementClient, providing
 * commonly used routines.
 */
@Component
public class AgentMonitorImpl implements AgentMonitor
{
    private final Log log = 
        LogFactory.getLog(AgentMonitorImpl.class.getName());

    private static final String ERR_REMOTE = "Error reported by Agent @ ";

    private ExpireMap badAgents = new ExpireMap();
    private static final long BAD_AGENT_EXPIRE = 60000;
    private MeasurementCommandsClientFactory measurementCommandsClientFactory;
    private AgentCommandsClientFactory agentCommandsClientFactory;
    private AgentManager agentManager;
    private SessionFactory sessionFactory;

    
    @Autowired
    public AgentMonitorImpl(MeasurementCommandsClientFactory measurementCommandsClientFactory,
            AgentCommandsClientFactory agentCommandsClientFactory,
            AgentManager agentManager,
            SessionFactory sessionFactory
            ) {
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
        this.agentCommandsClientFactory = agentCommandsClientFactory;
        this.agentManager = agentManager;
        this.sessionFactory = sessionFactory;
    } 

    /**
     * Ping the agent to see if it is alive
     * @param agent the agent connection info
     * @return true if the agent is up, false otherwise
     */
    public boolean ping(Agent agent) {
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);

        try {
            client.ping(); 
        } catch (AgentRemoteException e) {
            log.error("Agent exception: " + e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Stacktrace: ", e);
            }
            return false;
        } catch (AgentConnectionException e) {
            log.error("Agent connection exception: " +  e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Stacktrace: ", e);
            }
            return false;
        }
        return true;
    }
    
    /** 
     * Schedule the measurement to be retrieved at specified intervals.
     *
     * @param srn      The entity associated with the schedule
     * @param schedule Information about the schedule of metrics to collect
     */
    public void schedule(MeasurementCommandsClient client, SRN srn,
                         Measurement[] schedule)
        throws AgentRemoteException, AgentConnectionException
    {
        ScheduleMeasurements_args args = new ScheduleMeasurements_args();
        args.setSRN(srn);
        for(int i=0; i<schedule.length; i++){
            Measurement m = schedule[i];
            String category = m.getTemplate().getCategory().getName();
            args.addMeasurement(m.getDsn(), m.getInterval(),
                                m.getId().intValue(), m.getId().intValue(),
                                category);
        }
        client.scheduleMeasurements(args);
    }

    /** 
     * Unschedule measurements
     *
     * @param ids Array of entities to unschedule
     */
    public void unschedule(Agent agent, AppdefEntityID[] ids)
        throws MonitorAgentException 
    {
        // If the agent's bad in the last 60 seconds, let's not worry about it
        if (badAgents.containsKey(agent.getAddress()))
            return;

        try {
            MeasurementCommandsClient client = 
                measurementCommandsClientFactory.getClient(agent);

            UnscheduleMeasurements_args args = 
                new UnscheduleMeasurements_args();
            
            for (int i = 0; i < ids.length; i++) {
                args.addEntity(ids[i]);
            }

            client.unscheduleMeasurements(args);
        } catch (AgentConnectionException e) {
            this.log.warn(ERR_REMOTE + agent.connectionString() + 
                          ": " + e.getMessage());
            
            // Track bad agent
            badAgents.put(agent.getAddress(), agent, BAD_AGENT_EXPIRE);
            
            throw new MonitorAgentException(e.getMessage(), e);
        } catch (AgentRemoteException e) {
            String emsg = ERR_REMOTE + agent.connectionString() +
            		      ": " + e.getMessage();
            this.log.warn(emsg);
            
            // Track bad agent
            badAgents.put(agent.getAddress(), agent, BAD_AGENT_EXPIRE);
            
            throw new MonitorAgentException(emsg, e);
        }
    }

    /** Get the live value
     * @param agentID the ID of the agent to talk to
     * @param dsns the DSNs that identifies the values to fetch
     */
    @Transactional(readOnly = true)
    public MetricValue[] getLiveValues(int agentId, String[] dsns)
            throws MonitorAgentException, LiveMeasurementException 
    {
        Agent agent = agentManager.getAgent(agentId);
        Session hSession = sessionFactory.getCurrentSession();
        hSession.update(agent);
        return getLiveValues(agent, dsns);
    }

    /** Get the live value
     * @param agent the agent to talk to
     * @param dsns the DSNs that identifies the values to fetch
     */
    public MetricValue[] getLiveValues(Agent agent, String[] dsns)
        throws MonitorAgentException, LiveMeasurementException 
    {        
        try {
            if (log.isDebugEnabled()) {
                log.debug("Getting live values for agent: " + agent + ", dsns: " + dsns);
            }

            GetMeasurements_result result;
            GetMeasurements_args args;
            MetricValue[] res;

            args = new GetMeasurements_args();
            for(int i=0; i<dsns.length; i++){
                args.addMeasurement(dsns[i]);
            }
            
            MeasurementCommandsClient client = 
                measurementCommandsClientFactory.getClient(agent);

            result = client.getMeasurements(args);

            if (log.isDebugEnabled()) {
                log.debug("Got live values result for agent: " + agent);
            }

            res    = new MetricValue[dsns.length];

            for(int i=0; i<dsns.length; i++){
                res[i] = result.getMeasurement(i);
                if(res[i] == null){
                    throw new LiveMeasurementException(result.getException(i));
                }
            }
            return res;
        } catch (AgentConnectionException e) {
            final String emsg = ERR_REMOTE + agent.connectionString() + 
                                ": " + e.getMessage();
                        
            this.log.warn(emsg);
            throw new MonitorAgentException(e.getMessage(), e);
        } catch (AgentRemoteException e) {
            final String emsg = ERR_REMOTE + agent.connectionString() + 
                                ": " + e.getMessage();
            
            this.log.warn(emsg);
            throw new MonitorAgentException(emsg, e);
        }
    }
}

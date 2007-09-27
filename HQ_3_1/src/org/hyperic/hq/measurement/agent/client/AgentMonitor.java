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

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.ext.MonitorInterface;
import org.hyperic.hq.measurement.ext.ScheduleMetricInfo;
import org.hyperic.hq.measurement.ext.UnscheduleMetricInfo;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.collection.ExpireMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * The MoniteringInterface implementation that communicates with
 * the AgentDaemon.
 */

public class AgentMonitor implements MonitorInterface
{
    private final Log log = 
        LogFactory.getLog(AgentMonitor.class.getName());

    private static final String ERR_REMOTE = "Error reported by Agent @ ";

    private static ExpireMap badAgents = new ExpireMap();
    private static final long BAD_AGENT_EXPIRE = 60000;

    public AgentMonitor() {}

    /**
     * Ping the agent to see if it is alive
     * @param agent the agent connection info
     * @return true if the agent is up, false otherwise
     */
    public boolean ping(AgentValue agent) {
        SecureAgentConnection conn;

        conn = new SecureAgentConnection(agent);

        AgentCommandsClient client =
                new AgentCommandsClient(conn);
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
    public void schedule(AgentValue agent, SRN srn,
                         ScheduleMetricInfo[] schedule)
        throws MonitorAgentException
    {
        SecureAgentConnection conn;

        conn = new SecureAgentConnection(agent);

        try {
            ScheduleMeasurements_args args = new ScheduleMeasurements_args();
            MeasurementCommandsClient client =
                new MeasurementCommandsClient(conn);
            
            args.setSRN(srn);

            for(int i=0; i<schedule.length; i++){
                DerivedMeasurement dMetric = schedule[i].getDerivedMetric();
                RawMeasurement[] rMetrics = schedule[i].getRawMetrics();

                for(int j=0; j<rMetrics.length; j++) {
                    String category = 
                        rMetrics[j].getTemplate().getCategory().getName();
                    args.addMeasurement(rMetrics[j].getDsn(),
                                        dMetric.getInterval(),
                                        dMetric.getId().intValue(),
                                        rMetrics[j].getId().intValue(),
                                        category);
                }
            }

            client.scheduleMeasurements(args);
        } catch (AgentConnectionException e) {
            final String emsg = ERR_REMOTE + conn + ": " + e.getMessage();

            this.log.warn(emsg);
            throw new MonitorAgentException(e.getMessage(), e);
        } catch (AgentRemoteException e) {
            final String emsg = ERR_REMOTE + conn + ": " + e.getMessage();

            this.log.warn(emsg);
            throw new MonitorAgentException(emsg, e);
        }
    }

    /** 
     * Unschedule measurements
     *
     * @param schedule array of items to unschedule
     */

    public void unschedule(AgentValue agent, UnscheduleMetricInfo[] schedule)
        throws MonitorAgentException 
    {
        SecureAgentConnection conn;

        // If the agent's bad in the last 60 seconds, let's not worry about it
        if (badAgents.containsKey(agent.getAddress()))
            return;
        
        conn = new SecureAgentConnection(agent);

        try {
            MeasurementCommandsClient client =
                new MeasurementCommandsClient(conn);

            UnscheduleMeasurements_args args = 
                new UnscheduleMeasurements_args();
            
            for (int i = 0; i < schedule.length; i++) {
                args.addEntity(schedule[i].getEntity());
            }

            client.unscheduleMeasurements(args);
        } catch (AgentConnectionException e) {
            this.log.warn(ERR_REMOTE + conn + ": " + e.getMessage());
            
            // Track bad agent
            badAgents.put(agent.getAddress(), agent, BAD_AGENT_EXPIRE);
            
            throw new MonitorAgentException(e.getMessage(), e);
        } catch (AgentRemoteException e) {
            String emsg = ERR_REMOTE + conn + ": " + e.getMessage();
            this.log.warn(emsg);
            
            // Track bad agent
            badAgents.put(agent.getAddress(), agent, BAD_AGENT_EXPIRE);
            
            throw new MonitorAgentException(emsg, e);
        }
    }

    /** Get the live value
     * @param agent the agent to talk to
     * @param dsns the DSNs that identifies the values to fetch
     */
    public MetricValue[] getLiveValues(AgentValue agent, String[] dsns)
        throws MonitorAgentException, LiveMeasurementException 
    {
        SecureAgentConnection conn;

        conn   = new SecureAgentConnection(agent);

        try {
            MeasurementCommandsClient client;
            GetMeasurements_result result;
            GetMeasurements_args args;
            MetricValue[] res;

            args = new GetMeasurements_args();
            for(int i=0; i<dsns.length; i++){
                args.addMeasurement(dsns[i]);
            }

            client = new MeasurementCommandsClient(conn);
            result = client.getMeasurements(args);
            res    = new MetricValue[dsns.length];

            for(int i=0; i<dsns.length; i++){
                res[i] = result.getMeasurement(i);
                if(res[i] == null){
                    throw new LiveMeasurementException(result.getException(i));
                }
            }
            return res;
        } catch (AgentConnectionException e) {
            final String emsg = ERR_REMOTE + conn + ": " + e.getMessage();
                        
            this.log.warn(emsg);
            throw new MonitorAgentException(e.getMessage(), e);
        } catch (AgentRemoteException e) {
            final String emsg = ERR_REMOTE + conn + ": " + e.getMessage();
            
            this.log.warn(emsg);
            throw new MonitorAgentException(emsg, e);
        }
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MetricValue;

public interface AgentMonitor {

    /**
     * Ping the agent to see if it is alive
     * @param agent the agent connection info
     * @return true if the agent is up, false otherwise
     */
    boolean ping(Agent agent);

    /** 
     * Schedule the measurement to be retrieved at specified intervals.
     *
     * @param srn      The entity associated with the schedule
     * @param schedule Information about the schedule of metrics to collect
     */
    void schedule(MeasurementCommandsClient client, SRN srn, Measurement[] schedule)
            throws AgentRemoteException, AgentConnectionException;

    /** 
     * Unschedule measurements
     *
     * @param ids Array of entities to unschedule
     */
    void unschedule(Agent agent, AppdefEntityID[] ids) throws MonitorAgentException;

    /** Get the live value
     * @param agent the agent to talk to
     * @param dsns the DSNs that identifies the values to fetch
     */
    MetricValue[] getLiveValues(Agent agent, String[] dsns) throws MonitorAgentException,
            LiveMeasurementException;

    MetricValue[] getLiveValues(int agentId, String[] dsns) throws MonitorAgentException,
    LiveMeasurementException;

}
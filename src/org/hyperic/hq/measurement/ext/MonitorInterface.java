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

package org.hyperic.hq.measurement.ext;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.product.MetricValue;

import java.util.List;

/** 
 * The interface class to be implemented for any classes that
 * need to communicate with a measurement agent.
 */
public interface MonitorInterface {

    /**
     * Ping the agent to see if it is alive
     *
     * @param agent the agent connection info
     * @return true if the agent is up, false otherwise
     */
    public boolean ping(AgentValue agent);

    /** 
     * Schedule the measurement to be retrieved at specified intervals
     *
     * @param agent    Agent connection information
     * @param srn      The SRN for the updated schedule
     * @param schedule Metrics to schedule
     * @throws MonitorAgentException if an error occurs communicating with 
     *         the agent
     */
    public void schedule(AgentValue agent, SRN srn, Measurement[] schedule)
        throws MonitorAgentException;

    /** 
     * Unschedule measurements
     *
     * @param agent    Agent to contact to unschedule
     * @param ids      Array of entities to unschedule
     */
    public void unschedule(AgentValue agent, AppdefEntityID[] ids)
        throws MonitorAgentException;

    /** 
     * Get a series of live values
     *
     * @param agent Agent to use to connect to for the live value
     * @param dsns the DSNs that identify the values to fetch
     * @return an array of MetricValue objects matching the dsns
     */    
    public MetricValue[] getLiveValues(AgentValue agent, String[] dsns)
        throws MonitorAgentException, LiveMeasurementException;
}

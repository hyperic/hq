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

/*
 * ScheduleArgs.java
 * 
 * Created on Jun 13, 2003
 */
package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.ext.depgraph.Graph;

/**
 * This object contains the arguments for scheduling a measurement with the
 * agent
 */
public class ScheduleArgs implements Serializable {

    private AppdefEntityID  entId;
    private Graph[]         graphs;
    private Set             agentSchedule;
    private Set             serverSchedule;
    
    /**
     * @param entId the Entity ID
     * @param graphs the graphs of nodes
     * @param agentSchedule the agent schedule
     * @param serverSchedule the server schedule
     */
    public ScheduleArgs(AppdefEntityID entId, Graph[] graphs,
                        Set agentSchedule, Set serverSchedule) {
        this.entId          = entId;
        this.graphs         = graphs;
        this.agentSchedule  = agentSchedule;
        this.serverSchedule = serverSchedule;
    }
    
    /**
     * @return the agent schedule
     */
    public Set getAgentSchedule() {
        return agentSchedule;
    }

    /**
     * @return the entity ID
     */
    public AppdefEntityID getEntId() {
        return entId;
    }

    /**
     * @return the graphs of the dependent nodes
     */
    public Graph[] getGraphs() {
        return graphs;
    }

    /**
     * @return the server schedule
     */
    public Set getServerSchedule() {
        return serverSchedule;
    }
}

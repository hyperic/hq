/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.measurement.shared;

import java.util.Collection;
import java.util.List;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;

/**
 * Local interface for MeasurementProcessor.
 */
public interface MeasurementProcessor {
    /**
     * Ping the agent to make sure it's up
     */
    public boolean ping(Agent a) throws PermissionException;
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * 
     */
    public void scheduleHierarchyAfterCommit(Collection<Resource> resources);
    
    /**
     * Schedules enabled measurements for the entire ResourceEdge hierarchy
     * based on the "containment" relationship.  These metrics are scheduled
     * after the transaction is committed.
     * 
     */
    public void scheduleHierarchyAfterCommit(Resource resource);

    /**
     * Schedules all the {@link AppdefEntityID}s synchronously
     */
    public void scheduleSynchronous(Collection<AppdefEntityID> aeids);

    /**
     * Schedules all the enabled {@link Measurement}s associated with the {@link AppdefEntityID}s and 
     * {@link Agent} synchronously
     */
    public void scheduleEnabled(Agent agent, Collection<AppdefEntityID> eids) throws MonitorAgentException;

    /**
     * Unschedule metrics of multiple appdef entities
     * @param agentToken the entity whose agent will be contacted for the
     *        unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     */
    public void unschedule(String agentToken, Collection<AppdefEntityID> entIds);

    /**
     * Unschedule metrics of multiple appdef entities
     * @param agentEnt the entity whose agent will be contacted for the
     *        unschedule
     * @param entIds the entity IDs whose metrics should be unscheduled
     */
    public void unschedule(AppdefEntityID agentEnt, AppdefEntityID[] entIds);

    /**
     * Unschedule measurements
     * @param aeids List of {@link AppdefEntityID}
     */
    public void unschedule(Collection<AppdefEntityID> aeids);

}

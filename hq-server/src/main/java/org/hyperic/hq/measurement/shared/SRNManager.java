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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.AgentScheduleSynchronizer;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;

/**
 * Local interface for SRNManager.
 */
public interface SRNManager {
    /**
     * Initialize the SRN Cache, or just return if it's already been
     * initialized.
     */
    public void initializeCache();

    /**
     * Get a SRN
     * @param aid The entity id to lookup
     * @return The SRN for the given entity
     */
    public ScheduleRevNum get(AppdefEntityID aid);

    /**
     * Remove a SRN.
     * @param aid The AppdefEntityID to remove.
     */
    public void removeSrn(AppdefEntityID aid);

    /**
     * Increment SRN for the given entity.
     * @param aid The AppdefEntityID to remove.
     * @param newMin The new minimum interval
     * @return The ScheduleRevNum for the given entity id
     */
    public int incrementSrn(AppdefEntityID aid, long newMin);

    /**
     * Handle a SRN report from an agent.
     * @param srns The list of SRNs from the agent report.
     * @return A Collection of ScheduleRevNum objects that do not have a
     *         corresponding appdef entity. (i.e. Out of sync)
     */
    public Collection<AppdefEntityID> reportAgentSRNs(SRN[] srns);

    /**
     * Get a List of out-of-sync entities.
     * @return A list of ScheduleReNum objects that are out of sync.
     */
    public List<AppdefEntityID> getOutOfSyncEntities();

    /**
     * Get the list of out-of-sync SRNs based on the number of intervals back to
     * allow.
     * @param intervals The number of intervals to go back
     * @return A List of ScheduleRevNum objects.
     */
    public List<ScheduleRevNum> getOutOfSyncSRNs(int intervals);

    /**
     * Refresh the SRN for the given entity.
     * @param eid The appdef entity to refresh
     * @return The new ScheduleRevNum object.
     */
    public ScheduleRevNum refreshSRN(AppdefEntityID eid);

    /**
     * Reschedule metrics for an appdef entity. Generally should only be called
     * from the {@link AgentScheduleSynchronizer}
     * @param List of {@link AppdefEntityId}
     */
    public void reschedule(List<AppdefEntityID> aeids) throws MeasurementScheduleException, MonitorAgentException,
        MeasurementUnscheduleException;

}

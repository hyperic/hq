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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.server.session.AgentScheduleSynchronizer;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.server.session.SrnId;
import org.hyperic.hq.zevents.ZeventManager;

/**
 * Local interface for SRNManager.
 */
public interface SRNManager {

    /**
     * Get a SRN
     * @param aid The entity id to lookup
     * @return The SRN for the given entity
     */
    public ScheduleRevNum get(AppdefEntityID aeid);

    public ScheduleRevNum getSrnById(SrnId id);

    /**
     * Remove a SRN.
     * @param aid The AppdefEntityID to remove.
     */
    public void removeSrn(AppdefEntityID aeid);

    /**
     * Increment SRN for the given entity.
     * @param aeid The AppdefEntityID to remove.
     * @return The value of the SRN for the given entity id
     */
    public int incrementSrn(AppdefEntityID aeid);

    /**
     * Reschedule metrics for an appdef entity in the foreground. Generally should only be called
     * from the {@link AgentScheduleSynchronizer}
     * @param aeids {@link Collection} of {@link AppdefEntityId} to schedule
     * @param incrementSrns increment associated Srns
     * @param overrideRestrictions {@link Boolean} - false means adhere to the 1 hour restriction if an srn is updated
     * it cannot be updated again for this time period and therefore an agent should not be scheduled either.
     */
    public void schedule(Collection<AppdefEntityID> aeids, boolean incrementSrns, boolean overrideRestrictions);

    /**
     * Reschedule the aeids passed into the method.  If they do not have any associated measurements the agent will be
     * completely unscheduled for the associated resource.
     * @param aeids {@link Collection} of {@link AppdefEntityID}s to schedule in a background queue.  If
     * the associated aeids don't have any meaurements they will be unscheduled
     * @param incrementSrns increment associated Srns
     * @param overrideRestrictions {@link Boolean} - false means adhere to the 1 hour restriction if an srn is updated
     * it cannot be updated again for this time period and therefore an agent should not be scheduled either.
     */
    public void scheduleInBackground(Collection<AppdefEntityID> aeids, boolean incrementSrns,
                                     boolean overrideRestrictions);

    /**
     * Reschedule enabled measurements associated with the aeids.  Will not increment the associated SRNs
     * @param aeids {@link Collection} of {@link AppdefEntityID}s to schedule in a background queue.  If
     * the associated aeids don't have any meaurements they will be unscheduled
     * @param overrideRestrictions {@link Boolean} - false means adhere to the 1 hour restriction if an srn is updated
     * it cannot be updated again for this time period and therefore an agent should not be scheduled either.
     */
    public void scheduleInBackground(Collection<AppdefEntityID> aeids, boolean overrideRestrictions);
    
    /** Only used for integration tests! */
    public void setZeventManager(ZeventManager zeventManagerMock);

    /** Only used for integration tests! */
    public void setMeasurementProcessor(MeasurementProcessor measurementProcessorMock);

    /**
     * Reschedules if the SRN match the ScheduleRevNum associated with the AppdefEntityID in the db
     * @param {@link Collection} The associated {@link SRN}s will be rescheduled if they are out of sync or missing
     * @param overrideRestrictions {@link Boolean} - false means adhere to the 1 hour restriction if an srn is updated
     * it cannot be updated again for this time period and therefore an agent should not be scheduled either.
     */
    public void rescheduleOutOfSyncSrns(Collection<SRN> srns, boolean overrideRestrictions);

}

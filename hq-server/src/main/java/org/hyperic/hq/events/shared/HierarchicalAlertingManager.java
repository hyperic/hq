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

package org.hyperic.hq.events.shared;

import java.util.Map;

import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.ResourceDataPoint;

public interface HierarchicalAlertingManager {

    /**
     * Determine whether the measurement events can be suppressed as part of
     * hierarchical alerting
     * 
     * @param events Map<Integer, MeasurementEvent> Integer => Resource.getId()
     * @param isFromAgent --> indicates whether the events came from the agent
     *        or backfiller
     */
    public void suppressMeasurementEvents(Map<Integer, MeasurementEvent> events, boolean isFromAgent);

    /**
     * Perform a simple "secondary" availability check for down platforms.
     */
    public void performSecondaryAvailabilityCheck(Map<Integer, ResourceDataPoint> downPlatforms);

}

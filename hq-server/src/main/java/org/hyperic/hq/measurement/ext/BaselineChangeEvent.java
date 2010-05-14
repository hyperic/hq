/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004, 2005, 2006],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.measurement.ext;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;

/**
 * delivers baseline change events to any registered consumer.
 *
 *
 */
public class BaselineChangeEvent
    extends AbstractEvent implements Serializable, ResourceEventInterface
{

    private static final long serialVersionUID = 1620737031667245999L;
    private Double mean;
    private Double minExpectedVal;
    private Double maxExpectedVal;

    /**
     * Creates a new instance of MeasurementEvent
     * @param measurementId The measurement ID
     * @param mean The mean baseline value after the change
     * @param minExpectedVal The min expected value after the change
     * @param maxExpectedVal The max expected value after the change
     */
    public BaselineChangeEvent(Integer measurementId, Double mean, Double minExpectedVal, Double maxExpectedVal) {
        setInstanceId(measurementId);
        this.mean = mean;
        this.minExpectedVal = minExpectedVal;
        this.maxExpectedVal = maxExpectedVal;
    }

    /**
     *
     * @return The expected max after the change
     */
    public Double getMaxExpectedVal() {
        return maxExpectedVal;
    }

    /**
     *
     * @return The baseline mean after the change
     */
    public Double getMean() {
        return mean;
    }

    /**
     *
     * @return The expected min after the change
     */
    public Double getMinExpectedVal() {
        return minExpectedVal;
    }

    /**
     * @see org.hyperic.hq.events.ResourceEventInterface#getResource()
     */
    public AppdefEntityID getResource() {
        return null;
    }
}

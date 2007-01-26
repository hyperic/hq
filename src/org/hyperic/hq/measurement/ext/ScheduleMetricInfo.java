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

import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.RawMeasurement;

/**
 * This class simply wraps all the data needed to schedule measurements
 * on a MonitorInterface
 */

public class ScheduleMetricInfo {
    private DerivedMeasurement _derivedMetric;
    private RawMeasurement[] _rawMetrics;

    /**
     * Construct a new ScheduleMetricInfo object, encapsulating a bunch of
     * info needed for MonitorInterfaces to schedule the collection.
     *
     * @param derived    The derived measurement used for reference for 
     *                   interval and such
     * @param rawMetrics The raw metrics which will be collected, and are
     *                   constituents of 'derived'
     */
    public ScheduleMetricInfo(DerivedMeasurement derived,
                              RawMeasurement[] rawMetrics)
    {
        _derivedMetric = derived;
        _rawMetrics = rawMetrics;
    }

    public DerivedMeasurement getDerivedMetric(){
        return _derivedMetric;
    }

    public RawMeasurement[] getRawMetrics(){
        return _rawMetrics;
    }
}

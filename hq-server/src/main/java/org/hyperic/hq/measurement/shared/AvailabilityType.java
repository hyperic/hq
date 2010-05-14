/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.measurement.shared;

import java.util.ResourceBundle;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.HypericEnum;

public class AvailabilityType extends HypericEnum {
    
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.measurement.Resources");
    
    public static final AvailabilityType AVAIL_DOWN =
        new AvailabilityType(0, "down",
            "measurement.availability.availDown");
    public static final AvailabilityType AVAIL_UP =
        new AvailabilityType(1, "up",
            "measurement.availability.availUp");
    public static final AvailabilityType AVAIL_PAUSED =
        new AvailabilityType(2, "paused",
            "measurement.availability.availPaused");
    public static final AvailabilityType AVAIL_NULL =
        new AvailabilityType(3, "null",
            "measurement.availability.availNull");
    public static final AvailabilityType AVAIL_WARN =
        new AvailabilityType(4, "warn",
            "measurement.availability.availWarn");
    public static final AvailabilityType AVAIL_UNKNOWN =
        new AvailabilityType(5, "unknown",
            "measurement.availability.availUnknown");

    public AvailabilityType(int code, String desc, String localeProp) {
        super(AvailabilityType.class, code, desc, localeProp, BUNDLE);
    }
    
    public double getAvailabilityValue() {
        if (this.equals(AVAIL_UP)) {
            return MeasurementConstants.AVAIL_UP;
        } else if (this.equals(AVAIL_DOWN)) {
            return MeasurementConstants.AVAIL_DOWN;
        } else if (this.equals(AVAIL_WARN)) {
            return MeasurementConstants.AVAIL_WARN;
        } else if (this.equals(AVAIL_NULL)) {
            return MeasurementConstants.AVAIL_NULL;
        } else if (this.equals(AVAIL_PAUSED)) {
            return MeasurementConstants.AVAIL_PAUSED;
        } else {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
    }
    
    public static AvailabilityType findByAvailVal(double availVal) {
        if (availVal == MeasurementConstants.AVAIL_UP) {
            return AVAIL_UP;
        } else if (availVal == MeasurementConstants.AVAIL_DOWN) {
            return AVAIL_DOWN;
        } else if (availVal == MeasurementConstants.AVAIL_WARN) {
            return AVAIL_WARN;
        } else if (availVal == MeasurementConstants.AVAIL_NULL) {
            return AVAIL_NULL;
        } else if (availVal == MeasurementConstants.AVAIL_PAUSED) {
            return AVAIL_PAUSED;
        } else {
            return AVAIL_UNKNOWN;
        }
    }

    public static AvailabilityType findByCode(int code) {
        return (AvailabilityType)findByCode(AvailabilityType.class, code);  
    }
}

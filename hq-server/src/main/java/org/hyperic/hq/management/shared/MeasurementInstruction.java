/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2012], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.management.shared;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

@SuppressWarnings("serial")
public class MeasurementInstruction extends PersistedObject {
    
    private long interval;
    private boolean defaultOn;
    private MeasurementTemplate measurementTemplate;
    private ManagementPolicy managementPolicy;

    public long getInterval() {
        return interval;
    }
    public void setInterval(long interval) {
        this.interval = interval;
    }
    public boolean isDefaultOn() {
        return defaultOn;
    }
    public void setDefaultOn(boolean defaultOn) {
        this.defaultOn = defaultOn;
    }
    public MeasurementTemplate getMeasurementTemplate() {
        return measurementTemplate;
    }
    public void setMeasurementTemplate(MeasurementTemplate measurementTemplate) {
        this.measurementTemplate = measurementTemplate;
    }
    public ManagementPolicy getManagementPolicy() {
        return managementPolicy;
    }
    public void setManagementPolicy(ManagementPolicy managementPolicy) {
        this.managementPolicy = managementPolicy;
    }
    
    public int hashCode() {
        return super.hashCode();
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MeasurementInstruction) {
            return super.equals(o);
        }
        return false;
    }

}

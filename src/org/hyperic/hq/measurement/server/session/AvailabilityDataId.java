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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

public class AvailabilityDataId implements Serializable {
    private static final long serialVersionUID = 1L;
    private long _startime;
    private Measurement _measurement;
    
    public AvailabilityDataId() {
    }
    
    public AvailabilityDataId(long startime, Measurement measurement) {
        _startime = startime;
        _measurement = measurement;
    }

    public long getStartime() {
        return _startime;
    }

    public Measurement getMeasurement() {
        return _measurement;
    }

    public void setMeasurement(Measurement measurement) {
        _measurement = measurement;
    }

    public void setStartime(long startime) {
        _startime = startime;
    }
    
    public int hashCode() {
        return 17 + (37*(new Long(_startime)).hashCode()) +
            (37*_measurement.getId().hashCode());
    }
    
    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        } else if (rhs instanceof AvailabilityDataId) {
            return equals((AvailabilityDataId)rhs);
        }
        return false;
    }
    
    private boolean equals(AvailabilityDataId rhs) {
        if (_startime == rhs._startime &&
                _measurement.getId().equals(rhs._measurement.getId())) {
            return true;
        }
        return false;
    }
    
    public String toString() {
        return "startime -> " + _startime +
            ", measId -> " + _measurement.getId();
    }
}

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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class AvailabilityDataId implements Serializable {
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "MEASUREMENT_ID", nullable = false)
    private Measurement measurement;

    @Column(name = "STARTTIME", nullable = false)
    private long startime;

    public AvailabilityDataId() {
    }

    public AvailabilityDataId(long startime, Measurement measurement) {
        this.startime = startime;
        this.measurement = measurement;
    }

    private boolean equals(AvailabilityDataId rhs) {
        if (startime == rhs.startime && measurement.getId().equals(rhs.measurement.getId())) {
            return true;
        }
        return false;
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        } else if (rhs instanceof AvailabilityDataId) {
            return equals((AvailabilityDataId) rhs);
        }
        return false;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public long getStartime() {
        return startime;
    }

    public int hashCode() {
        return 17 + (37 * (new Long(startime)).hashCode()) + (37 * measurement.getId().hashCode());
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public void setStartime(long startime) {
        this.startime = startime;
    }

    public String toString() {
        return "startime -> " + startime + ", measId -> " + measurement.getId();
    }
}

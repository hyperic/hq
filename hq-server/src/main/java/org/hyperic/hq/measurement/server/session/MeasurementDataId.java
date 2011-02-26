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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MeasurementDataId implements Serializable {

    @Column(name="MEASUREMENT_ID",nullable=false)
    private Integer measurementId;
    
    @Column(name="TIMESTAMP",nullable=false)
    private long timestamp;
    
    @Column(name="ADDITIONAL")
    private Integer additional;
   
    
    public MeasurementDataId() {
    }

    public MeasurementDataId(Integer measurementId, long timestamp,
                             Integer additional) {
        this.measurementId = measurementId;
        this.timestamp = timestamp;
        this.additional = additional;
    }
  
    public Integer getMeasurementId() {
        return measurementId;
    }
    
    protected void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }
    public long getTimestamp() {
        return timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    
    
    public Integer getAdditional() {
        return additional;
    }

    public void setAdditional(int additional) {
        this.additional = additional;
    }

    public boolean equals(Object other) {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof MeasurementDataId)) return false;
        MeasurementDataId castOther = (MeasurementDataId) other; 
        
        return ((getMeasurementId() == castOther.getMeasurementId()) || 
                (getMeasurementId() != null &&
                 castOther.getMeasurementId() != null &&
                 getMeasurementId().equals(castOther.getMeasurementId()))) &&
            (getTimestamp() == castOther.getTimestamp()) &&
            ((getAdditional() == castOther.getAdditional()) || 
                (getAdditional() != null &&
                 castOther.getAdditional() != null &&
                 getAdditional().equals(castOther.getAdditional())));
    }
   
    public int hashCode() {
        int result = 17;
        
        result = 37 * result + 
            (getMeasurementId() == null ? 0 : getMeasurementId().hashCode() );
        result = 37 * result + (int)getTimestamp();
        result = 37 * result +
            (getAdditional() == null ? 0 : getAdditional().hashCode() );
        return result;
    }   
}



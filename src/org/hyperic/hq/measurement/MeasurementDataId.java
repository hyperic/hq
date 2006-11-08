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

package org.hyperic.hq.measurement;

public class MeasurementDataId implements java.io.Serializable {

    // Fields    
    private Measurement _measurement;
    private long _timestamp;

    // Constructors
    public MeasurementDataId() {
    }

    public MeasurementDataId(Measurement measurement, long timestamp) {
        _measurement = measurement;
        _timestamp = timestamp;
    }
   
    // Property accessors
    public Measurement getMeasurement() {
        return _measurement;
    }
    
    public void setMeasurement(Measurement measurement) {
        _measurement = measurement;
    }
    public long getTimestamp() {
        return _timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }


    public boolean equals(Object other) {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof MeasurementDataId)) return false;
        MeasurementDataId castOther = (MeasurementDataId) other; 
        
        return ((getMeasurement() == castOther.getMeasurement()) ||
                (getMeasurement() != null &&
                 castOther.getMeasurement() != null &&
                 getMeasurement().equals(castOther.getMeasurement()))) &&
            (getTimestamp() == castOther.getTimestamp());
    }
   
    public int hashCode() {
        int result = 17;
        
        result = 37 * result + 
            (getMeasurement() == null ? 0 : getMeasurement().hashCode() );
        result = 37 * result + (int)getTimestamp();
        return result;
    }   
}



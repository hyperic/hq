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

import org.hyperic.hq.measurement.shared.RawMeasurementValue;

public class RawMeasurement extends Measurement
    implements java.io.Serializable {

    // Fields    
    private String _dsn;

    // Constructors
    public RawMeasurement() {
    }

    public RawMeasurement(Integer instanceId, MeasurementTemplate template,
                          String dsn) {
        super(instanceId, template);
        _dsn = dsn;
    }
    
    // Property accessors
    public String getDsn() {
        return _dsn;
    }
    
    public void setDsn(String dsn) {
        _dsn = dsn;
    }

    /**
     * Legacy EJB DTO pattern
     * @deprecated Use (this) RawMeasurement object instead
     */
    public RawMeasurementValue getRawMeasurementValue() {
        RawMeasurementValue value = new RawMeasurementValue();
        value.setId(getId());
        value.setDsn(getDsn());
        value.setInstanceId(getInstanceId());
        value.setMtime(getMtime());
        value.setTemplate(getTemplate().getMeasurementTemplateValue());
        return value;
    }

    public boolean equals(Object obj) {
        return (obj instanceof RawMeasurement) && super.equals(obj);
    }
}

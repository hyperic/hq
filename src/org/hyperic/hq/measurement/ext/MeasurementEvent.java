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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.RawMeasurementDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.measurement.RawMeasurement;
import org.hyperic.hq.product.MetricValue;

public class MeasurementEvent extends AbstractEvent
    implements java.io.Serializable, ResourceEventInterface {
    private static final Log log = LogFactory.getLog(MeasurementEvent.class);
    
    private AppdefEntityID resource;
    private MetricValue value;
    private String units;

    /** Creates a new instance of MeasurementEvent */
    public MeasurementEvent(Integer mid, MetricValue value) {
        super.setInstanceId(mid);
        super.setTimestamp(value.getTimestamp());
        this.resource = null;
        this.value = value;
        this.units = null;
    }
    
    public AppdefEntityID getResource(){
        lazySetValues();
        return this.resource;
    }

    public MetricValue getValue() {
        return this.value;
    }
    
    public void setValue(MetricValue value) {
        this.value = value;
    }

    public String getUnits() {
        lazySetValues();
        return this.units;
    }
    
    private void lazySetValues() {
        if (this.resource != null && this.units != null)
            return;
        
        try {
            // Use RawMeasurement, because it will work for both
            RawMeasurementDAO dao =
                DAOFactory.getDAOFactory().getRawMeasurementDAO();
            RawMeasurement rm = dao.findById(getInstanceId());
            int resourceId, resourceType;

            resourceId = rm.getInstanceId().intValue();
            resourceType = rm.getTemplate().getMonitorableType()
                .getAppdefType();
            this.resource = new AppdefEntityID(resourceType, resourceId);
            this.units = rm.getTemplate().getUnits();
        } catch (Exception e) {
            // don't set anything
            log.warn("Couldn't setup measurement event values.", e);
            this.resource = null;
            this.units = null;
        }
    }

    public String toString() {
        return this.value.toString();
    }    
}

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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.hq.product.MetricValue;

public class MeasurementEvent extends AbstractEvent
    implements Serializable, ResourceEventInterface {

    private static final long serialVersionUID = -4805198063892667418L;

    private static final Log _log = LogFactory.getLog(MeasurementEvent.class);
    
    private AppdefEntityID _resource;
    private MetricValue    _value;
    private String         _units;

    public MeasurementEvent(Integer mid, MetricValue value) {
        super.setInstanceId(mid);
        super.setTimestamp(value.getTimestamp());
        _resource = null;
        _value = value;
        _units = null;
    }
    
    public AppdefEntityID getResource(){
        lazySetValues();
        return _resource;
    }

    public MetricValue getValue() {
        return _value;
    }
    
    public void setValue(MetricValue value) {
        _value = value;
    }

    public String getUnits() {
        lazySetValues();
        return _units;
    }
    
    private void lazySetValues() {
        if (_resource != null && _units != null)
            return;
        
        try {
            MeasurementDAO dao =
                new MeasurementDAO(DAOFactory.getDAOFactory());
            Measurement dm = dao.findById(getInstanceId());
            int resourceId, resourceType;

            resourceId   = dm.getInstanceId().intValue();
            resourceType = dm.getTemplate().getMonitorableType()
                .getAppdefType();
            _resource    = new AppdefEntityID(resourceType, resourceId);
            _units       = dm.getTemplate().getUnits();
        } catch (Exception e) {
            // don't set anything
            _log.warn("Couldn't setup measurement event values.", e);
            _resource = null;
            _units    = null;
        }
    }

    public String toString() {
        return _value.toString();
    }    
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;

/**
 * This class builds measurement-related objects for unit testing.
 * It is used when the required object has constructors and setter methods
 * with default or protected access.
 */
public class MeasurementObjectBuilder {
    
    public Measurement buildMeasurement(Integer id, 
                                        AppdefEntityID adeId, 
                                        long interval, 
                                        Resource r) {
        
        MonitorableType mType = buildMonitorableType(adeId.getType());
        MeasurementTemplate mTemplate = new MeasurementTemplate(null, null, null, 0, true, 60000, 
                                                                true, null, mType, null, null);
        Measurement m = new Measurement(adeId.getId(), mTemplate, interval);
        m.setId(id);        
        m.setResource(r);
        
        return m;
    }
    
    private MonitorableType buildMonitorableType(int appdefType) {
        MonitorableType mt = new MonitorableType();
        mt.setAppdefType(appdefType);
        
        return mt;
    }
}

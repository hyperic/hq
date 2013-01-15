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

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.util.TimeUtil;

public class ResourceDataPoint extends DataPoint {
    private Resource _resource;
    
    public ResourceDataPoint(Resource resource, DataPoint point) {
        super(point.getMeasurementId().intValue(), point.getValue(), point.getTimestamp());
        _resource = resource;
    }
    
    public Resource getResource() {
        return _resource;
    }   
    
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("resId=").append(_resource.getId());
        s.append(" timestamp=").append(TimeUtil.toString(getTimestamp()));
        s.append(" value=").append(getValue());
        return s.toString();
    }

}

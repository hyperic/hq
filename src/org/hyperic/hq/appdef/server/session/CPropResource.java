/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.product.MetricValue;

import java.util.Properties;

public class CPropResource {
    private AppdefResourceValue _res;
    private Properties _cprops;
    private MetricValue _mv;
    private EventLog _el;

    public CPropResource(AppdefResourceValue res, Properties cprops) {
        super();
        _res = res;
        _cprops = cprops;
    }

    public String getCPropValue(String key) {
        return _cprops.getProperty(key);
    }

    public String getResourceName() {
        return _res.getName();
    }

    public MetricValue getLastValue() {
        return _mv;
    }

    public void setLastValue(MetricValue mv) {
        _mv = mv;
    }
    
    public EventLog getLastEvent() {
        return _el;
    }
    
    public void setLastEvent(EventLog el) {
        _el = el;
    }
    
    public AppdefEntityID getEntityId() {
        return _res.getEntityId();
    }
}

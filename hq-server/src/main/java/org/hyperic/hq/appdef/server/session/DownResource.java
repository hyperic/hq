/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.measurement.ext.DownMetricValue;

public class DownResource {
    private AppdefResource _res;
    private DownMetricValue _dmv;
    
    public DownResource(AppdefResource res, DownMetricValue dmv) {
        _res = res;
        _dmv = dmv;
    }
    
    public String getId() {
        return _res.getEntityId().getAppdefKey();
    }
    
    public String getName() {
        return _res.getName();
    }
    
    public String getType() {
        return _res.getAppdefResourceType().getName();
    }
    
    public long getTimestamp() {
        return _dmv.getTimestamp();
    }
    
    public long getDuration() {
        return _dmv.getDuration();
    }
    
    public AppdefResource getResource() {
        return _res;
    }
}

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

package org.hyperic.hq.measurement.ext;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.product.MetricValue;

public class DownMetricValue extends MetricValue implements Comparable {
    private AppdefEntityID _entityId;

    public DownMetricValue(AppdefEntityID id, MetricValue mv) {
        super(mv);
        _entityId = id;
    }

    public AppdefEntityID getEntityId() {
        return _entityId;
    }

    public void setEntityId(AppdefEntityID entityId) {
        _entityId = entityId;
    }

    public String getDuration() {
        // TODO: actually, require the locale string and format the duration
        // appropriately
        long duration = System.currentTimeMillis() - getTimestamp();
        return "" + (duration / 60000) + "min";
    }

    /**
     * DownMetricValue orders by appdef type first, then by when entity
     * registered the down metric
     */
    public int compareTo(Object o) {
        DownMetricValue dmv = (DownMetricValue) o;
        if (getEntityId().getType() != dmv.getEntityId().getType()) {
            return new Integer(getEntityId().getType())
                .compareTo(new Integer(dmv.getEntityId().getType()));
        }
        
        return new Long(getTimestamp()).compareTo(new Long(dmv.getTimestamp())); 
    }

    public boolean equals(Object o) {
        DownMetricValue dmv = (DownMetricValue) o;
        if (_entityId.equals(dmv.getEntityId()))
            return super.equals(o);
        
        return false;
    }
}

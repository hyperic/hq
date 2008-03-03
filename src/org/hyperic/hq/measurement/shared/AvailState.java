/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.measurement.shared;

import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.product.MetricValue;

public class AvailState extends DataPoint {
    private int _id;
    private int _timestamp;
    private double _val;

    public AvailState(int id, double val, int timestamp) {
        // don't like this, but since we are changing the long timestamp
        // to integer soon it will be fine for now
        super(new Integer(id), new MetricValue(val, (long)timestamp*1000));
        _id = id;
        _val = val;
        _timestamp = timestamp;
    }
    
    public int getId() {
        return _id;
    }

    public double getVal() {
        return _val;
    }

    public int getTimestamp() {
        return _timestamp;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        return buf.append("id -> ").append(_id)
            .append(" val -> ").append(_val)
            .append(" timestamp -> ").append(_timestamp).toString();
    }

}

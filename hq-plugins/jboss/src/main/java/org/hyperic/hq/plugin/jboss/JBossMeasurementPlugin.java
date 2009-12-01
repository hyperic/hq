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

package org.hyperic.hq.plugin.jboss;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.MeasurementPlugin;

public class JBossMeasurementPlugin
    extends MeasurementPlugin {

    static final String ATTR_STATE = "StateString";

    static final String ATTR_STATE_MANAGEABLE =
        "StateManageable";

    static final String ATTR_STATISTIC =
        "Statistic";

    //"StateManageable" in 3.x, "stateManageable" in 4.0
    private boolean isStateManageable(String attr) {
        return attr.equalsIgnoreCase(ATTR_STATE_MANAGEABLE);
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        Object obj;
        try {
            obj = JBossUtil.getRemoteMBeanValue(metric);
        } catch (MetricUnreachableException e) {
            // if checking for availability, not being able to contact
            // jboss means it's unavailable
            if (metric.getAttributeName().equals(ATTR_STATE) ||
                isStateManageable(metric.getAttributeName())) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            throw e;
        }
            
        double val;
        // some JBoss MBeans expose lifecycle state
        if (metric.getAttributeName().equals(ATTR_STATE)) {
            val = obj.equals("Started") ? 
                Metric.AVAIL_UP :
                Metric.AVAIL_DOWN;
        }
        // others do not, so existence implies availability
        else if (isStateManageable(metric.getAttributeName())) {
            val = obj != null ?
                    Metric.AVAIL_UP :
                    Metric.AVAIL_DOWN;
        }
        else if (obj instanceof Boolean) {
            //assume Boolean is an avail metric
            //e.g. JGroups Channel.Connected metric
            val = obj.equals(Boolean.TRUE) ?
                    Metric.AVAIL_UP :
                    Metric.AVAIL_DOWN;
        }
        else {
            val = Double.valueOf(obj.toString()).doubleValue();
        }

        return new MetricValue(val);
    }
}

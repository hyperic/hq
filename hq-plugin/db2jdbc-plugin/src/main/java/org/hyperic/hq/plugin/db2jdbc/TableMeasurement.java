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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.util.Map;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class TableMeasurement extends Measurement {

    public MetricValue getValue(Metric metric) throws MetricUnreachableException, MetricNotFoundException {
        MetricValue res;
        try {
            res = super.getValue(metric);
        } catch (MetricNotFoundException e) {
            if (metric.getObjectProperties().getProperty("func").equalsIgnoreCase("SNAP_GET_TAB_V91")) {
                res = new MetricValue(0);
            } else {
                throw e;
            }
        }
        return res;
    }

    protected void postProcessResults(Map results) {
        if (results.get("DATA_OBJECT_P_SIZE") != null) {
            results.put("TOTAL_SIZE", new MetricValue(((MetricValue) results.get("DATA_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("INDEX_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("LOB_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("LONG_OBJECT_P_SIZE")).getValue() +
                    ((MetricValue) results.get("XML_OBJECT_P_SIZE")).getValue()));
        }
    }
}

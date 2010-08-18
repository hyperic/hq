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
import org.hyperic.hq.product.MetricValue;

/**
 *
 * @author laullon
 */
public class DataBaseMeasurement extends PoolMeasurement {

    protected void postProcessResults(Map results) {
        super.postProcessResults(results);
        results.put("LOCK_LIST_IN_USE", new MetricValue(((MetricValue) results.get("LOCK_LIST_IN_USE")).getValue() * 4));

        results.put("DIRECT_READS_RATIO", new MetricValue(((MetricValue) results.get("DIRECT_READS")).getValue() / ((MetricValue) results.get("DIRECT_READ_REQS")).getValue()));
        results.put("DIRECT_READ_TIME_AVE", new MetricValue(((MetricValue) results.get("DIRECT_READ_TIME")).getValue() / ((MetricValue) results.get("DIRECT_READS")).getValue()));

        results.put("DIRECT_WRITE_RATIO", new MetricValue(((MetricValue) results.get("DIRECT_WRITES")).getValue() / ((MetricValue) results.get("DIRECT_WRITE_REQS")).getValue()));
        results.put("DIRECT_WRITE_TIME_AVE", new MetricValue(((MetricValue) results.get("DIRECT_WRITE_TIME")).getValue() / ((MetricValue) results.get("DIRECT_WRITES")).getValue()));

    }
}

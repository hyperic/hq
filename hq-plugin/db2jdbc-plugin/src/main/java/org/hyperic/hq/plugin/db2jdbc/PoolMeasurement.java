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
public class PoolMeasurement extends Measurement {

    /**
     * 'TOTAL_LOGICAL_READS' and 'TOTAL_PHYSICAL_READS' Metrics.
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0021988.htm
     *
     * PAGE_HIT_RATIO
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0001235.htm
     *
     * INDEX_PAGE_HIT_RATIO
     * http://publib.boulder.ibm.com/infocenter/db2luw/v9/topic/com.ibm.db2.udb.admin.doc/doc/r0001238.htm
     */
    protected void postProcessResults(Map results) {
        if (results.get("POOL_DATA_L_READS") != null) {
            results.put("TOTAL_LOGICAL_READS", new MetricValue(((MetricValue) results.get("POOL_DATA_L_READS")).getValue() + ((MetricValue) results.get("POOL_INDEX_L_READS")).getValue()));
            results.put("TOTAL_PHYSICAL_READS", new MetricValue(((MetricValue) results.get("POOL_DATA_P_READS")).getValue() + ((MetricValue) results.get("POOL_INDEX_P_READS")).getValue()));
            results.put("DATA_PAGE_HIT_RATIO", new MetricValue(((MetricValue) results.get("POOL_DATA_P_READS")).getValue() / ((MetricValue) results.get("POOL_DATA_L_READS")).getValue()));
            results.put("INDEX_PAGE_HIT_RATIO", new MetricValue(((MetricValue) results.get("POOL_INDEX_P_READS")).getValue() / ((MetricValue) results.get("POOL_INDEX_L_READS")).getValue()));
        }
    }
}

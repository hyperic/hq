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

package org.hyperic.hq.plugin.apache;

import java.util.Properties;

import org.hyperic.util.StringUtil;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.hq.product.PluginException;

public class ApacheMeasurementPlugin
    extends SNMPMeasurementPlugin {

    public MetricValue getValue(Metric metric)
        throws PluginException,
        MetricNotFoundException,
        MetricUnreachableException {

        //XXX backward compat since metrics dont get rescheduled
        String assoc = "applInboundAssociations";        
        if (metric.getAttributeName().equals(assoc)) {
            String varType = "snmpVarType";
            Properties props = metric.getProperties(); 
            String type = props.getProperty(varType);
            if (type.equals("single")) {
                props.setProperty(varType, "next");
            }
        }

        try {
            return super.getValue(metric);
        } catch (MetricNotFoundException e) {
            //make the generic snmp error message easier to understand
            //specific to apache
            String msg =
                StringUtil.replace(e.getMessage(),
                                   ApacheSNMP.TCP_PROTO_ID, "");
            msg = StringUtil.replace(msg, "->", ":");
            throw new MetricNotFoundException(msg, e);
        }
    }
}

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

package org.hyperic.hq.plugin.netdevice;

import java.util.HashMap;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.snmp.SNMPClient;
import org.hyperic.util.StringUtil;

/**
 * Conditionally use Counter64 versions of IF-MIB metrics.
 * These metrics are not supported when using snmp v1.
 * Not all devices support the 64 bit versions regardless.
 */
public class NetworkInterfaceMeasurementPlugin
    extends SNMPMeasurementPlugin {

    static HashMap counter64 = new HashMap();
    static HashMap unsupported = new HashMap();

    static {
        counter64.put("ifInOctets", "ifHCInOctets");
        counter64.put("ifOutOctets", "ifHCOutOctets");

        counter64.put("ifInUcastPkts", "ifHCInUcastPkts");
        counter64.put("ifOutUcastPkts", "ifHCOutUcastPkts");

        counter64.put("ifInNUcastPkts", "ifInMulticastPkts");
        counter64.put("ifOutNUcastPkts", "ifOutMulticastPkts");
    }

    private boolean supportsCounter64(String version) {
        return (version != null) && !version.equals("v1");
    }

    private boolean supportsCounter64(Metric metric) {
        if (unsupported.get(metric.getObjectPropString()) == Boolean.TRUE) {
            return false;
        }

        return supportsCounter64(metric.getObjectProperty(SNMPClient.PROP_VERSION));
    }

    public MetricValue getValue(Metric metric)
        throws MetricUnreachableException,
               MetricNotFoundException,
               PluginException {

        if (supportsCounter64(metric)) {
            String attr = metric.getAttributeName();
            String counter = (String)counter64.get(attr);

            if (counter != null) {
                boolean isDebug = getLog().isDebugEnabled();

                if (isDebug) {
                    getLog().debug("Trying 64 bit counter: " +
                                   attr + "->" + counter);
                }

                //XXX would be nice to avoid this every time.
                String template =
                    StringUtil.replace(metric.toString(), attr, counter);

                Metric metric64 = Metric.parse(template);

                try {
                    return super.getValue(metric64);
                } catch (MetricNotFoundException e) {
                    //not supported, fallthru
                    unsupported.put(metric.getObjectPropString(),
                                    Boolean.TRUE); //dont try again
                    if (isDebug) {
                        getLog().debug("Device does not support Counter64: " +
                                       metric.getObjectPropString());
                    }
                }
            }
        }

        return super.getValue(metric);
    }
}

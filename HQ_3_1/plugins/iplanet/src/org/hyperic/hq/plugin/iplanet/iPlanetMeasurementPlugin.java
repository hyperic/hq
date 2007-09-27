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

package org.hyperic.hq.plugin.iplanet;

import java.io.File;

import org.hyperic.snmp.SNMPClient;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.PluginException;

public class iPlanetMeasurementPlugin
    extends SNMPMeasurementPlugin {

    private static final String MIB_61 = "webserv61.mib";
    private static final String MIB_60 = "iws.mib";

    private static String[] MIBS = { null };

    private static final String THRPOOL_AVAIL = "avail=true";

    private static final String CPU_PREFIX = "iwsCpu";

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {

        if (info.getType() == TypeInfo.TYPE_SERVER) {
            ConfigSchema schema = super.getConfigSchema(info, config); 

            schema.setDefault(SNMPClient.PROP_VERSION,
                              SNMPClient.VALID_VERSIONS[0]);

            return schema;
        }

        return super.getConfigSchema(info, config); 
    }

    public MetricValue getValue(Metric metric)
        throws PluginException,
        MetricNotFoundException,
        MetricUnreachableException {

        boolean isThrpoolAvail =
            metric.toString().endsWith(THRPOOL_AVAIL);

        try {
            MetricValue value = super.getValue(metric);
            if (isThrpoolAvail) {
                value.setValue(Metric.AVAIL_UP);
            }
            return value;
        } catch (MetricNotFoundException e) {
            if (isThrpoolAvail) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }

            // XXX these values only seem to show up in the solaris
            // snmp agent.  once we have the mechanism in place for
            // plugins to specify which metrics are enabled by default
            // these will be off by default and this special case
            // will go away.
            if (metric.getAttributeName().startsWith(CPU_PREFIX)) {
                return new MetricValue(-1);
            }
            else {
                throw e;
            }
        }
    }

    //this sucks, but Sun changed the OIDs between 6.0 and 6.1
    //so we need to choose which mib file to load at runtime.
    protected String[] getMIBs() {
        if (MIBS[0] != null) {
            return MIBS;
        }

        String serverMIB = null;

        String path = iPlanet6Detector.findServerProcess();
        if (path != null) {
            File dir = new File(path).getParentFile();
            File mib = new File(dir,
                                "plugins" + File.separator + "snmp" +
                                File.separator + MIB_61);
            if (!mib.exists()) {
                serverMIB = MIB_60;
                getLog().info("Detected 6.0, loading: " + MIB_60);
            }
        }

        if (serverMIB == null) {
            serverMIB = MIB_61;
            getLog().info("Defaulting to 6.1, loading: " + MIB_61);
        }

        MIBS[0] = serverMIB;

        return MIBS;
    }
}

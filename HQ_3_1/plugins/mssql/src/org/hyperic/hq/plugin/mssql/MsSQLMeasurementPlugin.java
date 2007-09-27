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

package org.hyperic.hq.plugin.mssql;

import java.util.Properties;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin
    extends Win32MeasurementPlugin {

    private static final String PROP_LOCK    = "lock.name";
    private static final String PROP_CACHE   = "cache.name";

    private static final String TOTAL_NAME  = "_Total";

    protected String getDomainName(Metric metric) {
        Properties props = metric.getProperties();
        
        String serviceName =
            props.getProperty(Win32ControlPlugin.PROP_SERVICENAME,
                              MsSQLDetector.DEFAULT_SERVICE_NAME);

        if (serviceName.equalsIgnoreCase(MsSQLDetector.DEFAULT_SERVICE_NAME)) {
            // not sure why they drop the 'MS' from the service name
            // in the default case.
            serviceName = "SQLServer";
        }
        
        return serviceName + ":" + metric.getDomainName();
    }
    
    protected double adjustValue(Metric metric, double value) {
        if (metric.getAttributeName().startsWith("Percent")) {
            value /= 100;
        }
        
        return value;
    }
    
    public String translate(String template, ConfigResponse config) {
        String[] props = {
            MsSQLDetector.PROP_DB,
            PROP_LOCK,
            PROP_CACHE,
        };

        // parse the template-config
        template = super.translate(template, config);

        for (int i=0; i<props.length; i++) {
            String prop = props[i];

            if (template.indexOf(prop) > 0) {
                String value = config.getValue(prop, TOTAL_NAME);
                return StringUtil.replace(template,
                                          "${" + prop + "}",
                                          value);
            }
        }

        return template;
    }
}

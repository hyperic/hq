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
package org.hyperic.hq.plugin.dotnet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class DotNetMeasurementPlugin
        extends Win32MeasurementPlugin {

    private static final String DATA_DOMAIN = ".NET CLR Data";
    private static final String DATA_PREFIX = "SqlClient: ";
    private static final String RUNTIME_NAME = "_Global_";
    private static Log log = LogFactory.getLog(DotNetMeasurementPlugin.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue val = null;
        if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            val = getPDHMetric(metric);
        } else {
            try {
                val = super.getValue(metric);
                if (metric.isAvail()) {
                    val = new MetricValue(Metric.AVAIL_UP);
                }
            } catch (MetricNotFoundException ex) {
                if (metric.isAvail()) {
                    val = new MetricValue(Metric.AVAIL_DOWN);
                } else {
                    throw ex;
                }
            }
        }
        return val;
    }

    protected String getAttributeName(Metric metric) {
        //avoiding Metric parse errors on ':' in DATA_PREFIX.
        if (metric.getDomainName().equals(DATA_DOMAIN)) {
            return DATA_PREFIX + metric.getAttributeName();
        } else {
            return metric.getAttributeName();
        }
    }

    public String translate(String template, ConfigResponse config) {
        if (!template.startsWith(".NET 4.0 ASP.NET App:pdh:")) {
            final String prop = DotNetDetector.PROP_APP;
            template = StringUtil.replace(template, "__percent__", "%");
            template = StringUtil.replace(template, "${" + prop + "}", config.getValue(prop, RUNTIME_NAME));
        } else {
            template = super.translate(template, config);
        }
        return template;
    }

    private MetricValue getPDHMetric(Metric metric) {
        MetricValue res;
        String obj = "\\" + metric.getObjectPropString();
        if (!metric.isAvail()) {
            obj += "\\" + metric.getAttributeName();
        }
        try {
            Double val = new Pdh().getFormattedValue(obj);
            res = new MetricValue(val);
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_UP);
            }
        } catch (Win32Exception ex) {
            if (metric.isAvail()) {
                res = new MetricValue(Metric.AVAIL_DOWN);
                log.info("error on mteric:'" + metric + "' :" + ex.getLocalizedMessage(), ex);
            } else {
                res = MetricValue.NONE;
                log.info("error on mteric:'" + metric + "' :" + ex.getLocalizedMessage());
            }
        }
        return res;
    }
}

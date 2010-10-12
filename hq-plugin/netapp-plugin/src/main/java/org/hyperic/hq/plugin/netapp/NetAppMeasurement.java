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
package org.hyperic.hq.plugin.netapp;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class NetAppMeasurement extends SNMPMeasurementPlugin {

    Log log = getLog();
    private final static double HIGH = Math.pow(2, 32);

    public String translate(String template, ConfigResponse config) {
        template = super.translate(template, config);
        if ((template.contains("AvailOK=")) || (template.contains(":calc="))) {
            log.debug("[translate] --> " + template);
            template = template.replace(":" + SNMPMeasurementPlugin.DOMAIN + ":", ":calc_" + SNMPMeasurementPlugin.DOMAIN + ":");
            template = template.replaceAll("calc=([^\\[]*)\\[([^,]*),([^\\]]*)\\]([^_|:]*)", "calc=$1$2$4,$1$3$4");
            log.debug("[translate] <-- " + template);
        }

        return template;
    }

    public MetricValue getValue(Metric metric) throws MetricUnreachableException, MetricNotFoundException, PluginException {
        MetricValue res = null;

        if (metric.getAttributeName().startsWith("calc=")) {
            String template_a = metric.toString().replaceAll("calc=([^,]*),([^$|:]*)", "$1");
            String template_b = metric.toString().replaceAll("calc=([^,]*),([^$|:]*)", "$2");

            Metric metric_a = Metric.parse(template_a);
            Metric metric_b = Metric.parse(template_b);
            if (log.isDebugEnabled()) {
                log.debug("[getValue] " + metric_a + "'");
                log.debug("[getValue] " + metric_b + "'");
            }

            double res_a = super.getValue(metric_a).getValue();
            double res_b = super.getValue(metric_b).getValue();

            res = new MetricValue((res_a * HIGH) + res_b);
            if (log.isDebugEnabled()) {
                log.debug("[getValue] " + metric_a.getAttributeName() + " = '" + res_a + "'");
                log.debug("[getValue] " + metric_b.getAttributeName() + " = '" + res_b + "'");
                log.debug("[getValue] res = '" + res + "'");
            }
        } else if (metric.getObjectProperty("AvailOK") != null) {
            double resOK = Double.parseDouble(metric.getObjectProperty("AvailOK"));
            res = super.getValue(metric);

            if (log.isDebugEnabled()) {
                log.debug("[getValue] " + metric.getAttributeName() + " = '" + res + "' (AvailOK=" + resOK + ")");
            }

            if (res.getValue() == resOK) {
                res = new MetricValue(Metric.AVAIL_UP);
            } else {
                res = new MetricValue(Metric.AVAIL_DOWN);
            }
        } else {
            res = super.getValue(metric);
        }
        return res;
    }
}

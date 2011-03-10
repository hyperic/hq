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
package org.hyperic.hq.plugin.jboss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class JBoss5MeasurementPlugin extends JBossMeasurementPlugin {

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, PluginException {

        if (getLog().isDebugEnabled()) {
            getLog().debug("*******************");
            getLog().debug("** MT " + metric);
            getLog().debug("** DN " + metric.getDomainName());
            getLog().debug("** OP " + metric.getObjectProperties());
            getLog().debug("** ON " + metric.getObjectName());
            getLog().debug("** AT " + metric.getAttributeName());
            getLog().debug("** PR " + metric.getProperties());
            getLog().debug("*******************");
        }

        MetricValue res;
        try {
            res = super.getValue(metric);
        } catch (Exception ex) {
            getLog().debug(ex.getMessage());
            if (Metric.ATTR_AVAIL.equals(metric.getAttributeName())) {
                res = new MetricValue(Metric.AVAIL_DOWN);
            } else {
                throw new MetricNotFoundException(ex.getMessage(), ex);
            }
        }

        //TOPIC
        if ("topic".equalsIgnoreCase(metric.getObjectProperties().getProperty("service"))) {
            if (res.getValue() < 0) {
                res.setValue(Metric.AVAIL_DOWN);
            } else {
                res.setValue(Metric.AVAIL_UP);
            }
        } else if (metric.getAttributeName().equalsIgnoreCase("state")) {
            switch ((int) res.getValue()) {
                case 8:
                    res.setValue(Metric.AVAIL_UP);
                    break;
                default:
                    res.setValue(Metric.AVAIL_DOWN);
            }
        } else if (metric.getAttributeName().equalsIgnoreCase("AvailableCount")) {
            if (res.getValue() < 0d) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("[getValue] AvailableCount='" + res.getValue() + "' => '0'");
                }
                res.setValue(0);
            }
        }

        if (getLog().isDebugEnabled() && Metric.ATTR_AVAIL.equals(metric.getAttributeName())) {
            getLog().debug("*******************");
            getLog().debug("res = '" + res + "'");
            getLog().debug("*******************");
        }
        return res;
    }

    @Override
    public String translate(String template, ConfigResponse config) {
        return super.translate(translateMetic(template, config), config);

    }

    public static String translateMetic(String template, ConfigResponse config) {
        Pattern p = Pattern.compile("([:|,])([^=]*)=_(%([^%]*)%)([:|,])");
        Matcher m = p.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            //System.out.println("--> "+m);
            if (config.getValue(m.group(4)) != null) {
                m.appendReplacement(sb, m.group(1) + m.group(2) + "=" + m.group(3) + m.group(5));
            } else {
                m.appendReplacement(sb, m.group(1));
            }
        }
        m.appendTail(sb);
        return sb.toString();

    //template=template.replaceAll("=_(%[^%]*%)", "=$1");
    }
}

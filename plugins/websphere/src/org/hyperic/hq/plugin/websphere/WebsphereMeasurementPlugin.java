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

package org.hyperic.hq.plugin.websphere;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.util.StringUtil;

public abstract class WebsphereMeasurementPlugin
    extends MeasurementPlugin {

    protected abstract double getAvailValue(Metric metric);

    protected double getCustomValue(Metric metric)
        throws PluginException,
        MetricUnreachableException,
        MetricNotFoundException {
        throw new MetricInvalidException(); //wont happen
    }
    
    public MetricValue getValue(Metric metric)
        throws PluginException,
        MetricUnreachableException,
        MetricNotFoundException {

        String domain = metric.getDomainName();
        if (domain.equals("ws.avail")) {
            double avail = getAvailValue(metric);
            return new MetricValue(avail);
        }
        else if (domain.equals("ws.custom")) {
            return new MetricValue(getCustomValue(metric));
        }
        else if (domain.equals("hyperic-hq")) {
            //XXX these templates have been removed
            return MetricValue.NONE;
        }

        MetricValue mValue = null;

        Double val = WebspherePMI.getValue(metric);

        mValue = new MetricValue(val, System.currentTimeMillis());

        return mValue;
    }

    public String translate(String template, ConfigResponse config) {
        template = StringUtil.replace(template,
                                      "${admin.vers}",
                                      WebsphereProductPlugin.VERSION_WS5);

        return super.translate(template, config);
    }
}

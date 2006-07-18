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

import java.net.HttpURLConnection;
import java.util.Properties;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.servlet.client.JMXRemote;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.util.StringUtil;

public abstract class WebsphereMeasurementPlugin
    extends MeasurementPlugin {

    private static final double NOT_FOUND = Double.NaN;

    private boolean servletDeployed = true;

    private String adminVersion = null;

    public WebsphereMeasurementPlugin() {
        setName(WebsphereProductPlugin.NAME);
    }

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
        else if (domain.equals(JMXRemote.DEFAULT_DOMAIN)) {
            //if we find out the servlet is not deployed
            //dont bother trying to collect the metrics again.
            if (!this.servletDeployed) {
                return new MetricValue(NOT_FOUND);
            }
        
            Properties props = metric.getProperties();
            if (props.getProperty(JMXRemote.PROP_JMX_URL) == null) {
                String port =
                    props.getProperty(WebsphereProductPlugin.PROP_SERVER_PORT,
                                      "9080");
                String host =
                    props.getProperty(WebsphereProductPlugin.PROP_SERVER_NODE,
                                      "localhost");
                //XXX check host can be resolved, if not fallback to localhost
                //XXX dont want to hardcode protocol
                props.setProperty(JMXRemote.PROP_JMX_URL,
                                  "http://" + host + ":" + port);
            }

            JMXRemote remote = JMXRemote.getInstance(props);

            Object o = null;
            try {
                o = remote.getRemoteMBeanValue(metric);
            } catch (MetricUnreachableException e) {
                //if the url is not found, the servlet is not deployed.
                //we don't want to require that the servlet be deployed.
                if (remote.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                    this.servletDeployed = false;
                    return new MetricValue(NOT_FOUND);
                }
                throw e;
            } catch (MetricInvalidException e) {
                //thrown by JMXRemote if no MBean exists for this name
            }

            if (o == null) {
                return new MetricValue(NOT_FOUND);
            }
            //o will always be a String for us.
            return new MetricValue(Double.parseDouble(o.toString()));
        }

        MetricValue mValue = null;

        Double val = WebspherePMI.getValue(metric);

        mValue = new MetricValue(val, System.currentTimeMillis());

        return mValue;
    }

    private String getAdminVersion() {
        String name = getName();
        if (name.indexOf(WebsphereProductPlugin.VERSION_40) > 0) {
            return WebsphereProductPlugin.VERSION_AE;
        }

        return WebsphereProductPlugin.VERSION_WS5;
    }

    public String translate(String template, ConfigResponse config) {
        //rewrite the appended template-config
        if (template.indexOf(JMXRemote.DEFAULT_DOMAIN + ":") != -1) {
            template =
                StringUtil.replace(template,
                                   WebsphereProductPlugin.PROP_ADMIN_PORT,
                                   WebsphereProductPlugin.PROP_SERVER_PORT);
            template =
                StringUtil.replace(template,
                                   WebsphereProductPlugin.PROP_ADMIN_HOST,
                                   WebsphereProductPlugin.PROP_SERVER_NODE);
        }

        if (adminVersion == null) {
            adminVersion = getAdminVersion();
        }

        template = StringUtil.replace(template,
                                      "${admin.vers}", adminVersion);

        return super.translate(template, config);
    }
}

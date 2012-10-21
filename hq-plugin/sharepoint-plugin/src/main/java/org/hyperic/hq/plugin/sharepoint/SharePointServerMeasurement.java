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
package org.hyperic.hq.plugin.sharepoint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.*;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public class SharePointServerMeasurement extends Win32MeasurementPlugin {

    private Log log = LogFactory.getLog(SharePointServerMeasurement.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = MetricValue.NONE;
        if (metric.getDomainName().equalsIgnoreCase("web")) {
            res = testWebServer(metric);
        } else {
            boolean isAvail = MeasurementConstants.CAT_AVAILABILITY.equalsIgnoreCase(metric.getObjectProperty("Type")) || metric.isAvail();
            try {
                res = super.getValue(metric);
                if (isAvail) {
                    res = new MetricValue(Metric.AVAIL_UP);
                }
            } catch (MetricNotFoundException ex) {
                if (isAvail) {
                    log.debug(ex,ex);
                    res = new MetricValue(Metric.AVAIL_DOWN);
                } else {
                    res = MetricValue.NONE;
                }
            }
        }
        return res;
    }

    private MetricValue testWebServer(Metric metric) throws PluginException {
        double res = Metric.AVAIL_DOWN;

        Properties props = metric.getObjectProperties();
        URL url;
        try {
            url = new URL(props.getProperty("url"));
        } catch (IOException ex) {
            throw new PluginException("Bad Main URL", ex);
        }

        HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        HttpGet get = new HttpGet(targetHost.toURI() + url.getPath());
        AgentKeystoreConfig ksConfig = new AgentKeystoreConfig();
        HQHttpClient client = new HQHttpClient(ksConfig, new HttpConfig(5000, 5000, null, 0), ksConfig.isAcceptUnverifiedCert());
        try {
            HttpResponse response = client.execute(get, new BasicHttpContext());
            int r = response.getStatusLine().getStatusCode();
            log.debug("[testWebServer] url='" + get.getURI() + "' statusCode='" + r + "'");
            res = r < 500 ? Metric.AVAIL_UP : Metric.AVAIL_DOWN;
        } catch (IOException ex) {
            log.debug(ex.getMessage(), ex);
        }
        return new MetricValue(res);
    }
}

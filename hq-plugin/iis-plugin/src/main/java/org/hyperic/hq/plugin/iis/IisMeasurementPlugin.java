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
package org.hyperic.hq.plugin.iis;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.*;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public class IisMeasurementPlugin extends Win32MeasurementPlugin {

    private Log log = LogFactory.getLog(IisMeasurementPlugin.class);
    // The special hostname that means the base server instead of a Vhost
    private static String SERVER_HOSTNAME = "_Total";
    public static String PROP_IISHOST = "iishost";

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = MetricValue.NONE;
        if (metric.getDomainName().equalsIgnoreCase("web")) {
            res = testWebServer(metric);
        } else {
            res = super.getValue(metric);
        }
        return res;
    }

    private MetricValue testWebServer(Metric metric) throws PluginException {
        double res = Metric.AVAIL_DOWN;

        Properties props = metric.getObjectProperties();
        int port = Integer.parseInt(props.getProperty("port"));
        String addr = props.getProperty("hostname");
        String path = props.getProperty("path");
        boolean https = "true".equals(props.getProperty("ssl"));

        HttpHost targetHost = new HttpHost(addr, port, https ? "https" : "http");
        HttpGet get = new HttpGet(targetHost.toURI() + path);
        AgentKeystoreConfig ksConfig = new AgentKeystoreConfig();
        HQHttpClient client = new HQHttpClient(ksConfig, new HttpConfig(5000, 5000, null, 0), ksConfig.isAcceptUnverifiedCert());
        try {
            HttpResponse response = client.execute(get, new BasicHttpContext());
            int r = response.getStatusLine().getStatusCode();
            log.debug("[testWebServer] url='" + get.getURI() + "' statusCode='" + r + "'");
            res = Metric.AVAIL_UP;
        } catch (IOException ex) {
            log.debug(ex.getMessage(), ex);
        }
        return new MetricValue(res);
    }

    @Override
    public String translate(String template, ConfigResponse config) {
        /**
         * If there is no hostname in the ConfigResponse, then we want the value
         * for the server, which is an aggregate of all of the services. IIS
         * gives this to use with the _Total hostname.
         */
        if (config.getValue(PROP_IISHOST) == null) {
            config.setValue(PROP_IISHOST, SERVER_HOSTNAME);
        }
        template = StringUtil.replace(template,
                "${" + PROP_IISHOST + "}",
                config.getValue(PROP_IISHOST));

        return super.translate(template, config);
    }
}

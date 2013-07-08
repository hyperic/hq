/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2013], Hyperic, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public class SharePointServerMeasurement extends Win32MeasurementPlugin {

    private static Log log = LogFactory.getLog(SharePointServerMeasurement.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res;
        log.debug("[getValue] metric=" + metric);
        if (metric.getDomainName().equalsIgnoreCase("server")) {
            res = checkServerAvail(metric);
        } else if (metric.getDomainName().equalsIgnoreCase("web")) {
            if (metric.isAvail()) {
                res = new MetricValue(checkWebAvail(metric.getObjectProperty("name")));
            } else {
                try {
                    long rt = System.currentTimeMillis();
                    testWebServer(metric.getObjectProperty("url"));
                    rt = System.currentTimeMillis() - rt;
                    res = new MetricValue(rt);
                } catch (PluginException ex) {
                    log.debug(ex, ex);
                    throw ex;
                }
            }
        } else if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            if (metric.getAttributeName().equalsIgnoreCase("Object Cache Hit %")) {
                double hits = getPDHMetric("\\" + metric.getObjectPropString() + "\\Object Cache Hit Count");
                double miss = getPDHMetric("\\" + metric.getObjectPropString() + "\\Object Cache Miss Count");
                if ((hits >= 0) && (miss >= 0) && ((hits + miss) > 0)) {
                    res = new MetricValue(hits / (hits + miss));
                } else {
                    res = MetricValue.NONE;
                }
            } else {
                res = getPDHMetric(metric);
            }
        } else {
            throw new PluginException("incorrect domain '" + metric.getDomainName() + "'");
        }
        return res;
    }

    private MetricValue checkServerAvail(Metric metric) {
        double res = Metric.AVAIL_UP;

        List<String> services, webs;
        String w = metric.getObjectProperty("webs");
        if (w != null) {
            webs = Arrays.asList(w.split(","));
        } else {
            webs = new ArrayList<String>();
        }

        String s = metric.getObjectProperty("services");
        if (s != null) {
            services = Arrays.asList(s.split(","));
        } else {
            services = new ArrayList<String>();
        }

        for (int i = 0; (i < webs.size()) && (res == Metric.AVAIL_UP); i++) {
            String web = webs.get(i).trim();
            res = checkWebAvail(web);
        }

        for (int i = 0; (i < services.size()) && (res == Metric.AVAIL_UP); i++) {
            String service = services.get(i).trim();
            res = checkServiceAvail(service);
        }

        return new MetricValue(res);
    }

    private double checkServiceAvail(String service) {
        log.debug("[checkServiceAvail] * service='" + service + "'");
        double res = Metric.AVAIL_DOWN;
        try {
            if (service != null) {
                Service s = new Service(service);
                if (s.getStatus() == Service.SERVICE_RUNNING) {
                    res = Metric.AVAIL_UP;
                }
                log.debug("[checkServiceAvail] service='" + service + "' res=" + res);
            }
        } catch (Win32Exception ex) {
            log.debug("[checkServiceAvail] error. service='" + service + "'", ex);
        }
        return res;
    }

    private double checkWebAvail(String webserver) {
        double res = Metric.AVAIL_DOWN;
        String[] cmd = {IisMetaBase.APPCMD, "list", "site", webserver};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecuteWatchdog wdog = new ExecuteWatchdog(60 * 1000);
        Execute exec = new Execute(new PumpStreamHandler(output), wdog);
        exec.setCommandline(cmd);
        try {
            int exitStatus = exec.execute();
            log.debug("[checkWebAvail] webserver=" + webserver + ",exitStatus:" + exitStatus + ", output=" + output.toString());
            if (exitStatus == 0 || !wdog.killedProcess()) {
                log.debug("[checkWebAvail] webserver=" + webserver + ", output=" + output);
                if (output.toString().toLowerCase().contains(":started")) {
                    res = Metric.AVAIL_UP;
                }
            }
        } catch (Exception e) {
            log.debug("[checkWebAvail] webserver=" + webserver + ", error=" + e.getMessage(), e);
        }
        return res;
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
                log.debug("error on mteric:'" + metric + "' :" + ex.getLocalizedMessage(), ex);
            } else {
                res = MetricValue.NONE;
                log.debug("error on metric:'" + metric + "' :" + ex.getLocalizedMessage());
            }
        }
        return res;
    }

    private double getPDHMetric(String obj) {
        double res = -1;
        try {
            res = new Pdh().getFormattedValue(obj);
        } catch (Win32Exception ex) {
            log.debug("error on value for object:'" + obj + "' :" + ex.getLocalizedMessage());
        }
        return res;
    }

    private void testWebServer(String url) throws PluginException {
        HttpGet get = new HttpGet(url);
        AgentKeystoreConfig ksConfig = new AgentKeystoreConfig();
        HQHttpClient client = new HQHttpClient(ksConfig, new HttpConfig(5000, 5000, null, 0), ksConfig.isAcceptUnverifiedCert());
        try {
            HttpResponse response = client.execute(get, new BasicHttpContext());
            int r = response.getStatusLine().getStatusCode();
            log.debug("[testWebServer] url='" + get.getURI() + "' statusCode='" + r + "' " + response.getStatusLine().getReasonPhrase());
            if (r >= 500) {
                throw new PluginException("[testWebServer] error=" + r);
            }
        } catch (IOException ex) {
            throw new PluginException(ex.getMessage(), ex);
        }
    }
}

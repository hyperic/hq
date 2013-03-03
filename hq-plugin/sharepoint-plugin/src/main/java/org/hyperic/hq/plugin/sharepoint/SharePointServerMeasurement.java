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
import java.net.URL;
import java.util.Properties;
import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.auth.NTLMEngineException;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public class SharePointServerMeasurement extends Win32MeasurementPlugin {

    private static Log log = LogFactory.getLog(SharePointServerMeasurement.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res;
        log.debug("[getValue] metric="+metric);
        if (metric.getDomainName().equalsIgnoreCase("web")) {
            try {
                long rt = System.currentTimeMillis();
                testWebServer(metric.getObjectProperties());
                rt = System.currentTimeMillis() - rt;
                if (metric.isAvail()) {
                    res = new MetricValue(Metric.AVAIL_UP);
                } else {
                    res = new MetricValue(rt);
                }
            } catch (PluginException ex) {
                log.debug(ex, ex);
                if (metric.isAvail()) {
                    res = new MetricValue(Metric.AVAIL_DOWN);
                } else {
                    throw ex;
                }
            }
        } else if (metric.getDomainName().equalsIgnoreCase("pdh")) {
            if (metric.getAttributeName().equalsIgnoreCase("Object Cache Hit %")) {
                double hits = getPDHMetric("\\" + metric.getObjectPropString() + "\\Object Cache Hit Count");
                double miss = getPDHMetric("\\" + metric.getObjectPropString() + "\\Object Cache Miss Count");
                if ((hits >= 0) && (miss >= 0) && ((hits+miss)>0)) {
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
                log.info("error on metric:'" + metric + "' :" + ex.getLocalizedMessage());
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

    protected static void testWebServer(Properties props) throws PluginException {

        String user = props.getProperty("user");
        String pass = props.getProperty("password");
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
        if ((user != null) && (pass != null)) {
            client.getAuthSchemes().register("NTLM", new NTLMJCIFSSchemeFactory());
            client.getAuthSchemes().unregister("NEGOTIATE");
            NTCredentials creds = new NTCredentials(user, pass, "", "");
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
        }

        try {
            HttpResponse response = client.execute(get, new BasicHttpContext());
            int r = response.getStatusLine().getStatusCode();
            log.debug("[testWebServer] url='" + get.getURI() + "' user='" + user + "' statusCode='" + r + "' ("+response.getStatusLine().getReasonPhrase()+")");
            if (r>=500) {
                throw new PluginException(response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            log.debug(ex.getMessage(), ex);
            throw new PluginException(ex.getMessage(), ex);
        }
    }

    /**
     * http://hc.apache.org/httpcomponents-client-ga/ntlm.html
     */
    public static final class JCIFSEngine implements NTLMEngine {

        private static final int TYPE_1_FLAGS =
                NtlmFlags.NTLMSSP_NEGOTIATE_56
                | NtlmFlags.NTLMSSP_NEGOTIATE_128
                | NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2
                | NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN
                | NtlmFlags.NTLMSSP_REQUEST_TARGET;

        public String generateType1Msg(final String domain, final String workstation)
                throws NTLMEngineException {
            final Type1Message type1Message = new Type1Message(TYPE_1_FLAGS, domain, workstation);
            return Base64.encode(type1Message.toByteArray());
        }

        public String generateType3Msg(final String username, final String password,
                final String domain, final String workstation, final String challenge)
                throws NTLMEngineException {
            Type2Message type2Message;
            try {
                type2Message = new Type2Message(Base64.decode(challenge));
            } catch (final IOException exception) {
                throw new NTLMEngineException("Invalid NTLM type 2 message", exception);
            }
            final int type2Flags = type2Message.getFlags();
            final int type3Flags = type2Flags
                    & (0xffffffff ^ (NtlmFlags.NTLMSSP_TARGET_TYPE_DOMAIN | NtlmFlags.NTLMSSP_TARGET_TYPE_SERVER));
            final Type3Message type3Message = new Type3Message(type2Message, password, domain,
                    username, workstation, type3Flags);
            return Base64.encode(type3Message.toByteArray());
        }
    }

    public static class NTLMJCIFSSchemeFactory implements AuthSchemeFactory {

        public AuthScheme newInstance(final HttpParams params) {
            return new NTLMScheme(new JCIFSEngine());
        }
    }
}

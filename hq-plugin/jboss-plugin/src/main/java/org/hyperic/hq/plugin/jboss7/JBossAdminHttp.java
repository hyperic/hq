/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.plugin.jboss7.objects.Connector;
import org.hyperic.hq.plugin.jboss7.objects.DataSource;
import org.hyperic.hq.plugin.jboss7.objects.DataSource71;
import org.hyperic.hq.plugin.jboss7.objects.Deployment;
import org.hyperic.hq.plugin.jboss7.objects.ServerInfo;
import org.hyperic.hq.plugin.jboss7.objects.ServerMemory;
import org.hyperic.hq.plugin.jboss7.objects.ThreadsInfo;
import org.hyperic.hq.plugin.jboss7.objects.TransactionsStats;
import org.hyperic.hq.plugin.jboss7.objects.WebSubsystem;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public final class JBossAdminHttp {

    private static final Log log = LogFactory.getLog(JBossAdminHttp.class);
    private final DefaultHttpClient client;
    private String user;
    private String pass;
    private BasicHttpContext localcontext;
    private HttpHost targetHost;
    private String hostName;
    private String serverName;

    public JBossAdminHttp(Properties props) throws PluginException {
        try {
            int port = Integer.parseInt(props.getProperty(JBossStandaloneDetector.PORT));
            String addr = props.getProperty(JBossStandaloneDetector.ADDR);
            boolean https = "true".equals(props.getProperty(JBossStandaloneDetector.HTTPS));
            this.user = props.getProperty(JBossStandaloneDetector.USERNAME);
            this.pass = props.getProperty(JBossStandaloneDetector.PASSWORD);
            this.hostName = props.getProperty(JBossStandaloneDetector.HOST);
            this.serverName = props.getProperty(JBossStandaloneDetector.SERVER);
            log.debug("props=" + props);

            targetHost = new HttpHost(addr, port, https ? "https" : "http");
            log.debug("targetHost=" + targetHost);
            AgentKeystoreConfig config = new AgentKeystoreConfig();
            client = new HQHttpClient(config, new HttpConfig(5000, 5000, null, 0), config.isAcceptUnverifiedCert());
            if ((user != null) && (pass != null)) {
                client.getCredentialsProvider().setCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                        new UsernamePasswordCredentials(user, pass));
            }
        } catch (Throwable ex) {
            throw new PluginException(ex.getMessage(), ex);
        }

//        AuthCache authCache = new BasicAuthCache();
//        DigestScheme digestAuth = new DigestScheme();
//        digestAuth.overrideParamter("realm", "'TestRealm'");
//        authCache.put(targetHost, digestAuth);
//        localcontext = new BasicHttpContext();
//        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    }

    public JBossAdminHttp(ConfigResponse props) throws PluginException {
        this(props.toProperties());
    }

    private Object get(String api, Type type) throws PluginException {
        Object res = null;
        try {
            String url = targetHost.toURI() + "/management";
            if (hostName != null) {
                url += "/host/" + hostName;
                if (serverName != null) {
                    url += "/server/" + serverName;
                }
            }
            HttpGet get = new HttpGet(url + api);
            HttpResponse response = client.execute(get, localcontext);
            int statusCode = response.getStatusLine().getStatusCode();
            // response must be read in order to "close" the connection.
            // https://jira.hyperic.com/browse/HHQ-5063#comment-154101
            String responseBody = readInputString(response.getEntity().getContent());

            if (log.isDebugEnabled()) {
                log.debug("[" + api + "] -(" + get.getURI() + ")-> " + responseBody);
            }

            if (statusCode != 200) {
                throw new PluginException("[" + get.getURI() + "] http error code: '" + statusCode + "' msg='" + response.getStatusLine().getReasonPhrase() + "'");
            }

            GsonBuilder gsb = new GsonBuilder();
            if (!((type instanceof Class)
                    && ((Class) type).getCanonicalName().equals(Connector.class.getCanonicalName()))) {
                gsb.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
            }
            Gson gson = gsb.create();

            res = gson.fromJson(responseBody, type);
            if (log.isDebugEnabled()) {
                if (res.getClass().isArray()) {
                    log.debug("[" + api + "] -(" + statusCode + ")*> " + Arrays.asList((Object[]) res));
                } else {
                    log.debug("[" + api + "] -(" + statusCode + ")-> " + res);
                }
            }
        } catch (JsonParseException ex) {
            log.debug(ex.getMessage(), ex);
            throw new PluginException(ex.getMessage(), ex);
        } catch (IOException ex) {
            log.debug(ex.getMessage(), ex);
            throw new PluginException(ex.getMessage(), ex);
        }
        return res;
    }

    public WebSubsystem getWebSubsystem() throws PluginException {
        Type type = new TypeToken<WebSubsystem>() {
        }.getType();
        return (WebSubsystem) get("/subsystem/web?recursive=true", type);
    }

    public Connector getConnector(String connector) throws PluginException {
        Type type = new TypeToken<Connector>() {
        }.getType();
        try {
            connector = URLEncoder.encode(connector, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return (Connector) get("/subsystem/web/connector/" + connector + "?include-runtime=true", type);
    }

    public ThreadsInfo getThreadsInfo() throws PluginException {
        Type type = new TypeToken<ThreadsInfo>() {
        }.getType();
        return (ThreadsInfo) get("/core-service/platform-mbean/type/threading", type);
    }

    public ServerMemory getServerMemory() throws PluginException {
        Type type = new TypeToken<ServerMemory>() {
        }.getType();
        return (ServerMemory) get("/core-service/platform-mbean/type/memory?include-runtime=true", type);
    }

    public TransactionsStats getTransactionsStats() throws PluginException {
        Type type = new TypeToken<TransactionsStats>() {
        }.getType();
        return (TransactionsStats) get("/subsystem/transactions?include-runtime=true", type);
    }

    public List<Deployment> getDeployments() throws PluginException {
        Type type = new TypeToken<List<Deployment>>() {
        }.getType();
        return (List<Deployment>) get("/deployment/*?recursive=true", type);
    }

    public List<String> getDatasources() throws PluginException {
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {
        }.getType();
        Map<String, Map<String, Object>> ds = (Map<String, Map<String, Object>>) get("/subsystem/datasources", type);
        List<String> res = new ArrayList<String>();
        if (ds.get("data-source") != null) {
            res.addAll(ds.get("data-source").keySet());
        }
        if (ds.get("xa-data-source") != null) {
            res.addAll(ds.get("xa-data-source").keySet());
        }
        return res;
    }

    public DataSource getDatasource(String ds, boolean runtime, String jbossVersion) throws PluginException {
        Type type;

        if (jbossVersion.equalsIgnoreCase("7")) {
            type = new TypeToken<DataSource>() {
            }.getType();
        } else {
            type = new TypeToken<DataSource71>() {
            }.getType();
        }

        try {
            ds = URLEncoder.encode(ds, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        if (runtime) {
            ds += "?include-runtime=true";
            if (!jbossVersion.equalsIgnoreCase("7")) {
                ds += "&recursive";
            }
        }
        DataSource res = (DataSource) get("/subsystem/datasources/data-source/" + ds, type);
        return res;
    }

    void testConnection() throws PluginException {
        Type type = new TypeToken<ServerInfo>() {
        }.getType();
        get("/", type);
    }

    public static String readInputString(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}

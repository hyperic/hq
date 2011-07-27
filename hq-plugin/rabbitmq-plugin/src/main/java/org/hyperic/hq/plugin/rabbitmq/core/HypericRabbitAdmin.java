/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.core;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

/**
 * A HypericRabbitAdmin is created for each node/virtualHost.
 * HypericRabbitAdmin
 * @author Helena Edelson
 */
public final class HypericRabbitAdmin {

    private static final Log logger = LogFactory.getLog(HypericRabbitAdmin.class);
    private final DefaultHttpClient client;
    private String user;
    private String pass;
    private BasicHttpContext localcontext;
    private HttpHost targetHost;

    public HypericRabbitAdmin(Properties props) throws PluginException {
        int port = Integer.parseInt(props.getProperty(DetectorConstants.PORT));
        String addr = props.getProperty(DetectorConstants.ADDR);
        boolean https = "true".equals(props.getProperty(DetectorConstants.HTTPS));
        this.user = props.getProperty(DetectorConstants.USERNAME);
        this.pass = props.getProperty(DetectorConstants.PASSWORD);

        targetHost = new HttpHost(addr, port, https ? "https" : "http");
        AgentKeystoreConfig config = new AgentKeystoreConfig();
        client = new HQHttpClient(config, new HttpConfig(5000, 5000, null, 0), config.isAcceptUnverifiedCert());
        client.getCredentialsProvider().setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort(), "Management: Web UI"),
                new UsernamePasswordCredentials(user, pass));
        List authPrefs = new ArrayList(1);
        authPrefs.add("Management: Web UI");


        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        localcontext = new BasicHttpContext();
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    }

    public HypericRabbitAdmin(ConfigResponse props) throws PluginException {
        this(props.toProperties());
    }

    public RabbitOverview getOverview() throws PluginException {
        return get("/api/overview", RabbitOverview.class);
    }

    public List<RabbitVirtualHost> getVirtualHosts() throws PluginException {
        List<RabbitVirtualHost> res;
        try {
            res = Arrays.asList(get("/api/vhosts", RabbitVirtualHost[].class));
        } catch (PluginException ex) {
            logger.debug("[getVirtualHosts] " + ex.getLocalizedMessage(),ex);
            res = new ArrayList<RabbitVirtualHost>();
            List<String> names = Arrays.asList(get("/api/vhosts", String[].class));
            for (String name : names) {
                res.add(new RabbitVirtualHost(name));
            }
        }
        return res;
    }

    public List<RabbitQueue> getQueues(RabbitVirtualHost vh) throws PluginException {
        try {
            return Arrays.asList(get("/api/queues/" + URLEncoder.encode(vh.getName(), "UTF-8"), RabbitQueue[].class));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<RabbitExchange> getExchanges(RabbitVirtualHost vh) throws PluginException {
        try {
            return Arrays.asList(get("/api/exchanges/" + URLEncoder.encode(vh.getName(), "UTF-8"), RabbitExchange[].class));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<RabbitConnection> getConnections() throws PluginException {
        return Arrays.asList(get("/api/connections", RabbitConnection[].class));
    }

    public RabbitConnection getConnection(String cName) throws PluginException {
        try {
            cName = URLEncoder.encode(cName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return get("/api/connections/" + cName, RabbitConnection.class);
    }

    public List<RabbitChannel> getChannels() throws PluginException {
        return Arrays.asList(get("/api/channels", RabbitChannel[].class));
    }

    public RabbitChannel getChannel(String chName) throws PluginException {
        try {
            chName = URLEncoder.encode(chName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return get("/api/channels/" + chName, RabbitChannel.class);
    }

    public RabbitVirtualHost getVirtualHost(String vhName) throws PluginException {
        try {
            vhName = URLEncoder.encode(vhName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        RabbitVirtualHost res;
        try {
            res = get("/api/vhosts/" + vhName, RabbitVirtualHost.class);
        } catch (PluginException ex) {
            logger.debug("[getVirtualHost] " + ex.getLocalizedMessage(),ex);
            String name = get("/api/vhosts/" + vhName, String.class);
            res = new RabbitVirtualHost(name);
        }
        return res;
    }

    public RabbitQueue getVirtualQueue(String vhName, String qName) throws PluginException {
        try {
            vhName = URLEncoder.encode(vhName, "UTF-8");
            qName = URLEncoder.encode(qName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        return get("/api/queues/" + vhName + "/" + qName, RabbitQueue.class);
    }

    public RabbitExchange getExchange(String vhost, String exch) throws PluginException {
        try {
            vhost = URLEncoder.encode(vhost, "UTF-8");
            exch = URLEncoder.encode(exch, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        return get("/api/exchanges/" + vhost + "/" + exch, RabbitExchange.class);
    }

    public RabbitNode getNode(String node) throws PluginException {
        RabbitNode res;
        try {
            node = URLEncoder.encode(node, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        try {
            res = get("/api/nodes/" + node, RabbitNode.class);
        } catch (PluginException ex) {
            logger.debug("[getVirtualHost] " + ex.getLocalizedMessage(),ex);
            res = get("/api/overview/", RabbitNode.class);
            res.setName(node);
            res.setRunning(true);
        }
        return res;
    }

    private <T extends Object> T get(String api, Class<T> classOfT) throws PluginException {
        T res = null;
        try {
            HttpGet get = new HttpGet(targetHost.toURI() + api);
            HttpResponse response = client.execute(get, localcontext);
            int r = response.getStatusLine().getStatusCode();
            // response must be read in order to "close" the connection.
            // https://jira.hyperic.com/browse/HHQ-5063#comment-154101
            String responseBody = readInputString(response.getEntity().getContent());

            if (logger.isDebugEnabled()) {
                logger.debug("[" + api + "] -(" + r + ")-> " + responseBody);
            }

            if (r != 200) {
                throw new PluginException("[" + api + "] http error code: '" + r + "'");
            }

            GsonBuilder gsb = new GsonBuilder();
            gsb.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            gsb.registerTypeAdapter(Integer.class, new IntegerDeserializer());
            gsb.registerTypeAdapter(int.class, new IntegerDeserializer());
            gsb.registerTypeAdapter(Date.class, new DateTimeDeserializer());
            gsb.registerTypeAdapter(MessageStats.class, new MessageStatsDeserializer());
            Gson gson = gsb.create();

            res = gson.fromJson(responseBody, classOfT);
            if (logger.isDebugEnabled()) {
                if (res.getClass().isArray()) {
                    logger.debug("[" + api + "] -(" + r + ")*> " + Arrays.asList((Object[]) res));
                } else {
                    logger.debug("[" + api + "] -(" + r + ")-> " + res);
                }
            }
        } catch (IOException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new PluginException(ex.getMessage(), ex);
        }
        return res;
    }

    private class DateTimeDeserializer implements JsonDeserializer<Date> {

        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                return (Date) formatter.parse(json.getAsString());
            } catch (ParseException ex) {
                throw new JsonParseException(ex.getMessage(), ex);
            }
        }
    }

    private class IntegerDeserializer implements JsonDeserializer<Integer> {

        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return json.getAsInt();
            } catch (NumberFormatException ex) {
                if (json.getAsString().equals("install_handle_from_sysinternals")) {
                    logger.debug("'Handle V3.42 (sysinternals)' is required by 'rabbitmq-management-agent' plugin on Windows Platforms. "
                            + ex.getMessage());
                    return null;
                }
                throw new JsonParseException(ex.getMessage(), ex);
            }
        }
    }

    private class MessageStatsDeserializer implements JsonDeserializer<MessageStats> {

        public MessageStats deserialize(JsonElement je, Type Type, JsonDeserializationContext jdc) throws JsonParseException {
            MessageStats res = null;
            if (je.isJsonArray()) {
                res = new MessageStats();
            } else {
                GsonBuilder gsb = new GsonBuilder();
                gsb.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                Gson gson = gsb.create();
                res = gson.fromJson(je, Type);
            }
            return res;
        }
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

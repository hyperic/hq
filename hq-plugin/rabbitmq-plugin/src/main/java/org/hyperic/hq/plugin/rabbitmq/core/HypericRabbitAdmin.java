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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 * A HypericRabbitAdmin is created for each node/virtualHost.
 * HypericRabbitAdmin
 * @author Helena Edelson
 */
public class HypericRabbitAdmin {

    private static final Log logger = LogFactory.getLog(HypericRabbitAdmin.class);
    private final HttpClient client;
    private String nodeName;
    private String addr;
    private String user;
    private String pass;
    private int port;

    public HypericRabbitAdmin(Properties props) {
        this.nodeName = props.getProperty("node");
        this.port = Integer.parseInt(props.getProperty("port"));
        this.addr = props.getProperty("addr");
        this.user = props.getProperty("user");
        this.pass = props.getProperty("pass");

        client = new HttpClient();
        client.getState().setCredentials(
                new AuthScope(addr, port, "Management: Web UI"),
                new UsernamePasswordCredentials(user, pass));
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

    }

    public HypericRabbitAdmin(ConfigResponse props) {
        this(props.toProperties());
    }

    public void destroy() {
        logger.debug("[HypericRabbitAdmin] destroy()");
    }

    public List<RabbitVirtualHost> getVirtualHosts() throws PluginException {
        return Arrays.asList(get("/api/vhosts", RabbitVirtualHost[].class));
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
        return get("/api/vhosts/" + vhName, RabbitVirtualHost.class);
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
        try {
            node = URLEncoder.encode(node, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        return get("/api/nodes/" + node, RabbitNode.class);
    }

    private <T extends Object> T get(String api, Class<T> classOfT) throws PluginException {
        T res = null;
        try {
            GetMethod get = new GetMethod("http://" + addr + ":" + port + api);
            get.setDoAuthentication(true);
            int r = client.executeMethod(get);
            if (r != 200) {
                throw new PluginException("[" + api + "] http error code: '" + r + "'");
            }
            String responseBody = get.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("[" + api + "] -(" + r + ")-> " + responseBody);
            }

            GsonBuilder gsb = new GsonBuilder();
            gsb.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            gsb.registerTypeAdapter(Date.class, new DateTimeDeserializer());
            Gson gson = gsb.create();

            res = gson.fromJson(responseBody, classOfT);
            if (logger.isDebugEnabled()) {
                if (res.getClass().isArray()) {
                    logger.debug("[" + api + "] -(" + r + ")*> " + Arrays.asList((Object[])res));
                } else {
                    logger.debug("[" + api + "] -(" + r + ")-> " + res);
                }
            }
        } catch (IOException ex) {
            logger.error(ex);
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
}

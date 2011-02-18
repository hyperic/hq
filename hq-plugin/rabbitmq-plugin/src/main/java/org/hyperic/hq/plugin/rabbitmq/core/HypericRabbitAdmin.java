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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * A HypericRabbitAdmin is created for each node/virtualHost.
 * HypericRabbitAdmin
 * @author Helena Edelson
 */
public class HypericRabbitAdmin {

    private static final Log logger = LogFactory.getLog(HypericRabbitAdmin.class);
    private final HttpClient client;

    public HypericRabbitAdmin(Properties props) {
         client = new HttpClient();

        client.getState().setCredentials(
                new AuthScope("192.168.183.140", 55672, "Management: Web UI"),
                new UsernamePasswordCredentials("guest", "guest"));

        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
    }

    public HypericRabbitAdmin(ConfigResponse props) {
        this(props.toProperties());
    }

    public void destroy() {
        logger.debug("[HypericRabbitAdmin] destroy()");
    }

    public List<RabbitVirtualHost> getVirtualHosts() {
        List<RabbitVirtualHost> res = null;
        GetMethod get = new GetMethod("http://192.168.183.140:55672/api/vhosts");
        get.setDoAuthentication(true);

        try {
            int r = client.executeMethod(get);
            String responseBody = get.getResponseBodyAsString();
            logger.debug("-(" + r + ")-> " + responseBody);
            Gson gson = new Gson();
            res = Arrays.asList(gson.fromJson(responseBody, RabbitVirtualHost[].class));
        } catch (IOException ex) {
            logger.error(ex);
        }

        return res;
    }

    public List getQueues(RabbitVirtualHost vh) {
        List<RabbitQueue> res = null;
        GetMethod get = new GetMethod("http://192.168.183.140:55672/api/queues/"+vh.getName());
        get.setDoAuthentication(true);

        try {
            int r = client.executeMethod(get);
            String responseBody = get.getResponseBodyAsString();
            logger.debug("-(" + r + ")-> " + responseBody);
            Gson gson = new Gson();
            res = Arrays.asList(gson.fromJson(responseBody, RabbitQueue[].class));
        } catch (IOException ex) {
            logger.error(ex);
        }

        return res;
    }

    public List getExchanges(RabbitVirtualHost virtualHost) {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List getBindings(String virtualHost) {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List<RabbitConnection> getConnections() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public List<RabbitChannel> getChannels() {
        List res = null;
        return res;
    }

    public boolean getStatus() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public String getPeerNodeName() {
        throw new RuntimeException("XXXXXXXXXX");
    }

    public boolean virtualHostAvailable(String virtualHost, String node) {
        throw new RuntimeException("XXXXXXXXXX");
    }
}

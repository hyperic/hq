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

import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Properties;
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
    private HypericErlangTemplate customErlangTemplate;
    private String peerNodeName;
    private static boolean NEW = false;
    private static boolean MAKE_TEST = true;

    public HypericRabbitAdmin(Properties props) {
        this(new ConfigResponse(props));
    }

    public HypericRabbitAdmin(ConfigResponse props) {
        this(props.getValue(DetectorConstants.NODE), props.getValue(DetectorConstants.AUTHENTICATION));
    }

    private HypericRabbitAdmin(String peerNodeName, String cookie) {
        logger.debug("[HypericRabbitAdmin] init(" + peerNodeName + "," + cookie + ")");
        this.peerNodeName = peerNodeName;
        SingleConnectionFactory otpConnectionFactory = new SingleConnectionFactory("rabbit-monitor", cookie, peerNodeName);
        otpConnectionFactory.afterPropertiesSet();
        this.customErlangTemplate = new HypericErlangTemplate(otpConnectionFactory);
        this.customErlangTemplate.afterPropertiesSet();

        if (MAKE_TEST) {
            try {
                customErlangTemplate.executeRpcAndConvert("rabbit_access_control", "list_vhosts", new ErlangArgs(null, String.class));
                logger.debug("old 'list_vhosts' detected.");
            } catch (ErlangBadRpcException ex) {
                NEW = true;
                logger.debug("new 'list_vhosts' detected. ('" + ex.getMessage() + "')");
            }
            MAKE_TEST=false;
        }
    }

    public void destroy() {
        logger.debug("[HypericRabbitAdmin] destroy()");
        ((SingleConnectionFactory) customErlangTemplate.getConnectionFactory()).destroy();
    }

    public List<String> getVirtualHosts() throws PluginException {
        if (NEW) {
            return (List<String>) customErlangTemplate.executeRpcAndConvert("rabbit_vhost", "list", new ErlangArgs(null, String.class));
        } else {
            return (List<String>) customErlangTemplate.executeRpcAndConvert("rabbit_access_control", "list_vhosts", new ErlangArgs(null, String.class));
        }
    }

    public List<QueueInfo> getQueues(String virtualHost) throws ErlangBadRpcException {
        return (List<QueueInfo>) customErlangTemplate.executeRpcAndConvert("rabbit_amqqueue", "info_all", new ErlangArgs(virtualHost, QueueInfo.class));
    }

    public List<Exchange> getExchanges(String virtualHost) throws ErlangBadRpcException {
        return (List<Exchange>) customErlangTemplate.executeRpcAndConvert("rabbit_exchange", "list", new ErlangArgs(virtualHost, Exchange.class));
    }

    public List<RabbitBinding> getBindings(String virtualHost) throws ErlangBadRpcException {
        return (List<RabbitBinding>) customErlangTemplate.executeRpcAndConvert("rabbit_exchange", "list_bindings", new ErlangArgs(virtualHost, RabbitBinding.class));
    }

    public List<RabbitConnection> getConnections() throws ErlangBadRpcException {
        return (List<RabbitConnection>) customErlangTemplate.executeRpcAndConvert("rabbit_networking", "connection_info_all", new ErlangArgs(null, RabbitConnection.class));
    }

    public List<RabbitChannel> getChannels() throws ErlangBadRpcException {
        return (List<RabbitChannel>) customErlangTemplate.executeRpcAndConvert("rabbit_channel", "info_all", new ErlangArgs(null, RabbitChannel.class));
    }

    public List<String> listUsers() {
        return (List<String>) customErlangTemplate.executeRpcAndConvert("rabbit_access_control", "list_users", new ErlangArgs(null, String.class));
    }

    public boolean getStatus() {
        String status = customErlangTemplate.executeRpc("rabbit_mnesia", "status").toString();
        return status.contains(peerNodeName);
    }

    public String getPeerNodeName() {
        return peerNodeName;
    }

    public boolean virtualHostAvailable(String virtualHost, String node) {
        Assert.notNull(virtualHost, "'virtualHost' must not be null");
        Assert.notNull(node, "'node' must not be null");

        List<String> vhosts = null;
        try {
            vhosts = getVirtualHosts();
        } catch (PluginException e) {
            logger.error(e);
        }

        return getStatus() && vhosts != null && vhosts.contains(virtualHost);
    }
}

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

import com.rabbitmq.client.Connection;
import java.io.IOException;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.erlang.core.Node;
import org.springframework.util.Assert;

import java.util.List;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;

/**
 * A HypericRabbitAdmin is created for each node/virtualHost.
 * HypericRabbitAdmin
 * @author Helena Edelson
 */
public class HypericRabbitAdmin extends RabbitBrokerAdmin implements DisposableBean {

    private HypericErlangTemplate customErlangTemplate;

    private SingleConnectionFactory otpConnectionFactory;

    private String virtualHost;

    private String peerNodeName;

    public HypericRabbitAdmin(ConnectionFactory rabbitConnectionFactory, SingleConnectionFactory otpConnectionFactory, String peerNode) {
        super(rabbitConnectionFactory);
        this.peerNodeName = peerNode;
        this.virtualHost = rabbitConnectionFactory.getVirtualHost();
        this.otpConnectionFactory = otpConnectionFactory;

        createErlangTemplate(otpConnectionFactory);
    }

    @Override
    protected void createErlangTemplate(org.springframework.erlang.connection.ConnectionFactory otpCf) {
        super.createErlangTemplate(otpCf);

        this.customErlangTemplate = new HypericErlangTemplate(otpCf);
        this.customErlangTemplate.afterPropertiesSet();
    }

    public void destroy() throws Exception {
        otpConnectionFactory.destroy();
    }

    /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() throws PluginException {
        return (List<String>) customErlangTemplate.executeRpcAndConvert("rabbit_access_control", "list_vhosts", new ErlangArgs(null, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<QueueInfo> getQueues() throws ErlangBadRpcException {
        return (List<QueueInfo>) customErlangTemplate.executeRpcAndConvert("rabbit_amqqueue", "info_all", new ErlangArgs(virtualHost, QueueInfo.class));
    }

    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges() throws ErlangBadRpcException {
        return (List<Exchange>) customErlangTemplate.executeRpcAndConvert("rabbit_exchange", "list", new ErlangArgs(virtualHost, Exchange.class));
    }

    @SuppressWarnings("unchecked")
    public List<RabbitBinding> getBindings() throws ErlangBadRpcException {
        return (List<RabbitBinding>) customErlangTemplate.executeRpcAndConvert("rabbit_exchange", "list_bindings", new ErlangArgs(virtualHost, RabbitBinding.class));
    }

    @SuppressWarnings("unchecked")
    public List<RabbitConnection> getConnections() throws ErlangBadRpcException {
        return (List<RabbitConnection>) customErlangTemplate.executeRpcAndConvert("rabbit_networking", "connection_info_all", new ErlangArgs(null, RabbitConnection.class));
    }

    @SuppressWarnings("unchecked")
    public List<RabbitChannel> getChannels() throws ErlangBadRpcException {
        return (List<RabbitChannel>) customErlangTemplate.executeRpcAndConvert("rabbit_channel", "info_all", new ErlangArgs(null, RabbitChannel.class));
    }

    /**
     * Get broker data
     * @return
     * @throws ErlangBadRpcException
     */
    public String getVersion() throws ErlangBadRpcException {
        return (String) customErlangTemplate.executeRpcAndConvert("rabbit", "status", new ErlangArgs(null, null));
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public String getPeerNodeName() {
        return peerNodeName;
    }


    public boolean nodeAvailable(String nodeName) {
        Assert.notNull(nodeName, "'node' must not be null");
        List<Node> runningNodes = getStatus().getRunningNodes();
        if (runningNodes != null) {
            for (Node node : runningNodes) {
                if (node.toString().contains(nodeName)) {
                    return true;
                }
            }
        }

        return false;
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

        return nodeAvailable(node) && vhosts != null && vhosts.contains(virtualHost);
    }


    public RabbitVirtualHost buildRabbitVirtualHost() {
        VirtualHostBuilder builder = new VirtualHostBuilder();
        return builder.build();
    }

    private class VirtualHostBuilder {

        public RabbitVirtualHost build() {
            RabbitVirtualHost vHost = new RabbitVirtualHost(virtualHost, peerNodeName);
            vHost.setChannels(getChannels());
            vHost.setConnectionCount(getConnections());
            vHost.setAvailable(virtualHostAvailable(virtualHost, peerNodeName));
            vHost.setQueueCount(getQueues());
            vHost.setExchangeCount(getExchanges());
            vHost.setUsers(listUsers());
            return vHost;
        }
    }

}

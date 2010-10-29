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

import com.ericsson.otp.erlang.OtpErlangList;
import org.hyperic.hq.plugin.rabbitmq.validate.ConfigurationValidator;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.connection.SingleConnectionFactory;

import java.util.List;

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

    /* *//**
     * Constructor uses the Node's cookie and the node name such as:
     * 'rabbit_1' from where 'rabbit_1' refers to 'rabbit_1@vmhost'
     * @param connectionFactory
     * @param erlangCookie
     *//*
    public HypericRabbitAdmin(ConnectionFactory connectionFactory, String erlangCookie, String peerNodeName) {
        super(connectionFactory);

        initializeDefaultErlangTemplate(new RabbitTemplate(connectionFactory), erlangCookie, peerNodeName);
    }

    */

    /**
     * Uses the Node's cookie to create ConnectionFactory
     * Note: from Hyperic this can be rabbit_3@vmhost
     * Before: String peerNodeName = "rabbit@" + host
     * @param template
     * @param erlangCookie
     * @param peerNodeName
     *//*
    public void initializeDefaultErlangTemplate(RabbitTemplate template, String erlangCookie, String peerNodeName) {
        final String validatedPeerNodeName = ConfigurationValidator.validatePeerNodeName(template.getConnectionFactory().getHost(), peerNodeName);
        logger.debug("Using peer node name: " + validatedPeerNodeName);

        SingleConnectionFactory otpCf = new SingleConnectionFactory("rabbit-monitor", erlangCookie, validatedPeerNodeName);
        otpCf.afterPropertiesSet();
        createErlangTemplate(otpCf);
    }*/
    public boolean isNodeAvailable() {
        Object response = customErlangTemplate.executeRpc("erlang", "date");
        logger.debug("Response=" + response);
        return response != null;
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

    public RabbitVirtualHost getRabbitVirtualHost(String vHost) {
        RabbitVirtualHost virtualHost = new RabbitVirtualHost();
        virtualHost.setName(vHost);
        virtualHost.setNode(this.peerNodeName);
        virtualHost.setConnections(getConnections());
        virtualHost.setChannels(getChannels());
        return virtualHost;
    }
}

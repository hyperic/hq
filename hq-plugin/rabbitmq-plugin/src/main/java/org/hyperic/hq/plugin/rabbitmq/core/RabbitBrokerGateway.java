/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.product.PluginException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.Application;
import org.springframework.erlang.core.Node;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * RabbitPluginGateway
 * @author Helena Edelson
 */
public class RabbitBrokerGateway implements RabbitGateway {

    private static final Log logger = LogFactory.getLog(RabbitBrokerGateway.class);

    private CachingConnectionFactory ccf;

    private RabbitTemplate rabbitTemplate;
 
    private RabbitBrokerAdmin rabbitBrokerAdmin;

    private HypericErlangConverter erlangConverter;

    public RabbitBrokerGateway(Configuration configuration) {
        initialize(configuration);
    }

    public void initialize(Configuration configuration) {
        this.ccf = new CachingConnectionFactory(configuration.getHostname());
        this.ccf.setUsername(configuration.getUsername());
        this.ccf.setPassword(configuration.getPassword());
        this.ccf.setChannelCacheSize(10);
        this.ccf.setVirtualHost(configuration.getVirtualHost());

        this.rabbitTemplate = new RabbitTemplate(this.ccf);
        this.rabbitBrokerAdmin = new HypericBrokerAdmin(this.ccf, configuration.getAuthentication(), configuration.getNodename()); 
        this.erlangConverter = new HypericErlangControlConverter(this.rabbitBrokerAdmin.getErlangTemplate());
    }

    public boolean isValidUsernamePassword() {
        return true;
        /*Connection con = null;
        try {
            con = ccf.createConnection();
            return true;
        } catch (IOException e) {
            return false;
        }  finally {
            if (con != null) try {
                con.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }*/
    }

    /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() throws PluginException {
        return (List<String>) erlangConverter.fromErlangRpc("rabbit_access_control", "list_vhosts", null, String.class);
    }
      
    @SuppressWarnings("unchecked")
    public List<QueueInfo> getQueues() throws ErlangBadRpcException {
        return (List<QueueInfo>) erlangConverter.fromErlangRpc("rabbit_amqqueue", "info_all", ccf.getVirtualHost(), QueueInfo.class);
    }

    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges() throws ErlangBadRpcException {
        return (List<Exchange>) erlangConverter.fromErlangRpc("rabbit_exchange", "list", ccf.getVirtualHost(), Exchange.class);
    }

    @SuppressWarnings("unchecked")
    public List<RabbitBinding> getBindings() throws ErlangBadRpcException {
        return (List<RabbitBinding>) erlangConverter.fromErlangRpc("rabbit_exchange", "list_bindings", ccf.getVirtualHost(), RabbitBinding.class);
    }

    /**
     * Get <connectioninfoitem> must be a member of the list [pid, address, port,
     * peer_address, peer_port, state, channels, user, vhost, timeout, frame_max,
     * client_properties, recv_oct, recv_cnt, send_oct, send_cnt, send_pend]
     * @return
     * @throws com.ericsson.otp.erlang.OtpErlangException
     *
     */
    @SuppressWarnings("unchecked")
    public List<RabbitConnection> getConnections() throws ErlangBadRpcException {
        return (List<RabbitConnection>) erlangConverter.fromErlangRpc("rabbit_networking", "connection_info_all", null, RabbitConnection.class);
    }

    @SuppressWarnings("unchecked")
    public List<RabbitChannel> getChannels() throws ErlangBadRpcException {
        return (List<RabbitChannel>) erlangConverter.fromErlangRpc("rabbit_channel", "info_all", null, RabbitChannel.class);
    }

    /**
     * Get broker data
     * @return
     * @throws ErlangBadRpcException
     */
    public String getVersion() throws ErlangBadRpcException {
        return (String) erlangConverter.fromErlangRpc("rabbit", "status", null, null);
    }

    /**
     * Get a list of users.
     * @return
     */
    public List<String> getUsers() {
        return rabbitBrokerAdmin.listUsers();
    }

    /**
     * Get RabbitStatus object.
     * @return
     */
    public RabbitStatus getStatus() {
        return rabbitBrokerAdmin.getStatus();
    }

    /**
     * Get a list of running broker apps.
     * @return
     */
    public List<Application> getRunningApplications() {
        return getStatus().getRunningApplications();
    }

    /**
     * Get a list of all nodes.
     * @return
     */
    public List<Node> getNodes() {
        return getStatus().getNodes();
    }

    /**
     * Get a list of running nodes.
     * @return
     */
    public List<Node> getRunningNodes() {
        return getStatus().getRunningNodes();
    }

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    public RabbitBrokerAdmin getRabbitBrokerAdmin() {
        return rabbitBrokerAdmin;
    }
}

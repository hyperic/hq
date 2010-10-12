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

import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.*;
import org.springframework.erlang.core.Application;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * RabbitPluginGateway
 * @author Helena Edelson
 */
public class RabbitBrokerGateway implements RabbitGateway {

    private static final Log logger = LogFactory.getLog(RabbitBrokerGateway.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitBrokerAdmin rabbitBrokerAdmin;

    @Autowired
    private ErlangConverter erlangConverter;

    @PostConstruct
    public void initialize() {
        Assert.notNull(rabbitTemplate, "rabbitTemplate must not be null");
        Assert.notNull(rabbitBrokerAdmin, "rabbitBrokerAdmin must not be null");
        Assert.notNull(erlangConverter, "erlangConverter must not be null");
    }

    @SuppressWarnings("unchecked")
    public List<QueueInfo> getQueues(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        return (List<QueueInfo>) erlangConverter.fromErlangRpc("rabbit_amqqueue", "info_all", virtualHost, QueueInfo.class);
    }

    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        return (List<Exchange>) erlangConverter.fromErlangRpc("rabbit_exchange", "list", virtualHost, Exchange.class);
    }

    @SuppressWarnings("unchecked")
    public List<HypericBinding> getBindings(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        return (List<HypericBinding>) erlangConverter.fromErlangRpc("rabbit_exchange", "list_bindings", virtualHost, HypericBinding.class);
    }
    
    /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() throws ErlangBadRpcException {
        return (List<String>) erlangConverter.fromErlangRpc("rabbit_access_control", "list_vhosts", null, String.class);
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
    public List<HypericConnection> getConnections(String virtualHost) throws ErlangBadRpcException {
        return (List<HypericConnection>) erlangConverter.fromErlangRpc("rabbit_networking", "connection_info_all", null, HypericConnection.class);
    }

    @SuppressWarnings("unchecked")
    public List<HypericChannel> getChannels(String virtualHost) throws ErlangBadRpcException {
        return (List<HypericChannel>) erlangConverter.fromErlangRpc("rabbit_channel", "info_all", null, HypericChannel.class);
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
    public List<String> getUsers(String virtualHost) {
        return rabbitBrokerAdmin.listUsers();
    }

    public String getHost() {
        return rabbitTemplate.getConnectionFactory().getHost();
    }

    /**
     * Get the RabbitMQ server version.
     * @return
     */
    public String getServerVersion() {
        return getRabbitStatus().getRunningApplications().get(0).getVersion();
    }


    /**
     * Get RabbitStatus object.
     * @return
     */
    public RabbitStatus getRabbitStatus() {
        return rabbitBrokerAdmin.getStatus();
    }

    /**
     * Get a list of running broker apps.
     * @return
     */
    public List<Application> getRunningApplications() {
        return getRabbitStatus().getRunningApplications();
    }

    /**
     * Get a list of all nodes.
     * @return
     */
    public List<Node> getNodes() {
        return getRabbitStatus().getNodes();
    }

    /**
     * Get a list of running nodes.
     * @return
     */
    public List<Node> getRunningNodes() {
        return getRabbitStatus().getRunningNodes();
    }


}

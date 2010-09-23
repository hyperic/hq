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

import com.ericsson.otp.erlang.*;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.erlang.ErlangBadRpcException;

import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

/**
 * ErlangBrokerGateway provides functionality *not*
 * in the Spring RabbitBrokerAdmin api where we have
 * to use jinterface directly.
 *
 * @author Helena Edelson
 */
public class ErlangBrokerGateway implements ErlangGateway {

    @Autowired
    private ErlangConverter converter;

     /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() throws ErlangBadRpcException {
        return (List<String>) converter.fromErlangRpc("rabbit_access_control", "list_vhosts", null, String.class);
    }
 
    /**
     * Get <connectioninfoitem> must be a member of the list [pid, address, port,
     * peer_address, peer_port, state, channels, user, vhost, timeout, frame_max,
     * client_properties, recv_oct, recv_cnt, send_oct, send_cnt, send_pend] 
     * @return
     * @throws OtpErlangException
     */
    @SuppressWarnings("unchecked") 
    public List<Connection> getConnections() throws ErlangBadRpcException {
        return (List<Connection>) converter.fromErlangRpc("rabbit_networking", "connection_info_all", null, Connection.class);
    }

    /**
     * Get broker data 
     * @return
     * @throws ErlangBadRpcException
     */
    public String getVersion() throws ErlangBadRpcException {
        return (String) converter.fromErlangRpc("rabbit", "status", null, null);
    }

    /**
     * Not started yet.
     * @param virtualHost
     * @throws OtpErlangException
     */
    public void clusterNodes(String virtualHost) throws OtpErlangException {
        Assert.hasText(virtualHost);
        converter.fromErlangRpc("rabbit_mnesia", "cluster", virtualHost, Exchange.class);
    }
    
    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        return (List<Exchange>) converter.fromErlangRpc("rabbit_exchange", "list", virtualHost, Exchange.class);
    }

    @SuppressWarnings("unchecked")
    public List<Binding> getBindings(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        return (List<Binding>) converter.fromErlangRpc("rabbit_exchange", "list_bindings", virtualHost, Binding.class);
    }

    @SuppressWarnings("unchecked")
    public List<Channel> getChannels() throws ErlangBadRpcException { 
        return (List<Channel>) converter.fromErlangRpc("rabbit_channel", "info_all", null, Channel.class);
    }
 
}

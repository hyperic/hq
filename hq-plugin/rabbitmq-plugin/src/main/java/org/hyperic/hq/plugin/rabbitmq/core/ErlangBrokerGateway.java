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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.ConnectionCallback;
import org.springframework.erlang.core.ErlangTemplate;
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

    private static final Log logger = LogFactory.getLog(ErlangBrokerGateway.class);

    private AMQPConverter converter = new AMQPErlangConverter();

    private ErlangTemplate erlangTemplate;
 
    public ErlangBrokerGateway(ErlangTemplate erlangTemplate) {
        this.erlangTemplate = erlangTemplate;
    }

    /**
     * @param module
     * @param function
     * @return
     * @throws Exception
     */
    public OtpErlangObject doInErlang(final String module, final String function, String virtualHost) {
        final OtpErlangObject[] args = generateArgs(virtualHost);

        OtpErlangObject response = (OtpErlangObject) erlangTemplate.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(OtpConnection connection) throws IOException, OtpErlangExit, OtpAuthException {
                connection.sendRPC(module, function, new OtpErlangList(args));
                return connection.receiveRPC();
            }
        });
         
        if (response.toString().startsWith("{badrpc")) {
            throw new ErlangBadRpcException(response.toString());
        }

        return response;
    }

    /**
     * Handle both null and not null equally-valid input to generate the array.
     * @param virtualHost
     * @return
     */    
    private OtpErlangObject[] generateArgs(String virtualHost) {
        return virtualHost == null ? new OtpErlangObject[]{} : new OtpErlangObject[]{new OtpErlangBinary(virtualHost.getBytes())};
    }

     /**
     * Get a List of virtual hosts.
     * @return List of String representations of virtual hosts
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualHosts() throws ErlangBadRpcException {
        OtpErlangObject response = doInErlang("rabbit_channel", "info_all", null);
        return (List<String>) converter.convert(response, null, String.class);
    }

    /**
     * Get <connectioninfoitem> must be a member of the list [pid, address, port,
     * peer_address, peer_port, state, channels, user, vhost, timeout, frame_max,
     * client_properties, recv_oct, recv_cnt, send_oct, send_cnt, send_pend] 
     * @return
     * @throws OtpErlangException
     */
    @SuppressWarnings("unchecked") 
    public List<AmqpConnection> getConnections() throws ErlangBadRpcException {
        OtpErlangObject response = doInErlang("rabbit_networking", "connection_info_all", null);
        return (List<AmqpConnection>) converter.convert(response, null, Connection.class);
    }

    /**
     * Get broker data 
     * @return
     * @throws ErlangBadRpcException
     */
    public String getVersion() throws ErlangBadRpcException {
        OtpErlangObject response = doInErlang("rabbit", "status", null);
        return converter.convertVersion(response);
    }

    /**
     * Not started yet. "rabbit_mnesia", "cluster"
     * @param clusterNodes
     * @throws OtpErlangException
     */
    public void clusterNodes(Object clusterNodes) throws OtpErlangException {
       
    }
    
    @SuppressWarnings("unchecked")
    public List<Exchange> getExchanges(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        OtpErlangObject response = doInErlang("rabbit_exchange", "list", virtualHost);
        return (List<Exchange>) converter.convert(response, virtualHost, Exchange.class);
    }

    @SuppressWarnings("unchecked")
    public List<Binding> getBindings(String virtualHost) throws ErlangBadRpcException {
        Assert.hasText(virtualHost);
        OtpErlangObject response = doInErlang("rabbit_exchange", "list_bindings", virtualHost);
        return (List<Binding>) converter.convert(response, virtualHost, Binding.class);
    }

    @SuppressWarnings("unchecked")
    public List<AmqpChannel> getChannels() throws ErlangBadRpcException {
        OtpErlangObject response = doInErlang("rabbit_channel", "info_all", null);
        return (List<AmqpChannel>) converter.convert(response, null, Channel.class);
    }
 
}

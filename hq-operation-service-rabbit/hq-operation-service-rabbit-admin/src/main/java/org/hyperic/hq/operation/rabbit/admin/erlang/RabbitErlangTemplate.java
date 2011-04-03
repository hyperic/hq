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
package org.hyperic.hq.operation.rabbit.admin.erlang;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.operation.rabbit.admin.ConnectionCallback;
import org.hyperic.hq.operation.rabbit.admin.RpcException;
import org.springframework.erlang.connection.SimpleConnectionFactory;
import org.springframework.util.Assert;

import org.springframework.erlang.connection.Connection;
import org.springframework.erlang.connection.ConnectionFactory;


import com.ericsson.otp.erlang.*;

import java.net.InetAddress;
import java.net.UnknownHostException; 

/**
 * @author Helena Edelson
 */
public class RabbitErlangTemplate implements ErlangTemplate {

    private final Log logger = LogFactory.getLog(RabbitErlangTemplate.class);

    private volatile ErlangConverter converter = new SimpleErlangConverter();

    private SimpleConnectionFactory connectionFactory;

    private static String DEFAULT_NODE_NAME;

    static {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            DEFAULT_NODE_NAME = "rabbit@" + hostName;
        } catch (UnknownHostException e) {
            DEFAULT_NODE_NAME = "rabbit@localhost";
        }
    }

    public RabbitErlangTemplate() {
        this(null);
    }

    public RabbitErlangTemplate(String nodeName) {
        this.connectionFactory = new SimpleConnectionFactory("rabbit-monitor", nodeName != null ? nodeName : DEFAULT_NODE_NAME);
        this.connectionFactory.afterPropertiesSet();
        Assert.notNull(connectionFactory, "'connectionFactory' must not be null.");
    }

    /**
     * Converts the java args to Erlang for RPC operations and converts
     * the response Erlang values back to Java.
     * @param action
     * @param args
     * @return
     * @throws Exception
     */
    public Object executeAndConvertRpc(final ControlAction action, final Object... args) {
        return execute(new ConnectionCallback<Object>() {
            public Object doInConnection(Connection connection) throws Exception {
                connection.sendRPC(action.getModule(), action.getFunction(), (OtpErlangList) converter.toErlang(args));
                OtpErlangObject response = connection.receiveRPC();
                return handleResponse(action, response);
            }
        });
    }

    public Object handleResponse(ControlAction action, OtpErlangObject response) throws RpcException {
        if (response.toString().startsWith("{badrpc")) {
            throw new RpcException(response.toString());
        } else {
            return getConverter(action).fromErlang(response);
        }

    }

    private ErlangConverter getConverter(ControlAction action) {
        return action.getConverter() != null ? action.getConverter() : converter;
    }

    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    public <T> T execute(ConnectionCallback<T> action) {
        Assert.notNull(action, "Callback object must not be null");
        Connection con = null;
        try {
            con = getConnectionFactory().createConnection();
            return action.doInConnection(con);
        }
        catch (Exception e) {
            logger.error("Unable to create Erlang connection");
        }
        finally {
            releaseConnection(con);
        }
        return null;
    }


    private void releaseConnection(Connection con) {
        if (con == null) return;
        try {
            con.close();
        }
        catch (Throwable ex) {
            logger.debug("Could not close Otp Connection", ex);
        }
    }

}

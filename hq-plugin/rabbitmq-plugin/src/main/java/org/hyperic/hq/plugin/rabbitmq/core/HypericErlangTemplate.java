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

import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.OtpException;
import org.springframework.erlang.connection.Connection;
import org.springframework.erlang.connection.ConnectionFactory;
import org.springframework.erlang.core.ConnectionCallback;
import org.springframework.erlang.core.ErlangTemplate;
import com.ericsson.otp.erlang.*;
import org.springframework.util.Assert;


/**
 * HypericErlangTemplate
 * @author Helena Edelson
 */
public class HypericErlangTemplate extends ErlangTemplate {

    private volatile HypericErlangConverter converter = new HypericErlangControlConverter();

    public HypericErlangTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public OtpErlangObject[] getArgs(String virtualHost) {
        return virtualHost == null ? new OtpErlangObject[]{} : new OtpErlangObject[]{converter.toErlang(virtualHost)};
    }

    public Object executeRpc(final String module, final String function) throws OtpException {
        return this.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(org.springframework.erlang.connection.Connection connection) throws Exception {
                connection.sendRPC(module, function, new OtpErlangList());
                return connection.receiveRPC();
            }
        });
    }

    public Object executeRpcAndConvert(final String module, final String function, final ErlangArgs args) throws OtpException {
        OtpErlangObject response = (OtpErlangObject) this.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(org.springframework.erlang.connection.Connection connection) throws Exception {
                connection.sendRPC(module, function, new OtpErlangList(getArgs(args.getVirtualHost())));
                return connection.receiveRPC();
            }
        });

        if (response.toString().startsWith("{badrpc")) {
            throw new ErlangBadRpcException(response.toString());
        }

        return converter.fromErlangRpc(response, args);
    }

	public <T> T execute(ConnectionCallback<T> action) throws OtpException {
		Assert.notNull(action, "Callback object must not be null");
		Connection con = null;
		try {
			con = createConnection();
			return action.doInConnection(con);
		}
		catch (OtpException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw convertOtpAccessException(ex);
		}
		finally {
            logger.debug("Closing erlang connection " + con);
			org.springframework.erlang.connection.ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
		}

	}

}

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


import java.io.IOException;
import java.net.UnknownHostException;

import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.erlang.OtpIOException;
import org.springframework.erlang.connection.ConnectionFactory;
import org.springframework.util.Assert;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;

/**
 * RabbitConnectionFactory
 * @author Helena Edelson
 */
public class RabbitConnectionFactory extends SingleConnectionFactory {

	private String selfNodeName;

	private String cookie;

	private String peerNodeName;

	private OtpSelf otpSelf;

	private OtpPeer otpPeer;

	public RabbitConnectionFactory(String selfNodeName, String peerNodeName) {
		this.selfNodeName = selfNodeName;
		this.peerNodeName = peerNodeName;
	}

    public com.rabbitmq.client.Connection createConnection() throws java.io.IOException {
        return super.createConnection();
    }

	/*public OtpConnection createConnection() throws OtpAuthException, IOException {
		try {
			return otpSelf.connect(otpPeer);
		}
		catch (IOException ex) {
			throw new OtpIOException("failed to connect from '" + this.selfNodeName
					+ "' to peer node '" + this.peerNodeName + "'", ex);
		}
	}
*/
	public void afterPropertiesSet() {
		Assert.isTrue(this.selfNodeName != null || this.peerNodeName != null,
				"'selfNodeName' or 'peerNodeName' is required");
		try {
			if (this.cookie == null) {
				this.otpSelf = new OtpSelf(this.selfNodeName);
			}
			else {
				this.otpSelf = new OtpSelf(this.selfNodeName, this.cookie);
			}
		}
		catch (IOException e) {
			throw new OtpIOException(e);
		}
		this.otpPeer = new OtpPeer(this.peerNodeName);
	}

}

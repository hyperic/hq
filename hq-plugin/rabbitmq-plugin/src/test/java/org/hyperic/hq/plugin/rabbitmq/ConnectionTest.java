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
package org.hyperic.hq.plugin.rabbitmq;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * ConnectionTest tests the underlying jinterface connection
 * @author Helena Edelson
 */
public class ConnectionTest {

    private static final String NODE = "rabbit@localhost";

    public static void main(String[] args) throws IOException, OtpAuthException {
        /** if the node's cookie is not locatable use this constructor with the cookie value */
        //new OtpSelf("rabbit-monitor", cookieString);

        OtpSelf self = new OtpSelf("rabbit-monitor");
        OtpPeer peer = new OtpPeer(NODE);
        OtpConnection conn = self.connect(peer);

        Assert.notNull(conn);
        Assert.state(conn.isAlive());
        Assert.state(conn.isConnected());
        Assert.isTrue(conn.getState().name().equalsIgnoreCase("RUNNABLE"), "Connection must be runnable.");    
    }
}

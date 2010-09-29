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
package org.hyperic.hq.plugin.rabbitmq.configure;

import com.ericsson.otp.erlang.*;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.util.Assert;
 
import java.io.IOException;

/**
 * ConnectionTest tests the underlying jinterface connection
 * @author Helena Edelson
 */
public class ConnectionTest {

    private static final String NODE = "localhost";

       public static void main(String[] args) throws IOException, OtpAuthException, OtpErlangExit {
           ConfigResponse conf = new ConfigResponse();
           conf.setValue(DetectorConstants.HOST, NODE);
           conf.setValue(DetectorConstants.USERNAME, "guest");
           conf.setValue(DetectorConstants.PASSWORD, "guest");
           conf.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");

           String value = RabbitUtils.configureCookie(conf);
           conf.setValue(DetectorConstants.NODE_COOKIE_VALUE, value);


           OtpSelf self = new OtpSelf("rabbit-spring-monitor", value);
           OtpPeer peer = new OtpPeer("rabbit@"+NODE);
           //peer.setCookie(value);
           OtpConnection conn = self.connect(peer);

           conn.sendRPC("erlang","date",new OtpErlangList());
           OtpErlangObject received = conn.receiveRPC();
           Assert.notNull(received);


           Assert.notNull(conn);
           Assert.state(conn.isAlive());
           Assert.state(conn.isConnected());
           Assert.isTrue(conn.getState().name().equalsIgnoreCase("RUNNABLE"), "Connection must be runnable.");

           conn.close();
       }


}

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
package org.hyperic.hq.plugin.rabbitmq.validate;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpErlangExit;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.core.HypericBrokerAdmin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.erlang.core.ErlangTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PermissionsTest {

    protected static final Log logger = LogFactory.getLog(PermissionsTest.class);

    private static final String FULLY_QUALIFIED_HOST = "localhost.foo.com";

    private static final String NODE_FROM_PTQL = "rabbit@hostname";


    public static void main(String[] args) throws IOException, OtpAuthException, OtpErlangExit, PluginException {
        List<QueueInfo> queues = new ArrayList<QueueInfo>();

        try {
            /** assert Spring does not handle this for us */
            logger.debug("Testing Spring RabbitBrokerAdmin and " + FULLY_QUALIFIED_HOST + " as host...");
            org.springframework.amqp.rabbit.connection.SingleConnectionFactory scf = new org.springframework.amqp.rabbit.connection.SingleConnectionFactory(FULLY_QUALIFIED_HOST);
            scf.setUsername("guest");
            scf.setPassword("guest");
            RabbitBrokerAdmin admin = new RabbitBrokerAdmin(scf);
            queues = admin.getQueues();
        }
        catch (Exception e) {
            assertTrue(e instanceof org.springframework.erlang.OtpIOException);
            logger.debug("Anticipated 'java.net.UnknownHostException: " + FULLY_QUALIFIED_HOST + "'\n" + e);
        }

        String hostFromPS = getHostFromNode(NODE_FROM_PTQL);

        /* fully qualified does not work. have to parse. However, in the plugin we get the node name from ps. */
        org.springframework.amqp.rabbit.connection.SingleConnectionFactory cf =
                new org.springframework.amqp.rabbit.connection.SingleConnectionFactory(hostFromPS);
        cf.setUsername("guest");
        cf.setPassword("guest");

        try {
            logger.debug("\nTesting Spring RabbitBrokerAdmin and " + hostFromPS + " as host...no cookie");
            RabbitBrokerAdmin admin = new RabbitBrokerAdmin(cf);
            queues = admin.getQueues();
            logger.debug(queues.size());
            logger.debug("Test Spring RabbitBrokerAdmin and " + hostFromPS + " as host no cookie successful.");
        }
        catch (Exception e) {
            assertTrue(e instanceof org.springframework.erlang.OtpAuthException);
            logger.debug("Anticipated 'java.net.SocketException: Connection reset or java.io.IOException: expected 2 bytes, got EOF after 0 bytes\n'" + e);
        }

        try {
            logger.debug("Testing HypericBrokerAdmin (extends RBA) with cookie...");
            String c = ErlangCookieHandler.configureCookie(getConfigResponse(NODE_FROM_PTQL));
            RabbitBrokerAdmin admin = new HypericBrokerAdmin(cf, c);
            queues = admin.getQueues();
            assertTrue("Queues using cookie: ", queues.size() > 0);
            logger.debug("Testing HypericBrokerAdmin (extends RBA) with cookie successful.");


            ErlangTemplate template = admin.getErlangTemplate();
            org.springframework.erlang.connection.ConnectionFactory factory = template.getConnectionFactory();
            assertTrue(factory instanceof org.springframework.erlang.connection.SingleConnectionFactory);

            org.springframework.erlang.connection.Connection conn = factory.createConnection();
            assertNotNull(conn);
            conn.close();
        } catch (org.springframework.erlang.OtpAuthException e) {
            logger.debug(e);
        }

    }

    private static ConfigResponse getConfigResponse(String node) {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, node);
        conf.setValue(DetectorConstants.USERNAME, "guest");
        conf.setValue(DetectorConstants.PASSWORD, "guest");
        conf.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
        return conf;
    }

    private static String getHostFromNode(String nodeName) {
        if (nodeName != null) {
            Pattern p = Pattern.compile("@([^\\s.]+)");
            Matcher m = p.matcher(nodeName);
            return (m.find()) ? m.group(1) : null;
        }
        return null;
    }
}

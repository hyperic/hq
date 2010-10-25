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
import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.hyperic.hq.plugin.rabbitmq.configure.PluginContextCreator;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.core.HypericBrokerAdmin;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.erlang.connection.Connection;
import org.springframework.erlang.core.ErlangTemplate;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


/**
 * ValidationTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class ValidationTest extends AbstractSpringTest {

    private String fullyQualifiedHost = configuration.getHostname() + ".dev.com";

    @Test
    @ExpectedException(IllegalArgumentException.class)
    public void noHost() throws PluginException {
        configResponse.setValue(DetectorConstants.HOST, null);
        PluginContextCreator.createContext(Configuration.toConfiguration(configResponse));
        RabbitGateway rabbitGateway = PluginContextCreator.getBean(RabbitGateway.class);
        assertNotNull(rabbitGateway);
    }

    @Test
    public void pluginInitializationAssertSuccess() throws PluginException {
        /** in the plugin the erlang cookie value is set during creation
         * of the ServerResource and the value set in the productConfig.
         */
        configResponse.setValue(DetectorConstants.AUTHENTICATION, ErlangCookieHandler.configureCookie(configResponse));
        RabbitProductPlugin.initialize(Configuration.toConfiguration(configResponse));
        assertNotNull(RabbitProductPlugin.getRabbitGateway());
    }

    @Test
    public void hello() throws IOException, OtpAuthException, OtpErlangExit, PluginException {
        List<QueueInfo> queues = new ArrayList<QueueInfo>();

        try {
            /** assert Spring does not handle this for us */
            SingleConnectionFactory scf = new SingleConnectionFactory(fullyQualifiedHost);
            scf.setUsername(configuration.getUsername());
            scf.setPassword(configuration.getPassword());
            RabbitBrokerAdmin admin = new RabbitBrokerAdmin(scf);
            queues = admin.getQueues();
        }
        catch (Exception e) {
            assertTrue(e instanceof org.springframework.erlang.OtpIOException);
            logger.debug("Anticipated 'java.net.UnknownHostException: " + configuration.getHostname() + "'\n" + e);
        }

        /* fully qualified does not work. have to parse. However, in the plugin we get the node name from ps. */
        SingleConnectionFactory cf = new SingleConnectionFactory(configuration.getHostname());
        cf.setUsername(configuration.getUsername());
        cf.setPassword(configuration.getPassword());

        try {
            logger.debug("\nTesting Spring RabbitBrokerAdmin and " + configuration.getHostname() + " as host...no cookie");
            RabbitBrokerAdmin admin = new RabbitBrokerAdmin(cf);
            queues = admin.getQueues();
            logger.debug(queues.size());
            logger.debug("Test Spring RabbitBrokerAdmin and " + configuration.getHostname() + " as host no cookie successful.");
        }
        catch (Exception e) {
            assertTrue(e instanceof org.springframework.erlang.OtpAuthException);
            logger.debug("Anticipated 'java.net.SocketException: Connection reset or java.io.IOException: expected 2 bytes, got EOF after 0 bytes\n'" + e);
        }

        try {
            logger.debug("Testing HypericBrokerAdmin (extends RBA) with cookie...");
            String auth = ErlangCookieHandler.configureCookie(configResponse);
            RabbitBrokerAdmin admin = new HypericBrokerAdmin(cf, auth, configuration.getNodename());
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

}

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
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitConnection; 
import org.hyperic.hq.product.PluginException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitAdminAuthException;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.erlang.OtpIOException;
import org.springframework.test.annotation.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


/**
 * ValidationTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class ValidationTest extends AbstractSpringTest {

    private String fullyQualified = ".dev.foo.com";

//    @Test
//    public void duh() throws IOException {
//        com.rabbitmq.client.Connection conn = configurationManager.getConnectionFactory().createConnection();
//        System.out.println("ConnectionProperties = " + conn.getServerProperties());
//
//        conn.createChannel();
//        conn.createChannel();
//
//        HypericRabbitAdmin admin = configurationManager.getVirtualHostForNode(configuration.getDefaultVirtualHost(), configuration.getNodename());
//        admin.getQueues();
//
//        List<RabbitConnection> conns = admin.getConnections();
//        RabbitConnection con = conns.get(0);
//
//        if (conns != null) {
//            for (RabbitConnection c : conns) {
//                if (con.getPid().equalsIgnoreCase(c.getPid())) {
//                    System.out.println("equalsIgnoreCase match on " + con + " and " + c);
//                }
//            }
//        }
//
//        conn.close();
//    }

//    @Test
//    @ExpectedException(PluginException.class)
//    public void isValidUsernamePassword() throws PluginException {
//        assertTrue(ConfigurationValidator.isValidUsernamePassword(configuration));
//
//        configuration.setPassword("invalid");
//        assertFalse(ConfigurationValidator.isValidUsernamePassword(configuration));
//    }

    @Test
    @ExpectedException(PluginException.class)
    public void isValidOtpConnection() throws PluginException {
        assertTrue(ConfigurationValidator.isValidOtpConnection(configuration));
        configuration.setNodename("rabbit@invalid");
        assertFalse(ConfigurationValidator.isValidOtpConnection(configuration));
    }

    @Test
    @ExpectedException(RabbitAdminAuthException.class)
    public void createRabbitBrokerAdmin() {
        RabbitBrokerAdmin admin = new RabbitBrokerAdmin(ccf);
        assertNull(admin.getStatus());
    }

//    @Test
//    @ExpectedException(OtpIOException.class)
//    public void failOnFullyQualifiedHostName() {
//        List<QueueInfo> queues = new ArrayList<QueueInfo>();
//        SingleConnectionFactory scf = new SingleConnectionFactory(configuration.getHostname() + fullyQualified);
//        scf.setUsername(configuration.getUsername());
//        scf.setPassword(configuration.getPassword());
//
//        RabbitBrokerAdmin admin = new RabbitBrokerAdmin(scf);
//        queues = admin.getQueues();
//    }

//    @Test
//    public void failOnAuthNoCookie() throws IOException, OtpAuthException, OtpErlangExit, PluginException {
//        List<QueueInfo> queues = new ArrayList<QueueInfo>();
//        SingleConnectionFactory cf = new SingleConnectionFactory(configuration.getHostname());
//        cf.setUsername(configuration.getUsername());
//        cf.setPassword(configuration.getPassword());
//
//        try {
//            logger.debug("\nTesting Spring RabbitBrokerAdmin and " + configuration.getHostname() + " as host...no cookie");
//            RabbitBrokerAdmin admin = new RabbitBrokerAdmin(cf);
//            queues = admin.getQueues();
//        }
//        catch (Exception e) {
//            assertTrue(e instanceof org.springframework.erlang.OtpAuthException);
//            logger.debug("Anticipated 'java.net.SocketException: Connection reset or java.io.IOException: expected 2 bytes, got EOF after 0 bytes\n'" + e);
//        }
//    }

}

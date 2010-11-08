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

import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.test.annotation.ExpectedException;

import static org.junit.Assert.*;

/**
 * HypericBrokerAdminTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class HypericBrokerAdminTest extends AbstractSpringTest {

    private SingleConnectionFactory otpConnectionFactory;

    @Before
    public void doBefore() throws PluginException {
        this.otpConnectionFactory = new SingleConnectionFactory("rabbit-monitor", configuration.getAuthentication(), configuration.getNodename());
        otpConnectionFactory.afterPropertiesSet();
    }

    @Test
    public void createHypericBrokerAdmin() throws Exception {
        HypericRabbitAdmin admin = configurationManager.getVirtualHostForNode(configuration.getDefaultVirtualHost(), configuration.getNodename());
        assertNotNull(admin.getStatus());
        admin.declareQueue(new Queue("nonDurable"));
        assertNotNull(admin.getQueues());
        org.springframework.erlang.connection.Connection conn = otpConnectionFactory.createConnection();
        assertNotNull(conn);
        conn.close();
        admin.destroy();
    }

    @Test
    @ExpectedException(org.springframework.erlang.OtpAuthException.class)
    public void testSpringAdmin() throws PluginException {
        RabbitBrokerAdmin rba = new RabbitBrokerAdmin(ccf);
        assertNotNull(rba.getQueues());
    }
}

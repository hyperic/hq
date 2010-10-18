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
import org.hyperic.hq.plugin.rabbitmq.configure.ConfigurationManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * ConfigurationManagerTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class ConfigurationManagerTest extends AbstractSpringTest {

    @Autowired
    private ConfigurationManager configurationManager;

    @Before
    public void doBefore(){
        assertNotNull(configurationManager);
    }

    @Test
    public void test() {
        CachingConnectionFactory ccf = configurationManager.getConnectionFactory();
        assertNotNull(ccf);
        assertTrue(ccf.getHost().equalsIgnoreCase(configurationManager.getConnectionFactory().getHost()));
        RabbitStatus status = configurationManager.getRabbitGateway().getRabbitStatus();
        assertNotNull(status);
        assertNotNull(configurationManager.getRabbitTemplate());
        assertNotNull(configurationManager.getRabbitBrokerAdmin());
        assertNotNull(configurationManager.getErlangConverter());

    }

}

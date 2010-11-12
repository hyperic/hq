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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
//import org.hyperic.hq.plugin.rabbitmq.configure.ConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.configure.TestContextLoader;
//import org.hyperic.hq.plugin.rabbitmq.manage.RabbitManager;
import org.hyperic.util.config.ConfigResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * AbstractSpringTest
 * @author Helena Edelson
 */
@ContextConfiguration(loader = TestContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Need to mock the connection for automation")
public abstract class AbstractSpringTest {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());
//
//    @Autowired
//    protected ConfigurationManager configurationManager;

    @Autowired
    protected Configuration configuration;

    @Autowired
    protected CachingConnectionFactory ccf;

    @Autowired
    protected ConfigResponse configResponse;

 /*   @Autowired
    protected RabbitManager rabbitManager;
*/
    @Autowired
    protected Queue stocksQueue;

    @Autowired
    protected Queue alertsQueue;

    @Autowired
    protected Queue trendsQueue;

    @Autowired
    protected List<Queue> queues;

//    @Before
//    public void before() {
//        assertNotNull("configurationManager should not be null", configurationManager);
//        assertNotNull("configuration must not be null", configuration);
//    }

    @AfterClass
    public static void doAfter() {

    }
    
}

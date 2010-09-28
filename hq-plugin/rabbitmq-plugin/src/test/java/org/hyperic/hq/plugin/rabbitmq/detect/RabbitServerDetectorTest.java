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
package org.hyperic.hq.plugin.rabbitmq.detect;

import com.rabbitmq.client.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * RabbitServerDetectorTest
 * @author Helena Edelson
 */
@ContextConfiguration("classpath:/etc/rabbit-test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Need to mock the connection for automation")
public class RabbitServerDetectorTest extends RabbitServerDetector {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitGateway rabbitGateway;

    @Autowired
    private SingleConnectionFactory singleConnectionFactory;

    protected static RabbitServerDetector rabbitServerDetector = new RabbitServerDetector();

    private Properties properties;

    private final String fqdn = "localhost";

    private final String platformName = "localhost";

    private final String platformType = "Linux";


    @Before
    public void doBefore() throws PluginException {
        rabbitServerDetector.configure(createConfigResponse());
        this.properties = rabbitServerDetector.getConfig().toProperties();
        assertNotNull(properties);
    }

    @Test
    public void getChannelsAndConnections() throws Exception, IOException {

        /** Insure we have a connection and at least one channel to test */
        Connection conn = singleConnectionFactory.createConnection();
        conn.createChannel(0);
        conn.createChannel(1);

        int total = rabbitGateway.getChannels().size() + rabbitGateway.getConnections().size() +
                rabbitGateway.getExchanges().size() + rabbitGateway.getUsers().size() +
                rabbitGateway.getQueues().size() + rabbitGateway.getRunningApplications().size();

        final String virtualHost = rabbitGateway.getVirtualHost();
        List<ServiceResource> rabbitResources = new ArrayList<ServiceResource>();

        List<ServiceResource> connections = createConnectionServiceResources(rabbitGateway, virtualHost);
        if (connections != null) rabbitResources.addAll(connections);

        conn.close();
        
        List<ServiceResource> channels = createChannelServiceResources(rabbitGateway, virtualHost);
        if (channels != null) rabbitResources.addAll(channels);

        List<ServiceResource> queues = createQueueServiceResources(rabbitGateway, virtualHost);
        if (queues != null) rabbitResources.addAll(queues);

        List<ServiceResource> exchanges = createExchangeServiceResources(rabbitGateway, virtualHost);
        if (exchanges != null) rabbitResources.addAll(exchanges);

        List<ServiceResource> runningApps = createAppServiceResources(rabbitGateway, virtualHost);
        if (runningApps != null) rabbitResources.addAll(runningApps);

        List<ServiceResource> users = createUserServiceResources(rabbitGateway, virtualHost);
        if (users != null) rabbitResources.addAll(users);


        assertNotNull(rabbitResources);
        assertEquals(total, rabbitResources.size());
    }

    @Test
    /* todo */
    public void getServerResources() throws PluginException {
        rabbitServerDetector.getServerResources(createConfigResponse());
    }

    private ConfigResponse createConfigResponse() {
        ConfigResponse config = new ConfigResponse();
        config.setValue("fqdn", fqdn);
        config.setValue("platform.name", platformName);
        config.setValue("platform.type", platformType);

        return config;
    }

}

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

import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitBrokerGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.core.Exchange;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.junit.Assert.*;

/**
 * ApplicationContextCreatorTest
 * @author Helena Edelson
 */
//@Ignore("Need to mock the connection for automation")
public class RC1Tests {

    private static final String SERVER_NAME = "rabbit@localhost";

    private static final String HOST = "localhost";

    @Test
    public void test() throws PluginException {
        Configuration configuration = Configuration.toConfiguration(getConfig());
        configuration.setVirtualHost("/");

        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway(configuration);
        assertNotNull(rabbitGateway);
        assertTrue(RabbitProductPlugin.isNodeAvailabile(configuration));
        assertTrue(rabbitGateway.getStatus().getNodes().size() > 0);

        boolean valid = rabbitGateway.isValidUsernamePassword();
        assertTrue(valid);
        //System.out.println("isvalid? " + valid);

        List<String> virtualHosts = rabbitGateway.getVirtualHosts();
        assertNotNull(virtualHosts.get(0));

        for (String vhost : virtualHosts) {
            List<String> users = rabbitGateway.getUsers();

            List exchanges = rabbitGateway.getExchanges();
            System.out.println("exchanges=" + exchanges.size());

            List channels = rabbitGateway.getChannels();
            if (channels != null) System.out.println("channels=" + channels.size());

            List connections = rabbitGateway.getConnections();
            if (connections != null) System.out.println("connections=" + connections.size());

            List queues = rabbitGateway.getQueues();
            if (queues != null) System.out.println("queues=" + queues.size());
        }
    }

    private ConfigResponse getConfig() throws PluginException {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, HOST);
        conf.setValue(DetectorConstants.USERNAME, "guest");
        conf.setValue(DetectorConstants.PASSWORD, "guest");
        conf.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
        conf.setValue(DetectorConstants.SERVER_NAME, SERVER_NAME);

        String value = ErlangCookieHandler.configureCookie(conf);
        assertNotNull(value);
        conf.setValue(DetectorConstants.AUTHENTICATION, value);
        return conf;
    }
}

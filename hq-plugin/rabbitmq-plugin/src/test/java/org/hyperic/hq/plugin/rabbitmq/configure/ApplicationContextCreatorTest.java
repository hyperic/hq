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

import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * ApplicationContextCreatorTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class ApplicationContextCreatorTest {

    private static final String HOST = "localhost";

    @Test
    public void create() throws PluginException {
        ConfigResponse serviceConfig = new ConfigResponse();
        serviceConfig.setValue(DetectorConstants.HOST, HOST);
        serviceConfig.setValue(DetectorConstants.USERNAME, "guest");
        serviceConfig.setValue(DetectorConstants.PASSWORD, "guest");
        serviceConfig.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");

        String value = ErlangCookieHandler.configureCookie(serviceConfig);
        assertNotNull(value);
        serviceConfig.setValue(DetectorConstants.AUTHENTICATION, value);

        if (RabbitProductPlugin.getRabbitGateway() == null) {
            RabbitProductPlugin.initialize(Configuration.toConfiguration(serviceConfig));
        }
 
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();

        if (rabbitGateway != null) {
            assertTrue(rabbitGateway.getRabbitStatus().getNodes().size() > 0);
            List<String> virtualHosts = rabbitGateway.getVirtualHosts();
            assertNotNull(virtualHosts.get(0));
        }
    }
}

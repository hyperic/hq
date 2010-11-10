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

import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.ErlangCookieHandler;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.test.annotation.ExpectedException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InitializationTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class InitializationTest extends AbstractSpringTest {

    private String node = "rabbit@vm-host";

//    @Test @ExpectedException(PluginException.class)
//    public void initServerCollector() throws PluginException {
//        Configuration configuration = Configuration.toConfiguration(getConfigResponse());
//        if (!RabbitProductPlugin.isInitialized()) {
//            RabbitProductPlugin.initialize(configuration);
//        }
//    }

//    @Test
//    public void initServerCollectorWithUP() throws PluginException {
//        Configuration configuration = Configuration.toConfiguration(getConfigResponse());
//        configuration.setUsername("guest");
//        configuration.setPassword("guest");
//        if (!RabbitProductPlugin.isInitialized()) {
//            assertTrue(RabbitProductPlugin.initialize(configuration));
//        }
//    }
//
//    @Test
//    public void discoverServicesInit() throws PluginException {
//        Configuration configuration = Configuration.toConfiguration(getConfigResponse());
//        configuration.setUsername("guest");
//        configuration.setPassword("guest");
//        if (!RabbitProductPlugin.isInitialized()) {
//            assertTrue(RabbitProductPlugin.initialize(configuration));
//        }
//    }

    @Test
    public void pluginInitializationAssertSuccess() throws PluginException {
        /** in the plugin the erlang cookie value is set during creation
         * of the ServerResource and the value set in the productConfig.
         */
        configResponse.setValue(DetectorConstants.AUTHENTICATION, ErlangCookieHandler.configureCookie(configResponse));
        /*RabbitProductPlugin.initialize(Configuration.toConfiguration(configResponse));
        assertNotNull(RabbitProductPlugin.getRabbitGateway());*/
    }

    public ConfigResponse getConfigResponse() {
        ConfigResponse conf = new ConfigResponse();
        conf.setValue(DetectorConstants.HOST, getHostFromNode(node));
        conf.setValue(DetectorConstants.SERVER_NAME, node);
        conf.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");
        String value = null;

        try {
            value = ErlangCookieHandler.configureCookie(conf);
        } catch (PluginException e) {
            logger.error("", e);
        }
        conf.setValue(DetectorConstants.AUTHENTICATION, value);
        return conf;
    }

    private String getHostFromNode(String nodeName) {
        if (nodeName != null && nodeName.length() > 0) {
            Pattern p = Pattern.compile("@([^\\s.]+)");
            Matcher m = p.matcher(nodeName);
            return (m.find()) ? m.group(1) : null;
        }
        return null;
    }

}

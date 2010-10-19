/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.product;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.ConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.configure.PluginContextCreator;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitVirtualHost;
import org.hyperic.hq.plugin.rabbitmq.validate.PluginValidator;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.product.*;

import java.util.List;

/**
 * RabbitProductPlugin
 * manager.getProps={user.home=/root, file.encoding=UTF-8, user.name=root, user.language=en, file.separator=/ }
 * @author Helena Edelson
 */
public class RabbitProductPlugin extends ProductPlugin {

    private static final Log logger = LogFactory.getLog(RabbitProductPlugin.class);

    private static ConfigurationManager configurationManager;

    private static RabbitGateway rabbitGateway;

    /**
     * Object could be null if gateway has not been initialized yet.
     * This is dependent on getting values in ConfigResponse from user input
     * in the UI. We have detected the RabbitMQ server if one exists on the host,
     * however we need to initialize before we get services.
     * @return RabbitGateway or null
     */
    public static RabbitGateway getRabbitGateway() {
        return rabbitGateway;
    }

    /**
     * Determine if the node available for the ServerResource Collector
     * @param configuration
     * @return
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static boolean isNodeAvailabile(Configuration configuration) throws PluginException {
        return configuration.isConfiguredOtpConnection() && PluginValidator.isValidOtpConnection(configuration);
    }

    public static List<String> getVirtualHosts(Configuration configuration) throws PluginException {
        return PluginValidator.getVirtualHosts(configuration);
    }

    public static void addVirtualHost(Configuration configuration) throws PluginException {
        if (configurationManager != null) {
            configurationManager.addVirtualHost(configuration);
        }
    }


    /**
     * Called by Collectors which only have Properties.
     * @param configuration
     * @return true if all values are set and valid
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static boolean initialize(Configuration configuration) throws PluginException {
        if (configuration.isConfigured()) {

            PluginContextCreator.createContext(configuration);

            if (PluginContextCreator.isInitialized()) {
                configurationManager = PluginContextCreator.getBean(ConfigurationManager.class);
                rabbitGateway = configurationManager.getRabbitGateway();
                /** ToDo add a valid username/password check. */
                return getRabbitGateway() != null;
            }

        } else {
            logger.debug("Postponing initialization of RabbitMQ metric Services until all required config values are set.");
        }

        return false;
    }

}

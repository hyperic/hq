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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.configure.ConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.validate.ConfigurationValidator;
import org.hyperic.hq.product.*;

import java.util.Map;

/**
 * RabbitProductPlugin
 * manager.getProps={user.home=/root, file.encoding=UTF-8, user.name=root, user.language=en, file.separator=/ }
 * @author Helena Edelson
 */
public class RabbitProductPlugin extends ProductPlugin {

    private static final Log logger = LogFactory.getLog(RabbitProductPlugin.class);

    private static ConfigurationManager configurationManager;

    @Override
    public void init(PluginManager manager) throws PluginException {
      super.init(manager);
      logger.debug(manager.getProperties());
    }
    /**
     * @param configuration
     * @return
     * @throws PluginException
     */
    public static boolean initialize(Configuration configuration) throws PluginException {
        logger.debug("Starting initialization of plugin");
        if (configuration != null) {
             if (configuration.getVirtualHost() == null) {
                configuration.setDefaultVirtualHost(true);
             }

            if (configuration.isConfigured() && isValidUsernamePassword(configuration) && isValidOtpConnection(configuration)) {
                logger.debug("Initializing ConfigurationManager");
                if (configurationManager == null || !configurationManager.isInitialized()) {
                    configurationManager = new RabbitConfigurationManager(configuration);
                }
            }
        }

        boolean initialized = isInitialized();
        logger.debug("Initialized=" + initialized);

        return initialized;
    }

    public static boolean isInitialized() {
        return configurationManager != null && configurationManager.isInitialized();
    }

    public static HypericRabbitAdmin getVirtualHostForNode(String virtualHost, String node) {
         return configurationManager.getVirtualHostForNode(virtualHost, node);
    }

    /**
     * @return
     */
    public static Map<String, HypericRabbitAdmin> getVirtualHostsForNode() {
        return configurationManager.getVirtualHostsForNode();
    }

    /**
     * Determine if the node available for a Collector
     * @param key
     * @return
     * @throws org.hyperic.hq.product.PluginException
     *
     */
    public static boolean isNodeAvailabile(Configuration key) throws PluginException {
        return isValidOtpConnection(key);
    }

    public static boolean isValidOtpConnection(Configuration configuration) throws PluginException {
        return ConfigurationValidator.isValidOtpConnection(configuration);
    }

    public static boolean isValidUsernamePassword(Configuration configuration) throws PluginException {
        return ConfigurationValidator.isValidUsernamePassword(configuration);
    }

}

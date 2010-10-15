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
import org.hyperic.hq.plugin.rabbitmq.configure.PluginContextCreator;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitConfiguration;
import org.hyperic.hq.plugin.rabbitmq.validate.PluginValidator;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.product.*;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.util.Assert;
 

/**
 * RabbitProductPlugin
 * @author Helena Edelson
 */
public class RabbitProductPlugin extends ProductPlugin {

    private static final Log logger = LogFactory.getLog(RabbitProductPlugin.class);

    private static RabbitGateway rabbitGateway;

    private static String authentication;

    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        logger.debug("init manager.getProps=" + manager.getProperties());
        logger.debug("init getConfig="+getConfig());
        //ERLANG_COOKIE_FILE = manager.getProperty(ERLANG_COOKIE_PROP);
        //System.getProperties().setProperty("OtpConnection.trace", "99");
    }

    public static String getAuthentication() {
        return authentication;
    }

    /**
     * Object could be null if gateway has not been initialized yet.
     * This is dependent on getting values in ConfigResponse from user input
     * in the UI. We have detected the RabbitMQ server if one exists on the host,
     * however we need to initialize before we get services and do control actions.
     * @return RabbitGateway or null
     */
    public static RabbitGateway getRabbitGateway() {
        return rabbitGateway;
    }

    /**
     * Intercept bean registration in order to extract ConfigResponse plugin parameters and inject
     * key/value pairs to dynamically create Spring Beans.
     * If context not initialized, since it can not be initialized until we have
     * necessary parameters from the config parameter, initialize it.
     * @param conf ConfigResponse from discoverServices
     */
    public static void createRabbitContext(ConfigResponse conf) throws PluginException {

        if (PluginValidator.isValidConfiguration(conf.toProperties())) {
            logger.debug("Initializing Rabbit context");
            PluginContextCreator.createContext(conf, new Class[]{RabbitConfiguration.class});

            if (PluginContextCreator.isInitialized()) {
                rabbitGateway = PluginContextCreator.getBean(RabbitGateway.class);
                Assert.notNull(rabbitGateway, "rabbitGateway must not be null");
            }
        } else {
            logger.info("Postponing initialization of RabbitMQ metric Services until all required config values are set.");
        }
    }
 
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.configure.RabbitConfigurationManager;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public abstract class RabbitMQDefaultCollector extends Collector {

    private RabbitConfigurationManager configManager;
    private Configuration configuration;

    @Override
    protected final void init() throws PluginException {
        super.init();
        Properties props = getProperties();
        configuration = Configuration.toConfiguration(props);
        if (getLog().isDebugEnabled()) {
            getLog().debug("[init] props=" + props);
            getLog().debug("[init] configuration=" + configuration);
        }
        if (configuration.isConfigured()) {
            configManager = new RabbitConfigurationManager(configuration);
        }
    }

    public final HypericRabbitAdmin getAdmin() {
        return configManager.getVirtualHostForNode(configuration.getVirtualHost(), configuration.getNodename());

    }

    public abstract Log getLog();
}

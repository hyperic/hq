/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.rabbitmq.configure.Configuration;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public abstract class RabbitMQDefaultCollector extends Collector {

    private Configuration configuration;
    private HypericRabbitAdmin admin;
    @Override
    protected final void init() throws PluginException {
        super.init();
        Properties props = getProperties();
        configuration = Configuration.toConfiguration(props);
        if (getLog().isDebugEnabled()) {
            getLog().debug("[init] props=" + props);
            getLog().debug("[init] configuration=" + configuration);
        }

        try {
            if (configuration.isConfigured()) {
                admin=new HypericRabbitAdmin(configuration.getNodename(),configuration.getAuthentication());
            }
        } catch (RuntimeException ex) {
            getLog().debug(ex.getMessage(),ex);
            throw new PluginException(ex.getMessage(),ex);
        }
    }

    public final HypericRabbitAdmin getAdmin() {
        return admin;

    }

    public abstract Log getLog();
}

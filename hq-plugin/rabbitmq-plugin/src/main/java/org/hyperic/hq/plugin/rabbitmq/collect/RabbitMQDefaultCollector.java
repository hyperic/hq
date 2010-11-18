/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.validate.ConfigurationValidator;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public abstract class RabbitMQDefaultCollector extends Collector {

    private HypericRabbitAdmin admin;

    @Override
    protected final void init() throws PluginException {
        super.init();
        Properties props = getProperties();
        if (getLog().isDebugEnabled()) {
            getLog().debug("[init] props=" + props);
        }

        try {
            if (ConfigurationValidator.isValidOtpConnection(props)) {
                if (admin != null) {
                    admin.destroy();
                }
                admin = new HypericRabbitAdmin(props);
            }
        } catch (RuntimeException ex) {
            getLog().debug(ex.getMessage(), ex);
            throw new PluginException(ex.getMessage(), ex);
        }
    }

    public final void collect() {
        if (admin == null) {
            admin = new HypericRabbitAdmin(getProperties());
        }
        try {
            if (admin != null) {
                collect(admin);
            }
        } catch (Throwable ex) {
            setAvailability(false);
            getLog().debug(ex.getMessage(), ex);
            admin.destroy();
            admin = null;
        }
    }

    public abstract void collect(HypericRabbitAdmin admin);

    public abstract Log getLog();
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
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

        admin = new HypericRabbitAdmin(props);
        admin.getOverview(); // test the connection.

    }

    /**
     * Set default values to counters with null value. Fix [HHQ-5962]
     * 
     * @author Tal Goldman
     * @param key
     * @param val
     */
    protected void setValue(String key, Number val) {
        val = (val!=null)?val:new Double(Double.NaN);
        super.setValue(key, val.doubleValue()); 
    }
    
    @Override
    public final void collect() {
        try {
            if (admin == null) {
                admin = new HypericRabbitAdmin(getProperties());
            }
            if (admin != null) {
                collect(admin);
            }
        } catch (Throwable ex) {
            setAvailability(false);
            getLog().debug(ex.getMessage(), ex);
            admin = null;
        }

        boolean https = "true".equals(getProperties().getProperty(DetectorConstants.HTTPS));
        if (https) {
            admin = null;
        }
    }

    public abstract void collect(HypericRabbitAdmin admin);

    public abstract Log getLog();
}

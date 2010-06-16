/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;

/**
 *
 * @author administrator
 */
public class RabbitMQProductPlugin extends ProductPlugin {

    Log log = getLog();

    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
    }
}

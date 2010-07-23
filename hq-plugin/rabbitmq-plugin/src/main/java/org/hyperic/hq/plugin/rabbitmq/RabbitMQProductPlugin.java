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
    protected static String ERLANG_COOKIE;
    protected static String ERLANG_COOKIE_FILE;
    protected static String ERLANG_COOKIE_PROP="rabbitmq.erlang.cookie.file";

    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        log.debug("[init] props=" + manager.getProperties());
        ERLANG_COOKIE_FILE = manager.getProperty(ERLANG_COOKIE_PROP);
        log.debug("[init] "+ERLANG_COOKIE_PROP+"="+ERLANG_COOKIE_FILE);
        //System.getProperties().setProperty("OtpConnection.trace", "99");
    }
}

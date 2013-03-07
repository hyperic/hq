/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss7;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class JBoss7ManagedControl extends ControlPlugin {

    private final Log log = getLog();
    private JBossAdminHttp admin;

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        log.debug("[configure] config=" + config);
        admin = new JBossAdminHttp(getConfig().toProperties());
    }

    @Override
    public List<String> getActions() {
        return Arrays.asList("start", "restart", "stop");
    }

    public void stop() throws PluginException {
        log.debug("[stop] config=" + getConfig());
        if (!isRunning()) {
            throw new PluginException("Server is not running");
        }
        admin.stop();
    }

    public void start() throws PluginException {
        log.debug("[start] config=" + getConfig());
        if (isRunning()) {
            throw new PluginException("Server is running");
        }
        admin.start();
    }

    public void restart() throws PluginException {
        log.debug("[restart] config=" + getConfig());
        if (isRunning()) {
            admin.restart();
        } else {
            admin.start();
        }
    }

    @Override
    protected boolean isRunning() {
        try {
            admin.testConnection();
        } catch (PluginException ex) {
            log.debug(ex, ex);
            return false;
        }
        return true;
    }
}

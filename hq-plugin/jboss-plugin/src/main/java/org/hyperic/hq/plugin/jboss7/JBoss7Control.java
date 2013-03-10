/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss7;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class JBoss7Control extends ServerControlPlugin {

    private final Log log = getLog();
    private JBossAdminHttp admin;
    private String control;
    
    @Override
    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        log.debug("[configure] config=" + config);
        admin = new JBossAdminHttp(getConfig().toProperties());
        control = getConfig().getValue(JBossDetectorBase.START);
    }

    @Override
    public List<String> getActions() {
        return Arrays.asList("start", "stop");
    }

    public void stop() throws PluginException {
        log.debug("[stop] config=" + getConfig());
        if (!isRunning()) {
            throw new PluginException("Server is not running");
        }

        admin.stop();
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

    public void start() throws PluginException {
        if (isRunning()) {
            throw new PluginException("Server is running");
        }
        doCommand();
        waitForState(STATE_STARTED);
    }

    @Override
    public String getControlProgram() {
        return control;
    }

    @Override
    protected boolean isBackgroundCommand() {
        return true;
    }
}

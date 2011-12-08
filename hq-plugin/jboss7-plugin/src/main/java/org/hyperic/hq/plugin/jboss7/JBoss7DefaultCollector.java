package org.hyperic.hq.plugin.jboss7;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

public abstract class JBoss7DefaultCollector extends Collector {

    private JBossAdminHttp admin;

    @Override
    protected final void init() throws PluginException {
        super.init();
        Properties props = getProperties();
        if (getLog().isDebugEnabled()) {
            getLog().debug("[init] props=" + props);
        }

        admin = new JBossAdminHttp(props);
        admin.getTestConnection();
    }

    public final void collect() {
        try {
            if (admin == null) {
                admin = new JBossAdminHttp(getProperties());
            }
            if (admin != null) {
                collect(admin);
            }
        } catch (Throwable ex) {
            setAvailability(false);
            getLog().debug(ex.getMessage(), ex);
            admin = null;
        }

        boolean https = "true".equals(getProperties().getProperty(JBossStandaloneDetector.HTTPS));
        if (https) {
            admin = null;
        }
    }

    public abstract void collect(JBossAdminHttp admin);

    public abstract Log getLog();
}

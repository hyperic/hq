package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

public class JMXAgentCollector extends Collector {

    static Log log = LogFactory.getLog(JMXAgentCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    @Override
    public void collect() {
        Properties props = getProperties();
        JMXConnector connector = null;
        try {
            connector = MxUtil.getMBeanConnector(props);
            MBeanServerConnection mServer = connector.getMBeanServerConnection();
            ObjectName obj = new ObjectName("GemFire:type=Agent");
            mServer.getAttribute(obj, "version");
            setAvailability(true);
        } catch (Exception ex) {
            setAvailability(false);
            log.debug(ex, ex);
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                log.debug(e, e);
            }
        }
    }
}

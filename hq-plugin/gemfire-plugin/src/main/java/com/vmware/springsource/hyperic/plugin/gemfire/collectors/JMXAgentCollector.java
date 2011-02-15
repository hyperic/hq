package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
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
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            ObjectName obj = new ObjectName("GemFire:type=Agent");
            mServer.getAttribute(obj, "version");
            setAvailability(true);
        } catch (Exception ex) {
            setAvailability(false);
            log.debug(ex, ex);
        }
    }
}

package org.hyperic.hq.plugin.jboss7;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.Connector;
import org.hyperic.hq.product.PluginException;

public class ConnectorCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(ConnectorCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        String connector = (String) getProperties().get("connector");
        try {
            Connector c = admin.getConnector(connector);
            setAvailability(true);
            setValue("bytesReceived", c.getBytesReceived());
            setValue("bytesSent", c.getBytesSent());
            setValue("errorCount", c.getErrorCount());
            setValue("maxTime", c.getMaxTime());
            setValue("processingTime", c.getProcessingTime());
            setValue("requestCount", c.getRequestCount());
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}

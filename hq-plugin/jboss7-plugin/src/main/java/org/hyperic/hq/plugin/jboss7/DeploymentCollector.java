package org.hyperic.hq.plugin.jboss7;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.Deployment;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class DeploymentCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(ConnectorCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        String connector = (String) getProperties().get("connector");
        try {
            List<Deployment> deployments = admin.getDeployments();
            for (Deployment d : deployments) {
                String name = d.getName();
                setValue(name + "." + Metric.ATTR_AVAIL, d.getEnabled() ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);
            }
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = result.getMetricValue(metric.getAttributeName());
        if (metric.getAttributeName().endsWith(Metric.ATTR_AVAIL)) {
            if (res.getValue() != Metric.AVAIL_UP) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            }
            log.debug("[getValue] Member=" + metric.getObjectProperty("member.name") + " metric=" + metric.getAttributeName() + " res=" + res.getValue());
        }
        return res;
    }

    @Override
    public Log getLog() {
        return log;
    }
}

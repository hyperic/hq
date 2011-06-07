/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class JBossWeb2XMeasurementPlugin extends JBossWebMeasurementPlugin {

    /**
     * For non-JBossWeb metrics we use standard JMX protocol.
     */
    @Override
    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricNotFoundException,
            MetricUnreachableException {

        if (metric.getDomainName().equals("java.lang")) {
            return super.getJMXValue(metric);
        }

        return super.getValue(metric);
    }

    /**
     * We need to change the JMX.URL from JBoss JNP to java JMX (using sigar to get the correct url)
     */
    @Override
    public String translate(String template, ConfigResponse config) {
        getLog().debug("[translate] template=" + template);
        String metric = super.translate(template, config);
        if (template.contains(":java.lang:")) {
            getLog().debug("[translate] process.query=" + config.getValue("process.query"));
            getLog().debug("[translate] jmx.url=" + config.getValue("jmx.url"));
            metric=metric.replaceAll("jmx.url=[^,]*,", "jmx.url="+Metric.encode("ptql:" + config.getValue("process.query"))+",");
        }
        getLog().debug("[translate] metric=" + metric);
        return metric;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import java.io.IOException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxCompositeData;
import org.hyperic.hq.product.jmx.MxMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class JBossWebMeasurementPlugin extends MxMeasurementPlugin {

    private static final String COMPOSITE_PREFIX = "Composite.";

    public MetricValue getJMXValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        return super.getValue(metric);
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        double doubleVal;

        Object objectVal;
        if (metric.getAttributeName().startsWith(COMPOSITE_PREFIX)) {
            objectVal = getCompositeMetric(metric);
        } else {
            objectVal = JBossUtil.getRemoteMBeanValue(metric);
        }

        String stringVal = objectVal.toString();

        //check for value mappings in plugin.xml:
        //<property name"StateVal.Stopped"  value="0.0"/>
        //<property name="StateVal.Started" value="1.0"/>
        //<property name"State.3" value="1.0"/>
        String mappedVal =
                getTypeProperty(metric.getAttributeName() + "."
                + stringVal);

        if (mappedVal != null) {
            doubleVal = doubleValue(mappedVal);
        } else if (objectVal instanceof Number) {
            doubleVal = ((Number) objectVal).doubleValue();
        } else if (objectVal instanceof Boolean) {
            doubleVal =
                    ((Boolean) objectVal).booleanValue()
                    ? Metric.AVAIL_UP : Metric.AVAIL_DOWN;
        } else {
            doubleVal = doubleValue(stringVal);
        }

        if (doubleVal == -1) {
            return new MetricValue(Double.NaN);
        }

        return new MetricValue(doubleVal);
    }

    private double doubleValue(Object obj)
            throws PluginException {

        try {
            return Double.valueOf(obj.toString()).doubleValue();
        } catch (NumberFormatException e) {
            throw new PluginException("Cannot convert '" + obj
                    + "' to double");
        }
    }

    static Object getCompositeMetric(Metric metric) {

        String name = metric.getAttributeName().substring(COMPOSITE_PREFIX.length());

        int ix = name.indexOf('.');
        if (ix == -1) {
            throw new MetricInvalidException("Missing composite key");
        }

        String attr = name.substring(0, ix);
        String key = name.substring(ix + 1);

        Object obj;

        try {
            MBeanServerConnection mServer = JBossUtil.getMBeanServerConnection(metric);
            ObjectName objName = new ObjectName(metric.getObjectName());
            obj = mServer.getAttribute(objName, attr);
        } catch (Exception ex) {
            throw new MetricInvalidException("[getCompositeMetric] errror: " + ex, ex);
        }

        if (obj instanceof CompositeData) {
            return MxCompositeData.getValue((CompositeData) obj, key);
        } else {
            throw new MetricInvalidException("Not CompositeData");
        }
    }

    @Override
    public String translate(String template, ConfigResponse config) {
        String metric = super.translate(template, config);
        metric = metric.replace("Catalina", "jboss.web");
        return metric;
    }
}

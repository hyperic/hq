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
import org.hyperic.hq.product.jmx.MxMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class JBossWeb20MeasurementPlugin extends JBossWebMeasurementPlugin {

    @Override
    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricNotFoundException,
            MetricUnreachableException {
        double doubleVal;
        Object objectVal = JBossUtil.getRemoteMBeanValue(metric);
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
}

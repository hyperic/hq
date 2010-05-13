/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.zimbra.five;

import java.util.Arrays;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

/**
 *
 * @author laullon
 */
public class ZimbraMeasurement extends SigarMeasurementPlugin {

    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = null;
        try {
            res = super.getValue(metric);
            getLog().debug("--->" + res + " (" + metric + ")");
        } catch (Exception e) {
            getLog().debug(e);
        }
        return res;
    }

    public MeasurementInfo[] getMeasurements(TypeInfo info) {
        getLog().debug("[getMeasurements] (" + info + ")");
        MeasurementInfo[] res = super.getMeasurements(info);
        getLog().debug("[getMeasurements] (" + Arrays.asList(res) + ")");
        return res;
    }
}

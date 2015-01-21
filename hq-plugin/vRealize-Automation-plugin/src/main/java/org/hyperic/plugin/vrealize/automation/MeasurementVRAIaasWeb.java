    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author glaullon
 */
public class MeasurementVRAIaasWeb extends MeasurementPlugin {

    //private static final Log log = LogFactory.getLog(MeasurementVRAIaasWeb.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        return new MetricValue(Metric.AVAIL_UP);
    }

}

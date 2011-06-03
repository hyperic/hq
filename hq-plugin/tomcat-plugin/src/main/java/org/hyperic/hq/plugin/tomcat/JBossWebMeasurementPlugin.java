/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import org.hyperic.hq.product.jmx.MxMeasurementPlugin;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class JBossWebMeasurementPlugin extends MxMeasurementPlugin {

    @Override
    public String translate(String template, ConfigResponse config) {
        String metric = super.translate(template, config);
        metric = metric.replace("Catalina", "jboss.web");
        return metric;
    }
}

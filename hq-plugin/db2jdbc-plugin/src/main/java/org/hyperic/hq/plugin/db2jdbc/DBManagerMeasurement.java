/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import org.hyperic.hq.product.Metric;

/**
 *
 * @author laullon
 */
public class DBManagerMeasurement extends Measurement {

    protected String getQuery(Metric metric) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("** metric = " + metric);
        }
        String func = metric.getObjectProperties().getProperty("func");
        return "SELECT * FROM TABLE(" + func + "(-2))";
    }

}

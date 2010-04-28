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
            getLog().debug("*******************");
            getLog().debug("** MT " + metric);
            getLog().debug("** DN " + metric.getDomainName());
            getLog().debug("** OP " + metric.getObjectProperties());
            getLog().debug("** ON " + metric.getObjectName());
            getLog().debug("** AT " + metric.getAttributeName());
            getLog().debug("** PR " + metric.getProperties());
            getLog().debug("*******************");
        }
        String func = metric.getObjectProperties().getProperty("func");
        return "SELECT * FROM TABLE(" + func + "(-2))";
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class JBoss5MeasurementPlugin extends JBossMeasurementPlugin {

    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, PluginException {

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

        MetricValue res;
        try {
            res = super.getValue(metric);
        } catch (Exception ex) {
            getLog().debug(ex.getMessage());
            if (Metric.ATTR_AVAIL.equals(metric.getAttributeName())) {
                res = new MetricValue(Metric.AVAIL_DOWN);
            } else {
                throw new MetricNotFoundException(ex.getMessage(), ex);
            }
        }

        //TOPIC
        if ("topic".equalsIgnoreCase(metric.getObjectProperties().getProperty("service"))) {
            if (res.getValue() < 0) {
                res.setValue(Metric.AVAIL_DOWN);
            } else {
                res.setValue(Metric.AVAIL_UP);
            }
        } else if (metric.getAttributeName().equalsIgnoreCase("state")) {
            switch ((int) res.getValue()) {
                case 8:
                    res.setValue(Metric.AVAIL_UP);
                    break;
                default:
                    res.setValue(Metric.AVAIL_DOWN);
            }
        } else if (metric.getAttributeName().equalsIgnoreCase("AvailableCount")) {
            if (res.getValue() < 0d) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("[getValue] AvailableCount='" + res.getValue() + "' => '0'");
                }
                res.setValue(0);
            }
        }

        if (getLog().isDebugEnabled() && Metric.ATTR_AVAIL.equals(metric.getAttributeName())) {
            getLog().debug("*******************");
            getLog().debug("res = '" + res + "'");
            getLog().debug("*******************");
        }
        return res;
    }

    public String translate(String template, ConfigResponse config) {
        return super.translate(translateMetic(template, config), config);

    }

    public static String translateMetic(String template, ConfigResponse config) {
        Pattern p = Pattern.compile("([:|,])([^=]*)=_(%([^%]*)%)([:|,])");
        Matcher m = p.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            //System.out.println("--> "+m);
            if (config.getValue(m.group(4)) != null) {
                m.appendReplacement(sb, m.group(1) + m.group(2) + "=" + m.group(3) + m.group(5));
            } else {
                m.appendReplacement(sb, m.group(1));
            }
        }
        m.appendTail(sb);
        return sb.toString();

    //template=template.replaceAll("=_(%[^%]*%)", "=$1");
    }
}

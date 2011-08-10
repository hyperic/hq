/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitNode;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;

/**
 *
 * @author administrator
 */
public class RabbitServerMeasurement extends SigarMeasurementPlugin {

    private static final Log logger = LogFactory.getLog(RabbitServerMeasurement.class);
    private final static Map<String, Integer> pidsCahce = new HashMap();

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        MetricValue res = null;
        if (metric.getDomainName().equals("rabbitmq-sigar")) {
            String nName = metric.getObjectProperty("node");
            Integer pid = pidsCahce.get(nName);
            if (pid == null) {
                HypericRabbitAdmin admin = new HypericRabbitAdmin(metric.getObjectProperties());
                RabbitNode node = admin.getNode(nName);
                pid = node.getOsPid();
                pidsCahce.put(nName, pid);
            }
            Metric _metric = Metric.parse("sigar:Type=" + metric.getObjectProperty("Type") + ",Arg=Pid.Pid.eq=" + pid + ":" + metric.getAttributeName());
            if (logger.isDebugEnabled()) {
                logger.debug("[getValue] -> metric=" + metric);
                logger.debug("[getValue] <- metric=" + _metric);
            }
            try {
                res = super.getValue(_metric);
            } catch (MetricNotFoundException ex) {
                pidsCahce.clear();
                throw ex;
            }
        } else {
            try{
            res = Collector.getValue(this, metric);
            }catch(PluginException ex){
                if(metric.isAvail()){
                    res=new MetricValue(Metric.AVAIL_DOWN);
                }else{
                    throw ex;
                }
            }
        }
        return res;
    }
}

package org.hyperic.hq.plugin.hyper_v;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.sigar.win32.Pdh;

public class HypervCollector  extends Collector {

    private static Log log = LogFactory.getLog(HypervCollector.class);
    private Map<String,Long> counters = new HashMap<String, Long>();
    // we allow to keep an object that is not queryied for an hour in the hash
    private static long MAX_TIME_WITHOUT_QUERY = 1000*60 *60;
    
    
    
    @Override
    public void collect() {
        log.debug("[collect] [" + getProperties() + "] counters.size() = " + counters.size());
        if (counters.size() > 0) {
            try {
                
                /* sigar doe snot support generics */
                if (log.isDebugEnabled()) {
                    log.debug("before getFormattedValues: counetrs=" + counters);
                }
                Map<String,Double>  res  = new Pdh().getFormattedValues(counters.keySet());
                if (log.isDebugEnabled()) {
                    log.debug("after getFormattedValues res=" + res);
                }
                for (Map.Entry<String,Double> entry : res.entrySet()) {
                    String obj = entry.getKey();
                    Double val = entry.getValue();
                    log.debug("[collect] " + obj + " = " + val);
                    setValue(obj, val);
                }
            } catch (Exception ex) {
                log.debug("[collect] " + ex, ex);
            }
            
            cleanExpiredEntries();
        }
    }
    
    private void  cleanExpiredEntries() {
        Set<Map.Entry<String, Long>> entries = counters.entrySet();
        Iterator<Map.Entry<String, Long>> it = entries.iterator();
        long currentTime = System.currentTimeMillis();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if ( (currentTime - entry.getValue()) > MAX_TIME_WITHOUT_QUERY) {
                log.debug("removing counter=" + entry.getKey() + " was not queryied for more than:" + MAX_TIME_WITHOUT_QUERY/60000 + " minutes");
                it.remove();
            }
        }
    }
    
    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        String obj = HyperVMeasurementPlugin.getPath(metric);        
        
        MetricValue res = MetricValue.NONE;
        if (obj != null) {
            if (counters.containsKey(obj)) {
                res = result.getMetricValue(obj);
            }
            // add / or update expiration time
            counters.put(obj, System.currentTimeMillis());           
        }

        log.debug("[getValue] obj:'" + obj + "' res:'" + res.getValue() + "'");
        return res;
    }

    

}

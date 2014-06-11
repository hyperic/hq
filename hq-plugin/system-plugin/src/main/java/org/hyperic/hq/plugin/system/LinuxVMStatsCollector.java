/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.system;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;

/**
 *
 * @author glaullon
 */
public class LinuxVMStatsCollector extends Collector {

    private static Log log = LogFactory.getLog(LinuxVMStatsCollector.class);
    private static final String metrics[] = {"pgfault","pgmajfault"};

    @Override
    public void collect() {
        log.debug("[collect]");
        
        Map<String,Integer> stats = LinuxUtils.getVMStats();
        for (String metric : metrics) {
            setValue(metric, stats.get(metric));
        }
    }
    
}

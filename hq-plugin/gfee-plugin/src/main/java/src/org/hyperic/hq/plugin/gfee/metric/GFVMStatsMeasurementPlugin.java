/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.gfee.metric;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFProductPlugin;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.cache.MetricCache;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.StringUtil;

/**
 * Special handling for GF related jvm metrics.
 * <p>
 * GemFire is wrapping jvm related mbeans under its own mbeans. Like what happens with other jvm's
 * also GemFire is giving different mbean names based on used jvm strategy. Below list outlines possible
 * naming conventions:
 * 
 * VMStats:
 *   vmStats
 *   
 * VMMemoryUsageStats:
 *   vmHeapMemoryStats
 *   VMMemoryUsageStats
 * 
 * VMMemoryPoolStats:
 *   Young Eden                  | Young Survivor                 | Old Tenured              | Old Permanent               | Code Cache
 *   --------------------------------------------------------------------------------------------------------------------------------------------------
 *   PS Eden Space-Heap memory   | PS Survivor Space-Heap memory  | PS Old Gen-Heap memory   | PS Perm Gen-Non-heap memory | Code Cache-Non-heap memory
 *   Eden Space-Heap memory      | Survivor Space-Heap memory     | Tenured Gen-Heap memory  | Perm Gen-Non-heap memory    | Cache-Non-heap memory
 *   
 * 
 * VMGCStats: (GC's)
 *   Cheap           | Expensive
 *   ---------------------------------
 *   PS Scavenge     | PS MarkSweep
 *   Copy            | MarkSweepCompact
 *   ParNew          | ConcurrentMarkSweep
 */
public class GFVMStatsMeasurementPlugin extends GFMeasurementPlugin {

    /** Default set of metrics. */    
    private static final String[] vmstatsMetrics;
    private static final String[] vmheapmemorystatsMetrics;
    private static final String[] vmnonheapmemorystatsMetrics;
    private static final String[] codecacheMetrics;
    private static final String[] youngedenMetrics;
    private static final String[] youngsurvivorMetrics;
    private static final String[] oldtenuredMetrics;
    private static final String[] oldpermanentMetrics;
    private static final String[] gccheapMetrics;
    private static final String[] gcexpensiveMetrics;
    
    static{
        vmstatsMetrics = new String[]{
                "vmstats-daemonThreads","vmstats-loadedClasses","vmstats-cpus","vmstats-fdLimit","vmstats-fdsOpen","vmstats-freeMemory","vmstats-maxMemory","vmstats-peakThreads","vmstats-pendingFinalization","vmstats-processCpuTime","vmstats-threadStarts","vmstats-threads","vmstats-totalMemory","vmstats-unloadedClasses"
        };
        vmheapmemorystatsMetrics = new String[]{
                "vmheapmemorystats-committedMemory"
        };
        vmnonheapmemorystatsMetrics = new String[]{
                "vmnonheapmemorystats-committedMemory"
        };
        codecacheMetrics = new String[]{
                "codecache-currentUsedMemory"
         };
        youngedenMetrics = new String[]{
                "youngeden-currentUsedMemory"
         };
        youngsurvivorMetrics = new String[]{
                "youngsurvivor-currentUsedMemory"
         };
        oldtenuredMetrics = new String[]{
                "oldtenured-currentUsedMemory"
         };
        oldpermanentMetrics = new String[]{
                "oldpermanent-currentUsedMemory"
         };
        gccheapMetrics = new String[]{
                "gccheap-collectionTime","gccheap-collections"
         };
        gcexpensiveMetrics= new String[]{
                "gcexpensive-collectionTime","gcexpensive-collections"
         };
         
    }

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(GFVMStatsMeasurementPlugin.class);

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.MeasurementPlugin#getValue(org.hyperic.hq.product.Metric)
     */
    @Override
    public MetricValue getValue(Metric metric) throws PluginException,
    MetricNotFoundException, MetricUnreachableException {
        
        if(log.isDebugEnabled()) {
            log.debug("Plugin hash id:" + hashCode());
            log.debug("Metric:" + metric.toDebugString());
        }

        Properties mProps = metric.getObjectProperties();

        MemberCache memberCache = ((GFProductPlugin)getProductPlugin())
            .getMemberCache(mProps.getProperty(GFMXConstants.CONF_JMX_URL));

        String workingDirectory = mProps.getProperty(GFMXConstants.ATTR_PWD);
        String host = mProps.getProperty(GFMXConstants.ATTR_HOST);
        String name = mProps.getProperty(GFMXConstants.ATTR_NAME);
        String gfid = memberCache.getGfid(workingDirectory, host, name);

        // If member is gone, we also lost reference to gfid because
        // background task flushed it during member update.
        // Case like this we also lost metricCache and
        // need to return avail metric down and others none.

        // If members returns, we continue from there.
        if(gfid == null) {
            memberCache.refresh(mProps);
            if(metric.isAvail()) {
                log.debug("Don't know gfid, returning down for avail.");
                return new MetricValue(Metric.AVAIL_DOWN);                              
            } else {
                log.debug("Don't know gfid, returning none.");
                return new MetricValue(MetricValue.VALUE_NONE);             
            }
        }

        // from this point forward we need to have metric cache.
        MetricCache metricCache = getMetricCache(gfid);

        if(metricCache.getMetricCacheLastUpdate() < System.currentTimeMillis()-55000) {
            String[] keys = metricCache.getTrackKeySet();

            if(log.isDebugEnabled()) {
                if(keys.length > 0)
                    log.debug("Time to update metric cache using keys:" + StringUtil.arrayToString(keys));
                else
                    log.debug("Time to update metric cache, asking all mbean attributes.");
            }

            Map<String, Double> stats = collectVMStats(mProps, keys);
            
            // if null or empty, set member offline
            metricCache.setMemberOnline(stats != null && stats.size() > 0); 
            if(!metricCache.isMemberOnline()) {
                memberCache.refresh(mProps);
            }

            if(stats != null)
                metricCache.getMetricCache().putAll(stats);

            metricCache.setMetricCacheLastUpdate(System.currentTimeMillis());

            // if we got real values, mark service/server available
            metricCache.getMetricCache().put(Metric.ATTR_AVAIL, stats != null ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);
        }

        String alias = metric.getAttributeName();
        Double value;

        // update tracking cache so that next collection cycle
        // have better understanding which metrics are enabled.
        if(alias.startsWith("custom_")) {
            CustomMetric cm = CustomMetric.buildByAlias(alias);
            String[] metrics = cm.getMetrics();
            value = cm.calculate(new Double[]{metricCache.getMetricCache().get(metrics[0]),metricCache.getMetricCache().get(metrics[1])});
            metricCache.putToTrackCache(metrics[0], new Double(1));
            metricCache.putToTrackCache(metrics[1], new Double(1));            
        } else {
            value = metricCache.getMetricCache().get(alias);
            metricCache.putToTrackCache(alias, new Double(1));            
        }

        if(metricCache.isMemberOnline()) {
            log.debug("Resource online, returning metric:" + value);
            // if we get null from cache, return none
            return new MetricValue(value != null ? value : MetricValue.VALUE_NONE);          
        } else {
            if(metric.isAvail()) {
                log.debug("Resource not online, returning down for avail.");
                return new MetricValue(Metric.AVAIL_DOWN);                              
            } else {
                log.debug("Resource not online, returning none.");
                return new MetricValue(MetricValue.VALUE_NONE);             
            }
        }
    }
    
    /**
     * Collecting actual stats from JMX.
     * 
     * @param props
     * @param keys
     * @return
     */
    protected Map<String, Double> collectVMStats(Properties props, String[] keys) {

        MemberCache memberCache = ((GFProductPlugin)getProductPlugin())
            .getMemberCache(props.getProperty(GFMXConstants.CONF_JMX_URL));

        String workingDirectory = props.getProperty(GFMXConstants.ATTR_PWD);
        String host = props.getProperty(GFMXConstants.ATTR_HOST);       
        String name = props.getProperty(GFMXConstants.ATTR_NAME);       
        MemberInfo member = memberCache.getMember(workingDirectory, host, name);
        GFJmxConnection gf = new GFJmxConnection(props);

        Map<String, Double> map = new HashMap<String, Double>();

        Map<String, String[]> requestMap = getMetricRequestMap(keys, props);
        if(requestMap.size() == 0)
            requestMap = createDefaultMetricRequestMap(props);

        Iterator<String> iter = requestMap.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            String[] metrics = requestMap.get(key);
            // get needed prefix from first metric for back mapping
            String prefix = metrics[0].substring(0,metrics[0].indexOf('-'));
            Map<String, Double> values = gf.getStatValues(member, key, removePrefixes(metrics));
            // if we get null from stats, move on to next iteration
            if(values == null)
                continue;
            Set<Entry<String,Double>> set = values.entrySet();
            for (Entry<String, Double> entry : set) {
                map.put(prefix+"-"+entry.getKey(), entry.getValue());
            }
        }
        
        return map;
    }

    private Map<String, String[]> createDefaultMetricRequestMap(Properties props) {
        Map<String, String[]> map = new HashMap<String, String[]>();

        map.put(props.getProperty(GFMXConstants.CONF_VMSTATS), vmstatsMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_VMHEAPMEMORYSTATS), vmheapmemorystatsMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_VMNONHEAPMEMORYSTATS), vmnonheapmemorystatsMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_CODECACHE), codecacheMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_YOUNGEDEN), youngedenMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_YOUNGSURVIVOR), youngsurvivorMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_OLDTENURED), oldtenuredMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_OLDPERMANENT), oldpermanentMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_GCCHEAP), gccheapMetrics);
        map.put(props.getProperty(GFMXConstants.CONF_GCEXPENSIVE), gcexpensiveMetrics);
        
        return map;
    }

    /**
     * Helper method to build request map which will be used to
     * pull out metrics from jmx.
     * <p>
     * This function basically iterates keys (which maps back to HQ),
     * and checks mapping tables to know which MBeans whould be accessed.
     * 
     * @param keys
     * @return
     */
    private Map<String, String[]> getMetricRequestMap(String[] keys, Properties props) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        
        for (String key : keys) {
            // get prefix and skip all keys without dash. e.g. Availability
            int dashIndex = key.indexOf('-');
            if(dashIndex < 0) continue;
            String prefix = key.substring(0, dashIndex);
            
            // create empty array if map doesn't contain it
            if(!map.containsKey(props.getProperty(prefix))) {
                map.put(props.getProperty(prefix), new String[0]);
            }
            
            // get array from map
            String[] array = map.get(props.getProperty(prefix));
            
            // replace it with new one
            String[] extended = new String[array.length+1];
            System.arraycopy(array, 0, extended, 0, array.length);
            extended[extended.length-1] = key;
            map.put(props.getProperty(prefix), extended);
        }
        
        return map;
    }
    
    /**
     * Helper method to remove prefix from all array members.
     * <p>
     * e.g. "vmstats-daemonThreads" will become "daemonThreads"
     * 
     * @param array String array to modify
     * @return New array with prefixes removed
     */
    private String[] removePrefixes(String[] array) {
        String[] fixedArray = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            fixedArray[i] = array[i].substring(array[i].indexOf('-')+1);
        }
        return fixedArray;
    }

}

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

import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFProductPlugin;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MetricCache;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.StringUtil;

/**
 * 
 */
public class GFStatsMeasurementPlugin extends GFMeasurementPlugin {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(GFStatsMeasurementPlugin.class);

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
        String statname = mProps.getProperty(GFMXConstants.CONF_STATNAME);

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
        MetricCache metricCache = getMetricCache(gfid+statname);

        if(metricCache.getMetricCacheLastUpdate() < System.currentTimeMillis()-55000) {
            String[] keys = metricCache.getTrackKeySet();

            if(log.isDebugEnabled()) {
                if(keys.length > 0)
                    log.debug("Time to update metric cache using keys:" + StringUtil.arrayToString(keys));
                else
                    log.debug("Time to update metric cache, asking all mbean attributes.");
            }

            Map<String, Double> stats = collectStats(mProps, keys);
            
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
            if(log.isDebugEnabled()) {
                log.debug("Resource online, returning metric:" + value);
                if(metric.isAvail()) {
                    log.debug("Availability is " + value);
                }
            }
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

}

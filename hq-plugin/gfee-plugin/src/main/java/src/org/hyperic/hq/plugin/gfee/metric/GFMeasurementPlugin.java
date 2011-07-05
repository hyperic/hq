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

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFProductPlugin;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.cache.MetricCache;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;

/**
 * The Class GFMeasurementPlugin.
 */
public abstract class GFMeasurementPlugin extends MeasurementPlugin {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(GFMeasurementPlugin.class);

    /** References to metric caches.*/
    private Map<String, MetricCache> metricCaches;


    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);

        // measurement plugin may be shared between same
        // resource types. we access it by unique id.
        metricCaches = new Hashtable<String, MetricCache>();
    }

    /**
     * Returns metric cache.
     * 
     * @param id Unique id identifying cache. Use something unique like gfid.
     * @return Metric cache
     */
    protected MetricCache getMetricCache(String id) {
        MetricCache cache = metricCaches.get(id);
        if(cache == null) {
            cache = new MetricCache();
            metricCaches.put(id, cache);
        }
        return cache;
    }

    /**
     * Collect real statistics from GemFire JMX Agent.
     * 
     * @param props Connection properties
     * @param keys Attributes if known, null means we ask all values
     * 
     * @return Map containing metric alias mapped to value. Null if collection failed.
     */
    protected Map<String, Double> collectStats(Properties props, String[] keys) {

        if(log.isDebugEnabled()) {
            log.debug("Got these properties for collection: " + props);
        }

        // Cache identified by jmx.url. "There can be only one member cache per url"
        MemberCache memberCache = ((GFProductPlugin)getProductPlugin())
            .getMemberCache(props.getProperty(GFMXConstants.CONF_JMX_URL));

        String workingDirectory = props.getProperty(GFMXConstants.ATTR_PWD);
        String host = props.getProperty(GFMXConstants.ATTR_HOST);		
        String name = props.getProperty(GFMXConstants.ATTR_NAME);		

        Map<String, Double> map = null;
        try {
            GFJmxConnection gf = new GFJmxConnection(props);

            MemberInfo member = memberCache.getMember(workingDirectory, host, name);
            if(keys.length > 0)
                map = gf.getStatValues(member, props.getProperty(GFMXConstants.CONF_STATNAME), keys);
            else
                map = gf.getStatValues(member, props.getProperty(GFMXConstants.CONF_STATNAME));
        } catch (Exception e) {
            log.debug("Error collecting stats. " + e.getMessage(), e);
        }
        return map;
    }



}

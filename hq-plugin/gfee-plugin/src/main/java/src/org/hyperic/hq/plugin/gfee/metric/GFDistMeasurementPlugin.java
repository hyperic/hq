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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

/**
 * Metric collection on platform level. This measurement plugin
 * is only used to track GemFire DS level metrics.
 */
public class GFDistMeasurementPlugin extends MeasurementPlugin {

    /** The Constant log. */
    private static final Log log = 
        LogFactory.getLog(GFDistMeasurementPlugin.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException,
    MetricNotFoundException, MetricUnreachableException {

        Properties mProps = metric.getObjectProperties();

        if(log.isDebugEnabled()) {
            log.debug("Plugin hash id:" + hashCode());
            log.debug("Metric:" + metric.toDebugString());
            log.debug("Properties for metric getValue:" + mProps);
        }

        String jmxUrl = mProps.getProperty("jmx.url");
        if(jmxUrl == null)
            throw new PluginException("jmx.url is null, cannot continue.");

        GFJmxConnection gf = new GFJmxConnection(mProps);

        // we only ask if we're able to connect to DS system.
        boolean alive = gf.isDistributionAlive();

        if(log.isDebugEnabled()) {
            log.debug("Is GF DS alive:" + alive);			
        }

        return new MetricValue(alive ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);		
    }



}

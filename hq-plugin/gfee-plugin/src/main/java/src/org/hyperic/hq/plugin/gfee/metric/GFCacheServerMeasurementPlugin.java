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

import javax.management.InstanceNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.GFProductPlugin;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

/**
 * 
 *
 */
public class GFCacheServerMeasurementPlugin extends GFMeasurementPlugin {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(GFCacheServerMeasurementPlugin.class);

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
        
        boolean needMemberUpdate = false;
        double value = Metric.AVAIL_DOWN;
        Map<String, Object> map;

        GFJmxConnection gf = new GFJmxConnection(mProps);
        
        // switch workingDirectory from null to ""
        MemberInfo member = memberCache.getMember(workingDirectory == null ? "" : workingDirectory, host, name);
        if(member == null) {
            needMemberUpdate = true;
        } else {
            String gfid = member.getGfid();
            try {
                if(member.getMemberType() == MemberInfo.MEMBER_TYPE_CACHEVM) {
                    map = gf.getCacheVmAttributes(gfid, new String[]{"running"});
                    if(map.containsKey("running"))
                        value = Metric.AVAIL_UP;   
                } else if(member.getMemberType() == MemberInfo.MEMBER_TYPE_APPLICATION) {
                    map = gf.getApplicationAttributes(gfid, new String[]{"id"});
                    if(map.containsKey("id"))
                        value = Metric.AVAIL_UP;   
                } else {
                    needMemberUpdate = true;
                }                
            } catch (Exception e) {
                needMemberUpdate = true;
            }
        }

        if(needMemberUpdate)
            memberCache.refresh(mProps);
        
        if(log.isDebugEnabled()) {
            if(member == null) {
                log.debug("[getValue] member is null");                
            } else {
                log.debug("[getValue] for " + member.getGfid() + " is " + value);
            }            
        }

        return new MetricValue(value);          
    }


}

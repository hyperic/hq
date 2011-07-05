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
package org.hyperic.hq.plugin.gfee.detector;

import java.util.ArrayList;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.GFMXConstants;
import org.hyperic.hq.plugin.gfee.util.GFMXUtils;
import org.hyperic.hq.plugin.gfee.util.GFUtils;
import org.hyperic.util.config.ConfigResponse;

/**
 * 
 */
public class GatewayHubDetector extends MemberDetector {

    /** The Constant log. */
    private final static Log log =
        LogFactory.getLog(GatewayHubDetector.class);

    @Override
    protected boolean hasCorrectRoles(int mask) {
        if(log.isDebugEnabled()) log.debug(GFUtils.roleMaskToDebugString(mask));
        return ((mask & GFMXConstants.MEMBER_ROLE_GATEWAYHUB) == GFMXConstants.MEMBER_ROLE_GATEWAYHUB);
    }    
    
    @Override
    protected StatType[] filterSupportedStats(Object[][] statObjects, ConfigResponse config) {

        ArrayList<StatType> stats = new ArrayList<StatType>();

        for (int i = 0; i < statObjects.length; i++) {
            ObjectName o = (ObjectName)statObjects[i][0];
            String type = (String)statObjects[i][1];
            String name = GFMXUtils.getField(o, "name");
            if(name.equals("distributionStats")) {
                stats.add(new StatType("Distribution Statistics", o));
            } else if(name.startsWith("gatewayHubStats-")) {
                stats.add(new StatType("Gateway Hub Statistics", o));				
            } else if(name.startsWith("gatewayStats-")) {
                stats.add(new StatType("Gateway Statistics", o, o.getKeyProperty("name").substring(13)));               
            } else if(name.startsWith("RegionStats-")) {
                stats.add(new StatType("Region", o, o.getKeyProperty("name").substring(12)));				
            } else if(name.startsWith("cachePerfStats")) {
                stats.add(new StatType("Cache Performance", o));
            } else if(name.startsWith("Partitioned Region")) {
                stats.add(new StatType("Partitioned Region", o, o.getKeyProperty("name").substring(19)));               
            } else if(type.equals("CacheServerStats") && !name.startsWith("RegionStats-")) {
                stats.add(new StatType("Cache Server", o, o.getKeyProperty("name")));				
            } else if(name.equals("dlockStats")) {
                stats.add(new StatType("Distributed Lock", o));				
            } else if(name.equals("ResourceManagerStats")) {
                stats.add(new StatType("Resource Manager", o));             
            } else if(name.equals("FunctionExecution")) {
                stats.add(new StatType("Function Service", o));
            } else if(type.equals("FunctionStatistics")) {
                stats.add(new StatType("Function", o, o.getKeyProperty("name")));               
            } else if(name.equals("cacheClientNotifierStats")) {
                stats.add(new StatType("Cache Client Notifier", o));             
            } else if(type.equals("DiskStoreStatistics")) {
                stats.add(new StatType("Disk Store", o, o.getKeyProperty("name")));               
            } else if(type.equals("DiskDirStatistics")) {
                stats.add(new StatType("Disk Directory", o, o.getKeyProperty("name")));               
            } else if(type.equals("DiskRegionStatistics")) {
                stats.add(new StatType("Disk Region", o, o.getKeyProperty("name")));               
            } else if(type.equals("PoolStats")) {
                stats.add(new StatType("Pool Stats", o, o.getKeyProperty("name")));               
            }			
        }

        return stats.toArray(new StatType[0]);
    }

}

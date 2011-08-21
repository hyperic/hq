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
package org.hyperic.hq.plugin.gfee.cache;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.MemberUpdater;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;
import org.hyperic.util.config.ConfigResponse;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Member cache acts as a middle tier between gemfire member id's
 * and immutable member variables.
 * 
 * Unique id(referenced as gfid throughout the code base) usually looks
 * like "localhost(8176)<v1>:40659/33359". It differs slightly depending
 * on member acting role(hub,cacheserver,peer,etc) and changes every time
 * member is re-started.
 * 
 * We try to keep some immutable information what we expect not to change
 * during the restarts and use that information to map back to gfid. Immutable
 * information is used in HQ resource configuration to identify the real
 * instance.
 */
public class MemberCache {

    private static final Log log =
        LogFactory.getLog(MemberCache.class);

    /** Map for cache vm members */
    private BiMap<String, MemberInfo> cacheVMMap = HashBiMap.create();

    MemberUpdater memberUpdater;

    /**
     * 
     */
    public MemberCache() {
        super();
        memberUpdater = new MemberUpdater(this);		
    }

    /**
     * Finds gf member unique id from cache.
     * 
     * @param workingDirectory
     * @param host
     * @param name
     * @return
     */
    public String getGfid(String workingDirectory, String host, String name) {		
        MemberInfo tmp = new MemberInfo(null, name, host, workingDirectory);
        return cacheVMMap.inverse().get(tmp);
    }

    public MemberInfo getMember(String workingDirectory, String host, String name) {      
        MemberInfo tmp = new MemberInfo(null, name, host, workingDirectory);
        return cacheVMMap.get(cacheVMMap.inverse().get(tmp));
    }

    /**
     * 
     * @return
     */
    public Set<MemberInfo> getMembers() {
        return cacheVMMap.values();
    }
    
    public MemberInfo getMember(String id) {
        return cacheVMMap.get(id);
    }

    /**
     * Updates cache member information mappings.
     * 
     * @param config
     */
    public void refreshCacheVMs(ConfigResponse config) {
        GFJmxConnection gf = new GFJmxConnection(config);

        MemberInfo[] members = gf.getCacheVmMembers();

        for (MemberInfo memberInfo : members) {
            cacheVMMap.forcePut(memberInfo.getGfid(), memberInfo);
        }
    }

    public void refresh(Properties config) {
        memberUpdater.refreshCacheVMs(config);
    }

    /**
     * 
     */
    public void refreshPeers(ConfigResponse config) {
        GFJmxConnection gf = new GFJmxConnection(config);

        MemberInfo[] members = gf.getSystemMemberApplications();

        for (MemberInfo memberInfo : members) {
            cacheVMMap.forcePut(memberInfo.getGfid(), memberInfo);
        }		
    }

    public BiMap<String, MemberInfo> getCacheVMMap() {
        return cacheVMMap;
    }

    public void shutdown() {
        memberUpdater.stop();
    }

}

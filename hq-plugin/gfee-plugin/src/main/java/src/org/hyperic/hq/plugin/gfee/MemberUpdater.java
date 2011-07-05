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
package org.hyperic.hq.plugin.gfee;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.plugin.gfee.cache.MemberInfo;
import org.hyperic.hq.plugin.gfee.mx.GFJmxConnection;

import com.google.common.collect.BiMap;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

/**
 * Background service to handle member mapping updates.
 */
public class MemberUpdater extends AbstractExecutionThreadService {

    /** The Constant log. */
    private static final Log log =
        LogFactory.getLog(MemberUpdater.class);

    /** Handle back to member cache. */
    private MemberCache memberCache;

    /** Config used to access GF Jmx Agent. */
    private Properties config;

    /**
     * Initialize this member updater with associated member cache.
     * 
     * @param memberCache Member cache to work with.
     */
    public MemberUpdater(MemberCache memberCache) {
        this.memberCache = memberCache;
    }

    /**
     * 
     * @param config
     */
    public void refreshCacheVMs(Properties config) {
        if(this.config != null) {
            log.debug("Config exists, skipping refresh:");
            return;
        }
        this.config = config;
        log.debug("Service status:" + isRunning());
        log.debug("Asking updater to start the task");
        start();
    }

    /**
     * Run method run by executor service.
     */
    @Override
    protected void run() throws Exception {

        while(isRunning()) {
            updateMembers();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.debug("Got interrupt, continue...");
            }        }

        log.debug("I'm done baby...");
    }

    /**
     * 
     */
    private void updateMembers() {
        log.debug("Check if we need to update members with config:" + config);
        if(config == null) return;
        log.debug("Doing member cache update");
        GFJmxConnection gf = new GFJmxConnection(config);
        MemberInfo[] cMembers = gf.getCacheVmMembers();
        MemberInfo[] aMembers = gf.getSystemMemberApplications();
        BiMap<String, MemberInfo> cacheVMMap = memberCache.getCacheVMMap();
        cacheVMMap.clear();
        for (MemberInfo memberInfo : cMembers) {
            cacheVMMap.put(memberInfo.getGfid(), memberInfo);
        }
        for (MemberInfo memberInfo : aMembers) {
            cacheVMMap.put(memberInfo.getGfid(), memberInfo);
        }
        log.debug("Members updated");
        config = null;
    }

}

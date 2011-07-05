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

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.plugin.gfee.cache.MemberCache;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;

/**
 * The Class GFProductPlugin. 
 * 
 * Storing plugin level caches.
 */
public class GFProductPlugin extends ProductPlugin {

    /** The member mapping. */
    private Map<String, MemberCache> memberCaches = new HashMap<String, MemberCache>();


    /**
     * Returns instance if member mapping cache.
     * 
     * @param id Id to identify membercache. Need to do this e.g. using jmx.url
     *           to allow this plugin to support multiple DS envs.
     * @return Member cache
     */
    public MemberCache getMemberCache(String id){
        MemberCache cache = memberCaches.get(id);
        if(cache == null) {
            cache = new MemberCache();
            memberCaches.put(id, cache);
        }
        return cache;
    }


    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
    }


    @Override
    public void shutdown() throws PluginException {

        for (MemberCache cache: memberCaches.values()) {
            cache.shutdown();
        }

        super.shutdown();
    }

}

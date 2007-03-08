/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.server.session;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.dao.DAOFactory;

import java.util.List;

public class SRNCache {

    // The cache name, must match the definition in ehcache.xml
    private static String CACHENAME = "SRNCache";

    private static Cache _cache;

    private static SRNCache _singleton = new SRNCache();

    public static SRNCache getInstance() {
        return _singleton;
    }

    private SRNCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }

    /**
     * Get the current cache size.
     * 
     * @return The size of the cache
     */
    public long getSize() {
        return _cache.getSize();
    }

    public void put(ScheduleRevNum val) {
        Element el = new Element(val.getId(), val);
        _cache.put(el);
    }

    public ScheduleRevNum get(AppdefEntityID aid) {
        SrnId id = new SrnId(aid.getType(), aid.getID());
        return get(id);
    }

    /**
     * Get the SRN entry from the cache falling back to loading from the
     * database if the SRN is not found.  Since the SRNCache is pre-populated
     * the fallback to the database should only occur in clustered setups.
     */
    public ScheduleRevNum get(SrnId id) {
        Element el = _cache.get(id);
        if (el != null) {
            return (ScheduleRevNum)el.getObjectValue();
        }
        
        ScheduleRevNumDAO dao =
            DAOFactory.getDAOFactory().getScheduleRevNumDAO();
        ScheduleRevNum srn = dao.get(id);
        if (srn != null) {
            this.put(srn);
        }
        return srn;
    }

    public boolean remove(SrnId id) {
        return _cache.remove(id);
    }

    public List getKeys() {
        return _cache.getKeys();
    }
}

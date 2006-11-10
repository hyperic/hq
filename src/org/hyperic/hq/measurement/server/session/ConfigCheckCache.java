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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.MeasurementConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A singleton utility class to cache lookups of the measurements
 * we use when validating configs.
 */
class ConfigCheckCache {

    private final Log log = LogFactory.getLog(ConfigCheckCache.class);
    public static final int SAMPLE_SIZE = 4;
    public static final String availCat = MeasurementConstants.CAT_AVAILABILITY;

    private static ConfigCheckCache _instance = new ConfigCheckCache();
    public static ConfigCheckCache instance () { return _instance; }
    private ConfigCheckCache () {}

    private Map cache = new HashMap();

    public ConfigCheckCacheEntry getMetricsToCheck(AuthzSubjectValue s,
                                                   AppdefEntityID id, 
                                                   MeasurementTemplateDAO dao) 
        throws AppdefEntityNotFoundException, PermissionException {

        String mType = (new AppdefEntityValue(id, s)).getMonitorableType();
        ConfigCheckCacheEntry entry = null;
        synchronized (cache) {
            entry = (ConfigCheckCacheEntry) cache.get(mType);
        }
        if (entry != null) return entry;

        List templates =
                dao.findDefaultsByMonitorableType(mType, id.getType());
        if (templates.size() == 0) {
            String msg = "No default templates for monitorable type " + mType;
            log.error(msg);
            return updateCache(mType, ConfigCheckCacheEntry.EMPTY);
        }
        
        List dsnList = new ArrayList(SAMPLE_SIZE);
        int idx = 0;
        int availIdx = -1;
        MeasurementTemplate template;
        for (int i=0; i<templates.size(); i++) {

            template = (MeasurementTemplate)templates.get(i);
            
            if (template.getCategory().getName().equals(availCat) &&
                template.isDesignate()) {
                availIdx = idx;
            }
            
            // Need to get the raw measurements
            Collection args = template.getMeasurementArgs();
            MeasurementArg arg = (MeasurementArg)args.iterator().next();
            template = arg.getTemplateArg();

            if (idx == availIdx
                || (availIdx == -1 && idx < (SAMPLE_SIZE-1))
                || (availIdx != -1 && idx < SAMPLE_SIZE))
            {
                dsnList.add(template.getTemplate());
                // Increment only after we have successfully added DSN
                idx++;
                if (idx >= SAMPLE_SIZE) break;
            }
        }

        String[] dsns = (String[]) dsnList.toArray(new String[dsnList.size()]);
        entry = new ConfigCheckCacheEntry(dsns, availIdx);
        return updateCache(mType, entry);
    }

    private ConfigCheckCacheEntry updateCache (String mtype, 
                                               ConfigCheckCacheEntry entry) {
        synchronized (cache) {
            cache.put(mtype, entry);
        }
        return entry;
    }
}

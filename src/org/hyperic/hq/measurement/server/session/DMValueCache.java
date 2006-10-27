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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.collection.LULOMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A static LRU cache singleton object that can keep track of
 * DerivedMeasurementValues to prevent repetitive lookups
 * of derived measurement values for message processing.
 */
public class DMValueCache {
    private static Log log = LogFactory.getLog(DMValueCache.class);
    private static DMValueCache singleton = new DMValueCache();
    
    private static MBeanServer mServer = null;
    private static ObjectName cacheService = null;
    private static String FQN = "/hq/metricValues";

    private LULOMap cache;

    private DMValueCache() {}

    /**
     * Singleton accessor
     */
    public static DMValueCache getInstance() {
        if (singleton.cache == null &&
            (mServer == null || cacheService == null)) {
            try {
                cacheService = new ObjectName(HQConstants.JBOSSCACHE);
                mServer = MBeanUtil.getMBeanServer();

                // Test it
                mServer.invoke(cacheService, "exists",
                        new Object[] { FQN },
                        new String[] { String.class.getName() });
                
                log.debug("Using JBoss Cache for DMValueCache");
            } catch (Exception e) {
                // Fall back to using internal cache
                singleton.cache = new LULOMap(5000);

                log.debug("Using LULOMap for DMValueCache");
            }
        }
        
        return singleton;
    }

    /**
     * Get the derived measurement value object
     */
    public DerivedMeasurementValue get(Integer dmId) {
        if (this.cache != null)
            return (DerivedMeasurementValue) this.cache.get(dmId);
        
        try {
            DerivedMeasurementValue ret = (DerivedMeasurementValue)
                mServer.invoke(cacheService, "get",
                    new Object[] { FQN, dmId },
                    new String[] { String.class.getName(),
                                   Object.class.getName() });

            if (log.isDebugEnabled())
                log.debug("Retrieved DerivedMeasurementValue(" + dmId +
                          ") from JBoss Cache");
            
            return ret;
        } catch (InstanceNotFoundException e) {
            return null;
        } catch (MBeanException e) {
            if (log.isDebugEnabled())
                log.debug("JBoss Cache MBeanException", e);
            return null;
        } catch (ReflectionException e) {
            log.error("JBoss Cache ReflectionException for get");
            return null;
        }
    }

    public synchronized void put(DerivedMeasurementValue val) {
        if (this.cache != null)
            cache.put(val.getId(), val);
        else {
            try {
                mServer.invoke(cacheService, "put",
                        new Object[] { FQN, val.getId(), val },
                        new String[] { String.class.getName(),
                                       Object.class.getName(),
                                       Object.class.getName()});

                if (log.isDebugEnabled())
                    log.debug("Stored DerivedMeasurementValue(" + val.getId() +
                              ") in JBoss Cache");
                
            } catch (InstanceNotFoundException e) {
                // Do nothing
            } catch (MBeanException e) {
                // Do nothing
                if (log.isDebugEnabled())
                    log.debug("JBoss Cache MBeanException", e);
            } catch (ReflectionException e) {
                // Do nothing
                log.error("JBoss Cache ReflectionException for put");
            }
        }
    }

} 

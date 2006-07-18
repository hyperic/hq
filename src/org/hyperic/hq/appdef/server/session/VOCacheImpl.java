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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.util.collection.IntHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is meant to be a short term cache of value objects to
 * keep an in-memory map of value objects that have been recently 
 * retrieved.
 */
public class VOCacheImpl extends VOCache {

    private Log log = LogFactory.getLog(VOCacheImpl.class.getName());
    private IntHashMap platformCache;
    private IntHashMap platformTypeCache;
    private IntHashMap serverCache;
    private IntHashMap serverTypeCache;
    private IntHashMap serviceCache;
    private IntHashMap serviceTypeCache;
    private IntHashMap applicationCache;
    private IntHashMap groupCache;
    private IntHashMap platformLightCache;
    private IntHashMap serviceLightCache;
    private IntHashMap serverLightCache;
    
    public VOCacheImpl() {
        platformCache      = new IntHashMap();
        platformTypeCache  = new IntHashMap();
        platformLightCache = new IntHashMap();
        serverCache        = new IntHashMap(50);
        serverLightCache   = new IntHashMap(50);
        serverTypeCache    = new IntHashMap();
        serviceCache       = new IntHashMap(500);
        serviceLightCache  = new IntHashMap(500);
        serviceTypeCache   = new IntHashMap();
        applicationCache   = new IntHashMap();
        groupCache         = new IntHashMap();
    }

    /**
     * Put an inventory value object into the cache
     */
    public void put(Integer idObj, AppdefResourceValue vo) {
        int id = idObj.intValue();
        
        if( vo instanceof PlatformValue) {
            platformCache.put(id, vo);
            
            PlatformValue plt = (PlatformValue) vo;
            PlatformTypeValue type = plt.getPlatformType();
            platformTypeCache.put(type.getId().intValue(), type);
        }
        else if( vo instanceof ServerValue) {
            serverCache.put(id, vo);
            
            ServerValue svr = (ServerValue) vo;
            ServerTypeValue type = svr.getServerType();
            serverTypeCache.put(type.getId().intValue(), type);
        } 
        else if( vo instanceof ServiceValue) {
            serviceCache.put(id, vo);
            
            ServiceValue svc = (ServiceValue) vo;
            ServiceTypeValue type = svc.getServiceType();
            serviceTypeCache.put(type.getId().intValue(), type);
        }
        else if( vo instanceof ApplicationValue) {
            applicationCache.put(id, vo);
        }
        else if( vo instanceof AppdefGroupValue) {
            groupCache.put(id, vo);
        }
        else if( vo instanceof ServiceLightValue) {
            serviceLightCache.put(id, vo);
            
            ServiceLightValue svc = (ServiceLightValue) vo;
            ServiceTypeValue type = svc.getServiceType();
            serviceTypeCache.put(type.getId().intValue(), type);
        }
        else if( vo instanceof ServerLightValue) {
            serverLightCache.put(id, vo);
            
            ServerLightValue svr = (ServerLightValue) vo;
            ServerTypeValue type = svr.getServerType();
            serverTypeCache.put(type.getId().intValue(), type);
        }
        else if (vo instanceof PlatformLightValue) {
            platformLightCache.put(id, vo);
                        
            PlatformLightValue plt = (PlatformLightValue) vo;
            PlatformTypeValue type = plt.getPlatformType();
            platformTypeCache.put(type.getId().intValue(), type);
        }
        else {
            throw new IllegalArgumentException("Unsupported Type");
        }
    }

    /**
     * Put a catalog value object into the cache
     */
    public void put(Integer idObj, AppdefResourceTypeValue vo) {
        int id = idObj.intValue();
        
        if( vo instanceof PlatformTypeValue) {
            platformTypeCache.put(id, vo);
        }
        else if( vo instanceof ServerTypeValue) {
            serverTypeCache.put(id, vo);
        } 
        else if( vo instanceof ServiceTypeValue) {
            serviceTypeCache.put(id, vo);
        }
        else {
            throw new IllegalArgumentException("Unsupported Type");
        }
    }

    /** AppdefGroupValue Cache APIs **/
    public AppdefGroupValue getGroup(Integer id) {
        return (AppdefGroupValue) groupCache.get(id.intValue());
    }
    
    public ApplicationValue getApplication(Integer id) {
        return (ApplicationValue)applicationCache.get(id.intValue());
    }
    
    public PlatformLightValue getPlatformLight(Integer id) {
        PlatformLightValue plat =
            (PlatformLightValue) platformLightCache.get(id.intValue());
        
        if (plat != null && !platformTypeCache.containsKey(
               plat.getPlatformType().getId().intValue()))
            return null;
                                 
        return plat;
    }
    
    public PlatformValue getPlatform(Integer id) {
        PlatformValue plat = (PlatformValue) platformCache.get(id.intValue());
        
        if (plat != null && !platformTypeCache.containsKey(
               plat.getPlatformType().getId().intValue()))
            return null;
                                 
        return plat;
    }
    
    public PlatformTypeValue getPlatformType(Integer id) {
        return (PlatformTypeValue)platformTypeCache.get(id.intValue());
    }
    
    public ServerValue getServer(Integer id) {
        ServerValue svr = (ServerValue) serverCache.get(id.intValue());
        
        // If there were no server type, then it may have been changed
        if (svr != null && !serverTypeCache.containsKey(
                svr.getServerType().getId().intValue()))
            return null;
        
        return svr;
    }
    
    public ServerLightValue getServerLight(Integer id) {
        ServerLightValue svr =
            (ServerLightValue) serverLightCache.get(id.intValue());
        
        // If there were no server type, then it may have been changed
        if (svr != null && !serverTypeCache.containsKey(
                svr.getServerType().getId().intValue()))
            return null;
        
        return svr;
    }
    
    public ServerTypeValue getServerType(Integer id) {
        return (ServerTypeValue)serverTypeCache.get(id.intValue());
    }
    
    public ServiceValue getService(Integer id) {
        ServiceValue svc = (ServiceValue) serviceCache.get(id.intValue());
        
        // If there were no server type, then it may have been changed
        if (svc != null && !serviceTypeCache.containsKey(
                svc.getServiceType().getId().intValue()))
            return null;
        
        return svc;
    }
    
    public ServiceLightValue getServiceLight(Integer id) {
        ServiceLightValue svc =
            (ServiceLightValue) serviceLightCache.get(id.intValue());
        
        // If there were no server type, then it may have been changed
        if (svc != null && !serviceTypeCache.containsKey(
                svc.getServiceType().getId().intValue()))
            return null;
        
        return svc;
    }

    public ServiceTypeValue getServiceType(Integer id) {
        return (ServiceTypeValue)serviceTypeCache.get(id.intValue());
    }
    
    public void removeGroup(Integer id, boolean notifyHA) {
        if (id.intValue() == 0)
            removeAllGroups(false);
        else {
            synchronized(getGroupLock()) {
                log.debug("Removing group from VOCache: " + id);
                groupCache.remove(id.intValue());
            }
        }
    }

    public void removeAllGroups(boolean notifyHA) {
        if (groupCache.size() > 0) {
            log.debug("Removing all groups from VOCache");
            synchronized(getGroupLock()) {
                groupCache.clear();
            }
        }
    }

    public void removeApplication(Integer id, boolean notifyHA) {
        synchronized(getApplicationLock()) {
            log.debug("Removing application from VOCache: " + id);
            applicationCache.remove(id.intValue());
        }
    }

    public void removePlatform(Integer id, boolean notifyHA) {
        log.debug("Attempting to get exclusive platform lock");
        synchronized(getPlatformLock()) {
            log.debug("Removing platform from VOCache: " + id);
            platformCache.remove(id.intValue());
            platformLightCache.remove(id.intValue());
        }
        log.debug("Release platform lock");
    }

    public void removePlatformType(Integer id, boolean notifyHA) {
        log.debug("Attempting to get exclusive platform type lock");
        synchronized(getPlatformTypeLock()) {
            log.debug("Removing platformType from VOCache: " + id);
            platformTypeCache.remove(id.intValue());
        }
        log.debug("Release platform type lock");
    }

    public void removeServer(Integer id, boolean notifyHA) {
        log.debug("Attempting to get server lock");
        synchronized(getServerLock()) {
            log.debug("Removing server from VOCache: " + id);
            serverCache.remove(id.intValue());
            serverLightCache.remove(id.intValue());
        }
        log.debug("Released server lock");
    }

    public void removeServerType(Integer id, boolean notifyHA) {
        log.debug("Attempting to get server type lock");
        synchronized(getServerTypeLock()) {
            log.debug("Removing serverType from VOCache: " + id);
            serverTypeCache.remove(id.intValue());
        }
        log.debug("Released server type lock");
    }

    public void removeService(Integer id, boolean notifyHA) {
        log.debug("Attempting to get service lock");
        synchronized(getServiceLock()) {
            log.debug("Removing service from VOCache: " + id);
            serviceCache.remove(id.intValue());
            serviceLightCache.remove(id.intValue());
        }
        log.debug("Released service lock");
    }

    public void removeServiceType(Integer id, boolean notifyHA) {
        synchronized(getServiceTypeLock()) {
            log.debug("Removing serviceType from VOCache: " + id);
            serviceTypeCache.remove(id.intValue());
        }
    }
}

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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationLocal;
import org.hyperic.hq.appdef.shared.ApplicationPK;
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
import org.hyperic.hq.common.shared.ProductProperties;

/**
 * This class is meant to be a short term cache of value objects to
 * keep an in-memory map of value objects that have been recently 
 * retrieved.
 */
public abstract class VOCache {
    private static VOCache singleton;

    public static VOCache getInstance() {
        if (singleton == null) {
            singleton = (VOCache) ProductProperties
                .getPropertyInstance("hyperic.hq.appdef.vocache");

            if (singleton == null)
                singleton = new VOCacheImpl();
        }
        return singleton;
    }

    private Object serviceLock = new Object();

    private Object groupLock = new Object();

    private Object platformLock = new Object();

    private Object platformTypeLock = new Object();

    private Object serverLock = new Object();

    private Object applicationLock = new Object();

    private Object serverTypeLock = new Object();

    private Object serviceTypeLock = new Object();
    
    /**
     * Put an inventory value object into the cache
     */
    public abstract void put(Integer idObj, AppdefResourceValue vo);

    /**
     * Put a catalog value object into the cache
     */
    public abstract void put(Integer idObj, AppdefResourceTypeValue vo);

    /** ApplicationValue Cache APIs **/
    
    public ApplicationValue getApplication(ApplicationLocal ejb) {
        return this.getApplication(((ApplicationPK)ejb.getPrimaryKey()).getId()); 
    }
    
    public abstract ApplicationValue getApplication(Integer id);

    /** AppdefGroupValue Cache APIs **/
    public abstract AppdefGroupValue getGroup(Integer id);

    public abstract PlatformValue getPlatform(Integer id);

    public abstract PlatformLightValue getPlatformLight(Integer id);

    public abstract PlatformTypeValue getPlatformType(Integer id);

    public abstract ServerLightValue getServerLight(Integer id);

    public abstract ServerValue getServer(Integer id);

    public abstract ServerTypeValue getServerType(Integer id);

    public abstract ServiceValue getService(Integer id);

    public abstract ServiceLightValue getServiceLight(Integer id);

    public abstract ServiceTypeValue getServiceType(Integer id);

    public abstract void removeAllGroups(boolean notifyHA);

    public abstract void removeGroup(Integer id, boolean notifyHA);

    public abstract void removeApplication(Integer id, boolean notifyHA);

    public abstract void removePlatform(Integer id, boolean notifyHA);

    public abstract void removePlatformType(Integer id, boolean notifyHA);

    public abstract void removeServer(Integer id, boolean notifyHA);

    public abstract void removeServerType(Integer id, boolean notifyHA);

    public abstract void removeService(Integer id, boolean notifyHA);

    public abstract void removeServiceType(Integer id, boolean notifyHA);

    public void removeEntity(AppdefEntityID id) {
        int type = id.getType();
        switch (type) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM):
                this.removePlatform(id.getId());
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER):
                this.removeServer(id.getId());
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_SERVICE):
                this.removeService(id.getId());
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION):
                this.removeApplication(id.getId());
                break;
            case (AppdefEntityConstants.APPDEF_TYPE_GROUP):
                this.removeGroup(id.getId());
                break;    
            default:
                throw new IllegalArgumentException("Unsupported Type");        
        }
    }

    public void removeGroup(Integer id) {
        removeGroup(id, true);
    }

    public void removeAllGroups() {
        removeAllGroups(true);
    }

    public void removeApplication(Integer id) {
        removeApplication(id, true);
    }

    public void removePlatform(Integer id) {
        removePlatform(id, true);
    }

    public void removePlatformType(Integer id) {
        removePlatformType(id, true);
    }

    public void removeServer(Integer id) {
        removeServer(id, true);
    }

    public void removeServerType(Integer id) {
        removeServerType(id, true);
    }

    public void removeService(Integer id) {
        removeService(id, true);
    }

    public void removeServiceType(Integer id) {
        removeServiceType(id, true);
    }

    public Object getGroupLock() {
        return groupLock;
    }

    public Object getApplicationLock() {
        return this.applicationLock;
    }

    public Object getPlatformLock() {
        return this.platformLock;
    }

    public Object getPlatformTypeLock() {
        return this.platformTypeLock;
    }

    public Object getServerLock() {
        return this.serverLock;
    }

    public Object getServerTypeLock() {
        return this.serverTypeLock;
    }

    public Object getServiceLock() {
        return this.serviceLock;
    }

    public Object getServiceTypeLock() {
        return this.serviceTypeLock;
    }
}

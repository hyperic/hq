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

package org.hyperic.hq.authz.server.session;

import java.util.HashMap;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.util.collection.IntHashMap;

/**
 * This class is meant to be a short term cache of value objects to
 * keep an in-memory map of value objects that have been recently 
 * retrieved.
 */
public class VOCache {

    private IntHashMap resourceCache;
    private HashMap    resourceTypeCache;
    private HashMap    subjectCache;

    private Object resourceLock = new Object();
    private Object resourceTypeLock = new Object();
    private Object subjectLock = new Object();

    private static VOCache singleton = new VOCache();
    
    private VOCache() {
        resourceCache     = new IntHashMap();
        resourceTypeCache = new HashMap();
        subjectCache      = new HashMap();
    }

    /**
     * Put an inventory value object into the cache
     */
    public void put(Integer id, ResourceValue vo) {
        resourceCache.put(id.intValue(), vo);
    }

    public ResourceValue getResource(Integer id) {
        return (ResourceValue)resourceCache.get(id.intValue());
    }
    
    public void removeResource(Integer id) {
        synchronized(getResourceLock()) {
            resourceCache.remove(id.intValue());
        }
    }

    public Object getResourceLock() {
        return this.resourceLock;
    }

    /**
     * Put an inventory value object into the cache
     */
    public void put(String name, ResourceTypeValue vo) {
        resourceTypeCache.put(name, vo);
    }

    public ResourceTypeValue getResourceType(String name) {
        return (ResourceTypeValue)resourceTypeCache.get(name);
    }
    
    public void removeResourceType(String name) {
        synchronized(getResourceTypeLock()) {
            resourceTypeCache.remove(name);
        }
    }   

    public Object getResourceTypeLock() {
        return this.resourceTypeLock;
    }

    /**
     * Put an inventory value object into the cache
     */
    public void put(String name, AuthzSubjectValue vo) {
        subjectCache.put(name, vo);
    }

    public AuthzSubjectValue getAuthzSubject(String name) {
        return (AuthzSubjectValue)subjectCache.get(name);
    }

    public void removeSubject(String name) {
        synchronized(getSubjectLock()) {
            subjectCache.remove(name);
        }
    }

    public Object getSubjectLock() {
        return this.subjectLock;
    }

    public static VOCache getInstance() {
        return singleton;
    }
    
}

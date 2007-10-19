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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServiceValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An abstract class which all appdef value objects inherit from
 * inheritance is achieved by using:
 * <code>@ejb:value-object name="SomeEntity" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"</code>
 * 
 * The accessors provided in this class represent what the UI model labels
 * "General Properties". Any other attribute is assumed to be specific
 * to the resource type.
 *
 *
 */
public abstract class AppdefResourceValue
    implements Serializable, Comparable
{

    // they all have id's
    public abstract Integer getId();
    public abstract void setId(Integer id);

    // they all have names
    public abstract String getName();
    public abstract void setName(String name);

    // they all have owners;
    public abstract String getOwner();
    public abstract void setOwner(String owner);

    // they all have modifiers
    public abstract String getModifiedBy();
    public abstract void setModifiedBy(String modifiedBy);

    // they all have descriptions
    public abstract String getDescription();
    public abstract void setDescription(String desc);

    // they all have ctime 
    public abstract Long getCTime();
    // they all have mtime
    public abstract Long getMTime();

    // they all have location
    public abstract String getLocation();
    public abstract void setLocation(String loc);

    // Storage for host name
    private String hostName = null;    
    public String getHostName() {
        return hostName;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    /**
     * get an entity ID for the object
     */
    public abstract AppdefEntityID getEntityId();

    // get a map of resource types and instances
    private static Map getResourceTypeMap(int mapType, Collection objColl) {
        Map aMap = new HashMap();
        switch (mapType) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                for(Iterator i = objColl.iterator();i.hasNext();) {
                    ServerLightValue aServer = (ServerLightValue)i.next();
                    ServerTypeValue aType = aServer.getServerType();
                    if (!aMap.containsKey(aType.getName())) { 
                        // collection needs to be initialized
                        List aList = new ArrayList();
                        aList.add(aServer);
                        aMap.put(aType.getName(), aList);
                    } else {
                        // add it to the collection
                        ((List)aMap.get(aType.getName())).add(aServer);
                    }
                }
                break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    for(Iterator i = objColl.iterator();i.hasNext();) {
                    ServiceLightValue aService = (ServiceLightValue)i.next();
                    ServiceTypeValue aType = aService.getServiceType();
                    if (!aMap.containsKey(aType.getName())) { 
                        // collection needs to be initialized
                        List aList = new ArrayList();
                        aList.add(aService);
                        aMap.put(aType.getName(), aList);
                    } else {
                        // add it to the collection
                        ((List)aMap.get(aType.getName())).add(aService);
                    }
                }
                break;

            default:
                throw new NoSuchElementException("Unrecognized type: " + 
                                                 mapType);
        }
        return aMap;
    }

    /**
     * Get a map of server types from this collection of serverlightvalues
     * @param a collection of <code>ServerLightValue</code> objects
     * @return map with key: serverTypeValue, value: a <code>List</code> 
     * of ServerLightValues matching that type
     */
    public static Map getServerTypeMap(Collection serverColl) {
        return getResourceTypeMap(
            AppdefEntityConstants.APPDEF_TYPE_SERVER, serverColl);
    }

    /**
     * Get a map of Service types from this collection of ServiceLightValue
     * @param a collection of <code>ServiceLightValue</code> objects
     * @return map with key: serviceTypeValue, value: a <code>List</code> 
     * of ServiceLightValues matching that type
     */
    public static Map getServiceTypeMap(Collection serviceColl) {
        return getResourceTypeMap(AppdefEntityConstants.APPDEF_TYPE_SERVICE, 
                                  serviceColl);
    }

    // get a map of resource types and instances
    public static Map getResourceTypeCountMap(Collection objColl) {
        Map aMap = new HashMap();
                
        // try using the AppdefResourceValue & AppdefResourceTypeValue
        for(Iterator i = objColl.iterator();i.hasNext();) {
            AppdefResourceValue rVal = (AppdefResourceValue)i.next();
            AppdefResourceTypeValue rType = rVal.getAppdefResourceTypeValue();
            if (!aMap.containsKey(rType.getName())) { 
                // count needs to be initialized
                aMap.put(rType.getName(), new Integer(1));
            } else {
                // increment the count
                int count =
                    ((Integer) aMap.get(rType.getName())).intValue();
                aMap.put(rType.getName(), new Integer(++count));
            }
        }
        return aMap;
    }

    /** 
     * Get an upcasted reference to our resource type value.
     * @return the "type value" value object upcasted to its
     *         abstract base class for use in agnostic context.
     */
    public AppdefResourceTypeValue getAppdefResourceTypeValue () {
        int entityType = this.getEntityId().getType();
        switch(entityType) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                if (this instanceof PlatformValue)
                    return ((PlatformValue)this).getPlatformType();
                else 
                    return ((PlatformLightValue)this).getPlatformType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                if (this instanceof ServerValue)
                    return ((ServerValue)this).getServerType();
                else
                    return ((ServerLightValue)this).getServerType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                if (this instanceof ServiceValue)
                    return ((ServiceValue)this).getServiceType();
                else
                    return ((ServiceLightValue)this).getServiceType();
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return ((ApplicationValue)this).getApplicationType();
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                if (this instanceof ServiceClusterValue)
                    return ((ServiceClusterValue) this).getServiceType();
                    
                return ((AppdefGroupValue) this).getAppdefResourceTypeValue();
            default:
                throw new InvalidAppdefTypeException
                  ("Unrecognized appdef type:" +entityType);
        }
    }

    /**
     * Get a map of platform types from this collection of
     * PlatformLightValue objects.
     *
     * @param platformColl collection of <code>PlatformLightValue</code> objects
     * @return map with key: platformTypeValue, value: a <code>List</code> 
     * of PlatformLightValues matching that type
     */
    public static Map getPlatformTypeCountMap(Collection platformColl) {
        return getResourceTypeCountMap(platformColl);
    }

    /**
     * Get a map of server types from this collection of serverlightvalues
     * @param a collection of <code>ServerLightValue</code> objects
     * @return map with key: serverTypeValue, value: a <code>List</code> 
     * of ServerLightValues matching that type
     */
    public static Map getServerTypeCountMap(Collection serverColl) {
        // remove any virtual servers
        Collection nonVirtual = new ArrayList(serverColl.size());
        
        for (Iterator i = serverColl.iterator(); i.hasNext();) {
            AppdefResourceValue av = (AppdefResourceValue)i.next();
            ServerTypeValue st =
                (ServerTypeValue) av.getAppdefResourceTypeValue();

            if (!st.getVirtual())
                nonVirtual.add(av);
        }

        return getResourceTypeCountMap(nonVirtual);
    }

    /**
     * Get a map of Service types from this collection of ServiceLightValue
     * @param a collection of <code>ServiceLightValue</code> objects
     * @return map with key: serviceTypeValue, value: a <code>List</code> 
     * of ServiceLightValues matching that type
     */
    public static Map getServiceTypeCountMap(Collection serviceColl) {
        return getResourceTypeCountMap(serviceColl);
    }
    
    public int compareTo(Object arg0) {
        if (!(arg0 instanceof AppdefResourceValue))
            return -1;
            
        return this.getName().compareTo(((AppdefResourceValue) arg0).getName());
    }
}

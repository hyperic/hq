/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.context.Bootstrap;

/**
 * An abstract class which all appdef value objects inherit from
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
    /**
     * @deprecated
     */
    @Deprecated
    public static Map<String,Integer> getResourceTypeCountMap(Collection objColl) {
        Map<String, Integer> aMap = new HashMap<String, Integer>();
                
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
                    aMap.get(rType.getName()).intValue();
                aMap.put(rType.getName(), new Integer(++count));
            }
        }
        return aMap;
    }

    /** 
     * Get an upcasted reference to our resource type value.
     * @return the "type value" value object upcasted to its
     *         abstract base class for use in agnostic context.
     * @deprecated
     */
    @Deprecated
    public AppdefResourceTypeValue getAppdefResourceTypeValue () {
        int entityType = this.getEntityId().getType();
        switch(entityType) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return ((PlatformValue)this).getPlatformType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                if (this instanceof ServerValue)
                    return ((ServerValue)this).getServerType();
                else
                    return ((ServerLightValue)this).getServerType();
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return ((ServiceValue)this).getServiceType();
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return ((ApplicationValue)this).getApplicationType()
                    .getAppdefResourceTypeValue();
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
     * @deprecated
     */
    @Deprecated
    public static Map getPlatformTypeCountMap(Collection platformColl) {
        return getResourceTypeCountMap(platformColl);
    }

    /**
     * Get a map of server types from this collection of serverlightvalues
     * @param a collection of <code>ServerLightValue</code> objects
     * @return map with key: serverTypeValue, value: a <code>List</code> 
     * of ServerLightValues matching that type
     * @deprecated
     */
    @Deprecated
    public static Map<String,Integer> getServerTypeCountMap(Collection<? extends AppdefResourceValue> serverColl) {
        // remove any virtual servers
        Collection<AppdefResourceValue> nonVirtual = new ArrayList<AppdefResourceValue>(serverColl.size());
        
        for (AppdefResourceValue av : serverColl) {
            ServerTypeValue st =
                (ServerTypeValue) av.getAppdefResourceTypeValue();
            if (!st.getVirtual()) {
                nonVirtual.add(av);
            }
        }

        return getResourceTypeCountMap(nonVirtual);
    }

    /**
     * Get a map of Service types from this collection of ServiceLightValue
     * @param a collection of <code>ServiceLightValue</code> objects
     * @return map with key: serviceTypeValue, value: a <code>List</code> 
     * of ServiceLightValues matching that type
     * @deprecated
     */
    @Deprecated
    public static Map<String,Integer> getServiceTypeCountMap(Collection serviceColl) {
        return getResourceTypeCountMap(serviceColl);
    }
    
    public int compareTo(Object arg0) {
        if (!(arg0 instanceof AppdefResourceValue))
            return -1;
            
        return this.getName().compareTo(((AppdefResourceValue) arg0).getName());
    }
    
    /**
     * @deprecated
     */
    @Deprecated
    public static AppdefResourceType getAppdefResourceType(AuthzSubject subject, ResourceGroup group) {
        if (group.isMixed())
            throw new IllegalArgumentException("Group " + group.getId() +
                                               " is a mixed group");
        return getResourceTypeById(group.getGroupEntType().intValue(),
                                   group.getGroupEntResType().intValue());
    }
    
    @Override
    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs instanceof AppdefResourceValue) {
            AppdefEntityID aeid = (AppdefEntityID) rhs;
            return getEntityId().equals(aeid);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return getEntityId().hashCode();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public AppdefResourceTypeValue getAppdefResourceTypeValue(AuthzSubject subject, ResourceGroup group) {
        if (group.isMixed()) {
            AppdefResourceTypeValue res = new GroupTypeValue();
            int iGrpType = group.getGroupType().intValue();
            res.setId(group.getGroupType());
            res.setName(AppdefEntityConstants.getAppdefGroupTypeName(iGrpType));
            return res;
        } else {
            return getAppdefResourceType(subject, group).getAppdefResourceTypeValue();
        }
    }
    
    
    /**
     * This method is a helper method to bridge the gap btwn Resources and AppdefResourceValue.  AppdefResourceValues
     * should not be used unless it is for backwards compatibility
     * @deprecated
     */
    @Deprecated
    public static AppdefResourceValue convertToAppdefResourceValue(Resource resource) {
        if (resource == null) {
            return null;
        }
        final int type = resource.getResourceType().getId();
        if (type == AuthzConstants.authzPlatform) {
            return getPlatformById(resource);
        } else if (type == AuthzConstants.authzServer) {
            return getServerById(resource);
        } else if (type == AuthzConstants.authzService) {
            return getServiceById(resource);
        } else if (type == AuthzConstants.authzApplication) {
            return getApplicationById(resource);
        } else if (type == AuthzConstants.authzGroup) {
            return getGroupById(resource);
        } else {
            //HHQ- Guys return null so that appdef types unsupported by the authorization mechanizm would be 
            //filters by the permissionManagear. 
            return null ;
        }
    }

    private static AppdefResourceValue getGroupById(Resource r) {
        final ResourceGroup group = getGroupManager().getGroupById(r.getInstanceId());
        if (group == null) {
            return null;
        }
        return group.getAppdefResourceValue();
    }

    private static AppdefResourceValue getApplicationById(Resource r) {
        final Application application = getApplicationManager().getApplicationById(r.getInstanceId());
        if (application == null) {
            return null;
        }
        return application.getAppdefResourceValue();
    }
    
    private static AppdefResourceValue getServiceById(Resource r) {
        final Service service = getServiceManager().getServiceById(r.getInstanceId());
        if (service == null) {
            return null;
        }
        return service.getAppdefResourceValue();
    }
    
    private static AppdefResourceValue getServerById(Resource r) {
        final Server server = getServerManager().getServerById(r.getInstanceId());
        if (server == null) {
            return null;
        }
        return server.getAppdefResourceValue();
    }
    
    private static AppdefResourceValue getPlatformById(Resource r) {
        final Platform platform = getPlatformManager().getPlatformById(r.getInstanceId());
        if (platform == null) {
            return null;
        }
        return platform.getAppdefResourceValue();
    }

    private static ResourceGroupManager getGroupManager() {
        return Bootstrap.getBean(ResourceGroupManager.class);
    }

    private static ApplicationManager getApplicationManager() {
        return Bootstrap.getBean(ApplicationManager.class);
    }

    private static ServiceManager getServiceManager() {
        return Bootstrap.getBean(ServiceManager.class);
    }

    private static ServerManager getServerManager() {
        return Bootstrap.getBean(ServerManager.class);
    }

    private static PlatformManager getPlatformManager() {
        return Bootstrap.getBean(PlatformManager.class);
    }

    private static AppdefResourceType getResourceTypeById(int type, int id) {
        switch (type) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM):
                return getPlatformTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER):
                return getServerTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVICE):
                return getServiceTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION):
                return getApplicationTypeById(id);
            default:
                throw new IllegalArgumentException("Invalid resource type:" + type);
        }
    }
    
    private static PlatformType getPlatformTypeById(int id) {
        return Bootstrap.getBean(PlatformManager.class).findPlatformType(new Integer(id));
    }

    private static ServerType getServerTypeById(int id) {
        return Bootstrap.getBean(ServerManager.class).findServerType(new Integer(id));
    }

    private static ServiceType getServiceTypeById(int id) {
        return Bootstrap.getBean(ServiceManager.class).findServiceType(new Integer(id));
    }

    private static ApplicationType getApplicationTypeById(int id) {
        return Bootstrap.getBean(ApplicationManager.class).findApplicationType(new Integer(id));
    }
    
   
    
}

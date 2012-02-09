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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceManagerImpl;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * A class which handles fetching of data common to appdef entities.
 * This class should only be used from within the server.  Really 
 * this is just a superclass which is not.  </Harrie>
 */

public class AppdefEntityValue {
    private PlatformManager    platformManagerLocal;
    private ApplicationManager applicationManagerLocal;
    private ServerManager      serverManagerLocal;
    private ServiceManager     serviceManagerLocal;

    private Application             application = null;
    private Platform                _platform    = null;
    private Server                  server      = null;
    private Service                 service     = null;
    private AppdefGroupValue        group       = null;

    private AppdefEntityID          _id;
    private AuthzSubject            _subject;

    public AppdefEntityValue(AppdefEntityID id, AuthzSubject subject) {
        _id      = id;
        _subject = subject;
    }
    
    public AppdefEntityValue(AuthzSubject subject, Platform platform) {
        this(platform.getEntityId(), subject);
        _platform = platform;
    }
    
    public AppdefEntityID getID() {
        return _id;
    }

    public AuthzSubject getSubject() {
        return _subject;
    }
    
    private PlatformManager getPlatformManager() {
        if (platformManagerLocal == null) {
            platformManagerLocal = Bootstrap.getBean(PlatformManager.class);
        }
        return platformManagerLocal;
    }

    private ServerManager getServerManager() {
        if(serverManagerLocal == null){
            serverManagerLocal = Bootstrap.getBean(ServerManager.class);
        }
        return serverManagerLocal;
    }

    private ServiceManager getServiceManager() {
        if(serviceManagerLocal == null){
            serviceManagerLocal = Bootstrap.getBean(ServiceManager.class);
        }
        return serviceManagerLocal;
    }

    private ApplicationManager getApplicationManager() {
        if (applicationManagerLocal == null) {
            applicationManagerLocal = Bootstrap.getBean(ApplicationManager.class);
        }
        return applicationManagerLocal;
    }

    private Platform getPlatform(boolean permCheck)
        throws AppdefEntityNotFoundException, PermissionException {
        if (_platform == null) {
            if (permCheck) {
                _platform = getPlatformManager().getPlatformById(getSubject(),
                                                                _id.getId());
            }
            else {
                _platform = getPlatformManager().findPlatformById(_id.getId());
            }
        }
        
        return _platform;
    }
    
    private Application getApplication()
        throws AppdefEntityNotFoundException, ApplicationNotFoundException,
               PermissionException {
        if(application == null){
            application =
                getApplicationManager().findApplicationById(getSubject(),
                                                            _id.getId());
        } 
        return application;
    }
    
    private Server getServer(boolean permCheck)
        throws AppdefEntityNotFoundException, PermissionException {
        if (server == null) {
            if (permCheck) {
                server = getServerManager().getServerById(getSubject(),
                                                          _id.getId());
            }
            else {
                server = getServerManager().findServerById(_id.getId());
            }
        } 
        
        return server;
    }

    private Service getService(boolean permCheck)
        throws AppdefEntityNotFoundException, PermissionException {
        if (service == null) {
            if (permCheck) {
                service = getServiceManager().getServiceById(getSubject(),
                                                              _id.getId());
            } else {
                service = getServiceManager().findServiceById(_id.getId());
            }
        }

        return service;
    }

    private ResourceGroup getGroup()
        throws PermissionException, AppdefGroupNotFoundException {
        ResourceGroupManager groupMan = 
           Bootstrap.getBean(ResourceGroupManager.class);
        ResourceGroup g = 
            groupMan.findResourceGroupById(getSubject(), _id.getId());
        if (g == null) {
            throw new AppdefGroupNotFoundException("Unable to find " + 
                                                   "group [" + _id + "]");
        }
        return g;
    }
    
    private AppdefGroupValue getGroup(boolean full)
        throws PermissionException, AppdefGroupNotFoundException 
    {
        if (group == null) {
            ResourceGroupManager groupMan = 
               Bootstrap.getBean(ResourceGroupManager.class);
            group = groupMan.getGroupConvert(getSubject(), getGroup());
        }
        return group;
    }
    
    private PageList getGroupAppdefEntityValues(int groupAppdefType,
                                                PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {
        AppdefGroupValue group = getGroup(true);
        AppdefResourceTypeValue resType = group.getAppdefResourceTypeValue();

        List eids = group.getAppdefGroupEntries();
        if (eids.size() == 0)
            return new PageList();

        switch (groupAppdefType) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            if (resType instanceof ServerTypeValue)
                return getPlatformManager()
                    .getPlatformsByServers(getSubject(), eids);
            
            if (!(resType instanceof PlatformTypeValue))
                throw new IllegalArgumentException(_id +
                    " group is not a valid platform compatible group");
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            if (resType instanceof ServiceTypeValue) {
                return getServerManager()
                    .getServersByServices(getSubject(), eids);
            } 

            if (!(resType instanceof ServerTypeValue))
                throw new IllegalArgumentException(_id +
                    " group is not a valid server compatible group");
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            if (!(resType instanceof ServiceTypeValue))
                throw new IllegalArgumentException(_id +
                    " group is not a valid service compatible group");
            break;
        default:
            throw new IllegalArgumentException(
                "Cannot get " + groupAppdefType + " type from group");
        }
        
        // Now fetch the resources in this group
        PageList res = new PageList();
        for (Iterator it = eids.iterator(); it.hasNext(); ) {
            AppdefEntityID eid = (AppdefEntityID) it.next();
            AppdefEntityValue val = new AppdefEntityValue(eid, getSubject());
            res.add(val.getResourceValue());
        }

        res.setTotalSize(res.size());
        return res;
    }

    public String getMonitorableType()
        throws PermissionException, AppdefEntityNotFoundException {
        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getTypeName();
        default:
            throw new IllegalArgumentException(_id.getTypeName() + 
                                               " does not support measurement");
        }
    }

    public String getName()
        throws PermissionException, AppdefEntityNotFoundException {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getResourcePOJO().getName();
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroup().getName();
        default:
            throw new IllegalStateException("Unknown appdef entity type"); 
        }
    }

    public String getDescription()
        throws PermissionException, AppdefEntityNotFoundException {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getResourcePOJO().getDescription();
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroup().getDescription();
        default:
            throw new IllegalStateException("Unknown appdef entity type"); 
        }
    }

    public String getTypeName()
        throws AppdefEntityNotFoundException, PermissionException
    {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            ResourceGroup g = getGroup();
            if (g.isMixed()) {
                return getGroup(false).getAppdefResourceTypeValue().getName();
            }
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getAppdefResourceType().getName();
        default:
            throw new IllegalStateException("Unknown appdef entity type");
        }
    }

    /**
     * Get the POJO object for a given AppdefEntityID.  Groups are not supported
     */
    public AppdefResource getResourcePOJO()
        throws PermissionException, AppdefEntityNotFoundException {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return getPlatform(false);
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return getServer(false);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return getService(false);
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getApplication();
        default:
            throw new IllegalStateException("Appdef entity type not supported "
                                            + _id);
        }
    }
    
    /**
     * Get the AppdefResourceValue for a given AppdefEntityID.  This can also
     * be used to validate that a given AppdefEntityID is valid.
     */
    public AppdefResourceValue getResourceValue()
        throws PermissionException, AppdefEntityNotFoundException {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return getPlatform(true).getPlatformValue();
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return getServer(true).getServerValue();
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return getService(true).getServiceValue();
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getApplication().getApplicationValue();
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroup(true);
        default:
            throw new IllegalStateException("Unknown appdef entity type");
        }
    }

    /**
     * Get the AppdefResourceType POJO
     */
    public AppdefResourceType getAppdefResourceType()
        throws PermissionException, AppdefEntityNotFoundException 
    {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getResourcePOJO().getAppdefResourceType();
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            // Make sure this is a compatible group
            ResourceGroup g = getGroup();
            if (g.isMixed())
                throw new IllegalStateException("Can't return for mixed group: "
                                                + _id);
       
            return AppdefResourceValue.getAppdefResourceType(getSubject(), g);
        default:
            throw new IllegalStateException("Unknown appdef entity type");
        }
    }
    
   
   
    /**
     * Get the AppdefGroupValue if this was a group
     */
    public AppdefGroupValue getAppdefGroupValue()
        throws AppdefGroupNotFoundException, PermissionException {
        if (!_id.isGroup())
            throw new IllegalStateException("Appdef entity type is not a group "
                                            + _id);
        return getGroup(false);
    }

    /**
     * @deprecated
     */
    public PageList<AppdefResourceValue> getAssociatedPlatforms(PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException {
        Integer iId = _id.getId();

        PlatformManager pManager = getPlatformManager();
        PageList<AppdefResourceValue> res = new PageList<AppdefResourceValue>();
        if(_id.isApplication()){
            res.addAll(pManager.getPlatformsByApplication(getSubject(), iId, pc));
            return res;
        }

       
        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res.add(pManager.getPlatformByService(getSubject(), iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            res.add(pManager.getPlatformByServer(getSubject(), iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            res.add(getResourceValue());        // Add self
            break;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroupAppdefEntityValues(
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM, pc);
        default:
            throw new IllegalArgumentException(
                _id.getTypeName() + 
                " type does not have valid platform association");
        }
        res.setTotalSize(1);
        return res;
    }

    // validate that our group type matches the argument - throw
    // illegal argument since this is a programmer error.
    private void validateGroupType (int groupTypeId) 
        throws PermissionException, AppdefEntityNotFoundException { 
        ResourceGroup g = getGroup();
        if (g.getGroupType().intValue() != groupTypeId) {
            throw new IllegalArgumentException("Invalid group type."+
                "Expecting type:"+AppdefEntityConstants
                .getAppdefGroupTypeName(groupTypeId));
        }
    }

    /** Get the servers associated with this resource
     * @param pc the page control object
     * @return a PageList of ServiceValue's
     * @deprecated
     */
    public PageList getAssociatedServers(PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException {
        ServerManager sManager;
        PageList res;
        Integer iId;

        iId      = _id.getId();
        sManager = getServerManager();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return sManager.getServersByApplication(getSubject(), iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return sManager.getServersByPlatform(getSubject(), iId, true, pc);
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroupAppdefEntityValues(
                 AppdefEntityConstants.APPDEF_TYPE_SERVER, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res = new PageList();
            res.add(sManager.getServerByService(getSubject(), iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            res = new PageList();
            res.add(getResourceValue());
            break;
        default :
            throw new IllegalArgumentException(_id.getTypeName() + 
                                               " type does not have " +
                                               "valid server association");
        }
        res.setTotalSize(1);
        return res;
    }

    /** Get the servers of a specific type associated with this resource
     * @param pc the page control object
     * @return a PageList of ServiceValue's
     * @deprecated
     */
    public PageList getAssociatedServers(Integer typeId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {
        ServerManager sManager;
        PageList res;
        Integer iId;

        iId      = _id.getId();
        sManager = getServerManager();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            res =
                sManager.getServersByApplication(getSubject(), iId, typeId, pc);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            // default to exclude virtual servers
            res = sManager.getServersByPlatform(getSubject(), iId, typeId, true,
                                                pc);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            res = getAssociatedServers(pc);
            break;
        default:
            throw new IllegalArgumentException(_id.getTypeName() + 
                                               " type does not have " +
                                               "valid server association");
        }
        return res;
    }

    /** Get the AppdefEntityIDs of servers of a specific type associated with
     * this resource
     * @param pc the page control object
     * @return a PageList of ServiceValue's
     * @deprecated
     */
    public List getAssociatedServerIds(Integer typeId)
        throws AppdefEntityNotFoundException, PermissionException {
        ServerManager sManager;
        Integer[] ids;
        Integer iId;
    
        iId      = _id.getId();
        sManager = getServerManager();
    
        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            ids = sManager.getServerIdsByApplication(getSubject(), iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            ids = sManager.getServerIdsByPlatform(getSubject(), iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            ServerValue server = sManager.getServerByService(getSubject(), iId);
            ids = new Integer[] { server.getId() };
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            ids = new Integer[] { iId };
            break;
        default:
            throw new IllegalArgumentException(_id.getTypeName() + 
                                               " type does not have " +
                                               "valid server association");
        }
        
        List entIds = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            entIds.add(AppdefEntityID.newServerID(ids[i]));
        }
        return entIds;
    }

    /** Get the services associated with this resource
     * @return a PageList of ServiceValue's
     * @deprecated
     */
    public PageList getAssociatedServices(PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException,
               ApplicationNotFoundException {
        ServiceManager sManager;
        PageList res;
        Integer iId;
        
        sManager = getServiceManager();
        iId      = _id.getId();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
            return sManager.getServiceInventoryByApplication(getSubject(), iId,
                                                             pc);
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            validateGroupType(
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);
            return getGroupAppdefEntityValues(
                AppdefEntityConstants.APPDEF_TYPE_SERVICE, pc);
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return sManager.getPlatformServices(getSubject(), iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVER :
            return sManager.getServicesByServer(getSubject(), iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res = sManager.getServicesByService(getSubject(), iId, pc);
            res.add(getResourceValue());     // Also add self
            res.setTotalSize(res.size());
            return res;
        default :
            throw new IllegalArgumentException(_id.getTypeName() + 
                " type does not have valid service associations");
        }
    }

    /** Get the services of a specific type associated with this resource
     * @param pc the page control object
     * @return a PageList of ServiceValues and ServiceClusterValues (in case of
     *         applications)
     * @deprecated
     */
    public PageList getAssociatedServices(Integer typeId, PageControl pc)
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManager sManager;
        PageList res;
        Integer iId;

        sManager = getServiceManager();
        iId = _id.getId();

        switch (_id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
                res = sManager.getServiceInventoryByApplication(getSubject(),
                                                                iId, typeId,pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                res = sManager.getServicesByService(getSubject(), iId, typeId, pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                res = sManager.getServicesByServer(getSubject(), iId, typeId, pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM :
                res = sManager.getPlatformServices(getSubject(), iId, typeId, pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP :
                res = new PageList();
                break;
            default :
                throw new IllegalArgumentException(_id.getTypeName() +
                    " type does not have valid service associations");
        }
        return res;
    }

    /** Get the service IDs of a specific type associated with this resource
     * @return a PageList of ServiceValues and ServiceClusterValues (in case of
     *         applications)
     */
    public List getAssociatedServiceIds(Integer typeId)
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManager sManager;
        List vals, res;
        Integer iId;
        Integer[] sids;
        PageControl pc = PageControl.PAGE_ALL;
            
        sManager = getServiceManager();
        iId = _id.getId();

        switch (_id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            vals = sManager.getServiceInventoryByApplication(getSubject(), iId,
                                                             typeId, pc);
            res = new ArrayList(vals.size());
            for (Iterator it = vals.iterator(); it.hasNext();) {
                AppdefResourceValue aval = (AppdefResourceValue) it.next();
                res.add(aval.getEntityId());
            }

            return res;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            sids = sManager.getServiceIdsByServer(getSubject(), iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            sids = sManager.getServiceIdsByService(getSubject(), iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            vals = sManager.getPlatformServices(getSubject(), iId, typeId, pc);
            res = new ArrayList(vals.size());
                for (Iterator it = vals.iterator(); it.hasNext(); ) {
                    AppdefResourceValue aval = (AppdefResourceValue) it.next();
                    res.add(aval.getEntityId());
                }
                return res;
                
            default :
                throw new IllegalArgumentException(_id.getTypeName() +
                    " type does not have valid service associations");
        }

        res = new ArrayList(sids.length);
        for (int i = 0; i < sids.length; i++) {
            res.add(AppdefEntityID.newServiceID(sids[i]));
        }
        return res;
    }

    public AppdefEntityID[] getFlattenedServiceIds()
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManager sManager = getServiceManager();
        PageControl pc = PageControl.PAGE_ALL;
        
        AppdefEntityID[] servEntIds;
        if (_id.isApplication()) {
            Integer[] servicePKs = sManager
                .getFlattenedServiceIdsByApplication(getSubject(), _id.getId());
            
            servEntIds = new AppdefEntityID[servicePKs.length];
            for (int i = 0; i < servicePKs.length; i++) {
                servEntIds[i] = AppdefEntityID.newServiceID(servicePKs[i]);
            }
        }
        else {
            List services = getAssociatedServices(null, pc);
            servEntIds = new AppdefEntityID[services.size()];
            Iterator it = services.iterator();
            for (int i = 0; it.hasNext(); i++) {
                ServiceValue servVal = (ServiceValue) it.next();
                servEntIds[i] = AppdefEntityID.newServiceID(servVal.getId());
            }
        }
        
        return servEntIds;
    }


    /**
     * Get the platform name associated with the given entity.
     * @return The name of the platform type, such as Linux, Win32,
     * HPUX, Solaris, etc.
     */
    //XXX should be more generic getBasePlatform that returns PlatformTypeValue
    public String getBasePlatformName()
        throws PermissionException, AppdefEntityNotFoundException
    {
        AppdefEntityID id = getID();
        String platform;
    
        if (id.isPlatform()) {
            platform = getTypeName();
        }
        else {
            List platforms = getAssociatedPlatforms(PageControl.PAGE_ALL);
            PlatformValue pValue = (PlatformValue)platforms.get(0);
            platform = pValue.getPlatformType().getName();
        }
    
        return platform;
    }
}

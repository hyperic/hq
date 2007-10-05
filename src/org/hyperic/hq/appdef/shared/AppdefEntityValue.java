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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * A class which handles fetching of data common to appdef entities.
 * This class should only be used from within the server.  Really 
 * this is just a superclass which is not.  </Harrie>
 */

public class AppdefEntityValue {
    private ConfigManagerLocal      configManagerLocal;
    private AppdefGroupManagerLocal groupManagerLocal;
    private PlatformManagerLocal    platformManagerLocal;
    private ApplicationManagerLocal applicationManagerLocal;
    private ServerManagerLocal      serverManagerLocal;
    private ServiceManagerLocal     serviceManagerLocal;

    private ApplicationValue        application = null;
    private PlatformValue           platform    = null;
    private ServerValue             server      = null;
    private ServiceValue            service     = null;
    private AppdefGroupValue        group       = null;

    private AppdefEntityID          _id;
    private AuthzSubjectValue       _subject;

    public AppdefEntityValue(AppdefEntityID id, AuthzSubjectValue subject) {
        _id      = id;
        _subject = subject;
    }
    
    public AppdefEntityValue(AppdefEntityID id, AuthzSubject subject) {
        _id      = id;
        _subject = subject.getAuthzSubjectValue();
    }
    
    public AppdefEntityID getID() {
        return _id;
    }

    private AppdefGroupManagerLocal getGroupManager() {
        return AppdefGroupManagerEJBImpl.getOne();
    }

    private PlatformManagerLocal getPlatformManager() {
        try {
            if(platformManagerLocal == null){
                platformManagerLocal = 
                    PlatformManagerUtil.getLocalHome().create();
            }
            return platformManagerLocal;
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private ApplicationManagerLocal getApplicationManager() {
        try {
            if(applicationManagerLocal == null){
                applicationManagerLocal = 
                    ApplicationManagerUtil.getLocalHome().create();
            }
            return applicationManagerLocal;
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private ConfigManagerLocal getConfigManager() {
        try {
            if(configManagerLocal == null){
                configManagerLocal = 
                    ConfigManagerUtil.getLocalHome().create();
            }
            return configManagerLocal;
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private PlatformType getPlatformType()
        throws AppdefEntityNotFoundException {
        return getPlatformManager()
            .findPlatformById(_id.getId()).getPlatformType();
    }

    private AppdefResourceValue getPlatform(boolean full)
        throws AppdefEntityNotFoundException, PermissionException {
        if (platform == null) {
            if (full) {
                Platform p = getPlatformManager().getPlatformById(_subject,
                                                                  _id.getId());
                platform = p.getPlatformValue();
            }
            else {
                Platform p = getPlatformManager().findPlatformById(_id.getId());
                return p.getPlatformLightValue();
            }
        } 
        return platform;
    }
    
    private ApplicationValue getApplication()
        throws AppdefEntityNotFoundException, ApplicationNotFoundException,
               PermissionException {
        if(application == null){
            Integer aId = _id.getId();
            application = getApplicationManager().getApplicationById(_subject,
                                                                     aId);
        } 
        return application;
    }
    
    private ServerManagerLocal getServerManager() {
        if(serverManagerLocal == null){
            serverManagerLocal = ServerManagerEJBImpl.getOne();
        }
        return serverManagerLocal;
    }

    private ServerType getServerType()
        throws AppdefEntityNotFoundException {
        return getServerManager().getServerById(_id.getId()).getServerType();
    }

    private AppdefResourceValue getServer(boolean full)
        throws AppdefEntityNotFoundException, PermissionException {
        if (server == null) {
            if (full) {
                Server s = getServerManager().getServerPOJOById(_subject,
                                                                _id.getId());
                server = s.getServerValue();
            }
            else {
                Server s = getServerManager().findServerById(_id.getId());
                return s.getServerLightValue();
            }
        } 
        return server;
    }

    private ServiceManagerLocal getServiceManager() {
        if(serviceManagerLocal == null){
            serviceManagerLocal = ServiceManagerEJBImpl.getOne();
        }
        return serviceManagerLocal;
    }

    private ServiceType getServiceType()
        throws AppdefEntityNotFoundException {
        return getServiceManager().getServiceById(_id.getId()).getServiceType();
    }

    private AppdefResourceValue getService(boolean full)
        throws AppdefEntityNotFoundException, PermissionException {
        if(service == null){
            Service s = getServiceManager().getServiceById(_id.getId());
            if (full) {
                service = s.getServiceValue();
            }
            else {
                return s.getServiceLightValue();
            }
        } 
        return service;
    }

    private AppdefResourceTypeValue getGroupType()
        throws PermissionException, AppdefGroupNotFoundException {
        return getGroup(true).getAppdefResourceTypeValue();
    }

    private AppdefGroupValue getGroup(boolean full)
        throws PermissionException, AppdefGroupNotFoundException {
        if (group == null)
            group = getGroupManager().findGroup(_subject, _id, full);
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
                    .getPlatformsByServers(_subject, eids);
            
            if (!(resType instanceof PlatformTypeValue))
                throw new IllegalArgumentException(_id +
                    " group is not a valid platform compatible group");
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            if (resType instanceof ServiceTypeValue) {
                return getServerManager()
                    .getServersByServices(_subject, eids);
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
            AppdefEntityValue val = new AppdefEntityValue(eid, _subject);
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
        return getLiteResourceValue().getName();
    }

    public String getDescription()
        throws PermissionException, AppdefEntityNotFoundException {
        return getLiteResourceValue().getDescription();
    }

    public String getTypeName()
        throws AppdefEntityNotFoundException, PermissionException
    {
        switch(_id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return getPlatformType().getName();
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return getServerType().getName();
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return getServiceType().getName();
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            throw new IllegalArgumentException("Application type name not " +
                                               "yet supported");
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroupType().getName();
        default:
            throw new IllegalStateException("Unknown appdef entity type");
        }
    }

    public String getPluginName()
        throws AppdefEntityNotFoundException
    {
        if (_id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP)
            throw new IllegalArgumentException ("Groups do not have plugins");
        
        return getConfigManager().getPluginName(_id);
    }

    /**
     * Get the AppdefResourceValue for a given AppdefEntityID.  This can also
     * be used to validate that a given AppdefEntityID is valid.
     */
    public AppdefResourceValue getLiteResourceValue()
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
          case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroup(false);
        default:
          throw new IllegalStateException("Unknown appdef entity type");
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
            return getPlatform(true);
          case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return getServer(true);
          case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return getService(true);
          case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return getApplication();
          case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroup(true);
        default:
          throw new IllegalStateException("Unknown appdef entity type");
        }
    }

    /**
     * Get the AppdefResourceTypeValue for a given AppdefEntityID.  
     */
    public AppdefResourceTypeValue getResourceTypeValue()
        throws PermissionException, AppdefEntityNotFoundException 
    {
        return getResourceValue().getAppdefResourceTypeValue();
    }

    public PageList getAssociatedPlatforms(PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {
        PlatformManagerLocal pManager;
        PageList res;
        Integer iId;

        iId = _id.getId();

        pManager = getPlatformManager();
        if(_id.getType() == AppdefEntityConstants.APPDEF_TYPE_APPLICATION){
            return pManager.getPlatformsByApplication(_subject, iId, pc);
        }

        res = new PageList();
        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res.add(pManager.getPlatformByService(_subject, iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            res.add(pManager.getPlatformByServer(_subject, iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            res.add(getPlatform(true));
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
        AppdefGroupValue groupVo = (AppdefGroupValue) getResourceValue();
        if (groupVo.getGroupType() != groupTypeId) {
            throw new IllegalArgumentException("Invalid group type."+
                "Expecting type:"+AppdefEntityConstants
                .getAppdefGroupTypeName(groupTypeId));
        }
    }

    /** Get the servers associated with this resource
     * @param pc the page control object
     * @return a PageList of ServiceValue's
     */
    public PageList getAssociatedServers(PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException {
        ServerManagerLocal sManager;
        PageList res;
        Integer iId;

        iId      = _id.getId();
        sManager = getServerManager();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return sManager.getServersByApplication(_subject, iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return sManager.getServersByPlatform(_subject, iId, true, pc);
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return getGroupAppdefEntityValues(
                 AppdefEntityConstants.APPDEF_TYPE_SERVER, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res = new PageList();
            res.add(sManager.getServerByService(_subject, iId));
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            res = new PageList();
            res.add(getServer(true));
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
     */
    public PageList getAssociatedServers(Integer typeId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException {
        ServerManagerLocal sManager;
        PageList res;
        Integer iId;

        iId      = _id.getId();
        sManager = getServerManager();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            res =
                sManager.getServersByApplication(_subject, iId, typeId, pc);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            // default to exclude virtual servers
            res = sManager.getServersByPlatform(_subject, iId, typeId, true, pc);
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
     */
    public List getAssociatedServerIds(Integer typeId)
        throws AppdefEntityNotFoundException, PermissionException {
        ServerManagerLocal sManager;
        Integer[] ids;
        Integer iId;
    
        iId      = _id.getId();
        sManager = getServerManager();
    
        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            ids = sManager.getServerIdsByApplication(_subject, iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            ids = sManager.getServerIdsByPlatform(_subject, iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            ServerValue server = sManager.getServerByService(_subject, iId);
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
            entIds.add(
                new AppdefEntityID(
                    AppdefEntityConstants.APPDEF_TYPE_SERVER,
                    ids[i]));
        }
        return entIds;
    }

    /** Get the services associated with this resource
     * @return a PageList of ServiceValue's
     */
    public List getAssociatedServices(PageControl pc)
        throws PermissionException, AppdefEntityNotFoundException,
               ApplicationNotFoundException {
        ServiceManagerLocal sManager;
        PageList res;
        Integer iId;
        
        sManager = getServiceManager();
        iId      = _id.getId();

        switch(_id.getType()){
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
            return sManager.getServiceInventoryByApplication(_subject, iId,
                                                             pc);
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            validateGroupType(
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);
            return getGroupAppdefEntityValues(
                AppdefEntityConstants.APPDEF_TYPE_SERVICE, pc);
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return sManager.getPlatformServices(_subject, iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVER :
            return sManager.getServicesByServer(_subject, iId, pc);
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            res = sManager.getServicesByService(_subject, iId, pc);
            res.add(getService(true));     // Also add self
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
     */
    public PageList getAssociatedServices(Integer typeId, PageControl pc)
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManagerLocal sManager;
        PageList res;
        Integer iId;

        sManager = getServiceManager();
        iId = _id.getId();

        switch (_id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
                res = sManager.getServiceInventoryByApplication(_subject,
                                                                iId, typeId,pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                res = sManager.getServicesByService(_subject, iId,
                                                    typeId, pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                res = sManager.getServicesByServer(_subject, iId, typeId,
                                                   pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM :
                res = sManager.getPlatformServices(_subject, iId, typeId,
                                                   pc);
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

    /** Get either the internal or deployed services associated with this
     * resource
     * @return a PageList of ServiceValues and ServiceClusterValues (in case of
     *         applications)
     */
    public List getAssociatedServices(boolean internal)
        throws AppdefEntityNotFoundException, PermissionException
    {
        // Get ALL associated services
        List services = getAssociatedServices(null, PageControl.PAGE_ALL);

        // Filter by internal flag         
        for (Iterator it = services.iterator(); it.hasNext(); ) {
            Object o = it.next();
            ServiceTypeValue serviceType;
            if (o instanceof ServiceValue) {
                serviceType = ((ServiceValue) o).getServiceType();
            }
            else {
                serviceType = ((ServiceClusterValue) o).getServiceType();
            }
            if (serviceType.getIsInternal() != internal)
                it.remove();
        }
        return services;
    }

    /** Get the service IDs of a specific type associated with this resource
     * @return a PageList of ServiceValues and ServiceClusterValues (in case of
     *         applications)
     */
    public List getAssociatedServiceIds(Integer typeId)
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManagerLocal sManager;
        List vals, res;
        Integer iId;
        Integer[] sids;
        PageControl pc = PageControl.PAGE_ALL;
            
        sManager = getServiceManager();
        iId = _id.getId();

        switch (_id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            vals = sManager.getServiceInventoryByApplication(_subject, iId,
                                                             typeId, pc);
            res = new ArrayList(vals.size());
            for (Iterator it = vals.iterator(); it.hasNext();) {
                AppdefResourceValue aval = (AppdefResourceValue) it.next();
                res.add(aval.getEntityId());
            }

            return res;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            sids = sManager.getServiceIdsByServer(_subject, iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            sids = sManager.getServiceIdsByService(_subject, iId, typeId);
            break;
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            vals = sManager.getPlatformServices(_subject, iId, typeId, pc);
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
            res.add(new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVICE,
                        sids[i]));
        }
        return res;
    }

    public AppdefEntityID[] getFlattenedServiceIds()
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        return getFlattenedServiceIds(_subject);
    }

    /** Get all of the service IDs associated with this resource
     * @return a PageList of ServiceValues and ServiceClusterValues (in case of
     *         applications)
     */
    public AppdefEntityID[] getFlattenedServiceIds(AuthzSubjectValue subject)
        throws ApplicationNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        ServiceManagerLocal sManager = getServiceManager();
        PageControl pc = PageControl.PAGE_ALL;
        
        AppdefEntityID[] servEntIds;
        if (_id.getType() == AppdefEntityConstants.APPDEF_TYPE_APPLICATION){
            Integer[] servicePKs =
                sManager.getFlattenedServiceIdsByApplication(
                    subject, _id.getId());
            
            servEntIds = new AppdefEntityID[servicePKs.length];
            for (int i = 0; i < servicePKs.length; i++) {
                servEntIds[i] = new AppdefEntityID(
                    AppdefEntityConstants.APPDEF_TYPE_SERVICE, servicePKs[i]);
            }
        }
        else {
            List services = getAssociatedServices(null, pc);
            servEntIds = new AppdefEntityID[services.size()];
            Iterator it = services.iterator();
            for (int i = 0; it.hasNext(); i++) {
                ServiceValue servVal = (ServiceValue) it.next();
                servEntIds[i] = new AppdefEntityID(
                    AppdefEntityConstants.APPDEF_TYPE_SERVICE, servVal.getId());
            }
        }
        
        return servEntIds;
    }
//
// THIS IS NOT SAFE FOR AUTHZ    
//    /**
//     * Get the platform plugin name associated with the given entity,
//     * used to lookup a plugin via a GenericPluginManager.
//     * @return The name of the platform plugin name, such as
//     * "Apache 2.0 Linux", "Apache 2.0 Win32", etc.
//     */
//    public String getPlatformPluginName()
//        throws AppdefEntityNotFoundException, PermissionException,
//               NamingException
//    {
//        //XXX getAssociatedPlatforms does not work with groups.
//        if (getID().getType() ==
//            AppdefEntityConstants.APPDEF_TYPE_GROUP) {
//            return getTypeName();
//        }
//        String platform = getBasePlatformName();
//        return getTypeName() + " " + platform;
//    }

    /**
     * Get the platform name associated with the given entity.
     * @return The name of the platform type, such as Linux, Win32,
     * HPUX, Solaris, etc.
     */
    //XXX should be more generic getBasePlatform that returns
    //PlatformTypeValue
    public String getBasePlatformName()
        throws PermissionException, AppdefEntityNotFoundException
    {
        AppdefEntityID id = getID();
        String name = getTypeName();
        int type = id.getType();
        String platform;
    
        if (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            platform = name;
        }
        else {
            List platforms = getAssociatedPlatforms(PageControl.PAGE_ALL);
            PlatformValue pValue = (PlatformValue)platforms.get(0);
            platform = pValue.getPlatformType().getName();
        }
    
        return platform;
    }
}

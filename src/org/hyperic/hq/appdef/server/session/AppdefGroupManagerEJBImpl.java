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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerUtil;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilter;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupManagerLocal;
import org.hyperic.hq.grouping.shared.GroupModificationException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.grouping.shared.GroupNotFoundException;
import org.hyperic.hq.grouping.server.session.GroupManagerEJBImpl;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * Appdef Group Manager - Appdef's view of the grouping subsystem's
 *  Group mechanism. Augments GroupValues with appdef-like qualities.
 *
 * @ejb:bean name="AppdefGroupManager"
 *      jndi-name="ejb/appdef/AppdefGroupManager"
 *      local-jndi-name="LocalAppdefGroupManager"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class AppdefGroupManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {
    private final String APPDEF_PAGER_PROCESSOR =
        "org.hyperic.hq.appdef.shared.pager.AppdefPagerProc";
    private final Log log = LogFactory.getLog(AppdefGroupManagerEJBImpl.class);
    private final int APPDEF_TYPE_UNDEFINED = -1;
    private final int APPDEF_RES_TYPE_UNDEFINED = -1;
    private final int CLUSTER_UNDEFINED = -1;
    private GroupManagerLocal groupManager;
    private ServiceManagerLocal serviceManager;
    
    private GroupManagerLocal getGroupManager () {
        if (groupManager == null) {
            groupManager = GroupManagerEJBImpl.getOne();
        }
        return groupManager;
    }

    private ServiceManagerLocal getServiceManager() {
        if (serviceManager == null) {
            serviceManager = ServiceManagerEJBImpl.getOne();
        }
        return serviceManager;
    }
    
    public AppdefGroupManagerEJBImpl() {}

    /**
     * Produce a paged list of all groups where caller is authorized
     * to modify.
     * @param subject subject value.
     * @param pc control
     * @return List containing AppdefGroupValue.
     * @throw PermissionException when group access is not authorized.
     * @ejb:interface-method
     */
    public PageList findAllGroups(AuthzSubject subject, PageControl pc)
        throws PermissionException {
        return findAllGroups(subject, (ResourceValue) null, pc, null);
    }

    /**
     * Produce list of all groups where caller is authorized
     * to modify. Apply filterSet to control group list membership.
     * @param subject subject
     * @param pc control
     * @param grpFilters set for groups
     * @return PageList containing AppdefGroupValues.
     * @throw PermissionException when group access is not authorized.
     * @ejb:interface-method
     */
    public PageList findAllGroups(AuthzSubject subject, PageControl pc,
                                  AppdefPagerFilter[] grpFilters)
        throws PermissionException {
        return findAllGroups(subject, (ResourceValue) null, pc, grpFilters);
    }

    /**
     * Produce list of all groups that contain the specified appdef entity.
     * Apply filterSet to control group list membership.
     *
     * @param subject subject
     * @param id for inclusive search.
     * @param pc control
     * @param grpFilters set for groups
     * @return PageList containing AppdefGroupValues.
     * @throw PermissionException when group access is not authorized.
     * @ejb:interface-method
     * */
    public PageList findAllGroups(AuthzSubject subject, AppdefEntityID id,
                                  PageControl pc,
                                  AppdefPagerFilter[] grpFilters)
        throws PermissionException, ApplicationException {
        ResourceValue rv;

        try {
            rv = null;
            if (id != null) {
                rv = getResourceFromInstance( 
                    AppdefUtil.appdefTypeIdToAuthzTypeStr(id.getType()),
                    new Integer (id.getID()));
            } 
        }
        catch (FinderException fe) {
            // XXX - Temporary! Throw more appdef specific exception.
            // need a clean way to convert from authz to appdef type
            throw new ApplicationException("unable to find entity: "+
                                          id.toString(),fe);
        }
        return findAllGroups(subject,rv,pc,grpFilters);
    }

    private PageList findAllGroups(AuthzSubject subject, 
                                   ResourceValue rv, PageControl pc,
                                   AppdefPagerFilter[] grpFilters)
        throws PermissionException 
    {
        PageList retVal = null;

        ResourceGroupManagerLocal groupMan = 
            ResourceGroupManagerEJBImpl.getOne();

        if (pc == null)
            pc = new PageControl();

        Collection allGroups = groupMan.getAllResourceGroups(subject, true);
        log.debug("All groups size: " + allGroups.size());

        List toBePaged = new ArrayList();
        for (Iterator i=allGroups.iterator();i.hasNext();) {
            ResourceGroup g = (ResourceGroup)i.next();
            toBePaged.add(groupMan.convertGroup(subject, g));
        }
        retVal = getPageList ( toBePaged, pc, grpFilters );
        if (retVal==null)
            retVal = new PageList();  // return empty list if no groups.

        return retVal;
    }

    /**
     * Produce list of all groups that contain the specified appdef entity.
     * Apply filterSet to control group list membership.
     *
     * @param subject subject
     * @param id for inclusive search.
     * @return PageList containing AppdefGroupValues.
     * @throw PermissionException when group access is not authorized.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * */
    public Integer[] findClusterIds(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, ApplicationException {
        ResourceValue rv = null;
        try {
            rv = getResourceFromInstance( 
                AppdefUtil.appdefTypeIdToAuthzTypeStr(id.getType()),
                                                      id.getId());
        }
        catch (FinderException fe) {
            // XXX - Temporary! Throw more appdef specific exception.
            // need a clean way to convert from authz to appdef type
            throw new ApplicationException("unable to find entity: " + id, fe);
        }
        
        List grps = findAllGroups(subject, rv, PageControl.PAGE_ALL,
                                  new AppdefPagerFilter[0]);

        ArrayList clusterIds = new ArrayList(0);
        for (Iterator it = grps.iterator(); it.hasNext(); ) {
            AppdefGroupValue gv = (AppdefGroupValue) it.next();

            if (gv.getGroupType() == 
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC && 
                gv.getClusterId() != CLUSTER_UNDEFINED) {
                clusterIds.add(new Integer(gv.getClusterId()));
            }
        }
        return (Integer[]) clusterIds.toArray(new Integer[clusterIds.size()]);
    }
    
    // dbfetch the resource
    private ResourceValue getResourceFromInstance (String authzTypeStr, 
        Integer instanceId) throws FinderException {
        try {
            ResourceManagerLocal rmLoc = 
                ResourceManagerUtil.getLocalHome().create();
            return rmLoc.findResourceByInstanceId(rmLoc
                .findResourceTypeByName(authzTypeStr), instanceId);
        } catch (NamingException ne) {
            throw new SystemException(ne);
        } catch (CreateException ce) {
            throw new SystemException(ce);
        }
    }

    // Page out the collection, applying any filters in the process.
    private PageList getPageList (Collection coll, PageControl pc,
        AppdefPagerFilter[] filters) {
        PageList retVal;
        Pager pager;

        pc = PageControl.initDefaults(pc,SortAttribute.RESOURCE_NAME);
        try {
            pager = Pager.getPager( APPDEF_PAGER_PROCESSOR );
        }
        catch (InstantiationException e) {
            log.debug("InstantiationException caught instantiating " +
                      APPDEF_PAGER_PROCESSOR);
            throw new SystemException (e.getMessage());
        }
        catch (IllegalAccessException e) {
            log.debug("IllegalAccessException caught instantiating " +
                      APPDEF_PAGER_PROCESSOR);
            throw new SystemException (e.getMessage());
        }
        catch (ClassNotFoundException e) {
            log.debug("ClassNotFoundException caught instantiating " +
                      APPDEF_PAGER_PROCESSOR);
            throw new SystemException (e.getMessage());
        }
        retVal = pager.seek(coll, pc.getPagenum(),pc.getPagesize(), filters);
 
        int adj = 0; // keep track of number filtered for offsetting
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                adj += filters[i].getFilterCount(); 
            }
        }
        retVal.setTotalSize(retVal.getTotalSize() - adj);

        return retVal;
    }

    /* This had to be adopted from appdef boss's similar version. This will
     * eventually be available from a new EJB that will represent all of the
     * appdef entities agnostically.*/
    private AppdefResourceValue findById(AuthzSubject subject,
                                         AppdefEntityID entityId)
        throws AppdefEntityNotFoundException, PermissionException {

        ServerManagerLocal      serverManagerLocal      = null;
        ApplicationManagerLocal appManagerLocal         = null;

        switch (entityId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM: 
                return PlatformManagerEJBImpl.getOne().getPlatformValueById(
                        subject, entityId.getId());
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                serverManagerLocal = ServerManagerEJBImpl.getOne();
                return serverManagerLocal.getServerById(
                        subject, entityId.getId());
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return getServiceManager().getServiceById(
                        subject, entityId.getId());
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                appManagerLocal = ApplicationManagerEJBImpl.getOne();
                return appManagerLocal.getApplicationById(
                        subject, entityId.getId());
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                ResourceGroupManagerLocal groupMan =
                    ResourceGroupManagerEJBImpl.getOne();
                ResourceGroup g = 
                    groupMan.findResourceGroupById(subject, entityId.getId()); 
                               
                return groupMan.convertGroup(subject, g);
            default:
                throw new InvalidAppdefTypeException (entityId.getType()
                        + " is not a valid appdef entity type");
        }
    }


    public static AppdefGroupManagerLocal getOne() {
        try {
            return AppdefGroupManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
    }

    public void ejbPostCreate() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void ejbRemove() {}
}

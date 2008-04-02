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
     * Lookup and return a group value object.
     * @param subject subject value.
     * @param id entity id
     * @return AppdefGroupValue object
     * @ejb:interface-method
     * @throws AppdefGroupNotFoundException when group cannot be located in db.
     * @throws PermissionException if the caller is not authorized.
     */
    public AppdefGroupValue findGroup(AuthzSubject subject,
                                      AppdefEntityID id)
        throws AppdefGroupNotFoundException, PermissionException {
        return findGroup(subject, id, true);
    }

    /**
     * Lookup and return a group value object that may or may not be fully 
     * populated.
     * @param subject subject value.
     * @param id entity id
     * @return AppdefGroupValue object
     * @ejb:interface-method
     * @throws AppdefGroupNotFoundException when group cannot be located in db.
     * @throws PermissionException if the caller is not authorized.
     */
    public AppdefGroupValue findGroup(AuthzSubject subject,
                                      AppdefEntityID id, boolean full)
        throws AppdefGroupNotFoundException, PermissionException {
        return findGroup(subject, id, null, full);
    }

    /*
     * Lookup and return a group value object by either its name or identifier.
     */
    private AppdefGroupValue findGroup(AuthzSubject subject,
                                       AppdefEntityID id,
                                       String groupName, boolean full)
        throws AppdefGroupNotFoundException, PermissionException
    {
        AppdefGroupValue retVal;

        try {
            GroupManagerLocal manager = getGroupManager();

            retVal = new AppdefGroupValue();

            // One of name or id is required to find the group.
            if (groupName != null) {
                retVal.setName(groupName);
            } else if (id != null) {
                retVal.setId(id.getId());
            } else {
                // One of either id or name is required..
                throw new AppdefGroupNotFoundException("Unable to find " +
                                                       "group.  No name or " +
                                                       "id specified.");
            }

            retVal = (AppdefGroupValue) manager.findGroup(subject, retVal, full);

            // Check permission, making sure to generate an appdef id if only
            // a group name was passed in.
            if (id == null) {
                id = AppdefEntityID.newGroupID(retVal.getId().intValue());
            }

            checkPermission(subject, id,
                            AuthzConstants.groupOpViewResourceGroup);

            // Setup the group to contain a valid type object.
            setGroupAppdefResourceType(subject, retVal);
   
            // register any visitors
            registerVisitors(retVal);
        } catch (GroupNotFoundException e) {
            log.debug("findGroup() Unable to find group:" + id); 
            throw new AppdefGroupNotFoundException ("Unable to find group:",e);
        } catch (IllegalArgumentException e) {
            log.debug("findGroup() unable to find an appdef group:" + id);
            throw new AppdefGroupNotFoundException("Group not an appdef group");
        }

        return retVal;
    }

     /**
      * Fetch a group's members as a paged list of resource values. 
      *
      * Note: This method is expensive and unnecessary for most scenarios as
      * each group member's appdef value is looked up. Please USE SPARINGLY.
      * Use findGroup().getAppdefGroupEntries() for low cost alternative.
      *
      * @param subject - valid spider subject
      * @param gid     - group id
      * @param pc      - page control
      * @throws AppdefGroupNotFoundException  - non-existent group
      * @throws AppdefEntityNotFoundException - group member doesn't exist
      * @throws PermissionException           - unable to view group
      * @ejb:interface-method
      */
    public PageList getGroupMemberValues(AuthzSubject subject,
                                         Integer gid, PageControl pc)
        throws AppdefGroupNotFoundException, AppdefEntityNotFoundException,
               PermissionException {
        PageList retVal = null;
        AppdefEntityID aeid = AppdefEntityID.newGroupID(gid.intValue());
        AppdefGroupValue groupVo = findGroup(subject, aeid, null, true);
        retVal = groupVo.getAppdefGroupEntries();

        // Replace each AppdefEntityID with an AppdefResourceValue
        for (int i = 0; i < retVal.size(); i++) {
            AppdefEntityID id = (AppdefEntityID) retVal.get(i);
            retVal.set(i, findById(subject, id));
        }
        return retVal;
    }

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
        throws PermissionException {
        PageList retVal = null;

        try {
            GroupManagerLocal manager = getGroupManager();

            // create a valid appdef group vo for cloning.
            AppdefGroupValue gv = new AppdefGroupValue();

            if (pc == null)
                pc = new PageControl();

            PageList allGroups = manager.findAllGroups(subject, gv, rv,
                                                       PageControl.PAGE_ALL);
            
            log.debug("All groups size: " + allGroups.size());

            List toBePaged = new ArrayList();
            for (Iterator i=allGroups.iterator();i.hasNext();) {
                gv = (AppdefGroupValue)i.next();

                // Setup the group to contain a valid type object.
                setGroupAppdefResourceType(subject,gv);

                // register any visitors
                registerVisitors((AppdefGroupValue)gv);

                toBePaged.add(gv);
            }
            retVal = getPageList ( toBePaged, pc, grpFilters );

            if (log.isDebugEnabled()) {
                log.debug("Filtered groups size: " + retVal.size() +
                          " filter size: " +
                          (grpFilters == null ? 0 : grpFilters.length));
                if (grpFilters != null) {
                    for (int i = 0; i < grpFilters.length; i++) {
                        log.debug("Filter type: " +
                                  grpFilters[i].getClass().getName());                
                    }
                }
            }
        }
        catch (GroupNotFoundException gnf) {
            // Catch exception to return empty list.
            log.debug("Caught harmless GroupNotFound exception whilest looking "+
                "for groups [subject="+subject.toString()+"]- "+gnf.getMessage());
        }
        catch (CloneNotSupportedException e) {
            log.error("The group value object does not support cloning.",e);
            throw new SystemException ("Group value object doesn't support "+
                                          "cloning.",e);
        }
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

    // Register any visitors for a group value.
    private void registerVisitors(AppdefGroupValue gv) {
        // XXX - forthcoming, required reworking to 
        // handle new group types and subtypes.
    }

    // All UIs will require a group to express a valid type (encapsulated by a
    // concrete decendent of AppdefResourceTypeValue).  However, since a group
    // is not an actual appdef entity, we set the group up as a surrogate
    // for either a dummy type (GroupTypeValue) or a copy of one of its
    // member's type objects.
    private void setGroupAppdefResourceType(AuthzSubject subject,
                                            AppdefGroupValue gv) {

        try {
            if (gv.isGroupCompat()) {
                gv.setAppdefResourceTypeValue(
                    getResourceTypeById(gv.getGroupEntType(),
                                        gv.getGroupEntResType()));
            }
            else {
                AppdefResourceTypeValue tvo = new GroupTypeValue();
                tvo.setId ( new Integer(gv.getGroupType()) );
                tvo.setName( AppdefEntityConstants.getAppdefGroupTypeName(
                    gv.getGroupType()) );
                gv.setAppdefResourceTypeValue( tvo );
            }
        }
        catch (FinderException e) {
            // this is not a fatal error
            if (log.isDebugEnabled())
                log.debug("Caught exception setting group resource type value.",
                          e);
        } catch (AppdefEntityNotFoundException e) {
            // this is not a fatal error
            if (log.isDebugEnabled())
                log.debug("Caught exception setting group resource type value.",
                          e);
        }
    }

    private AppdefResourceTypeValue getResourceTypeById (int type, int id)
        throws FinderException, AppdefEntityNotFoundException {
        switch (type) {
            case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM) :
                return getPlatformTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVER) :
                return getServerTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_SERVICE) :
                return getServiceTypeById(id);
            case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION) :
                return getApplicationTypeById(id);
            default:
                throw new IllegalArgumentException ("Invalid resource type:"
                                                    +type);
        }
    }

    private PlatformTypeValue getPlatformTypeById (int id)
        throws PlatformNotFoundException {
        PlatformManagerLocal platLoc;
        try {
            platLoc = PlatformManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        return platLoc.findPlatformTypeValueById(new Integer(id));
    }

    private ServerTypeValue getServerTypeById (int id)
        throws FinderException {
        ServerManagerLocal servLoc;
        try {
            servLoc = ServerManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        return servLoc.findServerTypeById(new Integer(id));
    }

    private ServiceTypeValue getServiceTypeById(int id) throws FinderException {
        return getServiceManager().findServiceTypeById(new Integer(id));
    }

    private ApplicationTypeValue getApplicationTypeById (int id)
        throws FinderException {
        ApplicationManagerLocal appLoc;
        try {
            appLoc = ApplicationManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        return appLoc.findApplicationTypeById(new Integer(id));
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

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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;

/**
 * An Action that retrieves data from the BizApp to facilitate display
 * of the <em>AddGroupResources</em> form. 
 */
public class AddGroupResourcesFormPrepareAction extends Action {
    
    // ---------------------------------------------------- Public Methods
    
    /**
     * Retrieve this data and store it in the specified request
     * parameters:
     *
     * <ul>
     *   <li><code>GroupValue</code> object identified by
     *     <code>Constants.RESOURCE_PARAM</code> request parameter in
     *     <code>Constants.RESOURCE_ATTR</code></li>
     *
     *   <li><code>List</code> of available <code>AppdefResourceValue</code>
     *     objects (those not already associated with the group) in
     *     <code>Constants.AVAIL_RESOURCES_ATTR</code></li>
     *   <li><code>Integer</code> number of available roles in
     *     <code>Constants.NUM_AVAIL_RESOURCES_ATTR</code></li>
     *
     *   <li><code>List</code> of pending <code>OwnedRoleValue</code>
     *     objects (those in queue to be associated with the resource) in
     *     <code>Constants.PENDING_RESOURCES_ATTR</code></li>
     *   <li><code>Integer</code> number of pending resources in
     *     <code>Constants.NUM_PENDING_RESOURCES_ATTR</code></li>
     *
     *   <li><code>List</code> of pending <code>AppdefResourceValue</code>
     *     ids (those in queue to be associated with the resource) in
     *     <code>Constants.PENDING_RESOURCES_SES_ATTR</code>
     *    </li>
     * </ul>
     *
     * This Action edits 2 lists of Resources: pending, and available.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory.getLog(getClass().getName());
        
        AddGroupResourcesForm addForm = (AddGroupResourcesForm) form;
        
        Integer groupId = addForm.getRid();
        
        PageControl pcPending;
        
        if (groupId == null) {
            groupId = RequestUtils.getResourceId(request);
        }
        
        ServletContext ctx = getServlet().getServletContext();
        
        int sessionId = RequestUtils.getSessionIdInt(request);
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        
        PageControl pcAvail =
            RequestUtils.getPageControl(request, "psa", "pna","soa", "sca");
        pcPending = 
            RequestUtils.getPageControl(request, "psp", "pnp","sop", "scp");
        
        AppdefGroupValue group =
            (AppdefGroupValue) RequestUtils.getResource(request);
        if (group == null) {
            RequestUtils.setError(request,
                "resource.group.inventory.error.GroupNotFound");
            return null;
        }
        
        RequestUtils.setResource(request, group);
        addForm.setRid(group.getId());
        
        log.trace("available page control: " + pcAvail);
        log.trace("pending page control: " + pcPending);
        log.trace("getting group [" + groupId + "]");
        
        // XXX: if group == null, throw AppdefGroupNotFoundException
        /* pending resources are those on the right side of the "add
           to list" widget- awaiting association with the group
           when the form's "ok" button is clicked. */
        List pendingResourceIds =
            SessionUtils.getListAsListStr(request.getSession(),
                Constants.PENDING_RESOURCES_SES_ATTR);
        
        String nameFilter = RequestUtils.getStringParameter(request,
                                                            "nameFilter", null);
        
        log.trace("getting pending resources for group [" + groupId + "]");
        
        List entities = BizappUtils.buildAppdefEntityIds(pendingResourceIds);
        
        AppdefEntityID[] pendingResItems;
        
        if (entities.size() > 0) {
            pendingResItems = new AppdefEntityID[entities.size()];
            entities.toArray(pendingResItems);
        }
        else {
            pendingResItems = null;
        }        
        
        List pendingResources =
            BizappUtils.buildAppdefResources(sessionId, boss, pendingResItems);
        
        List sortedPendingResource =
            BizappUtils.sortAppdefResource(pendingResources, pcPending);
        PageList pendingList = new PageList();
        
        pendingList.setTotalSize(  sortedPendingResource.size() );
        
        Pager pendingPager = Pager.getDefaultPager();
        
        pendingList = pendingPager.seek(sortedPendingResource,
                                        pcPending.getPagenum(),
                                        pcPending.getPagesize());
        
        request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pendingList);
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR,
        new Integer(sortedPendingResource.size()));
        
        /* available resources are all resources in the system that are
         * not associated with the user and are not pending
         */
        log.trace("getting available resources for group [" + groupId + "]");
        
        String filterBy = addForm.getFilterBy();
        
        int appdefType = -1;
        if (filterBy != null) {
            appdefType = Integer.parseInt(filterBy);
        }

        PrepareResourceGroup p;
        
        if (group.isGroupCompat())
            p = new PrepareCompatGroup();
        else if (group.getGroupType() ==
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)
            p = new PrepareApplicationGroup();
        else if (group.getGroupType() ==
        AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)
            p = new PrepareGroupOfGroups();
        else
            p = new PrepareMixedGroup();
        
        p.loadGroupMembers(sessionId, addForm, group, boss, appdefType,
                           nameFilter, pendingResItems, pcAvail);
        PageList availResources = p.getAvailResources();
        
        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, availResources);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR,
            new Integer(availResources.getTotalSize()));
                
        return null;
    }
    
    /**
     * base abstract class for representing different group types.
     *
     * The responsibility of this class is to provide the template methods
     * supported by the subclasses.
     */
    private abstract class PrepareResourceGroup {
        /**
         * @return a list of available resources.
         */
        protected abstract PageList getAvailResources();
        
        /**
         * This method loads group members from the back-end.
         */
        protected abstract void loadGroupMembers(int sessionId,
                                                 AddGroupResourcesForm addForm,
                                                 AppdefGroupValue group,
                                                 AppdefBoss boss,
                                                 int appdefType,
                                                 String nameFilter,
                                                 AppdefEntityID[] pendingResItems,
                                                 PageControl pcAvail)
            throws Exception;
    }
    
    /**
     * inner class which represents the Compatible groups.
     */
    private class PrepareCompatGroup extends PrepareResourceGroup {
        private PageList avail = null;
        
        protected PageList getAvailResources() {
            return avail;
        }
        
        protected void loadGroupMembers(int sessionId,
                                        AddGroupResourcesForm addForm,
                                        AppdefGroupValue group,
                                        AppdefBoss boss, int appdefType,
                                        String nameFilter,
                                        AppdefEntityID[] pendingResItems,
                                        PageControl pcAvail)
            throws Exception {
            avail = boss.findCompatInventory(sessionId,
                                             group.getGroupEntType(),
                                             group.getGroupEntResType(),
                                             group.getEntityId(),
                                             pendingResItems,
                                             nameFilter,
                                             pcAvail);
        }
    }
    
    /**
     * prepares a Group of Groups
     */
    private class PrepareApplicationGroup extends PrepareResourceGroup {
        PageList availMembers = null;
        
        protected PageList getAvailResources() {
            return availMembers;
        }
        
        protected void loadGroupMembers(int sessionId,
                                        AddGroupResourcesForm addForm,
                                        AppdefGroupValue group,
                                        AppdefBoss boss, int appdefType,
                                        String nameFilter,
                                        AppdefEntityID[] pendingResItems,
                                        PageControl pcAvail)
            throws Exception {
            if (appdefType == -1)
                appdefType = AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
            
            availMembers = boss.findCompatInventory(sessionId,
                                                    appdefType,
                                                    -1,
                                                    group.getEntityId(),
                                                    pendingResItems,
                                                    pcAvail);
        }
        
    }
    
    /**
     * Inner class which represents the Group of Groups.
     */
    private class PrepareGroupOfGroups extends PrepareResourceGroup {
        PageList availMembers = null;
        
        protected PageList getAvailResources() {
            return availMembers;
        }
        
        protected void loadGroupMembers(int sessionId,
                                        AddGroupResourcesForm addForm,
                                        AppdefGroupValue group,
                                        AppdefBoss boss, int appdefType,
                                        String nameFilter,
                                        AppdefEntityID[] pendingResItems,
                                        PageControl pcAvail)
            throws Exception {
            if (appdefType == -1)
                appdefType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
            
            availMembers = boss.
                findCompatInventory(sessionId,
                                    AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                    appdefType,
                                    group.getEntityId(),
                                    pendingResItems,
                                    pcAvail);
            
            /**
             * load the group type filters
             */
            addForm.setAvailResourceTypes(buildGroupTypes());
        }
    }
    
    /**
     * inner class representing the Mixed Group of PSS
     */
    private class PrepareMixedGroup extends PrepareResourceGroup {
        PageList filteredAvailList = null;
        
        protected PageList getAvailResources() {
            return filteredAvailList;
        }
        
        protected void loadGroupMembers(int sessionId,
                                        AddGroupResourcesForm addForm,
                                        AppdefGroupValue group,
                                        AppdefBoss boss, int appdefType,
                                        String nameFilter,
                                        AppdefEntityID[] pendingResItems,
                                        PageControl pcAvail)
            throws Exception {
            if (appdefType == -1)
                appdefType = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
            
            filteredAvailList = boss.findCompatInventory(sessionId,
                                                         appdefType,
                                                         -1,
                                                         group.getEntityId(),
                                                         pendingResItems,
                                                         nameFilter,
                                                         pcAvail);
            
            /**
             * load the resource type filters
             */
            addForm.setAvailResourceTypes(buildResourceTypes());
        }
    }
    
    /**
     * builds a list of resource types (platform, server, service).
     *
     * @return a list of group types from the list
     */
    public static List buildResourceTypes() {
        List gTypes = new ArrayList();
        
        LabelValueBean bv = null;
        int type = -1;
        type = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
                                Integer.toString(type));
        
        gTypes.add(bv);
        
        type = AppdefEntityConstants.APPDEF_TYPE_SERVER;
        bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
                                Integer.toString(type));
        
        gTypes.add(bv);
        
        type = AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
                                Integer.toString(type));
        
        gTypes.add(bv);
        
        return gTypes;
    }
    
    /**
     * builds a unique list of group types.
     *
     * @return a unique list of group types from the list
     */
    public static List buildGroupTypes() {
        List gTypes = new ArrayList();
        
        LabelValueBean bv = null;
        int type = -1;
        type = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
        bv = new LabelValueBean(
        AppdefEntityConstants.getAppdefGroupTypeName(type),
        Integer.toString(type));
        
        gTypes.add(bv);
        
        type = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
        bv = new LabelValueBean(
        AppdefEntityConstants.getAppdefGroupTypeName(type),
        Integer.toString(type));
        
        gTypes.add(bv);
        
        type = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
        bv = new LabelValueBean(
        AppdefEntityConstants.getAppdefGroupTypeName(type),
        Integer.toString(type));
        
        gTypes.add(bv);
        
        return gTypes;
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;

/**
 * An Action that retrieves data from the user preferences
 * of the form. The purpose of this is to add
 * resources to the resource health dashboard widget
 *
 * This implementation heavily based on:
 * org.hyperic.hq.ui.action.admin.role.AddUsersRoleFormPrepareAction
 */
public class AddResourcesPrepareAction extends Action {
 
    // ---------------------------------------------------- Public Methods
    private static final String BLANK_LABEL = "";
    private static final String BLANK_VAL = "";
    private static final String PLATFORM_KEY =
        "resource.hub.filter.PlatformType";
    private static final String SERVER_KEY =
        "resource.hub.filter.ServerType";
    private static final String SERVICE_KEY =
        "resource.hub.filter.ServiceType";
    private static final String GROUP_ADHOC_GRP_KEY =
        "resource.hub.filter.GroupGroup";
    private static final String GROUP_ADHOC_GRP_VAL =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP).toString();
    private static final String GROUP_ADHOC_PSS_KEY =
        "resource.hub.filter.GroupPSS";
    private static final String GROUP_ADHOC_PSS_VAL =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS).toString();
    private static final String GROUP_ADHOC_APP_KEY =
        "resource.hub.filter.GroupApp";
    private static final String GROUP_ADHOC_APP_VAL =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP).toString();
    
    private static final int DEFAULT_RESOURCE_TYPE = -1;
    
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
        Log log = LogFactory.getLog(AddResourcesPrepareAction.class.getName());

        AddResourcesForm addForm = (AddResourcesForm)form;
            
        ServletContext ctx = getServlet().getServletContext();
        HttpSession session = request.getSession();            
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        WebUser user = (WebUser) session.getAttribute( 
                                            Constants.WEBUSER_SES_ATTR );
        AuthzBoss aBoss = ContextUtils.getAuthzBoss(ctx);
        DashboardConfig dashConfig = DashboardUtils.findDashboard(
        		(Integer)session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
        		user, aBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        PageControl pcAvail = RequestUtils.getPageControl(request, "psa", "pna",
                                                          "soa", "sca");
        PageControl pcPending = RequestUtils.getPageControl(request, "psp",
                                                            "pnp", "sop", "scp");


        /* pending resources are those on the right side of the "add
           to list" widget- awaiting association with the group
           when the form's "ok" button is clicked. */

        log.debug("check session if there are pending resources");
        List pendingResourcesIds = (List) session.getAttribute(
                                      Constants.PENDING_RESOURCES_SES_ATTR);

        if (pendingResourcesIds == null){
            log.debug("get avalable resources from user preferences");
            try {
                pendingResourcesIds =
                	dashPrefs.getPreferenceAsList(addForm.getKey(),
                                             StringConstants.DASHBOARD_DELIMITER);
            } catch (InvalidOptionException e) {
                // Then we don't have any pending resources
                pendingResourcesIds = new ArrayList(0);
            }
            log.debug("put entire list of pending resources in session");
            session.setAttribute(Constants.PENDING_RESOURCES_SES_ATTR,
                                 pendingResourcesIds);
        }

        log.debug("get page of pending resources selected by user");
        Pager pendingPager = Pager.getDefaultPager(); 
        List pendingResources =
            DashboardUtils.listAsResources(pendingResourcesIds, ctx, user);

        PageList pageOfPendingResources = pendingPager. 
                                seek(pendingResources, 
                                     pcPending.getPagenum(), 
                                     pcPending.getPagesize() );                                      

        log.debug("put selected page of pending resources in request");

        request.setAttribute(Constants.PENDING_RESOURCES_ATTR,
                             pageOfPendingResources); 
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR,
                             new Integer(pageOfPendingResources.getTotalSize()));


        /* available resources are all resources in the system that are
         * not associated with the user and are not pending 
         */                                 
        log.debug("determine if user wants to filter available resources");

        Integer ff = addForm.getFf();
        Integer ft = addForm.getFt();

        int appdefType =
            (ff == null) ? Constants.FILTER_BY_DEFAULT : ff.intValue();
        int resourceType = ft == null ? -1 : ft.intValue();
        boolean compat = false;
        if(appdefType == 0) 
            appdefType = Constants.FILTER_BY_DEFAULT;
        else if(appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC){
            //this is all to accomidate the compat group type as a seperate dropdown
            appdefType = AppdefEntityConstants.APPDEF_TYPE_GROUP;            
            compat = true;
        }
        
        List pendingEntityIds =
            DashboardUtils.listAsEntityIds(pendingResourcesIds); 

        AppdefEntityID[] pendingEntities =
            (AppdefEntityID[]) pendingEntityIds.toArray(new AppdefEntityID[0]);

        PageList avail;
        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            int groupSubtype = -1;
            
            if (compat) {
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
            }
            else {
                // resourceType straight up tells us what group
                // subtype was chosen
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;

                // for findCompatInventory, resourceType always need
                // to be this, for whatever reason
                resourceType = DEFAULT_RESOURCE_TYPE;
            }

            avail = boss.findCompatInventory(sessionId.intValue(),
                                             groupSubtype,
                                             appdefType, 
                                             DEFAULT_RESOURCE_TYPE,
                                             resourceType,
                                             addForm.getNameFilter(),
                                             pendingEntities,
                                             pcAvail);
        }
        else{
            avail = boss.findCompatInventory(sessionId.intValue(), 
                                             appdefType, 
                                             resourceType, 
                                             null, 
                                             pendingEntities,
                                             addForm.getNameFilter(),
                                             pcAvail);                 
        }

        PageList filteredAvailList = new PageList();
        Pager availPager = Pager.getDefaultPager();

        filteredAvailList = availPager.seek(avail, pcAvail.getPagenum(),
                                            pcAvail.getPagesize());

        filteredAvailList.setTotalSize( avail.getTotalSize() );

        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, avail);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR,
                             new Integer(filteredAvailList.getTotalSize()));

        log.debug("get the available resources user can filter by");                                                
        setDropDowns(boss, addForm, request, sessionId.intValue(), appdefType,
                     compat, (BaseActionMapping) mapping); 

        return null;

    }

    /**
     * returns a filtered subset of available resources for a given resource based on 
     * the the resource type 
     * type
     * 
     * @return a list of AppdefResourceValue objects
     */
    public static List subsetAvailableResources(List avail, Integer filterType) {
        List resources = new ArrayList();
        Iterator aIterator = avail.iterator();
        while (aIterator.hasNext())
        {
            AppdefResourceValue aVal = (AppdefResourceValue)aIterator.next();
            AppdefResourceTypeValue rVal = aVal.getAppdefResourceTypeValue();
            if (rVal == null)
                continue;
            int id = rVal.getAppdefType();
            if (id == filterType.intValue())
                resources.add(aVal);
        }
                    
        return resources;
    }

    /**
     * builds a unique list of AppdefResourceTypeValue objects
     * 
     * @return a unique list of AppdefResourceTypeValue objects
     */
    public List buildAvailableResourceTypes(List avail) {
        List resourceTypes = new ArrayList();
        Iterator aIterator = avail.iterator();
        while (aIterator.hasNext())
        {
            AppdefResourceValue aVal = (AppdefResourceValue)aIterator.next();
            AppdefResourceTypeValue  aType = 
                    findAppdefResourceType(resourceTypes,
                        aVal.getAppdefResourceTypeValue());
            if (aType == null && aVal.getAppdefResourceTypeValue() != null)
                resourceTypes.add(aVal.getAppdefResourceTypeValue());
        }
        
        List resType = BizappUtils.buildAppdefOptionList(resourceTypes, false);            
        return resType;
    }

    
    private AppdefResourceTypeValue findAppdefResourceType(
                                    List resourceType, 
                                    AppdefResourceTypeValue compare)
    {
        Iterator rIterator = resourceType.iterator();
        while (rIterator.hasNext())
        {
            AppdefResourceTypeValue val = 
                    (AppdefResourceTypeValue)rIterator.next();
            if (val != null && compare != null && val.getId() != null && 
                        val.getId().equals(compare.getId()))
                return val;
        }
        
        return null;
    }
    
    private void setDropDowns(AppdefBoss boss, 
                              AddResourcesForm addForm, 
                              HttpServletRequest request,
                              int sessionId,
                              int appdefType,
                              boolean compat,
                              BaseActionMapping mapping)
    throws Exception{

        //just need  a blank one for this stuff
        PageControl pc = PageControl.PAGE_ALL;
        
        // set up resource "functions" (appdef entity s)
        String[][] entityTypes = boss.getAppdefTypeStrArrMap();

        // CAM's group constructs suck, so we do sucky things to support them
        boolean pss = "platform-server-service".equals(mapping.getWorkflow());
        
        if (entityTypes != null){
            for (int i = 0; i < entityTypes.length; i++) {
                int type = Integer.parseInt(entityTypes[i][0]);
                if (pss && type > AppdefEntityConstants.APPDEF_TYPE_SERVICE)
                    continue;
                
                // suck: for the portlet's purposes, explicitly call
                // "Groups" "Mixed Groups"
                if (type == AppdefEntityConstants.APPDEF_TYPE_GROUP)
                    continue;
                
                addForm.addFunction(new LabelValueBean(entityTypes[i][1],
                                                       entityTypes[i][0]));

            } 
            
            if (!pss) {
                // there are two "major" types of groups, suckah mofo
                addForm.addFunction( new LabelValueBean("mixedGroups", 
                        Integer.toString(AppdefEntityConstants.APPDEF_TYPE_GROUP)));
                addForm.addFunction( new LabelValueBean("compatibleGroups", 
                        Integer.toString(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC ) ) );
            }
        }
        
        if(appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP){
            if(compat){
                // the entity is a compatible group- we build a
                // combined menu containing all platform, server and
                // service types
                List platformTypes =
                    boss.findViewablePlatformTypes(sessionId, pc);
                addCompatTypeOptions(addForm, platformTypes,
                                     msg(request, PLATFORM_KEY));
                List serverTypes =
                    boss.findViewableServerTypes(sessionId, pc);
                addCompatTypeOptions(addForm, serverTypes,
                                     msg(request, SERVER_KEY));
                List serviceTypes =
                    boss.findViewableServiceTypes(sessionId, pc);
                addCompatTypeOptions(addForm, serviceTypes,
                                     msg(request, SERVICE_KEY));
                
            }
            else{
                
                addForm.addType(new LabelValueBean(msg(request,
                                                   GROUP_ADHOC_GRP_KEY),
                                                   GROUP_ADHOC_GRP_VAL));
                addForm.addType(new LabelValueBean(msg(request,
                                                   GROUP_ADHOC_PSS_KEY),
                                                   GROUP_ADHOC_PSS_VAL));
                addForm.addType(new LabelValueBean(msg(request,
                                                   GROUP_ADHOC_APP_KEY),
                                                   GROUP_ADHOC_APP_VAL));
            }
        
        }
        else{    
            List types = boss.findAllResourceTypes(sessionId, appdefType, pc);
            for (Iterator itr=types.iterator();itr.hasNext();) {
                AppdefResourceTypeValue value=(AppdefResourceTypeValue)itr.next();
                addForm.addType(new LabelValueBean(value.getName(), value.getId().toString()));
            }        
        }
            
            
        
            
    }
    
    private void addCompatTypeOptions(AddResourcesForm form, List types,
                                      String label) {
        if (types.size() > 0) {
            form.addType(new LabelValueBean(BLANK_LABEL, BLANK_VAL));
            form.addType(new LabelValueBean(label, BLANK_VAL));
            addTypeOptions(form, types);
        }
    }
    
    private void addTypeOptions(AddResourcesForm form, List types) {
        if (types.size() > 0) {
            for (Iterator itr = types.iterator(); itr.hasNext(); ) {
                AppdefResourceTypeValue value =
                    (AppdefResourceTypeValue) itr.next();
                form.addType(new LabelValueBean(value.getName(),
                                                   value.getId().toString()));
            }
        }
    }
    
    private String msg(HttpServletRequest request, String key) {
        return RequestUtils.message(request, key);
    }
    
    
    
}

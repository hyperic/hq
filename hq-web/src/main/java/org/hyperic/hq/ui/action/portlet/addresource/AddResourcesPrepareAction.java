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

package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.ArrayList;
import java.util.List;

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
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An Action that retrieves data from the user preferences of the form. The
 * purpose of this is to add resources to the resource health dashboard widget
 * 
 * This implementation heavily based on:
 * org.hyperic.hq.ui.action.admin.role.AddUsersRoleFormPrepareAction
 */
public class AddResourcesPrepareAction
    extends Action {

    private static final String BLANK_LABEL = "";
    private static final String BLANK_VAL = "";
    private static final String PLATFORM_KEY = "resource.hub.filter.PlatformType";
    private static final String SERVER_KEY = "resource.hub.filter.ServerType";
    private static final String SERVICE_KEY = "resource.hub.filter.ServiceType";
    private static final String GROUP_ADHOC_GRP_KEY = "resource.hub.filter.GroupGroup";
    private static final String GROUP_ADHOC_GRP_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)
        .toString();
    private static final String GROUP_ADHOC_PSS_KEY = "resource.hub.filter.GroupPSS";
    private static final String GROUP_ADHOC_PSS_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS)
        .toString();
    private static final String GROUP_ADHOC_APP_KEY = "resource.hub.filter.GroupApp";
    private static final String GROUP_ADHOC_APP_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)
        .toString();

    private static final int DEFAULT_RESOURCE_TYPE = -1;
    private final Log log = LogFactory.getLog(AddResourcesPrepareAction.class.getName());
    private AuthzBoss authzBoss;
    private AppdefBoss appdefBoss;
    private DashboardManager dashboardManager;

    @Autowired
    public AddResourcesPrepareAction(AuthzBoss authzBoss, AppdefBoss appdefBoss, DashboardManager dashboardManager) {
        super();
        this.authzBoss = authzBoss;
        this.appdefBoss = appdefBoss;
        this.dashboardManager = dashboardManager;
    }

    /**
     * Retrieve this data and store it in the specified request parameters:
     * 
     * <ul>
     * <li><code>GroupValue</code> object identified by
     * <code>Constants.RESOURCE_PARAM</code> request parameter in
     * <code>Constants.RESOURCE_ATTR</code></li>
     * 
     * <li><code>List</code> of available <code>AppdefResourceValue</code>
     * objects (those not already associated with the group) in
     * <code>Constants.AVAIL_RESOURCES_ATTR</code></li>
     * <li><code>Integer</code> number of available roles in
     * <code>Constants.NUM_AVAIL_RESOURCES_ATTR</code></li>
     * 
     * <li><code>List</code> of pending <code>OwnedRoleValue</code> objects
     * (those in queue to be associated with the resource) in
     * <code>Constants.PENDING_RESOURCES_ATTR</code></li>
     * <li><code>Integer</code> number of pending resources in
     * <code>Constants.NUM_PENDING_RESOURCES_ATTR</code></li>
     * 
     * <li><code>List</code> of pending <code>AppdefResourceValue</code> ids
     * (those in queue to be associated with the resource) in
     * <code>Constants.PENDING_RESOURCES_SES_ATTR</code></li>
     * </ul>
     * 
     * This Action edits 2 lists of Resources: pending, and available.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        AddResourcesForm addForm = (AddResourcesForm) form;

        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        Integer sessionId = user.getSessionId();

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        PageControl pcAvail = RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
        PageControl pcPending = RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");

        /*
         * pending resources are those on the right side of the "add to list"
         * widget- awaiting association with the group when the form's "ok"
         * button is clicked.
         */

        log.debug("check session if there are pending resources");
        List pendingResourcesIds = (List) session.getAttribute(Constants.PENDING_RESOURCES_SES_ATTR);

        if (pendingResourcesIds == null) {
            log.debug("get avalable resources from user preferences");
            try {
                pendingResourcesIds = dashPrefs.getPreferenceAsList(addForm.getKey(),
                    StringConstants.DASHBOARD_DELIMITER);
            } catch (InvalidOptionException e) {
                // Then we don't have any pending resources
                pendingResourcesIds = new ArrayList(0);
            }
            log.debug("put entire list of pending resources in session");
            session.setAttribute(Constants.PENDING_RESOURCES_SES_ATTR, pendingResourcesIds);
        }

        log.debug("get page of pending resources selected by user");
        Pager pendingPager = Pager.getDefaultPager();
        List pendingResources = DashboardUtils.listAsResources(pendingResourcesIds, getServlet().getServletContext(),
            user, appdefBoss);

        PageList pageOfPendingResources = pendingPager.seek(pendingResources, pcPending.getPagenum(), pcPending
            .getPagesize());

        log.debug("put selected page of pending resources in request");

        request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pageOfPendingResources);
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR, new Integer(pageOfPendingResources.getTotalSize()));

        /*
         * available resources are all resources in the system that are not
         * associated with the user and are not pending
         */
        log.debug("determine if user wants to filter available resources");

        Integer ff = addForm.getFf();
        AppdefEntityTypeID ft = null;

        int appdefType = (ff == null) ? Constants.FILTER_BY_DEFAULT : ff.intValue();

        if (addForm.getFt() != null && !addForm.getFt().equals(String.valueOf(DEFAULT_RESOURCE_TYPE))) {
            try {
                ft = new AppdefEntityTypeID(addForm.getFt());
            } catch (InvalidAppdefTypeException e) {
                ft = new AppdefEntityTypeID(appdefType, new Integer(addForm.getFt()));
            }
        }

        int resourceType = ft == null ? -1 : ft.getID();
        boolean compat = false;
        if (appdefType == 0)
            appdefType = Constants.FILTER_BY_DEFAULT;
        else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) {
            // this is all to accomidate the compat group type as a seperate
            // dropdown
            appdefType = AppdefEntityConstants.APPDEF_TYPE_GROUP;
            compat = true;
        }

        List<AppdefEntityID> pendingEntityIds = DashboardUtils.listAsEntityIds(pendingResourcesIds);

        AppdefEntityID[] pendingEntities = pendingEntityIds.toArray(new AppdefEntityID[0]);

        PageList<AppdefResourceValue> avail;
        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            int groupSubtype = -1;

            if (compat) {
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
            } else {
                // resourceType straight up tells us what group
                // subtype was chosen
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;

                // for findCompatInventory, resourceType always need
                // to be this, for whatever reason
                resourceType = DEFAULT_RESOURCE_TYPE;
            }

            avail = appdefBoss.findCompatInventory(sessionId.intValue(), groupSubtype, appdefType,
                ft == null ? DEFAULT_RESOURCE_TYPE : ft.getType(), resourceType, addForm.getNameFilter(),
                pendingEntities, pcAvail);
        } else {
            avail = appdefBoss.findCompatInventory(sessionId.intValue(), appdefType, resourceType, null,
                pendingEntities, addForm.getNameFilter(), pcAvail);
        }

        PageList filteredAvailList = new PageList();
        Pager availPager = Pager.getDefaultPager();

        filteredAvailList = availPager.seek(avail, pcAvail.getPagenum(), pcAvail.getPagesize());

        filteredAvailList.setTotalSize(avail.getTotalSize());

        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, avail);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR, new Integer(filteredAvailList.getTotalSize()));

        log.debug("get the available resources user can filter by");
        setDropDowns(addForm, request, sessionId.intValue(), appdefType, compat, (BaseActionMapping) mapping);

        return null;

    }

    private void setDropDowns(AddResourcesForm addForm, HttpServletRequest request, int sessionId, int appdefType,
                              boolean compat, BaseActionMapping mapping) throws Exception {

        // just need a blank one for this stuff
        PageControl pc = PageControl.PAGE_ALL;

        // set up resource "functions" (appdef entity s)
        String[][] entityTypes = appdefBoss.getAppdefTypeStrArrMap();

        // CAM's group constructs suck, so we do sucky things to support them
        boolean pss = "platform-server-service".equals(mapping.getWorkflow());

        if (entityTypes != null) {
            for (int i = 0; i < entityTypes.length; i++) {
                int type = Integer.parseInt(entityTypes[i][0]);
                if (pss && type > AppdefEntityConstants.APPDEF_TYPE_SERVICE)
                    continue;

                // suck: for the portlet's purposes, explicitly call
                // "Groups" "Mixed Groups"
                if (type == AppdefEntityConstants.APPDEF_TYPE_GROUP)
                    continue;

                addForm.addFunction(new LabelValueBean(entityTypes[i][1], entityTypes[i][0]));

            }

            if (!pss) {
                // there are two "major" types of groups, suckah mofo
                addForm.addFunction(new LabelValueBean("mixedGroups", Integer
                    .toString(AppdefEntityConstants.APPDEF_TYPE_GROUP)));
                addForm.addFunction(new LabelValueBean("compatibleGroups", Integer
                    .toString(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC)));
            }
        }

        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            if (compat) {
                // the entity is a compatible group- we build a
                // combined menu containing all platform, server and
                // service types
                List<PlatformTypeValue> platformTypes = appdefBoss.findViewablePlatformTypes(sessionId, pc);
                addCompatTypeOptions(addForm, platformTypes, msg(request, PLATFORM_KEY));
                List<ServerTypeValue> serverTypes = appdefBoss.findViewableServerTypes(sessionId, pc);
                addCompatTypeOptions(addForm, serverTypes, msg(request, SERVER_KEY));
                List<ServiceTypeValue> serviceTypes = appdefBoss.findViewableServiceTypes(sessionId, pc);
                addCompatTypeOptions(addForm, serviceTypes, msg(request, SERVICE_KEY));

            } else {

                addForm.addType(new LabelValueBean(msg(request, GROUP_ADHOC_GRP_KEY), GROUP_ADHOC_GRP_VAL));
                addForm.addType(new LabelValueBean(msg(request, GROUP_ADHOC_PSS_KEY), GROUP_ADHOC_PSS_VAL));
                addForm.addType(new LabelValueBean(msg(request, GROUP_ADHOC_APP_KEY), GROUP_ADHOC_APP_VAL));
            }

        } else {
            List<AppdefResourceTypeValue> types = appdefBoss.findAllResourceTypes(sessionId, appdefType, pc);
            for (AppdefResourceTypeValue value : types) {
                addForm.addType(new LabelValueBean(value.getName(), value.getId().toString()));
            }
        }

    }

    private void addCompatTypeOptions(AddResourcesForm form, List<? extends AppdefResourceTypeValue> types, String label) {
        if (types.size() > 0) {
            form.addType(new LabelValueBean(BLANK_LABEL, BLANK_VAL));
            form.addType(new LabelValueBean(label, BLANK_VAL));
            addTypeOptions(form, types);
        }
    }

    private void addTypeOptions(AddResourcesForm form, List<? extends AppdefResourceTypeValue> types) {
        if (types.size() > 0) {
            for (AppdefResourceTypeValue value : types) {
                form.addType(new LabelValueBean(value.getName(), value.getAppdefTypeKey()));
            }
        }
    }

    private String msg(HttpServletRequest request, String key) {
        return RequestUtils.message(request, key);
    }

}

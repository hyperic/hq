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

package org.hyperic.hq.ui.action.resource.hub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.taglib.display.StringUtil;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/**
 * An <code>Action</code> that sets up the Resource Hub portal.
 */
public class ResourceHubPortalAction extends BaseAction {

    private static final String BLANK_LABEL = "";
    private static final String BLANK_VAL = "";
    private static final String PLATFORM_KEY =
        "resource.hub.filter.PlatformType";
    private static final String SERVER_KEY = "resource.hub.filter.ServerType";
    private static final String SERVICE_KEY = "resource.hub.filter.ServiceType";
    public static final int SELECTOR_GROUP_COMPAT = 1;
    public static final int SELECTOR_GROUP_ADHOC = 2;

    private static final int DEFAULT_ENTITY_TYPE = Constants.FILTER_BY_DEFAULT;
    private static final int DEFAULT_RESOURCE_TYPE = -1;
    private static final int DEFAULT_GROUP_TYPE = 1;
    private static final String DEFAULT_RESOURCE_NAME = null;

    private static final String SEPARATOR = " > ";
    private static final String VIEW_ATTRIB = "Resource Hub View";
    private static final String TYPE_ATTRIB = "Resource Hub Apppdef Type";
    private static final String GRP_ATTRIB  = "Resource Hub Group Type";
    
    protected Log log =
        LogFactory.getLog(ResourceHubPortalAction.class.getName());

    /**
     * Set up the Resource Hub portal.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        boolean prefChanged = false;
        
        ResourceHubForm hubForm = (ResourceHubForm) form;
        
        int sessionId = RequestUtils.getSessionId(request).intValue();
        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);

        // Set the view in the form
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        
        PageControl pc = RequestUtils.getPageControl(request);

        /* Disable setting of paging preferences
        if (hubForm.getPs() != null) {
            user.setPreference(PAGE_ATTRIB, hubForm.getPs());
            prefChanged = true; // Save new preference
        } else {
            try {
                String pageSizeStr = user.getPreference(PAGE_ATTRIB);
                Integer pageSize = Integer.valueOf(pageSizeStr);
                if (pageSize.intValue() != pc.getPagesize()) {
                    pc.setPagenum(pageSize.intValue());
                    hubForm.setPs(pageSize);
                }
            } catch (InvalidOptionException e) {
                // Just use default
            }
        }
        */
        
        String view = hubForm.getView();
        if (!ResourceHubForm.LIST_VIEW.equals(view) &&
            !ResourceHubForm.CHART_VIEW.equals(view)) { // Invalid view
            view = null;
        }
        
        String prefView;
        try {
            prefView = user.getPreference(VIEW_ATTRIB);
        } catch (InvalidOptionException e) {
            prefView = ResourceHubForm.LIST_VIEW;
        }
        
        if (view == null) {
            hubForm.setView(prefView);
        }
        else if (!view.equals(prefView)) {
            user.setPreference(VIEW_ATTRIB, view);
            prefChanged = true;                         // Save new preference
        }
        
        String navHierarchy = null;
        
        // find resources specified by entity type and potentially
        // resource type. collect query parameters and replace invalid
        // ones with defaults.
        int entityType;
        
        try {
            String prefFFStr = user.getPreference(TYPE_ATTRIB);
            entityType = Integer.parseInt(prefFFStr);
        } catch (InvalidOptionException e) {
            entityType = DEFAULT_ENTITY_TYPE;
        }
        
        Integer ff = hubForm.getFf();
        if (ff == null || ff.intValue() == 0) {
            ff = new Integer(entityType);
            hubForm.setFf(ff);
        }
        else if (ff.intValue() != entityType) {
            entityType = ff.intValue();
            user.setPreference(TYPE_ATTRIB, new Integer(entityType));
            prefChanged = true;                         // Save new preference
        }
        
        // start the navHierarchy with the ff type
        navHierarchy = StringUtil.toUpperCaseAt(
                AppdefEntityConstants.typeToString(entityType), 0) + "s"
                + SEPARATOR;
        
        // are we looking at a group? check now, because if we *are*
        // looking at a group, we're going to override the entity
        // type.

        boolean isGroupSelected = isGroupSelected(entityType);

        int resourceType = DEFAULT_RESOURCE_TYPE;
        String ft = hubForm.getFt();
        AppdefEntityTypeID aetid = null;
        if (ff.intValue() != AppdefEntityConstants.APPDEF_TYPE_APPLICATION &&
            ft != null && ft.length() > 0) {
            try {
                // compat groups use the entity id format for ft
                aetid = new AppdefEntityTypeID(ft);
                resourceType = aetid.getId().intValue();
                // only compat groups specify ft as an entity id. in
                // this case, the entity id's entity type overrides
                // the one specified by ff (which is just
                // APPDEF_TYPE_GROUP anyway).
                entityType = aetid.getType();
            }
            catch (InvalidAppdefTypeException e) {
                // what we got from the menu was not an entity id at
                // all, but rather an integer
                resourceType = (new Integer(ft)).intValue();
            }
            
            if (aetid != null) {
                String typeName = appdefBoss.findResourceTypeById(
                    sessionId, new AppdefEntityTypeID(ft)).getName();
                navHierarchy += typeName;
            }
        }
        else {
            hubForm.setFt(new Integer(resourceType).toString());
            navHierarchy += "All " + StringUtil.toUpperCaseAt(
                    AppdefEntityConstants.typeToString(entityType), 0) + "s";
        }

        Integer g = hubForm.getG();
        int groupType;
        if (g == null || g.intValue() < SELECTOR_GROUP_COMPAT) {
            try {
                String grpTypeStr = user.getPreference(GRP_ATTRIB);
                groupType = Integer.parseInt(grpTypeStr);
            } catch (InvalidOptionException e) {
                groupType = DEFAULT_GROUP_TYPE;
            }
            hubForm.setG(new Integer(groupType));
        }
        else {
            groupType = g.intValue();
            user.setPreference(GRP_ATTRIB, new Integer(groupType));
            prefChanged = true;                         // Save new preference
        }

        MessageResources res = getResources(request);
        String jsInserted =
            res.getMessage("resource.hub.search.KeywordSearchText");
        String resourceName = hubForm.getKeywords();
        if (resourceName != null &&
            (resourceName.equals("") || resourceName.equals(jsInserted))) {
            resourceName = DEFAULT_RESOURCE_NAME;
            hubForm.setKeywords(resourceName);
        }

        PageList resources = null;
        StopWatch watch = new StopWatch();
        watch.markTimeBegin("findCompatInventory");
        Integer gid = null;
        int[] groupSubtype = null;
        
        if (isGroupSelected) {
            // as far as the backend is concerned, the resource type
            // is the actual group type we want. our groupType just
            // tells us compat or adhoc.
            if (isCompatGroupSelected(groupType)) {
                // entity type tells us which type of compat group was
                // chosen
                groupSubtype = new int[] {
                        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS,
                        AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
                };
            }
            else if (isAdhocGroupSelected(groupType)) {
                if (resourceType != DEFAULT_RESOURCE_TYPE) {
                    // resourceType straight up tells us what groupsubtype was 
                    // chosen
                    groupSubtype = new int[] { resourceType };
                }
                else {
                    groupSubtype = new int[] {
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
                            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
                    };
                }

                // for findCompatInventory, resourceType always need
                // to be this, for whatever reason
                resourceType = DEFAULT_RESOURCE_TYPE;
            }
            else {
                throw new ServletException("Invalid group type: " + groupType);
            }
        }
        else {
            // Look up groups
            Collection groups = appdefBoss.findAllGroupPojos(sessionId);
            
            if (groups.size() > 0) {
                ArrayList groupOptions = new ArrayList(groups.size());
                
                for (Iterator it = groups.iterator(); it.hasNext(); ) {
                    ResourceGroup group = (ResourceGroup) it.next();

                    String appdefKey =
                        AppdefEntityID.newGroupID(group.getId()).getAppdefKey();
                    groupOptions.add(new LabelValueBean(group.getName(),
                                                        appdefKey));
                }
                
                // Set the group options in request
                request.setAttribute(Constants.AVAIL_RESGRPS_ATTR,
                                     groupOptions);
            }
            
            // Lastly, check for group to filter by
            if (hubForm.getFg() != null && hubForm.getFg().length() > 0) {
                AppdefEntityID geid = new AppdefEntityID(hubForm.getFg());
                gid = geid.getId();
            }
            
        }
        
        // TODO: Pass groupSubType as int[]
        resources = appdefBoss.search(sessionId, entityType, resourceName,
                                      aetid, gid, groupSubtype, 
                                      hubForm.isAny(),
                                      pc);

        watch.markTimeEnd("findCompatInventory");

        request.setAttribute(Constants.ALL_RESOURCES_ATTR, resources);

        ArrayList ids = new ArrayList();
        if (resources != null) {
            for (Iterator it = resources.iterator(); it.hasNext();) {
                AppdefResourceValue rv = (AppdefResourceValue) it.next();
                ids.add(rv.getEntityId());
            }
        }
        
        watch.markTimeBegin("batchCheckControlPermissions");
        if (ids.size() > 0) {
            if (prefView.equals(ResourceHubForm.LIST_VIEW) &&
                !isGroupSelected && resourceType != DEFAULT_RESOURCE_TYPE) {
                // Get the indicator templates
                MeasurementBoss mboss = ContextUtils.getMeasurementBoss(ctx);

                HashSet cats = new HashSet(3);
                cats.add(MeasurementConstants.CAT_UTILIZATION);
                cats.add(MeasurementConstants.CAT_THROUGHPUT);
                cats.add(MeasurementConstants.CAT_PERFORMANCE);

                Collection templates = null;
                
                for (Iterator it = ids.iterator(); it.hasNext(); ) {
                    templates =
                        mboss.getDesignatedTemplates(sessionId,
                                                     (AppdefEntityID) it.next(),
                                                     cats);
                    
                    if (templates.size() > 0)
                        break;
                }
                
                if (templates.size() > 0)
                    request.setAttribute("Indicators", templates);
            }

            /*
            // Set additional flags
            ControlBoss controlBoss = ContextUtils.getControlBoss(ctx);
            List controllableResources =
                controlBoss.batchCheckControlPermissions(sessionId, idArr);
            request.setAttribute(Constants.ALL_RESOURCES_CONTROLLABLE,
                                 controllableResources);
                                 */
        }
        
        watch.markTimeEnd("batchCheckControlPermissions");

        // retrieve inventory summary
        watch.markTimeBegin("getInventorySummary");
        AppdefInventorySummary summary =
            appdefBoss.getInventorySummary(sessionId, false);
        request.setAttribute(Constants.RESOURCE_SUMMARY_ATTR, summary);
        watch.markTimeEnd("getInventorySummary");

        watch.markTimeBegin("findAllResourceTypes");
        // generate list of selectable resource types for the chosen
        // entity type.
        pc = PageControl.PAGE_ALL;
        if (isGroupSelected) {
            if (isCompatGroupSelected(groupType)) {
                // the entity is a compatible group- we build a
                // combined menu containing all platform, server and
                // service types

                List platformTypes =
                    appdefBoss.findViewablePlatformTypes(sessionId, pc);
                addCompatTypeOptions(hubForm, platformTypes,
                                     msg(request, PLATFORM_KEY));

                List serverTypes =
                    appdefBoss.findViewableServerTypes(sessionId, pc);
                addCompatTypeOptions(hubForm, serverTypes,
                                     msg(request, SERVER_KEY));

                List serviceTypes =
                    appdefBoss.findViewableServiceTypes(sessionId, pc);
                addCompatTypeOptions(hubForm, serviceTypes,
                                     msg(request, SERVICE_KEY));
            }
            else if (isAdhocGroupSelected(groupType)) {
                // the entity is an adhoc group- we offer no adhoc group
                // options
                addMixedTypeOptions(hubForm, res);
            }
            else {
                throw new ServletException("invalid group type: " + groupType);
            }
        }
        else {
            // the entity is not a group- this is easy.
            List types =
                appdefBoss.findAllResourceTypes(sessionId, entityType, pc);
            addTypeOptions(hubForm, types);
        }
        
        watch.markTimeEnd("findAllResourceTypes");
        if (log.isDebugEnabled()) {
            log.debug("ResourceHubPortalAction: " + watch);
        }

        // Save the preferences if necessary
        if (prefChanged) {
            AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
            boss.setUserPrefs(user.getSessionId(), user.getId(),
                              user.getPreferences());
        }
        
        // clean out the return path 
        SessionUtils.resetReturnPath(request.getSession());
        
        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle",
                                            ".resource.hub");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);
        
        return null;
    }

    private void addTypeOptions(ResourceHubForm form, List types) {
        if (types.size() > 0) {
            for (Iterator itr = types.iterator(); itr.hasNext(); ) {
                AppdefResourceTypeValue value =
                    (AppdefResourceTypeValue) itr.next();
                form.addType(new LabelValueBean(value.getName(),
                                                value.getAppdefTypeKey()));
            }
        }
    }

    private void addCompatTypeOptions(ResourceHubForm form, List types,
                                      String label) {
        if (types.size() > 0) {
            form.addType(new LabelValueBean(BLANK_LABEL, BLANK_VAL));
            form.addType(new LabelValueBean(label, BLANK_VAL));
            addTypeOptions(form, types);
        }
    }
    
    private void addMixedTypeOptions(ResourceHubForm form, MessageResources mr)
    {
        form.addType(
            new LabelValueBean(
                mr.getMessage("resource.group.inventory.New.props.GroupOfGroups"),
                String.valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)));
        form.addType(
            new LabelValueBean(
                mr.getMessage("resource.group.inventory.New.props.GroupOfMixed"),
                String.valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS)));
        form.addType(
            new LabelValueBean(
                mr.getMessage("resource.group.inventory.New.props.GroupOfApplications"),
                String.valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)));
    }

    private boolean isAdhocGroupSelected(int type) {
        return type == SELECTOR_GROUP_ADHOC;
    }

    private boolean isCompatGroupSelected(int type) {
        return type == SELECTOR_GROUP_COMPAT;
    }

    private boolean isGroupSelected(int type) {
        return type == AppdefEntityConstants.APPDEF_TYPE_GROUP;
    }

    private String msg(HttpServletRequest request, String key) {
        return RequestUtils.message(request, key);
    }
}

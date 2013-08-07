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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.taglib.display.StringUtil;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;
import org.hyperic.util.units.FormattedNumber;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> that sets up the Resource Hub portal.
 */
public class ResourceHubPortalAction
    extends BaseAction {

    private static final String BLANK_LABEL = "";
    private static final String BLANK_VAL = "";
    private static final String PLATFORM_KEY = "resource.hub.filter.PlatformType";
    private static final String SERVER_KEY = "resource.hub.filter.ServerType";
    private static final String SERVICE_KEY = "resource.hub.filter.ServiceType";
    public static final int SELECTOR_GROUP_COMPAT = 1;
    public static final int SELECTOR_GROUP_ADHOC = 2;
    public static final int SELECTOR_GROUP_DYNAMIC = 3;

    private static final int DEFAULT_ENTITY_TYPE = Constants.FILTER_BY_DEFAULT;
    private static final int DEFAULT_RESOURCE_TYPE = -1;
    private static final int DEFAULT_GROUP_TYPE = 1;
    private static final String DEFAULT_RESOURCE_NAME = null;

    private static final String SEPARATOR = "&nbsp;&rsaquo;&nbsp;";
    private static final String VIEW_ATTRIB = "Resource Hub View";
    private static final String TYPE_ATTRIB = "Resource Hub Apppdef Type";
    private static final String GRP_ATTRIB = "Resource Hub Group Type";

    private final Log log = LogFactory.getLog(ResourceHubPortalAction.class.getName());
    private AppdefBoss appdefBoss;
    private AuthzBoss authzBoss;
    private MeasurementBoss measurementBoss;
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public ResourceHubPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, MeasurementBoss measurementBoss, AlertPermissionManager alertPermissionManager) {
        super();
        this.appdefBoss = appdefBoss;
        this.authzBoss = authzBoss;
        this.measurementBoss = measurementBoss;
        this.alertPermissionManager = alertPermissionManager;
    }

    /**
     * Set up the Resource Hub portal.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        boolean prefChanged = false;

        ResourceHubForm hubForm = (ResourceHubForm) form;

        int sessionId = RequestUtils.getSessionId(request).intValue();

        // Set the view in the form
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        PageControl pc = RequestUtils.getPageControl(request);

        String view = hubForm.getView();
        if (!ResourceHubForm.LIST_VIEW.equals(view) && !ResourceHubForm.CHART_VIEW.equals(view)) { // Invalid
                                                                                                   // view
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
        } else if (!view.equals(prefView)) {
            user.setPreference(VIEW_ATTRIB, view);
            prefChanged = true; // Save new preference
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
        } else if (ff.intValue() != entityType) {
            entityType = ff.intValue();
            user.setPreference(TYPE_ATTRIB, new Integer(entityType));
            prefChanged = true; // Save new preference
        }

        // start the navHierarchy with the ff type
        navHierarchy = StringUtil.toUpperCaseAt(AppdefEntityConstants.typeToString(entityType), 0) + "s" + SEPARATOR;

        // are we looking at a group? check now, because if we *are*
        // looking at a group, we're going to override the entity
        // type.

        boolean isGroupSelected = isGroupSelected(entityType);

        int resourceType = DEFAULT_RESOURCE_TYPE;
        String ft = hubForm.getFt();
        AppdefEntityTypeID aetid = null;
        if (ff.intValue() != AppdefEntityConstants.APPDEF_TYPE_APPLICATION && ft != null && ft.length() > 0 &&
            !ft.equals(String.valueOf(DEFAULT_RESOURCE_TYPE))) {
            try {
                // compat groups use the entity id format for ft
                aetid = new AppdefEntityTypeID(ft);
                resourceType = aetid.getId().intValue();
                // only compat groups specify ft as an entity id. in
                // this case, the entity id's entity type overrides
                // the one specified by ff (which is just
                // APPDEF_TYPE_GROUP anyway).
                entityType = aetid.getType();
            } catch (InvalidAppdefTypeException e) {
                // what we got from the menu was not an entity id at
                // all, but rather an integer
                resourceType = (new Integer(ft)).intValue();
            }

            if (aetid != null) {
                String typeName = appdefBoss.findResourceTypeById(sessionId, new AppdefEntityTypeID(ft)).getName();
                navHierarchy += typeName;
            }
        } else {
            hubForm.setFt(new Integer(resourceType).toString());
            navHierarchy += "All " + StringUtil.toUpperCaseAt(AppdefEntityConstants.typeToString(entityType), 0) + "s";
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
        } else {
            groupType = g.intValue();
            user.setPreference(GRP_ATTRIB, new Integer(groupType));
            prefChanged = true; // Save new preference
        }

        MessageResources res = getResources(request);
        String jsInserted = res.getMessage("resource.hub.search.KeywordSearchText");
        String resourceName = hubForm.getKeywords();
        if (resourceName != null && (resourceName.equals("") || resourceName.equals(jsInserted))) {
            resourceName = DEFAULT_RESOURCE_NAME;
            hubForm.setKeywords(resourceName);
        }

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
                groupSubtype = new int[] { AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS,
                                          AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC };
            } else if (isAdhocGroupSelected(groupType)) {
                if (resourceType != DEFAULT_RESOURCE_TYPE) {
                    // resourceType straight up tells us what groupsubtype was
                    // chosen
                    groupSubtype = new int[] { resourceType };
                } else {
                    groupSubtype = new int[] { AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
                                              AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
                                              AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP };
                }

                // for findCompatInventory, resourceType always need
                // to be this, for whatever reason
                resourceType = DEFAULT_RESOURCE_TYPE;
            } else if (isDynamicGroupSelected(groupType)){
                groupSubtype = new int[] { AppdefEntityConstants.APPDEF_TYPE_GROUP_DYNAMIC };
            } else {
                throw new ServletException("Invalid group type: " + groupType);
            }
        } else {
            // Look up groups
            Collection<ResourceGroup> groups = appdefBoss.findAllGroupPojos(sessionId);

            if (groups.size() > 0) {
                ArrayList<LabelValueBean> groupOptions = new ArrayList<LabelValueBean>(groups.size());

                for (ResourceGroup group : groups) {

                    String appdefKey = AppdefEntityID.newGroupID(group.getId()).getAppdefKey();
                    groupOptions.add(new LabelValueBean(group.getName(), appdefKey));
                }

                // Set the group options in request
                request.setAttribute(Constants.AVAIL_RESGRPS_ATTR, groupOptions);
            }

            // Lastly, check for group to filter by
            if (hubForm.getFg() != null && hubForm.getFg().length() > 0) {
                AppdefEntityID geid = new AppdefEntityID(hubForm.getFg());
                gid = geid.getId();
            }

        }

        // TODO: Pass groupSubType as int[]
        PageList<AppdefResourceValue> resources = appdefBoss.search(sessionId, entityType, org.hyperic.util.StringUtil
            .escapeForRegex(resourceName, true), aetid, gid, groupSubtype, hubForm.isAny(), hubForm.isOwn(), hubForm
            .isUnavail(), pc);

        // Generate root breadcrumb url based on the filter criteria
        // submitted...
        String rootBrowseUrl = BreadcrumbUtil.createRootBrowseURL(mapping.getInput(), hubForm, pc);

        // ...store it in the session, so that the bread crumb tag can get at it
        session.setAttribute(Constants.ROOT_BREADCRUMB_URL_ATTR_NAME, rootBrowseUrl);

        watch.markTimeEnd("findCompatInventory");

        request.setAttribute(Constants.ALL_RESOURCES_ATTR, resources);
        
        boolean canModify = false;
        ArrayList<AppdefEntityID> ids = new ArrayList<AppdefEntityID>();

        // ...to determine the resource type, first see if we have any resources...
        if (resources != null && resources.size() > 0) {
            // ...use the first element to get the resource type...
        	AppdefResourceValue resource = (AppdefResourceValue) resources.get(0);
            
        	try {
        		AuthzSubject subject = authzBoss.getCurrentSubject(sessionId);
	            
        		// ...check to see if user can modify resources of this type...
        		alertPermissionManager.canModifyAlertDefinition(subject, resource.getEntityId());
        		
        		canModify = true;
            } catch(PermissionException e) {
                // ...user doesn't have permission to modify this resource type...
            	log.debug("No permission to modify alert definition for resource: " + resource.getEntityId());
            }   

            for (AppdefResourceValue rv : resources) {
                ids.add(rv.getEntityId());
            }
        }

        request.setAttribute(Constants.CAN_MODIFY_ALERT_ATTR, canModify);
        
        watch.markTimeBegin("batchGetIndicators");
        if (ids.size() > 0) {
            if (prefView.equals(ResourceHubForm.LIST_VIEW) && !isGroupSelected && resourceType != DEFAULT_RESOURCE_TYPE) {
                // Get the indicator templates

                HashSet<String> cats = new HashSet<String>(3);
                cats.add(MeasurementConstants.CAT_UTILIZATION);
                cats.add(MeasurementConstants.CAT_THROUGHPUT);
                cats.add(MeasurementConstants.CAT_PERFORMANCE);

                Collection<MeasurementTemplate> templates = null;

                Map<AppdefEntityID, String[]> metricsMap = new HashMap<AppdefEntityID, String[]>(ids.size());
                for (AppdefEntityID entityId : ids) {
                    if (templates == null || templates.size() == 0) {
                        templates = measurementBoss.getDesignatedTemplates(sessionId, entityId, cats);

                        if (templates.size() > 0) {
                            request.setAttribute("Indicators", templates);
                        }
                    }

                    String[] metrics = getResourceMetrics(request, sessionId, measurementBoss, templates, entityId);
                    metricsMap.put(entityId, metrics);
                }
                request.setAttribute("indicatorsMap", metricsMap);
            }
        }

        watch.markTimeEnd("batchGetIndicators");

        // retrieve inventory summary
        watch.markTimeBegin("getInventorySummary");
        AppdefInventorySummary summary = appdefBoss.getInventorySummary(sessionId, false);
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
                final Collection<Integer> platformTypeIds = summary.getPlatformTypeMap().keySet();
                final Collection<AppdefResourceTypeValue> platformTypes =
                    new ArrayList<AppdefResourceTypeValue>(platformTypeIds.size());
                for (final Integer id : platformTypeIds) {
                    platformTypes.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
                addCompatTypeOptions(hubForm, platformTypes, msg(request, PLATFORM_KEY));

                final Collection<Integer> serverTypeIds = summary.getServerTypeMap().keySet();
                final Collection<AppdefResourceTypeValue> serverTypes =
                    new ArrayList<AppdefResourceTypeValue>(serverTypeIds.size());
                for (final Integer id : serverTypeIds) {
                    serverTypes.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
                addCompatTypeOptions(hubForm, serverTypes, msg(request, SERVER_KEY));

                final Collection<Integer> serviceTypeIds = summary.getServiceTypeMap().keySet();
                final Collection<AppdefResourceTypeValue> serviceTypes =
                    new ArrayList<AppdefResourceTypeValue>(serviceTypeIds.size());
                for (final Integer id : serviceTypeIds) {
                    serviceTypes.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
                addCompatTypeOptions(hubForm, serviceTypes, msg(request, SERVICE_KEY));
            } else if (isAdhocGroupSelected(groupType)) {
                // the entity is an adhoc group- we offer no adhoc group
                // options
                addMixedTypeOptions(hubForm, res);
            } else if (isDynamicGroupSelected(groupType)) {
                addDynamicTypeOptions(hubForm, res);
            } else {
                throw new ServletException("invalid group type: " + groupType);
            }
        } else {
            Collection<AppdefResourceTypeValue> types = new TreeSet<AppdefResourceTypeValue>(getTypeComparator());
            if (entityType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                final Collection<Integer> platformTypeIds = summary.getPlatformTypeMap().keySet();
                for (final Integer id : platformTypeIds) {
                    types.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
            } else if (entityType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
                final Collection<Integer> serverTypeIds = summary.getServerTypeMap().keySet();
                for (final Integer id : serverTypeIds) {
                    types.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
            } else if (entityType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
                final Collection<Integer> serviceTypeIds = summary.getServiceTypeMap().keySet();
                for (final Integer id : serviceTypeIds) {
                    types.add(appdefBoss.findResourceTypeByResId(sessionId, id));
                }
            }
            addTypeOptions(hubForm, types);
        }

        watch.markTimeEnd("findAllResourceTypes");
        if (log.isDebugEnabled()) {
            log.debug("ResourceHubPortalAction: " + watch);
        }

        // Save the preferences if necessary
        if (prefChanged) {

            authzBoss.setUserPrefs(user.getSessionId(), user.getId(), user.getPreferences());
        }

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle", ".resource.hub");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);

        return null;
    }

    private Comparator<AppdefResourceTypeValue> getTypeComparator() {
        return new Comparator<AppdefResourceTypeValue>() {
            public int compare(AppdefResourceTypeValue o1, AppdefResourceTypeValue o2) {
                if (o1 == o2) {
                    return 0;
                }
                int rtn = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                if (rtn != 0) {
                    return rtn;
                } else {
                    return o1.getId().compareTo(o2.getId());
                }
            }
        };
    }

    private String[] getResourceMetrics(HttpServletRequest request, int sessionId, MeasurementBoss mboss,
                                        Collection<MeasurementTemplate> templates, final AppdefEntityID entityId)
        throws RemoteException {
        Map<Integer, MetricValue> vals = mboss.getLastIndicatorValues(sessionId, entityId);

        // Format the values
        String[] metrics = new String[templates.size()];
        if (vals.size() == 0) {
            Arrays.fill(metrics, RequestUtils.message(request, "common.value.notavail"));
        } else {
            int i = 0;
            for (Iterator<MeasurementTemplate> it = templates.iterator(); it.hasNext(); i++) {
                MeasurementTemplate mt = it.next();

                if (vals.containsKey(mt.getId())) {
                    MetricValue mv = vals.get(mt.getId());
                    FormattedNumber fn = UnitsConvert.convert(mv.getValue(), mt.getUnits());
                    metrics[i] = fn.toString();
                } else {
                    metrics[i] = RequestUtils.message(request, "common.value.notavail");
                }
            }
        }
        return metrics;
    }

    private void addTypeOptions(ResourceHubForm form, Collection<? extends AppdefResourceTypeValue> types) {

        for (AppdefResourceTypeValue value : types) {
            form.addType(new LabelValueBean(value.getName(), value.getAppdefTypeKey()));
        }
    }

    private void addCompatTypeOptions(ResourceHubForm form, Collection<? extends AppdefResourceTypeValue> types, String label) {
        if (types != null && !types.isEmpty()) {
            form.addType(new LabelValueBean(BLANK_LABEL, BLANK_VAL));
            form.addType(new LabelValueBean(label, BLANK_VAL));
            addTypeOptions(form, types);
        }
    }

    private void addMixedTypeOptions(ResourceHubForm form, MessageResources mr) {
        form.addType(new LabelValueBean(mr.getMessage("resource.group.inventory.New.props.GroupOfGroups"), String
            .valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)));
        form.addType(new LabelValueBean(mr.getMessage("resource.group.inventory.New.props.GroupOfMixed"), String
            .valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS)));
        form.addType(new LabelValueBean(mr.getMessage("resource.group.inventory.New.props.GroupOfApplications"), String
            .valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)));
    }

    private void addDynamicTypeOptions(ResourceHubForm form, MessageResources mr) {
        form.addType(new LabelValueBean(mr.getMessage("resource.group.inventory.New.props.DynamicGroup"), String
            .valueOf(AppdefEntityConstants.APPDEF_TYPE_GROUP_DYNAMIC)));
    }

    private boolean isAdhocGroupSelected(int type) {
        return type == SELECTOR_GROUP_ADHOC;
    }

    private boolean isDynamicGroupSelected(int type) {
        return type == SELECTOR_GROUP_DYNAMIC;
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

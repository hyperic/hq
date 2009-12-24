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

package org.hyperic.hq.ui.action.resource;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.application.monitor.VisibilityPortalAction;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action class to figure out which page to go to based on a navigation map
 * click of a resource and the previous page.
 * 
 */
public class NavMapAction
    extends BaseAction {
    private VisibilityPortalAction visibilityPortalAction;
    private AppdefBoss appdefBoss;

    @Autowired
    public NavMapAction(VisibilityPortalAction visibilityPortalAction, AppdefBoss appdefBoss) {
        super();
        this.visibilityPortalAction = visibilityPortalAction;
        this.appdefBoss = appdefBoss;
    }

    // ---------------------------------------------------- Public Methods

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        // this is the base of the forward path
        String defaultPageUrl = request.getParameter("defaultPage");

        String mode = request.getParameter("currentMode");

        AppdefEntityID[] eids = RequestUtils.getEntityIds(request);

        Integer sessionId = RequestUtils.getSessionId(request);

        String resType = AppdefEntityConstants.typeToString(eids[0].getType());

        AppdefResourceValue resVal = appdefBoss.findById(sessionId.intValue(), eids[0]);

        boolean useDefaultPage = useDefaultPage(request, resVal);
        String currentResType = request.getParameter("currentResType");

        String ctype = RequestUtils.getStringParameter(request, Constants.CHILD_RESOURCE_TYPE_ID_PARAM, null);

        /**
         * we are only inserting the autogrouptype for applications. These
         * changes are not done due to the fact that we are late in 1.0.3
         * release. We should use autogrouptypes for all resources for 1.1
         * release.
         */
        String autogrouptype = RequestUtils.getStringParameter(request, Constants.AUTOGROUP_TYPE_ID_PARAM, null);
        if (autogrouptype != null) {
            ctype = autogrouptype;
        }

        HashMap<String, Object> params = new HashMap<String, Object>();
        if (null == eids) {
            // platform auto-group
            params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
        } else {
            if (null == ctype && autogrouptype == null) {
                // individual resource or group
                params.put(Constants.RESOURCE_PARAM, new Integer(eids[0].getID()));
                params.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(eids[0].getType()));
            } else {
                // server / service auto-group
                params.put(Constants.ENTITY_ID_PARAM, BizappUtils.stringifyEntityIds(eids));
                params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
                // add the autogroup type if available
                if (autogrouptype != null) {
                    params.put(Constants.AUTOGROUP_TYPE_ID_PARAM, autogrouptype);
                }

                // need to set the auto-group type - this type is not part
                // of the resource type
                resType = AppdefEntityConstants.typeToString(AppdefEntityConstants.APPDEF_TYPE_AUTOGROUP);

                // send auto-group resource to defaulg page if the mode
                // is not resourceMetrics
                if (!(Constants.MONITOR_VISIBILITY_LOC.equalsIgnoreCase(currentResType) && Constants.MODE_MON_RES_METS
                    .equalsIgnoreCase(mode))) {
                    // send to default page
                    useDefaultPage = true;
                }
            }
        }

        ActionForward forward = null;
        if (!useDefaultPage) {
            params.put(Constants.MODE_PARAM, mode);

            if (Constants.ALERT_LOC.equalsIgnoreCase(currentResType) ||
                Constants.ALERT_CONFIG_LOC.equalsIgnoreCase(currentResType)) {
                forward = new ActionForward("lastpage", currentResType, /* redirect */true);
            } else
                forward = new ActionForward("lastpage", "/resource/" + resType + "/" + currentResType, /* redirect */
                    true);
        } else
            // send to the default page
            forward = new ActionForward("lastpage", defaultPageUrl, /* redirect */true);

        forward = ActionUtils.changeForwardPath(forward, params);

        return forward;
    }

    protected boolean useDefaultPage(HttpServletRequest request, AppdefResourceValue resVal) throws Exception {
        String mode = request.getParameter("currentMode");

        // not current Resource Type specified
        String currentResType = request.getParameter("currentResType");

        // check if the Resource supports the mode
        return !isValidResourceMode(resVal, mode, currentResType);
    }

    /**
     * contains a map of valid modes for a given resource
     */
    protected boolean isValidResourceMode(AppdefResourceValue resVal, String mode, String currentResType) {
        // application controller
        if (resVal.getEntityId().getType() == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {

            if (Constants.MONITOR_VISIBILITY_LOC.equalsIgnoreCase(currentResType)) {

                Map tabs = visibilityPortalAction.getKeyMethodMap();

                if (!tabs.containsKey(mode)) {
                    return false;
                }
            }

            if (Constants.ALERT_LOC.equalsIgnoreCase(currentResType) ||
                Constants.ALERT_CONFIG_LOC.equalsIgnoreCase(currentResType) ||
                Constants.MONITOR_CONFIG_LOC.equalsIgnoreCase(currentResType)) {
                // send to default page
                return false;
            }
        }

        // by default a resource supports all modes
        return currentResType != null && !"".equalsIgnoreCase(currentResType);
    }
}

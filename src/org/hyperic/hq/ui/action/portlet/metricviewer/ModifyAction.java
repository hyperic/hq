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

package org.hyperic.hq.ui.action.portlet.metricviewer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;

public class ModifyAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        PropertiesForm pForm = (PropertiesForm) form;
        HttpSession session = request.getSession();
        WebUser user = (WebUser)
            request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);

        String forwardStr = Constants.SUCCESS_URL;
        DashboardConfig dashConfig = DashboardUtils.findDashboard(
        		(Integer)session.getAttribute(Constants.SELECTED_DASHBOARD_ID),
        		user, boss);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        if(pForm.isRemoveClicked()){
            DashboardUtils
                .removeResources(pForm.getIds(),
                                 PropertiesForm.RESOURCES,
                                 dashPrefs);
            ConfigurationProxy.getInstance().setDashboardPreferences(session, user,
            		boss, dashPrefs);
            forwardStr = "review";
        }

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        String token = pForm.getToken();

        // For multi-portlet configuration
        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String resTypeKey = PropertiesForm.RES_TYPE;
        String metricKey = PropertiesForm.METRIC;
        String descendingKey = PropertiesForm.DECSENDING;
        String titleKey = PropertiesForm.TITLE;
        
        if (token != null) {
            numKey += token;
            resKey += token;
            resTypeKey += token;
            metricKey += token;
            descendingKey += token;
            titleKey += token;
        }

        Integer numberToShow = pForm.getNumberToShow();
        String resourceType = pForm.getResourceType();
        String metric = pForm.getMetric();
        String descending = pForm.getDescending();

        // If the selected resource type does not match the previous value,
        // clear out the resources
        try {
            if (!resourceType.equals(dashPrefs.getValue(resTypeKey))) {
                dashPrefs.setValue(resKey, "");
            }
        } catch (InvalidOptionException e) {
            // Ok, not set yet..
        }

        dashPrefs.setValue(resTypeKey, resourceType);
        dashPrefs.setValue(numKey, numberToShow.toString());
        dashPrefs.setValue(metricKey, metric);
        dashPrefs.setValue(descendingKey, descending);
        dashPrefs.setValue(titleKey, pForm.getTitle());

        ConfigurationProxy.getInstance().setDashboardPreferences(session, user,
        		boss, dashPrefs);
        
        session.removeAttribute(Constants.USERS_SES_PORTAL);

        if (!pForm.isOkClicked()) {
            forwardStr="review";
        }

        return mapping.findForward(forwardStr);
    }
}

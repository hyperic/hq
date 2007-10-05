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

package org.hyperic.hq.ui.action.portlet.savedqueries;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class ModifyAction extends BaseAction {
    
    // --------------------------------------------------------- Public Methods
    
    /**
     *
     * @param mapping The ActionMapping used to select this instance
     * @param actionForm The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        Log log = LogFactory.getLog(ModifyAction.class.getName());
        ServletContext ctx = getServlet().getServletContext();
        HttpSession session = request.getSession();
        WebUser user = (WebUser) session.getAttribute( Constants.WEBUSER_SES_ATTR );
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        DashboardConfig dashConfig = (DashboardConfig) session.getAttribute(Constants.SELECTED_DASHBOARD);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        PropertiesForm pForm = (PropertiesForm) form;
        ActionForward forward = checkSubmit(request, mapping, form);
        String returnString = "success";
        if (forward != null) {
            return forward;
        }

        String[] charts = pForm.getCharts();
        if (charts != null && pForm.isDeleteClicked()) {                
            String userCharts =
            	dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS);

            for(int i = 0; i < charts.length; i++){
                userCharts = StringUtil.remove(userCharts, charts[i]);                
            }
            dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, userCharts);
            returnString = "remove";
        } else {
            // Sort by order
            List chartList = new ArrayList();
            chartList.add(dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS));
            chartList.add(dashPrefs.getValue(StringConstants.DASHBOARD_DELIMITER));
            
            for (Iterator it = chartList.iterator(); it.hasNext(); ) {
                if ("null".equals(it.next()))
                    it.remove();
            }
            
            String[] orderedCharts = new String[chartList.size()];

            StringTokenizer orderTK = new StringTokenizer(pForm.getOrder(),
                                                          "=&");
            for (int i = 0; orderTK.hasMoreTokens(); i++) {
                orderTK.nextToken();                                // left-hand
                int index = Integer.parseInt(orderTK.nextToken());  // index
                orderedCharts[i] = (String) chartList.get(index - 1);
            }
            
            dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS,
                StringUtil.arrayToString(orderedCharts, StringConstants
                                         .DASHBOARD_DELIMITER.charAt(0)));
        }

        ConfigurationProxy.getInstance().setDashboardPreferences(session, user, boss, dashPrefs );
        
        LogFactory.getLog("user.preferences").trace("Invoking setUserPrefs"+
            " in savedqueries/ModifyAction " +
            " for " + user.getId() + " at "+System.currentTimeMillis() +
            " user.prefs = " + dashPrefs.getKeys().toString());
        return mapping.findForward(returnString);

    }
}

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

package org.hyperic.hq.ui.action.portlet.controlactions;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
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
        
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        Integer sessionId = RequestUtils.getSessionId(request);
        PropertiesForm pForm = (PropertiesForm) form;
        HttpSession session = request.getSession();
        WebUser user = (WebUser) session.getAttribute( Constants.WEBUSER_SES_ATTR );

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        String lastCompleted = pForm.getLastCompleted().toString();            
        String mostFrequent  = pForm.getMostFrequent().toString();            
        String nextScheduled = pForm.getNextScheduled() == null ?
                "1" : pForm.getNextScheduled().toString();            

        String useLastCompleted = String.valueOf( pForm.isUseLastCompleted() );
        String useMostFrequent  = String.valueOf( pForm.isUseMostFrequent() );
        String useNextScheduled = String.valueOf( pForm.isUseNextScheduled() );
        String past             = String.valueOf(pForm.getPast());

        DashboardConfig dashConfig = (DashboardConfig) session.getAttribute(Constants.SELECTED_DASHBOARD);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        
        dashPrefs.setValue(".dashContent.controlActions.lastCompleted", lastCompleted );
        dashPrefs.setValue(".dashContent.controlActions.mostFrequent", mostFrequent );
        dashPrefs.setValue(".dashContent.controlActions.nextScheduled", nextScheduled );

        dashPrefs.setValue(".dashContent.controlActions.useLastCompleted", useLastCompleted );
        dashPrefs.setValue(".dashContent.controlActions.useMostFrequent", useMostFrequent );
        dashPrefs.setValue(".dashContent.controlActions.useNextScheduled", useNextScheduled );
        dashPrefs.setValue(".dashContent.controlActions.past", past);
        
        ConfigurationProxy.getInstance().setDashboardPreferences(session, user, boss, dashPrefs);
        
        LogFactory.getLog("user.preferences").trace("Invoking setUserPrefs"+
            " in controlactions/ModifyAction " +
            " for " + user.getId() + " at "+System.currentTimeMillis() +
            " user.prefs = " + dashPrefs.getKeys().toString());
        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(Constants.SUCCESS_URL);
    }
}

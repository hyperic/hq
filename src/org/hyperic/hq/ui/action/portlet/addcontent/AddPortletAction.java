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

package org.hyperic.hq.ui.action.portlet.addcontent;

import java.text.NumberFormat;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */

public class AddPortletAction extends BaseAction {

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
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
        HttpSession session = request.getSession();
        WebUser user =
            (WebUser) session.getAttribute( Constants.WEBUSER_SES_ATTR );
        DashboardConfig dashConfig = (DashboardConfig) session.getAttribute(Constants.SELECTED_DASHBOARD);
        ConfigResponse dashPrefs = dashConfig.getConfig();
        PropertiesForm pForm = (PropertiesForm) form;

        if( pForm.getPortlet() == null || "bad".equals( pForm.getPortlet() ) ) {
            return mapping.findForward(Constants.SUCCESS_URL);
        }
        
        String prefKey;
        if (pForm.isWide()) {
            prefKey = Constants.USER_PORTLETS_SECOND;
        }                            
        else{
            prefKey = Constants.USER_PORTLETS_FIRST;
        }

        String userPrefs = dashPrefs.getValue(prefKey);
        
        String portlet = pForm.getPortlet();
        while (userPrefs.indexOf(portlet) > -1) {
            // We need to add a multi portlet
            StringBuffer portletName = new StringBuffer(pForm.getPortlet());
            // 1. Generate random token
            NumberFormat nf = NumberFormat.getIntegerInstance();
            nf.setMinimumIntegerDigits(3);      // Exactly 3 digits
            nf.setMaximumIntegerDigits(3);
            portletName.append(DashboardUtils.MULTI_PORTLET_TOKEN)
                       .append(nf.format(new Random().nextInt(1000)));
            // 2. Create unique portlet name based on the new random token
            portlet = portletName.toString();
        }
        
        String preferences = Constants.DASHBOARD_DELIMITER + portlet +
                             Constants.DASHBOARD_DELIMITER;
        // Clean up the delimiters
		preferences = StringUtil.replace(preferences,
				Constants.EMPTY_DELIMITER, Constants.DASHBOARD_DELIMITER);

		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in AddPortletAction " + " for "
						+ user.getId() + " at " + System.currentTimeMillis()
						+ " user.prefs = " + userPrefs);
		ConfigurationProxy.getInstance().setPreference(session, user, boss,
				prefKey, userPrefs + preferences);
        

        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(Constants.SUCCESS_URL);
    }
}

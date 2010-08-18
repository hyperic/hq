/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.action.portlet;

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
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class SetDefaultDashboardAction
    extends BaseAction {
    private AuthzBoss authzBoss;

    @Autowired
    public SetDefaultDashboardAction(AuthzBoss authzBoss) {
        super();
        this.authzBoss = authzBoss;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        DashboardForm dForm = (DashboardForm) form;
        WebUser user = RequestUtils.getWebUser(session);
        String currentDefaultDashboardId = user.getPreference(Constants.DEFAULT_DASHBOARD_ID, null);
        String submittedDefaultDashboardId = dForm.getDefaultDashboard();

        // Compare the incoming default dashboard id with the one we had in our
        // user preferences
        // If they aren't equal it means the user is changing it, so update
        if (!submittedDefaultDashboardId.equals(currentDefaultDashboardId)) {
            user.setPreference(Constants.DEFAULT_DASHBOARD_ID, dForm.getDefaultDashboard());
            session.setAttribute(Constants.SELECTED_DASHBOARD_ID, new Integer(dForm.getDefaultDashboard()));
            authzBoss.setUserPrefs(user.getSessionId(), user.getSubject().getId(), user.getPreferences());
        }

        return mapping.findForward(Constants.AJAX_URL);
    }
}

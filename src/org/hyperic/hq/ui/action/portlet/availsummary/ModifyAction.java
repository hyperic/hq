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

package org.hyperic.hq.ui.action.portlet.availsummary;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
            session.getAttribute(Constants.WEBUSER_SES_ATTR);

        String forwardStr = Constants.SUCCESS_URL;

        String token = pForm.getToken();

        String numKey = PropertiesForm.NUM_TO_SHOW;
        String resKey = PropertiesForm.RESOURCES;
        String titleKey = PropertiesForm.TITLE;
        
        if (token != null) {
            resKey += token;
            numKey += token;
            titleKey += token;
        }

        if(pForm.isRemoveClicked()){
            DashboardUtils.removeResources(pForm.getIds(), resKey, user);
            forwardStr = "review";
        }

        ActionForward forward = checkSubmit(request, mapping, form);

        if (forward != null) {
            return forward;
        }

        Integer numberToShow = pForm.getNumberToShow();
        user.setPreference(numKey, numberToShow.toString());
        user.setPreference(titleKey, pForm.getTitle());

        boss.setUserPrefs(user.getSessionId(), user.getId(),
                          user.getPreferences());

        session.removeAttribute(Constants.USERS_SES_PORTAL);

        return mapping.findForward(forwardStr);
    }
}

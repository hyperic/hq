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

package org.hyperic.hq.ui.action.portlet;

import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class MoveUpAction extends BaseAction {

    public static final String delim = Constants.DASHBOARD_DELIMITER;

    private static final Log log
        = LogFactory.getLog(MoveUpAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss az = ContextUtils.getAuthzBoss(ctx);
        String portlet = request.getParameter("portletName");

        if (!tryMoveUp(user, portlet, session,
                       Constants.USER_PORTLETS_FIRST, az) &&
            !tryMoveUp(user, portlet, session,
                       Constants.USER_PORTLETS_SECOND, az)) {
            // Just log the error and don't do anything.
            log.error("Didn't find portlet " + portlet + " in any column");
        }
        
        return mapping.findForward(Constants.AJAX_URL);
    }

    private boolean tryMoveUp (WebUser user, String portlet, 
                               HttpSession session, String columnKey, 
                               AuthzBoss boss) throws Exception {

        String portlets = user.getPreferences().getValue(columnKey);

        // portlet is not in this column
        if (portlets.indexOf(portlet) == -1) return false;

        // tokenize and reshuffle
        StringBuffer newColumn = new StringBuffer();
        StringTokenizer st = new StringTokenizer(portlets, "|");
        String token, lastToken = null;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token.equals(portlet)) {
                if (lastToken == null) {
                    log.error("Cannot move portlet " + portlet 
                              + " any further up");
                    return true;
                }
                newColumn.append(delim).append(token)
                    .append(delim).append(lastToken);
                lastToken = null;
            } else {
                if (lastToken != null) {
                    newColumn.append(delim).append(lastToken);
                }
                lastToken = token;
            }
        }
        if (lastToken != null) {
            newColumn.append(delim).append(lastToken);
        }

        user.setPreference(columnKey, newColumn.toString());
        boss.setUserPrefs(user.getSessionId(), user.getId(), 
                          user.getPreferences());
        session.removeAttribute(Constants.USERS_SES_PORTAL);
        return true;
    }
}

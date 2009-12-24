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

package org.hyperic.hq.ui.action.admin.user;

import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseDispatchAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>BaseDispatchAction</code> that sets up user admin portals.
 */
public class UserAdminPortalAction
    extends BaseDispatchAction {

    private static final String TITLE_LIST = "admin.user.ListUsersTitle";

    private static final String PORTLET_LIST = ".admin.user.List";

    private static final String TITLE_ADD_ROLES = "admin.user.AddUserRolesTitle";

    private static final String PORTLET_ADD_ROLES = ".admin.user.UserRoles";

    private static final String TITLE_EDIT = "admin.user.EditUserTitle";

    private static final String PORTLET_EDIT = ".admin.user.Edit";

    private static final String TITLE_NEW = "admin.user.NewUserTitle";

    private static final String PORTLET_NEW = ".admin.user.New";

    private static final String TITLE_VIEW = "admin.user.ViewUserTitle";

    private static final String PORTLET_VIEW = ".admin.user.View";

    private static final String TITLE_CHANGE_PASSWORD = "admin.user.ChangeUserPasswordTitle";
    private static final String PORTLET_CHANGE_PASSWORD = ".admin.user.EditPassword";

    private static final String TITLE_REGISTER = "admin.user.RegisterUserTitle";

    private static final String PORTLET_REGISTER = ".admin.user.RegisterUser";

    protected final Log log = LogFactory.getLog(UserAdminPortalAction.class.getName());

    private final Properties keyMethodMap = new Properties();

    private AuthBoss authBoss;

    private AuthzBoss authzBoss;

    @Autowired
    public UserAdminPortalAction(AuthBoss authBoss, AuthzBoss authzBoss) {
        super();
        this.authBoss = authBoss;
        this.authzBoss = authzBoss;
        initializeKeyMethodMap();
    }

    private void initializeKeyMethodMap() {
        keyMethodMap.setProperty(Constants.MODE_LIST, "listUsers");
        keyMethodMap.setProperty(Constants.MODE_ADD_ROLES, "addUserRoles");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editUser");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newUser");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewUser");
        keyMethodMap.setProperty(Constants.MODE_EDIT_PASS, "changeUserPassword");
        keyMethodMap.setProperty(Constants.MODE_REGISTER, "registerUser");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward listUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
        setReturnPath(request, mapping, Constants.MODE_LIST);

        Portal portal = Portal.createPortal(TITLE_LIST, PORTLET_LIST);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addUserRoles(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_ADD_ROLES, PORTLET_ADD_ROLES);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_EDIT, PORTLET_EDIT);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward newUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {
        setUser(request);
        setReturnPath(request, mapping, Constants.MODE_VIEW);

        Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward changeUserPassword(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_CHANGE_PASSWORD, PORTLET_CHANGE_PASSWORD);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward registerUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal(TITLE_REGISTER, PORTLET_REGISTER);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    /**
     * Set the user for the current action.
     * 
     * @param request The request to get the session to store the returnPath
     *        into.
     * 
     */
    protected void setUser(HttpServletRequest request) throws Exception {
        Integer userId = RequestUtils.getUserId(request);
        Integer sessionId = RequestUtils.getSessionId(request);

        if (log.isTraceEnabled()) {
            log.trace("finding user [" + userId + "]");
        }
        AuthzSubject user = authzBoss.findSubjectById(sessionId, userId);

        // when CAM is in LDAP mode, we may still have
        // users logging in with JDBC. the only way we can
        // distinguish these users is by checking to see
        // if they have an entry in the principals table.
        WebUser webUser = new WebUser(user.getAuthzSubjectValue());
        boolean hasPrincipal = authBoss.isUser(sessionId.intValue(), user.getName());
        webUser.setHasPrincipal(hasPrincipal);

        request.setAttribute(Constants.USER_ATTR, webUser);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, BizappUtils.makeSubjectFullName(user));
    }

    /**
     * Set the return path for the current action, including the mode and (if
     * necessary) user id request parameters.
     * 
     * @param request The request to get the session to store the return path
     *        into.
     * @param mapping The ActionMapping to get the return path from.
     * @param mode The name of the current display mode.
     * 
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, String mode) throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.MODE_PARAM, mode);
        try {
            params.put(Constants.USER_PARAM, RequestUtils.getUserId(request));
        } catch (ParameterNotFoundException e) {
            ; // not in a specific user's context
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }
        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}

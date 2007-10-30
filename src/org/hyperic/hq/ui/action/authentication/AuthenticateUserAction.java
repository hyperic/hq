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

package org.hyperic.hq.ui.action.authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.util.config.ConfigResponse;

/**
 * An <code>Action</code> subclass that authenticates the web user's
 * credentials and establishes his identity.
 */
public class AuthenticateUserAction extends TilesAction {

    private static final String URL_REGISTER =
        "/admin/user/UserAdmin.do?mode=register";
    private static final String URL_DASHBOARD = "/";

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve a <code>RoleValue</code> identified by the value of
     * the request parameter <code>Constants.ROLE_PARAM</code> from
     * the BizApp and save it into the request attribute
     * <code>Constants.ROLE_ATTR</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        Log log = LogFactory.getLog(AuthenticateUserAction.class.getName());
        HttpSession session = request.getSession(true);              
        LogonForm logonForm = (LogonForm) form;
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);

        WebUser webUser;
        HashMap userOpsMap = new HashMap();
        boolean needsRegistration = false;
        int sid = 0;
        try {
            // authenticate the credentials
            AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
            sid = authBoss.login(logonForm.getJ_username(),
                                 logonForm.getJ_password());
            Integer sessionId = new Integer(sid);
            if (log.isTraceEnabled()) {
                log.trace("Logged in as [" + logonForm.getJ_username() +
                          "] with session id [" + sessionId + "]");
            }

            // look up the subject record
            AuthzSubjectValue subject =
                authzBoss.findSubjectByName(sessionId,
                                            logonForm.getJ_username());
            if (subject == null) {

                subject = new AuthzSubjectValue();
                subject.setName(logonForm.getJ_username());

                needsRegistration = true;
            } else if (subject.getEmailAddress() == null ||
                       subject.getEmailAddress().length() == 0) {
                needsRegistration = true;
            }

            // figure out if the user has a principal
            boolean
                hasPrincipal = authBoss.isUser(sessionId.intValue(),
                                               logonForm.getJ_username());

            ConfigResponse preferences = new ConfigResponse();
            if (!needsRegistration) {
                // look up the user's preferences

                ConfigResponse defaultPreferences =
                    (ConfigResponse)ctx.getAttribute(Constants.DEF_USER_PREFS);

                preferences = authzBoss.getUserPrefs(sessionId,
                                                     subject.getId());
            
                preferences.merge(defaultPreferences, false );

                // look up the user's permissions
                List userOps = authzBoss.getAllOperations(sessionId);
                for (Iterator it = userOps.iterator(); it.hasNext();) {
                    OperationValue op = (OperationValue) it.next();
                    userOpsMap.put( op.getName(), Boolean.TRUE );
                }
            }

            webUser = new WebUser(subject, sessionId, 
                                  logonForm.getJ_password(), preferences,
                                  hasPrincipal);
        }
        catch ( Exception e ) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("username") >= 0 ||
                msg.indexOf("password") >= 0)
                request.setAttribute(Constants.LOGON_STATUS, "login.info.bad");
            else if (msg.indexOf("disabled") >= 0)
                request.setAttribute(Constants.LOGON_STATUS, "login.disabled");
            else
                request.setAttribute(Constants.LOGON_STATUS, "login.bad.backend");
            
            return ( mapping.findForward("bad") );
        }

        // compute the post-login destination
        ActionForward af;
        boolean setRedirect = true;
        if (needsRegistration) {
            log.debug("User registration required");
            af = new ActionForward(URL_REGISTER);

        } else {
            // if the user's session timed out, we "bookmarked" the
            // url that he was going to so that we can send him there
            // after login. otherwise, he gets the dashboard.
            String url = getBookmarkedUrl(session);
            if (url == null || url.equals("/Logout.do")) url = URL_DASHBOARD;
            af = new ActionForward(url);
        }

        if (setRedirect) {
            af.setRedirect(true);
        }

        // now that we've constructed a forward to the bookmarked url,
        // if any, forget the old session and start a new one,
        // setting the web user to show that we're logged in

        session = request.getSession(true);              
        session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
        session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOpsMap);

		// Load the user dashboard if it doesn't exist create a new one
        // and mix in the defaults
		try {
			DashboardManagerLocal dashManager = DashboardManagerEJBImpl
					.getOne();
			ConfigResponse defaultUserDashPrefs = (ConfigResponse) ctx
					.getAttribute(Constants.DEF_USER_DASH_PREFS);
			AuthzSubject me = AuthzSubjectManagerEJBImpl.getOne()
					.findSubjectById(webUser.getSubject().getId());
			UserDashboardConfig userDashboard = dashManager.getUserDashboard(
					me, me);
			if (userDashboard == null) {
				userDashboard = dashManager.createUserDashboard(me, me, webUser
						.getName());
				ConfigResponse userDashobardConfig = userDashboard.getConfig();
				userDashobardConfig.merge(defaultUserDashPrefs, false);
				dashManager.configureDashboard(me, userDashboard,
						userDashobardConfig);
			}
		} catch (PermissionException e) {
			e.printStackTrace();
		} 

        if (needsRegistration) {
            // will be cleaned out during registration
            session.setAttribute(Constants.PASSWORD_SES_ATTR,
                                 logonForm.getJ_password());
        }
        
        // See if graphics engine is present
        try {
            new ResourceTree(1);
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.TRUE);
        } catch (Throwable t) {
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.FALSE);
        }
        
        // Look up any UI attach points
        ProductBoss pBoss = ContextUtils.getProductBoss(ctx);
        Collection mastheadAttachments = pBoss.findMastAttachments(sid);
        session.setAttribute("mastheadAttachments", mastheadAttachments);
        ArrayList resourceAttachments = new ArrayList();
		ArrayList trackerAttachments = new ArrayList();
		for (Iterator itr = mastheadAttachments.iterator(); itr.hasNext();) {
			AttachmentMasthead attachment = (AttachmentMasthead) itr.next();
			if (attachment.getCategory().equals(ViewMastheadCategory.RESOURCE)) {
				resourceAttachments.add(attachment);
			} else if (attachment.getCategory().equals(
					ViewMastheadCategory.TRACKER)) {
				trackerAttachments.add(attachment);
			}
		}
        session.setAttribute("mastheadResourceAttachments", resourceAttachments);
        session.setAttribute("mastheadTrackerAttachments", trackerAttachments);
        return af;
    }

    /*
	 * Return the "bookmarked" url saved when we discovered the user's session
	 * had timed out, or null if there is no bookmarked url.
	 */
    private String getBookmarkedUrl(HttpSession session) {
        String val = (String) session.getAttribute(Constants.LOGON_URL_KEY);
        if (val == null || val.length() == 0) {
            return null;
        }
        StringBuffer url = new StringBuffer(val);

        Map parameters =
            (Map) session.getAttribute(Constants.LOGON_URL_PARAMETERS);
        if (parameters != null && ! parameters.isEmpty()) {
            String sep = "?";
            for (Iterator i = parameters.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                String value = (String) parameters.get(key);
                url.append(sep).append(key).append("=").append(value);

                if (sep.equals("?")) {
                    sep = "&";
                }
            }
        }

        return url.toString();
    }
}

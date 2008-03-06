/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.hq.ui.pages;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.RedirectException;
import org.apache.tapestry.annotations.Component;
import org.apache.tapestry.annotations.InitialValue;
import org.apache.tapestry.annotations.Meta;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.callback.ICallback;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.link.PageLink;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.ui.tapestry.page.PageListing;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;

@Meta({"org.apache.tapestry.default-binding-prefix=ognl"})
public abstract class SignIn extends BasePage {

    static Log log = LogFactory.getLog(SignIn.class);

    @Persist()
    public abstract String getUserName();
    public abstract void setUserName(String userName);

    @Persist()
    public abstract String getPassword();
    public abstract void setPassword(String password);

    @Persist()
    @InitialValue("literal:")
    public abstract String getMessage();
    public abstract void setMessage(String msg);

    @Persist()
    public abstract ICallback getCallback();
    public abstract void setCallback(ICallback value);

    /*
     * TODO rip this out when struts is no more
     */
    @Component(type = "PageLink", bindings = { "page='SignIn'" })
    public abstract PageLink getSigninLink();

    /**
     * Listener for the SignIn button
     * @param cycle
     * @return the signin page if error and the dashboard if login successful
     */
    public ILink signinButtonListener(IRequestCycle cycle) {
	HttpServletRequest request = getRequest();
	ServletContext ctx = getServletContext();
	HttpSession session = request.getSession(true);
	WebUser webUser;
	Map userOpsMap = new HashMap();
	boolean needsRegistration = false;
	AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
	try {
	    webUser = loginUser(getRequest(), ctx, getUserName(), getPassword());

	    needsRegistration = webUser.getPreferences().getKeys().size() == 0;
	    if (!needsRegistration) {
		userOpsMap = loadUserPermissions(webUser.getSessionId(),
			authzBoss);
	    }
	} catch (Exception e) {
	    String msg = e.getMessage().toLowerCase();
	    if (msg.indexOf("username") >= 0 || msg.indexOf("password") >= 0)
		//setMessage("FUBAR");
		setMessage(this.getMessages().getMessage("credentialError"));
	    else if (msg.indexOf("disabled") >= 0)
		setMessage(this.getMessages().getMessage("userDisabled"));
	    else
		setMessage(this.getMessages().getMessage("serverError"));
	    log.debug(getSigninLink().getLink(cycle));
	    return getSigninLink().getLink(cycle);
	}

	if (needsRegistration) {
	    // will be cleaned out during registration
	    session.setAttribute(Constants.PASSWORD_SES_ATTR, getPassword());
	}

	session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
	session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOpsMap);

	loadDashboard(ctx, webUser);

	try {
	    new ResourceTree(1); // See if graphics engine is present
	    session.setAttribute(Constants.XLIB_INSTALLED, Boolean.TRUE);
	} catch (Throwable t) {
	    session.setAttribute(Constants.XLIB_INSTALLED, Boolean.FALSE);
	}
	//log.debug(getDashboardLink().getLink(cycle).getAbsoluteURL());
	//String link = getDashboardLink().getLink(cycle).getURL();
	throw new RedirectException(PageListing.DASHBOARD_URL);
    }

    private void loadDashboard(ServletContext ctx, WebUser webUser) {
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
    }

    public static WebUser loginUser(HttpServletRequest request,
	    ServletContext ctx, String username, String password)
	    throws RemoteException, SecurityException, LoginException,
	    ApplicationException, ConfigPropertyException, FinderException {
	AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
	AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
	boolean needsRegistration = false;
	// authenticate the credentials
	int sid = authBoss.login(username, password);
	Integer sessionId = new Integer(sid);
	if (log.isTraceEnabled()) {
	    log.trace("Logged in as [" + username + "] with session id ["
		    + sessionId + "]");
	}
	// look up the subject record
	AuthzSubject subjPojo = authzBoss.getCurrentSubject(sid);
	AuthzSubjectValue subject = null;

	if (subjPojo == null) {
	    subject = new AuthzSubjectValue();
	    subject.setName(username);

	    needsRegistration = true;
	} else {
	    subject = subjPojo.getAuthzSubjectValue();
	    needsRegistration = subjPojo.getEmailAddress() == null
		    || subjPojo.getEmailAddress().length() == 0;
	}

	// figure out if the user has a principal
	boolean hasPrincipal = authBoss.isUser(sessionId.intValue(), subject
		.getName());

	ConfigResponse preferences = new ConfigResponse();
	if (!needsRegistration) {
	    // look up the user's preferences
	    ConfigResponse defaultPreferences = (ConfigResponse) ctx
		    .getAttribute(Constants.DEF_USER_PREFS);

	    preferences = authzBoss.getUserPrefs(sessionId, subject.getId());
	    preferences.merge(defaultPreferences, false);
	}

	return new WebUser(subject, sessionId, preferences, hasPrincipal);
    }

    public static Map loadUserPermissions(Integer sessionId, AuthzBoss authzBoss)
	    throws FinderException, PermissionException,
	    SessionTimeoutException, SessionNotFoundException, RemoteException {
	// look up the user's permissions
	HashMap userOpsMap = new HashMap();
	List userOps = authzBoss.getAllOperations(sessionId);
	for (Iterator it = userOps.iterator(); it.hasNext();) {
	    OperationValue op = (OperationValue) it.next();
	    userOpsMap.put(op.getName(), Boolean.TRUE);
	}
	return userOpsMap;
    }
}

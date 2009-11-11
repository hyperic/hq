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
import java.util.Set;
import java.util.Map.Entry;

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
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Meta;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.callback.ICallback;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.link.PageLink;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardManagerImpl;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.service.SearchService;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.ui.tapestry.page.PageListing;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;

@Meta( { "org.apache.tapestry.default-binding-prefix=ognl" })
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

    /*
     * TODO rip this out when struts is no more
     */
    @Component(type = "PageLink", bindings = { "page='SignIn'" })
    public abstract PageLink getSigninLink();

    /**
     * Listener for the SignIn button
     * 
     * @param cycle
     * @return the signin page if error and the dashboard if login successful
     */
    public ILink signinButtonListener(IRequestCycle cycle) {
        ServletContext ctx = getServletContext();
        HttpSession session = getRequest().getSession(true);
        WebUser webUser;
        Map userOpsMap = new HashMap();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        try {
            webUser = loginUser(ctx, getUserName(), getPassword());

            if (webUser.getPreferences().getKeys().size() == 0) {
                // will be cleaned out during registration
                session.setAttribute(Constants.PASSWORD_SES_ATTR, getPassword());
                session.setAttribute(Constants.NEEDS_REGISTRATION, Boolean.TRUE);
            }
            else {
                userOpsMap = loadUserPermissions(webUser.getSessionId(),
                        authzBoss);
            }
        } catch (Exception e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("username") >= 0 || msg.indexOf("password") >= 0)
                // setMessage("FUBAR");
                setMessage(this.getMessages().getMessage("credentialError"));
            else if (msg.indexOf("disabled") >= 0)
                setMessage(this.getMessages().getMessage("userDisabled"));
            else
                setMessage(this.getMessages().getMessage("serverError"));
            return getSigninLink().getLink(cycle);
        }

        getBaseSessionBean().setWebUser(webUser);
        session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOpsMap);

        loadDashboard(ctx, webUser, authzBoss);
        setXlibFlag(session);
        
        // log.debug(getDashboardLink().getLink(cycle).getAbsoluteURL());
        // String link = getDashboardLink().getLink(cycle).getURL();
        throw new RedirectException(PageListing.DASHBOARD_URL);
    }

    private static void setXlibFlag(HttpSession session) {
        try {
            new ResourceTree(1); // See if graphics engine is present
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.TRUE);
        } catch (Throwable t) {
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.FALSE);
        }
    }
    
    //clone ConfigResponse.merge - cannot change its method signature
    private boolean mergeValues(ConfigResponse config,
                                ConfigResponse other,
                                boolean overWrite) {
        boolean updated = true;
        Set entrySet = other.toProperties().entrySet();
        for (Iterator i = entrySet.iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();

            if (overWrite || config.getValue(key) == null) {
                config.setValue(key, value);
                updated = true;
            }
        }
        return updated;
    }

    private void loadDashboard(ServletContext ctx, WebUser webUser,
                               AuthzBoss authzBoss) {
        try {
            DashboardManager dashManager =
                DashboardManagerImpl.getOne();
            ConfigResponse defaultUserDashPrefs =
                (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_DASH_PREFS);
            AuthzSubject me =
                authzBoss.findSubjectById(webUser.getSessionId(),
                                          webUser.getSubject().getId());
            UserDashboardConfig userDashboard =
                dashManager.getUserDashboard(me, me);
            if (userDashboard == null) {
                userDashboard =
                    dashManager.createUserDashboard(me, me, webUser.getName());
            }
            
            ConfigResponse userDashobardConfig = userDashboard.getConfig();
            if (mergeValues(userDashobardConfig, defaultUserDashPrefs, false)) {
                dashManager.configureDashboard(me, userDashboard,
                                               userDashobardConfig);
            }
        } catch (PermissionException e) {
            e.printStackTrace();
        } catch (SessionNotFoundException e) {
            // User not logged in
        } catch (SessionTimeoutException e) {
            // User session has expired
        } 
    }

    private static ConfigResponse getUserPrefs(ServletContext ctx,
                                               Integer sessionId,
                                               Integer subjectId,
                                               AuthzBoss authzBoss)
        throws RemoteException {
        // look up the user's preferences
        ConfigResponse defaultPreferences =
            (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_PREFS);

        ConfigResponse preferences =
            authzBoss.getUserPrefs(sessionId, subjectId);
        preferences.merge(defaultPreferences, false);
        return preferences;
    }
    
    private static Map loadUserPermissions(Integer sessionId,
                                           AuthzBoss authzBoss)
        throws FinderException, PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException {
        // look up the user's permissions
        HashMap userOpsMap = new HashMap();
        List userOps = authzBoss.getAllOperations(sessionId);
        for (Iterator it = userOps.iterator(); it.hasNext();) {
            Operation op = (Operation) it.next();
            userOpsMap.put(op.getName(), Boolean.TRUE);
        }
        return userOpsMap;
    }
    
    public static WebUser loginUser(ServletContext ctx, String username,
                                    String password)
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
        boolean hasPrincipal =
            authBoss.isUser(sessionId.intValue(), subject.getName());
    
        ConfigResponse preferences =
            needsRegistration ? new ConfigResponse() :
                                getUserPrefs(ctx, sessionId, subject.getId(),
                                             authzBoss);
    
        return new WebUser(subject, sessionId, preferences, hasPrincipal);
    }
    
    public static WebUser loginGuest(ServletContext ctx,
                                     HttpServletRequest request) {
        AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
        try {
            int sid = authBoss.loginGuest();
    
            AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
            AuthzSubject subject = authzBoss.getCurrentSubject(sid);
    
            Integer sessionId = new Integer(sid);

            ConfigResponse preferences = getUserPrefs(ctx, sessionId,
                                                      subject.getId(),
                                                      authzBoss);
        
            WebUser webUser = new WebUser(subject, sessionId, preferences, true);
    
            Map userOpsMap = loadUserPermissions(sessionId, authzBoss);
            HttpSession session = request.getSession();
            session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOpsMap);
    
            try {
                DashboardManager dashManager =
                    DashboardManagerImpl.getOne();
                ConfigResponse defaultUserDashPrefs = (ConfigResponse) ctx
                        .getAttribute(Constants.DEF_USER_DASH_PREFS);
                AuthzSubject me =
                    authzBoss.findSubjectById(sessionId,
                                              webUser.getSubject().getId());
                UserDashboardConfig userDashboard =
                    dashManager.getUserDashboard(me, me);
                if (userDashboard == null) {
                    userDashboard =
                        dashManager.createUserDashboard(me, me,
                                                        webUser.getName());
                    ConfigResponse userDashobardConfig =
                        userDashboard.getConfig();
                    userDashobardConfig.merge(defaultUserDashPrefs, false);
                    dashManager.configureDashboard(me, userDashboard,
                                                   userDashobardConfig);
                }
            } catch (PermissionException e) {
                e.printStackTrace();
            }

            session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
            setXlibFlag(session);
    
            return webUser;
        } catch (Exception e) {
            // No guest account available
            return null;
        }
    }

}

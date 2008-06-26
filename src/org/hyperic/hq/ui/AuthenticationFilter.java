/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.ui;

import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.pages.SignIn;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.ui.tapestry.page.PageListing;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.encoding.Base64;

public final class AuthenticationFilter extends BaseFilter {

    private static Log log = LogFactory.getLog(AuthenticationFilter.class
            .getName());

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession();
        ServletContext ctx = session.getServletContext();
        WebUser webUser = SessionUtils.getWebUser(session);
        String servletPath = request.getServletPath(), contextPath = request
                .getContextPath(), queryString = request.getQueryString();

        if (webUser == null) {
            // See if there is authentication information
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                StringTokenizer token = new StringTokenizer(auth, " ");
                if (token.countTokens() == 2) {
                    String tok = token.nextToken();
                    assert (tok.equals("Basic"));
                    tok = token.nextToken();
                    String userpass = new String(Base64.decode(tok));

                    token = new StringTokenizer(userpass, ":");
                    assert (token.countTokens() == 2);
                    String user = token.nextToken();
                    String pass = token.nextToken();
                    try {
                        webUser = SignIn.loginUser(ctx, user, pass);
                        session.setAttribute(Constants.WEBUSER_SES_ATTR,
                                webUser);
                    } catch (Exception e) {
                        // Unsuccessful login
                        log.error("Unsuccessful login from " + user);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            }
        }

        if (webUser == null) {
            // See if there is a guest user
            webUser = loginGuest(request, ctx);
        }

        if (webUser == null) {
            // if the user is requesting the login page continue
            if (PageListing.SIGN_IN_URL.equals(servletPath)
                    || "/j_security_check.do".equals(servletPath))
                chain.doFilter(request, response);
            else {
                // not requesting the login page so add a callback and send them
                // there
                if (servletPath.indexOf("RecentAlerts") < 0
                        && servletPath.indexOf("rss") < 0
                        && servletPath.indexOf("IndicatorCharts") < 0
                        && servletPath.indexOf("dashboard/") < 0) {
                    StringBuffer forwardURL = new StringBuffer();
                    forwardURL.append(servletPath).append("?").append(
                            queryString == null ? "" : queryString);
                    setCallback(forwardURL.toString(), session);
                }
                String redirectURL = contextPath + PageListing.SIGN_IN_URL;
                response.sendRedirect(redirectURL);
                return;
            }
        }
        String callbackURL = (String) session
                .getAttribute(Constants.POST_AUTH_CALLBACK_URL);
        if (webUser != null && !PageListing.SIGN_IN_URL.equals(servletPath)
                && callbackURL != null) {
            session.removeAttribute(Constants.POST_AUTH_CALLBACK_URL);
            response.sendRedirect(callbackURL);
        } else if (webUser != null
                && (PageListing.SIGN_IN_URL.equals(servletPath) || "/j_security_check.do"
                        .equals(servletPath))) {
            response.sendRedirect(contextPath + PageListing.DASHBOARD_URL);
            return;
        } else {
            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                Throwable trace = e;
                if (e.getRootCause() != null) {
                    trace = e.getRootCause();
                }
                log.error("Caught ServletException from client "
                        + request.getRemoteAddr() + ": "
                        + e.getMessage(), trace);
            } catch (Exception e) {
                log.warn("Caught Exception from client "
                        + request.getRemoteAddr() + ": " + e.getMessage());
            }
        }
    }

    public void init(FilterConfig filterConfig) {
        super.init(filterConfig);
    }

    private WebUser loginGuest(HttpServletRequest request, ServletContext ctx) {
        AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);
        try {
            int sid = authBoss.loginGuest();

            ConfigResponse preferences = new ConfigResponse();

            // look up the user's preferences
            ConfigResponse defaultPreferences = (ConfigResponse) ctx
                    .getAttribute(Constants.DEF_USER_PREFS);

            AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
            AuthzSubjectValue subject = authzBoss.getCurrentSubject(sid)
                    .getAuthzSubjectValue();

            Integer sessionId = new Integer(sid);
            preferences = authzBoss.getUserPrefs(sessionId, subject.getId());
            preferences.merge(defaultPreferences, false);

            WebUser webUser = new WebUser(subject, sessionId, preferences, true);

            Map userOpsMap = SignIn.loadUserPermissions(sessionId, authzBoss);
            request.getSession().setAttribute(Constants.USER_OPERATIONS_ATTR,
                    userOpsMap);

            try {
                DashboardManagerLocal dashManager = DashboardManagerEJBImpl
                        .getOne();
                ConfigResponse defaultUserDashPrefs = (ConfigResponse) ctx
                        .getAttribute(Constants.DEF_USER_DASH_PREFS);
                AuthzSubject me = AuthzSubjectManagerEJBImpl.getOne()
                        .findSubjectById(webUser.getSubject().getId());
                UserDashboardConfig userDashboard = dashManager
                        .getUserDashboard(me, me);
                if (userDashboard == null) {
                    userDashboard = dashManager.createUserDashboard(me, me,
                            webUser.getName());
                    ConfigResponse userDashobardConfig = userDashboard
                            .getConfig();
                    userDashobardConfig.merge(defaultUserDashPrefs, false);
                    dashManager.configureDashboard(me, userDashboard,
                            userDashobardConfig);
                }
            } catch (PermissionException e) {
                e.printStackTrace();
            }
            request.getSession().setAttribute(Constants.WEBUSER_SES_ATTR,
                    webUser);
            return webUser;
        } catch (Exception e) {
            // No guest account available
            return null;
        }
    }
    
    private void setCallback(String url, HttpSession session) {
        String currVal = (String) session.getAttribute(Constants.POST_AUTH_CALLBACK_URL);
        if (currVal == null) {
            session.setAttribute(Constants.POST_AUTH_CALLBACK_URL, url);
        }
    }

}

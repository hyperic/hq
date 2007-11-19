/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.FinderException;
import javax.security.auth.login.LoginException;
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
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.action.authentication.AuthenticateUserAction;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.encoding.Base64;

public final class AuthenticationFilter extends BaseFilter {
    
    private static Log log =
        LogFactory.getLog(AuthenticationFilter.class.getName());
    
    private static String LOGIN_URL = "/Login.do";
    
    /**
     * @param request The servlet request we are processing
     * @param result The servlet response we are creating
     * @param chain The filter chain we are processing
     */
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain)
        throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        //if a session does not already exist this call will create one
        HttpSession session = request.getSession();
        
        /* check if the user object is in the session.
         * if not then the user is not validated
         * and should be forwarded to the login page
         */
        WebUser webUser = SessionUtils.getWebUser(session);
        
        ServletContext ctx = session.getServletContext();
        if (webUser == null) {
            // See if there is authentication information
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                StringTokenizer token = new StringTokenizer(auth, " ");
                if (token.countTokens() == 2) {
                    String tok = token.nextToken();
                    assert(tok.equals("Basic"));
                    tok = token.nextToken();
                    String userpass = new String(Base64.decode(tok));
                    
                    token = new StringTokenizer(userpass, ":");
                    assert(token.countTokens() == 2);
                    String user = token.nextToken();
                    String pass = token.nextToken();
                    try {
                        webUser = AuthenticateUserAction.loginUser(request, ctx,
                                                                   user, pass);
                        session.setAttribute(Constants.WEBUSER_SES_ATTR,
                                             webUser);
                    } catch (Exception e) {
                        // Unsuccessful login
                        log.debug("User attempted to log in with " + userpass);
                    }
                }
            }
        }
        
        if (webUser == null) {
            // See if there is a guest user
            webUser = AuthenticateUserAction.loginGuest(request, ctx);            
        }
        
        if (webUser == null ){
            String path = request.getServletPath();
            
            if( LOGIN_URL.equals(path) || "/j_security_check.do".equals(path) )
                chain.doFilter(request, response);
            else{
                //copy the url and request parameters so that the user can be
                // forwarded to the originally requested page after
                // authorization
                if (path.indexOf("RecentAlerts") < 0 && path.indexOf("rss") < 0
                        && path.indexOf("IndicatorCharts") < 0) {
                    Map parameters = request.getParameterMap();
                    if( !parameters.isEmpty() ){
                        HashMap newMap = new HashMap();
                        
                        Iterator i = parameters.keySet().iterator();
                        
                        while( i.hasNext() ){
                            String key = (String) i.next();
                            newMap.put( key,  request.getParameter(key) );
                        }
                        
                        session.setAttribute(Constants.LOGON_URL_PARAMETERS,
                                             newMap);
                    }
                    
                    session.setAttribute( Constants.LOGON_URL_KEY, path);
                }
                
                response.sendRedirect(request.getContextPath() + LOGIN_URL);
            }
        }
        else{            
            HttpServletRequest hreq=(HttpServletRequest)request;
            try {
                chain.doFilter(request, response);
            } catch (IOException e) {
                log.warn("Caught IO Exception from client " +
                         hreq.getRemoteAddr() + ": " + e.getMessage());
            }
        }
    }
   
    public void init(FilterConfig filterConfig) {
        super.init(filterConfig);
    }    
}

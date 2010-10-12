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

package org.hyperic.hq.web.login;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * This controller provides the data needs of the login view. It doesn't actually perform any 
 * authentication as that's handled by Spring Security.
 * 
 * @author David Crutchfield
 *
 */
@Controller
public class LoginController {
    private static final Log log = LogFactory.getLog(LoginController.class.getName());
    
    private AuthzSubjectManager authzSubjectManager;
    
    @Autowired
    public LoginController(AuthzSubjectManager authzSubjectManager) {
    	this.authzSubjectManager = authzSubjectManager;
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        final boolean debug = log.isDebugEnabled();
        
        ModelAndView result = new ModelAndView();
        
        // ...first check for an authentication object, if one exists we are already logged in...
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && 
            !(authentication instanceof AnonymousAuthenticationToken) && 
            authentication.isAuthenticated()) {
            try {
                if (debug) log.debug("User has already been authenticated.  Redirecting to dashboard.");
                
                response.sendRedirect("/Dashboard.do");
                
                return result;
            } catch(IOException e) {
                log.warn("Could not perform the redirect for an authenticated user, displaying login page instead");
            }
        }
        
        // ...we're dealing with an unauthenticated user, we're going to show the login form...
        AuthzSubject guestUser = authzSubjectManager.getSubjectById(AuthzConstants.guestId);
        
        // ...before we return, check for an error message...
        boolean loginError = request.getParameter("authfailed") != null;
        
        if (loginError) {
            if (session != null) {
                AuthenticationException ex = (AuthenticationException) session.getAttribute(AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY);
                
                if (ex != null) {
                    result.addObject("errorMessage", RequestUtils.message(request, ex.getMessage()));
                }
            }
        }
        
        result.addObject("guestUsername", (guestUser != null) ? guestUser.getName() : "guest");
        result.addObject("guestEnabled", (guestUser != null && guestUser.getActive()));
        
        // ...set a response header so we can identify the login page explicitly...
        response.setHeader("hq-requires-auth", "1");
        
        return result;
    }
}
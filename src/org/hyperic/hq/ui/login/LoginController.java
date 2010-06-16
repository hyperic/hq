package org.hyperic.hq.ui.login;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {
    private static final Log log = LogFactory.getLog(LoginController.class.getName());
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response) {
        final boolean debug = log.isDebugEnabled();
        
        // ...first check for an authentication object, if one exists we are already logged in
        // This may be too simplistic if we start using the anonymous user mechanism,  
        // but right now we don't use it, so this is fine...
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && 
            !(authentication instanceof AnonymousAuthenticationToken) && 
            authentication.isAuthenticated()) {
            try {
                if (debug) log.debug("User has already been authenticated.  Redirect to dashboard.");
                
                response.sendRedirect("/Dashboard.do");
            } catch(IOException e) {
                log.warn("Could not perform the redirect for an authenticated user, displaying login page instead");
            }
        }
        
        // ...we're dealing with an unauthenticated user, we're going to show the login form...
        ModelAndView result = new ModelAndView();
        
        // TODO This is temporary for 4.3, we'll be revisiting this mechanism in a future version
        //      were this EJB business will be reworked...
        AuthzSubjectManagerLocal authzManager = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject guestUser = authzManager.getSubjectById(AuthzConstants.guestId);
        
        // ...before we return, check for an error message...
        boolean loginError = request.getParameter("authfailed") != null;
        
        if (loginError) {
            HttpSession session = request.getSession(false);

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

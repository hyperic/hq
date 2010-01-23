package org.hyperic.hq.ui.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.Constants;
import org.springframework.security.web.RedirectStrategy;

public class RegistrationRedirectStrategy implements RedirectStrategy {
    private String registrationUrl;
    
    public RegistrationRedirectStrategy(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }
    
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        // We determine if the user needs to register with HQ
        HttpSession session = request.getSession();
        Boolean needRegistration = (Boolean) session.getAttribute(Constants.NEEDS_REGISTRATION);
        
        if (Boolean.TRUE.equals(needRegistration)) {
            // We need to go to the user registration page
            session.removeAttribute(Constants.NEEDS_REGISTRATION);
            
            response.sendRedirect(this.registrationUrl);
        } else {
            response.sendRedirect(url);
        }
    }
}

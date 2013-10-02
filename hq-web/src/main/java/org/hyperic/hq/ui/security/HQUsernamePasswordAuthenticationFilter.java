package org.hyperic.hq.ui.security;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class HQUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

    public HQUsernamePasswordAuthenticationFilter(){
        super();
    }
    
    protected String obtainUsername(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("UTF-8");
        }catch(UnsupportedEncodingException e) {
        }
        return request.getParameter(SPRING_SECURITY_FORM_USERNAME_KEY);
    }
}

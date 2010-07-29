package org.hyperic.hq.web.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * This class is responsible for adding the WebUser object to the ModelAndView
 * 
 * @author David Crutchfield
 *
 */
public class UserPreferenceInterceptor extends HandlerInterceptorAdapter {
	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// ...TODO Right now we get this directly from the session, but it'd be nice to get this 
		// from Authentication.getPrincipal() instead...
		HttpSession session = request.getSession();
        WebUser webUser = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
        
        // ...if we have a web user, add it to the ModelAndView...
        if (webUser != null) {
        	modelAndView.addObject(webUser);
        }
	}	
}
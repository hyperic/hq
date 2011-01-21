package org.hyperic.hq.hqu.grails.web.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.hqu.grails.commons.WebUserWrapper;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * This interceptor is used to update WebUser in session scoped WebUserWrapper
 * bean. Interceptor should handle all requests in context where session is
 * expected. Any security filters, etc, should happen before this
 * interceptor is called.
 */
public class HQUGrailsRequestInterceptor extends HandlerInterceptorAdapter {

	private final static Log log = LogFactory.getLog(HQUGrailsRequestInterceptor.class);

	@Autowired
	private WebUserWrapper webUserWrapper;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		if(webUserWrapper.getWebUser() == null) {
			if(log.isDebugEnabled())
				log.debug("WebUser doesn't exist in wrapper, getting and setting it from request.");
			webUserWrapper.setWebUser((WebUser)request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR));
		} else {
			if(log.isDebugEnabled())
				log.debug("WebUser already exist in wrapper");			
		}
		
		return true;
	}	
	
}

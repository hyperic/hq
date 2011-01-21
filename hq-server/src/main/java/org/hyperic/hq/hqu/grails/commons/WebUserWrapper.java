package org.hyperic.hq.hqu.grails.commons;

import org.hyperic.hq.ui.WebUser;

/**
 * A simple class to wrap WebUser object. The actual usage of this
 * class should be handled through session scoped bean.
 * 
 * E.G:
 * <bean id="wrappedWebUser" class="org.hyperic.hq.hqu.grails.commons.WebUserWrapper" scope="session">
 *   <aop:scoped-proxy/>
 * </bean>
 */
public class WebUserWrapper {

	public WebUser webUser;

	public WebUser getWebUser() {
		return webUser;
	}

	public void setWebUser(WebUser webUser) {
		this.webUser = webUser;
	}
	
}

package org.hyperic.hq.ui.action.admin.home;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.springframework.stereotype.Component;

@Component(value="adminHomePortalActionNG")
public class AdminHomePortalActionNG extends BaseActionNG {

    private static final String TITLE_HOME = "user.admin.page.title";
    private static final String PORTLET_HOME = ".ng.admin.home";
    
	private final Log log = LogFactory.getLog(AdminHomePortalActionNG.class.getName());
    
	public String execute() throws Exception {
		
        Portal portal = Portal.createPortal(TITLE_HOME, PORTLET_HOME);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setPlugins();
        
		return SUCCESS;
	}
    
  
}

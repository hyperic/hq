/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
		setHeaderResources();
        Portal portal = Portal.createPortal(TITLE_HOME, PORTLET_HOME);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setPlugins();
        
		return SUCCESS;
	}
    
  
}

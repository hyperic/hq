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

package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("addNewResourcesProtletActionNG")
@Scope("prototype")
public class AddResourcesActionNG extends BaseActionNG implements ModelDriven<AddResourcesFormNG>{

    private final Log log = LogFactory.getLog(AddResourcesActionNG.class.getName());
    
    @Resource
    private ConfigurationProxy configurationProxy;
	
	private AddResourcesFormNG addForm = new AddResourcesFormNG();

	public AddResourcesFormNG getModel() {
		return addForm;
	}
	
	public AddResourcesFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddResourcesFormNG addForm) {
		this.addForm = addForm;
	}
	
	public String execute() throws Exception {
		this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);

		String forward = checkSubmit(addForm);
		
        if (forward != null) {

            if (forward.equals(BaseActionNG.CANCELED)) {
                log.trace("removing pending resources list");
                SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            } else if (forward.equals(BaseActionNG.ADD)) {
                log.trace("adding to pending resources list");
                if (addForm.getAvailableResources()!= null ) {
                	SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getAvailableResources());
                }
            } else if (forward.equals(BaseActionNG.REMOVE)) {
                log.trace("removing from pending resources list");
                if (addForm.getPendingResources()!= null ) {
                	SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getPendingResources());
                }
            }
            addForm.reset();
            return forward;
        }
        
        log.trace("getting pending resources list");
        List<String> pendingResourceIds = SessionUtils.getListAsListStr(request.getSession(),
            Constants.PENDING_RESOURCES_SES_ATTR);

        StringBuffer resourcesAsString = new StringBuffer();

        for (Iterator<String> i = pendingResourceIds.iterator(); i.hasNext();) {
            resourcesAsString.append(StringConstants.DASHBOARD_DELIMITER);
            resourcesAsString.append(i.next());
        }

        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        // RequestUtils.setConfirmation(request, "admin.user.confirm.AddResource");
        
        String currentKey = addForm.getKey();
        if (currentKey == null || currentKey.equals("")) {
        	currentKey = (String) session.getAttribute("currentPortletKey");
        }
        
        configurationProxy.setPreference(session, user, currentKey , resourcesAsString.toString()); 
        addForm.reset();
		return SUCCESS;
	}
	

}

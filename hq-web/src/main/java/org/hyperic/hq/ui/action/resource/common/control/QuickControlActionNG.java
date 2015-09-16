/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.resource.common.control;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Perform a quick control action on a resource.
 */
@Component("quickControlActionNG")
@Scope(value="prototype")
public class QuickControlActionNG
    extends BaseActionNG implements ModelDriven<QuickControlFormNG> {

    private final Log log = LogFactory.getLog(QuickControlActionNG.class.getName());
    @Resource
    private ControlBoss controlBoss;
    
    QuickControlFormNG qcForm = new QuickControlFormNG();
    private String type;
	private String rid;
   
    public String save() throws Exception {
    	request = getServletRequest();
        log.trace("performing resouce quick control action: " + qcForm.getResourceAction());

        try {

            int sessionId = RequestUtils.getSessionIdInt(request);

            // create the new action to schedule
            Integer id = qcForm.getResourceId();
            Integer type = qcForm.getResourceType();
            AppdefEntityID appdefId = new AppdefEntityID(type.intValue(), id);
            request.setAttribute(Constants.RESOURCE_PARAM, id);
            request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, type);

            String action = qcForm.getResourceAction();
            String args = qcForm.getArguments();

            if (AppdefEntityConstants.APPDEF_TYPE_GROUP == type) {
                controlBoss.doGroupAction(sessionId, appdefId, action, args, null);
            } else {
                controlBoss.doAction(sessionId, appdefId, action, args);
            }
            

            // set confirmation message
            String ctrlStr = qcForm.getResourceAction();
            addActionMessage(getText("resource.server.QuickControl.Confirmation", new String[] {ctrlStr}));
            type=qcForm.getResourceType();
            rid=qcForm.getResourceId()+"";
            qcForm.reset();
            return SUCCESS;
        } catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            this.addActionError(getText("resource.common.error.ControlNotEnabled"));
            return "Failure";
        } catch (PermissionException pe) {
        	this.addActionError(getText("resource.common.control.error.NewPermission"));
            return "Failure";
        }
    }

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public QuickControlFormNG getModel() {
		
		return qcForm;
	}

	public QuickControlFormNG getQcForm() {
		return qcForm;
	}

	public void setQcForm(QuickControlFormNG qcForm) {
		this.qcForm = qcForm;
	}
	
}

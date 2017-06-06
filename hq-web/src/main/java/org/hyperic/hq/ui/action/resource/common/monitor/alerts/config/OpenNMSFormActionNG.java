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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Edit an alert definition -- OpenNMS action.
 * 
 */
@Component("openNMSFormActionNG")
@Scope("prototype")
public class OpenNMSFormActionNG
    extends BaseActionNG implements ModelDriven<OpenNMSFormNG>{

	@Autowired
    private EventsBoss eventsBoss;

	protected String eid;
	protected String aetid;
	protected String alertDefId;

	
    private OpenNMSFormNG oForm = new OpenNMSFormNG();

    public String execute() throws Exception {

    	request = getServletRequest();
        int sessionID = RequestUtils.getSessionId(request).intValue();

        
        AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition(request, sessionID, eventsBoss);

        // See if there is already an OpenNMSAction
        ActionValue[] actions = adv.getActions();
        ActionValue existing = null;
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].getId().equals(oForm.getId())) {
                existing = actions[i];
                break;
            }
        }

        if (oForm.isDeleteClicked()) {
            if (existing != null) {
                existing.setConfig(null);
            }
        } else {
            // Create new OpenNMSAction to get configuration
            OpenNMSAction nmsAction = new OpenNMSAction();
            nmsAction.setServer(oForm.getServer());
            nmsAction.setIp(oForm.getIp());
            nmsAction.setPort(oForm.getPort());

            if (existing == null) {
                eventsBoss.createAction(sessionID, oForm.getAd(), nmsAction.getImplementor(), nmsAction
                    .getConfigResponse());
            } else {
                // Set the action configuration
                existing.setClassname(nmsAction.getImplementor());
                existing.setConfig(nmsAction.getConfigResponse().encode());
            }
        }

        if (existing != null) {
            eventsBoss.updateAction(sessionID, existing);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.ALERT_DEFINITION_PARAM, oForm.getAd());

        if (oForm.getAetid() != null && oForm.getAetid().length() > 0)
            params.put(Constants.APPDEF_RES_TYPE_ID, oForm.getAetid());
        else {
            AppdefEntityID aeid = RequestUtils.getEntityId(request);
            params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        }
        fillForwardParams();
        return SUCCESS;
    }

    public void fillForwardParams() {
		if (oForm.getAd() != null) {
			alertDefId = oForm.getAd().toString();
		}else{
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (oForm.getEid() != null) {
			eid = oForm.getEid();
		}else{
			eid = getServletRequest().getParameter("eid");
		}
		if (oForm.getAetid() != null) {
			aetid = oForm.getAetid();
		}else{
			aetid = getServletRequest().getParameter("aetid");
		}
	}
    
	public OpenNMSFormNG getoForm() {
		return oForm;
	}


	public String getEid() {
		return eid;
	}


	public void setEid(String eid) {
		this.eid = eid;
	}


	public String getAetid() {
		return aetid;
	}


	public void setAetid(String aetid) {
		this.aetid = aetid;
	}


	public String getAlertDefId() {
		return alertDefId;
	}


	public void setAlertDefId(String alertDefId) {
		this.alertDefId = alertDefId;
	}


	public void setoForm(OpenNMSFormNG oForm) {
		this.oForm = oForm;
	}


	public OpenNMSFormNG getModel() {
		
		return oForm;
	}
}

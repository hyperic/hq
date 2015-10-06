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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Create a new alert definition.
 * 
 */
@Component("editDefinitionPropertiesActionNG")
@Scope("prototype")
public class EditDefinitionPropertiesActionNG extends BaseActionNG implements
		ModelDriven<DefinitionFormNG> {

	private final Log log = LogFactory
			.getLog(EditDefinitionPropertiesActionNG.class.getName());

	private String eid;
	private String aetid;
	private String alertDefId;

	private DefinitionFormNG defForm = new DefinitionFormNG();

	@Autowired
	private EventsBoss eventsBoss;

	public String save() throws Exception {

		log.trace("defForm.id=" + defForm.getAd());

		if (defForm.getAd() != null) {
			alertDefId = defForm.getAd().toString();
		} else {
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (defForm.getEid() != null) {
			eid = defForm.getEid();
		} else {
			eid = getServletRequest().getParameter("eid");
		}
		if (defForm.getAetid() != null) {
			aetid = defForm.getAetid();
		} else {
			aetid = getServletRequest().getParameter("aetid");
		}
		Map<String, Object> params = new HashMap<String, Object>();
		AppdefEntityID adeId;
		if (defForm.getRid() != null) {
			adeId = new AppdefEntityID(defForm.getType().intValue(),
					defForm.getRid());
			params.put(Constants.ENTITY_ID_PARAM, adeId.getAppdefKey());
		} else {
			adeId = new AppdefEntityTypeID(defForm.getType().intValue(),
					defForm.getResourceType());
			params.put(Constants.APPDEF_RES_TYPE_ID, adeId.getAppdefKey());
		}
		params.put("ad", defForm.getAd());

		String forward = checkSubmit(defForm);
		if (forward != null) {
			log.trace("returning " + forward);
			return forward;
		}

		int sessionID = RequestUtils.getSessionId(request).intValue();

		try {
			eventsBoss.updateAlertDefinitionBasic(sessionID, defForm.getAd(),
					defForm.getName(), defForm.getDescription(),
					defForm.getPriority(), defForm.isActive());
		} catch (Exception e) {
			addFieldError("editAlertDefinitionError",
					getText("error.generic.temporarily.unavailable"));
			return "failure";
		}

		log.trace("returning success");
		return "save";

	}

	public String reset() throws Exception {
		defForm.reset();
		if (defForm.getAd() != null) {
			alertDefId = defForm.getAd().toString();
		} else {
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (defForm.getEid() != null) {
			eid = defForm.getEid();
		} else {
			eid = getServletRequest().getParameter("eid");
		}
		if (defForm.getAetid() != null) {
			aetid = defForm.getAetid();
		} else {
			aetid = getServletRequest().getParameter("aetid");
		}
		return RESET;
	}

	public String cancel() throws Exception {
		if (defForm.getAd() != null) {
			alertDefId = defForm.getAd().toString();
		} else {
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (defForm.getEid() != null) {
			eid = defForm.getEid();
		} else {
			eid = getServletRequest().getParameter("eid");
		}
		if (defForm.getAetid() != null) {
			aetid = defForm.getAetid();
		} else {
			aetid = getServletRequest().getParameter("aetid");
		}
		return CANCELED;
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

	public DefinitionFormNG getDefForm() {
		return defForm;
	}

	public void setDefForm(DefinitionFormNG defForm) {
		this.defForm = defForm;
	}

	public DefinitionFormNG getModel() {

		return defForm;
	}
}

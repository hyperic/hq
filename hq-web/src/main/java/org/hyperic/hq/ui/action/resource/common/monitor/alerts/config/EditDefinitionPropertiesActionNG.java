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
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceControllerNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

/**
 * Create a new alert definition.
 * 
 */
@Component("editDefinitionPropertiesActionNG")
@Scope("prototype")
public class EditDefinitionPropertiesActionNG extends ResourceControllerNG implements
		ModelDriven<DefinitionFormNG> , Preparable{

	private final Log log = LogFactory
			.getLog(EditDefinitionPropertiesActionNG.class.getName());

	private String internalEid;
	private String internalAetid;
	private String internalAlertDefId;

	private DefinitionFormNG defForm = new DefinitionFormNG();

	@Autowired
	private EventsBoss eventsBoss;

	
	public String save() throws Exception {

		log.trace("defForm.id=" + defForm.getAd());

		fillParams();
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

	@SkipValidation
	public String reset() throws Exception {
		defForm.reset();
		fillParams();
		return RESET;
	}

	@SkipValidation
	public String cancel() throws Exception {
		fillParams();
		return CANCELED;
	}

	public void fillParams() {
		if (defForm.getAd() != null) {
			internalAlertDefId = defForm.getAd().toString();
			getServletRequest().setAttribute("ad",internalAlertDefId);
		} else {
			internalAlertDefId = getServletRequest().getParameter("ad");
			getServletRequest().setAttribute("ad",internalAlertDefId);
		}
		if (defForm.getEid() != null) {
			internalEid = defForm.getEid();
			getServletRequest().setAttribute("eid",internalEid);
		} else {
			internalEid = getServletRequest().getParameter("eid");
			getServletRequest().setAttribute("eid",internalEid);
		}
		if (defForm.getAetid() != null) {
			internalAetid = defForm.getAetid();
			getServletRequest().setAttribute("aetid",internalAetid);
		} else {
			internalAetid = getServletRequest().getParameter("aetid");
			getServletRequest().setAttribute("aetid",internalAetid);
		}
	}

	
	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public String getInternalAetid() {
		return internalAetid;
	}

	public void setInternalAetid(String internalAetid) {
		this.internalAetid = internalAetid;
	}

	public String getInternalAlertDefId() {
		return internalAlertDefId;
	}

	public void setInternalAlertDefId(String internalAlertDefId) {
		this.internalAlertDefId = internalAlertDefId;
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

	public void prepare() throws Exception {
		setResource();
		fillParams();
		
	}
}

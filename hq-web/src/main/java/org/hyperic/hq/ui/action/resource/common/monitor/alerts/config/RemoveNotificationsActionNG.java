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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that removes notifications for an alert definition.
 * 
 */
public abstract class RemoveNotificationsActionNG extends BaseActionNG
		implements ModelDriven<RemoveNotificationsFormNG>, NotificationsAction {
	private final Log log = LogFactory.getLog(RemoveNotificationsActionNG.class
			.getName());

	@Autowired
	protected EventsBoss eventsBoss;

	protected String eid;
	protected String aetid;
	protected String alertDefId;

	
	protected RemoveNotificationsFormNG rnForm = new RemoveNotificationsFormNG();

	/**
	 * removes alert definitions
	 */
	public String execute() throws Exception {

		request = getServletRequest();
		Map<String, Object> params = new HashMap<String, Object>();

		if (rnForm.getAetid() != null)
			params.put(Constants.APPDEF_RES_TYPE_ID, rnForm.getAetid());
		else {
			AppdefEntityID aeid = new AppdefEntityID(rnForm.getType()
					.intValue(), rnForm.getRid());
			params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
		}
		params.put("ad", rnForm.getAd());

		Integer sessionID = RequestUtils.getSessionId(request);

		AlertDefinitionValue adv = eventsBoss.getAlertDefinition(
				sessionID.intValue(), rnForm.getAd());
		ActionValue[] actions = adv.getActions();
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].classnameHasBeenSet()
					&& !(actions[i].getClassname().equals(null) || actions[i]
							.getClassname().equals(""))) {
				EmailActionConfig emailCfg = new EmailActionConfig();
				ConfigResponse configResponse = ConfigResponse
						.decode(actions[i].getConfig());

				try {
					emailCfg.init(configResponse);
				} catch (InvalidActionDataException e) {
					// Not an EmailAction
					log.debug("Action is " + actions[i].getClassname());
					continue;
				}

				if (emailCfg.getType() == getNotificationType()) {
					return handleRemove(request, params, sessionID, actions[i],
							emailCfg, eventsBoss, rnForm);
				}
			}
		}

		return "failure";

	}

	public RemoveNotificationsFormNG getModel() {

		return rnForm;
	}

	public RemoveNotificationsFormNG getRnForm() {
		return rnForm;
	}

	public void setRnForm(RemoveNotificationsFormNG rnForm) {
		this.rnForm = rnForm;
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

	protected abstract String handleRemove(HttpServletRequest request,
			Map<String, Object> params, Integer sessionID, ActionValue action,
			EmailActionConfig ea, EventsBoss eb,
			RemoveNotificationsFormNG rnForm) throws Exception;

	public void fillForwardParams(RemoveNotificationsFormNG rnForm) {
		if (rnForm.getAd() != null) {
			alertDefId = rnForm.getAd().toString();
		}else{
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (rnForm.getEid() != null) {
			eid = rnForm.getEid();
		}else{
			eid = getServletRequest().getParameter("eid");
		}
		if (rnForm.getAetid() != null) {
			aetid = rnForm.getAetid();
		}else{
			aetid = getServletRequest().getParameter("aetid");
		}
	}
}

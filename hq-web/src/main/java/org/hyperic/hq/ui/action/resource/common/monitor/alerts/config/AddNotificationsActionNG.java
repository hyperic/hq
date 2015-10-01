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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for adding notifications to an alert definition.
 * 
 */
public abstract class AddNotificationsActionNG extends BaseActionNG implements
		NotificationsAction {
	private final Log log = LogFactory.getLog(AddNotificationsActionNG.class);

	protected String eid;
	protected String aetid;
	protected String alertDefId;
	
	protected AddNotificationsFormNG addForm = new AddNotificationsFormNG();
	
	@Autowired
	private EventsBoss eventsBoss;

	/**
	 * Add roles to the alert definition specified in the given
	 * <code>AddRolesForm</code>.
	 */
	public String execute() throws Exception {

		request = getServletRequest();
		HttpSession session = request.getSession();

		
		if (request.getParameter("ad") != null) {
			addForm.setAd(Integer.parseInt(request.getParameter("ad")));
		}
		if (request.getParameter("ad") != null) {
			addForm.setAetid(request.getParameter("aetid"));
		}

		Integer alertDefId = addForm.getAd();

		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Constants.ALERT_DEFINITION_PARAM, alertDefId);

		if (addForm.getAetid() != null && addForm.getAetid().length() > 0)
			params.put(Constants.APPDEF_RES_TYPE_ID, addForm.getAetid());
		else {
			AppdefEntityID aeid = RequestUtils.getEntityId(request);
			params.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
		}

		String forward = preProcess(request, addForm, params, session);
		if (forward != null) {
			return forward;
		}

		Integer sessionId = RequestUtils.getSessionId(request);

		AlertDefinitionValue ad = eventsBoss.getAlertDefinition(
				sessionId.intValue(), alertDefId);

		Set<Object> notifs = getNotifications(addForm, session);
		if (!notifs.isEmpty()) {
			// We'll try to get the appropriate email action for
			// the alert definition and update it if it already
			// exists. If not, we'll just create a new one.
			Object[] actionObjs = getActionObjects(ad);
			if (null == actionObjs) {
				EmailActionConfig ea = new EmailActionConfig();
				ea.setType(getNotificationType());
				log.debug("new notifs=" + notifs);
				ea.setNames(StringUtil.iteratorToString(notifs.iterator(), ",",
						""));
				eventsBoss.createAction(sessionId.intValue(), alertDefId,
						ea.getImplementor(), ea.getConfigResponse());
			} else {
				ActionValue action = (ActionValue) actionObjs[0];
				EmailActionConfig ea = (EmailActionConfig) actionObjs[1];
				notifs.addAll(ea.getUsers());
				log.debug("all notifs=" + notifs);
				ea.setNames(StringUtil.iteratorToString(notifs.iterator(), ",",
						""));
				byte[] configSchema = ea.getConfigResponse().encode();
				action.setConfig(configSchema);
				eventsBoss.updateAction(sessionId.intValue(), action);
			}
		}

		postProcess(request, session);

		return SUCCESS;
	}

	protected abstract String preProcess(HttpServletRequest request,
			AddNotificationsFormNG form, Map<String, Object> params,
			HttpSession session) throws Exception;

	protected abstract void postProcess(HttpServletRequest request,
			HttpSession session);

	protected abstract Set<Object> getNotifications(
			AddNotificationsFormNG form, HttpSession session);

	private Object[] getActionObjects(AlertDefinitionValue ad) throws Exception {
		ActionValue[] actions = ad.getActions();
		for (int i = 0; i < actions.length; i++) {
			if (actions[i].classnameHasBeenSet()
					&& !(actions[i].getClassname().equals(null) || actions[i]
							.getClassname().equals(""))) {
				Object obj = Class.forName(actions[i].getClassname())
						.newInstance();
				if (obj instanceof EmailActionConfig) {
					EmailActionConfig ea = (EmailActionConfig) obj;
					ConfigResponse configResponse = ConfigResponse
							.decode(actions[i].getConfig());
					ea.init(configResponse);
					if (ea.getType() == getNotificationType()) {
						// as soon as we find the right notification type,
						// return
						log.debug("found action: action=" + actions[i]
								+ ", ea=" + ea);
						return new Object[] { actions[i], ea };
					}
				}
			}
		}

		log.debug("no action found");
		return null;
	}

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getAlertDefId() {
		return alertDefId;
	}

	public void setAlertDefId(String alertDefId) {
		this.alertDefId = alertDefId;
	}

	public String getAetid() {
		return aetid;
	}

	public void setAetid(String aetid) {
		this.aetid = aetid;
	}
	
}

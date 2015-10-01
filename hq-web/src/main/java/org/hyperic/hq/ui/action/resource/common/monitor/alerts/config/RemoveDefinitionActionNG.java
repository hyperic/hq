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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.GalertBoss;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that removes an alert definition
 */
@Component("removeDefinitionActionNG")
@Scope("prototype")
public class RemoveDefinitionActionNG extends BaseActionNG implements
		ModelDriven<RemoveDefinitionFormNG> {

	@Autowired
	private EventsBoss eventsBoss;

	@Autowired
	private GalertBoss galertBoss;

	private RemoveDefinitionFormNG rdForm = new RemoveDefinitionFormNG();

	
	/**
	 * removes alert definitions
	 */
	public String execute() throws Exception {

		Log log = LogFactory.getLog(RemoveDefinitionActionNG.class.getName());

		request = getServletRequest();
		AppdefEntityID adeId;
		Map<String, Object> params = new HashMap<String, Object>();
		if (rdForm.getRid() != null) {
			adeId = new AppdefEntityID(rdForm.getType().intValue(),
					rdForm.getRid());
			params.put(Constants.ENTITY_ID_PARAM, adeId.getAppdefKey());
			log.debug("###eid = " + adeId.getAppdefKey());
		} else {
			adeId = new AppdefEntityTypeID(rdForm.getAetid());
			params.put(Constants.APPDEF_RES_TYPE_ID, adeId.getAppdefKey());
			log.debug("###aetid = " + adeId.getAppdefKey());
		}

		Integer[] defs = rdForm.getDefinitions();

		if (defs == null || defs.length == 0) {
			return SUCCESS;
		}

		Integer sessionId = RequestUtils.getSessionId(request);

		boolean enable = false;

		if (rdForm.getSetActiveInactive().equals("y")) {
			enable = rdForm.getActive().intValue() == 1;

			if (adeId.isGroup()) {
				GalertDef[] defPojos = new GalertDef[defs.length];
				for (int i = 0; i < defs.length; i++) {
					defPojos[i] = galertBoss.findDefinition(
							sessionId.intValue(), defs[i]);
				}
				galertBoss.enable(sessionId.intValue(), defPojos, enable);
			} else {
				eventsBoss.activateAlertDefinitions(sessionId.intValue(), defs,
						enable);
			}

			addActionMessage(getText("alerts.config.confirm.activeInactive"));
			return SUCCESS;
		}

		if (rdForm.isDeleteClicked()) {
			if (rdForm.getAetid() != null) {
				for (Integer def : defs) {
					eventsBoss.deleteAlertDefinitions(sessionId.intValue(),
							new Integer[] { def });
				}
				params.put(Constants.APPDEF_RES_TYPE_ID, rdForm.getAetid());
			} else {
				if (adeId.isGroup()) {
					galertBoss.markDefsDeleted(sessionId.intValue(), defs);
				} else {
					eventsBoss.deleteAlertDefinitions(sessionId.intValue(),
							defs);
				}
			}

			addActionMessage(getText("alerts.config.confirm.deleteConfig"));
		} else {
			// Delete the alerts for the definitions
			if (adeId.isGroup()) {
				// XXX - implement alert deletion in gBoss
			} else {
				eventsBoss.deleteAlertsForDefinitions(sessionId.intValue(),
						defs);
			}

			addActionMessage(getText("alerts.config.confirm.deleteAlerts"));
		}

		return SUCCESS;
	}

	
	public RemoveDefinitionFormNG getRdForm() {
		return rdForm;
	}


	public void setRdForm(RemoveDefinitionFormNG rdForm) {
		this.rdForm = rdForm;
	}


	public RemoveDefinitionFormNG getModel() {
		return rdForm;
	}
}

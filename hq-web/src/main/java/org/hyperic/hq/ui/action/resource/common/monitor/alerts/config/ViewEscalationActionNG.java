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

import javax.servlet.ServletException;

import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component("viewEscalationActionNG")
public class ViewEscalationActionNG extends ViewDefinitionActionNG {

	//private EscalationSchemeFormNG eForm = new EscalationSchemeFormNG();
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
                EscalationSchemeFormNG eForm = new EscalationSchemeFormNG();
		request = getServletRequest();
		Integer sessionID;
		try {
			sessionID = RequestUtils.getSessionId(request);

			int sessionId = sessionID.intValue();

			if(request.getParameter("ad") != null){
				try{
					eForm.setAd(Integer.parseInt(request.getParameter("ad")));
				}catch(Exception e){
					// do nothing parameter wasn't found
				}
			}
			if(request.getParameter("escId") != null){
				try{
					eForm.setEscId(Integer.parseInt(request.getParameter("escId")));
				}catch(Exception e){
					// do nothing parameter wasn't found
				}
			}
			// Get alert type
			EscalationAlertType mat = ClassicEscalationAlertType.CLASSIC;
			try {
				RequestUtils.getEntityTypeId(request);
			} catch (ParameterNotFoundException e) {
				AppdefEntityID aeid = RequestUtils.getEntityId(request);
				if (aeid.isGroup()) {
					mat = GalertEscalationAlertType.GALERT;
				}
			}

			// Get the list of escalations

			if (request.getAttribute("escalations") == null) {
				JSONArray arr = eventsBoss.listAllEscalationName(sessionId);
				request.setAttribute("escalations", arr);
			}

			// Get the list of users

			PageList<AuthzSubjectValue> availableUsers = authzBoss
					.getAllSubjects(sessionID, null, PageControl.PAGE_ALL);
			request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

			if (eForm.getEscId() == null) {
				eForm.setEscId(eventsBoss.getEscalationIdByAlertDefId(
						sessionId, new Integer(eForm.getAd()), mat));
			} else {
				if (eForm.getEscId().intValue() == 0) {
					// Unset current escalation scheme
					eventsBoss.unsetEscalationByAlertDefId(sessionId,
							new Integer(eForm.getAd()), mat);
					eForm.setEscId(null);
				} else {
					// We actually need to set the escalation scheme for alert
					// definition
					
					AlertDefinitionValue adv = eventsBoss.getAlertDefinition(sessionID,
							eForm.getAd());
					// check if alert has already an Esc associated
					if (adv.getEscalationId() != null ) {
						
						int escID = eForm.getEscId();
						int alertEscId = adv.getEscalationId();
						// Only update if Esc has changed
						if ( !(alertEscId == escID) ) {
							eventsBoss.setEscalationByAlertDefId(sessionId,
									new Integer(eForm.getAd()), eForm.getEscId(), mat);
						}
					} else {
						eventsBoss.setEscalationByAlertDefId(sessionId,
								new Integer(eForm.getAd()), eForm.getEscId(), mat);
					}
				}
			}

			// Look for the escalation request parameter
			try {
				if (eForm.getEscId() != null) {
					JSONObject escalation = Escalation.getJSON(eventsBoss
							.findEscalationById(sessionId, eForm.getEscId()));
					request.setAttribute("escalationJSON",
							escalation.toString());
				}
			} catch (ParameterNotFoundException e) {
				// No big deal, assume new
			}

			super.doExecute(request);
		} catch (ServletException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (JSONException e) {
			log.error(e);
		} catch (NotFoundException e) {
			log.error(e);
		}
	}
}

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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceControllerNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.StringifiedException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("platformAutoDiscoveryActionNG")
@Scope(value="prototype")
public class PlatformAutoDiscoveryActionNG extends ResourceControllerNG {

    private static final String TITLE_PATH = "resource.autodiscovery.inventory.";

    @Resource
    private AIBoss aiBoss;
    
	public String newAutoDiscovery() throws Exception {

		int sessionId = RequestUtils.getSessionIdInt(request);

		// If the caller specified an aiPlatformID, then lookup
		// the agent token and other stuff from that.

		try {
			Integer aiPid = RequestUtils.getIntParameter(request, Constants.AI_PLATFORM_PARAM);
			AIPlatformValue aiPlatform = aiBoss.findAIPlatformById(sessionId, aiPid.intValue());

			try {
				aiBoss.getScanStatusByAgentToken(sessionId, aiPlatform.getAgentToken());
			} catch (AgentConnectionException e) {
				log.warn("AgentConnectException: " + e);
			} catch (AgentNotFoundException e) {
				log.warn("AgentNotFoundException: " + e);
			}

			request.setAttribute(Constants.TITLE_PARAM_ATTR, aiPlatform.getName());
		} catch (ParameterNotFoundException e) {
			findAndSetResource(request, response);

			PlatformValue pVal = (PlatformValue) RequestUtils.getResource(request);
			try {
				aiBoss.getScanStatus(sessionId, pVal.getId().intValue());
			} catch (AgentConnectionException ace) {
				// redirect to viewResource
				return "viewResource";
			} catch (AgentNotFoundException anfe) {
				// redirect to viewResource
				return "viewResource";
			}
		}

		Portal portal = Portal.createPortal(TITLE_PATH
				+ "NewPlatformAutodiscoveryTitle",
				".resource.platform.autodiscovery.NewAutoDiscovery");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "newAutoDiscovery";
	}

	public String editAutoDiscovery() throws Exception {

		findAndSetResource(request, response);
		findAndSetAISchedule(request);

		Portal portal = Portal.createPortal(TITLE_PATH
				+ "EditPlatformAutodiscoveryTitle",
				".resource.platform.autodiscovery.EditAutoDiscovery");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editAutoDiscovery";
	}

	public String viewResource( )
			throws Exception {

		findAndSetResource(request, response);

		Portal portal = Portal.createPortal(
				"resource.platform.inventory.ViewPlatformTitle",
				".resource.platform.inventory.ViewPlatform");
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewResource";
	}

	public String viewResults( )
			throws Exception {

		results();
		
		findAndSetResource(request, response);

		Portal portal = Portal.createPortal(TITLE_PATH
				+ "ViewAutodiscoveryResultsTitle",
				".resource.platform.autodiscovery.ViewResults");
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewResults";
	}

	private void results( )
			throws Exception {

		loadAIResource(request);

	}

	private void findAndSetAISchedule(HttpServletRequest request)
			throws Exception {

		int sessionId = RequestUtils.getSessionIdInt(request);

		Integer scheduleId = RequestUtils.getScheduleId(request);
		AIScheduleValue sValue = aiBoss.findScheduledJobById(sessionId,
				scheduleId);

		request.setAttribute(Constants.AISCHEDULE_ATTR, sValue);
	}

	private void findAndSetResource(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		setResource();

		PlatformValue pVal = (PlatformValue) RequestUtils.getResource(request);
		if (pVal != null)
			loadScanState(request, pVal);
	}

	/**
	 * Loads the scan state using a platform value
	 */
	private void loadScanState(HttpServletRequest request, PlatformValue pVal)
			throws ServletException, SessionTimeoutException,
			SessionNotFoundException, PermissionException,
			AgentRemoteException, AutoinventoryException, RemoteException {

		int sessionId = RequestUtils.getSessionIdInt(request);

		AIPlatformValue aip;
		ScanState ss;
		ScanStateCore ssc;
		try {
			ssc = aiBoss.getScanStatus(sessionId, pVal.getId().intValue());
			ss = new ScanState(ssc);
			StringifiedException lastException = BizappUtils.findLastError(ssc);

			request.setAttribute(Constants.LAST_AI_ERROR_ATTR, lastException);

		} catch (AgentConnectionException e) {
			addActionError("resource.platform.inventory.configProps.NoAgentConnection");
			return;
		} catch (AgentNotFoundException e) {
			addActionError("resource.platform.inventory.configProps.NoAgentConnection");
			return;
		}

		try {
			aip = aiBoss.findAIPlatformByPlatformID(sessionId, pVal.getId()
					.intValue());
		} catch (PlatformNotFoundException e) {
			aip = null;
		}

		// load the scan state object
		request.setAttribute(Constants.SCAN_STATE_ATTR, ss);
		request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
	}

	public void loadAIResource(HttpServletRequest request) throws Exception {

		int sessionId = RequestUtils.getSessionIdInt(request);

		Integer id = RequestUtils.getIntParameter(request,
				Constants.AI_PLATFORM_PARAM);
		AIPlatformValue aip = aiBoss.findAIPlatformById(sessionId,
				id.intValue());

		request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
		request.setAttribute(Constants.TITLE_PARAM_ATTR, aip.getName());
	}
    
}

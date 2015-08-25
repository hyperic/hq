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

package org.hyperic.hq.ui.action.resource.platform.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * A <code>BaseAction</code> subclass that edits the type and network properties
 * of a platform in the BizApp.
 */
@Component("editPlatformTypeNetworkPropertiesActionNG")
@Scope(value = "prototype")
public class EditPlatformTypeNetworkPropertiesActionNG extends
		ResourceInventoryPortalActionNG implements ModelDriven<PlatformFormNG> {

	private final Log log = LogFactory
			.getLog(EditPlatformTypeNetworkPropertiesActionNG.class.getName());
	@Autowired
	private AppdefBoss appdefBoss;
	private PlatformFormNG platformForm = new PlatformFormNG();
	private String internalEid;

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	/**
	 * Retrieve the data necessary to display the
	 * <code>TypeNetworkPropertiesForm</code> page.
	 * 
	 */
	@SkipValidation
	public String start() throws Exception {

		setResource();

		Integer platformId = platformForm.getRid();
		Integer sessionId = RequestUtils.getSessionId(request);
		PlatformValue platform = appdefBoss.findPlatformById(
				sessionId.intValue(), platformId);

		if (platform == null) {
			// addActionError(getText(Constants.ERR_PLATFORM_NOT_FOUND));
			log.error(Constants.FAILURE_URL);
			return Constants.FAILURE_URL;
		}

		platformForm.loadPlatformValue(platform);

		log.trace("getting all platform types");
		List<PlatformTypeValue> platformTypes = appdefBoss
				.findAllPlatformTypes(sessionId.intValue(),
						PageControl.PAGE_ALL);
		platformForm.setResourceTypes(platformTypes);

		String usedIpPort = "";
		if (platform.getAgent() != null) {
			usedIpPort = platform.getAgent().getAddress() + ":"
					+ platform.getAgent().getPort();
		}
		BizappUtilsNG.populateAgentConnections(sessionId.intValue(),
				appdefBoss, request, platformForm, usedIpPort);

		// respond to an add or remove click- we do this here
		// rather than in the edit Action because in between
		// these two actions, struts repopulates the form bean and
		// resets numIps to whatever value was submitted in the
		// request.

		if (platformForm.isAddClicked()) {
			IpValue[] savedIps = platform.getIpValues();
			int numSavedIps = savedIps != null ? savedIps.length : 0;

			int nextIndex = platformForm.getNumIps();
			log.trace("ips next index: " + nextIndex);
			for (int i = 0; i < nextIndex + 1; i++) {
				IpValue oldIp = i < numSavedIps ? savedIps[i] : null;
				if (oldIp == null) {
					platformForm.setIp(i, new IpValue());
				}
			}

			platformForm.setNumIps(nextIndex + 1);
			log.trace("add num ips: " + platformForm.getNumIps());
		} else if (platformForm.isRemoveClicked()) {
			int ri = Integer.parseInt(platformForm.getRemove().getX());

			IpValue oldIps[] = platformForm.getIps();
			if (oldIps != null) {
				// remove the indicated ip, leaving all others
				// intact
				ArrayList<IpValue> oldIpsList = new ArrayList<IpValue>(
						Arrays.asList(oldIps));
				oldIpsList.remove(ri);
				IpValue[] newIps = (IpValue[]) oldIpsList
						.toArray(new IpValue[0]);

				// automatically sets numIps
				platformForm.setIps(newIps);
			}
		} else if (!platformForm.isOkClicked()) {
			// the form is being set up for the first time.
			IpValue[] savedIps = platform.getIpValues();
			int numSavedIps = savedIps != null ? savedIps.length : 0;

			for (int i = 0; i < numSavedIps; i++) {
				platformForm.setIp(i, savedIps[i]);
			}

			platformForm.setNumIps(numSavedIps);
		}

		// the OSType dropdown is NOT editable in edit mode hence the false
		request.setAttribute(Constants.PLATFORM_OS_EDITABLE, Boolean.FALSE);
		return "formLoad";

	}

	public String save() throws Exception {
		request = getServletRequest();
		AppdefEntityID aeid = new AppdefEntityID(platformForm.getType()
				.intValue(), platformForm.getRid());
		request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
		request.setAttribute(Constants.ACCORDION_PARAM, "1");
		Integer platformId = platformForm.getRid();
		Integer entityType = platformForm.getType();
		internalEid = entityType + ":" + platformId;

		try {
			String forward = checkSubmit(platformForm);

			Integer sessionId = RequestUtils.getSessionId(request);

			// now set up the platform
			PlatformValue platform = appdefBoss.findPlatformById(
					sessionId.intValue(), platformId);

			if (platform == null) {
				addActionError(getText("resource.platform.error.PlatformNotFound"));
				return "input";
			}
			platform = (PlatformValue) platform.clone();
			String[] addressesToValidate = platformForm.getAddresses();
			boolean validationFailed=false;
			for (int i = 0; i < addressesToValidate.length; i++) {
				if (addressesToValidate[i] == null
						|| addressesToValidate[i].isEmpty()) {
					this.addFieldError(
							"addresses[" + i + "]",
							getText("resource.platform.inventory.error.IpAddressIsRequired"));
					// add error attribute to the request and use it to set td class in jsp
					request.setAttribute("e","error");
					validationFailed=true;
					// return INPUT;
				} else {
					Pattern p = Pattern
							.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");
					Matcher m = p.matcher(addressesToValidate[i]);
					if (!m.find()) {
						this.addFieldError(
								"addresses[" + i + "]",
								getText("resource.platform.inventory.error.IpAddressInvalid"));
						validationFailed=true;
						// return INPUT;

					}
				}
			}
			if (validationFailed) {
				return INPUT;
			}
			platformForm.updatePlatformValue(platform);
			if (forward != null) {
				if (ADD.equals(forward) || REMOVE.equals(forward)) {
					return start();
				}

				return forward;

			}

			Agent agent = BizappUtilsNG.getAgentConnection(
					sessionId.intValue(), appdefBoss, request, platformForm);
			if (agent != null) {
				platform.setAgent(agent);
			}

			log.trace("editing general properties of platform ["
					+ platform.getName() + "]" + " with attributes " + platform
					+ " and ips " + Arrays.asList(platform.getIpValues()));
			appdefBoss.updatePlatform(sessionId.intValue(), platform);

			addActionMessage(getText("resource.platform.inventory.confirm.EditTypeNetworkProperties"));
			return SUCCESS;
		} catch (AppdefDuplicateFQDNException e) {
			this.addActionError("resource.platform.inventory.error.DuplicateFQDN");
			return Constants.FAILURE_URL;

		} catch (ApplicationException e) {
			// RequestUtils.setErrorObject(request,
			// "dash.autoDiscovery.import.Error", e.getMessage());
			return Constants.FAILURE_URL;
		}
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		internalEid = getServletRequest().getParameter("eid").toString();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		platformForm.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	public PlatformFormNG getPlatformForm() {
		return platformForm;
	}

	public void setPlatformForm(PlatformFormNG platformForm) {
		this.platformForm = platformForm;
	}

	public PlatformFormNG getModel() {
		return platformForm;
	}

}

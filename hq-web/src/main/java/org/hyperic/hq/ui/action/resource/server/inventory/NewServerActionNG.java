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

package org.hyperic.hq.ui.action.resource.server.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

/**
 * Create the server with the attributes specified in the given
 * <code>ServerForm</code>.
 */
@Component("newServerActionNG")
@Scope(value = "prototype")
public class NewServerActionNG extends BaseActionNG implements
		ModelDriven<ServerFormNG>, Preparable {

	private final Log log = LogFactory
			.getLog(NewServerActionNG.class.getName());
	@Resource
	private AppdefBoss appdefBoss;
	private ServerFormNG newForm = new ServerFormNG();
	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
	
	private List resourceTypesList;

	@SkipValidation
	public String start() throws Exception {
		request = getServletRequest();
		setHeaderResources();
		Integer platformId = newForm.getRid();
		Integer resourceType = newForm.getType();

		// Clean older drop down list
		this.removeValueInSession("newServerResourcesTypeList");
		
		try {
			Integer sessionId = RequestUtils.getSessionId(request);

			if (platformId == null) {
				platformId = RequestUtils.getResourceId(request);
			}
			if (resourceType == null) {
				resourceType = RequestUtils.getResourceTypeId(request);
			}
/*
			PlatformValue pValue = appdefBoss.findPlatformById(
					sessionId.intValue(), platformId);

			List<ServerTypeValue> stValues = appdefBoss
					.findServerTypesByPlatformType(sessionId.intValue(), pValue
							.getPlatformType().getId(), PageControl.PAGE_ALL);

			TreeMap<String, ServerTypeValue> returnMap = new TreeMap<String, ServerTypeValue>();
			for (ServerTypeValue stv : stValues) {

				if (!stv.getVirtual()) {
					returnMap.put(stv.getSortName(), stv);
				}
			}
			*/
			// newForm.setResourceTypes(new ArrayList<ServerTypeValue>(returnMap.values()));
			// request.setAttribute(Constants.PARENT_RESOURCE_ATTR, pValue);
			// request.setAttribute("resourcesTypeList", new ArrayList<ServerTypeValue>(returnMap.values()));
			// this.setResourceTypesList(new ArrayList<ServerTypeValue>(returnMap.values()));
			// this.setValueInSession("newServerResourcesTypeList",new ArrayList<ServerTypeValue>(returnMap.values()));
			// newForm.setRid(platformId);
			// newForm.setType(resourceType);

			return "formLoad";
		} catch (Throwable t) {
			throw new ServletException("Can't prepare new server form", t);
		}
	}

	public void prepare() throws Exception {
		request = getServletRequest();
		HttpSession session = request.getSession();
		this.setResourceTypesList((ArrayList<ServerTypeValue>) session.getAttribute("newServerResourcesTypeList") ); 
	}
	
	public String save() throws Exception {
		ServerValue server = new ServerValue();
		request = getServletRequest();
		Integer platformId = newForm.getRid();
		Integer entityType = newForm.getType();
		// internalEid = entityType + ":" + platformId;
		try {
			AppdefEntityID aeid = new AppdefEntityID(newForm.getType()
					.intValue(), newForm.getRid());

			request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
			request.setAttribute(Constants.ACCORDION_PARAM, "2");

			String forward = checkSubmit(newForm);
			if (forward != null) {
				return forward;
			}

			Integer sessionId = RequestUtils.getSessionId(request);

			

			server.setName(newForm.getName());
			server.setDescription(newForm.getDescription());
			server.setInstallPath(newForm.getInstallPath());
			// NOTE: DON'T SET THE AI IDENTIFIER -- ONLY SERVERS CREATED VIA
			// AUTOINVENTORY SHOULD EVER SET THIS VALUE.
			// FOR OTHER SERVERS, IT WILL BE SET AUTOMAGICALLY TO A UNIQUE VALUE
			// server.setAutoinventoryIdentifier(newForm.getInstallPath());

			platformId = RequestUtils.getResourceId(request);
			Integer ppk = platformId;
			Integer stPk = newForm.getResourceType();

			log.trace("creating server [" + server.getName() + "]"
					+ " with attributes " + newForm);

			ServerValue newServer = appdefBoss.createServer(
					sessionId.intValue(), server, ppk, stPk, null);
			Integer serverId = newServer.getId();
			AppdefEntityID entityId = newServer.getEntityId();

			newForm.setRid(serverId);
			
			addActionMessage(getText("resource.server.inventory.confirm.CreateServer",new String[] { newForm.getName() }));

			request.setAttribute(Constants.ENTITY_ID_PARAM,entityId.getAppdefKey());
			request.setAttribute(Constants.ACCORDION_PARAM, "0");
			String eid = entityId.getAppdefKey();
			setInternalEid(eid);
			
		} catch (AppdefDuplicateNameException e1) {
			this.addActionError("ERR_DUP_RESOURCE_FOUND");
			return INPUT;

		}
		return SUCCESS;
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			internalEid = aeid.toString();
		}
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		newForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			setEntityRequestParams(aeid);
		}
		return "reset";
	}

	public ServerFormNG getNewForm() {
		return newForm;
	}

	public void setNewForm(ServerFormNG newForm) {
		this.newForm = newForm;
	}

	public ServerFormNG getModel() {
		return newForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public List getResourceTypesList() {
		return resourceTypesList;
	}

	public void setResourceTypesList(List resourceTypesList) {
		this.resourceTypesList = resourceTypesList;
	}

	public Integer getInternalRid() {
		return internalRid;
	}

	public void setInternalRid(Integer internalRid) {
		this.internalRid = internalRid;
	}

	public Integer getInternalType() {
		return internalType;
	}

	public void setInternalType(Integer internalType) {
		this.internalType = internalType;
	}

	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId();
		this.internalType = eid.getType();
	}
}

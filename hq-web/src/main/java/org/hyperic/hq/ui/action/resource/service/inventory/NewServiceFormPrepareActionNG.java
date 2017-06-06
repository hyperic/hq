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

package org.hyperic.hq.ui.action.resource.service.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.action.resource.server.inventory.NewServerFormPrepareActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.stereotype.Component;

@Component("newServiceFormPrepareActionNG")
public class NewServiceFormPrepareActionNG extends BaseActionNG implements
		ViewPreparer {
	// log
	private final Log log = LogFactory
			.getLog(NewServerFormPrepareActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;
	
	private List<ServiceTypeValue> serviceTypes;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			request = getServletRequest();
			setHeaderResources();
			ResourceFormNG newForm = new ResourceFormNG();

			int sessionId = RequestUtils.getSessionId(request).intValue();

			AppdefEntityID aeid = RequestUtils.getEntityId(request);
			request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
			Integer parentId = aeid.getId();
			List serviceTypeVals = new ArrayList();
			ServerTypeValue svrType = null;
			ServerValue svrVal = null;
			try {
				AppdefEntityTypeID atid = RequestUtils
						.getChildResourceTypeId(request);

				// parent is a platform, we're creating a platform service
				if (atid.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
					List<ServerValue> servers = appdefBoss
							.findServersByTypeAndPlatform(sessionId, parentId,
									atid.getID(), PageControl.PAGE_ALL);
					// look for the correct server parent
					svrVal = (ServerValue) servers.get(0);
					svrType = svrVal.getServerType();
				} else {
					// Just get the service type
					serviceTypeVals.add(appdefBoss.findServiceTypeById(sessionId, atid.getId()));
					newForm.setResourceType(atid.getId());

					svrVal = appdefBoss.findVirtualServerByPlatformServiceType(
							sessionId, parentId, atid.getId());
				}
			} catch (ParameterNotFoundException e) {
				if (aeid.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
					svrVal = appdefBoss.findServerById(sessionId, parentId);
					svrType = svrVal.getServerType();
				} else {
					serviceTypeVals.addAll(appdefBoss.findViewablePlatformServiceTypes(sessionId,aeid.getId()));

				}
			}

			// Set the server value
			request.setAttribute(Constants.PARENT_RESOURCE_ATTR, svrVal);

			if (svrVal != null) {
				newForm.setRid(svrVal.getId());
				newForm.setType(new Integer(
						AppdefEntityConstants.APPDEF_TYPE_SERVER));
			} else {
				newForm.setRid(aeid.getId());
				newForm.setType(new Integer(aeid.getType()));
			}

			if (svrType != null) {
				List<ServiceTypeValue> serviceTypes = appdefBoss
						.findServiceTypesByServerType(sessionId, svrType
								.getId().intValue());
				CollectionUtils.addAll(serviceTypeVals, serviceTypes.toArray());
			}
			newForm.setResourceTypes(serviceTypeVals);
			this.setValueInSession("newServiceResourcesTypeList",serviceTypeVals);
			
			if (request.getParameter("name")!= null ){
				newForm.setName(request.getParameter("name"));
			}
			if (request.getParameter("description")!= null ){
				newForm.setDescription(request.getParameter("description"));
			}
			if (request.getParameter("resourceType")!= null ){
				Integer resType = Integer.valueOf(request.getParameter("resourceType"));
				newForm.setResourceType(resType);
			}
			request.setAttribute("newForm", newForm);	
			// request.setAttribute("serviceTypesList", serviceTypes);	
		} catch (Exception e) {
			log.error("Error in preparer", e);
		}
	}

	public List<ServiceTypeValue> getServiceTypes() {
		return serviceTypes;
	}

	public void setServiceTypes(List<ServiceTypeValue> serviceTypes) {
		this.serviceTypes = serviceTypes;
	}

}

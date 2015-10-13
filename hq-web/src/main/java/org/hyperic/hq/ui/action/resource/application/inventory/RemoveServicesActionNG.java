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

package org.hyperic.hq.ui.action.resource.application.inventory;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("applicationRemoveServicesActionNG")
@Scope("prototype")
public class RemoveServicesActionNG extends BaseActionNG implements
		ModelDriven<RemoveAppServicesFormNG> {

	private final Log log = LogFactory.getLog(RemoveServicesActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;

	RemoveAppServicesFormNG cform = new RemoveAppServicesFormNG();

	private String internalEid;

	@SkipValidation
	public String execute() throws Exception {
		
		request.setAttribute(Constants.ENTITY_ID_PARAM, cform.getEid());
		request.setAttribute(Constants.ACCORDION_PARAM, "3");

		Integer[] appSvcIds = cform.getResources();
		if (appSvcIds != null && appSvcIds.length > 0) {

			Integer sessionId = RequestUtils.getSessionId(request);

			for (int i = 0; i < appSvcIds.length; i++) {
				Integer appSvcId = appSvcIds[i];
				log.debug("Removing appSvc = " + appSvcId
						+ "  from application " + cform.getRid());
				appdefBoss.removeAppService(sessionId.intValue(),
						cform.getRid(), appSvcId);
			}

			addActionMessage( getText("resource.application.inventory.confirm.RemoveServices") );

		}
		internalEid = cform.getEid();

		return SUCCESS;
	}

	public RemoveAppServicesFormNG getModel() {
		return cform;
	}

	public RemoveAppServicesFormNG getPForm() {
		return cform;
	}

	public void setPForm(RemoveAppServicesFormNG cform) {
		this.cform = cform;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
}

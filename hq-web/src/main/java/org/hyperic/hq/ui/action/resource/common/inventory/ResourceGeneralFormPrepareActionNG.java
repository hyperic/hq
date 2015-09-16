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

package org.hyperic.hq.ui.action.resource.common.inventory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;
import org.hyperic.hq.ui.action.resource.group.inventory.EditGeneralActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("resourceGeneralFormPrepareActionNG")
public class ResourceGeneralFormPrepareActionNG extends BaseActionNG implements
		ViewPreparer {
	private final Log log = LogFactory
			.getLog(ResourceGeneralFormPrepareActionNG.class);
	@Resource
	private AppdefBoss appdefBoss;

	/**
	 * Retrieve this data and store it in the <code>GroupForm</code>:
	 * 
	 */
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		ResourceFormNG resourceForm = new ResourceFormNG();
		request = getServletRequest();
		Integer sessionId;
		try {
			
				sessionId = RequestUtils.getSessionId(request);
    			AppdefEntityID rid = RequestUtils.getEntityId(request);
				AppdefResourceValue resource = appdefBoss.findById(sessionId.intValue(), rid);
				resourceForm.loadResourceValue(resource);
				request.setAttribute("resourceForm", resourceForm);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

	}

}

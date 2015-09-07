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

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.inventory.EditPlatformTypeNetworkPropertiesActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

@Component("editServerTypeHostPrepareActionNG")
public class EditTypeHostPrepareActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory
			.getLog(EditTypeHostPrepareActionNG.class);

	@Resource
	private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			
			this.request = getServletRequest();
			ServerFormNG serverForm = new ServerFormNG();

			Integer sessionId;

			sessionId = RequestUtils.getSessionId(request);

			Integer serverId = RequestUtils.getResourceId(request);

			ServerValue sValue = appdefBoss.findServerById(sessionId.intValue(), serverId);

			serverForm.loadResourceValue(sValue);

			request.setAttribute(Constants.PARENT_RESOURCE_ATTR,
					sValue.getPlatform());
			RequestUtils.setResource(request, sValue);
			request.setAttribute("serverForm", serverForm);
			request.setAttribute(Constants.TITLE_PARAM_ATTR, serverForm.getName() ); 	
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}

}

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

import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

@Component("addApplicationServiceFormPrepareActionNG")
public class AddApplicationServiceFormPrepareActionNG extends BaseActionNG
		implements ViewPreparer {

    private final Log log = LogFactory.getLog(AddApplicationServiceFormPrepareActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	        AddApplicationServicesFormNG addForm = new AddApplicationServicesFormNG();
	        // String nameFilter = addForm.getNameFilter();
	        String nameFilter = request.getParameter("nameFilter");
	        AppdefResourceValue resource = RequestUtils.getResource(request);
	        AppdefEntityID entityId = resource.getEntityId();
	        Integer sessionId = RequestUtils.getSessionId(request);
	        PageControl pca = RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
	        PageControl pcp = RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");
	
	        addForm.setRid(resource.getId());
	        addForm.setType(new Integer(entityId.getType()));
	
	        // pending groups are those on the right side of the "add
	        // to list" widget- awaiting association with the resource
	        // when the form's "ok" button is clicked.
	        // 
	        // available services are all services in the system that are
	        // _not_ associated with the application and are not pending
	
	        PageList<AppdefResourceValue> pendingServices;
	        AppdefEntityID[] pendingServiceIds = null;
	
	        // Pending Services
	        // Find pending services that aren't yet assigned but that
	        // need to be displayed as such.
	
	        if (request.getSession().getAttribute(Constants.PENDING_APPSVCS_SES_ATTR) != null &&
	            (SessionUtils.getListAsListStr(request.getSession(), Constants.PENDING_APPSVCS_SES_ATTR)).size() > 0) {
	            List<String> uiPendings = SessionUtils.getListAsListStr(request.getSession(),
	                Constants.PENDING_APPSVCS_SES_ATTR);
	
	            pendingServiceIds = new AppdefEntityID[uiPendings.size()];
	
	            for (int i = 0; i < uiPendings.size(); i++) {
	                String fromList = (String) uiPendings.get(i);
	                StringTokenizer tok = new StringTokenizer(fromList, " ");
	                while (tok.hasMoreTokens()) {
	                    pendingServiceIds[i] = new AppdefEntityID(tok.nextToken());
	                    log.debug("pendingServiceIds = " + pendingServiceIds[i]);
	                }
	            }
	
	            log.debug("pendingServiceIds = " + pendingServiceIds);
	
	            pendingServices = appdefBoss.findByIds(sessionId.intValue(), pendingServiceIds, pcp);
	
	            log.trace("Pending Services for [" + entityId + "]: " + pendingServices.toString());
	        } else {
	            pendingServices = new PageList<AppdefResourceValue>();
	        }
	        request.setAttribute(Constants.PENDING_APPSVCS_REQ_ATTR, pendingServices);
	        request.setAttribute(Constants.NUM_PENDING_APPSVCS_REQ_ATTR, new Integer(pendingServices.getTotalSize()));
	
	        // Available Services
	        // Find all services that aren't already assigned to this
	        // application and that aren't in our pending list.
	
	        PageList<AppdefResourceValue> availableServices = appdefBoss.findAvailableServicesForApplication(sessionId
	            .intValue(), resource.getId(), pendingServiceIds, nameFilter, pca);
	
	        log.trace("Available Services for [" + entityId + "]: " + availableServices.toString());
	        request.setAttribute(Constants.AVAIL_APPSVCS_REQ_ATTR, availableServices);
	        request.setAttribute(Constants.NUM_AVAIL_APPSVCS_REQ_ATTR, new Integer(availableServices.getTotalSize()));
		} catch (Exception ex){
			log.error(ex);
		}

	}

}

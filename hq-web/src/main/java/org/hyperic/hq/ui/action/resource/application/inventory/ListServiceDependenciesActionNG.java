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

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppServiceNodeBean;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("listServiceDependenciesActionNG")   
public class ListServiceDependenciesActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory.getLog(ListServiceDependenciesActionNG.class);
	
	@Resource
    private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	
	        ListServiceDependenciesFormNG cform = new ListServiceDependenciesFormNG();
	        ApplicationValue app = (ApplicationValue) RequestUtils.getResource(request);
	        if (app == null) {
	            addActionError(getText("resource.application.error.inventory.ApplicationNotFound") );
	            return ;
	        }
	
	        Integer appId = app.getId();
	        String inpAppSvcId = request.getParameter("appSvcId");
	
	        Integer appSvcId = Integer.valueOf(inpAppSvcId);
	
	        Integer sessionId = RequestUtils.getSessionId(request);
	
	        List<AppdefResourceValue> services = appdefBoss.findServiceInventoryByApplication(sessionId.intValue(), appId,
	            PageControl.PAGE_ALL);
	
	        // get the dependency tree
	        DependencyTree tree = appdefBoss.getAppDependencyTree(sessionId.intValue(), app.getId());
	        DependencyNode appSvcNode = DependencyTree.findAppServiceById(tree, appSvcId);
	        // log.trace("Got dependency tree: " + appSvcNode);
	        // build a list of AppServiceNodeBean's that this AppService depends on
	        List<AppServiceNodeBean> dependeeList = DependencyTree.findDependees(tree, appSvcNode, services);
	        // build a list of AppServiceNodeBean's that depend on this AppService
	        List<AppServiceNodeBean> dependerList = DependencyTree.findDependers(tree, appSvcId, services);
	        request.setAttribute(Constants.APPSVC_CURRENT_ATTR, appSvcNode.getAppService());
	
	        PageControl pc = RequestUtils.getPageControl(request, "ps", "pn", "so", "sc");
	        Pager dependeePager = Pager.getDefaultPager();
	        PageList<AppServiceNodeBean> pagedDependeeList = dependeePager.seek(dependeeList, pc.getPagenum(), pc
	            .getPagesize());
	
	        request.setAttribute(Constants.APPSVC_DEPENDEES_ATTR, pagedDependeeList);
	        request.setAttribute(Constants.APPSVC_DEPENDERS_ATTR, dependerList);
	        request.setAttribute(Constants.NUM_APPSVC_DEPENDEES_ATTR, new Integer(dependeeList.size()));
	        request.setAttribute(Constants.NUM_APPSVC_DEPENDERS_ATTR, new Integer(dependerList.size()));
		} catch (Exception ex) {
			log.error(ex);
		}
		
	}

}

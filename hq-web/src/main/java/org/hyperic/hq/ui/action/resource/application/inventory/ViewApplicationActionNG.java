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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppServiceNodeBean;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.inventory.RemoveResourceGroupsFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.stereotype.Component;

@Component("viewApplicationActionNG")    
public class ViewApplicationActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory.getLog(ViewApplicationActionNG.class
			.getName());
	@Resource
	private AppdefBoss appdefBoss;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
		this.request = getServletRequest();
		
		ApplicationValue app = (ApplicationValue) RequestUtils
				.getResource(request);
		if (app == null) {
			addActionError(getText("resource.application.error.inventory.ApplicationNotFound"));
			return;
		}
		Integer appId = app.getId();
		Integer appdefType = new Integer(app.getEntityId().getType());

		Integer sessionId = RequestUtils.getSessionId(request);

		Pager valuePager = Pager.getDefaultPager();

		// display of the services includes:
		// - linking to the dependency screen for the AppService
		// - service name
		// - entry point flag (YES/NO)
		// - service type label
		// - host server

		// first, get what we need to populate the services list tile
		// this is a List of ServiceValue objects
		log.trace("getting services for appId " + appId);
		PageControl pcs = RequestUtils.getPageControl(request, "pss", "pns",
				"sos", "scs");
		PageControl pc = new PageControl(0, -1, 1, SortAttribute.SERVICE_NAME);
		PageList<AppdefResourceValue> services = appdefBoss
				.findServiceInventoryByApplication(sessionId.intValue(), appId,
						pc);

		log.debug("AppdefBoss returning " + services.toString());

		log.trace("getting service type map for application");
		Map<String, Integer> typeMap = AppdefResourceValue
				.getServiceTypeCountMap(services);
		request.setAttribute(Constants.RESOURCE_TYPE_MAP_ATTR, typeMap);

		services = valuePager.seek(services, pcs);
		// second, get the dependency tree
		DependencyTree tree = appdefBoss.getAppDependencyTree(
				sessionId.intValue(), app.getId());

		List<AppServiceNodeBean> asnbList = getAppServiceNodes(tree, services);

		request.setAttribute(Constants.SERVICES_ATTR, asnbList);

		// services aren't "genetically children" per se but we'll piggy pack
		// on these constants since they're sort of "adopted children" of an
		// application
		request.setAttribute(Constants.CHILD_RESOURCES_ATTR, asnbList);
		// second, populate the request attr for the "total" for the service
		// count tile
		request.setAttribute(Constants.NUM_CHILD_RESOURCES_ATTR, new Integer(
				services.getTotalSize()));
		// third, populate the request attrs for the service types for the
		// service count tile

		// forth, get the groups
		PageControl pcg = RequestUtils.getPageControl(request, "psg", "png",
				"sog", "scg");
		PageList<AppdefGroupValue> groups = appdefBoss
				.findAllGroupsMemberInclusive(sessionId.intValue(), pcg,
						app.getEntityId());

		request.setAttribute(Constants.ALL_RESGRPS_ATTR, groups);

		// create and initialize the remove resources form for services
		log.trace("getting all service types");
		List<ServiceTypeValue> serviceTypes = appdefBoss.findAllServiceTypes(
				sessionId.intValue(), PageControl.PAGE_ALL);

		// horrible pagination stuff
		RemoveAppServicesFormNG removeAppServicesForm = new RemoveAppServicesFormNG();
		removeAppServicesForm.setRid(appId);
		removeAppServicesForm.setType(appdefType);
		removeAppServicesForm.setResourceTypes(serviceTypes);
		int fs = RequestUtils.getPageSize(request, "fs");
		removeAppServicesForm.setF(new Integer(fs));

		int pss = RequestUtils.getPageSize(request, "pss");
		int pns = RequestUtils.getPageNum(request, "pns");

		removeAppServicesForm.setPss(new Integer(pss));
		removeAppServicesForm.setPns(new Integer(pns));

		request.setAttribute(Constants.RESOURCE_REMOVE_APPSERVICES_FORM_ATTR,
				removeAppServicesForm);

		// create and initialize the remove resource groups form
		RemoveResourceGroupsFormNG rmGroupsForm = new RemoveResourceGroupsFormNG();
		rmGroupsForm.setRid(appId);
		rmGroupsForm.setType(appdefType);
		int psg = RequestUtils.getPageSize(request, "psg");
		rmGroupsForm.setPs(new Integer(psg));

		request.setAttribute(
				Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR,
				rmGroupsForm);
		} catch (Exception ex) {
			log.error(ex);
		}
		

	}

	private List<AppServiceNodeBean> getAppServiceNodes(DependencyTree tree,
			List<AppdefResourceValue> services) {
		List<AppServiceNodeBean> returnList = new ArrayList<AppServiceNodeBean>();
		/*
		 * We need to iterate over the service list (not the tree). This is so
		 * as not to offend our API method which spent numerous CPU cycles
		 * painstakingly sorting and paging the data. Also, this code assumes we
		 * have ALL svcs when we only have 1 page. for (Iterator iter =
		 * tree.getNodes().iterator(); iter.hasNext();) { DependencyNode node =
		 * (DependencyNode) iter.next(); AppServiceValue appSvc =
		 * node.getAppService(); ServiceValue service =
		 * (ServiceValue)serviceMap.
		 * get(node.getAppService().getService().getId()); returnList.add(new
		 * AppServiceNodeBean(service,node)); }
		 */
		for (AppdefResourceValue resVo : services) {

			DependencyNode appSvcDepNode = tree.findAppService(resVo);
			returnList.add(new AppServiceNodeBean(resVo, appSvcDepNode));
		}
		return returnList;
	}

}

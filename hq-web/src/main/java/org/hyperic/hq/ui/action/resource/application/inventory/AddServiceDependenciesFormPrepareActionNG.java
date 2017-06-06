/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.stereotype.Component;

/**
 * To populate the "Add Dependencies" page (2.1.6.5), this class accesses a list
 * of services associated with the current application that are not ancestors of
 * the current service (obviously, we can't allow circular dependencies).
 * <p>
 * Available services are all services in the system that are associated with
 * the application (i.e. it's in the
 * {@link org.hyperic.hq.appdef.shared.DependencyTree}) but <br>
 * 1) the current service doesn't presently depend on it (it's ok if the current
 * service depends on it _indirectly_ i.e. via another dependency) and <br>
 * 2) the potential dependency doesn't depend on the current one
 * <p>
 */

@Component("addServiceDependenciesFormPrepareActionNG")   
public class AddServiceDependenciesFormPrepareActionNG extends BaseActionNG
		implements ViewPreparer {

    private final Log log = LogFactory.getLog(AddServiceDependenciesFormPrepareActionNG.class);

    @Resource
    private AppdefBoss appdefBoss;	
	
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	
	        AddApplicationServicesFormNG addForm = new AddApplicationServicesFormNG();
	        AppdefResourceValue resource = RequestUtils.getResource(request);
	        AppdefEntityID entityId = resource.getEntityId();
	        Integer sessionId = RequestUtils.getSessionId(request);
	        String appSvcIdStr = request.getParameter("appSvcId");
	        Integer appSvcId = Integer.valueOf(appSvcIdStr);
	        PageControl pca = RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
	        PageControl pcp = RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");
	
	        // pending services are those on the right side of the "add
	        // to list" widget- awaiting association with the resource
	        // when the form's "ok" button is clicked.
	        boolean servicesArePending = false;
	        if (request.getSession().getAttribute(Constants.PENDING_SVCDEPS_SES_ATTR) != null &&
	            ((List) request.getSession().getAttribute(Constants.PENDING_SVCDEPS_SES_ATTR)).size() > 0)
	            servicesArePending = true;
	
	        if (log.isTraceEnabled())
	            log.trace("getting AppServices for application [" + entityId + "]");
	        // get the dependency tree
	        DependencyTree tree = appdefBoss.getAppDependencyTree(sessionId.intValue(), resource.getId());
	        DependencyNode appSvcNode = DependencyTree.findAppServiceById(tree, appSvcId);
	        List<AppdefResourceValue> services = appdefBoss.findServiceInventoryByApplication(sessionId.intValue(),
	            resource.getId(), PageControl.PAGE_ALL);
	        Map<AppdefEntityID, AppdefResourceValue> serviceMap = DependencyTree.mapServices(services);
	        log.debug("Map contains " + serviceMap.toString());
	        List<DependencyNode> availableServices = new ArrayList<DependencyNode>();
	        List<DependencyNode> potentialDeps = DependencyTree.findPotentialDependees(tree, appSvcNode, services);
	        log.debug("The list contains " + potentialDeps);
	        for (DependencyNode candidateNode : potentialDeps) {
	            availableServices.add(candidateNode);
	        }
	
	        // filter out the pending ones, if there are any
	        if (servicesArePending) {
	
	            List<String> uiPendings = SessionUtils.getListAsListStr(request.getSession(),
	                Constants.PENDING_SVCDEPS_SES_ATTR);
	
	            AppdefEntityID[] pendingServiceIds = new AppdefEntityID[uiPendings.size()];
	
	            for (int i = 0; i < uiPendings.size(); i++) {
	                StringTokenizer tok = new StringTokenizer((String) uiPendings.get(i), " ");
	                if (tok.countTokens() > 1) {
	                    pendingServiceIds[i] = new AppdefEntityID(AppdefEntityConstants.stringToType(tok.nextToken()),
	                        Integer.parseInt(tok.nextToken()));
	                } else {
	                    pendingServiceIds[i] = new AppdefEntityID(tok.nextToken());
	                }
	            }
	
	            if (log.isTraceEnabled())
	                log.trace("getting pending services for application [" + entityId + "] that service [" + appSvcId +
	                          "] depends on");
	
	            List<AppdefEntityID> pendingServiceIdList = Arrays.asList(pendingServiceIds);
	            PageList<AppdefResourceValue> pendingServices = new PageList<AppdefResourceValue>();
	            for (int i = 0; i < pendingServiceIds.length; i++) {
	                pendingServices.add(serviceMap.get(pendingServiceIds[i]));
	            }
	
	            PageList<AppdefResourceValue> pagedPendingList = new PageList<AppdefResourceValue>();
	            Pager pendingPager = Pager.getDefaultPager();
	            pagedPendingList = pendingPager.seek(pendingServices, pcp.getPagenum(), pcp.getPagesize());
	            request.setAttribute(Constants.PENDING_SVCDEPS_REQ_ATTR,
	            /* pendingServices */pagedPendingList);
	            request.setAttribute(Constants.NUM_PENDING_SVCDEPS_REQ_ATTR, new Integer(pendingServices.size()));
	
	            // begin filtering
	
	            for (Iterator<DependencyNode> iter = availableServices.iterator(); iter.hasNext();) {
	                DependencyNode element = iter.next();
	                if (pendingServiceIdList.contains(element.getEntityId())) {
	                    iter.remove();
	                }
	            }
	            // end filtering
	        } else {
	            // nothing is pending, so we'll initialize the attributes
	            request.setAttribute(Constants.PENDING_SVCDEPS_REQ_ATTR, new ArrayList<AppdefResourceValue>());
	            request.setAttribute(Constants.NUM_PENDING_SVCDEPS_REQ_ATTR, new Integer(0));
	        }
	
	        // Sort list
	        Collections.sort(availableServices);
	
	        PageList<DependencyNode> pagedAvailabileList = new PageList<DependencyNode>();
	        Pager pendingPager = Pager.getDefaultPager();
	        pagedAvailabileList = pendingPager.seek(availableServices, pca.getPagenum(), pca.getPagesize());
	        request.setAttribute(Constants.AVAIL_SVCDEPS_REQ_ATTR,
	        /* availableServices */pagedAvailabileList);
	        request.setAttribute(Constants.NUM_AVAIL_SVCDEPS_REQ_ATTR, new Integer(availableServices.size()));	
		} catch (Exception ex) {
			log.error(ex);
		}
		
	}

}

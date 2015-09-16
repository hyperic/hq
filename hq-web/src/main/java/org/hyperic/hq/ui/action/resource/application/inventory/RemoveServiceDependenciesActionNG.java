/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.action.resource.RemoveResourceFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * On screen 2.1.6.4, a user can select one or more checkboxes (
 * <code>resources</code>), for removal. Handling that action requires rewriting
 * the {@link org.hyperic.hq.appdef.shared.DependencyTree} and saving it.
 */

@Component("removeServiceDependenciesActionNG")
@Scope("prototype")
public class RemoveServiceDependenciesActionNG extends BaseActionNG implements ModelDriven<RemoveResourceFormNG> {

    private final Log log = LogFactory.getLog(RemoveServiceDependenciesActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    
    private RemoveResourceFormNG cform = new RemoveResourceFormNG();
	
	private String internalEid;
	private Integer internalAppSvcId;
	
	@SkipValidation
	public String execute() throws Exception {

		Integer resourceId = cform.getRid();
		Integer entityType = cform.getType();
		Integer[] resources = cform.getResources();
		Integer appSvcId = RequestUtils.getIntParameter(request, "appSvcId");
		internalAppSvcId = appSvcId;
		internalEid = entityType  + ":" + resourceId;
		
		request.setAttribute(Constants.RESOURCE_PARAM, resourceId);
		request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
		request.setAttribute("appSvcId", appSvcId);

		Integer sessionId = RequestUtils.getSessionId(request);

		try {
			DependencyTree tree = appdefBoss.getAppDependencyTree(
					sessionId.intValue(), resourceId);
			log.debug("got tree " + tree);
			// walk through the nodes to find the ones that are
			// to be removed as dependees

			DependencyNode depNode = DependencyTree.findAppServiceById(tree,
					appSvcId);
			log.debug("will remove selected children from node " + depNode);
			List<AppService> children = depNode.getChildren();
			List<AppService> toRemove = new ArrayList<AppService>();
			for (int i = 0; i < resources.length; i++) {
				for (AppService asv : children) {

					if (resources[i].equals(asv.getId())) {
						// remove this one
						toRemove.add(asv);
					}
				}
			}
			for (AppService asv : toRemove) {

				depNode.removeChild(asv);
			}

			log.debug("saving tree " + tree);
			appdefBoss.setAppDependencyTree(sessionId.intValue(), tree);
			DependencyTree savedTree = appdefBoss.getAppDependencyTree(
					sessionId.intValue(), tree.getApplication().getId());
			log.debug("retrieving saved tree " + savedTree);
		} catch (PermissionException e) {
			log.debug("removing services from application failed:", e);
			throw new ServletException(
					"can't remove services from application", e);
		} catch (ApplicationException e) {
			log.debug("removing services from application failed:", e);
			throw new ServletException(
					"can't remove services from application", e);
		}

		return SUCCESS;
	}	
	
	public RemoveResourceFormNG getModel() {
		return cform;
	}

	public RemoveResourceFormNG getPForm() {
		return cform;
	}

	public void setPForm(RemoveResourceFormNG cform) {
		this.cform = cform;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
	
	public Integer getInternalAppSvcId() {
		return internalAppSvcId;
	}


	public void setInternalAppSvcId(Integer internalAppSvcId) {
		this.internalAppSvcId = internalAppSvcId;
	}
}

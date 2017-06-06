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

/**
 * When the list of pending service dependencies on the Add Dependencies page
 * (2.1.6.5) is grown, shrunk or committed (by selecting from the checkbox lists
 * and clicking add, remove or ok) this class manages the pending list and
 * commitment.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("addServiceDependenciesActionNG")
@Scope("prototype")
public class AddServiceDependenciesActionNG extends BaseActionNG implements ModelDriven<AddApplicationServicesFormNG>{

    private final Log log = LogFactory.getLog(AddServiceDependenciesActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    
    private AddApplicationServicesFormNG addForm = new AddApplicationServicesFormNG();
    
	private Integer internalRid;
	private Integer internalType;
	private String internalEid;
	private Integer internalAppSvcId;
	
	public String execute() throws Exception {
		HttpSession session = request.getSession();

		Integer resourceId = addForm.getRid();
		Integer entityType = addForm.getType();
		Integer appSvcId = addForm.getAppSvcId();
        internalRid = addForm.getRid() ;
        internalType = addForm.getType().intValue();
        internalEid = entityType  + ":" + resourceId;
        internalAppSvcId = appSvcId;

		request.setAttribute(Constants.RESOURCE_PARAM, resourceId);
		request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
		request.setAttribute("appSvcId", appSvcId);

        String forward = checkSubmit(addForm);
        if (forward != null) {
            
            if (forward.equalsIgnoreCase("CANCEL") || forward.equalsIgnoreCase("CANCEL")) {
                log.trace("cancel action - remove pending group list");
                SessionUtils.removeList(session, Constants.PENDING_SVCDEPS_SES_ATTR);
            } else if (forward.equalsIgnoreCase("ADDED")) {
                log.trace("adding to pending group list");
                if (addForm.getAvailableServices() != null ) {
                	SessionUtils.addToList(session, Constants.PENDING_SVCDEPS_SES_ATTR, addForm.getAvailableServices());
                }
            } else if (forward.equalsIgnoreCase("REMOVED")) {
                log.trace("removing from pending group list");
                if (addForm.getPendingServices() != null ) {
                	SessionUtils.removeFromList(session, Constants.PENDING_SVCDEPS_SES_ATTR, addForm.getPendingServices());
                }
            }
            
            return forward;
        }

		Integer sessionId = RequestUtils.getSessionId(request);

		log.trace("getting pending service list");
		List<String> uiPendings = SessionUtils.getListAsListStr(session,
				Constants.PENDING_SVCDEPS_SES_ATTR);
		List<AppdefEntityID> pendingServiceIdList = new ArrayList<AppdefEntityID>();

		for (int i = 0; i < uiPendings.size(); i++) {
			StringTokenizer tok = new StringTokenizer(
					(String) uiPendings.get(i), " ");
			if (tok.countTokens() > 1) {
				pendingServiceIdList.add(new AppdefEntityID(
						AppdefEntityConstants.stringToType(tok.nextToken()),
						Integer.parseInt(tok.nextToken())));
			} else {
				pendingServiceIdList.add(new AppdefEntityID(tok.nextToken()));
			}
		}

		DependencyTree tree = appdefBoss.getAppDependencyTree(
				sessionId.intValue(), resourceId);

		Map<Integer, AppServiceValue> depNodeChildren = new HashMap<Integer, AppServiceValue>();
		DependencyNode depNode = DependencyTree.findAppServiceById(tree,
				appSvcId);
		// TODO this looks suspicious. Seems like depNode.getChildren returns
		// list of AppService, not AppServiceValue
		for (Iterator iter = depNode.getChildren().iterator(); iter.hasNext();) {
			AppServiceValue anAppSvc = (AppServiceValue) iter.next();
			if (anAppSvc.getIsCluster())
				depNodeChildren.put(anAppSvc.getServiceCluster().getGroupId(),
						anAppSvc);
			else
				depNodeChildren.put(anAppSvc.getService().getId(), anAppSvc);
		}

		if (log.isTraceEnabled())
			log.trace("adding servicess " + uiPendings.toString()
					+ " for application [" + resourceId + "]");

		// look through the tree's DependencyNodes to find the ones
		// we have pending (identified by their service ids)
		for (DependencyNode node : tree.getNodes()) {

			AppdefEntityID lookFor;

			if (node.isCluster())
				lookFor = AppdefEntityID.newGroupID(node.getAppService()
						.getResourceGroup().getId());
			else
				lookFor = node.getAppService().getService().getEntityId();
			if (pendingServiceIdList.contains(lookFor)
					&& !depNodeChildren.containsKey(lookFor)) {
				depNode.addChild(node.getAppService());
			}
		}
		log.trace("Saving tree: " + tree);
		appdefBoss.setAppDependencyTree(sessionId.intValue(), tree);
		// XXX remember to kill this, this is just to demonstrate for Javier
		DependencyTree savedTree = appdefBoss.getAppDependencyTree(
				sessionId.intValue(), tree.getApplication().getId());
		log.trace("Saved tree: " + savedTree);

		log.trace("removing pending service list");
		SessionUtils.removeList(session, Constants.PENDING_SVCDEPS_SES_ATTR);

		addActionMessage(getText("resource.application.inventory.confirm.") + " AddedServices");
		return SUCCESS;
		
	}
	
	
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		HttpSession session = request.getSession();
		SessionUtils.removeList(session, Constants.PENDING_APPSVCS_SES_ATTR);
		 AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(), addForm.getRid());
		if (aeid!= null) {
			internalEid = aeid.toString();
			internalAppSvcId = addForm.getAppSvcId();
		}
		
		this.removeValueInSession(Constants.PENDING_SVCDEPS_SES_ATTR);
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		addForm.reset();
		clearErrorsAndMessages();
		 AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(), addForm.getRid());
		if (aeid!= null) {
			setEntityRequestParams(aeid);
			internalAppSvcId = addForm.getAppSvcId();
		}
		return "reset";
	}

	
	public AddApplicationServicesFormNG getModel() {
		return addForm;
	}
	
    public AddApplicationServicesFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddApplicationServicesFormNG addForm) {
		this.addForm = addForm;
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

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId();
		this.internalType = eid.getType();
	}


	public Integer getInternalAppSvcId() {
		return internalAppSvcId;
	}


	public void setInternalAppSvcId(Integer internalAppSvcId) {
		this.internalAppSvcId = internalAppSvcId;
	}

}

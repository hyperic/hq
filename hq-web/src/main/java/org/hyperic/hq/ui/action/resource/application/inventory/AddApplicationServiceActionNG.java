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
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("addApplicationServiceActionNG")
@Scope("prototype")
public class AddApplicationServiceActionNG extends BaseActionNG implements ModelDriven<AddApplicationServicesFormNG> {
    private final Log log = LogFactory.getLog(AddApplicationServiceActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    
    private AddApplicationServicesFormNG addForm = new AddApplicationServicesFormNG();
	
	private Integer internalRid;
	private Integer internalType;
	private String internalEid;
	
	private String nameFilter;
	
	public String execute() throws Exception {
        AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(), addForm.getRid());
        internalRid = addForm.getRid() ;
        internalType = addForm.getType().intValue();
        internalEid = aeid.toString();
        nameFilter = addForm.getNameFilter();
        
        request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
        request.setAttribute(Constants.ACCORDION_PARAM, "3");
        HttpSession session = request.getSession();


        String forward = checkSubmit(addForm);
        if (forward != null) {
            
            if (forward.equalsIgnoreCase("CANCEL") || forward.equalsIgnoreCase("CANCEL")) {
                log.trace("cancel action - remove pending group list");
                SessionUtils.removeList(session, Constants.PENDING_APPSVCS_SES_ATTR);
            } else if (forward.equalsIgnoreCase("ADDED")) {
                log.trace("adding to pending group list");
                if (addForm.getAvailableServices() != null ) {
                	SessionUtils.addToList(session, Constants.PENDING_APPSVCS_SES_ATTR, addForm.getAvailableServices());
                }
            } else if (forward.equalsIgnoreCase("REMOVED")) {
                log.trace("removing from pending group list");
                if (addForm.getPendingServices() != null ) {
                	SessionUtils.removeFromList(session, Constants.PENDING_APPSVCS_SES_ATTR, addForm.getPendingServices());
                }
            }
            
            return forward;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("getting pending service list");
        List<String> uiPendings = SessionUtils.getListAsListStr(session, Constants.PENDING_APPSVCS_SES_ATTR);
        List<AppdefEntityID> svcList = new ArrayList<AppdefEntityID>();

        for (int pRcs = 0; pRcs < uiPendings.size(); pRcs++) {
            log.debug("uiPendings = " + uiPendings.get(pRcs));
            StringTokenizer tok = new StringTokenizer(uiPendings.get(pRcs), " ");
            svcList.add(new AppdefEntityID(tok.nextToken()));
        }
        // when we call boss.setApplicationServices(...) our map must
        // be populated with all of the existing services (and whether
        // or not they're entry points) as well as our new ones

        // first, get the existing ones
        PageControl nullPc = new PageControl(-1, -1);
        List<AppdefResourceValue> existingServices = appdefBoss.findServiceInventoryByApplication(sessionId.intValue(),
            aeid.getId(), nullPc);
        DependencyTree tree = appdefBoss.getAppDependencyTree(sessionId.intValue(), aeid.getId());
        for (AppdefResourceValue service : existingServices) {

            log.debug("service =" + service.getClass().getName());

            tree.findAppService(service);
            svcList.add(service.getEntityId());
        }

        // second, get the new ones

        // set all added services to be entry points, if they're not to be
        // entry points and are instead part of a dependency chain,
        // setting up the dependencies is a separate activity

        log.trace("adding servicess " + svcList + " for application [" + aeid.getID() + "]");
        appdefBoss.setApplicationServices(sessionId.intValue(), aeid.getId(), svcList);
        log.trace("removing pending service list");
        SessionUtils.removeList(session, Constants.PENDING_APPSVCS_SES_ATTR);

        addActionMessage(getText( "resource.application.inventory.confirm.AddedServices") );
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
		}
		
		this.removeValueInSession("resourceParentGroupsEid");
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
			nameFilter = addForm.getNameFilter();
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

	public String getNameFilter() {
		return nameFilter;
	}

	public void setNameFilter(String nameFilter) {
		this.nameFilter = nameFilter;
	}
}

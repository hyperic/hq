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

package org.hyperic.hq.ui.action.resource.platform.inventory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.platform.PlatformFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * A <code>BaseAction</code> subclass that creates a platform in the BizApp.
 */

@Component("newPlatformActionNG")
@Scope("prototype")
public class NewPlatformActionNG extends BaseActionNG  implements ModelDriven<PlatformFormNG>{
	
    private final Log log = LogFactory.getLog(NewPlatformActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    @Resource
    private AIBoss aiBoss;
    
    private PlatformFormNG resourceForm= new PlatformFormNG();

	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
	
	public String save() throws Exception {
		try {
    		String forward = checkSubmit(resourceForm);
    		if (forward != null) {
				return forward;
    		}
    		
			Integer sessionId = RequestUtils.getSessionId(request);

			// first make sure the form's "machine type" represents a
			// valid platform type
			Integer platformTypeId = resourceForm.getResourceType();
			log.trace("finding platform type [" + platformTypeId + "]");
			PlatformType platformType = appdefBoss.findPlatformTypeById(
					sessionId.intValue(), platformTypeId);

			// now set up the new platform
			PlatformValue platform = new PlatformValue();
			resourceForm.updatePlatformValue(platform);
			platform.setCpuCount(new Integer(1)); // at least

			log.trace("creating platform [" + platform.getName() + "]"
					+ " with attributes " + resourceForm);

			Agent agent = BizappUtilsNG.getAgentConnectionNG(sessionId.intValue(),	appdefBoss, request, resourceForm);

			Platform newPlatform = appdefBoss.createPlatform(
					sessionId.intValue(), platform, platformType.getId(),
					agent.getId());

			Integer platformId = newPlatform.getId();
			resourceForm.setRid(platformId);

			AppdefEntityID entityId = newPlatform.getEntityId();

			ServletContext ctx = ServletActionContext.getServletContext();
			BizappUtilsNG.startAutoScan(ctx, sessionId.intValue(), entityId, aiBoss);

			Integer entityType = new Integer(newPlatform.getEntityId()
					.getType());
			resourceForm.setType(entityType);

			addActionMessage(getText( "resource.platform.inventory.confirm.Create", new String[] { platform.getName() } ) );


			request.setAttribute(Constants.RESOURCE_PARAM, platformId);
			request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, entityType);
			setEntityRequestParams(newPlatform.getEntityId());

			return SUCCESS;
		} catch (AppdefDuplicateNameException e1) {
			log.error(e1, e1);
			addActionError(getText("resource.platform.inventory.error.DuplicateName"));
			return INPUT;
		} catch (AppdefDuplicateFQDNException e1) {
			log.error(e1, e1);
			addActionError(getText("resource.platform.inventory.error.DuplicateFQDN"));
			return INPUT;
		} catch (ApplicationException e) {
			log.error(e, e);
			addActionError(getText("dash.autoDiscovery.import.Error",
					new String[] { e.getMessage() }));
			return INPUT;
		}

	}
	
    public void validate() {
    	try { 
			String forward = checkSubmit(resourceForm);
			if (forward == null) {
				this.validateInformation();
				String[] addressesToValidate = resourceForm.getAddresses();
				this.validateAddress(addressesToValidate);
			}
    	} catch (Exception ex) {
    		log.error("validation failed", ex);
    	}
    }
	
    @SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		resourceForm.reset();
		clearErrorsAndMessages();
		return "reset";
	}
	


	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
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

	
	private void setEntityRequestParams (AppdefEntityID eid) {
		this.internalEid = eid.toString();
		this.internalRid = eid.getId();
		this.internalType = eid.getType();
	}

	public PlatformFormNG getModel() {
		return resourceForm;
	}

	public PlatformFormNG getResourceForm() {
		return resourceForm;
	}

	public void setResourceForm(PlatformFormNG resourceForm) {
		this.resourceForm = resourceForm;
	}
	
	public PlatformFormNG getEditForm() {
		return resourceForm;
	}

	private void validateInformation () {
		String valName = this.resourceForm.getName();
		if ( valName == null || valName.equals("")) {
			this.addFieldError("name",getText("resource.common.inventory.error.ResourceNameIsRequired"));
		} else {
			if ( valName.length() > 100) {
				this.addFieldError("name",getText("ng.errors.maxlength", new String [] { "100" }));
			}
		}
		
		valName = this.resourceForm.getFqdn();
		if ( valName == null || valName.equals("")) {
			this.addFieldError("fqdn",getText("resource.platform.inventory.error.FQDNIsRequired"));
		} 
		
		valName = this.resourceForm.getDescription();
		if ( valName != null && valName.length() > 100) {
			this.addFieldError("description",getText("ng.errors.maxlength", new String [] { "100"  }));
		} 
		
		Integer selResourceType = this.resourceForm.getResourceType();
		if ( selResourceType != null && selResourceType == -1) {
			this.addFieldError("resourceType",getText("resource.platform.inventory.error.MachineTypeIsRequired"));
		} 
		
	}

	private void validateAddress (String[] addressesToValidate) {
		for (int i = 0; i < addressesToValidate.length; i++) {
			if (addressesToValidate[i] == null
					|| addressesToValidate[i].isEmpty()) {
				this.addFieldError(
						"addresses[" + i + "]",
						getText("resource.platform.inventory.error.IpAddressIsRequired"));
				// add error attribute to the request and use it to set td class in jsp
				request.setAttribute("e","error");
			} else {
				Pattern p = Pattern
						.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");
				Matcher m = p.matcher(addressesToValidate[i]);
				if (!m.find()) {
					this.addFieldError(
							"addresses[" + i + "]",
							getText("resource.platform.inventory.error.IpAddressInvalid"));
				}
			}
		}
	}
	
    private void resetFormIps() {
        IpValue[] ips = new IpValue[1];
        ips[0] = new IpValue();
        // automatically sets numIps
        this.getResourceForm().setIps(ips);
    }
	

}

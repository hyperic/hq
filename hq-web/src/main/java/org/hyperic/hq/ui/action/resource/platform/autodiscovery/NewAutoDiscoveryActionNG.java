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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.InvalidOptionValsFoundException;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Action class which saves an auto-discovery. The autodiscovery can be an
 * new/edit auto-discovery.
 * 
 */

@Component("newAutoDiscoveryActionNG")
@Scope("prototype")
public class NewAutoDiscoveryActionNG extends BaseActionNG implements ModelDriven<PlatformAutoDiscoveryFormNG>{

    public final static long TIMEOUT = 5000;
    public final static long INTERVAL = 500;

    private final Log log = LogFactory.getLog(NewAutoDiscoveryActionNG.class);
    @Resource
    private AppdefBoss appdefBoss;
    @Resource
    private AIBoss aiBoss;
    
    private PlatformAutoDiscoveryFormNG newForm= new PlatformAutoDiscoveryFormNG();

	private String internalEid;
	private Integer internalRid;
	private Integer internalType;
	
    
	public String save() throws Exception {
		List<Pair<String,String>> errors = new ArrayList<Pair<String,String>>();
		try {
			
            Integer platformId = newForm.getRid();
            if (platformId == null) {
                addActionError(getText( Constants.ERR_PLATFORM_NOT_FOUND) );
                return INPUT;
            }
            Integer platformType = newForm.getType();

            request.setAttribute(Constants.RESOURCE_PARAM, platformId);
            request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, platformType);
            setEntityRequestParams(new AppdefEntityID (platformType + ":" + platformId));

    		String forward = checkSubmit(newForm);
    		if (forward != null) {
    			return forward;
    		}

            int sessionId = RequestUtils.getSessionIdInt(request);

            PlatformValue pValue = appdefBoss.findPlatformById(sessionId, platformId);
            buildAutoDiscoveryScan(request, newForm, pValue,errors);

            addActionMessage( "resource.platform.inventory.autoinventory.status.NewScan" );

            // See if there is an existing report

            try {
                AIPlatformValue aip = aiBoss.findAIPlatformByPlatformID(sessionId, platformId.intValue());
                request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
                
            } catch (PlatformNotFoundException e) {
                // Don't worry about it then
            }
            
            addActionMessage(getText("resource.platform.inventory.autoinventory.status.NewScan") );
            return SUCCESS;
        } catch (AgentConnectionException e) {
            addActionError(getText( "resource.platform.inventory.configProps.NoAgentConnection") );
            return INPUT;
        } catch (AgentNotFoundException e) {
            addActionError(getText( "resource.platform.inventory.configProps.NoAgentConnection" ) );
            return INPUT;
        } catch (InvalidOptionValsFoundException e) {
        	for (Pair <String,String> error : errors) {
        		addActionError( getText ( error.getLeft() , new String [] { error.getRight() }) );
        	}
            return INPUT;
        } catch (DuplicateObjectException e1) {
            addActionError(getText( Constants.ERR_DUP_RESOURCE_FOUND ) );
            return INPUT;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return INPUT;
		}

	}
    
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			internalEid = aeid.toString();
		}
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		newForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
		if (aeid!= null) {
			setEntityRequestParams(aeid);
		}
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
	
	
	
    public PlatformAutoDiscoveryFormNG getModel() {
		return newForm;
	}


	public PlatformAutoDiscoveryFormNG getNewForm() {
		return newForm;
	}

	public void setNewForm(PlatformAutoDiscoveryFormNG newForm) {
		this.newForm = newForm;
	}
	
	private void buildAutoDiscoveryScan(HttpServletRequest request, PlatformAutoDiscoveryFormNG newForm,
			PlatformValue pValue, List<Pair<String,String>> errors ) throws Exception {

		int sessionId = RequestUtils.getSessionIdInt(request);

		// update the ScanConfiguration from the form obect
		List<ServerTypeValue> stValues = appdefBoss.findServerTypesByPlatformType(sessionId, pValue.getPlatformType()
				.getId(), PageControl.PAGE_ALL);
		ServerTypeValue[] stArray = stValues.toArray(new ServerTypeValue[0]);

		Map<String, ServerSignature> serverDetectors = aiBoss.getServerSignatures(sessionId,
				newForm.getSelectedServerTypes(stArray));

		ServerSignature[] serverDetectorArray = new ServerSignature[serverDetectors.size()];
		serverDetectors.values().toArray(serverDetectorArray);

		String ptName = pValue.getPlatformType().getName();
		ScanMethod scanMethod = NewAutoDiscoveryPrepActionNG.getScanMethod(ptName);
		ScanConfiguration scanConfig = new ScanConfiguration();
		ConfigResponse oldCr = NewAutoDiscoveryPrepActionNG.getConfigResponse(ptName);
		ConfigResponse cr = BizappUtilsNG.buildSaveConfigOptionsNG(request, oldCr, scanMethod.getConfigSchema(), errors);

		// Only setup the FileScan if server types were actually selected
		if (serverDetectorArray.length > 0) {
			scanConfig.setScanMethodConfig(scanMethod, cr);
		}
		scanConfig.setServerSignatures(serverDetectorArray);

		aiBoss.startScan(sessionId, pValue.getId().intValue(), scanConfig.getCore(), null, null, /*
																								 * No
																								 * scanName
																								 * or
																								 * scanDesc
																								 * for
																								 * immediate
																								 * ,
																								 * one
																								 * -
																								 * time
																								 * scans
																								 */
				null);

		waitForScanStart(sessionId, aiBoss, pValue.getId().intValue());
	}

	private void waitForScanStart(int sessionId, AIBoss boss, int platformId) throws Exception {
		Thread.sleep(2000);
	}
	
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
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
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.InvalidOptionValsFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action class which saves an auto-discovery. The autodiscovery can be an
 * new/edit auto-discovery.
 * 
 */
public class NewAutoDiscoveryAction
    extends BaseAction {

    public final static long TIMEOUT = 5000;
    public final static long INTERVAL = 500;

    private AppdefBoss appdefBoss;
    private AIBoss aiBoss;

    @Autowired
    public NewAutoDiscoveryAction(AppdefBoss appdefBoss, AIBoss aiBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.aiBoss = aiBoss;
    }

    /**
     * Create a new auto-discovery with the attributes specified in the given
     * <code>AutoDiscoveryForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        ActionErrors errors = new ActionErrors();
        try {
            PlatformAutoDiscoveryForm newForm = (PlatformAutoDiscoveryForm) form;

            Integer platformId = newForm.getRid();
            if (platformId == null) {
                RequestUtils.setError(request, Constants.ERR_PLATFORM_NOT_FOUND);
                return returnFailure(request, mapping);
            }
            Integer platformType = newForm.getType();

            HashMap<String, Object> forwardParams = new HashMap<String, Object>(3);
            forwardParams.put(Constants.RESOURCE_PARAM, platformId);
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, platformType);

            ActionForward forward = checkSubmit(request, mapping, form, forwardParams, YES_RETURN_PATH);

            if (forward != null) {
                return forward;
            }

            int sessionId = RequestUtils.getSessionIdInt(request);

            PlatformValue pValue = appdefBoss.findPlatformById(sessionId, platformId);
            buildAutoDiscoveryScan(request, newForm, pValue, errors);

            RequestUtils.setConfirmation(request, "resource.platform.inventory.autoinventory.status.NewScan");

            // See if there is an existing report

            try {
                AIPlatformValue aip = aiBoss.findAIPlatformByPlatformID(sessionId, platformId.intValue());
                request.setAttribute(Constants.AIPLATFORM_ATTR, aip);
            } catch (PlatformNotFoundException e) {
                // Don't worry about it then
            }

            return returnNew(request, mapping, forwardParams);
        } catch (AgentConnectionException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return returnFailure(request, mapping);
        } catch (AgentNotFoundException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return returnFailure(request, mapping);
        } catch (InvalidOptionValsFoundException e) {
            RequestUtils.setErrors(request, errors);
            return returnFailure(request, mapping);

        } catch (DuplicateObjectException e1) {
            RequestUtils.setError(request, Constants.ERR_DUP_RESOURCE_FOUND);
            return returnFailure(request, mapping);
        }
    }

    /**
     * Return an <code>ActionForward</code> if the form has been cancelled or
     * reset; otherwise return <code>null</code> so that the subclass can
     * continue to execute.
     */
    protected ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form,
                                        Map<String, Object> params, boolean doReturnPath) throws Exception {
        PlatformAutoDiscoveryForm aiForm = (PlatformAutoDiscoveryForm) form;

        if (aiForm.isScheduleTypeChgSelected()) {
            return returnScheduleTypeChg(request, mapping, params, false);
        }

        return super.checkSubmit(request, mapping, form, params, doReturnPath);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>cancel</em>
     * form gesture.
     */
    private ActionForward returnScheduleTypeChg(HttpServletRequest request, ActionMapping mapping,
                                                Map<String, Object> params, boolean doReturnPath) throws Exception {
        return constructForward(request, mapping, Constants.SCHEDULE_TYPE_CHG_URL, params, doReturnPath);
    }

    private void buildAutoDiscoveryScan(HttpServletRequest request, PlatformAutoDiscoveryForm newForm,
                                        PlatformValue pValue, ActionErrors errors) throws Exception {

        int sessionId = RequestUtils.getSessionIdInt(request);

        // update the ScanConfiguration from the form obect
        List<ServerTypeValue> stValues = appdefBoss.findServerTypesByPlatformType(sessionId, pValue.getPlatformType()
            .getId(), PageControl.PAGE_ALL);
        ServerTypeValue[] stArray = stValues.toArray(new ServerTypeValue[0]);

        Map<String, ServerSignature> serverDetectors = aiBoss.getServerSignatures(sessionId, newForm
            .getSelectedServerTypes(stArray));

        ServerSignature[] serverDetectorArray = new ServerSignature[serverDetectors.size()];
        serverDetectors.values().toArray(serverDetectorArray);

        String ptName = pValue.getPlatformType().getName();
        ScanMethod scanMethod = NewAutoDiscoveryPrepAction.getScanMethod(ptName);
        ScanConfiguration scanConfig = new ScanConfiguration();
        ConfigResponse oldCr = NewAutoDiscoveryPrepAction.getConfigResponse(ptName);
        ConfigResponse cr = BizappUtils.buildSaveConfigOptions(request, oldCr, scanMethod.getConfigSchema(), errors);

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

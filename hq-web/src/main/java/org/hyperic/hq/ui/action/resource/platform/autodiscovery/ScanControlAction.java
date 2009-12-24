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

/*
 * Created on Mar 10, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * action for stopping the auto-discovery scan. This action is only enabled when
 * the auto-discovery scan is running.
 * 
 * 
 */
public class ScanControlAction
    extends BaseAction {

    private AIBoss aiBoss;

    @Autowired
    public ScanControlAction(AIBoss aiBoss) {
        super();
        this.aiBoss = aiBoss;
    }

    /**
     * Create a new auto-discovery with the attributes specified in the given
     * <code>ScanControlForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        try {
            ResourceForm newForm = (ResourceForm) form;

            Integer resourceId = newForm.getRid();
            Integer resourceType = newForm.getType();

            HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
            forwardParams.put(Constants.RESOURCE_PARAM, resourceId);
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, resourceType);

            controlScan(request, resourceId);

            return returnSuccess(request, mapping, forwardParams);

        } catch (AgentConnectionException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return returnFailure(request, mapping);
        } catch (AgentNotFoundException e) {
            RequestUtils.setError(request, "resource.platform.inventory.configProps.NoAgentConnection");
            return returnFailure(request, mapping);
        }
    }

    /**
     * control the auto-inventory scan
     */
    private void controlScan(HttpServletRequest request, Integer resourceId) throws Exception {

        Integer sessionId = RequestUtils.getSessionId(request);

        ScanStateCore scanStateCore = aiBoss.getScanStatus(sessionId.intValue(), resourceId.intValue());
        ScanState scanState = new ScanState(scanStateCore);

        // stop the scan if the scan is still running.
        if (!scanState.getIsInterrupted() && !scanState.getIsDone()) {
            aiBoss.stopScan(sessionId.intValue(), resourceId.intValue());
        }
    }

}

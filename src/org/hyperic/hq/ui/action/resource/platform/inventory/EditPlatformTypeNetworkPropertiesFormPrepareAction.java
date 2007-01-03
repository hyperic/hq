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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.platform.PlatformForm;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * An Action that retrieves data from the BizApp to facilitate display
 * of the form for editing a platform's type and network properties.
 */
public class EditPlatformTypeNetworkPropertiesFormPrepareAction
    extends TilesAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve the data necessary to display the
     * <code>TypeNetworkPropertiesForm</code> page.
     *
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log = LogFactory
            .getLog(EditPlatformTypeNetworkPropertiesFormPrepareAction
                    .class.getName());
        
        PlatformValue platform =
            (PlatformValue) RequestUtils.getResource(request);
        if (platform == null) {
            RequestUtils.setError(request, Constants.ERR_PLATFORM_NOT_FOUND);
            return null;
        }

        PlatformForm editForm = (PlatformForm) form;
        editForm.loadPlatformValue(platform);

        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);

        log.trace("getting all platform types");
        List platformTypes = boss.findAllPlatformTypes(sessionId.intValue(),
                                                       PageControl.PAGE_ALL);
        editForm.setResourceTypes(platformTypes);

        String usedIpPort = "";
        if (platform.getAgent() != null) { 
            usedIpPort =
                platform.getAgent().getAddress() +
                ":" + 
                platform.getAgent().getPort();
        }
        BizappUtils.populateAgentConnections(sessionId.intValue(),
                                             boss, request,
                                             editForm, usedIpPort);

        // respond to an add or remove click- we do this here
        // rather than in the edit Action because in between
        // these two actions, struts repopulates the form bean and
        // resets numIps to whatever value was submitted in the
        // request.

        if (editForm.isAddClicked()) {
            IpValue[] savedIps = platform.getIpValues();
            int numSavedIps = savedIps != null ? savedIps.length : 0;

            int nextIndex = editForm.getNumIps();
            log.trace("ips next index: " + nextIndex);
            for (int i=0; i<nextIndex+1; i++) {
                IpValue oldIp = i < numSavedIps ? savedIps[i] : null;
                if (oldIp == null) {
                    editForm.setIp(i, new IpValue());
                }
            }

            editForm.setNumIps(nextIndex + 1);
            log.trace("add num ips: " + editForm.getNumIps());
        }
        else if (editForm.isRemoveClicked()) {
            int ri = Integer.parseInt(editForm.getRemove().getX());

            IpValue oldIps[] = editForm.getIps();
            if (oldIps != null) {
                // remove the indicated ip, leaving all others
                // intact
                ArrayList oldIpsList =
                    new ArrayList(Arrays.asList(oldIps));
                oldIpsList.remove(ri);
                IpValue[] newIps =
                    (IpValue[]) oldIpsList.toArray(new IpValue[0]);

                // automatically sets numIps
                editForm.setIps(newIps);
            }
        }
        else if (! editForm.isOkClicked()) {
            // the form is being set up for the first time.
            IpValue[] savedIps = platform.getIpValues();
            int numSavedIps = savedIps != null ? savedIps.length : 0;

            for (int i=0; i<numSavedIps; i++) {
                editForm.setIp(i, savedIps[i]);
            }

            editForm.setNumIps(numSavedIps);
        }

        // the OSType dropdown is NOT editable in edit mode hence the false
        request.setAttribute(Constants.PLATFORM_OS_EDITABLE, Boolean.FALSE); 
        return null;

    }
}

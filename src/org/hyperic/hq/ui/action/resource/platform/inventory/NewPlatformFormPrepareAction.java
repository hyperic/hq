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
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.product.PlatformDetector;
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
 * of the form for creating a platform.
 */
public class NewPlatformFormPrepareAction extends TilesAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data necessary to display the
     * <code>NewPlatformForm</code>.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Log log =
            LogFactory.getLog(NewPlatformFormPrepareAction.class.getName());

        PlatformForm newForm = (PlatformForm) form;

        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);

        List resourceTypes = new ArrayList();
        List platformTypes = boss.findAllPlatformTypes(sessionId.intValue(),
                                                       PageControl.PAGE_ALL);
        //XXX need a finder for device types, this will do for the moment.
        for (Iterator it=platformTypes.iterator(); it.hasNext();) {
            PlatformTypeValue pType = (PlatformTypeValue)it.next(); 
            if (PlatformDetector.isSupportedPlatform(pType.getName())) {
                continue;
            }
            resourceTypes.add(pType);
        }
        newForm.setResourceTypes(resourceTypes);

        BizappUtils.populateAgentConnections(sessionId.intValue(),
                                             boss, request,
                                             newForm, "");

        // respond to an add or remove click- we do this here
        // rather than in NewPlatformAction because in between
        // these two actions, struts repopulates the form bean and
        // resets numIps to whatever value was submitted in the
        // request.

        if (newForm.isAddClicked()) {
            int nextIndex = newForm.getNumIps();
            for (int i=0; i<nextIndex+1; i++) {
                IpValue oldIp = newForm.getIp(i);
                if (oldIp == null) {
                    newForm.setIp(i, new IpValue());
                }
            }

            newForm.setNumIps(nextIndex + 1);
        }
        else if (newForm.isRemoveClicked()) {
            int ri = Integer.parseInt(newForm.getRemove().getX());

            IpValue[] oldIps = newForm.getIps();
            if (oldIps != null) {
                // remove the indicated ip, leaving all others
                // intact
                ArrayList oldIpsList =
                    new ArrayList(Arrays.asList(oldIps));
                oldIpsList.remove(ri);
                IpValue[] newIps =
                    (IpValue[]) oldIpsList.toArray(new IpValue[0]);

                // automatically sets numIps
                newForm.setIps(newIps);
            }
            else {
                // should never happen, but if it does, reset to a
                // single ip
                resetFormIps(newForm);
            }
        }
        else if (! newForm.isOkClicked()) {
            // reset to a single ip
            resetFormIps(newForm);
        }
        // the OSType dropdown is editable in new create mode hence the true
        request.setAttribute(Constants.PLATFORM_OS_EDITABLE, Boolean.TRUE); 
        return null;
    }

    private void resetFormIps(PlatformForm form) {
        IpValue[] ips = new IpValue[1];
        ips[0] = new IpValue();
        // automatically sets numIps
        form.setIps(ips);
    }
}

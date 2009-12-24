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
 * Created on Mar 3, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.RemoveResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

public class ViewAutoDiscoveryAction
    extends TilesAction {

    private AIBoss aiBoss;

    @Autowired
    public ViewAutoDiscoveryAction(AIBoss aiBoss) {
        super();
        this.aiBoss = aiBoss;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        PlatformValue platform = (PlatformValue) RequestUtils.getResource(request);
        if (platform == null) {
            RequestUtils.setError(request, "resource.platform.error.PlatformNotFound");
            return null;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        PageControl pc = RequestUtils.getPageControl(request, "ps", "pn", "so", "sc");

        PageList<AIScheduleValue> list = aiBoss.findScheduledJobs(sessionId.intValue(), platform.getEntityId(), pc);

        RemoveResourceForm rmSchedForm = new RemoveResourceForm();
        int ps = RequestUtils.getPageSize(request, "ps");
        rmSchedForm.setPs(new Integer(ps));

        request.setAttribute(Constants.RESOURCE_REMOVE_FORM_ATTR, rmSchedForm);

        request.setAttribute(Constants.ALL_SCHEDULES_ATTR, list);
        return null;
    }

}

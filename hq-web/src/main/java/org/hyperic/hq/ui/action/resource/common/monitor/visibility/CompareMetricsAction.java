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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionMapping;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class CompareMetricsAction
    extends MetricsControlAction {

    @Autowired
    public CompareMetricsAction(AuthzBoss authzBoss) {
        super(authzBoss);
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        CompareMetricsForm compareForm = (CompareMetricsForm) form;

        if (compareForm.isBackClicked()) {
            return returnBack(request, mapping, new HashMap<String, Object>());
        }

        return super.execute(mapping, form, request, response);
    }

    private ActionForward returnBack(HttpServletRequest request, ActionMapping mapping, Map<String, Object> params)
        throws Exception {
        whackMyReturnPath(request, (BaseActionMapping) mapping);
        return constructForward(request, mapping, Constants.BACK_URL, params, YES_RETURN_PATH);
    }

    private void whackMyReturnPath(HttpServletRequest request, BaseActionMapping mapping) throws ServletException {
        String workflowName = mapping.getWorkflow();
        if (workflowName == null || "".equals(workflowName.trim())) {
            throw new ServletException("mapping " + mapping.getName() + " has a null or invalid workflow " +
                                       " attribute.");
        }

        // there should be only one workflow url. if there are more,
        // it's because we've visited the compare metrics page
        // multiple times within the nested workflow without leaving,
        // and the compare metrics page sets a return path to itself
        // every single time. so, loop through and pop them all off
        // except the very last one, which points back to where we
        // came from originally.

        int size = SessionUtils.countWorkflow(request.getSession(false), workflowName);
        for (int i = size; i > 1; i--) {
            SessionUtils.popWorkflow(request.getSession(false), workflowName);
        }
    }
}

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

package org.hyperic.hq.ui.action.resource.common.control;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An Action that removes a control event from a resource.
 */
public class RemoveHistoryAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(RemoveHistoryAction.class);

    private ControlBoss controlBoss;

    @Autowired
    public RemoveHistoryAction(ControlBoss controlBoss) {
        super();
        this.controlBoss = controlBoss;
    }

    /**
     * removes controlactions from a resource
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        HashMap<String, Object> parms = new HashMap<String, Object>(2);

        RemoveHistoryForm rmForm = (RemoveHistoryForm) form;
        Integer[] actions = rmForm.getControlActions();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        parms.put(Constants.RESOURCE_PARAM, aeid.getId());
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(aeid.getType()));

        ActionForward forward = checkSubmit(request, mapping, form, parms);

        if (forward != null) {
            return forward;
        }

        if (actions == null || actions.length == 0) {
            return this.returnSuccess(request, mapping, parms);
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        controlBoss.deleteJobHistory(sessionId.intValue(), actions);

        log.trace("Removed server control events.");
        RequestUtils.setConfirmation(request, "resource.server.ControlHistory.Confirmation");

        return this.returnSuccess(request, mapping, parms);

    }
}

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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.ScheduleForm;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> subclass that creates a control action associated with
 * a server.
 */
public class NewAction
    extends BaseAction {

    private ControlBoss controlBoss;
    private final Log log = LogFactory.getLog(NewAction.class.getName());

    @Autowired
    public NewAction(ControlBoss controlBoss) {
        super();
        this.controlBoss = controlBoss;
    }

    /**
     * Create the control action and associate it with the server.
     * <code>NewForm</code> and save it into the session attribute
     * <code>Constants.ACTION_ATTR</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        log.trace("creating new action");

        HashMap<String, Object> parms = new HashMap<String, Object>(2);

        try {

            int sessionId = RequestUtils.getSessionId(request).intValue();
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            ControlForm cForm = (ControlForm) form;

            parms.put(Constants.RESOURCE_PARAM, appdefId.getId());
            parms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(appdefId.getType()));

            ActionForward forward = checkSubmit(request, mapping, cForm, parms);
            if (forward != null) {
                return forward;
            }

            // create the new action to schedule

            ScheduleValue sv = cForm.createSchedule();
            sv.setDescription(cForm.getDescription());

            // make sure that the ControlAction is valid.
            String action = cForm.getControlAction();
            List<String> validActions = controlBoss.getActions(sessionId, appdefId);
            if (!validActions.contains(action)) {
                RequestUtils.setError(request, "resource.common.control.error.ControlActionNotValid", action);
                return returnFailure(request, mapping, parms);
            }

            if (cForm.getStartTime().equals(ScheduleForm.START_NOW)) {
                controlBoss.doAction(sessionId, appdefId, action, (String) null);
            } else {
                controlBoss.doAction(sessionId, appdefId, action, sv);
            }

            // set confirmation message
            SessionUtils.setConfirmation(request.getSession(), "resource.common.scheduled.Confirmation");

            return returnSuccess(request, mapping, parms);
        } catch (PluginNotFoundException pnfe) {
            log.trace("no plugin available", pnfe);
            RequestUtils.setError(request, "resource.common.control.PluginNotFound");
            return returnFailure(request, mapping, parms);
        } catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            RequestUtils.setError(request, "resource.common.error.ControlNotEnabled");
            return returnFailure(request, mapping, parms);
        } catch (PermissionException pe) {
            RequestUtils.setError(request, "resource.common.control.error.NewPermission");
            return returnFailure(request, mapping, parms);
        } catch (SchedulerException se) {
            RequestUtils.setError(request, "resource.common.control.error.ScheduleInvalid");
            return returnFailure(request, mapping, parms);
        }
    }
}

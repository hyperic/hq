/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This populates the EditForm associated with a server control action.
 */
public class EditFormPrepareAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(EditFormPrepareAction.class.getName());
    private ControlBoss controlBoss;

    @Autowired
    public EditFormPrepareAction(ControlBoss controlBoss) {
        super();
        this.controlBoss = controlBoss;
    }

    /**
     * Retrieve server action data and store it in the specified request
     * parameters.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.trace("Preparing to modify server control properties action.");

        ControlForm cForm = (ControlForm) form;

        // populate the control form from that ControlActionSchedule.

        try {
            int sessionId = RequestUtils.getSessionId(request).intValue();

            Integer trigger = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM);

            ControlSchedule job = controlBoss.getControlJob(sessionId, trigger);

            cForm.populateFromSchedule(job.getScheduleValue(), request.getLocale());
            cForm.setControlAction(job.getAction());
            cForm.setDescription(job.getScheduleValue().getDescription());

            AppdefEntityID appdefId = RequestUtils.getEntityId(request);

            List<String> actions = controlBoss.getActions(sessionId, appdefId);
            List<OptionItem> options = OptionItem.createOptionsList(actions);
            cForm.setControlActions(options);
            cForm.setNumControlActions(new Integer(options.size()));

            return null;
        } catch (PluginNotFoundException pnfe) {
            log.trace("no plugin available", pnfe);
            RequestUtils.setError(request, "resource.common.error.PluginNotFound");
            return null;
        } catch (PluginException cpe) {
            log.trace("could not find trigger", cpe);
            RequestUtils.setError(request, "resource.common.error.ControlNotEnabled");
            return null;
        }
    }
}
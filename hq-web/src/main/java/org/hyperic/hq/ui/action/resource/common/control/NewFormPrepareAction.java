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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.beans.OptionItem;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> subclass that prepares to create a control action
 * associated with a server.
 */
public class NewFormPrepareAction
    extends BaseAction {

    private ControlBoss controlBoss;
    private final Log log = LogFactory.getLog(NewFormPrepareAction.class.getName());

    @Autowired
    public NewFormPrepareAction(ControlBoss controlBoss) {
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

        log.trace("preparing new server control action");

        int sessionId = RequestUtils.getSessionId(request).intValue();
        ControlForm cForm = (ControlForm) form;

        AppdefEntityID appdefId = RequestUtils.getEntityId(request);

        List<String> actions = controlBoss.getActions(sessionId, appdefId);
        List<OptionItem> options = OptionItem.createOptionsList(actions);
        cForm.setControlActions(options);
        cForm.setNumControlActions(new Integer(options.size()));

        return null;

    }
}

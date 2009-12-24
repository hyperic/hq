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

package org.hyperic.hq.ui.action.resource.group.control;

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
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Retrieves details for a group control history or current event.
 */
public class ListDetailAction
    extends TilesAction {

    private ControlBoss controlBoss;

    @Autowired
    public ListDetailAction(ControlBoss controlBoss) {
        super();
        this.controlBoss = controlBoss;
    }

    /**
     * Retrieves details of history event.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        Log log = LogFactory.getLog(ListDetailAction.class.getName());

        log.trace("getting group control history details");

        int sessionId = RequestUtils.getSessionId(request).intValue();
        AppdefEntityID appdefId = RequestUtils.getEntityId(request);
        int batchId = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM).intValue();
        PageControl pc = RequestUtils.getPageControl(request);

        PageList<ControlHistory> histList = controlBoss.findGroupJobHistory(sessionId, appdefId, batchId, pc);
        request.setAttribute(Constants.CONTROL_HST_DETAIL_ATTR, histList);
        request.setAttribute(Constants.LIST_SIZE_ATTR, new Integer(histList.getTotalSize()));

        // have set page size by hand b/c of redirects
        BaseValidatorForm sForm = (BaseValidatorForm) form;
        try {
            sForm.setPs(Constants.PAGESIZE_DEFAULT);
            sForm.setPs(RequestUtils.getIntParameter(request, Constants.PAGESIZE_PARAM));
        } catch (NullPointerException npe) {
        } catch (ParameterNotFoundException pnfe) {
        } catch (NumberFormatException nfe) {
        }

        log.trace("successfully obtained group control history");

        return null;

    }
}

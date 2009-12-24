/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import javax.servlet.ServletException;
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
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A tiles Controller that retrieves a history of control actions for a server.
 */
public class ListHistoryAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ListHistoryAction.class.getName());
    private ControlBoss controlBoss;

    @Autowired
    public ListHistoryAction(ControlBoss controlBoss) {
        super();
        this.controlBoss = controlBoss;
    }

    /**
     * Retrieve a <code>PageList</code> of all <code>EventLogValue</code>
     * objects and save it into the request attribute
     * <code>Constants.CONTROL_HST_DETAIL_ATTR</code>.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        try {
            log.trace("Getting resource control history list.");

            int sessionId = RequestUtils.getSessionId(request).intValue();
            PageControl pc = RequestUtils.getPageControl(request);

            // Default control history to descending
            if (!RequestUtils.parameterExists(request, Constants.SORTORDER_PARAM))
                pc.setSortorder(PageControl.SORT_DESC);

            AppdefEntityID appdefId = RequestUtils.getEntityId(request);

            PageList<ControlHistory> histList = controlBoss.findJobHistory(sessionId, appdefId, pc);

            request.setAttribute(Constants.CONTROL_HST_DETAIL_ATTR, histList);

            // have set page size by hand b/c of redirects
            BaseValidatorForm sForm = (BaseValidatorForm) form;
            try {
                sForm.setPs(Constants.PAGESIZE_DEFAULT);
                sForm.setPs(RequestUtils.getIntParameter(request, Constants.PAGESIZE_PARAM));
            } catch (NullPointerException npe) {
            } catch (ParameterNotFoundException pnfe) {
            } catch (NumberFormatException nfe) {
            }

            log.trace("Successfuly retrieved resource control history list.");

            return null;
        } catch (PluginException cpe) {
            log.trace("control not enabled", cpe);
            RequestUtils.setError(request, "resource.common.error.ControlNotEnabled");
            return null;
        } catch (PermissionException p) {
            // let struts handle the exception
            throw p;
        } catch (ApplicationException t) {
            throw new ServletException(ListHistoryAction.class.getName() + "Can't get resource control history list.",
                t);
        }
    }

}

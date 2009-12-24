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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * List all alert definitions for this entity
 * 
 */
public class ListDefinitionsAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ListDefinitionsAction.class.getName());
    private EventsBoss eventsBoss;

    @Autowired
    public ListDefinitionsAction(EventsBoss eventsBoss) {
        super();
        this.eventsBoss = eventsBoss;
    }

    /**
     * Retrieve this data and store it in request attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.trace("in ListDefinitionAction");
        Integer sessionId = RequestUtils.getSessionId(request);

        PageControl pc = RequestUtils.getPageControl(request);

        AppdefEntityID appEntId;
        PageList<AlertDefinitionValue> alertDefs;
        try {
            appEntId = RequestUtils.getEntityTypeId(request);
            request.setAttribute("section", AppdefEntityConstants.typeToString(appEntId.getType()));
            alertDefs = eventsBoss.findAlertDefinitions(sessionId.intValue(), (AppdefEntityTypeID) appEntId, pc);
        } catch (ParameterNotFoundException e) {
            appEntId = RequestUtils.getEntityId(request);
            try {
                alertDefs = eventsBoss.findAlertDefinitions(sessionId.intValue(), appEntId, pc);
            } catch (PermissionException pe) {
                // user cant manage alerts... set empty list
                alertDefs = new PageList<AlertDefinitionValue>();
            }
        }

        context.putAttribute(Constants.RESOURCE_ATTR, RequestUtils.getResource(request));
        context.putAttribute(Constants.RESOURCE_OWNER_ATTR, request.getAttribute(Constants.RESOURCE_OWNER_ATTR));
        context.putAttribute(Constants.RESOURCE_MODIFIER_ATTR, request.getAttribute(Constants.RESOURCE_MODIFIER_ATTR));
        request.setAttribute(Constants.ALERT_DEFS_ATTR, alertDefs);

        return null;
    }
}

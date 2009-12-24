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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * form to add other emailAddresses( non-CAM) to an alert Definition
 */
public class AddOthersFormPrepareAction
    extends Action {

    private EventsBoss eventsBoss;

    @Autowired
    public AddOthersFormPrepareAction(EventsBoss eventsBoss) {
        super();
        this.eventsBoss = eventsBoss;
    }

    /**
     * Retrieve this data and store it in the specified request parameters:
     * 
     * <ul>
     * <li><code>OwnedRoleValue</code> object identified by
     * <code>Constants.ROLE_PARAM</code> request parameter in in
     * <code>Constants.ROLE_ATTR</code></li>
     * <li><code>List</code> of available <code>AuthzSubjectValue</code> objects
     * (those not already associated with the role) in
     * </ul>
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        AddOthersForm addForm = (AddOthersForm) form;
        Integer alertDefId = addForm.getAd();
        if (alertDefId == null) {
            throw new ParameterNotFoundException("alert definition id not found");
        }

        try {
            AppdefEntityID appTypeId = RequestUtils.getEntityTypeId(request);
            addForm.setAetid(appTypeId.getAppdefKey());
        } catch (ParameterNotFoundException e) {
            AppdefEntityID aeid = RequestUtils.getEntityId(request);
            addForm.setType(new Integer(aeid.getType()));
            addForm.setRid(aeid.getId());
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        AlertDefinitionValue alertDef = (AlertDefinitionValue) request.getAttribute(Constants.ALERT_DEFS_ATTR);
        if (alertDef == null) {
            alertDef = eventsBoss.getAlertDefinition(sessionId.intValue(), alertDefId);
        }
        addForm.setAd(alertDef.getId());

        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, alertDef);

        return null;
    }
}

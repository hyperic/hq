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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create a new alert definition.
 * 
 */
public class EditDefinitionConditionsAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(EditDefinitionConditionsAction.class.getName());

    private EventsBoss eventsBoss;
    private MeasurementBoss measurementBoss;

    @Autowired
    public EditDefinitionConditionsAction(EventsBoss eventsBoss, MeasurementBoss measurementBoss) {
        super();
        this.eventsBoss = eventsBoss;
        this.measurementBoss = measurementBoss;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        DefinitionForm defForm = (DefinitionForm) form;
        log.trace("defForm.id=" + defForm.getAd());

        Map<String, Object> params = new HashMap<String, Object>();
        AppdefEntityID adeId;
        if (defForm.getRid() != null) {
            adeId = new AppdefEntityID(defForm.getType().intValue(), defForm.getRid());
            params.put(Constants.ENTITY_ID_PARAM, adeId.getAppdefKey());
        } else {
            adeId = new AppdefEntityTypeID(defForm.getType().intValue(), defForm.getResourceType());
            params.put(Constants.APPDEF_RES_TYPE_ID, adeId.getAppdefKey());
        }
        params.put("ad", defForm.getAd());

        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        int sessionID = RequestUtils.getSessionId(request).intValue();

        AlertDefinitionValue adv = eventsBoss.getAlertDefinition(sessionID, defForm.getAd());
        defForm.exportConditionsEnablement(adv, request, sessionID, measurementBoss, EventConstants.TYPE_ALERT_DEF_ID
            .equals(adv.getParentId()));
        
        try {
        	eventsBoss.updateAlertDefinition(sessionID, adv);
        } catch(Exception e) {
        	RequestUtils.setError(request, "error.generic.temporarily.unavailable", "editAlertDefinitionError");
        	return returnFailure(request, mapping, params);
        }
        
        return returnSuccess(request, mapping, params);
    }
}

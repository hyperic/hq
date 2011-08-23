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

package org.hyperic.hq.ui.action.resource.group.monitor.config;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.config.ConfigMetricsAction;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * modifies the metrics data.
 */
public class GroupConfigMetricsAction
    extends ConfigMetricsAction {

    private final Log log = LogFactory.getLog(ConfigMetricsAction.class.getName());

    @Autowired
    public GroupConfigMetricsAction(MeasurementBoss measurementBoss, TransactionRetry transactionRetry) {
        super(measurementBoss, transactionRetry);
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        log.trace("modifying metrics action");

        HashMap<String, Object> parms = new HashMap<String, Object>(2);

        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        parms.put(Constants.RESOURCE_PARAM, aeid.getId());
        parms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(aeid.getType()));

        ActionForward forward = super.execute(mapping, form, request, response);
        if (forward != null) {
            return forward;
        }

        RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.config.ConfigMetrics.Confirmation");

        return returnSuccess(request, mapping, parms);
    }

}

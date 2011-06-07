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

package org.hyperic.hq.ui.action.resource.common.monitor.config;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * modifies the metrics data.
 */
public class ConfigMetricsAction
    extends BaseAction {
    protected MeasurementBoss measurementBoss;
    private final Log log = LogFactory.getLog(ConfigMetricsAction.class.getName());
    private TransactionRetry transactionRetry;

    @Autowired
    public ConfigMetricsAction(MeasurementBoss measurementBoss, TransactionRetry transactionRetry) {
        super();
        this.measurementBoss = measurementBoss;
        this.transactionRetry = transactionRetry;
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        log.trace("modifying metrics action");

        HashMap<String, Object> parms = new HashMap<String, Object>(2);

        final int sessionId = RequestUtils.getSessionId(request).intValue();
        MonitoringConfigForm mForm = (MonitoringConfigForm) form;
        // this action will be passed an entityTypeId OR an entityId
        AppdefEntityTypeID aetid = null;
        AppdefEntityID appdefId = null;
        try {
            // check for appdef entity
            appdefId = RequestUtils.getEntityId(request);

            parms.put(Constants.RESOURCE_PARAM, appdefId.getId());
            parms.put(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(appdefId.getType()));

        } catch (ParameterNotFoundException e) {
            // we better have a entityTypeId or this will throw an
            // uncaught ParameterNotFOundException
            aetid = new AppdefEntityTypeID(RequestUtils.getStringParameter(request, Constants.APPDEF_RES_TYPE_ID));
            parms.put("aetid", aetid.getAppdefKey());
        }

        final Integer[] midsToUpdate = mForm.getMids();

        ActionForward forward = checkSubmit(request, mapping, form, parms);

        if (forward != null) {
            if (mForm.isRemoveClicked()) {

                // don't make any back-end call if user has not selected any
                // metrics.
                if (midsToUpdate.length == 0)
                    return forward;

                measurementBoss.disableMeasurements(sessionId, appdefId, midsToUpdate);
                RequestUtils.setConfirmation(request,
                    "resource.common.monitor.visibility.config.RemoveMetrics.Confirmation");
            }
            return forward;
        }

        // take the list of pending metric ids (mids),
        // and update them.);
        final long interval = mForm.getIntervalTime();

        // don't make any back-end call if user has not selected any metrics.
        if (midsToUpdate.length == 0)
            return returnSuccess(request, mapping, parms);

        String confirmation = "resource.common.monitor.visibility.config.ConfigMetrics.Confirmation";
        if (aetid == null) {
            measurementBoss.updateMeasurements(sessionId, appdefId, midsToUpdate, interval);
        } else {
            if (mForm.isIndSelected()) {
                measurementBoss.updateIndicatorMetrics(sessionId, aetid, midsToUpdate);
                confirmation = "resource.common.monitor.visibility.config.IndicatorMetrics.Confirmation";
            } else {
                final Runnable runner = new Runnable() {
                    public void run() {
                        try {
                            measurementBoss.updateMetricDefaultInterval(sessionId, midsToUpdate, interval);
                        } catch (SessionException e) {
                            log.error(e,e);
                        }
                    }
                };
                transactionRetry.runTransaction(runner, 3, 1000);
            }
        }
        RequestUtils.setConfirmation(request, confirmation);

        return returnSuccess(request, mapping, parms);

    }

}

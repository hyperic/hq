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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.GroupMetricDisplaySummary;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Create a new alert definition.
 *
 */
public class NewDefinitionAction extends BaseAction {

    private Log log = LogFactory.getLog(NewDefinitionAction.class.getName());

    // ---------------------------------------------------- Public Methods

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        DefinitionForm defForm = (DefinitionForm)form;

        Map params = new HashMap();
        AppdefEntityID adeId;
        if (defForm.getRid() != null) {
            adeId = new AppdefEntityID(defForm.getType().intValue(),
                                       defForm.getRid());
            params.put(Constants.ENTITY_ID_PARAM, adeId.getAppdefKey());
        }
        else {
            adeId = new AppdefEntityTypeID(defForm.getType().intValue(),
                    defForm.getResourceType());
            params.put(Constants.APPDEF_RES_TYPE_ID, adeId.getAppdefKey());
        }


        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);

        AlertDefinitionValue adv = new AlertDefinitionValue();
        defForm.exportProperties(adv);
        defForm.exportConditionsEnablement(adv, request, sessionID, mb,
                                           adeId instanceof AppdefEntityTypeID);
        adv.setAppdefType( adeId.getType() );
        adv.setAppdefId( adeId.getID() );
        log.trace("adv="+adv);
        
        if (adeId instanceof AppdefEntityTypeID)
            adv = eb.createResourceTypeAlertDefinition(
                    sessionID, (AppdefEntityTypeID) adeId, adv);
        else
            adv = eb.createAlertDefinition(sessionID, adv);

        params.put( Constants.ALERT_DEFINITION_PARAM, adv.getId() );

        if ( _areAnyMetricsDisabled(adv, adeId, sessionID, mb) ) {
            RequestUtils.setError
                (request, "resource.common.monitor.alert.config.error.SomeMetricsDisabled");
        } else {
            RequestUtils.setConfirmation
                (request, "resource.common.monitor.alert.config.confirm.Create");
        }
        return returnSuccess(request, mapping, params);
    }

    private boolean _areAnyMetricsDisabled(AlertDefinitionValue adv, AppdefEntityID adeId,
                                           int sessionID, MeasurementBoss mb)
        throws SessionNotFoundException,
               SessionTimeoutException,
               AppdefEntityNotFoundException,
               GroupNotCompatibleException,
               PermissionException,
               RemoteException
    {
        // create a map of metricId --> enabled for this resource
        List metrics = mb.findMeasurements( sessionID, adeId, PageControl.PAGE_ALL );
        Map metricEnabledFlags = new HashMap( metrics.size() );
        for (Iterator it=metrics.iterator(); it.hasNext();) {
            // Groups are handled differently here.  The list of
            // metrics that will be returned for a group will be
            // GroupMetricDisplaySummary beans instead of
            // DerivedMeasurementValue beans.  We cannot check the
            // enabled status of these measurements for groups, so
            // don't do anything here.
            try {
                DerivedMeasurementValue dmv = (DerivedMeasurementValue)it.next();
                metricEnabledFlags.put( dmv.getId(), new Boolean( dmv.getEnabled() ) );
            } catch (ClassCastException e) {
                GroupMetricDisplaySummary gds = (GroupMetricDisplaySummary)it.next();
            }
        }

        // iterate over alert conditions and see if any of the metrics
        // being used are disabled
        for (int i=0; i<adv.getConditions().length; ++i) {
            if ( adv.getConditions()[i].measurementIdHasBeenSet() ) {
                Integer mid = new Integer( adv.getConditions()[i].getMeasurementId() );
                Boolean metricEnabled = (Boolean)metricEnabledFlags.get(mid);
                if (null != metricEnabled) {
                    if (! metricEnabled.booleanValue() ) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}

// EOF

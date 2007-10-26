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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.CpropKey;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;

/**
 * Prepare the alert definition form for new / edit.
 *
 */
public abstract class DefinitionFormPrepareAction extends TilesAction {
    protected Log log =
        LogFactory.getLog(DefinitionFormPrepareAction.class.getName());    

    /**
     * Prepare the form for a new alert definition.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception 
    {
        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);
        ControlBoss cb = ContextUtils.getControlBoss(ctx);
        AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);

        DefinitionForm defForm = (DefinitionForm) form;
        setupForm(defForm, request, sessionID, eb, mb, cb, ab);

        if (! defForm.isOkClicked() ) {
            // setting up form for the first time
            setupConditions(request, defForm);
        }

        return null;
    }

    protected void setupForm(DefinitionForm defForm, HttpServletRequest request,
                             int sessionID, EventsBoss eb, MeasurementBoss mb,
                             ControlBoss cb, AppdefBoss ab)
        throws Exception 
    {
        request.setAttribute( "enableEachTime",
                              new Integer(EventConstants.FREQ_EVERYTIME) );
        request.setAttribute( "enableOnce",
                              new Integer(EventConstants.FREQ_ONCE) );
        request.setAttribute( "enableTimePeriod",
                              new Integer(EventConstants.FREQ_DURATION) );
        request.setAttribute( "enableNumTimesInPeriod",
                              new Integer(EventConstants.FREQ_COUNTER) );
        request.setAttribute( "noneDeleted",
                              new Integer(Constants.ALERT_CONDITION_NONE_DELETED) );

        PageControl pc = PageControl.PAGE_ALL;
        List metrics, baselines = new ArrayList();
        int numMetricsEnabled = 0;
        AppdefEntityID adeId;
        boolean controlEnabled;
        
        try {
            adeId = RequestUtils.getEntityTypeId(request);
            metrics = mb.findMeasurementTemplates(
                    sessionID, (AppdefEntityTypeID) adeId, null, pc);
            defForm.setType(new Integer(adeId.getType()));
            defForm.setResourceType(adeId.getId());
            numMetricsEnabled++;
            
            controlEnabled =
                cb.isControlSupported(sessionID, (AppdefEntityTypeID) adeId);            
        } catch (ParameterNotFoundException e) {
            adeId = RequestUtils.getEntityId(request);
            metrics = mb.findMeasurements( sessionID, adeId, pc );

            if (adeId.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                for (Iterator it = metrics.iterator(); it.hasNext();) {
                    DerivedMeasurementValue dmv =
                        (DerivedMeasurementValue) it.next();
                    if (dmv.getEnabled())
                        numMetricsEnabled++;
                }
            }

            controlEnabled = cb.isControlEnabled(sessionID, adeId);
            
            // Check for logging
            // [SUPPORT-21] Show log track alerting on all resources/resource
            // types
            /*
            boolean logTrackEnabled = mb.isLogTrackEnabled(sessionID, adeId);
            if (logTrackEnabled)
                request.setAttribute("logTrackEnabled", Boolean.TRUE);
                */
        }
        request.setAttribute("logTrackEnabled", Boolean.TRUE);
        
        defForm.setMetrics(metrics);

        if (metrics.size() == 0) {
            RequestUtils.setError(
                request, "resource.common.monitor.alert.config.error.NoMetricsConfigured");
        } else if (numMetricsEnabled == 0) {
            RequestUtils.setError(
                request, "resource.common.monitor.alert.config.error.NoMetricsEnabled");
        }
        
        // need to duplicate this for the JavaScript on the page
        request.setAttribute("baselines", baselines);
        
        request.setAttribute(Constants.CONTROL_ENABLED,
                             new Boolean(controlEnabled));
        if (controlEnabled) {
            defForm.setControlActions(
                AlertDefUtil.getControlActions(sessionID, adeId, cb));
        } else {
            List controlActions = new ArrayList(1);
            controlActions.add("(N/A)");
            defForm.setControlActions(controlActions);
        }
        
        List custProps = getCustomProperties(sessionID, adeId, ab);
        if (custProps != null && custProps.size() > 0) {
            request.setAttribute(Constants.CUSTPROPS_AVAIL, Boolean.TRUE);
            defForm.setCustomProperties(custProps);            
        }
    }

    protected abstract void setupConditions(HttpServletRequest request,
                                            DefinitionForm defForm)
        throws Exception;

    /**
     * Returns a List of custom property keys for the passed-in resource.
     */
    protected List getCustomProperties(int sessionID, AppdefEntityID adeId,
                                     AppdefBoss ab)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException,
               RemoteException 
    {
        List custProps;
        
        if (adeId instanceof AppdefEntityTypeID)
            custProps =
                ab.getCPropKeys(sessionID, adeId.getType(), adeId.getID());
        else
            custProps = ab.getCPropKeys(sessionID, adeId);
            
        ArrayList custPropStrs = new ArrayList(custProps.size());
        for (Iterator it = custProps.iterator(); it.hasNext();) {
            CpropKey custProp = (CpropKey) it.next();
            custPropStrs.add(new LabelValueBean(custProp.getDescription(),
                                                custProp.getKey()));
        }
    
        return custPropStrs;
    }
}

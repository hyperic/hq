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

package org.hyperic.hq.bizapp.server.session;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefSessionEJB;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossLocal;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertSortField;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GalertManagerImpl;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @ejb:bean name="DashboardPortletBoss"
 *      jndi-name="ejb/bizapp/DashboardPortletBoss"
 *      local-jndi-name="LocalDashboardPortletBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class DashboardPortletBossEJBImpl
    extends AppdefSessionEJB
    implements SessionBean {

    private static final String ALERT_CRITICAL = "red",
                                ALERT_WARN     = "yellow",
                                ALERT_UNKNOWN  = "gray",
                                ALERT_OK       = "green";
    
    private final Log _log =
        LogFactory.getLog(DashboardPortletBossEJBImpl.class);

    /**
     * @return JSONArray made up of several JSONObjects.  Output looks similar
     * to this:
     * [[{"data":{"2008-07-09T10:45:28-0700":[1],"2008-07-09T10:46:28-0700":[1],
     *            "2008-07-09T10:48:28-0700":[1],"2008-07-09T10:58:28-0700":[1]},
     *    "resourceName":"clone-0"}]]
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public JSONArray getMeasurementData(AuthzSubject subj,
                                        Integer resId, Integer mtid, 
                                        AppdefEntityTypeID ctype,
                                        long begin, long end)
        throws PermissionException
    {
        JSONArray rtn = new JSONArray();
        ResourceManager resMan = getResourceManager();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        DataManagerLocal dMan = DataManagerEJBImpl.getOne();
        DateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        long intv = (end - begin) / 60;
        JSONObject jObj = new JSONObject();
        Resource res = resMan.findResourceById(resId);
        if (res == null || res.isInAsyncDeleteState()) {
            return rtn;
        }
        AppdefEntityID aeid = new AppdefEntityID(res);
        try {
            jObj.put("resourceName", res.getName());

            AppdefEntityID[] aeids;
            if (aeid.isGroup()) {
                List members =
                    GroupUtil.getCompatGroupMembers(subj, aeid, null,
                                                    PageControl.PAGE_ALL);
                aeids = (AppdefEntityID[])
                members.toArray(new AppdefEntityID[members.size()]);
            } else if (ctype != null) {
                aeids = MeasurementBossEJBImpl.getOne()
                                    .getAutoGroupMemberIDs(
                                            subj,
                                            new AppdefEntityID[] { aeid },
                                            ctype); 
            } else {
                aeids = new AppdefEntityID[] { aeid };
            }
            List metrics = mMan.findMeasurements(subj, mtid, aeids);

            // Get measurement name
            if (!metrics.isEmpty()) {
                Measurement measurement = (Measurement) metrics.get(0);
                jObj.put("measurementName", measurement.getTemplate().getName());
            }
            
            List data = dMan.getHistoricalData(metrics, begin, end,
                                               intv, 0, true,
                                               PageControl.PAGE_ALL);

            JSONObject dataObj = new JSONObject();
            jObj.put("data", dataObj);
            for (Iterator dit = data.iterator(); dit.hasNext();) {
                JSONArray array = new JSONArray();
                HighLowMetricValue pt = (HighLowMetricValue) dit.next();
                double val = pt.getValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    continue;
                }
                array.put(val);
                Date date = new Date(pt.getTimestamp());
                dataObj.put(dateFmt.format(date), array);
            }
            rtn.put(jObj);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (AppdefEntityNotFoundException e) {
            log.error("AppdefEntityNotFound: " + aeid);
        } catch (GroupNotCompatibleException e) {
            log.error("GroupNotCompatibleException: " + aeid);
        }
        return rtn;
    }

    /**
     * @throws PermissionException 
     * @throws JSONException 
     * @ejb:interface-method
     */
    public JSONObject getAllGroups(AuthzSubject subj)
        throws PermissionException, JSONException
    {
        JSONObject rtn = new JSONObject();
        Collection groups =
            ResourceGroupManagerImpl.getOne().getAllResourceGroups(subj, true);
        for (Iterator i=groups.iterator(); i.hasNext(); ) {
            ResourceGroup group = (ResourceGroup)i.next();
            rtn.put(group.getId().toString(), group.getName());
        }
        return rtn;
    }
    
    /**
     * @ejb:interface-method
     */
    public JSONObject getAlertCounts(AuthzSubject subj, List groupIds,
                                     PageInfo pageInfo)
        throws PermissionException, JSONException, FinderException
    {
        final long PORTLET_RANGE = MeasurementConstants.DAY * 3;
        
        JSONObject rtn = new JSONObject();
        ResourceGroupManager rgMan = ResourceGroupManagerImpl.getOne();
        final int maxRecords = pageInfo.getStartRow() + pageInfo.getPageSize();
        int i=0;
        for (Iterator it=groupIds.iterator(); it.hasNext(); i++) {
            if ( maxRecords > 0 && i > maxRecords ) {
                break;
            }
            if (i < pageInfo.getStartRow()) {
                continue;
            }
            Integer gId = (Integer)it.next();
            ResourceGroup group = rgMan.findResourceGroupById(subj, gId);
            if (group != null) {
                JSONArray array = new JSONArray()
                    .put(getResourceStatus(subj, group, PORTLET_RANGE))
                    .put(getGroupStatus(subj, group, PORTLET_RANGE));
                rtn.put(group.getId().toString(), array);  
            }
        }
        return rtn;
    }

    private String getGroupStatus(AuthzSubject subj, ResourceGroup group,
                                  long range)
    {
        boolean debug = _log.isDebugEnabled();
        String rtn = ALERT_OK;
        long now = System.currentTimeMillis();

        try {
            long begin = now - range;
            GalertManager gaMan = GalertManagerImpl.getOne();
            List galerts = gaMan.findUnfixedAlertLogsByTimeWindow(group, begin,
                                                                   now);
            if (debug) {
                _log.debug("getGroupStatus: findUnfixedAlertLogsByTimeWindow execution time(ms)=" 
                                + (System.currentTimeMillis()-now));
            }            
            GalertLog galert = null;
            for (Iterator i = galerts.iterator(); i.hasNext();) {
                galert = (GalertLog)i.next();
                try {
                    checkAlertingPermission(subj, galert.getAlertDef().getAppdefID());
                } catch (PermissionException pe) {
                    // continue to next group alert
                    continue;
                }
                // a galert always has an associated escalation which may or may not
                // be acknowledged.
                if (galert.hasEscalationState() && galert.isAcknowledged()) {
                    rtn = ALERT_WARN;
                } else {
                    return ALERT_CRITICAL;
                }
            }
            
            // Is it that there are no alerts or that there are no alert
            // definitions?
            if (rtn.equals(ALERT_OK)) {
                List galertDefs = gaMan.findAlertDefs(group, PageControl.PAGE_ALL);
                if (galertDefs.size() == 0) {
                    return ALERT_UNKNOWN;
                }
            }

            return rtn;            
        } finally {
            if (debug) {
                _log.debug("getGroupStatus: groupId=" + group.getId()
                                +", execution time(ms)=" + (System.currentTimeMillis()-now));
            }
        }        
    }
    
    private String getResourceStatus(AuthzSubject subj, ResourceGroup group,
                                     long range)
        throws FinderException {
        boolean debug = _log.isDebugEnabled();
        long now = System.currentTimeMillis();
        StopWatch watch = new StopWatch(now);
        
        try {
            long begin = now - range;
            
            watch.markTimeBegin("getResourceStatus: getUnfixedCount");
            AlertManagerLocal alMan = AlertManagerEJBImpl.getOne();
            int unfixed = alMan.getUnfixedCount(subj.getId(), begin, now,
                                                 group.getId());
            watch.markTimeEnd("getResourceStatus: getUnfixedCount");
            
            // There are unfixed alerts
            if (unfixed > 0) {
                watch.markTimeBegin("getResourceStatus: findAlerts");            
                List alerts =
                    alMan.findAlerts(subj.getId(), 0, begin, now,
                                      true, true, group.getId(),
                                      PageInfo.getAll(AlertSortField.FIXED,
                                                      true));
                watch.markTimeEnd("getResourceStatus: findAlerts");

				// Are all unfixed alerts in escalation?
                if (alerts.size() != unfixed)
                    return ALERT_CRITICAL;

				// Make sure that all unfixed alerts have been ack'ed
                Alert alert = null;
                for (Iterator it = alerts.iterator(); it.hasNext(); ) {
                    alert = (Alert) it.next();
                    if (!isAckd(subj, alert)) {
                        return ALERT_CRITICAL;
                    }
                }
                return ALERT_WARN;
            }
            else {
                // Is it that there are no alerts or that there are no alert
                // definitions?
                ResourceGroupManager rgMan =
                    ResourceGroupManagerImpl.getOne();
                AlertDefinitionManager adMan =
                    AlertDefinitionManagerImpl.getOne();
                Collection resources = rgMan.getMembers(group);
                PageControl pc = new PageControl(0, 1);
                Resource r = null;
                AppdefEntityID aId = null;
                List alertDefs = null;
                for (Iterator i = resources.iterator(); i.hasNext();) {
                    r = (Resource) i.next();
                    aId = new AppdefEntityID(r);
    
                    try {
                        checkViewPermission(subj, aId);
                    } catch (PermissionException pe) {
                        // go to next resource
                        continue;
                    }
                    alertDefs = adMan.findAlertDefinitions(
                        subj, new AppdefEntityID(r), pc);
                    if (alertDefs.size() > 0) {
                        return ALERT_OK;
                    }
                }
            }
        } catch (PermissionException e) {
            // User has no permission to see these resources
        } finally {
            if (debug) {
                _log.debug("getResourceStatus: groupId=" + group.getId() +
                           ", execution time =" + watch);
            }            
        }
        return ALERT_UNKNOWN;
    }

    private boolean isAckd(AuthzSubject subj, Alert alert)
        throws FinderException
    {
        AlertDefinition alertDef = alert.getAlertDefinition();
        // a resource alert may not have an associated escalation
        Escalation esc = alertDef.getEscalation();
        if (esc == null || esc.getMaxPauseTime() == 0) {
            return false;
        }
        EscalationManagerLocal escMan = EscalationManagerEJBImpl.getOne();
        EscalationState state = escMan.findEscalationState(alertDef);
        return state != null && state.getAcknowledgedBy() != null;
    }

    public static DashboardPortletBossLocal getOne() {
        try {
            return DashboardPortletBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}

}

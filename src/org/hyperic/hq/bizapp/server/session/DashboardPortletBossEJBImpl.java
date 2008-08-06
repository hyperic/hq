/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.Map;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefSessionEJB;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossLocal;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.util.pager.PageControl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @ejb:bean name="DashboardPortletBoss"
 *      jndi-name="ejb/bizapp/DashboardPortletBoss"
 *      local-jndi-name="LocalDashboardPortletBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 */
public class DashboardPortletBossEJBImpl
    extends AppdefSessionEJB
    implements SessionBean {

    private GalertManagerLocal _gaMan;
    private ResourceGroupManagerLocal _rgMan;
    private AlertDefinitionManagerLocal _adMan;
    private EscalationManagerLocal _escMan;
    private AlertManagerLocal _alMan = AlertManagerEJBImpl.getOne();
    private static final String ALERT_CRITICAL = "red",
                                ALERT_WARN     = "yellow",
                                ALERT_UNKNOWN  = "gray",
                                ALERT_OK       = "green";
    
    public DashboardPortletBossEJBImpl() {
        _rgMan = ResourceGroupManagerEJBImpl.getOne();
        _gaMan = GalertManagerEJBImpl.getOne();
        _adMan = AlertDefinitionManagerEJBImpl.getOne();
        _escMan = EscalationManagerEJBImpl.getOne();
    }

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
    public JSONArray getMeasurementData(AuthzSubject subj, Map resIdsToTemplIds,
                                        long begin, long end)
        throws PermissionException
    {
        JSONArray rtn = new JSONArray();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        DataManagerLocal dMan = DataManagerEJBImpl.getOne();
        Map resToMeas = mMan.findMeasurements(subj, resIdsToTemplIds);
        long intv = (end-begin)/60;
        PageControl pc = new PageControl(0, PageControl.SIZE_UNLIMITED);
        DateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        for (Iterator i=resToMeas.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            Resource res = (Resource)entry.getKey();
            List measurements = (List)entry.getValue();
            List data = dMan.getHistoricalData(measurements, begin, end, intv,
                0, true, pc);
            JSONObject jObj = new JSONObject();
            try {
                jObj.put("resourceName", res.getName());
                JSONObject dataObj = new JSONObject();
                jObj.put("data", dataObj);
                for (Iterator it=data.iterator(); it.hasNext(); ) {
                    JSONArray array = new JSONArray();
                    HighLowMetricValue pt = (HighLowMetricValue)it.next();
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
                _log.error(e.getMessage(), e);
            }
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
        Collection groups = _rgMan.getAllResourceGroups(subj, true);
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
        JSONObject rtn = new JSONObject();
        _rgMan = ResourceGroupManagerEJBImpl.getOne();
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
            ResourceGroup group = _rgMan.findResourceGroupById(subj, gId);
            JSONArray array = new JSONArray();
            array.put(getResourceStatus(subj, group));
            array.put(getGroupStatus(subj, group));
            rtn.put(group.getId().toString(), array);
        }
        return rtn;
    }

    private String getGroupStatus(AuthzSubject subj, ResourceGroup group)
        throws PermissionException
    {
        List galertDefs = _gaMan.findAlertDefs(group, PageControl.PAGE_ALL);
        String rtn = ALERT_OK;
        if (galertDefs.size() == 0) {
            return ALERT_UNKNOWN;
        }
        long now = System.currentTimeMillis();
        long begin = now-MeasurementConstants.HOUR*24*7;
        PageControl pc = new PageControl(0, PageControl.SIZE_UNLIMITED);
        List galerts = _gaMan.findAlertLogsByTimeWindow(group, begin, now, pc);
        for (Iterator i=galerts.iterator(); i.hasNext(); ) {
            GalertLog galert = (GalertLog)i.next();
            checkAlertingPermission(subj, galert.getAlertDef().getAppdefID());
            if (galert.isFixed()) {
                continue;
            }
            // a galert always has an associated escalation which may or may not
            // be acknowledged.
            if (!galert.isAcknowledgeable()) {
                rtn = ALERT_WARN;
            } else {
                return ALERT_CRITICAL;
            }
        }
        return rtn;
    }
    
    private String getResourceStatus(AuthzSubject subj, ResourceGroup group)
        throws PermissionException, FinderException
    {
        String rtn = ALERT_UNKNOWN;
        boolean isSet = false;
        Collection resources = _rgMan.getMembers(group);
        PageControl pc = new PageControl(0, PageControl.SIZE_UNLIMITED);
        for (Iterator i=resources.iterator(); i.hasNext(); ) {
            Resource r = (Resource)i.next();
            AppdefEntityID aId = new AppdefEntityID(r);
            checkViewPermission(subj, aId);
            List alertDefs = _adMan.findAlertDefinitions(
                subj, new AppdefEntityID(r), PageControl.PAGE_ALL);
            if (alertDefs.size() == 0) {
                continue;
            }
            if (!isSet) {
                rtn = ALERT_OK;
            }
            isSet = true;
            List alerts = _alMan.findAlerts(subj, aId, pc);
            for (Iterator it=alerts.iterator(); it.hasNext(); ) {
                AlertValue alert = (AlertValue)it.next();
                if (alert.isFixed()) {
                    continue;
                }
                if (isAckd(subj, alert)) {
                    rtn = ALERT_WARN;
                } else {
                    return ALERT_CRITICAL;
                }
            }
        }
        return rtn;
    }

    private boolean isAckd(AuthzSubject subj, AlertValue alert)
        throws PermissionException, FinderException
    {
        AlertDefinitionValue alertDef =
            _adMan.getById(subj, alert.getAlertDefId());
        // a resource alert may not have an associated escalation
        Integer escId = alertDef.getEscalationId();
        if (escId == null) {
            return false;
        }
        Escalation esc = _escMan.findById(escId);
        if (esc == null || esc.getMaxPauseTime() == 0) {
            return false;
        }
        EscalationState state = _escMan.findEscalationState(
            _alMan.findAlertById(alert.getId()).getAlertDefinition());
        if (state == null || state.getAcknowledgedBy() == null) {
            return false;
        }
        return true;
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

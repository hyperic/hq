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

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossLocal;
import org.hyperic.hq.bizapp.shared.DashboardPortletBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
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
    extends MetricSessionEJB
    implements SessionBean {

    private AlertManagerLocal _alMan = AlertManagerEJBImpl.getOne();
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
                    array.put(pt.getValue());
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
        ResourceGroupManagerLocal _rgMan = ResourceGroupManagerEJBImpl.getOne();
        Collection groups = _rgMan.getAllResourceGroups(subj, true);
        for (Iterator i=groups.iterator(); i.hasNext(); ) {
            ResourceGroup group = (ResourceGroup)i.next();
            rtn.put(group.getId().toString(), group.getName());
        }
        return rtn;
    }
    
    /**
     * @throws PermissionException 
     * @throws JSONException 
     * @ejb:interface-method
     */
    public JSONObject getAlertCounts(AuthzSubject subj, List groupIds,
                                     PageInfo pageInfo)
        throws PermissionException, JSONException
    {
        JSONObject rtn = new JSONObject();
        ResourceGroupManagerLocal _rgMan = ResourceGroupManagerEJBImpl.getOne();
        _rgMan = ResourceGroupManagerEJBImpl.getOne();
        int i=0;
        for (Iterator it=groupIds.iterator(); it.hasNext(); i++) {
            if ( i > (pageInfo.getStartRow() + pageInfo.getPageSize()) ) {
                break;
            }
            if (i < pageInfo.getStartRow()) {
                continue;
            }
            Integer gId = (Integer)it.next();
            ResourceGroup group = _rgMan.findResourceGroupById(subj, gId);
            JSONArray array = new JSONArray();
            array.put(getResourceStatus(subj, group));
            array.put(getGroupStatus(group));
            rtn.put(group.getId().toString(), array);
        }
        return rtn;
    }

    /**
     * XXX: not sure if this is working
     */
    private String getGroupStatus(ResourceGroup group) {
        GalertManagerLocal _gaMan = GalertManagerEJBImpl.getOne();
        List galerts = _gaMan.findAlertLogs(group);
        String rtn = ALERT_OK;
        if (galerts.size() == 0) {
            return ALERT_UNKNOWN;
        }
        for (Iterator i=galerts.iterator(); i.hasNext(); ) {
            GalertLog galert = (GalertLog)i.next();
            if (galert.isFixed()) {
                continue;
            }
            if (isAckd(galert)) {
                rtn = ALERT_CRITICAL;
            } else {
                return ALERT_CRITICAL;
            }
        }
        return rtn;
    }
    
    private String getResourceStatus(AuthzSubject subj, ResourceGroup group)
        throws PermissionException
    {
        String rtn = ALERT_OK;
        ResourceGroupManagerLocal _rgMan = ResourceGroupManagerEJBImpl.getOne();
        _rgMan = ResourceGroupManagerEJBImpl.getOne();
        Collection resources = _rgMan.getMembers(group);
        PageControl pc = new PageControl(0, PageControl.SIZE_UNLIMITED);
        for (Iterator i=resources.iterator(); i.hasNext(); ) {
            Resource r = (Resource)i.next();
            AppdefEntityID aId = new AppdefEntityID(r);
            List alerts = _alMan.findAlerts(subj, aId, pc);
            if (alerts.size() == 0) {
                return ALERT_UNKNOWN;
            }
            for (Iterator it=alerts.iterator(); it.hasNext(); ) {
                Alert alert = (Alert)it.next();
                if (alert.isFixed()) {
                    continue;
                }
                if (isAckd(alert)) {
                    rtn = ALERT_WARN;
                } else {
                    return ALERT_CRITICAL;
                }
            }
        }
        return rtn;
    }
    
    /**
     * TODO finish this method
     */
    private boolean isAckd(AlertInterface def) {
        Random rand = new Random(1);
        return (rand.nextInt() == 1) ? true : false;
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
    public void ejbRemove() throws EJBException, RemoteException {
        ctx = null;
    }

    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return ctx;
    }
}

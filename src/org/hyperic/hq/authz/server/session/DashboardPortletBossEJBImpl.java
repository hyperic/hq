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

package org.hyperic.hq.authz.server.session;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.DashboardPortletBossLocal;
import org.hyperic.hq.authz.shared.DashboardPortletBossUtil;
import org.hyperic.hq.bizapp.server.session.MetricSessionEJB;
import org.hyperic.hq.common.SystemException;
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
 *      jndi-name="ejb/authz/DashboardPortletBoss"
 *      local-jndi-name="LocalDashboardPortletBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 */
public class DashboardPortletBossEJBImpl
    extends MetricSessionEJB
    implements SessionBean {

    private final Log _log =
        LogFactory.getLog(DashboardPortletBossEJBImpl.class);

    /**
     * @return JSONArray made up of several JSONObjects.  Output looks similar
     * to this:
     * [{"data":{"2008-07-08T17:31:15-0700":[1]},"resourceName":"name"},
     *  {"data":{"2008-07-08T17:32:15-0700":[1]},"resourceName":"name"}]
     * @ejb:interface-method
     */
    public JSONArray getMeasurementData(AuthzSubject subj, Map resIdsToTemplIds,
                                        long begin, long end) {
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
            for (Iterator it=data.iterator(); it.hasNext(); ) {
                HighLowMetricValue pt = (HighLowMetricValue)it.next();
                JSONObject jObj = new JSONObject();
                try {
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    array.put(pt.getValue());
                    Date date = new Date(pt.getTimestamp());
                    obj.put(dateFmt.format(date), array);
	                jObj.put("resourceName", res.getName());
	                jObj.put("data", obj);
                    rtn.put(jObj);
                } catch (JSONException e) {
                    _log.error(e.getMessage(), e);
                }
            }
        }
        return rtn;
    }
    
    public static DashboardPortletBossLocal getOne() {
        try {
            return DashboardPortletBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
    }

    public void ejbActivate() throws EJBException, RemoteException {
    }

    public void ejbPassivate() throws EJBException, RemoteException {
    }

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

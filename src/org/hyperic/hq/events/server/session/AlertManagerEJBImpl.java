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

package org.hyperic.hq.events.server.session;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertActionLogLocal;
import org.hyperic.hq.events.shared.AlertActionLogLocalHome;
import org.hyperic.hq.events.shared.AlertActionLogUtil;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogLocal;
import org.hyperic.hq.events.shared.AlertConditionLogLocalHome;
import org.hyperic.hq.events.shared.AlertConditionLogUtil;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertDefinitionLocal;
import org.hyperic.hq.events.shared.AlertDefinitionLocalHome;
import org.hyperic.hq.events.shared.AlertDefinitionPK;
import org.hyperic.hq.events.shared.AlertDefinitionUtil;
import org.hyperic.hq.events.shared.AlertLocal;
import org.hyperic.hq.events.shared.AlertLocalHome;
import org.hyperic.hq.events.shared.AlertPK;
import org.hyperic.hq.events.shared.AlertUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.UserAlertLocal;
import org.hyperic.hq.events.shared.UserAlertLocalHome;
import org.hyperic.hq.events.shared.UserAlertUtil;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * The alert manager.
 *
 * @ejb:bean name="AlertManager"
 *      jndi-name="ejb/events/AlertManager"
 *      local-jndi-name="LocalAlertManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="SUPPORTS"
 */

public class AlertManagerEJBImpl extends SessionEJB implements SessionBean {
    private final String logCtx = AlertManagerEJBImpl.class.getName();
    private final Log log = LogFactory.getLog(logCtx);
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private SessionContext ctx = null;
    private Pager valuePager = null;
    
    private AlertLocalHome ahome = null;
    private AlertLocalHome getAHome() {
        if (ahome == null) {
            try {
                ahome = AlertUtil.getLocalHome();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return ahome;
    }
    
    private UserAlertLocalHome uahome = null;
    private UserAlertLocalHome getUAHome() {
        if (uahome == null) {
            try {
                uahome = UserAlertUtil.getLocalHome();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return uahome;
    }

    public AlertManagerEJBImpl() {}

    /**
     * Create a new alert
     *
     * @return an AlertValue
     *
     * @ejb:interface-method
     * @ejb:transalert-type="REQUIRESNEW"
     */
    public AlertValue createAlert(AlertValue val) throws AlertCreateException {
        try {
            AlertLocal alert = this.getAHome().create(val);
            return alert.getAlertValue();
        } catch (CreateException e) {
            throw new AlertCreateException(e);
        }
    }

    /**
     * Update the alert
     *
     * @return an AlertValue
     *
     * @ejb:interface-method
     * @ejb:transalert-type="REQUIRESNEW"
     */
    public AlertValue updateAlert(AlertValue val) throws AlertCreateException {
        if (!val.idHasBeenSet())
            throw new AlertCreateException("AlertValue ID must be set");
    
        try {
            // Go through the AlertConditionLogs and create them
            Collection clogs = val.getAddedConditionLogs();
            if (clogs.size() > 0) {
                AlertConditionLogLocalHome clogHome;

                try {
                    clogHome = AlertConditionLogUtil.getLocalHome();
                } catch (NamingException e) {
                    throw new SystemException(e);
                }

                for (Iterator it = clogs.iterator(); it.hasNext(); ) {
                    AlertConditionLogValue aclv =
                        (AlertConditionLogValue) it.next();
                    AlertConditionLogLocal clogLocal = clogHome.create(aclv);
                    aclv.setId(clogLocal.getId());
                }
            }
            
            // Go through the AlertActionLogs and create them
            Collection alogs = val.getAddedActionLogs();
            if (alogs.size() > 0) {
                AlertActionLogLocalHome alogHome;
    
                try {
                    alogHome = AlertActionLogUtil.getLocalHome();
                } catch (NamingException e) {
                    throw new SystemException(e);
                }

                for (Iterator it = alogs.iterator(); it.hasNext(); ) {
                    AlertActionLogValue aalv = (AlertActionLogValue) it.next();
                    AlertActionLogLocal alogLocal = alogHome.create(aalv);
                    aalv.setId(alogLocal.getId());
                }
            }
            
            AlertLocal alert =
                this.getAHome().findByPrimaryKey(val.getPrimaryKey());
            
            // Set the values
            alert.setAlertValue(val);
            
            return alert.getAlertValue();
        } catch (CreateException e) {
            throw new AlertCreateException(e);
        } catch (FinderException e) {
            throw new AlertCreateException(e);
        }
    }

    private int deleteAlerts(Collection alerts) throws RemoveException {
        int count = 0;
        for (Iterator i = alerts.iterator(); i.hasNext(); count++) {
            AlertLocal alert = (AlertLocal) i.next();
            alert.remove();
        }
        return count;
    }
    
    /** Remove alerts
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteAlerts(Integer[] ids) throws RemoveException {

        for (int i = 0; i < ids.length; i++) {
            AlertPK pk = new AlertPK(ids[i]);
            try {
                AlertLocal alert = getAHome().findByPrimaryKey(pk);
                alert.remove();
            } catch (FinderException e) {
                // Then we don't have to remove it :-)
            }
        }
    }

    /** Remove alerts for an appdef entity
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public int deleteAlerts(AppdefEntityID id) throws RemoveException {
        // Find the collection of alerts
        try {
            List alerts =
                this.getAHome().findByAppdefEntity(id.getType(), id.getID());
            return deleteAlerts(alerts);
        } catch (FinderException e) {
            // Nothing to delete
            return 0;
        }
    }

    /** Remove alerts for an alert definition
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public int deleteAlerts(Integer defId) throws RemoveException {
        // Find the collection of alerts
        try {
            List alerts = getAHome().findByAlertDefinition(defId);
            return deleteAlerts(alerts);
        } catch (FinderException e) {
            // Nothing to delete
            return 0;
        }
    }

    /** Remove alerts for a range of time
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public int deleteAlerts(long begin, long end) throws RemoveException {
        int count = 0;
        // Find the collection of alerts
        try {
            List alerts = getAHome().findByCreateTime(begin, end);
            // Cap the number of deletions so that we don't time out
            for (Iterator it = alerts.iterator(); it.hasNext() && count < 1000;
                 count++) {
                AlertLocal alert = (AlertLocal) it.next();
                alert.remove();
            }
        } catch (FinderException e) {
            // Nothing to delete
        }
        return count;
    }

    /**
     * Find an alert by ID
     * 
     * @ejb:interface-method
     */
    public AlertValue getById(Integer id) throws AlertNotFoundException {
        AlertLocal ad;
        try {
            ad = getAHome().findByPrimaryKey(new AlertPK(id));
        } catch (FinderException e) {
            throw new AlertNotFoundException(id, e);
        }
        return ad.getAlertValue();
    }

    /**
     * Find an alert by ID and time
     * 
     * @ejb:interface-method
     */
    public AlertValue getByAlertDefAndTime(Integer id, long ctime)
        throws AlertNotFoundException {
        AlertLocal ad;
        try {
            ad = getAHome().findByAlertDefinitionAndCtime(id, ctime);
        } catch (FinderException e) {
            throw new AlertNotFoundException(id, e);
        }
        return ad.getAlertValue();
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public int getAlertCount(AppdefEntityID id) {
        try {
            Collection alerts =
                getAHome().findByAppdefEntity(id.getType(), id.getID());
            return alerts.size();
        } catch (FinderException e) {
            // No alerts found
            return 0;
        }
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public int getAlertCount(Integer alertDefId) {
        try {
            Collection alerts = getAHome().findByAlertDefinition(alertDefId);
            return alerts.size();
        } catch (FinderException e) {
            // No alerts found
            return 0;
        }
    }

    /**
     * Get a collection of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts() {
        ArrayList vals = new ArrayList();

        try {
            Collection alerts = getAHome().findAll();

            for (Iterator i = alerts.iterator(); i.hasNext();) {
                AlertLocal alert = (AlertLocal) i.next();
                vals.add(alert.getAlertValue());
            }
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
        }

        return new PageList(vals, vals.size());
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AppdefEntityID id, PageControl pc) {
        try {
            List alerts;
            if (pc.getSortattribute() == SortAttribute.NAME) {
                alerts =
                    this.getAHome().findByAppdefEntitySortByAlertDef(
                        id.getType(), id.getID());
            } else {
                alerts =
                    this.getAHome().findByAppdefEntity(id.getType(),
                                                       id.getID());
            }

            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(alerts);

            return valuePager.seek(alerts, pc);
        } catch (FinderException e) {
            // No alerts found, just return an empty list, then
            return new PageList();
        }
    }

    /**
     * Get a collection of alerts for an AppdefEntityID and time range
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AppdefEntityID id, long begin, long end,
                               PageControl pc) {
        try {
            List alerts;
            if (pc.getSortattribute() == SortAttribute.NAME) {
                alerts =
                    this.getAHome().findByAppdefEntityInRangeSortByAlertDef(
                        id.getType(), id.getID(), begin, end);
            } else {
                alerts =
                    this.getAHome().findByAppdefEntityInRange(
                        id.getType(), id.getID(), begin, end);
            }

            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(alerts);

            return valuePager.seek(alerts, pc);
        } catch (FinderException e) {
            // No alerts found, just return an empty list, then
            return new PageList();
        }
    }

    /**
     * Search alerts given a set of criteria
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int count, int priority, long timeRange,
                               List includes, PageControl pc) {
        AlertDefinitionLocalHome adHome;
        try {
            adHome = AlertDefinitionUtil.getLocalHome();
        } catch (NamingException e) {
            throw new SystemException(e);
        }

        try {
            // Offset current time by 1 minute to avoid selecting alerts
            // that are currently being modified
            long current = System.currentTimeMillis() - 60000;
            List alerts;
            
            if (priority == EventConstants.PRIORITY_ALL) {
                alerts =
                    getAHome().findByCreateTime(current - timeRange, current);
            }
            else {
                alerts =
                    getAHome().findByCreateTimeAndPriority(current - timeRange,
                                                           current, priority);
            }
            
            // Cache the alert definitions that we'll look up
            IntHashMap alertdefs = new IntHashMap();
            
            ArrayList result = new ArrayList();
            Iterator it = alerts.iterator();
            for (int i = 0; result.size() < count && it.hasNext(); i++) {
                AlertLocal alert = (AlertLocal) it.next();
                
                Integer adId = alert.getAlertDefId();
                AlertDefinitionLocal alertdef = (AlertDefinitionLocal)
                    alertdefs.get(adId.intValue());
                if (alertdef == null) {
                    // We'll have to look it up
                    alertdef =
                        adHome.findByPrimaryKey(new AlertDefinitionPK(adId));
                    alertdefs.put(adId.intValue(), alertdef);
                }
                
                // Filter by appdef entity
                AppdefEntityID aeid =
                    new AppdefEntityID(alertdef.getAppdefType(),
                                       alertdef.getAppdefId());
                if (includes != null && !includes.contains(aeid))
                    continue;
                
                // Finally add it
                result.add(alert);
            }
            
            return valuePager.seek(result, pc);
        } catch (FinderException e) {
            // No alerts found, just return an empty list, then
            return new PageList();
        }
    }

    /**
     * Get a collection of alerts for an alert definition
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(Integer adId, long begin, long end) {
        try {
            List alerts = this.getAHome()
                .findByAlertDefinitionAndCreateTime(adId, begin, end);
            return valuePager.seek(alerts, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            // No alerts found, just return an empty list, then
            return new PageList();
        }
    }

    /**
     * Get a collection of alerts for a Subject
     *
     * @ejb:interface-method
     */
    public List findSubjectAlerts(Integer uid) {
        Vector result = new Vector(2);
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(),
                                        HQConstants.DATASOURCE);
    
            stmt = conn.prepareStatement(
                "SELECT alert_id FROM EAM_USER_ALERT WHERE user_id = ?");
    
            stmt.setInt (1, uid.intValue());
            rs = stmt.executeQuery();

            // Order by date
            long mintime = Long.MAX_VALUE;

            while (rs.next()) {
                AlertPK pk = new AlertPK(new Integer(rs.getInt(1)));
                AlertLocal altLocal;
                try {
                    altLocal = getAHome().findByPrimaryKey(pk);
                } catch (FinderException fe) {
                    // Ignore and continue
                    log.error("Bad user " + uid + " alert " + pk.getId());
                    continue;
                }
                
                AlertValue altVal = altLocal.getAlertValue();
                if (altVal.getCtime() < mintime) {
                    mintime = altVal.getCtime();
                    result.insertElementAt(altVal, 0);
                }
                else
                    result.add(altVal);

            }
        } catch (SQLException e) {
            log.error("SQLException determining if alert definition is enabled",
                      e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
        
        return result;
    }

    /**
     * Add a reference to an alert for a user
     *
     * @ejb:interface-method
     * @ejb:transalert-type="REQUIRESNEW"
     */
    public void addSubjectAlert(Integer uid, Integer aid)
        throws CreateException, FinderException {
        UserAlertLocal ua = null;

        // Find the alert to set
        AlertLocal alert = getAHome().findByPrimaryKey(new AlertPK(aid));

        try {
            Collection alerts = getUAHome().findByUser(uid);
            
            if (alerts.size() < 2) {
                // Create a new reference
                ua = getUAHome().create(uid, alert);
            }
            else {
                Iterator it = alerts.iterator();
                UserAlertLocal ua1 = (UserAlertLocal) it.next();
                UserAlertLocal ua2 = (UserAlertLocal) it.next();
                
                if (ua1.getAlert() == null) {
                    ua = ua1;
                    
                    if (ua2.getAlert() == null)
                        try {
                            ua2.remove();
                        } catch (RemoveException e) {
                            // Leave it, then
                            log.error("User alert for subject " + uid + " has "+
                                      "null alert ID and cannot be removed", e);
                        }
                }
                else if (ua2.getAlert() == null) {
                    ua = ua2;
                }
                else {
                    ua = ua1.getAlert().getCtime() < ua2.getAlert().getCtime() ?
                         ua1 : ua2;
                }                

                // Set the alert                    
                ua.setAlert(alert);
            }
        } catch (FinderException e) {
            // No alerts found, just create a new one, then
            ua = getUAHome().create(uid, alert);
        }
    }

    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }
}

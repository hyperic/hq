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
import java.util.Comparator;
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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.ActionDAO;
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
 * @ejb:transaction type="REQUIRED"
 */
public class AlertManagerEJBImpl extends SessionEJB implements SessionBean {
    private final String _logCtx = AlertManagerEJBImpl.class.getName();
    private final Log    _log = LogFactory.getLog(_logCtx);
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private Pager          valuePager;
    
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

    private AlertDefinitionDAO getAlertDefDAO() {
        return DAOFactory.getDAOFactory().getAlertDefDAO();
    }
    
    private AlertDAO getAlertDAO() {
        return DAOFactory.getDAOFactory().getAlertDAO();
    }
    
    private AlertConditionDAO getAlertConDAO() {
        return DAOFactory.getDAOFactory().getAlertConditionDAO();
    }

    private ActionDAO getActionDAO() {
        return DAOFactory.getDAOFactory().getActionDAO();
    }
    
    private UserAlertDAO getUserAlertDAO() {
        return DAOFactory.getDAOFactory().getUserAlertDAO();
    }

    /**
     * Create a new alert
     *
     * @ejb:interface-method
     */
    public AlertValue createAlert(AlertValue val) {
        AlertDefinition def = getAlertDefDAO().findById(val.getAlertDefId());
        Alert alert = def.createAlert(val);
        
        return alert.getAlertValue();
    }

    /**
     * Update the alert
     *
     * @ejb:interface-method
     */
    public AlertValue updateAlert(AlertValue val) throws AlertCreateException {
        Alert alert;
        
        alert = getAlertDAO().findById(val.getId());
    
        // Go through the AlertConditionLogs and create them
        for (Iterator i = val.getAddedConditionLogs().iterator(); i.hasNext();){
            AlertConditionLogValue aclv = (AlertConditionLogValue) i.next();
            AlertCondition cond = 
                getAlertConDAO().findById(aclv.getCondition().getId());
            
            alert.createConditionLog(aclv.getValue(), cond); 
        }
            
        // Go through the AlertActionLogs and create them
        Collection alogs = val.getAddedActionLogs();
        for (Iterator i = alogs.iterator(); i.hasNext(); ) {
            AlertActionLogValue aalv = (AlertActionLogValue) i.next();
            Action action = getActionDAO().findById(aalv.getActionId());
                
            alert.createActionLog(aalv.getDetail(), action);
        }
            
        return alert.getAlertValue();
    }

    private int deleteAlerts(Collection alerts) {
        AlertDAO aDao = getAlertDAO();
        
        int count = 0;
        for (Iterator i = alerts.iterator(); i.hasNext(); count++) {
            Alert alert = (Alert) i.next();

            aDao.remove(alert);
        }
        return count;
    }
    
    /** Remove alerts
     * @ejb:interface-method
     */
    public void deleteAlerts(Integer[] ids) {
        AlertDAO aDao = getAlertDAO();
        
        for (int i = 0; i < ids.length; i++) {
            Alert a = aDao.findById(ids[i]);

            aDao.remove(a);
        }
    }

    /** 
     * Remove alerts for an appdef entity
     * @ejb:interface-method
     */
    public int deleteAlerts(AppdefEntityID id) {
        return deleteAlerts(getAlertDAO().findByEntity(id));
    }

    /** 
     * Remove alerts for an alert definition
     * @ejb:interface-method
     */
    public int deleteAlerts(Integer defId) throws RemoveException {
        return deleteAlerts(getAlertDefDAO().findById(defId).getAlerts());
    }

    /** 
     * Remove alerts for a range of time
     * @ejb:interface-method
     */
    public int deleteAlerts(long begin, long end) {
        return deleteAlerts(getAlertDAO().findByCreateTime(begin, end));
    }

    /**
     * Find an alert by ID
     * 
     * @ejb:interface-method
     */
    public AlertValue getById(Integer id) {
        return getAlertDAO().findById(id).getAlertValue();
    }

    /**
     * Find an alert by ID and time
     * 
     * @ejb:interface-method
     */
    public AlertValue getByAlertDefAndTime(Integer id, long ctime) {
        AlertDAO aDao = getAlertDAO();
        AlertDefinition def = getAlertDefDAO().findById(id);

        return aDao.findByAlertDefinitionAndCtime(def, ctime).getAlertValue(); 
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public int getAlertCount(AppdefEntityID id) {
        return getAlertDAO().findByEntity(id).size();
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public int getAlertCount(Integer alertDefId) {
        AlertDefinition def = getAlertDefDAO().findById(alertDefId);
        
        return def.getAlerts().size();
    }

    /**
     * Get a collection of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts() {
        Collection res = new ArrayList();

        res = getAlertDAO().findAll();
        for (Iterator i = getAlertDAO().findAll().iterator(); i.hasNext();) {
            Alert alert = (Alert) i.next();

            res.add(alert.getAlertValue());
        }

        return new PageList(res, res.size());
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AppdefEntityID id, PageControl pc) {
        List alerts;

        if (pc.getSortattribute() == SortAttribute.NAME) {
            alerts = getAlertDAO().findByAppdefEntitySortByAlertDef(id);
        } else {
            alerts = getAlertDAO().findByEntity(id);
        }
        
        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(alerts);

        return valuePager.seek(alerts, pc);
    }

    /**
     * Get a collection of alerts for an AppdefEntityID and time range
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AppdefEntityID id, long begin, long end,
                               PageControl pc) 
    {
        AlertDAO aDao = getAlertDAO();
        List alerts;

        if (pc.getSortattribute() == SortAttribute.NAME) {
            alerts = aDao.findByAppdefEntityInRangeSortByAlertDef(id, begin, 
                                                                  end);
        } else {
            alerts = aDao.findByAppdefEntityInRange(id, begin, end);
        }

        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(alerts);

        return valuePager.seek(alerts, pc);
    }

    /**
     * Search alerts given a set of criteria
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int count, int priority, long timeRange,
                               List includes, PageControl pc) 
    {
        AlertDAO aDao = getAlertDAO();
        
        // Offset current time by 1 minute to avoid selecting alerts
        // that are currently being modified
        long current = System.currentTimeMillis() - 60000;
        List alerts;
            
        if (priority == EventConstants.PRIORITY_ALL) {
            alerts = aDao.findByCreateTime(current - timeRange, current);
        } else {
            alerts = aDao.findByCreateTimeAndPriority(current - timeRange,
                                                      current, priority);
        }
            
        List result = new ArrayList();
        Iterator it = alerts.iterator();
        for (int i = 0; result.size() < count && it.hasNext(); i++) {
            Alert alert = (Alert) it.next();
            AlertDefinition alertdef = alert.getAlertDef();
            
            // Filter by appdef entity
            AppdefEntityID aeid = alertdef.getAppdefEntityId();
            if (includes != null && !includes.contains(aeid))
                continue;
                
            // Finally add it
            result.add(alert);
        }
            
        return valuePager.seek(result, pc);
    }

    /**
     * Get a collection of alerts for a Subject
     *
     * @ejb:interface-method
     */
    public List findSubjectAlerts(Integer uid) {
        List alerts = getAlertDAO().findBySubject(uid);
        
        Collections.sort(alerts, new Comparator() {
            public int compare(Object one, Object two) {
                Alert aOne = (Alert)one;
                Alert aTwo = (Alert)two;
                
                if (aOne.getCtime() < aTwo.getCtime())
                    return -1;
                else if (aOne.getCtime() == aTwo.getCtime())
                    return 0;
                return 1;
            }
        });

        List res = new ArrayList(alerts.size());
        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            Alert a = (Alert)i.next();
            
            res.add(a.getAlertValue());
        }
        return res;
    }

    /**
     * Add a reference to an alert for a user
     *
     * @ejb:interface-method
     */
    public void addSubjectAlert(Integer uid, Integer aid)
        throws CreateException, FinderException 
    {
        UserAlertDAO uDao = getUserAlertDAO();

        // Find the alert to set
        Alert alert = getAlertDAO().findById(aid);

        Collection alerts = uDao.findByUser(uid);
            
        if (alerts.size() < 2) {
            // Create a new reference
            alert.createUserAlert(uid);
        } else {
            Iterator i = alerts.iterator();
            UserAlert ua1 = (UserAlert) i.next();
            UserAlert ua2 = (UserAlert) i.next();

            if (ua1.getAlert().getCtime() < ua2.getAlert().getCtime()) {
                uDao.remove(ua1);
            } else {
                uDao.remove(ua2);
            }
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
    public void setSessionContext(SessionContext ctx) {}
}

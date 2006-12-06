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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.server.session.EscalationMediator;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

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
public class AlertManagerEJBImpl extends SessionBase implements SessionBean {
    private final String _logCtx = AlertManagerEJBImpl.class.getName();
    private final Log    _log = LogFactory.getLog(_logCtx);
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private Pager          valuePager;
    
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
    
    /**
     * Create a new alert
     *
     * @ejb:interface-method
     */
    public AlertValue createAlert(AlertValue val) throws PermissionException
    {
        AlertDefinition def = getAlertDefDAO().findById(val.getAlertDefId());
        Alert alert = def.createAlert(val);
        DAOFactory.getDAOFactory().getDAO(alert.getClass())
            .savePersisted(alert);
        return alert.getAlertValue();
    }

    /**
     * Update the alert
     *
     * @ejb:interface-method
     */
    public Alert updateAlert(AlertValue val) throws AlertCreateException {
        Alert alert;
        
        alert = getAlertDAO().findById(val.getId());

        try {
    
            // Go through the AlertConditionLogs and create them
            for (Iterator i = val.getAddedConditionLogs().iterator();
                 i.hasNext();){
                AlertConditionLogValue aclv = (AlertConditionLogValue) i.next();
                AlertCondition cond =
                    getAlertConDAO().findById(aclv.getCondition().getId());
            
                AlertConditionLog log =
                    alert.createConditionLog(aclv.getValue(), cond);
                DAOFactory.getDAOFactory().getDAO(log.getClass())
                    .savePersisted(log);
            }
            
            // Go through the AlertActionLogs and create them
            Collection alogs = val.getAddedActionLogs();
            for (Iterator i = alogs.iterator(); i.hasNext(); ) {
                AlertActionLogValue aalv = (AlertActionLogValue) i.next();
                Action action = getActionDAO().findById(aalv.getActionId());
                
                AlertActionLog log =alert.createActionLog(aalv.getDetail(),
                    action);
                DAOFactory.getDAOFactory().getDAO(log.getClass())
                    .savePersisted(log);
            }
            return alert;
        } catch(PermissionException e){
            throw new AlertCreateException(e);
        }
    }

    /** Remove alerts
     * @ejb:interface-method
     */
    public void deleteAlerts(Integer[] ids) {
        getAlertDAO().deleteByIds(ids);
    }

    /** 
     * Remove alerts for an appdef entity
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubjectValue subj, AppdefEntityID id)
        throws PermissionException {
        canManageAlerts(subj, id);
        return getAlertDAO().deleteByEntity(id);
    }

    /** 
     * Remove alerts for an alert definition
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubjectValue subj, Integer defId)
        throws RemoveException, PermissionException {
        AlertDefinition ad = getAlertDefDAO().findById(defId);
        canManageAlerts(subj, ad);
        return getAlertDAO().deleteByAlertDefinition(defId);
    }

    /** 
     * Remove alerts for a range of time
     * @ejb:interface-method
     */
    public int deleteAlerts(long begin, long end) {
        return getAlertDAO().deleteByCreateTime(begin, end);
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
     * Find an alert pojo by ID
     *
     * @ejb:interface-method
     */
    public Alert findAlertById(Integer id) {
        Alert alert = getAlertDAO().findById(id);
        Hibernate.initialize(alert);
        return alert;
    }

    /**
     * Find an alert by ID and time
     * @throws PermissionException 
     * 
     * @ejb:interface-method
     */
    public AlertValue getByAlertDefAndTime(AuthzSubjectValue subj, Integer id,
                                           long ctime)
        throws PermissionException {
        AlertDAO aDao = getAlertDAO();
        AlertDefinition def = getAlertDefDAO().findById(id);
        canManageAlerts(subj, def);
        return aDao.findByAlertDefinitionAndCtime(def, ctime).getAlertValue(); 
    }

    /**
     * Get the number of alerts for a given appdef entity
     *
     * @ejb:interface-method
     */
    public int getAlertCount(AppdefEntityID id) {
        return getAlertDAO().findByEntity(id).size();
    }

    /**
     * Get a the number of alerts for a given alert definition
     *
     * @ejb:interface-method
     */
    public int getAlertCount(Integer alertDefId) {
        AlertDefinition def = getAlertDefDAO().findById(alertDefId);
    
        return getAlertDAO().countAlerts(def);
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
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AuthzSubjectValue subj, AppdefEntityID id,
                               PageControl pc)
        throws PermissionException {
        canManageAlerts(subj, id);
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
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AuthzSubjectValue subj, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws PermissionException 
    {
        canManageAlerts(subj, id);
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
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AuthzSubjectValue subj, int count, int priority,
                               long timeRange, List includes, PageControl pc) 
        throws PermissionException 
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
            AlertDefinition alertdef = alert.getAlertDefinition();
            
            // Filter by appdef entity
            AppdefEntityID aeid = alertdef.getAppdefEntityId();
            if (includes != null && !includes.contains(aeid))
                continue;

            canManageAlerts(subj, aeid);
            // Finally add it
            result.add(alert);
        }
            
        return valuePager.seek(result, pc);
    }

    /**
     * delegate to EscalationMediator inside JTA context
     * @ejb:interface-method
     */
    public void processEscalation()
    {
        EscalationMediator.getInstance().processEscalation();
    }

    /**
     * delegate to EscalationMediator inside JTA context
     * @ejb:interface-method
     */
    public void dispatchAction(Integer stateId)
    {
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        EscalationState state = dao.findById(stateId);
        EscalationMediator.getInstance().dispatchAction(state);
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

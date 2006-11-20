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
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.escalation.EscalationJob;
import org.hyperic.hq.events.escalation.EscalationMediator;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.ConfigResponse;
import org.hibernate.Hibernate;

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
    
    public AlertManagerEJBImpl() {}

    private AlertDefinitionDAO getAlertDefDAO() {
        return DAOFactory.getDAOFactory().getAlertDefDAO();
    }
    
    private AlertDAO getAlertDAO() {
        return DAOFactory.getDAOFactory().getAlertDAO();
    }

    private EscalationDAO getEscalationDAO() {
        return DAOFactory.getDAOFactory().getEscalationDAO();
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

    /** Remove alerts
     * @ejb:interface-method
     */
    public void deleteAlerts(Integer[] ids) {
        getAlertDAO().deleteByIds(ids);
    }

    /** 
     * Remove alerts for an appdef entity
     * @ejb:interface-method
     */
    public int deleteAlerts(AppdefEntityID id) {
        return getAlertDAO().deleteByEntity(id);
    }

    /** 
     * Remove alerts for an alert definition
     * @ejb:interface-method
     */
    public int deleteAlerts(Integer defId) throws RemoveException {
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
    public Alert getAlertById(Integer id) {
        Alert alert = getAlertDAO().findById(id);
        Hibernate.initialize(alert);
        return alert;
    }

    /**
     * Find an alert pojo by ID
     *
     * @ejb:interface-method
     */
    public Escalation getEscalationById(Integer id) {
        Escalation e = getEscalationDAO().findById(id);
        Hibernate.initialize(e);
        return e;
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
     * schedule action
     * 
     * @param escalationId
     * @param alertId
     *
     * @ejb:interface-method
     */
    public void scheduleAction(Integer escalationId, Integer alertId)
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        Escalation  escalation = dao.findById(escalationId);
        Alert alert =
            DAOFactory.getDAOFactory().getAlertDAO().findById(alertId);
        Integer alertDefId = alert.getAlertDefinition().getId();
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        EscalationState state = sdao.getEscalationState(escalation, alertDefId);
        if (state.isFixed()) {
            // fixed so no need to schedule
            if (log.isInfoEnabled()) {
                log.info("Escalation fixed. alert=" +  alert + ", escalation=" +
                         escalation + ", state=" + state);
            }
            state.setCurrentLevel(0);
            state.setActive(false);
            dao.save(escalation);
            return;
        }
        int nextlevel = state.getCurrentLevel() + 1;
        if (nextlevel >= escalation.getActions().size()) {
            // at the end of escalation chain, so reset and wait for
            //  next alert to fire.  DO NOT schedule next job.
            state.setCurrentLevel(0);
            state.setActive(false);
            dao.save(escalation);
            if (log.isInfoEnabled()) {
                log.info("End escalation. alert=" +  alert + ", escalation=" +
                         escalation + ", state=" + state);
            }
        } else {
            EscalationAction ea =
                escalation.getCurrentAction(state.getCurrentLevel());
            EscalationJob.scheduleJob(
                escalation.getId(), alertId, ea.getWaitTime());
            state.setCurrentLevel(nextlevel);
            dao.save(escalation);
            if (log.isDebugEnabled()) {
                log.debug("schedule next action. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state + "action=" + ea);
            }
        }
    }

    /**
     * run action
     *
     * @param escalationId
     * @param alertId
     *
     * @ejb:interface-method
     */
    public void dispatchAction(Integer escalationId, Integer alertId)
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        Escalation escalation = dao.findById(escalationId);
        Alert alert =
            DAOFactory.getDAOFactory().getAlertDAO().findById(alertId);
        Integer alertDefId = alert.getAlertDefinition().getId();
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        EscalationState state = sdao.getEscalationState(escalation, alertDefId);
        if (state.isFixed()) {
            // fixed or is in progress so no need run
            if (log.isInfoEnabled()) {
                log.info("Escalation fixed. alert=" +  alert + ", escalation=" +
                         escalation + ", state=" + state);
            }
            state.setCurrentLevel(0);
            state.setActive(false);
            dao.save(escalation);
            return;
        }
        // check to see if there is remaining pauseWaitTime
        long remainder = getRemainingPauseWaitTime(escalation, state);
        if (remainder > 0) {
            // reschedule
            EscalationJob.scheduleJob(escalation.getId(), alertId, remainder);
            // reset the pause escalation flag to avoid wait loop.
            state.setPauseEscalation(false);
            dao.save(escalation);
            if (log.isDebugEnabled()) {
                log.debug("Pause for additional wait time. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state);
            }
            return;
        }
        int curlevel = state.getCurrentLevel();
        if (curlevel >= escalation.getActions().size()) {
            throw new IllegalStateException("current level out of bounds: " +
                                            "alert="+ alert + ", escalation=" +
                                            escalation+ ", state=" + state);
        }

        try {
            dispatchAction(escalation, alert, state);
            dao.save(escalation);

            // schedule next action;
            EscalationMediator.getInstance()
                .scheduleAction(escalation.getId(), alertId);
        } catch (ClassNotFoundException e) {
            throw new SystemException(e);
        } catch (IllegalAccessException e) {
            throw new SystemException(e);
        } catch (InstantiationException e) {
            throw new SystemException(e);
        } catch (EncodingException e) {
            throw new SystemException(e);
        } catch (InvalidActionDataException e) {
            throw new SystemException(e);
        } catch (ActionExecuteException e) {
            throw new SystemException(e);
        } catch (AlertCreateException e) {
            throw new SystemException(e);
        }
    }

    private long getRemainingPauseWaitTime(Escalation e, EscalationState s)
    {
        if (e.isAllowPause() && s.isPauseEscalation()) {
            long waitTime =
                e.getCurrentAction(s.getCurrentLevel()).getWaitTime();
            long remainder = s.getPauseWaitTime() - waitTime;

            // remaining pause wait time has to be greater than 1
            // minute to qualify
            return remainder > 60000 ? remainder : 0;
        } else {
            return 0;
        }
    }

    private void dispatchAction(Escalation escalation, Alert alert,
                                EscalationState state)
        throws ClassNotFoundException, IllegalAccessException,
               InstantiationException, EncodingException,
               InvalidActionDataException, ActionExecuteException,
               AlertCreateException
    {
        EscalationAction ea =
            escalation.getCurrentAction(state.getCurrentLevel());
        Action a = ea.getAction();

        if (log.isDebugEnabled()) {
            log.debug("escalation in progress. alert=" +  alert +
                      ", escalation=" + escalation + ", state=" +
                      state + ", action="+ea);
        }
        // prepare, instantiate,  and invoke action
        Class ac = Class.forName(a.getClassName());
        ActionInterface action = (ActionInterface) ac.newInstance();
        action.init(ConfigResponse.decode(action.getConfigSchema(),
                                          a.getConfig()));

        Collection coll = alert.getConditionLog();
        AlertConditionLog[] logs =
            (AlertConditionLog[]) coll.toArray(new AlertConditionLog[0]);

        String detail = action.execute(
            alert.getAlertDefinition(), logs, alert.getId());

        addAlertActionLog(alert, detail);
    }

    private void addAlertActionLog(Alert alert, String detail)
        throws AlertCreateException
    {
        // TODO: this gotta be done with pojos.  not value objects!
        AlertValue alertValue = new AlertValue();
        alertValue.setAlertDefId(alert.getAlertDefinition().getId());

        AlertActionLogValue alog = new AlertActionLogValue();
        alog.setActionId(alert.getId());
        alog.setDetail(detail);
        alertValue.addActionLog(alog);
        
        createAlert(alertValue);
    }

    /**
     * clear active status on all escalation
     *
     * @ejb:interface-method
     */
    public void clearActiveEscalation() {
        DAOFactory.getDAOFactory().getEscalationDAO().clearActiveEscalation();
    }

    /**
     * clear active status for an alertDefinition
     *
     * @ejb:interface-method
     */
    public void clearActiveEscalation(Integer escalationId, Integer alertDefId)
    {
        EscalationDAO dao =
            DAOFactory.getDAOFactory().getEscalationDAO();
        Escalation e = dao.findById(escalationId);
        dao.clearActiveEscalation(e, alertDefId);
    }

    /**
     * get escalationstate
     *
     * @ejb:interface-method
     */
    public EscalationState getEscalationState(Escalation e,
                                              Integer alertDefId) {
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        return dao.getEscalationState(e, alertDefId);
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
            AlertDefinition alertdef = alert.getAlertDefinition();
            
            // Filter by appdef entity
            AppdefEntityID aeid = alertdef.getAppdefEntityId();
            if (includes != null && !includes.contains(aeid))
                continue;
                
            // Finally add it
            result.add(alert);
        }
            
        return valuePager.seek(result, pc);
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

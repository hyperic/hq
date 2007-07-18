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
package org.hyperic.hq.escalation.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.EscalationEvent;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.escalation.shared.EscalationManagerUtil;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.ActionManagerEJBImpl;
import org.hyperic.hq.events.server.session.SessionBase;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;

/**
 * @ejb:bean name="EscalationManager"
 *      jndi-name="ejb/escalation/EscalationManager"
 *      local-jndi-name="LocalEscalationManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class EscalationManagerEJBImpl
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(EscalationManagerEJBImpl.class);

    private final EscalationDAO       _esclDAO;
    private final EscalationStateDAO  _stateDAO;
    
    public EscalationManagerEJBImpl() {
        DAOFactory f = DAOFactory.getDAOFactory();
        
        _esclDAO  = new EscalationDAO(f);
        _stateDAO = new EscalationStateDAO(f);
    }
    
    private void assertEscalationNameIsUnique(String name) 
        throws DuplicateObjectException
    {
        Escalation res;
    
        if ((res = _esclDAO.findByName(name)) != null) {
            throw new DuplicateObjectException("An escalation with that name " +
                                               "already exists", res);
        }
    }

    /**
     * Create a new escalation chain
     * 
     * @see Escalation for information on fields
     * @ejb:interface-method  
     */
    public Escalation createEscalation(String name, String description,
                                       boolean pauseAllowed, long maxWaitTime,
                                       boolean notifyAll) 
        throws DuplicateObjectException
    {
        Escalation res;
        
        assertEscalationNameIsUnique(name);
        res = new Escalation(name, description, pauseAllowed, maxWaitTime,
                             notifyAll); 
                              
        _esclDAO.save(res);
        return res;
    }

    /**
     * Update an escalation chain
     * 
     * @see Escalation for information on fields
     * @ejb:interface-method  
     */
    public void updateEscalation(AuthzSubject subject, Escalation esc,
                                 String name, String description,
                                 boolean pauseAllowed, long maxWaitTime,
                                 boolean notifyAll) 
        throws DuplicateObjectException, PermissionException
    {
        SessionBase.canModifyEscalation(subject.getId());
        
        if (!esc.getName().equals(name)) {
            assertEscalationNameIsUnique(name);
        }
        
        esc.setName(name);
        esc.setDescription(description);
        esc.setPauseAllowed(pauseAllowed);
        esc.setMaxPauseTime(maxWaitTime);
        esc.setNotifyAll(notifyAll);
    }

    private void unscheduleEscalation(Escalation e) {
        Collection states = _stateDAO.findStatesFor(e);
        
        // Unschedule any escalations currently in progress
        for (Iterator i=states.iterator(); i.hasNext(); ) {
            endEscalation((EscalationState) i.next());
        }
    }

    /**
     * Add an action to the end of an escalation chain.  Any escalations
     * currently in progress using this chain will be canceled.
     * 
     * @ejb:interface-method  
     */
    public void addAction(Escalation e, ActionConfigInterface cfg, 
                          long waitTime) 
    {
        Action a = ActionManagerEJBImpl.getOne().createAction(cfg);
        e.addAction(waitTime, a);
        unscheduleEscalation(e);
    }

    /**
     * Remove an action from an escalation chain.  Any escalations
     * currently in progress using this chain will be canceled.
     * 
     * @ejb:interface-method  
     */
    public void removeAction(Escalation e, Integer actId) { 
        // Iterate through the actions and find the one escalation action
        Action action = null;
        for (Iterator it = e.getActionsList().iterator(); it.hasNext(); ) {
            EscalationAction ea = (EscalationAction) it.next();
            if (ea.getAction().getId().equals(actId)) {
                action = ea.getAction();
                it.remove();
                break;
            }
        }
        
        if (action == null) {
            return;
        }

        unscheduleEscalation(e);
        
        ActionManagerEJBImpl.getOne().markActionDeleted(action);
    }

    /**
     * Delete an escalation chain.  This method will throw an exception if
     * the escalation chain is in use.
     * 
     * TODO:  Probably want to allow for the fact that people DO want to delete
     *        while states exist.
     *        
     * @ejb:interface-method  
     */
    public void deleteEscalation(AuthzSubject subject, Escalation e) 
        throws PermissionException
    {
        SessionBase.canRemoveEscalation(subject.getId());
        _esclDAO.remove(e);
    }
    
    /**
     * @ejb:interface-method  
     */
    public Escalation findById(Integer id) {
        return _esclDAO.findById(id);
    }
    
    /**
     * @ejb:interface-method  
     */
    public Escalation findById(AuthzSubject subject, Integer id) 
        throws PermissionException
    {
        return _esclDAO.findById(id);
    }

    /**
     * @ejb:interface-method  
     */
    public Collection findAll(AuthzSubject subject)
        throws PermissionException
    {
        return _esclDAO.findAllOrderByName();
    }

    /**
     * @ejb:interface-method  
     */
    public Escalation findByName(AuthzSubject subject, String name) 
        throws PermissionException
    {
        return _esclDAO.findByName(name);
    }

    /**
     * @ejb:interface-method  
     */
    public Escalation findByName(String name) {
        return _esclDAO.findByName(name);
    }

    /**
     * Start an escalation.  If the escalation has already been started, then
     * this method call will be a no-op.
     * 
     * @param def     Definition to start the escalation for
     * @param creator Object which will create an {@link Escalatable} object
     *                if invoking this method actually starts an escalation.
     *                 
     * @ejb:interface-method  
     */
    public void startEscalation(PerformsEscalations def, 
                                EscalatableCreator creator)
    {
        EscalationState curState = _stateDAO.find(def);
        Escalatable alert;

        if (def.getEscalation() == null) 
            return;
        
        if (curState != null) {
            _log.debug("startEscalation called on [" + def + "] but it was " +
                       "already running");
            return;  
        }
        
        alert    = creator.createEscalatable();
        curState = new EscalationState(alert);
        _stateDAO.save(curState);
        _log.debug("Escalation started: state=" + curState.getId());
        EscalationRuntime.getInstance().scheduleEscalation(curState);
    }
     
    /**
     * @ejb:interface-method  
     */
    public Escalatable getEscalatable(EscalationState s) {
        return s.getAlertType().findEscalatable(new Integer(s.getAlertId())); 
    }
    
    /**
     * End an escalation.  This will remove all state for the escalation
     * tied to a specific definition.
     *  
     * @ejb:interface-method  
     */
    public void endEscalation(PerformsEscalations def) {
        EscalationState curState = _stateDAO.find(def);
        
        if (curState == null)
            return; // Already ended
        
        endEscalation(curState);
    }

    private void endEscalation(EscalationState state) {
        _stateDAO.remove(state);
        EscalationRuntime.getInstance().unscheduleEscalation(state);
    }
    
    /**
     * This method is only for internal use by the {@link EscalationRuntime}.
     * It ensures that we have a session setup prior to executing any actions.
     * 
     * This method executes the action pointed at by the state, determines
     * the next stage of the escalation and (optionally) ends it, thus
     * unscheduling any further executions.
     * 
     * @ejb:interface-method  
     */
    public void executeState(Integer stateId) {
        EscalationState s = _stateDAO.findById(stateId);
        Escalation e = s.getEscalation();
        EscalationAction eAction;
        Action action;
        int actionIdx = s.getNextAction();
        
        // XXX -- Need to make sure the application is running before
        //        we allow this to proceed
        _log.debug("Executing state[" + s.getId() + "]");
        if (actionIdx >= e.getActions().size()) {
            _log.debug("Reached the end of the escalation state[" + 
                       s.getId() + "].  Ending it");
            endEscalation(s);
            return;
        }
        
        eAction = (EscalationAction)e.getActions().get(actionIdx);
        action = eAction.getAction();

        Escalatable esc = getEscalatable(s);

        // Always make sure that we increase the state offset of the
        // escalation so we don't loop fo-eva 
        long nextTime = System.currentTimeMillis() + eAction.getWaitTime();
            
        _log.debug("Moving onto next state of escalation, but chillin' for "
                   + eAction.getWaitTime() + " ms");
        s.setNextAction(actionIdx + 1);
        s.setNextActionTime(nextTime);
        s.setAcknowledgedBy(null);
        
        EscalationRuntime.getInstance().scheduleEscalation(s);
        
        try {
            ActionExecutionInfo execInfo = 
                new ActionExecutionInfo(esc.getShortReason(),
                                        esc.getLongReason(),
                                        esc.getAuxLogs());
            
            String detail = action.executeAction(esc.getAlertInfo(), execInfo);
            
            s.getAlertType().logActionDetails(esc.getId(), action, detail, 
                                              null);
        } catch(Exception exc) {
            _log.error("Unable to execute action [" + 
                       action.getClassName() + "] for escalation definition [" +
                       s.getEscalation().getName() + "]", exc);
        }
    }
    
    /**
     * Find an escalation based on the type and ID of the definition.  
     * 
     * @return null if the definition defined by the ID does not have any
     *         escalation associated with it
     * 
     * @ejb:interface-method  
     */
    public Escalation findByDefId(EscalationAlertType type, Integer defId) {
        return type.findDefinition(defId).getEscalation();
    }

    /**
     * Set the escalation for a given alert definition and type
     * 
     * @ejb:interface-method  
     */
    public void setEscalation(EscalationAlertType type, Integer defId,
                              Escalation escalation) 
    {
        type.setEscalation(defId, escalation);
    }

    /**
     * Acknowledge an alert, potentially sending out notifications.
     * 
     * @param subject Person who acknowledged the alert
     * 
     * @ejb:interface-method  
     */
    public void acknowledgeAlert(AuthzSubject subject, EscalationAlertType type, 
                                 Integer alertId, String moreInfo)  
                                 
    { 
        fixOrNotify(subject, type, alertId, false, moreInfo);
    }
    
    /**
     * See if an alert is acknowledgeable
     * 
     * @return true if the alert is currently acknowledgeable
     * 
     * @ejb:interface-method
     */
    public boolean isAlertAcknowledgeable(Integer alertId,
                                          PerformsEscalations def) {
        if (def.getEscalation() != null) {
            EscalationState escState = _stateDAO.find(def);
            
            if (escState != null) {
                if (escState.getAlertId() == alertId.intValue() &&
                    escState.getAcknowledgedBy() == null) {
                    return true;
                }
            }            
        }
        return false;
    }
    
    /**
     * Fix an alert for a an escalation if there is one currently running.
     *
     * @return true if there was an alert to be fixed.
     * 
     * @ejb:interface-method
     */
    public boolean fixAlert(AuthzSubject subject, PerformsEscalations def,
                            String moreInfo) 
    {
        EscalationState state = _stateDAO.find(def);
        Escalatable e;
        
        if (state == null)
            return false;
        
        // Find the alert, to see if it's been fixed.
        Integer alertId = new Integer(state.getAlertId());
        e = state.getAlertType().findEscalatable(alertId);
        
        // Strange condition, since we shouldn't have an escalation state if
        // it has been fixed.
        if (e.getAlertInfo().isFixed()) {
            _log.warn("Found a fixed alert inside an escalation.  alert=" + 
                      alertId + " defid=" + def.getDefinitionInfo().getId() + 
                      " alertType=" + state.getAlertType().getCode());
            return false;
        }
        
        fixOrNotify(subject, e, state, state.getAlertType(), true, moreInfo);
        return true;
    }
    
    /**
     * Fix an alert, potentially sending out notifications.  The state of
     * the escalation will be terminated and the alert will be marked fixed.
     * 
     * @param subject Person who fixed the alert
     * 
     * @ejb:interface-method  
     */
    public void fixAlert(AuthzSubject subject, EscalationAlertType type, 
                         Integer alertId, String moreInfo)
    { 
        fixOrNotify(subject, type, alertId, true, moreInfo);
    } 
    
    private void fixOrNotify(AuthzSubject subject, EscalationAlertType type, 
                             Integer alertId, boolean fixed, String moreInfo)
    {
        Escalatable esc = type.findEscalatable(alertId);
        EscalationState state = _stateDAO.find(esc);
        fixOrNotify(subject, esc, state, type, fixed, moreInfo);
    } 

    private void fixOrNotify(AuthzSubject subject, Escalatable esc,
                             EscalationState state, EscalationAlertType type,
                             boolean fixed, String moreInfo) 
    {
        Integer alertId = esc.getAlertInfo().getId();
        boolean acknowledged = !fixed;
        
        if (esc.getAlertInfo().isFixed()) {
            _log.warn(subject.getFullName() + " attempted to fix or " +
                      " acknowledge the " + type + " id=" + alertId + 
                      " but it was already fixed"); 
            return;
        }
        
        if (state == null && acknowledged) {
            _log.debug(subject.getFullName() + " acknowledged alertId[" + 
                       alertId + "] for type [" + type + "], but it wasn't " +
                       "running or was previously acknowledged.  " + 
                       "Button Masher?");
            return;
        }
        
        if (fixed) {  
            if (moreInfo == null)
                moreInfo = "(Fixed by " + subject.getFullName() + ")";
            
            _log.debug(subject.getFullName() + " has fixed alertId=" + alertId);
            type.changeAlertState(alertId, subject,
                                  EscalationStateChange.FIXED); 
            type.logActionDetails(alertId, null, moreInfo, subject);
            if (state != null)
                endEscalation(state);
        } else {
            if (moreInfo == null)
                moreInfo = "";
            
            if (state.getAcknowledgedBy() != null) {
                _log.warn(subject.getFullName() + " attempted to acknowledge "+
                          type + " alert=" + alertId + " but it was already "+
                          "acknowledged by " + 
                          state.getAcknowledgedBy().getFullName());
                return;
            }
            _log.debug(subject.getFullName() + " has acknowledged alertId=" + 
                       alertId);
            type.changeAlertState(alertId, subject, 
                                  EscalationStateChange.ACKNOWLEDGED);
            type.logActionDetails(alertId, null, 
                                  subject.getFullName() + " acknowledged " +
                                  "the alert" + moreInfo, subject);
                                  
            state.setAcknowledgedBy(subject);
        }

        if (state != null)
            sendNotifications(state, esc, subject, 
                              state.getEscalation().isNotifyAll(), fixed,
                              moreInfo);
    }

    /**
     * Send an acknowledge or fixed notification to the actions.
     *  
     * @param state     State specifying the escalation chain to use
     * @param notifyAll If false, only send to previously executed actions.
     */
    private void sendNotifications(EscalationState state, Escalatable alert,
                                   AuthzSubject subject, boolean notifyAll, 
                                   boolean fixed, String moreInfo)
    {
        String msg = subject.getFullName() + " has " + 
            (fixed ? "fixed" : "acknowledged") + " the alert raised by [" +
            alert.getDefinition().getName() + "] " + moreInfo;

        List actions = state.getEscalation().getActions();
        int idx = (notifyAll ? actions.size() : state.getNextAction()) - 1;

        while (idx >= 0) {
            EscalationAction ea = (EscalationAction)actions.get(idx--);
            Action a = (Action)ea.getAction();
            
            try {
                Class c = Class.forName(a.getClassName());
                Notify n;
                
                if (!Notify.class.isAssignableFrom(c))
                    continue;
                
                n = (Notify)a.getInitializedAction();
                n.send(alert, fixed ? EscalationStateChange.FIXED :
                                      EscalationStateChange.ACKNOWLEDGED, msg);
            } catch(Exception e) {
                _log.warn("Unable to send notification alert", e);
            }
        }
        
        // Send event to be logged
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC,
                              new EscalationEvent(alert, msg));
    }
    
    /**
     * Re-order the actions for an escalation.   If there are any states
     * associated with the escalation, they will be cleared.
     * 
     * @param actions a list of {@link EscalationAction}s (already contained
     *                within the escalation) specifying the new order.
     * @ejb:interface-method  
     */
    public void updateEscalationOrder(Escalation esc, List actions) {
        if (actions.size() != esc.getActions().size())
            throw new IllegalArgumentException("Actions size must be the same");
        
        for (Iterator i=actions.iterator(); i.hasNext(); ) {
            EscalationAction a = (EscalationAction)i.next();
            
            if (esc.getAction(a.getAction().getId()) == null) {
                throw new IllegalArgumentException("Action id=" + 
                                                   a.getAction().getId() +
                                                   " not found");
            }
        }
        esc.setActionsList(actions);

        unscheduleEscalation(esc);
    }

    /**
     * @ejb:interface-method  
     */
    public List getActiveEscalations(int maxEscalations) {
        return _stateDAO.getActiveEscalations(maxEscalations);
    }
    
    /**
     * @ejb:interface-method
     */
    public String getLastFix(PerformsEscalations def) {
        if (def != null) {
            EscalationAlertType type = def.getAlertType();
            return type.getLastFixedNote(def);
        }
        return null;
    }

    
    /**
     * @ejb:interface-method  
     */
    public void startup() {
        _log.info("Starting up Escalation subsystem");
        for (Iterator i=_stateDAO.findAll().iterator(); i.hasNext(); ) {
            EscalationState state = (EscalationState)i.next();
            
            _log.info("Loading escalation state [" + state.getId() + "]");
            EscalationRuntime.getInstance().scheduleEscalation(state);
        }
    }
        
    public static EscalationManagerLocal getOne() {
        try {
            return EscalationManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}

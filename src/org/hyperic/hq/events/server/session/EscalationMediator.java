package org.hyperic.hq.events.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.LockSet;
import org.hyperic.hq.Mediator;
import org.hyperic.hq.TransactionContext;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionManagerLocal;
import org.hyperic.hq.common.shared.TransactionManagerUtil;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.Notify;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.mbean.EscalationSchedulerMBean;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertActionLog;
import org.hyperic.hq.galerts.server.session.GalertActionLogDAO;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefDAO;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GalertLogDAO;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.json.JSONException;
import org.json.JSONObject;

public class EscalationMediator extends Mediator
{
    private static final int DEFAULT_LOCKSET_SIZE = 1024;
    private static final LockSet stateLocks=new LockSet(DEFAULT_LOCKSET_SIZE);
    private static Log log = LogFactory.getLog(EscalationMediator.class);

    private static String ESCALATION_SERVICE_MBEAN =
        "hyperic.jmx:type=Service,name=EscalationService";

    private static EscalationMediator instance = new EscalationMediator();

    public static EscalationMediator getInstance()
    {
        return instance;
    }

    private AlertDefinitionManagerLocal alertDefManagerLocal;
    private AlertManagerLocal alertManagerLocal;
    private TransactionManagerLocal transactionManager;
    private EscalationSchedulerMBean escalationServiceMBean;
    private GalertManagerLocal galertManagerLocal;

    protected EscalationMediator()
    {
        try {
            alertDefManagerLocal =
                AlertDefinitionManagerUtil.getLocalHome().create();
            alertManagerLocal = AlertManagerUtil.getLocalHome().create();
            transactionManager = TransactionManagerUtil.getLocalHome().create();
            galertManagerLocal = GalertManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        try {
            ObjectName name = new ObjectName(ESCALATION_SERVICE_MBEAN);
            escalationServiceMBean = (EscalationSchedulerMBean)
                MBeanUtil.getMBeanServer().getAttribute(name, "Instance");
        } catch (MalformedObjectNameException e) {
            throw new SystemException(e);
        } catch (ReflectionException e) {
            throw new SystemException(e);
        } catch (InstanceNotFoundException e) {
            throw new SystemException(e);
        } catch (MBeanException e) {
            throw new SystemException(e);
        } catch (AttributeNotFoundException e) {
            throw new SystemException(e);
        }
    }

    /**
     * run escalation process.  The must be invoked within JTA context.
     */
    public void processEscalation()
    {
        // get all scheduled escalations
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        List states = dao.findScheduledEscalationState();
        if (log.isDebugEnabled()) {
            log.debug("Found " + states.size() + " scheduled escalations.");
        }
        for (Iterator s = states.iterator(); s.hasNext(); ) {
            final EscalationState state = (EscalationState)s.next();
            if (log.isDebugEnabled()) {
                log.debug("EscalationState: "+state);
            }
            escalationServiceMBean.run(new Runnable() {
                public void run()
                {
                    alertManagerLocal.dispatchAction(state.getId());
                }
            });
        }
    }

    /**
     * escalation entry point
     * @param event TODO
     *
     * @throws PermissionException 
     */
    public void startEscalation(Integer alertDefId, TriggerFiredEvent event)
        throws ActionExecuteException, PermissionException
    {
        AlertDefinition alertDef;
    
        try {
            alertDef = alertDefManagerLocal.getByIdNoCheck(alertDefId);
        } catch (FinderException e) {
            throw new ActionExecuteException("Bad alert definition ID: " +
                                             alertDefId, e);
        }
        
        Escalation esc = alertDef.getEscalation();
        
        if (isEscalationActive(esc, alertDef.getId(),
                               EscalationState.ALERT_TYPE_CLASSIC)) {
            return;
        }
    
        Alert alert = createAlert(event, alertDef);
        activateEscalation(esc, alertDefId, alert.getId().intValue(),
                           EscalationState.ALERT_TYPE_CLASSIC);
    }

    /**
     * escalation entry point for Galert
     * TODO: There needs to be a event correlation module
     * and the entry to the escalation/workflow should come from
     * the event correlation module and not Galert.
     *
     * @throws PermissionException 
     */
    public void startGEscalation(GalertDef def, ExecutionReason reason)
        throws ActionExecuteException, PermissionException
    {
        Escalation esc = def.getEscalation();
        
        if (isEscalationActive(esc, def.getId(),
                               EscalationState.ALERT_TYPE_GROUP)) {
            return;
        }

        GalertLog alert = galertManagerLocal.createAlertLog(def, reason);
        activateEscalation(esc, def.getId(), alert.getId().intValue(),
                           EscalationState.ALERT_TYPE_GROUP);
    }

    private Alert createAlert(TriggerFiredEvent event, AlertDefinition alertDef)
        throws PermissionException, ActionExecuteException {
        // Start the Alert object
        AlertValue alert = new AlertValue();
        alert.setAlertDefId(alertDef.getId());

        // Create time is the same as the fired event
        alert.setCtime(event.getTimestamp());

        // Now create the alert
        alert = alertManagerLocal.createAlert(alert);

        // Create the trigger event map
        HashMap trigMap = new HashMap();
        TriggerFiredEvent[] tfes = event.getRootEvents();
        for (int i = 0; i < tfes.length; i++) {
            trigMap.put(tfes[i].getInstanceId(), tfes[i]);
        }
    
        // Create a alert condition logs for every condition
        Collection conds = alertDef.getConditions();
        for (Iterator it = conds.iterator(); it.hasNext(); ) {
            AlertCondition cond = (AlertCondition) it.next();
            AlertConditionLogValue clog = new AlertConditionLogValue();
            clog.setCondition(cond.getAlertConditionValue());
            if (trigMap.containsKey(cond.getTrigger().getId())) {
                clog.setValue(trigMap.get(
                    cond.getTrigger().getId()).toString());
            } 
            alert.addConditionLog(clog);
        }
    
        // Update the alert
        Alert alertpojo;
        try {
            // get alert pojo so retrieve array of AlertCondtionLogs
            alertpojo = alertManagerLocal.updateAlert(alert);
        } catch (AlertCreateException e) {
            throw new ActionExecuteException("Unable to update alert: " +
                                             alert.getId(), e);
        }
        
        // Regardless of whether or not the actions succeed, we will send an
        // AlertFiredEvent
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC,
                              new AlertFiredEvent(event, alert.getId(),
                                                  alertDef));

        Collection actions = alertDef.getActions();
        // Iterate through the actions
        for (Iterator it = actions.iterator(); it.hasNext(); ) {
            Action act = (Action) it.next();
            executeAction(EscalationState.ALERT_TYPE_CLASSIC, alertpojo, act);
        }
        
        return alertpojo;
    }

    /**
     */
    private boolean isEscalationActive(Escalation e,
                                       Integer alertDefId,
                                       int alertType)
    {
        if (e != null ) {
            EscalationState state = getEscalationState(e, alertDefId, alertType);
            if (state != null && state.isActive()) {
                // escalation is active, so do not start another escalation
                // for this chain.
                if (log.isDebugEnabled()) {
                    log.debug("Escalation already in progress. alert def ID=" +
                              alertDefId + ", escalation=" + e +
                              ", state="+ state);
                }
                return true;
            }
        }
        
        return false;
    }

    /**
     * set escalation state to active and take ownership of the escalation
     * chain. The caller is guaranteed that no other thread will have access to
     * this escalation chain.
     * 
     * This method is not cluster-safe.
     * 
     * @param e
     * @param alertDefId
     * @return true if escalation state changed from inactive to active. I.e,
     *         the caller now owns the escalation chain. false escalation chain
     *         is already in progress.
     */
    private void activateEscalation(final Escalation e,
                                    final Integer alertDefId,
                                    final int alertId,
                                    final int alertType)
    {
        if (e == null) {
            return;
        }
        
        synchronized(stateLocks.getLock(new StateLock(alertDefId.intValue(),
                                                      alertType)))
        {
            // the point of this is to make sure people wait
            // while we check on the active bit in a new JTA context
            // so by the time the transaction is over
            // we are sure the update is available.
            //
            // The code is all here for maintainability(?), sigh.
            TransactionContext context =
                transactionManager.executeReqNew(new TransactionContext() {
                    public TransactionContext run(TransactionContext context)
                    {
                        EscalationState state =
                            getEscalationState(e, alertDefId, alertType);
                        if (state == null) {
                            state = EscalationState.newInstance(
                                e, alertDefId, alertType);
                            DAOFactory.getDAOFactory().getEscalationStateDAO()
                                .save(state);
                        }
                        else if(state.isActive()) {
                            return null;
                        }
                        state.setActive(true);
                        state.setAlertId(alertId);
                        DAOFactory.getDAOFactory().getEscalationStateDAO()
                            .save(state);
                        return context;
                    }
                });
            if (context == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Escalation already in progress. alert=" +
                              alertId + ", escalation=" + e);
                }
                return;
            }
        }
        beginEscalation(e, alertDefId, alertType);
    }

    public void saveEscalation(Integer subjectId, Integer alertDefId,
                               int alertType, JSONObject escalation)
        throws JSONException, PermissionException
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        Escalation e = Escalation.newInstance(subjectId, escalation);
        if (e.getId() == null) {
            // save escalation
            dao.save(e);
        } else {
            // merge updated values
            dao.merge(e);
        }

        if (alertDefId != null) {
            // save escalation with alert definition
            switch(alertType) {
            case EscalationState.ALERT_TYPE_CLASSIC:
                AlertDefinitionDAO adao =
                    DAOFactory.getDAOFactory().getAlertDefDAO();
                AlertDefinition adef = adao.findById(alertDefId);
                adef.setEscalation(e);
                adao.save(adef);
                break;
            case EscalationState.ALERT_TYPE_GROUP:
                GalertDefDAO gdao
                    = DAOFactory.getDAOFactory().getGalertDefDAO();
                GalertDef gdef = gdao.findById(alertDefId);
                gdef.setEscalation(e);
                gdao.save(gdef);
                break;
            default:
                log.error("alertType " + alertType + " unknown");
                break;
            }
        }
    }

    public Escalation findEscalationById(Escalation e)
    {
        return findEscalationById(e.getId());
    }

    public Escalation findEscalationById(Integer id)
    {
        return DAOFactory.getDAOFactory().getEscalationDAO().findById(id);
    }

    public void deleteEscalationByName(Integer subjectId, String name)
        throws PermissionException
    {
        DAOFactory.getDAOFactory().getEscalationDAO()
            .deleteByName(subjectId, name);
    }

    public void deleteEscalationById(Integer subjectId, Integer[] ids)
        throws PermissionException
    {
        DAOFactory.getDAOFactory().getEscalationDAO()
            .deleteById(subjectId, ids);
    }

    public Collection findAll(Integer subjectId) throws PermissionException
    {
        return DAOFactory.getDAOFactory().getEscalationDAO().findAll(subjectId);
    }

    public Escalation findByEscalationName(Integer subjectId, String name)
        throws PermissionException
    {
        Escalation e = Escalation.newSearchable(subjectId, name);
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        return (Escalation)dao.findPersisted(e);
    }

    public Escalation findEscalationByAlertDefId(Integer subjectId, Integer id)
        throws PermissionException
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        return dao.findByAlertDefinitionId(subjectId, id);
    }

    public Escalation findByEscalationId(Integer subjectId, Integer id)
        throws PermissionException
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        return dao.findById(subjectId, id);
    }

    public EscalationState getEscalationState(Escalation e, Integer alertDefId,
                                              int alertType)
    {
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        return dao.getEscalationState(e, alertDefId, alertType);
    }

    private AlertInterface getAlert(int alertType, int alertId) {
        switch(alertType) {
        case EscalationState.ALERT_TYPE_CLASSIC:
            AlertDAO dao = DAOFactory.getDAOFactory().getAlertDAO();
            return dao.findById(new Integer(alertId));
        case EscalationState.ALERT_TYPE_GROUP:
            GalertLogDAO gdao = DAOFactory.getDAOFactory().getGalertLogDAO();
            return gdao.findById(new Integer(alertId));
        default:
            log.error("alertType " + alertType + " unknown");
            return null;            // Unknown alert type, can't do anything
        }

    }
    
    public void acknowledgeAlert(Integer subjectID, Integer alertID,
                                 long pauseWaitTime)
        throws PermissionException, ActionExecuteException
    {
        setEscalationAcknowledged(subjectID, alertID,
                                  EscalationState.ALERT_TYPE_CLASSIC,
                                  pauseWaitTime);
    }

    public void acknowledgeGalert(Integer subjectID, Integer alertID,
                                  long pauseWaitTime)
        throws PermissionException, ActionExecuteException
    {
        setEscalationAcknowledged(subjectID, alertID,
                                  EscalationState.ALERT_TYPE_GROUP,
                                  pauseWaitTime);
    }

    private void setEscalationAcknowledged(Integer subjectID, Integer alertID,
                                           int alertType, long pauseWaitTime)
        throws PermissionException, ActionExecuteException
    {
        EscalationState state =
            setEscalationState(subjectID, alertID, alertType, false,
                               pauseWaitTime);
        if (state != null) {
            AuthzSubject subj =
                DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                    .findById(subjectID);
            // TODO: refine message content
            StringBuffer message = new StringBuffer()
                .append(subj.getFirstName())
                .append(" ")
                .append(subj.getLastName())
                .append(" has acknowledged the ");
            
            switch (state.getAlertType()) {
            case EscalationState.ALERT_TYPE_GROUP:
                message.append("group alert");
                break;
            case EscalationState.ALERT_TYPE_CLASSIC:
            default:
                message.append("alert");
                break;
            }

            message.append(". The alert id number is ")
                   .append(alertID)
                   .append(".");
            logEscalation(null, state,
                          "Alert acknowledged by " + state.getUpdateBy());
            notifyEscalation(alertID, state, message.toString());
        }
    }
    
    /**
     * Mark the alert associated with the alert definition and its escalation
     * as fixed
     * @param subjectId the subject (should be overlord)
     * @param alertDefId the alert definition ID
     * @throws ActionExecuteException if alert definition ID is invalid
     * @throws PermissionException should never happen
     */
    public void fixAlertDefinition(Integer subjectId, Integer alertDefId)
        throws ActionExecuteException, PermissionException {
        AlertDefinition alertDef;
    
        try {
            alertDef = alertDefManagerLocal.getByIdNoCheck(alertDefId);
        } catch (FinderException e) {
            throw new ActionExecuteException("Bad alert definition ID: " +
                                             alertDefId, e);
        }

        if (alertDef.getEscalation() != null) {
            EscalationState state =
                getEscalationState(alertDef.getEscalation(), alertDef.getId(),
                                   EscalationState.ALERT_TYPE_CLASSIC);
            
            fixAlert(subjectId, new Integer(state.getAlertId()));
        }
    }

    public void fixAlert(Integer subjectID, Integer alertID)
        throws PermissionException, ActionExecuteException
    {
        setEscalationFixed(subjectID, alertID,
                           EscalationState.ALERT_TYPE_CLASSIC);
    }

    public void fixGalert(Integer subjectID, Integer alertID)
        throws PermissionException, ActionExecuteException
    {
        setEscalationFixed(subjectID, alertID,
                           EscalationState.ALERT_TYPE_GROUP);
    }

    private void setEscalationFixed(Integer subjectID, Integer alertID,
                                    int alertType)
        throws ActionExecuteException, PermissionException
    {
        EscalationState state =
            setEscalationState(subjectID, alertID, alertType, true, 0);

        if (state != null) {
            AuthzSubject subj = DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                    .findById(subjectID);
            // TODO: refine message content
            StringBuffer message = new StringBuffer()
                .append(subj.getFirstName())
                .append(" ")
                .append(subj.getLastName())
                .append(" has fixed the ");

            switch (state.getAlertType()) {
            case EscalationState.ALERT_TYPE_GROUP:
                message.append("group alert");
                break;
            case EscalationState.ALERT_TYPE_CLASSIC:
            default:
                message.append("alert");
                break;
            }

            message.append(". The alert id number is ").append(alertID)
                   .append(".");

            logEscalation(null, state, "Alert fixed by " + state.getUpdateBy());
            notifyEscalation(alertID, state, message.toString());
        }
    }

    private void notifyEscalation(Integer alertId,
                                  EscalationState state,
                                  String message)
        throws ActionExecuteException
    {
        Escalation e = state.getEscalation();
        // get the correct level to notify
        int level = e.isNotifyAll()
            ? state.getCurrentLevel()
            : e.getActions().size() - 1;
        while (level >= 0) {
            EscalationAction ea =
                (EscalationAction)e.getActions().get(level);
            if (ea.getAction() instanceof Notify) {
                ((Notify)ea.getAction()).send(alertId, message);
            }
            level--;
        }
    }

    private EscalationState setEscalationState(Integer subjectID,
                                               Integer alertId,
                                               int alertType,
                                               boolean fixed,
                                               long pauseWaitTime)
        throws PermissionException
    {
        SessionBase.canModifyEscalation(subjectID);

        Escalation escalation;
        Integer alertDefId;
        AlertInterface alert = getAlert(alertType, alertId.intValue());
        alertDefId = alert.getAlertDefinitionInterface().getId();
        escalation = alert.getAlertDefinitionInterface().getEscalation();

        if (escalation != null) {
            AuthzSubject subject =
                DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                    .findById(subjectID);
            synchronized(stateLocks.getLock(
                new StateLock(alertDefId.intValue(), alertType))) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId, alertType);
                if (state == null) {
                    log.error("Can't find escalation state. escalation="+
                              escalation + ", alertDefId="+alertDefId+
                              ", alertType=" + alertType);
                    return null;
                }
                if (fixed) {
                    if(state.isActive() &&
                       alertId.intValue() == state.getAlertId()) {
                        // escalation runtime needs to know when
                        // alert is fixed so it can stop the escalation chain.
                        resetEscalationState(state);
                    }
                    // mark alert as fixed as well
                    alert.setFixed(true);
                } else {
                    if (escalation.isAllowPause()) {
                        long waitTime =
                            System.currentTimeMillis() + pauseWaitTime;
                        state.setPauseWaitTime(waitTime);
                        state.setPauseEscalation(true);
                    }
                    state.setAcknowledge(true);
                }
                state.setUpdateBy(subject.getFirstName());
                DAOFactory.getDAOFactory().getEscalationStateDAO().save(state);
                return state;
            }
        }
        return null;
    }

    private void scheduleAction(EscalationState state)
    {
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        Escalation e = state.getEscalation();
        int alertDefId = state.getAlertDefinitionId();
        int alertType = state.getAlertType();

        synchronized(stateLocks.getLock(new StateLock(alertDefId, alertType))) {
            EscalationState s =
                getEscalationState(e, new Integer(alertDefId), alertType);
            if (s == null) {
                log.error("Can't find escalation state. escalation=" +
                          e + ", alertDefId="+alertDefId +
                          ", alertType=" + alertType);
                return;
            }
            if (!s.isActive()) {
                if (log.isInfoEnabled()) {
                    log.info("Escalation is no longer active. " +
                             "Stop escalation. state="+s);
                }
                return;
            }
            int nextlevel = s.getCurrentLevel() + 1;
            if (nextlevel >= e.getActions().size()) {
                // at the end of escalation chain, so reset and wait for
                //  next alert to fire.  DO NOT schedule next job.
                resetEscalationState(s);
                sdao.save(s);
                logEscalation(null, s,
                              "End of escalation chain. Stop escalation.");
                if (log.isInfoEnabled()) {
                    log.info("End escalation. alert=" +  s.getAlertId() +
                             ", escalation=" + e + ", state=" + s);
                }
            } else {
                EscalationAction ea = e.getCurrentAction(s.getCurrentLevel());
                // schedule next run time
                long schedule = System.currentTimeMillis() + ea.getWaitTime();
                s.setScheduleRunTime(schedule);
                s.setCurrentLevel(nextlevel);
                s.setAcknowledge(false);
                sdao.save(s);

                logEscalation(ea.getAction(), s,
                              "Escalation scheduled to run in " +
                              ea.getWaitTime()/60000 + " minutes.");
                if (log.isDebugEnabled()) {
                    log.debug("schedule next action. escalation=" + e +
                              ", state=" + s + "action=" + ea);
                }
            }
        }
    }

    private void beginEscalation(Escalation e, Integer alertDefId, int type)
    {
        List ealist = e.getActions();
        EscalationAction ea = (EscalationAction)ealist.get(0);
        EscalationState state =
            getEscalationState(e, alertDefId, type);
        if (state == null) {
            // log error and stop escalation chain
            log.error("Escalation state not found, stop chain. Escalation=" +e+
                      ", alertDefId=" + alertDefId + ", alertType=" + type);
            return;
        }
        if (!state.isActive()) {
            if (log.isInfoEnabled()) {
                logEscalation(ea.getAction(), state,
                              "End Escalation. alert def ID=" +  alertDefId +
                              ", escalation=" + e + ", state=" + state);
            }
            return;
        }
        logEscalation(ea.getAction(), state, "Start Escalation");
        if (log.isDebugEnabled()) {
            log.debug("Start escalation. alert def ID=" +  alertDefId +
                      ", escalation=" + e + ", state=" + state);
        }
        dispatchAction(state);
    }

    public void dispatchAction(EscalationState state)
    {
        Escalation escalation = state.getEscalation();

        AlertInterface alert = getAlert(state.getAlertType(),
                                        state.getAlertId());
        
        // check to see if there is remaining pauseWaitTime
        long remainder = getRemainingPauseWaitTime(state);
        if (remainder > 0) {
            rescheduleEscalation(state, remainder);
            return;
        }
        int curlevel = state.getCurrentLevel();
        if (curlevel >= escalation.getActions().size()) {
            throw new IllegalStateException("current level out of bounds: " +
                                            "alert="+ state.getAlertId() +
                                            ", escalation=" +
                                            escalation+ ", state=" + state);
        }

        try {
            EscalationAction ea =
                escalation.getCurrentAction(state.getCurrentLevel());
            Action a = ea.getAction();
            
            if (log.isDebugEnabled()) {
                log.debug("escalation in progress. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state + ", action="+ea);
            }
            executeAction(state.getAlertType(), alert, a);

            // schedule next action;
            scheduleAction(state);
        } catch (ActionExecuteException e) {
            throw new SystemException(e);
        } catch (PermissionException e) {
            throw new SystemException(e);
        }
    }

    private void rescheduleEscalation(EscalationState state, long remainder)
    {
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        int alertDefId = state.getAlertDefinitionId();
        int alertType = state.getAlertType();
        Escalation e = state.getEscalation();
        
        synchronized(stateLocks.getLock(new StateLock(alertDefId, alertType))) {
            // make sure the escalation is still active before reschedule
            EscalationState s =
                getEscalationState(e, new Integer(alertDefId), alertType);
            if (s == null) {
                log.error("Can't find Escalation State. escalation=" + e +
                          ", alertDefId=" + alertDefId + ", alertType=" +
                          alertType);
                return;
            }
            if (s.isActive()) {
                s.setScheduleRunTime(System.currentTimeMillis()+remainder);
                // reset the pause escalation flag to avoid wait loop.
                s.setPauseEscalation(false);
                sdao.save(s);
                logEscalation(null, s,
                              "Escalation rescheduled to run in " +
                              remainder/60000 + " minutes.");
                if (log.isDebugEnabled()) {
                    log.debug("Pause for additional wait time. alert=" +
                              s.getAlertId() + ", escalation=" + e +
                              ", state=" +
                              s);
                }
            }
        }
    }

    private long getRemainingPauseWaitTime(EscalationState s)
    {
        Escalation e = s.getEscalation();
        if (e.isAllowPause() && s.isPauseEscalation()) {
            long remainder = s.getPauseWaitTime() - System.currentTimeMillis();
            // remaining pause wait time has to be greater than 1
            // minute to qualify
            return remainder > 60000 ? remainder : 0;
        } else {
            return 0;
        }
    }

    private void executeAction(int alertType, AlertInterface alert, Action act)
        throws ActionExecuteException, PermissionException {
        try {
            // prepare, instantiate,  and invoke action
            Class ac = Class.forName(act.getClassName());
            ActionInterface action = (ActionInterface) ac.newInstance();
            action.init(ConfigResponse.decode(action.getConfigSchema(),
                                              act.getConfig()));
            
            String detail;
            if (action.isAlertInterfaceSupported()) {
                String shortReason, longReason;
                
                switch(alertType) {
                case EscalationState.ALERT_TYPE_CLASSIC:
                    AlertManagerLocal aman = null;
    
                    try {
                        aman = AlertManagerUtil.getLocalHome().create();
                    } catch (CreateException e) {
                        throw new SystemException(e);
                    } catch (NamingException e) {
                        throw new SystemException(e);
                    }
                    
                    shortReason = aman.getShortReason((Alert) alert);
                    longReason = aman.getLongReason((Alert) alert);
                    break;
                case EscalationState.ALERT_TYPE_GROUP:
                    GalertLog galert = (GalertLog) alert;
                    shortReason = galert.getShortReason();
                    longReason = galert.getLongReason();
                    break;
                default:
                    log.error("alertType " + alertType + " unknown");
                    return;            // Unknown alert type, can't do anything
                }

                detail = action.execute(alert, shortReason, longReason);
            }
            else {
                detail = action.execute((Alert) alert);
            }

            logActionDetail(alertType, alert, act, detail);
        } catch (ClassNotFoundException e) {
            // Can't execute if we can't lookup up the class
            throw new ActionExecuteException(
                "Action class not found for ID " + act.getId(), e);
        } catch (InstantiationException e) {
            // Can't execute if we can't instantiate the object
            throw new ActionExecuteException(
                "Cannot instantiate action for ID " + act.getId(), e);
        } catch (InvalidActionDataException e) {
            // Can't execute if we can't instantiate the object
            throw new ActionExecuteException(
                "Cannot initialize action for ID " + act.getId(), e);
        } catch (IllegalAccessException e) {
            // Can't execute if we can't access the class
            throw new ActionExecuteException(
                "Cannot access action for ID " + act.getId(), e);
        } catch (EncodingException e) {
            // Can't execute if we can't decode the config
            throw new ActionExecuteException(
                "Cannot decode action config for ID " + act.getId(), e);
        } catch (InvalidOptionException e) {
            // Can't execute if we can't decode the config
            throw new ActionExecuteException(
                "Action config contains invalid option for ID " +
                act.getId(), e);
        } catch (InvalidOptionValueException e) {
            // Can't execute if we don't have good config, just log it
            log.debug("Bad action config value for ID " + act.getId(), e);
        }
    }

    private void logActionDetail(int alertType, AlertInterface alert,
                                  Action act, String detail) {
        switch(alertType) {
        case EscalationState.ALERT_TYPE_CLASSIC:
            AlertActionLog alog =
                new AlertActionLog((Alert) alert, detail, act);
            AlertActionLogDAO dao =
                DAOFactory.getDAOFactory().getAlertActionLogDAO();
            dao.save(alog);
            break;
        case EscalationState.ALERT_TYPE_GROUP:
            GalertActionLog glog =
                new GalertActionLog((GalertLog) alert, detail, act);
            GalertActionLogDAO gdao =
                DAOFactory.getDAOFactory().getGalertActionLogDAO();
            gdao.save(glog);
            break;
        default:
            log.error("alertType " + alertType + " unknown");
            break;            // Unknown alert type, can't do anything
        }
    }

    private void resetEscalationState(EscalationState state)
    {
        state.setCurrentLevel(0);
        state.setScheduleRunTime(0);
        state.setActive(false);
    }

    private void logEscalation(Action action, EscalationState state,
                               String detail)
    {
        AlertInterface alert = getAlert(state.getAlertType(),
                                        state.getAlertId());
        logActionDetail(state.getAlertType(), alert, action, detail);
    }

    private class StateLock {
        int id;
        int type;

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        StateLock(int id, int type)
        {
            this.id = id;
            this.type = type;
        }

        public boolean equals(Object o)
        {
            if (o == null || !(o instanceof StateLock)) {
                return false;
            }
            StateLock l = (StateLock)o;
            return id == l.getId() && type == l.getType();
        }

        public int hashCode()
        {
            int result = 17;

            result = 37*result + id;
            result = 37*result + type;

            return result;
        }
    }
}

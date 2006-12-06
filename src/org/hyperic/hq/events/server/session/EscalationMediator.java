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
import org.hyperic.hibernate.PersistedObject;
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
        List states = findScheduledEscalationState();
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
            executeAction(alertpojo, act);
        }
        
        return alertpojo;
    }

    /**
     * set escalation state to active and take ownership of the escalation
     * chain.  The caller is guaranteed that no other thread will have
     * access to this escalation chain.
     *
     * This method is not cluster-safe.
     *
     * @param e the Escalation
     * @param alertDefId the alert definition ID
     * @return true if escalation state changed from inactive to active. I.e,
     *         the caller now owns the escalation chain.
     *         false escalation chain is already in progress.
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
                               JSONObject escalation)
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
            AlertDefinitionDAO adao = DAOFactory.getDAOFactory()
                .getAlertDefDAO();
            AlertDefinition adef = adao.findById(alertDefId);
            adef.setEscalation(e);
            adao.save(adef);
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

    private Alert getAlert(int alertId)
    {
        AlertDAO dao = DAOFactory.getDAOFactory().getAlertDAO();
        return dao.findById(new Integer(alertId));
    }

    private GalertLog getGalert(int alertId)
    {
        GalertLogDAO dao = DAOFactory.getDAOFactory().getGalertLogDAO();
        return dao.findById(new Integer(alertId));
    }

    public List findScheduledEscalationState()
    {
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        return dao.findScheduledEscalationState();
    }

    public void acknowledgeAlert(Integer subjectID, Integer alertID)
        throws PermissionException, ActionExecuteException
    {
        EscalationState state =
            setEscalationState(subjectID, alertID,
                               EscalationState.ALERT_TYPE_CLASSIC, false);
        if (state != null) {
            AuthzSubject subj =
                DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                    .findById(subjectID);
            // TODO: refine message content
            String message = new StringBuffer()
                .append(subj.getFirstName())
                .append(" ")
                .append(subj.getLastName())
                .append(" has acknowledged the alert. The alert id number is ")
                .append(alertID)
                .append(".")
                .toString();
            logEscalation(null, state,
                          "Alert acknowledged by " + state.getUpdateBy());
            notifyEscalation(alertID, state, message);
        }
    }

    public void fixAlert(Integer subjectID, Integer alertID)
        throws PermissionException, ActionExecuteException
    {
        EscalationState state =
            setEscalationState(subjectID, alertID,
                               EscalationState.ALERT_TYPE_CLASSIC, true);
        if (state != null) {
            AuthzSubject subj =
                DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                    .findById(subjectID);
            // TODO: refine message content
            String message = new StringBuffer()
                .append(subj.getFirstName())
                .append(" ")
                .append(subj.getLastName())
                .append(" has fixed the alert. The alert id number is ")
                .append(alertID)
                .append(".")
                .toString();
            logEscalation(null, state,
                          "Alert fixed by " + state.getUpdateBy());
            notifyEscalation(alertID, state, message);
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
                                               boolean fixed)
        throws PermissionException
    {
        SessionBase.canModifyEscalation(subjectID);

        Escalation escalation;
        Integer alertDefId;
        switch(alertType) {
        case EscalationState.ALERT_TYPE_CLASSIC:
            Alert alert = getAlert(alertId.intValue());
            alertDefId = alert.getAlertDefinition().getId();
            escalation = alert.getAlertDefinition().getEscalation();
            break;
        case EscalationState.ALERT_TYPE_GROUP:
            GalertLog galert = getGalert(alertId.intValue());
            alertDefId = galert.getAlertDef().getId();
            escalation = galert.getAlertDef().getEscalation();
            break;
        default:
            log.error("alertType " + alertType + " unknown");
            return null;            // Unknown alert type, can't do anything
        }

        if (escalation != null) {
            EscalationState state =
                getEscalationState(escalation, alertDefId, alertType);

            if (state != null) {
                if (fixed) {
                    state.setFixed(true);
                } else {
                    state.setAcknowledge(true);
                }
                AuthzSubject subject =
                    DAOFactory.getDAOFactory().getAuthzSubjectDAO()
                        .findById(subjectID);
                state.setUpdateBy(subject.getFirstName());
                DAOFactory.getDAOFactory().getEscalationStateDAO()
                    .save(state);
            }
            return state;
        }
        return null;
    }

    private void scheduleAction(EscalationState state)
    {
        Escalation  escalation = state.getEscalation();
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        if (state == null) {
            log.error("Escalation state not found. escalation="+escalation+
                ", alert Id="+ state.getAlertId());
            return;
        }

        if (state.isFixed()) {
            // fixed so no need to schedule
            if (log.isInfoEnabled()) {
                log.info("Escalation fixed. alert=" +  state.getAlertId() +
                         ", escalation=" +
                         escalation + ", state=" + state);
            }
            resetEscalationState(state);
            sdao.save(state);
            return;
        }
        int nextlevel = state.getCurrentLevel() + 1;
        if (nextlevel >= escalation.getActions().size()) {
            // at the end of escalation chain, so reset and wait for
            //  next alert to fire.  DO NOT schedule next job.
            resetEscalationState(state);
            sdao.save(state);
            logEscalation(null, state,
                          "End of escalation chain. Stop escalation.");
            if (log.isInfoEnabled()) {
                log.info("End escalation. alert=" +  state.getAlertId() +
                         ", escalation=" + escalation + ", state=" + state);
            }
        } else {
            EscalationAction ea =
                escalation.getCurrentAction(state.getCurrentLevel());
            // schedule next run time
            state.setScheduleRunTime(System.currentTimeMillis() +
                                     ea.getWaitTime());
            state.setCurrentLevel(nextlevel);
            sdao.save(state);
            logEscalation(ea.getAction(), state,
                          "Escalation scheduled to run in " +
                          ea.getWaitTime()/60000 + " minutes.");
            if (log.isDebugEnabled()) {
                log.debug("schedule next action. escalation=" + escalation +
                          ", state=" + state + "action=" + ea);
            }
        }
    }

    private void beginEscalation(Escalation e, Integer alertDefId, int type)
    {
        List ealist = e.getActions();
        EscalationAction ea = (EscalationAction)ealist.get(0);
        EscalationState state =
            getEscalationState(e, alertDefId, type);
        logEscalation(ea.getAction(), state, "Start Escalation");
        if (log.isDebugEnabled()) {
            log.debug("Start escalation. alert def ID=" +  alertDefId +
                      ", escalation=" + e + ", state=" + state);
        }
        dispatchAction(state);
    }

    public void dispatchAction(EscalationState state)
    {
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        Escalation escalation = state.getEscalation();

        if (state == null) {
            logEscalation(null, state, "Escalation stopped. Can't find state.");
            // log error and stop escalation chain
            log.error("Escalation state not found, stop chain. " +
                "escalation=" + escalation + ", alertId=" + state.getAlertId());
            return;
        }

        PersistedObject alert;
        switch(state.getAlertType()) {
        case EscalationState.ALERT_TYPE_CLASSIC:
            alert = getAlert(state.getAlertId());
            break;
        case EscalationState.ALERT_TYPE_GROUP:
            alert = getGalert(state.getAlertId());
            break;
        default:
            log.error("alertType " + state.getAlertType() + " unknown");
            return;
        }
        
        if (state.isFixed() || alert == null) {
            // fixed so stop.
            if (state.isFixed()) {
                if (log.isInfoEnabled()) {
                    log.info("Escalation fixed. alert=" +  state.getAlertId() +
                             ", escalation=" +
                             escalation + ", state=" + state);
                }
            } else {
                logEscalation(null, state,
                              "Escalation stopped. Can't find alert.");
                log.error("Stopping Escalation as the alert was not " +
                          "found. " +
                          "escalation=" + escalation + ", state=" + state);
            }
            resetEscalationState(state);
            sdao.save(state);
            return;
        }
        // check to see if there is remaining pauseWaitTime
        long remainder = getRemainingPauseWaitTime(escalation, state);
        if (remainder > 0) {
            // reschedule
            state.setScheduleRunTime(System.currentTimeMillis()+remainder);
            // reset the pause escalation flag to avoid wait loop.
            state.setPauseEscalation(false);
            sdao.save(state);
            logEscalation(null, state,
                          "Escalation rescheduled to run in " +
                          remainder/60000 + " minutes.");
            if (log.isDebugEnabled()) {
                log.debug("Pause for additional wait time. alert=" +
                          state.getAlertId() + ", escalation=" + escalation +
                          ", state=" +
                          state);
            }
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
            // XXX - Actions only know to execute alert definitions, not
            // galert definitions
            if (state.getAlertType() == EscalationState.ALERT_TYPE_CLASSIC) {            
                dispatchAction(escalation, (Alert) alert, state);
            }

            // schedule next action;
            scheduleAction(state);
        } catch (ActionExecuteException e) {
            throw new SystemException(e);
        } catch (PermissionException e) {
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
        throws ActionExecuteException, PermissionException
    {
        EscalationAction ea =
            escalation.getCurrentAction(state.getCurrentLevel());
        Action a = ea.getAction();

        if (log.isDebugEnabled()) {
            log.debug("escalation in progress. alert=" +  alert +
                      ", escalation=" + escalation + ", state=" +
                      state + ", action="+ea);
        }
        executeAction(alert, a);
    }

    private void executeAction(Alert alert, Action act)
        throws ActionExecuteException, PermissionException {
        try {
            // prepare, instantiate,  and invoke action
            Class ac = Class.forName(act.getClassName());
            ActionInterface action = (ActionInterface) ac.newInstance();
            action.init(ConfigResponse.decode(action.getConfigSchema(),
                                              act.getConfig()));

            Collection coll = alert.getConditionLog();
            AlertConditionLog[] logs =
                (AlertConditionLog[]) coll.toArray(new AlertConditionLog[0]);

            String detail = action.execute(
                alert.getAlertDefinition(), logs, alert.getId());

            AlertActionLog alog = new AlertActionLog(alert, detail, act);
            AlertActionLogDAO dao =
                DAOFactory.getDAOFactory().getAlertActionLogDAO();
            dao.save(alog);
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

    private void resetEscalationState(EscalationState state)
    {
        state.setCurrentLevel(0);
        state.setScheduleRunTime(0);
        state.setActive(false);
    }

    private void logEscalation(Action action, EscalationState state,
                               String detail)
    {
        switch(state.getAlertType()) {
        case EscalationState.ALERT_TYPE_CLASSIC:
            Alert alert = getAlert(state.getAlertId());
            AlertActionLog alog = new AlertActionLog(alert, detail, action);
            AlertActionLogDAO dao =
                DAOFactory.getDAOFactory().getAlertActionLogDAO();
            dao.save(alog);
            break;
        case EscalationState.ALERT_TYPE_GROUP:
            GalertLog galert = getGalert(state.getAlertId());
            GalertActionLog glog = new GalertActionLog(galert, detail, action);
            GalertActionLogDAO gdao =
                DAOFactory.getDAOFactory().getGalertActionLogDAO();
            gdao.save(glog);
            break;
        default:
            log.error("alertType " + state.getAlertType() + " unknown");
            break;            // Unknown alert type, can't do anything
        }
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

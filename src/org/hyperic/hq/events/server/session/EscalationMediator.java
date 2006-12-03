package org.hyperic.hq.events.server.session;

import org.hyperic.hq.Mediator;
import org.hyperic.hq.TransactionContext;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionManagerLocal;
import org.hyperic.hq.common.shared.TransactionManagerUtil;
import org.hyperic.hq.events.server.mbean.EscalationSchedulerMBean;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.Notify;
import org.hyperic.hibernate.LockSet;
import org.hyperic.dao.DAOFactory;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONException;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;

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

    private AlertManagerLocal alertManagerLocal;
    private TransactionManagerLocal transactionManager;
    private EscalationSchedulerMBean escalationServiceMBean;

    protected EscalationMediator()
    {
        try {
            alertManagerLocal = AlertManagerUtil.getLocalHome().create();
            transactionManager = TransactionManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        try {
            ObjectName name = new ObjectName(ESCALATION_SERVICE_MBEAN);
            escalationServiceMBean =
                (EscalationSchedulerMBean)
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
                    alertManagerLocal.dispatchAction(
                        state.getEscalation().getId(),
                        new Integer(state.getAlertDefinitionId()),
                        new Integer(state.getAlertId()));
                }
            });
        }
    }

    /**
     * escalation entry point
     *
     * @param escalationId
     * @param alertId
     */
    public void startEscalation(Integer escalationId, Integer alertId)
    {
        Alert alert = alertManagerLocal.findAlertById(alertId);
        Integer alertDefId = alert.getAlertDefinition().getId();
        Escalation escalation = findEscalationById(escalationId);

        if (setActiveEscalation(escalation, alertDefId, alertId.intValue())) {
            if (log.isDebugEnabled()) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId);
                log.debug("Start escalation. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state);
            }
            // Escalation is not active, start escalation.
            dispatchAction(escalationId, alertDefId, alertId);
        } else {
            // escalation is active, so do not start another escalation
            // for this chain.
            if (log.isDebugEnabled()) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId);
                log.debug("Escalation already in progress. alert=" +  alert +
                          ", escalation=" + escalation + ", state="+ state);
            }
        }
    }

    /**
     * set escalation state to active and take ownership of the escalation
     * chain.  The caller is guaranteed that no other thread will have
     * access to this escalation chain.
     *
     * This method is not cluster-safe.
     *
     * @param e
     * @param alertDefId
     * @return true if escalation state changed from inactive to active. I.e,
     *         the caller now owns the escalation chain.
     *         false escalation chain is already in progress.
     */
    private boolean setActiveEscalation(final Escalation e,
                                        final Integer alertDefId,
                                        final int alertId)
    {
        synchronized(stateLocks.getLock(alertDefId)) {
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
                            getEscalationState(e, alertDefId);
                        if (state == null) {
                            state = EscalationState.newInstance(e, alertDefId);
                            DAOFactory.getDAOFactory().getEscalationStateDAO()
                                .save(state);
                        }
                        if(state.isActive()) {
                            return null;
                        }
                        state.setActive(true);
                        state.setAlertId(alertId);
                        return context;
                    }
                });
            return context != null;
        }
    }

    public void saveEscalation(Integer subjectId, JSONObject escalation)
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
    }

    public Escalation findEscalationById(Escalation e)
    {
        return findEscalationById(e.getId());
    }

    public Escalation findEscalationById(Integer id)
    {
        return DAOFactory.getDAOFactory().getEscalationDAO().findById(id);
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

    public void clearActiveEscalation(Integer escalationId, Integer alertDefId)
    {
        // clear active status for this alertDef
        DAOFactory.getDAOFactory().getEscalationDAO()
                .clearActiveEscalation(escalationId, alertDefId);
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

    public EscalationState getEscalationState(Escalation e, Integer alertDefId)
    {
        EscalationStateDAO dao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();
        return dao.getEscalationState(e, alertDefId);
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
            setEscalationState(subjectID, alertID, false);
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
        notifyEscalation(alertID, state, message);
    }

    public void fixAlert(Integer subjectID, Integer alertID)
        throws PermissionException, ActionExecuteException
    {
        EscalationState state =
            setEscalationState(subjectID, alertID, true);
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
        notifyEscalation(alertID, state, message);
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
                                               Integer alertID,
                                               boolean fixed)
        throws PermissionException
    {
        SessionBase.canModifyEscalation(subjectID);

        Alert alert =
            DAOFactory.getDAOFactory().getAlertDAO().findById(alertID);
        Integer alertDefId =
            alert.getAlertDefinition().getId();
        Escalation escalation =
            alert.getAlertDefinition().getEscalation();

        EscalationState state =
            getEscalationState(escalation, alertDefId);

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

    private void scheduleAction(Integer escalationId, Integer alertDefId,
                                Integer alertId)
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        Escalation  escalation = dao.findById(escalationId);
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        EscalationState state = getEscalationState(escalation, alertDefId);
        if (state == null) {
            log.error("Escalation state not found. escalation="+escalation+
                ", alertDefId="+ alertDefId +", alertId="+ alertId);
            return;
        }
        Alert alert =
            DAOFactory.getDAOFactory().getAlertDAO().get(alertId);
        if (state.isFixed() || alert == null) {
            // fixed so no need to schedule
            if (log.isInfoEnabled()) {
                if (state.isFixed()) {
                    log.info("Escalation fixed. alert=" +  alert +
                        ", escalation=" +
                        escalation + ", state=" + state);
                } else {
                    log.error("Stopping Escalation chain as the Alert " +
                        "was not found. escalation=" + escalation +
                        ", state=" + state);
                }
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
            if (log.isInfoEnabled()) {
                log.info("End escalation. alert=" +  alert + ", escalation=" +
                         escalation + ", state=" + state);
            }
        } else {
            EscalationAction ea =
                escalation.getCurrentAction(state.getCurrentLevel());
            // schedule next run time
            state.setScheduleRunTime(System.currentTimeMillis() +
                                     ea.getWaitTime());
            state.setCurrentLevel(nextlevel);
            sdao.save(state);
            if (log.isDebugEnabled()) {
                log.debug("schedule next action. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state + "action=" + ea);
            }
        }
    }

    public void dispatchAction(Integer escalationId, Integer alertDefId,
                               Integer alertId)
    {
        EscalationDAO dao = DAOFactory.getDAOFactory().getEscalationDAO();
        EscalationStateDAO sdao =
            DAOFactory.getDAOFactory().getEscalationStateDAO();

        Escalation escalation = dao.findById(escalationId);
        EscalationState state = getEscalationState(escalation, alertDefId);
        if (state == null) {
            // log error and stop escalation chain
            log.error("Escalation state not found, stop chain. " +
                "escalation=" + escalation + ", alertDefid="+ alertDefId +
                ", alertId="+alertId);
            return;
        }

        Alert alert =
            DAOFactory.getDAOFactory().getAlertDAO().get(alertId);
        
        if (state.isFixed() || alert == null) {
            // fixed so stop.
            if (log.isInfoEnabled()) {
                if (state.isFixed()) {
                    log.info("Escalation fixed. alert=" +  alert +
                        ", escalation=" +
                        escalation + ", state=" + state);
                } else {
                    log.error("Stopping Escalation as the alert was not " +
                        "found. " +
                        "escalation=" + escalation + ", state=" + state);
                }
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

            // schedule next action;
            scheduleAction(escalation.getId(), alertDefId, alertId);
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
        throws ClassNotFoundException, IllegalAccessException,
        InstantiationException, EncodingException,
        InvalidActionDataException, ActionExecuteException,
        PermissionException
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

    private void resetEscalationState(EscalationState state)
    {
        state.setCurrentLevel(0);
        state.setScheduleRunTime(0);
        state.setActive(false);
    }

    private void addAlertActionLog(Alert alert, String detail)
        throws PermissionException
    {
        // TODO: this gotta be done with pojos.  not value objects!
        AlertValue alertValue = new AlertValue();
        alertValue.setAlertDefId(alert.getAlertDefinition().getId());

        AlertActionLogValue alog = new AlertActionLogValue();
        alog.setActionId(alert.getId());
        alog.setDetail(detail);
        alertValue.addActionLog(alog);

        alertManagerLocal.createAlert(alertValue);
    }
}

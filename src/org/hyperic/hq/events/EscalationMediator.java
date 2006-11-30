/**
 *
 */
package org.hyperic.hq.events;

import org.hyperic.hq.Mediator;
import org.hyperic.hq.TransactionContext;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionManagerLocal;
import org.hyperic.hq.common.shared.TransactionManagerUtil;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.events.server.session.EscalationDAO;
import org.hyperic.hq.events.server.session.EscalationStateDAO;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.mbean.EscalationSchedulerMBean;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hibernate.LockSet;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.dao.DAOFactory;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static boolean MOCKTEST =
        "true".equals(System.getProperty("hq.mocktest"));

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
        // MBEAN Service lookup does not work in mock unit test env.
        if (MOCKTEST) {
            return;
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
     * run escalation
     */
    public void processEscalation()
    {
        // get all scheduled escalations
        List states = findScheduledEscalationState();
        if (log.isDebugEnabled()) {
            log.debug("Found " + states.size() + " scheduled escalations.");
        }
        if (MOCKTEST) {
            return;
        }
        for (Iterator s = states.iterator(); s.hasNext(); ) {
            final EscalationState state = (EscalationState)s.next();
            if (log.isDebugEnabled()) {
                log.debug("EscalationState: "+state);
            }
            escalationServiceMBean.run(new Runnable() {
                public void run()
                {
                    actionDispatcher(state.getEscalation().getId(),
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

        if (setActiveEscalation(escalation, alertDefId)) {
            if (log.isDebugEnabled()) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId);
                log.debug("Start escalation. alert=" +  alert +
                          ", escalation=" + escalation + ", state=" +
                          state);
            }
            // Escalation is not active, start escalation.
            actionDispatcher(escalationId, alertDefId);
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
    private boolean setActiveEscalation(Escalation e, Integer alertDefId)
    {
        synchronized(stateLocks.getLock(alertDefId)) {
            EscalationState state = getEscalationState(e, alertDefId);
            if(state.isActive()) {
                return false;
            }
            state.setActive(true);
            transactionManager.saveReqNew(state);
        }
        return true;
    }

    public void save(PersistedObject p)
    {
        transactionManager.save(p);
    }

    public void remove(PersistedObject p)
    {
        transactionManager.delete(p);
    }

    public Escalation findEscalationById(Escalation e)
    {
        return findEscalationById(e.getId());
    }

    public Escalation findEscalationById(Integer id)
    {
        return DAOFactory.getDAOFactory().getEscalationDAO().findById(id);
    }

    public int deleteEscalationById(Integer[] ids)
    {
        return DAOFactory.getDAOFactory().getEscalationDAO()
                .deleteById(ids);
    }

    public Collection findAll()
    {
        return DAOFactory.getDAOFactory().getEscalationDAO().findAll();
    }

    public void clearActiveEscalation()
    {
        // usually invoke on hq start to clear all escalation marked
        // as in progress
        DAOFactory.getDAOFactory().getEscalationDAO().clearActiveEscalation();
    }

    public void clearActiveEscalation(Integer escalationId, Integer alertDefId)
    {
        // clear active status for this alertDef
        DAOFactory.getDAOFactory().getEscalationDAO()
                .clearActiveEscalation(escalationId, alertDefId);
    }

    public Escalation findByEscalationName(String name)
    {
        Escalation e = Escalation.newSearchable(name);
        return (Escalation)transactionManager.findPersisted(e);
    }

    public Escalation findEscalationByAlertDefId(Integer id)
    {
        EscalationDAO dao =DAOFactory.getDAOFactory().getEscalationDAO();
        return dao.findByAlertDefinitionId(id);
    }

    public EscalationState getEscalationState(Escalation e, Integer alertDefId)
    {
        return DAOFactory.getDAOFactory().getEscalationStateDAO()
                .getEscalationState(e, alertDefId);
    }

    public List findScheduledEscalationState()
    {
        TransactionContext context =
                transactionManager.execute(new TransactionContext() {
                    public TransactionContext run(TransactionContext ctx) {
                        List result = DAOFactory.getDAOFactory()
                                .getEscalationStateDAO()
                                .findScheduledEscalationState();
                        ctx.setList(result);
                        return ctx;
                    }
                });
        return context.getList();
    }

    private void actionScheduler(final Integer escalationId,
                                final Integer alertId)
    {
        transactionManager.execute(new TransactionContext() {
            public TransactionContext run(TransactionContext ctx) {
                scheduleAction(escalationId, alertId);
                return ctx;
            }
        });
    }

    private void actionDispatcher(final Integer escalationId,
                                 final Integer alertId)
    {
        transactionManager.execute(new TransactionContext() {
            public TransactionContext run(TransactionContext ctx) {
                dispatchAction(escalationId, alertId);
                return ctx;
            }
        });
    }

    private void scheduleAction(Integer escalationId, Integer alertId)
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

    private void dispatchAction(Integer escalationId, Integer alertId)
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
            // fixed so stop.
            if (log.isInfoEnabled()) {
                log.info("Escalation fixed. alert=" +  alert + ", escalation=" +
                         escalation + ", state=" + state);
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
            EscalationMediator.getInstance()
                .actionScheduler(escalation.getId(), alertId);
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

    private void resetEscalationState(EscalationState state)
    {
        state.setCurrentLevel(0);
        state.setScheduleRunTime(0);
        state.setActive(false);
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

        alertManagerLocal.createAlert(alertValue);
    }
}

/**
 *
 */
package org.hyperic.hq.events;

import org.hyperic.hq.Mediator;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.events.server.mbean.EscalationSchedulerMBean;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hibernate.LockSet;
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
    private EscalationSchedulerMBean escalationServiceMBean;

    protected EscalationMediator()
    {
        try {
            alertManagerLocal = AlertManagerUtil.getLocalHome().create();
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
        List states = getScheduledEscalationState();
        if (log.isInfoEnabled()) {
            log.info("Found " + states.size() + " scheduled escalations.");
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
                    dispatchAction(state.getEscalation().getId(),
                                   new Integer(state.getAlertId()));
                }
            });
        }
    }

    public List getScheduledEscalationState()
    {
        return alertManagerLocal.getScheduledEscalationState();
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
        Escalation escalation =
            alertManagerLocal.findEscalationById(escalationId);

        if (setActiveEscalation(escalation, alertDefId)) {
            if (log.isInfoEnabled()) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId);
                log.info("Start escalation. alert=" +  alert +
                         ", escalation=" + escalation + ", state=" +
                         state);
            }
            // Escalation is not active, start escalation.
            dispatchAction(escalationId, alertDefId);
        } else {
            // escalation is active, so do not start another escalation
            // for this chain.
            if (log.isInfoEnabled()) {
                EscalationState state =
                    getEscalationState(escalation, alertDefId);
                log.info("Escalation already in progress. alert=" +  alert +
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
            saveEscalationStateReqNew(state);
        }
        return true;
    }

    public Escalation findEscalationById(Escalation e)
    {
        return alertManagerLocal.findEscalationById(e.getId());
    }

    public void saveEscalation(Escalation e)
    {
        alertManagerLocal.saveEscalation(e);
    }

    public void saveEscalationState(EscalationState state)
    {
        alertManagerLocal.saveEscalationState(state);
    }

    public void saveEscalationStateReqNew(EscalationState state)
    {
        alertManagerLocal.saveEscalationStateReqNew(state);
    }

    public void removeEscalation(Escalation e)
    {
        alertManagerLocal.removeEscalation(e);
    }

    public void removeEscalationState(EscalationState state)
    {
        alertManagerLocal.removeEscalationState(state);
    }

    public EscalationState getEscalationState(Escalation e, Integer alertDefId)
    {
        return alertManagerLocal.getEscalationState(e, alertDefId);
    }

    public void scheduleAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.scheduleAction(escalationId, alertId);
    }

    public void dispatchAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.dispatchAction(escalationId, alertId);
    }

    public void clearActiveEscalation()
    {
        // usually invoke on hq start to clear all escalation marked
        // as in progress
        alertManagerLocal.clearActiveEscalation();
    }

    public void clearActiveEscalation(Integer escalationId, Integer alertDefId)
    {
        // clear active status for this alertDef
        alertManagerLocal.clearActiveEscalation(escalationId, alertDefId);
    }
}

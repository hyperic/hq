/**
 *
 */
package org.hyperic.hq.events.escalation.mediator;

import org.hyperic.hq.Mediator;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.command.SaveCommand;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationDAO;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.EscalationState;
import org.hyperic.hq.events.server.session.EscalationAction;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.escalation.EscalationJob;
import org.hyperic.hq.events.escalation.command.ScheduleActionCommand;
import org.hyperic.hq.events.escalation.command.EscalateCommand;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.dao.DAOFactory;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.hibernate.LockSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import java.util.Collection;

public class EscalationMediator extends Mediator
{
    private static final int DEFAULT_LOCKSET_SIZE = 1024;
    private static final LockSet stateLocks=new LockSet(DEFAULT_LOCKSET_SIZE);
    private static Log log = LogFactory.getLog(EscalationMediator.class);

    private static EscalationMediator instance = new EscalationMediator();

    public static EscalationMediator getInstance()
    {
        return instance;
    }

    private AlertManagerLocal alertManagerLocal;

    protected EscalationMediator()
    {
        try {
            alertManagerLocal = AlertManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
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
        Alert alert = alertManagerLocal.getAlertById(alertId);
        Integer alertDefId = alert.getAlertDefinition().getId();
        Escalation escalation =
            alertManagerLocal.getEscalationById(escalationId);
        EscalationState state = escalation.getEscalationState(alertDefId);

        if (setActiveEscalation(alertDefId, escalationId)) {
            if (log.isInfoEnabled()) {
                log.info("Start escalation. alert=" +  alert +
                         ", escalation=" + escalation + ", state=" +
                         state);
            }
            // Escalation is not active, start escalation.
            CommandContext context = CommandContext.createContext();
            context.execute(
                EscalateCommand.setInstance(escalationId, alertId)
            );
        } else {
            // escalation is active, so do not start another escalation
            // for this chain.
            if (log.isInfoEnabled()) {
                log.info("Escalation already in progress. alert=" +  alert +
                         ", escalation=" + escalation + ", state="+ state);
            }
        }
    }

    /**
     * set escalation state to active
     *
     * @param escalationId
     * @param alertDefId
     * @return true if escalation state changed from inactive to active.
     */
    private boolean setActiveEscalation(Integer escalationId,
                                        Integer alertDefId)
    {
        synchronized(stateLocks.getLock(alertDefId)) {
            Escalation escalation =
                alertManagerLocal.getEscalationById(escalationId);
            EscalationState state = escalation.getEscalationState(alertDefId);
            if(state.isActive()) {
                return false;
            }
            state.setActive(true);
            CommandContext context = CommandContext.createContext();
            context.setRequiresNew(true);
            context.execute(SaveCommand.setInstance(escalation));
        }
        return true;
    }

    public void scheduleAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.scheduleAction(escalationId, alertId);
    }

    public void dispatchAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.dispatchAction(escalationId, alertId);
    }

    public void clearActiveEscalation() {
        // usually invoke on hq start to clear all escalation marked
        // as in progress
        alertManagerLocal.clearActiveEscalation();
    }

    public void clearActiveEscalation(Integer escalationId,
                                      Integer alertDefId) {
        // clear active status for this alertDef
        alertManagerLocal.clearActiveEscalation(escalationId, alertDefId);
    }
}

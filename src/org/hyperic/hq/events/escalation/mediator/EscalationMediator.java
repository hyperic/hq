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

import javax.ejb.CreateException;
import javax.naming.NamingException;
import java.util.Collection;

public class EscalationMediator extends Mediator
{
    private static AlertManagerLocal alertManagerLocal;
    static {
        try {
            alertManagerLocal = AlertManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private static EscalationMediator instance = new EscalationMediator();

    public static EscalationMediator getInstance()
    {
        return instance;
    }

    /**
     * escalation entry point
     *
     * @param escalationId
     * @param alertId
     */
    public void startEscalation(Integer escalationId, Integer alertId)
    {
        Escalation escalation =
            alertManagerLocal.getEscalationById(escalationId);
        Alert alert = alertManagerLocal.getAlertById(alertId);
        Integer alertDefId = alert.getAlertDefinition().getId();
        EscalationState state = escalation.getEscalationState(alertDefId);
        CommandContext context = CommandContext.createContext();
        synchronized(state) {
            if(state.isActive()) {
                // escalation is active, so do not start another escalation
                // for this chain.
                return;
            }
            state.setActive(true);
            context.execute(SaveCommand.setInstance(escalation));
        }
        context.execute(
            EscalateCommand.setInstance(escalationId, alertId)
        );
    }

    public void scheduleAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.scheduleAction(escalationId, alertId);
    }

    public void dispatchAction(Integer escalationId, Integer alertId)
    {
        alertManagerLocal.dispatchAction(escalationId, alertId);
    }
}

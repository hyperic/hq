/**
 *
 */
package org.hyperic.hq.events.escalation.command;

import org.hyperic.hq.Command;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.escalation.mediator.EscalationMediator;

public class EscalateCommand extends Command {
    private Integer escalationId;
    private Integer alertId;

    public static EscalateCommand setInstance(Integer eid, Integer aId)
    {
        return new EscalateCommand(eid, aId);
    }

    protected EscalateCommand(Integer eid, Integer aid) {
        escalationId = eid;
        alertId = aid;
    }

    public void execute(CommandContext context) {
        verify();
        EscalationMediator mediator = EscalationMediator.getInstance();
        mediator.dispatchAction(escalationId, alertId);
    }

    private void verify()
    {
        if (escalationId == null) {
            throw new IllegalArgumentException("Missing Escalation ID");
        }
        if (alertId == null) {
            throw new IllegalArgumentException("Missing Alert ID");
        }
    }
}

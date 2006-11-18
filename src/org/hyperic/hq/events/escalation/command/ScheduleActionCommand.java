/**
 *
 */
package org.hyperic.hq.events.escalation.command;

import org.hyperic.hq.Command;
import org.hyperic.hq.CommandContext;
import org.hyperic.hq.events.escalation.mediator.EscalationMediator;

public class ScheduleActionCommand extends Command
{
    private Integer escalationId;
    private Integer alertId;

    public static ScheduleActionCommand setInstance(Integer eId, Integer aId)
    {
        return new ScheduleActionCommand(eId, aId);
    }

    protected ScheduleActionCommand() {
    }

    protected ScheduleActionCommand(Integer eid, Integer aid)
    {
        escalationId = eid;
        alertId = aid;
    }

    public void execute(CommandContext context)
    {
        verify();
        EscalationMediator mediator = EscalationMediator.getInstance();
        mediator.scheduleAction(escalationId, alertId);
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

package org.hyperic.hq.ui.json.action.escalation.crud;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;

/**
 */
public class RemoveEscalation extends BaseAction
{
    public void execute(JsonActionContext context)
    {
        EscalationWebMediator.getInstance()
                .removeEscalation(context.getSessionId(),context.getId());
    }
}

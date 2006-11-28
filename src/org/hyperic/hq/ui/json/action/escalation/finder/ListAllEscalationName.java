package org.hyperic.hq.ui.json.action.escalation.finder;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.json.JSONArray;

/**
 */
public class ListAllEscalationName extends BaseAction
{
    public void execute(JsonActionContext context)
    {
        JSONArray array = EscalationWebMediator.getInstance()
                .listAllEscalationName(context.getSessionId());
        context.setJSONResult(new JSONResult(array));
    }
}

package org.hyperic.hq.ui.json.action.escalation.finder;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.json.JSONObject;

/**
 */
public class JsonEscalationByAlertDefId extends BaseAction
{
    public void execute(JsonActionContext context)
    {
        JSONObject escalation = EscalationWebMediator.getInstance()
                .jsonEscalationByAlertDefId(
                        context.getSessionId(), context.getId());
        context.setJSONResult(new JSONResult(escalation));
    }
}

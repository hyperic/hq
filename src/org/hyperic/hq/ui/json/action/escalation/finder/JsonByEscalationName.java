package org.hyperic.hq.ui.json.action.escalation.finder;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.json.JSONObject;

/**
 */
public class JsonByEscalationName extends BaseAction
{
    public void execute(JsonActionContext context)
    {
        JSONObject escalation = EscalationWebMediator.getInstance()
                .jsonByEscalationName(context.getSessionId(),context.getName());
        context.setJSONResult(new JSONResult(escalation));
    }
}

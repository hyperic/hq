package org.hyperic.hq.ui.json.action.escalation.finder;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.json.JSONObject;
import org.json.JSONException;

/**
 */
public class JsonByEscalationName extends BaseAction
{
    public void execute(JsonActionContext context) throws JSONException,
        PermissionException, SessionTimeoutException, SessionNotFoundException
    {
        JSONObject escalation = EscalationWebMediator.getInstance()
                .jsonByEscalationName(context.getSessionId(),context.getName());
        context.setJSONResult(new JSONResult(escalation));
    }
}

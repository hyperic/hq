package org.hyperic.hq.ui.json.action.escalation.finder;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;

import java.rmi.RemoteException;

/**
 */
public class ListAllEscalationName extends BaseAction
{
    public void execute(JsonActionContext context)
        throws JSONException,
               PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        JSONArray array = EscalationWebMediator.getInstance()
                .listAllEscalationName(context, context.getSessionId());
        context.setJSONResult(new JSONResult(array));
    }
}

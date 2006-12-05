package org.hyperic.hq.ui.json.action.escalation.crud;

import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;

import java.rmi.RemoteException;

/**
 */
public class SaveEscalation extends BaseAction
{
    public void execute(JsonActionContext context)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        EscalationWebMediator wmed = EscalationWebMediator.getInstance();
        // fake data
        wmed.saveEscalation(context, context.getSessionId(), null);
    }
}

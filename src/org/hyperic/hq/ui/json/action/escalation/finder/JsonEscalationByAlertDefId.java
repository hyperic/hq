package org.hyperic.hq.ui.json.action.escalation.finder;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.json.action.escalation.EscalationWebMediator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 */
public class JsonEscalationByAlertDefId extends BaseAction
{
    private Log log = LogFactory.getLog(JsonEscalationByAlertDefId.class);
    
    public void execute(JsonActionContext context)
        throws JSONException,
               PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        JSONObject escalation = EscalationWebMediator.getInstance()
                .jsonEscalationByAlertDefId(
                        context, context.getSessionId(), context.getId(),
                        context.getAlertDefType());
        if (log.isDebugEnabled()) {
            log.debug("JsonEscalationByAlertDefId: " + context.getId() +
                      ": " + escalation);
        }
        context.setJSONResult(new JSONResult(escalation));
    }
}

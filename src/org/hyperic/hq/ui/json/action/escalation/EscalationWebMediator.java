package org.hyperic.hq.ui.json.action.escalation;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.util.ContextUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.rmi.RemoteException;

public class EscalationWebMediator
{
    private static EscalationWebMediator ourInstance =
            new EscalationWebMediator();

    public static EscalationWebMediator getInstance()
    {
        return ourInstance;
    }

    private EscalationWebMediator()
    {
    }

    private EventsBoss getEventsBoss(JsonActionContext context)
    {
        return ContextUtils.getEventsBoss(context.getServletContext());
    }

    public JSONObject jsonByEscalationName(JsonActionContext context,
                                           int sessionId, String name)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return getEventsBoss(context).jsonByEscalationName(sessionId, name);
    }

    public JSONObject jsonByEscalationId(JsonActionContext context,
                                         int sessionId, Integer id)
        throws JSONException, PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        return getEventsBoss(context).jsonByEscalationId(sessionId, id);
    }

    public JSONObject jsonEscalationByAlertDefId(JsonActionContext context,
                                                 int sessionId,
                                                 Integer alertDefId)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return getEventsBoss(context)
            .jsonEscalationByAlertDefId(sessionId, alertDefId);
    }

    public JSONArray listAllEscalationName(JsonActionContext context,
                                           int sessionId)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return getEventsBoss(context).listAllEscalationName(sessionId);
    }

    public void removeEscalation(JsonActionContext context,
                                 int sessionId, Integer id)
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        getEventsBoss(context)
            .deleteEscalationById(sessionId, new Integer[]{id});
    }

    public void saveEscalation(JsonActionContext context,
                               int sessionId, JSONObject json)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        getEventsBoss(context).saveEscalation(sessionId, json);
    }
}

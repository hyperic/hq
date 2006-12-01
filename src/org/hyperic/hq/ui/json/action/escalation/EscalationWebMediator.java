package org.hyperic.hq.ui.json.action.escalation;

import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.ejb.CreateException;
import javax.naming.NamingException;

public class EscalationWebMediator
{
    private static EventsBossLocal eventsBoss;

    static {
        try {
           eventsBoss = EventsBossUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private static EscalationWebMediator ourInstance =
            new EscalationWebMediator();

    public static EscalationWebMediator getInstance()
    {
        return ourInstance;
    }

    private EscalationWebMediator()
    {
    }

    public JSONObject jsonByEscalationName(int sessionId, String name) throws
        JSONException, PermissionException, SessionTimeoutException,
        SessionNotFoundException
    {
        return eventsBoss.jsonByEscalationName(sessionId, name);
    }

    public JSONObject jsonEscalationByAlertDefId(int sessionId,
                                                 Integer alertDefId) throws
        JSONException, PermissionException, SessionTimeoutException,
        SessionNotFoundException
    {
        return eventsBoss.jsonEscalationByAlertDefId(sessionId, alertDefId);
    }

    public JSONArray listAllEscalationName(int sessionId) throws JSONException,
        PermissionException, SessionTimeoutException, SessionNotFoundException
    {
        return eventsBoss.listAllEscalationName(sessionId);
    }

    public int removeEscalation(int sessionId, Integer id) throws
        PermissionException, SessionTimeoutException, SessionNotFoundException
    {
        return eventsBoss.deleteEscalationById(sessionId, new Integer[]{id});
    }
}

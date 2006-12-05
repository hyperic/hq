package org.hyperic.hq.ui.json.action.escalation;

import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import java.rmi.RemoteException;

public class EscalationWebMediator
{
    private static EventsBoss eventsBoss;

    static {
        try {
           eventsBoss = EventsBossUtil.getHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (java.rmi.RemoteException e) {
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

    public JSONObject jsonByEscalationName(int sessionId, String name)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return eventsBoss.jsonByEscalationName(sessionId, name);
    }

    public JSONObject jsonByEscalationId(int sessionId, Integer id)
        throws JSONException, PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        return eventsBoss.jsonByEscalationId(sessionId, id);
    }

    public JSONObject jsonEscalationByAlertDefId(int sessionId,
                                                 Integer alertDefId)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return eventsBoss.jsonEscalationByAlertDefId(sessionId, alertDefId);
    }

    public JSONArray listAllEscalationName(int sessionId)
        throws JSONException,
               PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               RemoteException
    {
        return eventsBoss.listAllEscalationName(sessionId);
    }

    public void removeEscalation(int sessionId, Integer id)
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        eventsBoss.deleteEscalationById(sessionId, new Integer[]{id});
    }

    public void saveEscalation(int sessionId, JSONObject json)
        throws PermissionException,
               SessionTimeoutException,
               SessionNotFoundException,
               JSONException, RemoteException
    {
        eventsBoss.saveEscalation(sessionId, json);
    }

    public JSONObject makeJsonEscalation() throws JSONException, RemoteException
    {
        return eventsBoss.makeJsonEscalation();
    }
}

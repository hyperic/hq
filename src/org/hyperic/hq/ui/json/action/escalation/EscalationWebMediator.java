package org.hyperic.hq.ui.json.action.escalation;

import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.common.SystemException;
import org.json.JSONObject;
import org.json.JSONArray;

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

    public JSONObject jsonByEscalationName(int sessionId, String name)
    {
        return eventsBoss.jsonByEscalationName(sessionId, name);
    }

    public JSONObject jsonEscalationByAlertDefId(int sessionId,
                                                 Integer alertDefId)
    {
        return eventsBoss.jsonEscalationByAlertDefId(sessionId, alertDefId);
    }

    public JSONArray listAllEscalationName(int sessionId)
    {
        return eventsBoss.listAllEscalationName(sessionId);
    }
}

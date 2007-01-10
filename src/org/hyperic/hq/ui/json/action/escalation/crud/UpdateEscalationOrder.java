package org.hyperic.hq.ui.json.action.escalation.crud;

import java.rmi.RemoteException;
import java.util.Map;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.json.JSONException;

public class UpdateEscalationOrder extends BaseAction {

    public void execute(JsonActionContext context)
        throws PermissionException, SessionTimeoutException,
        SessionNotFoundException, JSONException, RemoteException {
        Map map = context.getParameterMap();
        if (map.get(ID) == null) {
            throw new IllegalArgumentException("Escalation id not found");
        }
        
        Integer id = context.getId();
        String order = ((String[]) map.get("order"))[0];
        
        // TODO Update the escalation actions order
    }

}

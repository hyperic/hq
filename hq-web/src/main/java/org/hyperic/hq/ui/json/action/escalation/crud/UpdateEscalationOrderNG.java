package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAction;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.springframework.stereotype.Component;


@Component(value = "updateEscalationOrderNG")
public class UpdateEscalationOrderNG extends BaseActionNG {

	@Resource
	private EscalationManager escalationManager;
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		JsonActionContextNG context = this.setJSONContext();
	       Map map = context.getParameterMap();

	        if (map.get(JSONConstants.ID) == null) {
	            throw new IllegalArgumentException("Escalation id not found");
	        }

	        Integer id = context.getId();
	        String[] sOrder = (String[]) map.get("viewEscalationUL[]");

	        Escalation esc = escalationManager.findById(id);
	        List actions = new ArrayList(sOrder.length);

	        for (int i = 0; i < sOrder.length; i++) {
	            EscalationAction action;
	            Integer actionId;

	            try {
	                actionId = Integer.valueOf(sOrder[i]);
	            } catch (NumberFormatException e) {
	                throw new SystemException("Bad order", e);
	            }

	            action = esc.getAction(actionId);
	            if (action == null) {
	                throw new IllegalArgumentException("Escalation does not " + "contain an action with " + "id=" +
	                                                   actionId);
	            }
	            actions.add(action);
	        }

	        escalationManager.updateEscalationOrder(esc, actions);
		
		
        inputStream = this.streamJSONResult(context);
		
		return null;
	}

}

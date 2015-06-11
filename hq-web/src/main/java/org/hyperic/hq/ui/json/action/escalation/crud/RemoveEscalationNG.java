package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.springframework.stereotype.Component;


@Component(value = "removeEscalationNG")
public class RemoveEscalationNG extends BaseActionNG {
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		JsonActionContextNG ctx = this.setJSONContext();
		
        EventsBoss eBoss = Bootstrap.getBean(EventsBoss.class);
        
        eBoss.deleteEscalationById(ctx.getSessionId(), ctx.getId()); 
		
        inputStream = this.streamJSONResult(ctx);
		
		return null;
	}
}

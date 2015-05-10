package org.hyperic.hq.ui.json.action.escalation.finder;

import java.io.InputStream;

import javax.annotation.Resource;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.springframework.stereotype.Component;


@Component(value = "jsonByEscalationIdNG")
public class JsonByEscalationIdNG extends BaseActionNG {

	@Resource
	private EventsBoss eBoss;
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}
	
	public String execute() throws Exception {
	
		JsonActionContextNG ctx = this.setJSONContext();
		
        Escalation e = eBoss.findEscalationById(ctx.getSessionId(), ctx.getId());
        JSONResult res = new JSONResult(Escalation.getJSON(e));
        ctx.setJSONResult(res);
		
        inputStream = this.streamJSONResult(ctx);
        
		return null;
	}
}

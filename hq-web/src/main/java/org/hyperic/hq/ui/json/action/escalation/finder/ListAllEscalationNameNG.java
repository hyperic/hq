package org.hyperic.hq.ui.json.action.escalation.finder;

import java.io.InputStream;

import javax.annotation.Resource;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

@Component(value = "listAllEscalationNameNG")
public class ListAllEscalationNameNG extends BaseActionNG {

	@Resource
	private EventsBoss eBoss;

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		JsonActionContextNG ctx = this.setJSONContext();
		JSONArray array = eBoss.listAllEscalationName(ctx.getSessionId());
		ctx.setJSONResult(new JSONResult(array));
		inputStream = this.streamJSONResult(ctx);
		return null;
	}
}

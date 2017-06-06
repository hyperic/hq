package org.hyperic.hq.ui.action.resource.common.monitor.alerts;

import java.io.InputStream;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("jsonAlertsRemoveActionNG")
@Scope("prototype")
public class JsonRemoveActionNG extends RemoveActionNG {

	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public String execute() throws Exception {

		request = getServletRequest();
		super.execute();
		JsonActionContextNG ctx = this.setJSONContext();

		JSONObject ajaxJson = new JSONObject();
		String requestURI = request.getRequestURI();
		// generate a new CSRF token for subsequent requests if needed
		ajaxJson.put("actionToken", response.encodeURL(requestURI));
		request.setAttribute(Constants.AJAX_JSON, ajaxJson);

		JSONResult res = new JSONResult(ajaxJson);
		ctx.setJSONResult(res);

		inputStream = this.streamJSONResult(ctx);

		return null;
	}

}

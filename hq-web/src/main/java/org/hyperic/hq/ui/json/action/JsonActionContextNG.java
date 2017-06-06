package org.hyperic.hq.ui.json.action;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.util.RequestUtils;

public class JsonActionContextNG extends HashMap {

	private static Object ACTION_REQUEST = new Object();
	private static Object ACTION_RESPONSE = new Object();
	private static Object JSON_RESULT = new Object();

	protected JsonActionContextNG() {
	}

	protected JsonActionContextNG(HttpServletRequest request,
			HttpServletResponse response) {
		put(ACTION_REQUEST, request);
		put(ACTION_RESPONSE, response);
	}

	public static JsonActionContextNG newInstance(HttpServletRequest request,
			HttpServletResponse response) {
		return new JsonActionContextNG(request, response);
	}

	public Integer getId() throws ParameterNotFoundException{
		String id;
		try {
			id = RequestUtils.getStringParameter(getRequest(), "id");
		} catch (ParameterNotFoundException e) {
			// missing id
			throw e;
		}
		return Integer.valueOf(id);
	}

	public EscalationAlertType getAlertDefType() {
		// XXX: implement for galertDef
		return ClassicEscalationAlertType.CLASSIC;
	}


	public int getSessionId() {
		try {
			return RequestUtils.getSessionId(getRequest()).intValue();
		} catch (ServletException e) {
			throw new SystemException(e);
		}
	}

	public Writer getWriter() throws IOException {
		return getResponse().getWriter();
	}

	public JSONResult getJSONResult() {
		return (JSONResult) get(JSON_RESULT);
	}

	public void setJSONResult(JSONResult result) {
		put(JSON_RESULT, result);
	}

	public boolean isPrettyPrint() {
		String pretty = getRequest().getParameter("pretty");
		return pretty != null;
	}

	public Map getParameterMap() {
		return getRequest().getParameterMap();
	}

	public ServletContext getServletContext() {
		return getSession().getServletContext();
	}

	public HttpSession getSession() {
		return getRequest().getSession();
	}

	private HttpServletResponse getResponse() {
		return (HttpServletResponse) get(ACTION_RESPONSE);
	}
	
    public HttpServletRequest getRequest()
    {
        return (HttpServletRequest)get(ACTION_REQUEST);
    }
}

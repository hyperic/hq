package org.hyperic.hq.ui.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.ServletRequestAware;

import com.opensymphony.xwork2.ActionSupport;

public class BaseAction_2 extends ActionSupport implements
		ServletRequestAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6789351187805196957L;

	protected static final boolean YES_RETURN_PATH = true;
	protected static final boolean NO_RETURN_PATH = false;

	private Log log = LogFactory.getLog(BaseAction_2.class.getName());

	protected HttpServletRequest _request;

	public void setServletRequest(HttpServletRequest request) {
		_request = request;

	}

	public HttpServletRequest getServletRequest() {
		return _request;
	}

	public String execute(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		return null;
	}

}

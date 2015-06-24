package org.hyperic.hq.ui.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.apache.struts2.interceptor.SessionAware;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.json.JSONException;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

@Component(value = "baseActionNG")
public class BaseActionNG extends ActionSupport implements SessionAware,
		ServletRequestAware, ServletResponseAware {

	private final Log log = LogFactory.getLog(BaseActionNG.class.getName());
	public static final String CANCELED = "canceled";
	public static final String RESET = "reset";
	public static final String CREATED = "added";
	public static final String ADD = "added";
	public static final String REMOVE = "removed";

	protected Map<String, Object> userSession;

	protected HttpServletRequest request;
	
	protected HttpServletResponse response;

	@Resource
	private AuthBoss authBoss;

	@Resource
	private AuthzBoss authzBoss;

	@Resource
	private ProductBoss productBoss;
	
	private Collection<String> customActionErrorMessages;


	public void setSession(Map<String, Object> session) {
		userSession = session;
	}

	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletRequest getServletRequest() {
		if(this.request != null){
			if (this.request.getSession() != null) {
				return this.request;
			}
		}
		return ServletActionContext.getRequest();
		
	}
	
	public void setServletResponse(HttpServletResponse response) {
		this.response = response;
	}
	public HttpServletResponse getServletResponse() {
		if(this.response == null){
			return this.response;
		}else{
			return ServletActionContext.getResponse();
		}
	}	

	/**
	 * Set the user for the current action.
	 * 
	 * @param request
	 *            The request to get the session to store the returnPath into.
	 * 
	 */

	protected void setUser() throws Exception {
		Map<String, Object> parameters = ActionContext.getContext()
				.getParameters();

		Integer userId = RequestUtils.getUserId(getServletRequest());
		Integer sessionId = RequestUtils.getSessionId(getServletRequest());

		if (log.isTraceEnabled()) {
			log.trace("finding user [" + userId + "]");
		}

		AuthzSubject user = authzBoss.findSubjectById(sessionId, userId);

		// when CAM is in LDAP mode, we may still have
		// users logging in with JDBC. the only way we can
		// distinguish these users is by checking to see
		// if they have an entry in the principals table.
		WebUser webUser = new WebUser(user.getAuthzSubjectValue());
		boolean hasPrincipal = authBoss.isUser(sessionId.intValue(),
				user.getName());
		webUser.setHasPrincipal(hasPrincipal);

		getServletRequest().setAttribute(Constants.USER_ATTR, webUser);

		getServletRequest().setAttribute(Constants.TITLE_PARAM_ATTR,
				BizappUtils.makeSubjectFullName(user));
		/*
		 * Enumeration<String> iter = request.getAttributeNames(); while
		 * (iter.hasMoreElements()){ String temp = (String) iter.nextElement();
		 * log.info(temp); log.info(request.getAttribute(temp)); }
		 * 
		 * log.info("*** Session ****"); HttpSession sess =
		 * getServletRequest().getSession(); iter = sess.getAttributeNames();
		 * while (iter.hasMoreElements()){ String temp = (String)
		 * iter.nextElement(); log.info(temp);
		 * log.info(request.getAttribute(temp)); }
		 */
	}

	protected void setPlugins() throws Exception {
		Collection<AttachmentDescriptor> a = productBoss.findAttachments(
				RequestUtils.getSessionIdInt(request), AttachType.ADMIN);

		this.request.setAttribute("adminAttachments", a);
	}


	// Calling this method provides the drop down of the Analyze tab in the UI
	protected void setHeaderResources() throws Exception {

		Integer sessionId = RequestUtils.getSessionId(request);
		Collection<AttachmentDescriptor> mastheadAttachments = productBoss.findAttachments(sessionId.intValue(), AttachType.MASTHEAD);

		ArrayList<AttachmentDescriptor> resourceAttachments = new ArrayList<AttachmentDescriptor>();
		ArrayList<AttachmentDescriptor> trackerAttachments = new ArrayList<AttachmentDescriptor>();
		for (AttachmentDescriptor d : mastheadAttachments) {
			AttachmentMasthead attachment = (AttachmentMasthead) d
					.getAttachment();
			if (attachment.getCategory().equals(ViewMastheadCategory.RESOURCE)) {
				resourceAttachments.add(d);
			} else if (attachment.getCategory().equals(
					ViewMastheadCategory.TRACKER)) {
				trackerAttachments.add(d);
			}
		}

		request.setAttribute("mastheadResourceAttachments", resourceAttachments);
		request.setAttribute("mastheadTrackerAttachments", trackerAttachments);
	}
	
	/**
     * Return an <code>ActionForward</code> if the form has been cancelled or
     * reset; otherwise return <code>null</code> so that the subclass can
     * continue to execute.
     */
    protected String checkSubmit(BaseValidatorForm spiderForm) throws Exception {
        

        if (spiderForm.isCancelClicked()) {
        	return CANCELED;
        }

        if (spiderForm.isResetClicked()) {
            spiderForm.reset();
            return RESET;
        }

        if (spiderForm.isCreateClicked()) {
            return CREATED;
        }

        if (spiderForm.isAddClicked()) {
            return ADD;
        }

        if (spiderForm.isRemoveClicked()) {
            return REMOVE;
        }

        return null;
    }
	
	protected JsonActionContextNG setJSONContext() throws Exception {
		
    	response.setContentType("text/javascript");
    	
    	// IE will cache these responses, so we need make sure this doesn't happen
    	// by setting the appropriate response headers.
    	response.addHeader("Pragma", "no-cache");
    	response.addHeader("Cache-Control", "no-cache");
    	response.addIntHeader("Expires", -1);
    	
    	JsonActionContextNG context = JsonActionContextNG.newInstance(request, response);
        return context;
	}
	
    protected InputStream streamJSONResult(JsonActionContextNG context)
            throws JSONException, IOException
    {
    	String outcome = null;
    	InputStream inputStream;
        if (context.getJSONResult() != null) {
            outcome = context.getJSONResult().writeToString( context.getWriter(), context.isPrettyPrint());
        }
        
        if (outcome != null) {
        	inputStream = new ByteArrayInputStream(outcome.getBytes());
        	return inputStream;
    	}
        
        return null;
    }

    protected Map<Integer, String> getPaggingList(int totalSize) {
		
		Map<Integer, String> retVal = new LinkedHashMap<Integer, String>();
		retVal.put(15, getText("ListToolbar.ItemsPerPage.15"));
		if (totalSize > 15) {
			retVal.put(30, getText("ListToolbar.ItemsPerPage.30"));
		}
		if (totalSize > 30) {
			retVal.put(50, getText("ListToolbar.ItemsPerPage.50"));
		}
		if (totalSize > 50) {
			retVal.put(100, getText("ListToolbar.ItemsPerPage.100"));
		}
		if (totalSize > 100) {
			retVal.put(250, getText("ListToolbar.ItemsPerPage.250"));
		}
		if (totalSize > 250) {
			retVal.put(500, getText("ListToolbar.ItemsPerPage.5900"));
		}
		return retVal;
	} 
    
	public Collection<String> getCustomActionErrorMessages() {
		return customActionErrorMessages;
	}
	
	public String getCustomActionErrorMessagesForDisplay() {
		StringBuffer sb = null;
		if (this.customActionErrorMessages != null) {
			sb = new StringBuffer();
			Iterator<String> iter = this.customActionErrorMessages.iterator();
			if (iter.hasNext()) {
				while (iter.hasNext()) {
					sb.append(iter.next());
				}
			}
			return sb.toString();
		}
		return null;
	}

	public void setCustomActionErrorMessages(
			Collection<String> customActionErrorMessages) {
        if (this.customActionErrorMessages == null) {
        	this.customActionErrorMessages = new ArrayList<String>();
        }

        this.customActionErrorMessages = customActionErrorMessages;
	}
	
	public void addCustomActionErrorMessages(
			String msg) {
        if (this.customActionErrorMessages == null) {
        	this.customActionErrorMessages = new ArrayList<String>();
        }

        this.customActionErrorMessages.add(msg);
	}
	
	public void clearCustomErrorMessages(){
		if (this.customActionErrorMessages != null) {
			this.customActionErrorMessages.clear();
		}
	}
}

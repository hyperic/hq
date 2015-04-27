package org.hyperic.hq.ui.action;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.SessionAware;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.admin.home.AdminHomePortalActionNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

@Component(value="baseActionNG")
public class BaseActionNG extends ActionSupport implements SessionAware, ServletRequestAware {

	private final Log log = LogFactory.getLog(BaseActionNG.class.getName());
	
    protected Map<String, Object> userSession ;
    
    protected HttpServletRequest request;	
    
    @Resource
    private AuthBoss authBoss;
    
    @Resource
    private AuthzBoss authzBoss;
	
	@Resource
	private ProductBoss productBoss;
    
    public void setSession(Map<String, Object> session) {
        userSession = session ;
     }

 
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
 
	public HttpServletRequest getServletRequest() {
		return this.request;
	}
    
    /**
     * Set the user for the current action.
     * 
     * @param request The request to get the session to store the returnPath
     *        into.
     * 
     */
	
    protected void setUser() throws Exception {
    	Map<String,Object> parameters = ActionContext.getContext().getParameters();

        Integer userId = RequestUtils.getUserId(getServletRequest());
        Integer sessionId =RequestUtils.getSessionId( getServletRequest());

        if (log.isTraceEnabled()) {
            log.trace("finding user [" + userId + "]");
        }
        
        AuthzSubject user = authzBoss.findSubjectById(sessionId, userId);

        // when CAM is in LDAP mode, we may still have
        // users logging in with JDBC. the only way we can
        // distinguish these users is by checking to see
        // if they have an entry in the principals table.
        WebUser webUser = new WebUser(user.getAuthzSubjectValue());
        boolean hasPrincipal = authBoss.isUser(sessionId.intValue(), user.getName());
        webUser.setHasPrincipal(hasPrincipal);
        
        getServletRequest().setAttribute(Constants.USER_ATTR, webUser);
        
        getServletRequest().setAttribute(Constants.TITLE_PARAM_ATTR, BizappUtils.makeSubjectFullName(user));
        /*
        Enumeration<String> iter = request.getAttributeNames();
        while (iter.hasMoreElements()){
        	String temp = (String) iter.nextElement();
        	log.info(temp); 
        	log.info(request.getAttribute(temp));
        }
        
        log.info("*** Session ****"); 
        HttpSession sess =   getServletRequest().getSession();
        iter = sess.getAttributeNames();
        while (iter.hasMoreElements()){
        	String temp = (String) iter.nextElement();
        	log.info(temp); 
        	log.info(request.getAttribute(temp));
        }
        */
    }
    
    protected void setPlugins() throws Exception {
		Collection<AttachmentDescriptor> a = productBoss.findAttachments(
				RequestUtils.getSessionIdInt(request), AttachType.ADMIN);

		this.request.setAttribute("adminAttachments", a);
    }
}

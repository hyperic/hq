package org.hyperic.hq.ui.action.admin.config;

import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.config.EscalationSchemeFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


@Component(value = "configActionNG")
public class ConfigActionNG extends BaseActionNG implements ModelDriven<EscalationSchemeFormNG>{

	
	private final Log log = LogFactory.getLog(ConfigActionNG.class.getName());
	
	@Resource
    private AuthzBoss authzBoss;
	
	@Resource
    private ConfigBoss configBoss;
	
	@Resource
    private AppdefBoss appdefBoss;
	
	private EscalationSchemeFormNG escalationSchemeForm = new EscalationSchemeFormNG();
	

    public String escalate() throws Exception {
		// createPortal(request, false, "admin.home.EscalationSchemes", ".admin.config.EditEscalationConfig");
    	Map<String, Boolean> userOperationsMap = ( Map<String, Boolean>) request.getSession().getAttribute(Constants.USER_OPERATIONS_ATTR);
    	setHeaderResources();
		Integer sessionId = RequestUtils.getSessionId(request);

		PageList<AuthzSubjectValue> availableUsers = authzBoss.getAllSubjects(sessionId, null, PageControl.PAGE_ALL);
		request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

		Properties props = configBoss.getConfig();

		// See if the property exists
		if (props.containsKey(HQConstants.SNMPVersion)) {
			String ver = props.getProperty(HQConstants.SNMPVersion);
			request.setAttribute("snmpEnabled", new Boolean(ver.length() > 0));
		}
		return "escalateForm";
    }


	public EscalationSchemeFormNG getEscalationSchemeFormNG() {
		return escalationSchemeForm;
	}


	public void setEscalationSchemeFormNG(EscalationSchemeFormNG escalationSchemeFormNG) {
		this.escalationSchemeForm = escalationSchemeFormNG;
	}
    
    public EscalationSchemeFormNG getModel() {
    	return escalationSchemeForm;
    }
}

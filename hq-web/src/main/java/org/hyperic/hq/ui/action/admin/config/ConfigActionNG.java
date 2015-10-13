package org.hyperic.hq.ui.action.admin.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.config.EscalationSchemeFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component(value = "configActionNG")
public class ConfigActionNG extends BaseActionNG implements
		ModelDriven<EscalationSchemeFormNG> {

	private final Log log = LogFactory.getLog(ConfigActionNG.class.getName());

	@Resource
	private AuthzBoss authzBoss;

	@Resource
	private ConfigBoss configBoss;

	@Resource
	private AppdefBoss appdefBoss;

	private EscalationSchemeFormNG escalationSchemeForm = new EscalationSchemeFormNG();

	public String escalate() throws Exception {
		// createPortal(request, false, "admin.home.EscalationSchemes",
		// ".admin.config.EditEscalationConfig");
//		Map<String, Boolean> userOperationsMap = (Map<String, Boolean>) request
//				.getSession().getAttribute(Constants.USER_OPERATIONS_ATTR);
		request = getServletRequest();
		setHeaderResources();
		if (!validateDescription(escalationSchemeForm.getDescription() ) ){
			addFieldError("description", getText("alert.config.error.250Char") );
			return INPUT;
		}
		Integer sessionId = RequestUtils.getSessionId(request);

		PageList<AuthzSubjectValue> availableUsers = authzBoss.getAllSubjects(
				sessionId, null, PageControl.PAGE_ALL);
		request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

		Properties props = configBoss.getConfig();

		// See if the property exists
		if (props.containsKey(HQConstants.SNMPVersion)) {
			String ver = props.getProperty(HQConstants.SNMPVersion);
			request.setAttribute("snmpEnabled", new Boolean(ver.length() > 0));
		}
		return "escalateForm";
	}

	public String monitor()throws Exception {
		Integer sessionId = RequestUtils.getSessionId(getServletRequest());

		if (!BizappUtilsNG.canAdminHQ(sessionId, authzBoss))
			throw new PermissionException("User not authorized to configure "
					+ "monitor defaults");

		int session = sessionId.intValue();
		List<PlatformTypeValue> platTypes = appdefBoss.findAllPlatformTypes(
				session, PageControl.PAGE_ALL);
		getServletRequest().setAttribute(Constants.ALL_PLATFORM_TYPES_ATTR, platTypes);

		List<ServerTypeValue> serverTypes = appdefBoss.findAllServerTypes(
				session, PageControl.PAGE_ALL);

		// Get the special service types sans windows special case
		// XXX: What special case?
		List<ServiceTypeValue> platServices = new ArrayList<ServiceTypeValue>();
		List<ServiceTypeValue> winServices = new ArrayList<ServiceTypeValue>();
		LinkedHashMap<ServerTypeValue, List<ServiceTypeValue>> serverTypesMap = new LinkedHashMap<ServerTypeValue, List<ServiceTypeValue>>();
		for (int i = 0; i < serverTypes.size(); i++) {
			ServerTypeValue stv = serverTypes.get(i);
			List<ServiceTypeValue> serviceTypes = appdefBoss
					.findServiceTypesByServerType(session, stv.getId()
							.intValue());
			if (stv.getVirtual()) {
				if (stv.getName().startsWith("Win")) {
					winServices.addAll(serviceTypes);
				} else {
					platServices.addAll(serviceTypes);
				}
			} else {
				serverTypesMap.put(stv, serviceTypes);
			}
		}
		getServletRequest().setAttribute(Constants.ALL_SERVER_TYPES_ATTR, serverTypesMap);
		getServletRequest().setAttribute(Constants.ALL_PLATFORM_SERVICE_TYPES_ATTR,
				platServices);
		getServletRequest().setAttribute(Constants.ALL_WINDOWS_SERVICE_TYPES_ATTR,
				winServices);

		

		return "monitoringDefaults";
	}

	public EscalationSchemeFormNG getEscalationSchemeFormNG() {
		return escalationSchemeForm;
	}

	public void setEscalationSchemeFormNG(
			EscalationSchemeFormNG escalationSchemeFormNG) {
		this.escalationSchemeForm = escalationSchemeFormNG;
	}

	public EscalationSchemeFormNG getModel() {
		return escalationSchemeForm;
	}
	
	private boolean validateDescription(String description){
		
		if ( (description!=null) &&   (description.length() < 251) ) {
			return false;
		} 
		return true;
	}
}

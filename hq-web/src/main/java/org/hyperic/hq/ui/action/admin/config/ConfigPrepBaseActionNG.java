package org.hyperic.hq.ui.action.admin.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.vm.VCConfig;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.util.config.ConfigResponse;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(value = "configPrepBaseActionNG")
@Scope(value = "prototype")
public class ConfigPrepBaseActionNG extends BaseActionNG {
//	private final Log log = LogFactory.getLog(ConfigPrepBaseActionNG.class.getName());
	protected Log log; 

	@Resource
	private ConfigBoss configBoss;

	@Resource
	private UpdateBoss updateBoss;

	@Resource
	private VCManager vcManager;

	@Resource
	private SessionManager sessionManager;

	protected SystemConfigFormNG cForm;

	private List<String> updateModes;

	public String save() throws Exception {

		String checkResult = checkSubmit(cForm);
		if (checkResult != null) {
			return checkResult;
		}
		int sessionId = RequestUtils.getSessionIdInt(request);

		AuthzSubject subject = sessionManager.getSubject(sessionId);

		if (cForm.isOkClicked()) {
			if (log.isTraceEnabled())
				log.trace("Getting config");
			if (!cForm.getVCenterURL().isEmpty()
					&& !cForm.getVCenterUser().isEmpty()
					&& !cForm.getVCenterPassword().isEmpty()) {
				handleVCenterSettings(cForm, subject);
			}
			Properties props = cForm.saveConfigProperties(configBoss.getConfig());

			if (log.isTraceEnabled())
				log.trace("Setting config");
			configBoss.setConfig(sessionId, props);

			// Set the update mode

			updateBoss.setUpdateMode(sessionId,
					UpdateStatusMode.findByCode(cForm.getUpdateMode()));
		}

		clearMessages(); 	
		addActionMessage(getText("admin.config.confirm.saveSettings"));
		return "settingsSaved";

	}

	private void handleVCenterSettings(SystemConfigFormNG cForm,
			AuthzSubject subject) {
		try {
			VCConfig vc = vcManager.getVCConfigSetByUI();

			if (null != vc) {
				vc.setUrl(cForm.getVCenterURL());
				vc.setUser(cForm.getVCenterUser());
				if (!ConfigResponse.CONCEALED_SECRET_VALUE.equals(cForm
						.getVCenterPassword())) {
					vc.setPassword(cForm.getVCenterPassword());
				}
				vcManager.updateVCConfig(vc);
			} else {
				vcManager.addVCConfig(cForm.getVCenterURL(),
						cForm.getVCenterUser(), cForm.getVCenterPassword(),
						true);
			}
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();

		cForm.reset();
		clearErrorsAndMessages();
		return "reset";
	}

	@SkipValidation
	public String edit() throws Exception {

		setHeaderResources();
		if (cForm == null) {
			cForm = new SystemConfigFormNG();
		}

		if (log.isTraceEnabled()) {
			log.trace("getting config");
		}

		Properties props = configBoss.getConfig();
		cForm.loadConfigProperties(props);
		cForm.loadVCProps(vcManager.getVCConfigSetByUI());

		// Set the update mode
		UpdateStatusMode upMode = updateBoss.getUpdateMode();
		cForm.setUpdateMode(upMode.getCode());

		// Set the HQ SNMP local engine id
		String localEngineID = "0x"
				+ new OctetString(MPv3.createLocalEngineID());
		request.setAttribute(Constants.SNMP_LOCAL_ENGINE_ID, localEngineID);

		// set "#CONCEALED_SECRET_VALUE#" to be returned to the ui
		String vCenterPassword = cForm.getVCenterPassword();
		if ((vCenterPassword != null) && !vCenterPassword.equals("")) {
			cForm.setVCenterPassword(ConfigResponse.CONCEALED_SECRET_VALUE);
		}

		List<String> updateModes = new ArrayList<String>();
		updateModes.add("all");
		updateModes.add("admin.settings.Major");
		updateModes.add("common.label.None");
		return "adminEditConfig";
	}

	public List<String> getUpdateModes() {
		return updateModes;
	}

	public void setUpdateModes(List<String> updateModes) {
		this.updateModes = updateModes;
	}

}

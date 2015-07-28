package org.hyperic.hq.ui.action.admin.config;

import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component(value = "editConfigPrepActionNG")
@Scope(value = "prototype")
public class EditConfigPrepActionNG extends ConfigPrepBaseActionNG implements
		ModelDriven<SystemConfigFormNG> {

	public EditConfigPrepActionNG () {
		super.log = LogFactory.getLog(EditConfigPrepActionNG.class.getName());
		super.cForm = new SystemConfigFormNG();
	}
	
	public SystemConfigFormNG getModel() {
		return cForm;
	}

	public SystemConfigFormNG getcForm() {
		return cForm;
	}

	public void setcForm(SystemConfigFormNG cForm) {
		this.cForm = cForm;
	}

}

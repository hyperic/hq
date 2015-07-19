package org.hyperic.hq.ui.action.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.springframework.stereotype.Component;

@Component(value = "managementActionNG")
public class ManagementActionNG extends BaseActionNG {

	private final Log log = LogFactory.getLog(ManagementActionNG.class
			.getName());

	public String execute() throws Exception {
		setHeaderResources();

		setPlugins();

		return SUCCESS;
	}

}

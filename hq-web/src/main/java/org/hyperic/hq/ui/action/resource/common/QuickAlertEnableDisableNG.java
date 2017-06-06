package org.hyperic.hq.ui.action.resource.common;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("quickAlertEnableDisableNG")
@Scope("prototype")
public class QuickAlertEnableDisableNG extends BaseActionNG {
	private final Log log = LogFactory.getLog(QuickAlertEnableDisableNG.class);
	@Resource
	private EventsBoss eventsBoss;
	
	private String internalEid;
	private String alertsConfigView;
	
	
	public String enableAlert() throws Exception {
		
		this.flipAlertsStatus(true);
		
		return SUCCESS;
	}
	
	public String disableAlert() throws Exception {
		this.flipAlertsStatus(false);
		return SUCCESS;
	}
	
	private void flipAlertsStatus(boolean newState) {
		try {
			Integer sessionId = RequestUtils.getSessionId(request);
	
			AppdefEntityID aeid = RequestUtils.getEntityId(request);
			if (aeid!= null) {
				setInternalEid(aeid.toString());
			}
			
			AppdefEntityID[] entities = new AppdefEntityID[1];
			entities[0] = aeid;
	
			eventsBoss.activateAlertDefinitions(sessionId.intValue(), entities, newState);
		} catch (Exception ex) {
			log.error(ex,ex);
		}
		
	}
	

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public String getAlertsConfigView() {
		return alertsConfigView;
	}

	public void setAlertsConfigView(String alertsConfigView) {
		this.alertsConfigView = alertsConfigView;
	}
}

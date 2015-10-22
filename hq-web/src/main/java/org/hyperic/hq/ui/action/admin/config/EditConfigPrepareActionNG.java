package org.hyperic.hq.ui.action.admin.config;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.vm.VCManager;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;
import org.springframework.stereotype.Component;
import org.hyperic.hq.ui.Constants;
import org.hyperic.util.config.ConfigResponse;

@Component(value = "EditConfigPrepareActionNG")
public class EditConfigPrepareActionNG extends BaseActionNG implements ViewPreparer {

	
    private final Log log = LogFactory.getLog(EditConfigPrepareActionNG.class.getName());
    @Resource
    private ConfigBoss configBoss;
    @Resource
    private UpdateBoss updateBoss;
    @Resource
    private VCManager vcManager;
    
	public void execute(TilesRequestContext tilesContext, AttributeContext attributeContext) {
		try {
			this.request = getServletRequest();
	        SystemConfigFormNG cForm = new SystemConfigFormNG();
	
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
	        String localEngineID = "0x" + new OctetString(MPv3.createLocalEngineID());
	        request.setAttribute(Constants.SNMP_LOCAL_ENGINE_ID, localEngineID);
	        
	        // set "#CONCEALED_SECRET_VALUE#" to be returned to the ui
	        String vCenterPassword = cForm.getVCenterPassword();
	        if ((vCenterPassword!=null) && !vCenterPassword.equals("")) {
	            cForm.setVCenterPassword(ConfigResponse.CONCEALED_SECRET_VALUE);
	        }
	        request.setAttribute("editForm", cForm);
		} catch (Exception ex) {
			log.error(ex,ex);
		}

	}

}

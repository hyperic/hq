package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import com.vmware.springsource.hyperic.plugin.gemfire.GemFireLiveData;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class GemfirePlatformDetector extends PlatformDetector {

    private static Log log = LogFactory.getLog(GemfirePlatformDetector.class);
    private static Map<String, ConfigResponse> configs = new HashMap();

    @Override
    public PlatformResource getPlatformResource(ConfigResponse config) throws PluginException {
        log.debug("[getPlatformResource] config=" + config);
        PlatformResource res = super.getPlatformResource(config);

        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(config.toProperties());
            log.debug("mServer=" + mServer);
            String id=GemFireLiveData.getSystemID(mServer);
            log.debug("[getPlatformResource] id='"+id+"' config="+config);
            configs.put(id, config);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        return res;
    }

    public static void runAutoDiscovery(String id) {

        log.debug("[runAutoDiscovery] id=" + id + " >> start");
        try {
            ScanConfigurationCore scanConfig = new ScanConfigurationCore();
            ConfigResponse c = configs.get(id);
            if (c != null) {
                scanConfig.setConfigResponse(c);
                AgentRemoteValue configARV = new AgentRemoteValue();
                scanConfig.toAgentRemoteValue(AICommandsAPI.PROP_SCANCONFIG, configARV);
                AgentCommand ac = new AgentCommand(1, 1, "autoinv:startScan", configARV);
                AgentDaemon.getMainInstance().getCommandDispatcher().processRequest(ac, null, null);
                log.info("[runAutoDiscovery] id=" + id + " << OK");
            } else {
                log.debug("[runAutoDiscovery] Config not found for id=" + id);
            }
        } catch (Exception ex) {
            log.error("[runAutoDiscovery] id=" + id + " " + ex, ex);
        }
    }
}

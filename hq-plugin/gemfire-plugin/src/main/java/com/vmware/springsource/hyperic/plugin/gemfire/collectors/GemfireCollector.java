package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import com.vmware.springsource.hyperic.plugin.gemfire.detectors.GemfirePlatformDetector;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentCommand;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

public class GemfireCollector extends Collector {

    static Log log = LogFactory.getLog(GemfireCollector.class);
    String last_signature = "";

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    @Override
    public void collect() {
        int a = 0;
        int c = 0;
        int g = 0;
        Properties props = getProperties();
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            log.debug("mServer=" + mServer);

            ObjectName mbean = new ObjectName("GemFire:type=MemberInfoWithStatsMBean");
            String id = (String) mServer.getAttribute(mbean, "Id");

            Object[] args = {};
            String[] def = {};
            String[] members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);

            String signature = Arrays.asList(members).toString();
            if (!signature.equals(last_signature)) {
                last_signature=signature;
                GemfirePlatformDetector.runAutoDiscovery(id);
            }

            for (String menber : members) {
                Object[] args2 = {menber};
                String[] def2 = {String.class.getName()};
                Map memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
                if ("true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString())) {
                    ++g;
                } else if ("true".equalsIgnoreCase(memberDetails.get("gemfire.member.isserver.boolean").toString())) {
                    ++c;
                } else {
                    ++a;
                }
                log.debug("---> isserver=" + memberDetails.get("gemfire.member.isserver.boolean"));
            }
            setAvailability(true);
            setValue("n_gateways", new Double(g).doubleValue());
            setValue("n_apps", new Double(a).doubleValue());
            setValue("n_caches", new Double(c).doubleValue());
        } catch (Exception ex) {
            setAvailability(false);
            log.debug(ex, ex);
        }
    }
}

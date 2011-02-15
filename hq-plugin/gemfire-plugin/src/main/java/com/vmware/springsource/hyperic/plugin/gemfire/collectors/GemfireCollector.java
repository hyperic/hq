package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import com.vmware.springsource.hyperic.plugin.gemfire.GemFireLiveData;
import com.vmware.springsource.hyperic.plugin.gemfire.GemFireUtils;
import com.vmware.springsource.hyperic.plugin.gemfire.detectors.GemfirePlatformDetector;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

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

            String id=GemFireLiveData.getSystemID(mServer);

            List<String> members=GemFireUtils.getMembers(mServer);
            String signature = Arrays.asList(members).toString();
            if (!signature.equals(last_signature)) {
                last_signature=signature;
                GemfirePlatformDetector.runAutoDiscovery(id);
                GemFireUtils.clearNameCache();
            }

            for (String memberID : members) {
                Map memberDetails = GemFireUtils.getMemberDetails(memberID, mServer);
                if ("true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString())) {
                    ++g;
                } else if ("true".equalsIgnoreCase(memberDetails.get("gemfire.member.isserver.boolean").toString())) {
                    ++c;
                } else {
                    ++a;
                }
            }
            setAvailability(true);
            setValue("n_gateways", new Double(g).doubleValue());
            setValue("n_apps", new Double(a).doubleValue());
            setValue("n_caches", new Double(c).doubleValue());
        } catch (Exception ex) {
            setAvailability(false);
            log.debug("[collect] "+ex.getMessage(), ex);
        }
    }
}

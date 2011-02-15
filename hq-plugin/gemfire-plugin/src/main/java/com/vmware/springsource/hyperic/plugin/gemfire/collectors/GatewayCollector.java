package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import com.vmware.springsource.hyperic.plugin.gemfire.GemFireUtils;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

public class GatewayCollector extends Collector {

    static Log log = LogFactory.getLog(GatewayCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    public void collect() {
        Properties props = getProperties();
        log.debug("[collect] props=" + props);
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            String memberID = GemFireUtils.memberNameToMemberID(props.getProperty("member.name"), mServer);
            Map memberDetails = GemFireUtils.getMemberDetails(memberID, mServer);

            List<Map> gateways = (List) memberDetails.get("gemfire.member.gatewayhub.gateways.collection");
            String id = (String) props.get("gatewayID");
            for (Map gateway : gateways) {
                if (((String) gateway.get("gemfire.member.gateway.id.string")).equals(id)) {
                    setAvailability(((Boolean) gateway.get("gemfire.member.gateway.isconnected.boolean")));
                    setValue("queuesize", (Integer) gateway.get("gemfire.member.gateway.queuesize.int"));
                }
            }
        } catch (Exception ex) {
            setAvailability(false);
            log.debug("[collect] " + ex.getMessage(), ex);
        }
    }
}

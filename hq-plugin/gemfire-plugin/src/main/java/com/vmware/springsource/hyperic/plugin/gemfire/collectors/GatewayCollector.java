package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
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
        JMXConnector connector = null;
        try {
            connector = MxUtil.getMBeanConnector(props);
            MBeanServerConnection mServer = connector.getMBeanServerConnection();
            String memberID = props.getProperty("memberID");
            Object[] args2 = {memberID};
            String[] def2 = {String.class.getName()};
            Map memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            if (!memberDetails.isEmpty()) {
                List<Map> gateways = (List) memberDetails.get("gemfire.member.gatewayhub.gateways.collection");
                String id = (String) props.get("gatewayID");
                for (Map gateway : gateways) {
                    if (((String) gateway.get("gemfire.member.gateway.id.string")).equals(id)) {
                        setAvailability(((Boolean)gateway.get("gemfire.member.gateway.isconnected.boolean")));
                        setValue("queuesize", (Integer)gateway.get("gemfire.member.gateway.queuesize.int"));
                    }
                }
            } else {
                log.debug("Member '" + memberID + "' nof found!!!");
                setAvailability(false);
            }
        } catch (Exception ex) {
            setAvailability(false);
            log.debug(ex, ex);
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (IOException e) {
                log.debug(e, e);
            }
        }
    }
}

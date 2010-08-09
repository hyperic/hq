package com.vmware.springsource.hyperic.plugin.gemfire;

import java.io.IOException;
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

public class GemfireCollector extends Collector {

    static Log log = LogFactory.getLog(GemfireCollector.class);

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
        JMXConnector connector = null;
        try {
            connector = MxUtil.getMBeanConnector(props);
            MBeanServerConnection mServer = connector.getMBeanServerConnection();
            Object[] args = {};
            String[] def = {};
            String[] members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);
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

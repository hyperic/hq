package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

public class MemberCollector extends Collector {

    static Log log = LogFactory.getLog(MemberCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    @Override
    public void collect() {
        int a = 0, c = 0, g = 0;
        Properties props = getProperties();
        JMXConnector connector = null;
        try {
            connector = MxUtil.getMBeanConnector(props);
            MBeanServerConnection mServer = connector.getMBeanServerConnection();
            String memberID = props.getProperty("memberID");
            Object[] args2 = {memberID};
            String[] def2 = {String.class.getName()};
            Map memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            if (!memberDetails.isEmpty()) {
                if(log.isDebugEnabled()){
                    log.debug("[collect] memberDetails="+memberDetails);
                }
                long max = ((Long) memberDetails.get("gemfire.member.stat.maxmemory.long"));
                long used = ((Long) memberDetails.get("gemfire.member.stat.usedmemory.long"));
                if (max > 0) {
                    setValue("memory", (used * 100 / max));
                }
                setValue("cpu", (Integer) memberDetails.get("gemfire.member.stat.cpus.int"));
                setValue("uptime", (Long) memberDetails.get("gemfire.member.uptime.long"));
                setValue("clients", ((Map) memberDetails.get("gemfire.member.clients.map")).size());

                setAvailability(true);
                setAvailability(true);
            } else {
                log.debug("[collect] Member '" + memberID + "' not found!!!");
                setAvailability(Metric.AVAIL_PAUSED);
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

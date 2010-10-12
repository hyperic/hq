package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

public class RegionCollector extends Collector {

    static Log log = LogFactory.getLog(RegionCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    public void collect() {
        Properties props = getProperties();
        log.debug("[collect] props=" + props);
        setAvailability(true);
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            log.info("mServer=" + mServer);
            String memberID = props.getProperty("memberID");
            Object[] args2 = {memberID};
            String[] def2 = {String.class.getName()};
            Map memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            if (!memberDetails.isEmpty()) {
                Map<Object, Map> regions = (Map) memberDetails.get("gemfire.member.regions.map");
                for (Map region : regions.values()) {
                    if (log.isDebugEnabled()) {
                        log.debug("[collect] region=" + region);
                    }
                    String name = (String) region.get("gemfire.region.name.string");
                    setValue(name + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
                    setValue(name + ".entry_count", ((Integer) region.get("gemfire.region.entrycount.int")).intValue());
                }
            } else {
                log.debug("Member '" + memberID + "' nof found!!!");
                setAvailability(false);
            }
        } catch (Exception ex) {
            log.debug(ex, ex);
            setAvailability(false);
        }
    }
}

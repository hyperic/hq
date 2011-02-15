package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import com.vmware.springsource.hyperic.plugin.gemfire.GemFireUtils;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
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
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            String memberID = GemFireUtils.memberNameToMemberID(props.getProperty("member.name"), mServer);
            Map memberDetails = GemFireUtils.getMemberDetails(memberID, mServer);

            Map<Object, Map> regions = (Map) memberDetails.get("gemfire.member.regions.map");
            for (Map region : regions.values()) {
                if (log.isDebugEnabled()) {
                    log.debug("[collect] region=" + region);
                }
                String name = (String) region.get("gemfire.region.name.string");
                setValue(name + "." + Metric.ATTR_AVAIL, Metric.AVAIL_UP);
                setValue(name + ".entry_count", ((Integer) region.get("gemfire.region.entrycount.int")).intValue());
            }
        } catch (Exception ex) {
            log.debug(ex, ex);
        }
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = result.getMetricValue(metric.getAttributeName());
        if (metric.getAttributeName().endsWith(Metric.ATTR_AVAIL)) {
            if (res.getValue() != Metric.AVAIL_UP) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            }
            log.debug("[getValue] Member=" + metric.getObjectProperty("member.name") + " metric=" + metric.getAttributeName() + " res=" + res.getValue());
        }
        return res;
    }
}

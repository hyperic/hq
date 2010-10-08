package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxUtil;

public class MemberCollector extends Collector {

    static Log log = LogFactory.getLog(MemberCollector.class);

    //GemFire.Statistic:source=%memberID%,name=vmStats
    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        log.debug("[init] props=" + props);
        super.init();
    }

    @Override
    public void collect() {
        Properties props = getProperties();
        try {
            MBeanServerConnection mServer = MxUtil.getMBeanServer(props);
            log.info("mServer=" + mServer);
            String memberID = props.getProperty("memberID");
            addValues(getMetrics(memberID, mServer, false));
            setAvailability(true);
        } catch (Exception ex) {
            setAvailability(false);
            log.debug(ex, ex);
        }
    }
    private static final int prefixLength = "gemfire.member.".length();

    public static Map getMetrics(String memberID, MBeanServerConnection mServer, boolean hqu) {
        Map res = new java.util.HashMap<String, Object>();

        try {
            //refresh(memberID,mServer);

            Object[] args2 = {memberID};
            String[] def2 = {String.class.getName()};
            Map<String, Object> memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
            for (String k : memberDetails.keySet()) {
                res.put(k.substring(prefixLength, k.lastIndexOf('.')), memberDetails.get(k));
            }
        } catch (Exception ex) {
            log.debug(ex, ex);
        }

        double max = ((Long) res.get("stat.maxmemory"));
        double used = ((Long) res.get("stat.usedmemory"));
        res.put("used_memory", (used / max));
        res.put("used_cpu", calcCPU(res, memberID, hqu));
        res.put("uptime", ((Long) res.get("uptime")) / 1000);
        res.put("nclients", ((Map) res.get("clients")).size());

        return res;
    }
    static Map cpu_cache = new HashMap();

    private static double calcCPU(Map metrics, String memberID, boolean hqu) {
        double cpu = 0;
        log.debug("[calcCPU] memberID=" + memberID + " metrics=" + metrics);
        String cacheKey = (hqu ? "hqu_" : "") + memberID;

        long last_cpu = ((Long) metrics.get("stat.processcputime") / 1000000);
        long last_time = System.currentTimeMillis();

        CPUCacheEntry c_entry = (CPUCacheEntry) cpu_cache.get(cacheKey);
        if (c_entry != null) {
            long prev_cpu = c_entry.cpuTime;
            long prev_time = c_entry.time;
            int cpus = (Integer) metrics.get("stat.cpus");
            cpu = (double) (last_cpu - prev_cpu) / (double) ((last_time - prev_time) * cpus);
            log.debug("[calcCPU] cacheKey=" + cacheKey + " prev_cpu=" + prev_cpu + " last_cpu=" + last_cpu + " prev_time=" + prev_time + " last_time=" + last_time + " cpus=" + cpus);
            log.debug("[calcCPU] " + (last_cpu - prev_cpu) + "/" + ((last_time - prev_time) * cpus) + " cpu=" + cpu);
        } else {
            c_entry = new CPUCacheEntry();
        }

        c_entry.cpuTime = last_cpu;
        c_entry.time = last_time;
        cpu_cache.put(cacheKey, c_entry);
        return cpu;
    }

    private static class CPUCacheEntry {

        long cpuTime = 0, time = 0;
    }
}

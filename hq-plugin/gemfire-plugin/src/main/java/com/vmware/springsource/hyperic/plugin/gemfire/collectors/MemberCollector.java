package com.vmware.springsource.hyperic.plugin.gemfire.collectors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.management.Attribute;
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

    //GemFire.Statistic:source=%memberID%,name=vmStats
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
                if (log.isDebugEnabled()) {
                    log.debug("[collect] memberDetails=" + memberDetails);
                }
                setValue("uptime", ((Long) memberDetails.get("gemfire.member.uptime.long"))/1000);
                setValue("clients", ((Map) memberDetails.get("gemfire.member.clients.map")).size());

                addValues(getMetrics(memberID, mServer, false));

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
    private final static String[] vmStats_metrics = {"processCpuTime", "cpus", "threads"};
    private final static String[] cachePerfStats_metrics = {"gets", "puts", "getTime", "putTime"};
    private final static String[] vmHeapMemoryStats_metrics = {"usedMemory", "maxMemory"};
    private final static String[] distributionStats_metrics = {"receivers", "sentBytes", "receivedBytes"};
    private final static Map<String, String[]> metrics = new HashMap();

    static {
        metrics.put("vmStats", vmStats_metrics);
        metrics.put("cachePerfStats", cachePerfStats_metrics);
        metrics.put("vmHeapMemoryStats", vmHeapMemoryStats_metrics);
        metrics.put("distributionStats", distributionStats_metrics);
    }

    public static Map getMetrics(String memberID, MBeanServerConnection mServer, boolean hqu) {
        Map res = new java.util.HashMap<String, Object>();

        String memberIDjmx = memberID.replaceAll("\\((\\d*)\\)<(\\w*)>:(\\d*)/(\\d*)", "($1)<$2>-$3/$4");
        JMXConnector connector = null;
        Object[] args = new Object[0];
        String[] def = new String[0];
        try {
            for (String name : metrics.keySet()) {
                String omq = "GemFire.Statistic:source=" + memberIDjmx + ",name=" + name + ",*";
                log.debug("[collect] omq = " + omq);
                Set<ObjectName> names = mServer.queryNames(new ObjectName(omq), null);
                log.debug("[collect] names = " + names);

                if (!names.iterator().hasNext()) {
                    String mq = "GemFire.*:id=" + memberIDjmx + ",*";
                    Set<ObjectName> mn = mServer.queryNames(new ObjectName(mq), null);
                    log.debug("[collect] mn = " + mn);
                    if (mn.iterator().hasNext()) {
                        ObjectName mon = mn.iterator().next();
                        log.debug("[collect] " + mon + " -> manageStats");
                        mServer.invoke(mon, "manageStats", args, def);
                        names = mServer.queryNames(new ObjectName(omq), null);
                    }
                }
                ObjectName mbean = names.iterator().next();

                mServer.invoke(mbean, "getStatistics", args, def);
                mServer.invoke(mbean, "refresh", args, def);
                Iterator attrs = mServer.getAttributes(mbean, metrics.get(name)).iterator();
                while (attrs.hasNext()) {
                    Attribute attr = (Attribute) attrs.next();
                    res.put(name + "." + attr.getName(), attr.getValue());
                }
            }

            double max = ((Long) res.get("vmHeapMemoryStats.maxMemory"));
            double used = ((Long) res.get("vmHeapMemoryStats.usedMemory"));
            res.put("used_memory", (used / max));
            res.put("used_cpu", calcCPU(res, memberID, hqu));

        } catch (Exception ex) {
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
        return res;
    }
    static Map cpu_cache = new HashMap();

    private static double calcCPU(Map metrics, String memberID, boolean hqu) {
        double cpu = 0;
        log.debug("[calcCPU] memberID=" + memberID + " metrics=" + metrics);
        String cacheKey = (hqu ? "hqu_" : "") + memberID;

        long last_cpu = ((Long) metrics.get("vmStats.processCpuTime") / 1000000);
        long last_time = System.currentTimeMillis();

        CPUCacheEntry c_entry = (CPUCacheEntry) cpu_cache.get(cacheKey);
        if (c_entry != null) {
            long prev_cpu = c_entry.cpuTime;
            long prev_time = c_entry.time;
            int cpus = (Integer) metrics.get("vmStats.cpus");
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

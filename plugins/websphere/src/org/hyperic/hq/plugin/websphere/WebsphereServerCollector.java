package org.hyperic.hq.plugin.websphere;

import java.util.Map;

import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class WebsphereServerCollector extends WebsphereCollector {

    private ObjectName jvm;

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        this.jvm =
            newObjectNamePattern("name=JVM," +
                                 "type=JVM," +
                                 "j2eeType=JVM," +
                                  getServerAttributes());

        this.jvm = resolve(mServer, this.jvm);
    }

    public void collect() {
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return;
        }

        setAvailability(true);

        Map values = getResult().getValues();

        Stats jvmStats =
            (Stats)getStats(mServer, this.jvm);

        if (jvmStats != null) {
            double total = getStatCount(jvmStats, "HeapSize");
            double used  = getStatCount(jvmStats, "UsedMemory");
            values.put("totalMemory", new Double(total));
            values.put("usedMemory", new Double(total));
            values.put("freeMemory", new Double(total-used));
        }
    }

}

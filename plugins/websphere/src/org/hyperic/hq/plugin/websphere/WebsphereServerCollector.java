package org.hyperic.hq.plugin.websphere;

import java.util.Map;

import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class WebsphereServerCollector extends WebsphereCollector {

    private ObjectName txName;

    //MBean Attribute -> legacy pmi name
    private static final String[][] TX_ATTRS = {
        { "GlobalBegunCount", "globalTransBegun" },
        { "GlobalInvolvedCount", "globalTransInvolved" },
        { "LocalBegunCount", "localTransBegun" },
        { "ActiveCount", "activeGlobalTrans" },
        { "LocalActiveCount", "activeLocalTrans" },
        { "OptimizationCount", "numOptimization" },
        { "CommittedCount", "globalTransCommitted" },
        { "LocalCommittedCount", "localTransCommitted" },
        { "RolledbackCount", "globalTransRolledBack" },
        { "LocalRolledbackCount", "localTransRolledBack" },
        { "GlobalTimeoutCount", "globalTransTimeout" },
        { "LocalTimeoutCount","localTransTimeout" }
    };

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        this.name =
            newObjectNamePattern("name=JVM," +
                                 "type=JVM," +
                                 "j2eeType=JVM," +
                                  getServerAttributes());

        this.name = resolve(mServer, this.name);

        this.txName =
            newObjectNamePattern("type=TransactionService," +
                                 "j2eeType=JTAResource");

        try {
            this.txName = resolve(mServer, this.txName);
        } catch (PluginException e) {
            this.txName = null;
        }
    }

    public void collect() {
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return;
        }

        setAvailability(true);

        Map values = getResult().getValues();

        Stats jvmStats =
            (Stats)getStats(mServer, this.name);

        if (jvmStats != null) {
            double total = getStatCount(jvmStats, "HeapSize");
            double used  = getStatCount(jvmStats, "UsedMemory");
            values.put("totalMemory", new Double(total));
            values.put("usedMemory", new Double(used));
            values.put("freeMemory", new Double(total-used));
        }

        if (this.txName != null) {
            Stats txStats =
                (Stats)getStats(mServer, this.txName);
            if (txStats != null) {
                collectStatCount(txStats, TX_ATTRS);
            }
        }
    }
}

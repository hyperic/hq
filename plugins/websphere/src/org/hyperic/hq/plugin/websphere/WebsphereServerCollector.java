package org.hyperic.hq.plugin.websphere;

import javax.management.j2ee.statistics.Stats;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class WebsphereServerCollector extends WebsphereCollector {

    private boolean isJVM;
    private String[][] attrs;

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

        String module = getProperties().getProperty("Module");

        if (module.equals("jvmRuntimeModule")) {
            isJVM = true;
            this.name =
                newObjectNamePattern("name=JVM," +
                                     "type=JVM," +
                                     "j2eeType=JVM," +
                                     getServerAttributes());

            this.name = resolve(mServer, this.name);
        }
        else if (module.equals("transactionModule")) {
            this.name =
                newObjectNamePattern("type=TransactionService," +
                                     "j2eeType=JTAResource");

            this.name = resolve(mServer, this.name);
            this.attrs = TX_ATTRS;
        }
        else if (module.equals("servletSessionsModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("webappModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("beanModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("threadPoolModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("connectionPoolModule")) {
            this.name = null; //XXX
            setSource(module);
        }
    }

    public void collect() {
        if (this.name == null) {
            return; //XXX see above
        }
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return;
        }

        setAvailability(true);

        Stats stats =
            (Stats)getStats(mServer, this.name);

        if (stats == null) {
            return;
        }

        if (isJVM) {
            double total = getStatCount(stats, "HeapSize");
            double used  = getStatCount(stats, "UsedMemory");
            setValue("totalMemory", total);
            setValue("usedMemory", used);
            setValue("freeMemory", total-used);
        }
        else {
            collectStatCount(stats, this.attrs);
        }
    }
}

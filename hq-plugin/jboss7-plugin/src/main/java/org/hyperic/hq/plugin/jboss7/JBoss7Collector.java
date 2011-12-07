package org.hyperic.hq.plugin.jboss7;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.TransactionsStats;
import org.hyperic.hq.product.PluginException;

public class JBoss7Collector extends JBossHostControllerCollector {

    private static final Log log = LogFactory.getLog(JBoss7Collector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        super.collect(admin);
        try {
            TransactionsStats ts = admin.getTransactionsStats();
            setValue("aborted-transactions", ts.getAbortedTransactions());
            setValue("application-rollbacks", ts.getApplicationRollbacks());
            setValue("committed-transactions", ts.getCommittedTransactions());
            setValue("heuristics", ts.getHeuristics());
            setValue("inflight-transactions", ts.getInflightTransactions());
            setValue("nested-transactions", ts.getNestedTransactions());
            setValue("resource-rollbacks", ts.getResourceRollbacks());
            setValue("timed-out-transactions", ts.getTimedOutTransactions());
            setValue("transactions", ts.getTransactions());
        } catch (PluginException ex) {
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}

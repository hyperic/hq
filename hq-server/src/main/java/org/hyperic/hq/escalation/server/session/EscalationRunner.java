package org.hyperic.hq.escalation.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * This class actually performs the execution of a given segment of an
 * escalation. These are queued up and run by the executor thread pool.
 */
public class EscalationRunner implements Runnable {
    
    private Integer stateId;
    private EscalationRuntime escalationRuntime = Bootstrap.getBean(EscalationRuntime.class);
    private ConcurrentStatsCollector concurrentStatsCollector = Bootstrap.getBean(ConcurrentStatsCollector.class);
    private final Log log = LogFactory.getLog(EscalationRunner.class);
    
    public EscalationRunner(Integer stateId) {
        this.stateId = stateId;
    }

    public void run() {
        int maxRetries = 3;
        for (int i=0; i<maxRetries; i++) {
            try {
                final boolean debug = log.isDebugEnabled();
                if (debug) log.debug("Running escalation state [" + stateId + "]");
                final long start = System.currentTimeMillis();
                escalationRuntime.executeState(stateId);
                final long end = System.currentTimeMillis();
                concurrentStatsCollector.addStat((end-start), ConcurrentStatsCollector.ESCALATION_EXECUTE_STATE_TIME);
                break;
            }catch(OptimisticLockingFailureException e) {
                if ((i+1) < maxRetries) {
                    String times = (maxRetries - i == 1) ? "time" : "times";
                    log.warn("Warning, exception occurred while running escalation.  will retry "
                        + (maxRetries - (i+1)) + " more " + times + ".  errorMsg: " + e);
                    continue;
                }
                log.error("Exception occurred, runEscalation() will not be retried",e);
                break;
            } catch(Throwable t) {
                log.error("Exception occurred, runEscalation() will not be retried",t);
                break;
            }
        }
    }
}
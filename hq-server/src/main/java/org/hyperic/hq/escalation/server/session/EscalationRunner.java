/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
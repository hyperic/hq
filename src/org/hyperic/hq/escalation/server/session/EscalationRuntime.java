/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.escalation.server.session;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;

import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * This class manages the runtime execution of escalation chains.  The
 * persistence part of the escalation engine lives within 
 * {@link EscalationManagerEJBImpl}
 * 
 * This class does very little within hanging onto and remembering state
 * data.  It only knows about the escalation state ID and the time that it
 * is to wake up and perform the next actions.
 * 
 * The workflow looks something like this:
 * 
 *                                        /- EscalationRunner
 *        Runtime   ->[ScheduleWatcher]  -- EscalationRunner
 *                                        \_ EscalationRunner 
 *                                            |
 *                                            ->EsclManager.executeState
 *
 *
 * The Runtime puts {@link EscalationState}s into the schedule.  When the
 * schedule determines the state's time is ready to run, the task is passed
 * off into an EscalationRunner (which comes from a thread pool) and kicked
 * off.  
 */
class EscalationRuntime {
    private static final EscalationRuntime INSTANCE = new EscalationRuntime();

    private final Log _log = LogFactory.getLog(EscalationRuntime.class);
    private final ClockDaemon             _schedule = new ClockDaemon();
    private final Map                     _stateIdsToTasks = new HashMap();
    private final PooledExecutor          _executor;
    private final EscalationManagerLocal  _esclMan;
    
    private EscalationRuntime() {
        _executor = new PooledExecutor(new LinkedQueue());
        _executor.setKeepAliveTime(-1);  // Threads never die off
        _executor.createThreads(3);  // # of threads to service requests
        
        _esclMan = EscalationManagerEJBImpl.getOne();
    }

    /**
     * This class actually performs the execution of a given segment of an
     * escalation.  These are queued up and run by the executor thread pool.
     */
    private class EscalationRunner implements Runnable {
        private Integer _stateId;
        
        private EscalationRunner(Integer stateId) {
            _stateId = stateId;
        }

        public void run() {
            runEscalation(_stateId);
        }
    }
    
    /**
     * This class is invoked when the clock daemon wakes up and decides that
     * it is time to look at an escalation.
     */
    private class ScheduleWatcher implements Runnable {
        private Integer  _stateId;
        private Executor _executor;
        
        private ScheduleWatcher(Integer stateId, Executor executor) {
            _stateId  = stateId;
            _executor = executor;
        }
        
        public void run() {
            try {
                _executor.execute(new EscalationRunner(_stateId));
            } catch(InterruptedException e) {
                _log.warn("Interrupted while trying to execute state [" + 
                          _stateId + "]");
            }
        }
    }

    /**
     * Unschedule the execution of an escalation state.  The unschedule will
     * only occur if the transaction succesfully commits.
     */
    void unscheduleEscalation(EscalationState state) {
        final Integer stateId = state.getId();
        
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (success) {
                    unscheduleEscalation_(stateId);
                }
            }
        });
    }
    
    private void unscheduleEscalation_(Integer stateId) {
        synchronized (_stateIdsToTasks) {
            Object task = _stateIdsToTasks.get(stateId);
         
            if (task != null) {
                ClockDaemon.cancel(task);
                _log.info("Canceled state[" + stateId + "]");
            } else {
                _log.info("Canceling state[" + stateId + "] but was " + 
                          "not found");
            }
        }
    }
    
    /**
     * This method introduces an escalation state to the runtime.  The
     * escalation will be invoked according to the next action time of the
     * state.
     * 
     * If the state had been previously scheduled, it will be rescheduled with
     * the new time. 
     */
    void scheduleEscalation(EscalationState state) {
        final Integer stateId   = state.getId();
        final long    schedTime = state.getNextActionTime();
        
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                _log.info("Transaction committed:  success=" + success);
                if (success) {
                    scheduleEscalation_(stateId, schedTime);
                }
            }
        });
    }
    
    private void scheduleEscalation_(Integer stateId, long schedTime) {
        synchronized (_stateIdsToTasks) {
            Object task = _stateIdsToTasks.get(stateId);
            
            if (task != null) {
                // Previously scheduled.  Unschedule
                ClockDaemon.cancel(task);
                _log.info("Rescheduling state[" + stateId + "]");
            } else {
                _log.info("Scheduleing state[" + stateId + "]");
            }

            task = _schedule.executeAt(new Date(schedTime),
                                       new ScheduleWatcher(stateId, _executor)); 
                                                           
            _stateIdsToTasks.put(stateId, task);
        }
    }
    
    private void runEscalation(Integer stateId) {
        _log.info("Running escalation state [" + stateId + "]");
        _esclMan.executeState(stateId);
    }
    
    static EscalationRuntime getInstance() {
        return INSTANCE;
    }
}

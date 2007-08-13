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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    private final ThreadLocal _batchUnscheduleTxnListeners = new ThreadLocal();
    private final Log _log = LogFactory.getLog(EscalationRuntime.class);
    private final ClockDaemon             _schedule = new ClockDaemon();
    private final Map                     _stateIdsToTasks = new HashMap();
    private final Map                     _esclEntityIdsToStateIds = new HashMap();
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
     * only occur if the transaction successfully commits.
     */
    void unscheduleEscalation(EscalationState state) {
        final Integer stateId = state.getId();
        
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (success) {
                    unscheduleEscalation_(stateId);
                }
            }

            public void beforeCommit() {
            }
        });
    }
        
    /**
     * Unschedule the execution of all escalation states associated with this 
     * entity that performs escalations. The unschedule will only occur if the 
     * transaction successfully commits.
     */
    void unscheduleAllEscalationsFor(PerformsEscalations def) {
        BatchUnscheduleEscalationsTransactionListener batchTxnListener = 
            (BatchUnscheduleEscalationsTransactionListener)
            _batchUnscheduleTxnListeners.get();
        
        if (batchTxnListener == null) {
            batchTxnListener = new BatchUnscheduleEscalationsTransactionListener();
            _batchUnscheduleTxnListeners.set(batchTxnListener);
            HQApp.getInstance().addTransactionListener(batchTxnListener);
        }
        
        batchTxnListener.unscheduleAllEscalationsFor(def);
    }
        
    /**
     * A txn listener that unschedules escalations in batch. 
     * This class is not thread safe. We assume that this txn listener 
     * is called back by the same thread originally unscheduling the 
     * escalations.
     */
    private class BatchUnscheduleEscalationsTransactionListener 
        implements TransactionListener {
        private final Set _escalationsToUnschedule;
        
        public BatchUnscheduleEscalationsTransactionListener() {
            _escalationsToUnschedule = new HashSet();
        }
        
        /**
         * Unscheduled all escalations associated with this entity.
         * 
         * @param def The entity that performs escalations.
         */
        public void unscheduleAllEscalationsFor(PerformsEscalations def) {
            _escalationsToUnschedule.add(new EscalatingEntityIdentifier(def));
        }
        
        public void afterCommit(boolean success) {
            try {
                _log.debug("Transaction committed:  success=" + success);
                if (success) {
                    unscheduleAllEscalations_((EscalatingEntityIdentifier[])
                            _escalationsToUnschedule.toArray(
                                    new EscalatingEntityIdentifier[
                                        _escalationsToUnschedule.size()]));
                }                
            } finally {
                _batchUnscheduleTxnListeners.set(null);
            }
        }

        public void beforeCommit() {
            deleteAllEscalations_((EscalatingEntityIdentifier[])
                _escalationsToUnschedule.toArray(
                        new EscalatingEntityIdentifier[
                            _escalationsToUnschedule.size()]));            
        }
                
    }
    
    private void deleteAllEscalations_(EscalatingEntityIdentifier[] escalatingEntities) {
        List stateIds = new ArrayList(escalatingEntities.length);
        
        synchronized (_stateIdsToTasks) {
            for (int i = 0; i < escalatingEntities.length; i++) {
                Integer stateId = (Integer)_esclEntityIdsToStateIds.get(
                                                    escalatingEntities[i]);
                // stateId may be null if an escalation has not been scheduled 
                // for this escalating entity.
                if (stateId != null) {
                    stateIds.add(stateId);
                }
                    
            }
        }
        
        _esclMan.deleteAllEscalationStates(
                (Integer[])stateIds.toArray(new Integer[stateIds.size()])); 
    }
    
    private void unscheduleEscalation_(Integer stateId) {
        synchronized (_stateIdsToTasks) {
            doUnscheduleEscalation_(stateId);
            _esclEntityIdsToStateIds.values().remove(stateId);
        }
    }
        
    private void unscheduleAllEscalations_(EscalatingEntityIdentifier[] esclEntityIds) {
        synchronized (_stateIdsToTasks) {
            for (int i = 0; i < esclEntityIds.length; i++) {
                Integer stateId = 
                    (Integer)_esclEntityIdsToStateIds.remove(esclEntityIds[i]);                
                doUnscheduleEscalation_(stateId);
            }            
        }
    }
    
    private void doUnscheduleEscalation_(Integer stateId) {
        if (stateId != null) {
            Object task = _stateIdsToTasks.remove(stateId);
            
            if (task != null) {
                ClockDaemon.cancel(task);
                _log.debug("Canceled state[" + stateId + "]");
            } else {
                _log.debug("Canceling state[" + stateId + "] but was " + 
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
    void scheduleEscalation(final EscalationState state) {
        final long schedTime = state.getNextActionTime();
        
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                _log.debug("Transaction committed:  success=" + success);
                if (success) {
                    scheduleEscalation_(state, schedTime);
                }
            }

            public void beforeCommit() {
            }
        });
    }
    
    private void scheduleEscalation_(EscalationState state, long schedTime) {
        Integer stateId   = state.getId();
        
        if (stateId == null) {
            throw new IllegalStateException("Cannot schedule a " +
            		"transient escalation state (stateId=null).");
        }
        
        synchronized (_stateIdsToTasks) {
            Object task = _stateIdsToTasks.get(stateId);
            
            if (task != null) {
                // Previously scheduled.  Unschedule
                ClockDaemon.cancel(task);
                _log.debug("Rescheduling state[" + stateId + "]");
            } else {
                _log.debug("Scheduling state[" + stateId + "]");
            }

            task = _schedule.executeAt(new Date(schedTime),
                                       new ScheduleWatcher(stateId, _executor)); 
                                                           
            _stateIdsToTasks.put(stateId, task);
            _esclEntityIdsToStateIds.put(new EscalatingEntityIdentifier(state), 
                                         stateId);
        }
    }
    
    private void runEscalation(Integer stateId) {
        _log.debug("Running escalation state [" + stateId + "]");
        _esclMan.executeState(stateId);
    }
    
    static EscalationRuntime getInstance() {
        return INSTANCE;
    }
}

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

package org.hyperic.util.thread;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.emory.mathcs.backport.java.util.concurrent.Future;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * This class is able to interrupt threads after a certain interval -- useful
 * when threads need to have a bounded runtime.  
 */
public class ThreadWatchdog {
    private final Log _log = LogFactory.getLog(ThreadWatchdog.class);
    private ScheduledThreadPoolExecutor _executor; 
    private ThreadFactory _tFact;
    
    public ThreadWatchdog(final String threadName) {
        _tFact = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName);
            }
        };
    }

    /**
     * Must be called prior to any other use of this object.
     */
    public void initialize() {
        _executor = new ScheduledThreadPoolExecutor(1, _tFact);
    }
    
    /**
     * Schedule an interrupt for the current thread.
     * 
     * @param delay      The # of units to delay 
     * @param units      Combines with 'delay' to give the length of time to 
     *                   delay
     * @param targetMsg  An informational message about what the current thread
     *                   is doing, used when logging an interrupt.
     */
    public InterruptToken interruptMeIn(long delay, TimeUnit units,
                                        String targetMsg) 
    {
        Interruptor i = new Interruptor(Thread.currentThread(), targetMsg,
                                        delay + " " + units);
        Future f;
        
        synchronized (_executor) {
            f = _executor.schedule(i, delay, units);
        }

        InterruptToken res = new InterruptToken(f);
        return res;
    }

    /**
     * Cancel a scheduled interrupt.
     */
    public void cancelInterrupt(InterruptToken t) {
        t._f.cancel(false);
    }
    
    public static class InterruptToken {
        private Future _f;
        
        private InterruptToken(Future f) {
            _f = f;
        }
    }
    
    private class Interruptor 
        implements Runnable
    {
        private final Thread _target;
        private final String _targetMsg;
        private final String _timeMsg;
        
        public Interruptor(Thread target, String targetMsg, String timeMsg) {
            _target    = target;
            _targetMsg = targetMsg;
            _timeMsg   = timeMsg;
        }
        
        public void run() {
            _log.warn("Interrupting thread [" + _target + "] (" + _targetMsg +
                      ") it exceeded " + _timeMsg);
            _target.interrupt();
        }
    }
}

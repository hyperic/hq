/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.application;

import org.hyperic.util.thread.LoggingThreadGroup;
import org.hyperic.util.thread.ThreadGroupFactory;

import edu.emory.mathcs.backport.java.util.concurrent.ScheduledFuture;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A scheduler that internally uses a thread pool to permit concurrent execution 
 * of scheduled tasks. This scheduler is preferable to a {@link java.util.Timer} 
 * when the execution of one task may block long enough to delay execution of 
 * other tasks.
 */
public class Scheduler implements ShutdownCallback {
    
    /**
     * Specify no initial delay when scheduling a task. 
     */
    public static final long NO_INITIAL_DELAY = 0;
    
    private final ScheduledThreadPoolExecutor _executor;
    
    /**
     * Creates a scheduler.
     *
     * @param poolSize The thread pool size.
     * @throws IllegalStateException if the pool size is less than or equal to zero.
     */
    public Scheduler(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalStateException("illegal pool size: "+poolSize);
        }
        
        LoggingThreadGroup threadGroup = new LoggingThreadGroup("SchedulerGroup");
        
        // set pool threads as daemons in case an orderly shutdown does not occur
        ThreadGroupFactory tFactory= 
            new ThreadGroupFactory(threadGroup, "Scheduler-");
        tFactory.createDaemonThreads(true);      
        
        _executor = 
           new ScheduledThreadPoolExecutor(poolSize, 
                                           tFactory, 
                                           new ThreadPoolExecutor.DiscardPolicy());
        
        // delayed tasks should not execute after shutdown
        _executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
       
       // prestart one thread to make sure the first execution is timely
       _executor.prestartCoreThread();
    }
    
    /**
     * Creates and executes a periodic action that becomes enabled first after 
     * the given initial delay, and subsequently with the given period; that 
     * is executions will commence after initialDelay then initialDelay+period, 
     * then initialDelay + 2 * period, and so on. If any execution of the task 
     * encounters an exception, subsequent executions are suppressed. Otherwise, 
     * the task will only terminate via cancellation or termination of the 
     * executor. If any execution of this task takes longer than its period, 
     * then subsequent executions may start late, but will not concurrently 
     * execute.
     * 
     * @param task The task to execute.
     * @param initialDelay The initial delay (in msec) before the first execution.
     * @param period The period (in msec) between successive executions.
     * @return A ScheduledFuture that may be used to cancel task execution.
     */
    public ScheduledFuture scheduleAtFixedRate(Runnable task, long initialDelay, long period) {
        return _executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates and executes a periodic action that becomes enabled first after 
     * the given initial delay, and subsequently with the given delay between 
     * the termination of one execution and the commencement of the next. 
     * If any execution of the task encounters an exception, subsequent 
     * executions are suppressed. Otherwise, the task will only terminate via 
     * cancellation or termination of the executor.
     * 
     * @param task The task to execute.
     * @param initialDelay The initial delay (in msec) before the first execution.
     * @param delay The delay (in msec) between termination of one execution 
     *              and commencement of the next.
     * @return A ScheduledFuture that may be used to cancel task execution.
     */
    public ScheduledFuture scheduleWithFixedDelay(Runnable task, long initialDelay, long delay) {
        return _executor.scheduleWithFixedDelay(task, initialDelay, delay, TimeUnit.MILLISECONDS);
    }
        
    /**
     * Shut down the scheduler in an orderly manner, allowing any currently 
     * executing tasks to complete.
     */
    public void shutdown() {
        _executor.shutdown();
                
        // block at most for 5 seconds for any currently executing task to terminate
        try {
            _executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

}

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

package org.hyperic.hq.transport.util;

import org.hyperic.hq.transport.util.AsynchronousInvocationHandler;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.hyperic.util.thread.ThreadGroupFactory;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A helper class for performing asynchronous invocations within the HQ transport 
 * layer. Asynchronous invocations are performed using a thread pool executor. 
 */
public class AsynchronousInvoker {

    private final ThreadPoolExecutor _executor;
    
    /**
     * Creates an instance.
     *
     * @param poolSize The thread pool size.
     * @throws IllegalArgumentException if the pool size is less than or equal to zero.
     */
    public AsynchronousInvoker(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("illegal pool size: "+poolSize);
        }
        
        LoggingThreadGroup threadGroup = new LoggingThreadGroup("Async Invoker Group");
        
        ThreadGroupFactory tFactory= 
            new ThreadGroupFactory(threadGroup, "AsyncInvoker-");
        
        tFactory.createDaemonThreads(true);
        
        _executor =  new ThreadPoolExecutor(poolSize, 
                                            poolSize, 
                                            Long.MAX_VALUE, 
                                            TimeUnit.NANOSECONDS, 
                                            new LinkedBlockingQueue(), 
                                            tFactory,
                                            new ThreadPoolExecutor.AbortPolicy());
    }
    
    /**
     * Start the asynchronous invoker. This amounts to warming up all the 
     * threads in the thread pool.
     */
    public void start() {
        _executor.prestartAllCoreThreads();
    }
    
    /**
     * Stop the asynchronous invoker. After stopped, any attempt to make a 
     * non-guaranteed invocation will result in a {@link RejectedExecutionException}. 
     * Guaranteed invocations will still be allowed but will only consist of 
     * persisting the invocation for later execution, not making the invocation 
     * itself.
     */
    public void stop() {
        _executor.shutdown();
    }
    
    /**
     * Make an invocation.
     * 
     * @param handler The invocation handler.
     * @throws RejectedExecutionException if the invoker is {@link #stop() stopped} 
     * and delivery is not guaranteed.
     */
    public void invoke(AsynchronousInvocationHandler handler) {
        if (handler.isInvocationGuaranteed()) {
            // FIXME - need to implement guaranteed delivery correctly
            // 1) Store the externalized handler to a persistent queue
            // 2) A background thread will peek() and execute the handler immediately, 
            // calling handleInvocation() since it throws an Exception.
            // 3) After the handler is executed, the handler will be removed
            // If we store the handlers in the database, this could be made transactional
        } else {
            _executor.execute(handler);                        
        }
    }
    
}

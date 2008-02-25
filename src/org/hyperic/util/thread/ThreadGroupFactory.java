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

package org.hyperic.util.thread;

import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;

public class ThreadGroupFactory 
    implements ThreadFactory
{
    private ThreadGroup _group;
    private String      _namePrefix;
    private int         _numThreads;
    private boolean     _createDaemonThreads;
    private final Object      _syncLock = new Object();
    
    /**
     * Creates an instance where the threads created by this factory are 
     * assigned to the specified ThreadGroup.
     *
     * @param group The ThreadGroup.
     * @param namePrefix The name prefix for each thread created by this factory.
     */
    public ThreadGroupFactory(ThreadGroup group, String namePrefix) {
        _group      = group;
        _namePrefix = namePrefix;
        _numThreads = 0;
    }
    
    /**
     * Creates an instance where the threads created by this factory are 
     * assigned to the current thread's ThreadGroup.
     *
     * @param namePrefix The name prefix for each thread created by this factory.
     */
    public ThreadGroupFactory(String namePrefix) {
        this(Thread.currentThread().getThreadGroup(), namePrefix);
    }
    
    /**
     * Set the threads created by this factory to be daemon threads.
     * 
     * @param daemonThreads <code>true</code> to set threads created by this 
     *                      factory to be daemon threads.
     */
    public void createDaemonThreads(boolean daemonThreads) {
        synchronized (_syncLock) {
            _createDaemonThreads = daemonThreads;            
        }
    }
    
    public Thread newThread(Runnable r) {
        String name;
        boolean daemon;
        
        synchronized (_syncLock) {
            name = _namePrefix + ++_numThreads; 
            daemon = _createDaemonThreads;
        }
        
        Thread thread = new Thread(_group, r, name);
        thread.setDaemon(daemon);
        
        return thread;
    }
}

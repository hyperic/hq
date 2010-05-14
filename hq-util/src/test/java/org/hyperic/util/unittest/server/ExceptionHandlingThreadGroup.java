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

package org.hyperic.util.unittest.server;

/**
 * A thread group that a client may use for registering an action to perform 
 * if an uncaught exception is handled. The uncaught exception may also be 
 * retrieved.
 */
public class ExceptionHandlingThreadGroup extends ThreadGroup {
    
    private final Object _lock = new Object();
    private Runnable _runnable;
    private Throwable _uncaughtException;
    
    
    public ExceptionHandlingThreadGroup(String name) {
        super(name);
    }
    
    /**
     * @return The uncaught exception or <code>null</code>.
     */
    public Throwable getUncaughtException() {
        synchronized (_lock) {
            return _uncaughtException;
        }
    }
    
    /**
     * Set an action to execute in the uncaught exception handler.
     * 
     * @param runnable The runnable representing the action to execute.
     */
    public void setUncaughtExceptionAction(Runnable runnable) {
        synchronized (_lock) {
            _runnable = runnable;
        }
    }
    
    public void uncaughtException(Thread t, Throwable e) {
        synchronized (_lock) {
            _uncaughtException = e;
            
            if (_runnable != null) {
                _runnable.run();
            }
        }
    }        
}
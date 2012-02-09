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

package org.hyperic.lather.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.hyperic.hq.common.SystemException;

class ConnManager {
    private Object LOCK = new Object();
    private Map<String, AtomicInteger> connState = new HashMap<String, AtomicInteger>();
    public static final String PROP_PREFIX = "org.hyperic.lather.";
    public static final String PROP_MAXCONNS = "maxConns";
    private final Map<String, Semaphore> maxConns;

    public ConnManager(Map<String, Semaphore> maxConnMap) {
        if (!maxConnMap.containsKey(PROP_MAXCONNS)) {
            throw new SystemException(PROP_MAXCONNS + " property does not exist");
        }
        maxConns = Collections.synchronizedMap(new HashMap<String,Semaphore>(maxConnMap));
    }

    int getAvailablePermits(String method) {
        final Semaphore semaphore = getSemaphore(method);
        return semaphore.availablePermits();
    }

    boolean grabConn(String method) {
        final Semaphore semaphore = getSemaphore(method);
        return semaphore.tryAcquire();
    }

    private Semaphore getSemaphore(String method) {
        Semaphore semaphore = maxConns.get(method);
        return (semaphore == null) ? maxConns.get(PROP_MAXCONNS) : semaphore;
    }

    void releaseConn(String method) {
        final Semaphore semaphore = getSemaphore(method);
        semaphore.release();
    }

    int getNumConns(String method) {
        synchronized (LOCK) {
            AtomicInteger num = connState.get(method);
            if (num != null) {
                return num.get();
            }
            num = connState.get(PROP_MAXCONNS);
            return num.get();
        }
    }

}
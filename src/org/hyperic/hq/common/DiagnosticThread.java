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

package org.hyperic.hq.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The DiagnosticThread is a simple object running within the server that
 * prints diagnostic information to the server log for objects that have been
 * registered via addDiagnosticObject().
 *
 * The DiagnosticThread does not start until the first DiagnosticObject has
 * been added to the list.
 * 
 */
public class DiagnosticThread implements Runnable {

    private Log _log = LogFactory.getLog(DiagnosticThread.class);

    private static final Object INIT_LOCK = new Object();
    private static DiagnosticThread INSTANCE;

    // The actual diagnostic thread
    private static Thread _diagnosticThread;

    // List of DiagnosticObjects, may want to convert to a Map if we ever
    // want to allow objects to be removed from the DiagnosticThread at
    // runtime.
    private static List _diagnosticObjects = new ArrayList();

    // How often the thread prints info
    private static long _interval = 
        Long.getLong("org.hq.diagnostic.interval", 
                      1000 * 60 * 10).longValue(); // 10 minutes

    private DiagnosticThread() {}

    public long getInterval() {
        return _interval;
    }

    /**
     * Set the interval at which the DiagnosticThread will print status info
     * @param interval The interval in milliseconds.
     * XXX -- Technically, access to interval should be synchronized
     */
    public static void setInterval(long interval) {
        _interval = interval;
    }

    public static void addDiagnosticObject(DiagnosticObject o) {
        synchronized(INIT_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new DiagnosticThread();

                _diagnosticThread = new Thread(INSTANCE);
                _diagnosticThread.setDaemon(true);
                _diagnosticThread.start();
            }
        }

        synchronized(_diagnosticObjects) {
            _diagnosticObjects.add(o);
        }
    }

    public static Collection getDiagnosticObjects() {
        synchronized(INIT_LOCK) {
            return new ArrayList(_diagnosticObjects);
        }
    }
    
    public void run() {
        _log.info("Starting Diagnostic Thread (interval=" + _interval + " ms)");

        while (true) {
            try {
                Thread.sleep(_interval);
                _log.info("--- DIAGNOSTICS ---");
                synchronized(_diagnosticObjects) {
                    Iterator i = _diagnosticObjects.iterator();

                    while (i.hasNext()) {
                        DiagnosticObject o = (DiagnosticObject)i.next();
                        try {
                            _log.info("[" + o + "] " + o.getStatus());
                        } catch (Throwable e) {
                            _log.error("Error in diagnostics: " + e, e);
                        }
                    }
                }

            } catch (InterruptedException e) {
                _log.warn("Diagnostic thread interrupted, shutting down.");
                break;
            } catch (Exception e) {
                _log.warn("Error encountered while collecting diagnostics", e);
            }
        }
    }
}


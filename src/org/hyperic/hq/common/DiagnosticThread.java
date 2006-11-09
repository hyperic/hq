package org.hyperic.hq.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

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
    private static List _diagnosticObjects =
        Collections.synchronizedList(new ArrayList());

    // How often the thread prints info
    private static long _interval = 1000 * 60; // 60 seconds for now

    private DiagnosticThread() {}

    public long getInterval() {
        return _interval;
    }

    /**
     * Set the interval at which the DiagnosticThread will print status info
     * @param interval The interval in milliseconds.
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

        _diagnosticObjects.add(o);
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
                            _log.info("[" + o.getClass().getName() + "] " +
                                      o.getStatus());
                        } catch (Throwable e) {
                            // Don't let exceptions in DiagnosticObject's
                            // bring us down...
                        }
                    }
                }

            } catch (InterruptedException e) {
                _log.warn("Diagnostic thread interrupted, shutting down.");
                break;
            }
        }
    }
}


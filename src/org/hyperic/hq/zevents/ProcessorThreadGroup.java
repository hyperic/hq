package org.hyperic.hq.zevents;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * We use this threadgroup to start off the QueueProcessor, since it gives
 * us a facility for logging critical errors when they occur.
 */
class ProcessorThreadGroup 
    extends ThreadGroup
{
    private final Log _log = LogFactory.getLog(ProcessorThreadGroup.class);

    ProcessorThreadGroup() {
        super("ProcessorThreadGroup");
    }

    public void uncaughtException(Thread t, Throwable exc) {
        _log.warn("Unhandled exception", exc);
        super.uncaughtException(t, exc);
    }
}

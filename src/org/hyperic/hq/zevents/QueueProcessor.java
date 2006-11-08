package org.hyperic.hq.zevents;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;

/**
 * This class acts as the thread pulling data off the event queue and 
 * dispatching it to listeners.
 */
class QueueProcessor 
    implements Runnable
{
    private final Log _log = LogFactory.getLog(ZeventManager.class);

    private final BlockingQueue _eventQueue;
    private final ZeventManager _manager;  

    QueueProcessor(ZeventManager manager, BlockingQueue eventQueue){
        _manager    = manager;
        _eventQueue = eventQueue;
    }

    public void run() {
        while (true) {
            try {
                Zevent e = (Zevent)_eventQueue.take();

                e.leaveQueue();
                _manager.dispatchEvent(e);
            } catch(InterruptedException exc) {
                _log.warn("Thread interrupted.  I'm dying");
                return;
            } catch(Exception exc) {
                _log.warn("Unable to dequeue and dispatch", exc);
            }
        }
    }
}

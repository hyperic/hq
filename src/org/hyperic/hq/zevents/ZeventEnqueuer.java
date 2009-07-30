package org.hyperic.hq.zevents;

/**
 * Reponsible for enqueing Zevents for processing
 * @author jhickey
 *
 */
public interface ZeventEnqueuer {

    /**
     * Enqueues an {@link Zevent} for processing
     * @param event The {@link Zevent} to enqueue
     * @throws InterruptedException If interrupted while enqueueing event
     */
    void enqueueEvent(Zevent event) throws InterruptedException;
}

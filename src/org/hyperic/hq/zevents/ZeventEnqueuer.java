package org.hyperic.hq.zevents;

import java.util.List;

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

    /**
     * Enqueues a list of {@link Zevent}s for processing
     * @param events The list of {@link Zevent}s to enqueue
     * @throws InterruptedException If interrupted while enqueueing event
     */
    void enqueueEvents(List events) throws InterruptedException;
}

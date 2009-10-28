package org.hyperic.hq.zevents;

import java.util.List;
import java.util.Queue;
import java.util.Set;

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

    ZeventListener addBufferedListener(Class eventClass, ZeventListener listener);

    void enqueueEventAfterCommit(Zevent event);

    /**
     * Register an event class. These classes must be registered prior to
     * attempting to listen to individual event types.
     * 
     * @param eventClass a subclass of {@link Zevent}
     * @return false if the eventClass was already registered
     */
    boolean registerEventClass(Class eventClass);

    /**
     * Enqueue events if the current running transaction successfully commits.
     * @see #enqueueEvents(List)
     */
    void enqueueEventsAfterCommit(List inEvents);

    ZeventListener addBufferedListener(Set eventClasses, ZeventListener listener);

    void enqueueEvents(List events) throws InterruptedException;

    long getMaxTimeInQueue();

    long getZeventsProcessed();

    long getQueueSize();

    /**
     * Registers a buffer with the internal list, so data about its contents can
     * be printed by the diagnostic thread.
     */
    void registerBuffer(Queue q, ZeventListener e);

    boolean addBufferedGlobalListener(ZeventListener listener);
}

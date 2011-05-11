/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
     * Register a list of event classes. These classes must be registered prior to
     * attempting to listen to individual event types.
     * 
     * @param eventClasses a list of event classes to register
     * @return false if an event class is already registered
     */
    boolean registerEventClass(List<String> eventClasses);

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

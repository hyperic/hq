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

package org.hyperic.hq.zevents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticThread;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.hyperic.util.thread.ThreadGroupFactory;
import org.hyperic.util.thread.ThreadWatchdog;
import org.hyperic.util.thread.ThreadWatchdog.InterruptToken;

import edu.emory.mathcs.backport.java.util.Queue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * The Zevent subsystem is an event system for fast, non-reliable transmission
 * of events.  Important data should not be transmitted on this bus, since
 * it is not persisted, and there is never any guarantee of receipt.  

 * This manager provides no transactional guarantees, so the caller must 
 * rollback additions of listeners if the transaction fails. 
 */
public class ZeventManager { 
    private static final Log _log = LogFactory.getLog(ZeventManager.class);
    
    private static final Object INIT_LOCK = new Object();
    private static ZeventManager INSTANCE;

    // The thread group that the {@link EventQueueProcessor} comes from 
    private final LoggingThreadGroup _threadGroup;

    // The actual queue processor thread
    private Thread _processorThread;
    
    private final Object _listenerLock = new Object();
    
    /* Set of {@link ZeventListener}s listening to events of all types */
    private final Set _globalListeners = new HashSet(); 

    /* Map of {@link Class}es subclassing {@link ZEvent} onto lists of 
     * {@link ZeventListener}s */
    private final Map _listeners = new HashMap();

    /* Map of {@link Queue} onto the target listeners using them. */
    private final WeakHashMap _registeredBuffers = new WeakHashMap();
    
    // For diagnostics and warnings
    private long _lastWarnTime;
    private long _listenerTimeout;
    private long _warnSize;
    private long _warnInterval;
    private long _maxTimeInQueue;
    private long _numEvents;
    
    private BlockingQueue  _eventQueue; 

    
    private ZeventManager() {
        _threadGroup = new LoggingThreadGroup("ZEventProcessor");
        _threadGroup.setDaemon(true);
    }

    private long getProp(Properties p, String propName, long defaultVal) {
        String val = p.getProperty(propName, "" + defaultVal);
        long res;
        
        if (val == null) {
            _log.debug("tweak.properties:[" + propName + "] not set.  Using " +
                       defaultVal);
            res = defaultVal;
        } else {
            val = val.trim();
            try {
                res = Long.parseLong(val);
            } catch(NumberFormatException e) {
                _log.warn("tweak.properties:[" + propName + "] not a number.  "+
                          "( was " + val + ")  Using " + defaultVal);
                res = defaultVal;
            }
        }
        
        _log.info("[" + propName + "] = " + res);
        return res;
    }
    
    private void initialize() {
        Properties props = new Properties();
        try {
            props = HQApp.getInstance().getTweakProperties();
        } catch(Exception e) {
            _log.warn("Unable to get tweak properties", e);
        }
        
        // Maximum number of entries the queue can support
        long maxQueue  = getProp(props, "hq.zevent.maxQueueEnts", 100 * 1000);
        
        // Amount of time to warn between full zevent queue 
        // (shouldn't need to change this)
        _warnInterval = getProp(props, "hq.zevent.warnInterval", 5 * 60 * 1000); 
        
        // How many zevents are processed and dispatched at once?
        // (also dictates the maximum # of events a listener may receive in a 
        // single batch)
        long batchSize = getProp(props, "hq.zevent.batchSize", 100);
        
        // The # of entries needed in the queue in order to warn that it's
        // getting full.
        _warnSize = getProp(props, "hq.zevent.warnSize", maxQueue * 90 / 100); 
        
        _listenerTimeout = getProp(props, "hq.zevent.listenerTimeout", 60); 
        
        _eventQueue = new LinkedBlockingQueue((int)maxQueue);
        

        QueueProcessor p = new QueueProcessor(this, _eventQueue, 
                                              (int)batchSize);
                                              
        _processorThread = new Thread(_threadGroup, p, "ZeventProcessor"); 
        _processorThread.setDaemon(true);
        _processorThread.start();

        DiagnosticObject myDiag = new DiagnosticObject() {
            public String getStatus() {
                return ZeventManager.getInstance().getDiagnostics();
            }
            
            public String toString() {
                return "ZEvent Subsystem";
            }

            public String getName() {
                return "ZEvents";
            }

            public String getShortName() {
                return "zevents";
            }
        };
        
        DiagnosticThread.addDiagnosticObject(myDiag);
    }
    
    public long getQueueSize() {
        return _eventQueue.size();
    }
    
    public void shutdown() throws InterruptedException {
        while (!_eventQueue.isEmpty()) {
            System.out.println("Waiting for empty queue: " + _eventQueue.size());
            Thread.sleep(1000);
        }
        _processorThread.interrupt();
        _processorThread.join(5000);
    }

    public long getMaxTimeInQueue() {
        synchronized (INIT_LOCK) {
            return _maxTimeInQueue;
        }
    }
    
    public long getZeventsProcessed() {
        synchronized (INIT_LOCK) {
            return _numEvents;
        }
    }
    
    private long getWarnSize() {
        synchronized (INIT_LOCK) {
            return _warnSize;
        }
    }
    
    private long getListenerTimeout() {
        synchronized (INIT_LOCK) {
            return _listenerTimeout;
        }
    }

    private long getWarnInterval() {
        synchronized (INIT_LOCK) {
            return _warnInterval;
        }
    }
    
    private void assertClassIsZevent(Class c) {
        if (!Zevent.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("[" + c.getName() + 
                                               "] does not subclass [" +
                                               Zevent.class.getName() + "]");
        }
    }
    
    /**
     * Registers a buffer with the internal list, so data about its contents
     * can be printed by the diagnostic thread.
     */
    void registerBuffer(Queue q, ZeventListener e) {
        synchronized (_registeredBuffers) {
            _registeredBuffers.put(q, e);
        }
    }
    
    /**
     * Register an event class.  These classes must be registered prior to
     * attempting to listen to individual event types.
     * 
     * @param eventClass a subclass of {@link Zevent}
     * @return false if the eventClass was already registered
     */
    public boolean registerEventClass(Class eventClass) {
        assertClassIsZevent(eventClass);
     
        synchronized (_listenerLock) {
            if (_listeners.containsKey(eventClass))
                return false;
            
            _listeners.put(eventClass, new ArrayList());
            
            if (_log.isDebugEnabled()) {
                _log.debug("Register ZEvent " + eventClass);
            }
            
            return true;
        }
    }
    
    /**
     * Unregister an event class
     * 
     * @param eventClass subclass of {@link Zevent}
     * @return false if the eventClass was not registered
     */
    public boolean unregisterEventClass(Class eventClass) {
        assertClassIsZevent(eventClass);
        
        synchronized (_listenerLock) {
            return _listeners.remove(eventClass) != null;
        }
    }
    
    /**
     * Add an event listener which is called for every event type which
     * comes through the queue.
     *
     * @return false if the listener was already listening
     */
    public boolean addGlobalListener(ZeventListener listener) {
        synchronized (_listenerLock) {
            return _globalListeners.add(new TimingListenerWrapper(listener));
        }
    }

    public boolean addBufferedGlobalListener(ZeventListener listener) {
        ThreadGroupFactory threadFact = 
            new ThreadGroupFactory(_threadGroup, listener.toString());
        
        listener = new TimingListenerWrapper(listener);
        BufferedListener bListen = new BufferedListener(listener, threadFact);
        synchronized (_listenerLock) {
            return _globalListeners.add(new TimingListenerWrapper(bListen));
        }
    }
    
    /**
     * Remove a global event listener
     * 
     * @return false if the listener was not listening
     */
    public boolean removeGlobalListener(ZeventListener listener) {
        synchronized (_listenerLock) {
            return _globalListeners.remove(listener);
        }
    }
    
    private List getEventTypeListeners(Class eventClass) {
        synchronized (_listenerLock) {
            List res = (List)_listeners.get(eventClass);
            
            if (res == null) {
                // Register it
                registerEventClass(eventClass);
                return (List) _listeners.get(eventClass);
            }
            
            return res;
        }
    }
    
    /**
     * Add a buffered listener for event types.  A buffered listener is one
     * which implements its own private queue and thread for processing
     * entries.  If the actions performed by your listener take a while to
     * complete, then a buffered listener is a good candidate for use, since it
     * means that it will not be holding up the regular Zevent queue processor.
     * 
     * @param eventClasses  {@link Class}es which subclass {@link Zevent} to
     *                      listen for
     * @param listener      Listener to invoke with events
     * 
     * @return the buffered listener.  This return value must be used when
     *         trying to remove the listener later on.
     */
    public ZeventListener addBufferedListener(Set eventClasses, 
                                              ZeventListener listener) 
    { 
        ThreadGroupFactory threadFact = 
            new ThreadGroupFactory(_threadGroup, listener.toString());
        
        listener = new TimingListenerWrapper(listener);
        BufferedListener bListen = new BufferedListener(listener, threadFact); 
        
        for (Iterator i=eventClasses.iterator(); i.hasNext(); ) {
            addListener((Class)i.next(), bListen);
        }
        return bListen;
    }
    
    public ZeventListener addBufferedListener(Class eventClass,
                                              ZeventListener listener)
    {
        return addBufferedListener(Collections.singleton(eventClass),
                                   listener);
    }
    
    /**
     * Add a listener for a specific type of event.
     * 
     * @param eventClass A subclass of {@link Zevent}
     * @return false if the listener was already registered
     */
    public boolean addListener(Class eventClass, ZeventListener listener) {
        assertClassIsZevent(eventClass);

        synchronized (_listenerLock) {
            List listeners = getEventTypeListeners(eventClass);

            if (listeners.contains(listener))
                return false;
            listeners.add(new TimingListenerWrapper(listener));
            return true;
        }
    }

    /**
     * Remove a specific event type listener.
     * @see #addListener(Class, ZeventListener)
     */
    public boolean removeListener(Class eventClass, ZeventListener listener) {
        assertClassIsZevent(eventClass);
        
        synchronized (_listenerLock) {
            List listeners = getEventTypeListeners(eventClass);
                    
            return listeners.remove(listener);
        }
    }
    
    /**
     * Enqueue events onto the event queue.  This method will block if the
     * thread is full.
     *  
     * @param events List of {@link Zevent}s 
     * @throws InterruptedException if the queue was full and the thread was
     *                              interrupted
     */
    public void enqueueEvents(List events) throws InterruptedException {
        if (_eventQueue.size() > getWarnSize() && 
            (System.currentTimeMillis() - _lastWarnTime) > getWarnInterval())
        {
            _lastWarnTime = System.currentTimeMillis();
            _log.warn("Your event queue is having a hard time keeping up.  " +
                      "Get a faster CPU, or reduce the amount of events!");
        }
                        
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            Zevent e = (Zevent)i.next();  

            e.enterQueue();
            _eventQueue.offer(e, 1, TimeUnit.SECONDS);
        }
    }
    
    public void enqueueEventAfterCommit(Zevent event) {
        enqueueEventsAfterCommit(Collections.singletonList(event));
    }
    
    /**
     * Enqueue events if the current running transaction successfully commits.
     * @see #enqueueEvents(List)
     */
    public void enqueueEventsAfterCommit(List inEvents) {
        final List events = new ArrayList(inEvents);
        TransactionListener txListener = new TransactionListener() {
            public void afterCommit(boolean success) {
                try {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Listener[" + this + "] after tx " + 
                                   "enqueueing=" + success);
                    }
                    
                    if (success) {
                        enqueueEvents(events);
                    }
                } catch(InterruptedException e) {
                    _log.warn("Interrupted while enqueueing events");
                }
            }

            public void beforeCommit() {
            }
        };

        if (_log.isDebugEnabled()) {
            _log.debug("Listener[" + txListener + "] Enqueueing events: " + 
                       inEvents);
        }
        HQApp.getInstance().addTransactionListener(txListener);
    }
    
    public void enqueueEvent(Zevent event) throws InterruptedException {
        enqueueEvents(Collections.singletonList(event));
    }

    /**
     * Wait until the queue is empty.  This is a non-performant function, so
     * please only use it in test suites. 
     */
    public void waitUntilNoEvents() throws InterruptedException {
        while (_eventQueue.size() != 0)
            Thread.sleep(100);
    }
    
    /**
     * Returns the listeners which have specifically registered for the
     * specified event type.  This method does not return global event listeners.
     * 
     * If the event type was never registered, null will be returned, otherwise 
     * a list of {@link ZeventListener}s (of potentially size=0)
     */
    private List getTypeListeners(Zevent z) {
        synchronized (_listenerLock) {
            return (List)_listeners.get(z.getClass());
        }
    }
    
    /**
     * Internal method to dispatch events.  Called by the 
     * {@link QueueProcessor}. 
     * 
     * The strategy used in this method creates mini-batches of events
     * to send to each listener.  There is no defined order for listener
     * execution.  
     */
    void dispatchEvents(List events) {
        synchronized (INIT_LOCK) {
            for (Iterator i=events.iterator(); i.hasNext(); ) {
                Zevent z = (Zevent)i.next();
                
                long timeInQueue = z.getQueueExitTime() - z.getQueueEntryTime();
                if (timeInQueue > _maxTimeInQueue)
                    _maxTimeInQueue = timeInQueue;
                _numEvents++;
            }
        }

        List validEvents = new ArrayList(events.size());
        Map listenerBatches;
        synchronized (_listenerLock) {
            listenerBatches = new HashMap(_globalListeners.size());
            for (Iterator i=events.iterator(); i.hasNext(); ) {
                Zevent z = (Zevent)i.next();
                List typeListeners = getTypeListeners(z);
                
                if (typeListeners == null) {
                    _log.warn("Unable to dispatch event of type [" + 
                              z.getClass().getName() + "]:  Not registered");
                    continue;
                }
                validEvents.add(z);
                
                for (Iterator j=typeListeners.iterator(); j.hasNext(); ) {
                    ZeventListener listener = (ZeventListener)j.next();
                    List batch = (List)listenerBatches.get(listener);
                    
                    if (batch == null) {
                        batch = new ArrayList();
                        listenerBatches.put(listener, batch);
                    }
                    batch.add(z);
                }
            }
            
            for (Iterator i=_globalListeners.iterator(); i.hasNext(); ) {
                ZeventListener listener = (ZeventListener)i.next();
                listenerBatches.put(listener, validEvents);
            }
        }

        ThreadWatchdog dog = HQApp.getInstance().getWatchdog();
        long timeout = getListenerTimeout();
        for (Iterator i=listenerBatches.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            ZeventListener listener = (ZeventListener)ent.getKey();
            List batch = (List)ent.getValue();
            
            synchronized (_listenerLock) {
                InterruptToken t = null;
                try {
                    t = dog.interruptMeIn(timeout, TimeUnit.SECONDS,
                                          "Processing listener events");
                    listener.processEvents(Collections.unmodifiableList(batch));
                } catch(RuntimeException e) {
                    _log.warn("Exception while invoking listener [" + 
                              listener + "]", e);
                } finally {
                    if (t != null)
                        dog.cancelInterrupt(t);
                }
            }
        }
    }

    private String getDiagnostics() {
        synchronized (INIT_LOCK) {
            StringBuffer res = new StringBuffer();
            
            res.append("ZEvent Manager Diagnostics:\n")  
               .append("    Queue Size:        " + _eventQueue.size() + "\n")
               .append("    Events Handled:    " + _numEvents + "\n")
               .append("    Max Time In Queue: " + _maxTimeInQueue + "ms\n\n")
               .append("ZEvent Listener Diagnostics:\n");
            PrintfFormat timingFmt = 
                new PrintfFormat("        %-30s max=%-7.2f avg=%-5.2f " + 
                                 "num=%-5d\n");
            synchronized (_listenerLock) {
                
                for (Iterator i=_listeners.entrySet().iterator(); i.hasNext();){ 
                    Map.Entry ent = (Map.Entry)i.next();
                    Collection listeners = (Collection)ent.getValue();
                    
                    res.append("    EventClass: " +
                               ent.getKey() + "\n");
                    for (Iterator j=listeners.iterator(); j.hasNext(); ) {
                        TimingListenerWrapper l = 
                            (TimingListenerWrapper)j.next();
                        Object[] args = new Object[] { 
                            l.toString(),
                            new Double(l.getMaxTime()),
                            new Double(l.getAverageTime()),
                            new Long(l.getNumEvents())
                        };
                            
                        res.append(timingFmt.sprintf(args));
                    }
                    res.append("\n");
                }
                
                res.append("    Global Listeners:\n");
                for (Iterator i=_globalListeners.iterator(); i.hasNext(); ) {
                    TimingListenerWrapper l = (TimingListenerWrapper)i.next();
                    Object[] args = new Object[] { 
                        l.toString(),
                        new Double(l.getMaxTime()),
                        new Double(l.getAverageTime()),
                        new Long(l.getNumEvents()),
                    };
                    
                    res.append(timingFmt.sprintf(args));
                }
            }
            
            synchronized (_registeredBuffers) {
                PrintfFormat fmt = new PrintfFormat("    %-30s size=%d\n"); 
                
                res.append("\nZevent Registered Buffers:\n");
                for (Iterator i=_registeredBuffers.entrySet().iterator(); 
                     i.hasNext(); )
                {
                    Map.Entry ent = (Map.Entry)i.next();
                    Queue q = (Queue)ent.getKey();
                    TimingListenerWrapper targ = 
                        (TimingListenerWrapper)ent.getValue();

                    res.append(fmt.sprintf(new Object[] {
                        targ.toString(),
                        new Integer(q.size()),
                    }));
                    
                    res.append(timingFmt.sprintf(new Object[] {
                        "", // Target already printed above
                        new Double(targ.getMaxTime()),
                        new Double(targ.getAverageTime()),
                        new Long(targ.getNumEvents()),
                    }));
                }
            }
            
            return res.toString();
        }
    }
    
    public static ZeventManager getInstance() {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new ZeventManager();
                INSTANCE.initialize();
            }
        }
        return INSTANCE;
    }
}

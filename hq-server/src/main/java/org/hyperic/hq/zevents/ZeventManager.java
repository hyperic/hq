/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.hyperic.util.thread.ThreadGroupFactory;
import org.hyperic.util.thread.ThreadWatchdog;
import org.hyperic.util.thread.ThreadWatchdog.InterruptToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The Zevent subsystem is an event system for fast, non-reliable transmission
 * of events. Important data should not be transmitted on this bus, since it is
 * not persisted, and there is never any guarantee of receipt.
 * 
 * This manager provides no transactional guarantees, so the caller must
 * rollback additions of listeners if the transaction fails.
 */
@Component
public class ZeventManager implements ZeventEnqueuer {
    private static final Log _log = LogFactory.getLog(ZeventManager.class);
    private static final long DEFAULT_TIMEOUT = 1;  
    private static final Object INIT_LOCK = new Object();

    // The thread group that the {@link EventQueueProcessor} comes from
    private final LoggingThreadGroup _threadGroup;

    // The actual queue processor thread
    private Thread _processorThread;

    private final Object _listenerLock = new Object();

    /* Set of {@link ZeventListener}s listening to events of all types */
    private Set<TimingListenerWrapper<Zevent>> _globalListeners =
        new HashSet<TimingListenerWrapper<Zevent>>();

    /*
     * Map of {@link Class}es subclassing {@link ZEvent} onto lists of {@link
     * ZeventListener}s
     */
    private Map<Class<? extends Zevent>, List<TimingListenerWrapper<Zevent>>> _listeners =
        new HashMap<Class<? extends Zevent>, List<TimingListenerWrapper<Zevent>>>();

    /* Map of {@link Queue} onto the target listeners using them. */
    private WeakHashMap<Queue<?>, TimingListenerWrapper<Zevent>> _registeredBuffers =
        new WeakHashMap<Queue<?>, TimingListenerWrapper<Zevent>>();

    // For diagnostics and warnings
    private long _lastWarnTime;
    private final long _listenerTimeout;
    private final long _warnSize;
    private final long _warnInterval;
    private long _maxTimeInQueue;
    private long _numEvents;

    private BlockingQueue<Zevent> _eventQueue;
    private DiagnosticsLogger diagnosticsLogger;
    private final ThreadWatchdog threadWatchdog;
    private final long maxQueue;
    private final long batchSize;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    
    @Autowired
    public ZeventManager(DiagnosticsLogger diagnosticsLogger, ThreadWatchdog threadWatchdog,
    					 ConcurrentStatsCollector concurrentStatsCollector,
                         @Value("#{tweakProperties['hq.zevent.maxQueueEnts'] }") Long maxQueue,
                         @Value("#{tweakProperties['hq.zevent.batchSize'] }") Long batchSize,
                         @Value("#{tweakProperties['hq.zevent.warnInterval'] }") Long warnInterval,  
                         @Value("#{tweakProperties['hq.zevent.warnSize'] }") Long warnSize,
                         @Value("#{tweakProperties['hq.zevent.listenerTimeout'] }") Long listenerTimeout) {
        this._threadGroup = new LoggingThreadGroup("ZEventProcessor");
        this._threadGroup.setDaemon(true);
        this.diagnosticsLogger = diagnosticsLogger;
        this.threadWatchdog = threadWatchdog;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.maxQueue = maxQueue;
        this.batchSize = batchSize;
        this._warnInterval = warnInterval;
        this._warnSize = warnSize;
        this._listenerTimeout = listenerTimeout;
        
    }

    @PostConstruct
    @SuppressWarnings("unused")
    private void initialize() {
        _eventQueue = new LinkedBlockingQueue<Zevent>((int) maxQueue);

        QueueProcessor p = new QueueProcessor(this, _eventQueue, (int) batchSize);

        _processorThread = new Thread(_threadGroup, p, "ZeventProcessor");
        _processorThread.setDaemon(true);
        _processorThread.start();

        DiagnosticObject myDiag = new DiagnosticObject() {
            public String getStatus() {
                return getDiagnostics();
            }
            
            public String getShortStatus() {
                return getStatus();
            }

            @Override
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

        diagnosticsLogger.addDiagnosticObject(myDiag);
        concurrentStatsCollector.register(ConcurrentStatsCollector.ZEVENT_QUEUE_SIZE);
        concurrentStatsCollector.register(new StatCollector() {
            public long getVal() throws StatUnreachableException {
                return getTotalRegisteredBufferSize();
            }
            public String getId() {
                return ConcurrentStatsCollector.ZEVENT_REGISTERED_BUFFER_SIZE;
            }
        });
    }

    public long getQueueSize() {
        return _eventQueue.size();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        while (!_eventQueue.isEmpty()) {
            System.out.println("Waiting for empty queue: " + _eventQueue.size());
            Thread.sleep(1000);
        }
        _processorThread.interrupt();
        _processorThread.join(5000);
        
        _threadGroup.interrupt() ;
        diagnosticsLogger = null ;
        this._globalListeners = null ; 
        this._listeners = null ; 
        this._eventQueue = null ;
        synchronized(this._registeredBuffers) { 
            
            for (Entry<Queue<?>, TimingListenerWrapper<Zevent>> entry : _registeredBuffers.entrySet()) {
                entry.getKey().clear() ; 
            }//EO while there are more buffers 
            this._registeredBuffers = null ;
            
        }//EO sync block 
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

    private void assertClassIsZevent(Class<? extends Zevent> c) {
        if (!Zevent.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("[" + c.getName() + "] does not subclass [" +
                                               Zevent.class.getName() + "]");
        }
    }

    /**
     * Registers a buffer with the internal list, so data about its contents can
     * be printed by the diagnostic thread.
     */
    public void registerBuffer(Queue<?> q, ZeventListener<? extends Zevent> e) {
        synchronized (_registeredBuffers) {
            _registeredBuffers.put(q, new TimingListenerWrapper<Zevent>(e));
        }
    }

    /**
     * Register an event class. These classes must be registered prior to
     * attempting to listen to individual event types.
     * 
     * @param eventClass a subclass of {@link Zevent}
     * @return false if the eventClass was already registered
     */
    public boolean registerEventClass(Class<? extends Zevent> eventClass) {
        assertClassIsZevent(eventClass);

        synchronized (_listenerLock) {
            if (_listeners.containsKey(eventClass))
                return false;

            _listeners.put(eventClass, new LinkedList<TimingListenerWrapper<Zevent>>());

            if (_log.isDebugEnabled()) {
                _log.debug("Register ZEvent " + eventClass);
            }

            return true;
        }
    }
    
    /**
     * Register a list of event classes. These classes must be registered prior to
     * attempting to listen to individual event types.
     * 
     * @param eventClasses a list of event classes to register
     * @return false if an event class is already registered
     */
    @Resource(name="preregisterZevents")
    public boolean registerEventClass(List<String> eventClasses) {
    	if (_log.isDebugEnabled()) {
    		_log.debug("Zevent classes to register: " + eventClasses);
    	}
    	
        boolean success = true;
        
    	for (String className : eventClasses) {
            Class<? extends Zevent> clazz;
            className = className.trim();
            if (className.length() == 0 || className.startsWith("#")) {
                continue;
            }
            try {
                clazz = (Class<? extends Zevent>) Class.forName(className);
            } catch (Exception e) {
                _log.warn("Unable to find Zevent class [" + className + "]", e);
                continue;
            }
            if (!registerEventClass(clazz)) {
            	// return false if there is at least one failure
            	success = false;
            }
        }
    	
    	return success;
    }

    /**
     * Unregister an event class
     * 
     * @param eventClass subclass of {@link Zevent}
     * @return false if the eventClass was not registered
     */
    public boolean unregisterEventClass(Class<? extends Zevent> eventClass) {
        assertClassIsZevent(eventClass);

        synchronized (_listenerLock) {
            return _listeners.remove(eventClass) != null;
        }
    }

    /**
     * Add an event listener which is called for every event type which comes
     * through the queue.
     * 
     * @return false if the listener was already listening
     */
    public boolean addGlobalListener(ZeventListener<? extends Zevent> listener) {
        synchronized (_listenerLock) {
            return _globalListeners.add(new TimingListenerWrapper<Zevent>(listener));
        }
    }

    public boolean addBufferedGlobalListener(ZeventListener<? extends Zevent> listener) {
        ThreadGroupFactory threadFact = new ThreadGroupFactory(_threadGroup, listener.toString());

        listener = new TimingListenerWrapper<Zevent>(listener);
        BufferedListener<Zevent> bListen = new BufferedListener(listener, threadFact);
        synchronized (_listenerLock) {
            return _globalListeners.add(new TimingListenerWrapper<Zevent>(bListen));
        }
    }

    /**
     * Remove a global event listener
     * 
     * @return false if the listener was not listening
     */
    public boolean removeGlobalListener(ZeventListener<? extends Zevent> listener) {
        synchronized (_listenerLock) {
            return _globalListeners.remove(listener);
        }
    }

    private List<TimingListenerWrapper<Zevent>> getEventTypeListeners(Class<? extends Zevent> eventClass) {
        synchronized (_listenerLock) {
            List<TimingListenerWrapper<Zevent>> res = _listeners.get(eventClass);

            if (res == null) {
                // Register it
                registerEventClass(eventClass);
                return _listeners.get(eventClass);
            }

            return res;
        }
    }

    /**
     * Add a buffered listener for event types. A buffered listener is one which
     * implements its own private queue and thread for processing entries. If
     * the actions performed by your listener take a while to complete, then a
     * buffered listener is a good candidate for use, since it means that it
     * will not be holding up the regular Zevent queue processor.
     * 
     * @param eventClasses {@link Class}es which subclass {@link Zevent} to
     *        listen for
     * @param listener Listener to invoke with events
     * 
     * @return the buffered listener. This return value must be used when trying
     *         to remove the listener later on.
     */
    public BufferedListener<Zevent> addBufferedListener(Set<Class<? extends Zevent>> eventClasses,
                                                        ZeventListener<? extends Zevent> listener) {
        ThreadGroupFactory threadFact = new ThreadGroupFactory(_threadGroup, listener.toString());
        listener = new TimingListenerWrapper<Zevent>(listener);
        BufferedListener<Zevent> bListen = new BufferedListener(listener, threadFact);
        for (Class<? extends Zevent> clazz : eventClasses) {
            addListener(clazz, bListen);
        }
        return bListen;
    }

    public BufferedListener<Zevent> addBufferedListener(Class<? extends Zevent> eventClass,
                                                        ZeventListener<? extends Zevent> listener) {
        Set<Class<? extends Zevent>> s = new HashSet<Class<? extends Zevent>>();
        s.add(eventClass);
        return addBufferedListener(s, listener);
    }

    /**
     * Add a listener for a specific type of event.
     * 
     * @param eventClass A subclass of {@link Zevent}
     * @return false if the listener was already registered
     */
    public boolean addListener(Class<? extends Zevent> eventClass,
                               ZeventListener<? extends Zevent> listener) {
        assertClassIsZevent(eventClass);
        synchronized (_listenerLock) {
            List<TimingListenerWrapper<Zevent>> listeners = getEventTypeListeners(eventClass);
            if (listeners.contains(listener)) {
                return false;
            }
            listeners.add(new TimingListenerWrapper<Zevent>(listener));
            return true;
        }
    }

    /**
     * Remove a specific event type listener.
     * @see #addListener(Class, ZeventListener)
     */
    public boolean removeListener(Class<? extends Zevent> eventClass,
                                  ZeventListener<? extends Zevent> listener) {
        assertClassIsZevent(eventClass);
        synchronized (_listenerLock) {
            List<TimingListenerWrapper<Zevent>> listeners = getEventTypeListeners(eventClass);
            return listeners.remove(listener);
        }
    }

    /**
     * Enqueue events onto the event queue. This method will block if the thread
     * is full.
     * 
     * @param events List of {@link Zevent}s
     * @throws InterruptedException if the queue was full and the thread was
     *         interrupted
     */
    public void enqueueEvents(List<? extends Zevent> events, long timeout) throws InterruptedException {
        if (_eventQueue.size() > getWarnSize() &&
            (System.currentTimeMillis() - _lastWarnTime) > getWarnInterval()) {
            _lastWarnTime = System.currentTimeMillis();
            _log.warn("Your event queue is having a hard time keeping up.  "
                      + "Get a faster CPU, or reduce the amount of events!");
        }
        boolean debug = _log.isDebugEnabled();
        for (Zevent e : events) {
            e.enterQueue();
            boolean b = _eventQueue.offer(e, timeout, TimeUnit.SECONDS);
            if (debug) {
                _log.debug((b?"succeed":"failed") + " pushing " + e);
            }
        }
        
        concurrentStatsCollector.addStat(_eventQueue.size(), ConcurrentStatsCollector.ZEVENT_QUEUE_SIZE);
    }

    public void enqueueEvents(List<? extends Zevent> events) throws InterruptedException {
        enqueueEvents(events, DEFAULT_TIMEOUT);
    }
    
    public void enqueueEventAfterCommit(Zevent event, long timeout) {
        enqueueEventsAfterCommit(Collections.singletonList(event),timeout);
    }

    public void enqueueEventAfterCommit(Zevent event) {
        enqueueEventAfterCommit(event,DEFAULT_TIMEOUT);
    }
    
    public void enqueueEventsAfterCommit(List<? extends Zevent> inEvents) {
        enqueueEventsAfterCommit(inEvents, DEFAULT_TIMEOUT);
    }

    /**
     * Enqueue events if the current running transaction successfully commits.
     * @see #enqueueEvents(List)
     */
    public void enqueueEventsAfterCommit(List<? extends Zevent> inEvents, final long timeout) {
        final List<Zevent> events = new ArrayList<Zevent>(inEvents);
        TransactionSynchronization txListener = new TransactionSynchronization() {
            public void afterCommit() {
                try {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Listener[" + this.toString() + "] after tx.  Enqueueing. ");
                    }
                    enqueueEvents(events, timeout);
                } catch (InterruptedException e) {
                    _log.warn("Interrupted while enqueueing events: ",e);
                } catch (Exception e) {
                    _log.error("Errorwhile enqueueing events: ",e);
                }
            }

            public void afterCompletion(int status) {
            }

            public void beforeCommit(boolean readOnly) {
            }

            public void beforeCompletion() {
            }

            public void flush() {
            }

            public void resume() {
            }

            public void suspend() {
            }
        };

        if (_log.isDebugEnabled()) {
            _log.debug("Listener[" + txListener + "] Enqueueing events: " + inEvents);
        }
        TransactionSynchronizationManager.registerSynchronization(txListener);
    }

    public void enqueueEvent(Zevent event) throws InterruptedException {
        enqueueEvents(Collections.singletonList(event));
    }

    /**
     * Wait until the queue is empty. This is a non-performant function, so
     * please only use it in test suites.
     */
    public void waitUntilNoEvents() throws InterruptedException {
        while (_eventQueue.size() != 0)
            Thread.sleep(100);
    }

    /**
     * Returns the listeners which have specifically registered for the
     * specified event type. This method does not return global event listeners.
     * 
     * If the event type was never registered, null will be returned, otherwise
     * a list of {@link ZeventListener}s (of potentially size=0)
     */
    private List<TimingListenerWrapper<Zevent>> getTypeListeners(Zevent z) {
        synchronized (_listenerLock) {
            return _listeners.get(z.getClass());
        }
    }

    /**
     * Internal method to dispatch events. Called by the {@link QueueProcessor}.
     * 
     * The strategy used in this method creates mini-batches of events to send
     * to each listener. There is no defined order for listener execution.
     */
    void dispatchEvents(List<? extends Zevent> events) {
        synchronized (INIT_LOCK) {
            for (Zevent z : events) {
                long timeInQueue = z.getQueueExitTime() - z.getQueueEntryTime();
                if (timeInQueue > _maxTimeInQueue)
                    _maxTimeInQueue = timeInQueue;
                _numEvents++;
            }
        }

        List<Zevent> validEvents = new ArrayList<Zevent>(events.size());
        Map<ZeventListener<Zevent>, List<Zevent>> listenerBatches;
        synchronized (_listenerLock) {
            listenerBatches = new HashMap<ZeventListener<Zevent>, List<Zevent>>(_globalListeners.size());
            for (Zevent z : events) {
                List<TimingListenerWrapper<Zevent>> typeListeners = getTypeListeners(z);

                if (typeListeners == null) {
                    _log.warn("Unable to dispatch event of type [" + z.getClass().getName() +
                              "]:  Not registered");
                    continue;
                }
                validEvents.add(z);

                for (ZeventListener<Zevent> listener : typeListeners) {
                    List<Zevent> batch = listenerBatches.get(listener);

                    if (batch == null) {
                        batch = new LinkedList<Zevent>();
                        listenerBatches.put(listener, batch);
                    }
                    batch.add(z);
                }
            }

            for (ZeventListener<Zevent> listener : _globalListeners) {
                listenerBatches.put(listener, validEvents);
            }
        }

        long timeout = getListenerTimeout();
        for (Entry<ZeventListener<Zevent>, List<Zevent>> ent : listenerBatches.entrySet()) {
            ZeventListener<Zevent> listener = ent.getKey();
            List<Zevent> batch = ent.getValue();

            synchronized (_listenerLock) {
                InterruptToken t = null;
                try {
                    t = threadWatchdog.interruptMeIn(timeout, TimeUnit.SECONDS,
                        "Processing listener events");
                    listener.processEvents(Collections.unmodifiableList(batch));
                } catch (RuntimeException e) {
                    _log.warn("Exception while invoking listener [" + listener + "]", e);
                } finally {
                    if (t != null) {
                        threadWatchdog.cancelInterrupt(t);
                    }
                }
            }
        }
    }

    private String getDiagnostics() {
        synchronized (INIT_LOCK) {
            StringBuffer res = new StringBuffer();

            res.append("ZEvent Manager Diagnostics:\n").append(
                "    Queue Size:        " + _eventQueue.size() + "\n").append(
                "    Events Handled:    " + _numEvents + "\n").append(
                "    Max Time In Queue: " + _maxTimeInQueue + "ms\n\n").append(
                "ZEvent Listener Diagnostics:\n");
            PrintfFormat timingFmt = new PrintfFormat("        %-30s max=%-7.2f avg=%-5.2f "
                                                      + "num=%-5d\n");
            synchronized (_listenerLock) {

                for (Entry<Class<? extends Zevent>, List<TimingListenerWrapper<Zevent>>> ent : _listeners.entrySet()) {
                    List<TimingListenerWrapper<Zevent>> listeners = ent.getValue();
                    res.append("    EventClass: " + ent.getKey() + "\n");
                    for (TimingListenerWrapper<Zevent> l : listeners) {
                        Object[] args = new Object[] { l.toString(),
                                                      new Double(l.getMaxTime()),
                                                      new Double(l.getAverageTime()),
                                                      new Long(l.getNumEvents()) };

                        res.append(timingFmt.sprintf(args));
                    }
                    res.append("\n");
                }

                res.append("    Global Listeners:\n");
                for (TimingListenerWrapper<Zevent> l : _globalListeners) {
                    Object[] args = new Object[] { l.toString(),
                                                  new Double(l.getMaxTime()),
                                                  new Double(l.getAverageTime()),
                                                  new Long(l.getNumEvents()), };

                    res.append(timingFmt.sprintf(args));
                }
            }

            synchronized (_registeredBuffers) {
                PrintfFormat fmt = new PrintfFormat("    %-30s size=%d\n");
                res.append("\nZevent Registered Buffers:\n");
                for (Entry<Queue<?>, TimingListenerWrapper<Zevent>> ent : _registeredBuffers.entrySet()) {
                    Queue<?> q = ent.getKey();
                    TimingListenerWrapper<Zevent> targ = ent.getValue();
                    res.append(fmt.sprintf(new Object[] { targ.toString(), new Integer(q.size()), }));
                    res.append(timingFmt.sprintf(new Object[] { "", // Target
                                                               // already
                                                               // printed
                                                               // above
                                                               new Double(targ.getMaxTime()),
                                                               new Double(targ.getAverageTime()),
                                                               new Long(targ.getNumEvents()), }));
                }
            }

            return res.toString();
        }
    }
    
    private long getTotalRegisteredBufferSize() {
        synchronized (_registeredBuffers) {
            long rtn = 0;
            for (Entry<Queue<?>, TimingListenerWrapper<Zevent>> ent : _registeredBuffers.entrySet()) {
                rtn += ent.getKey().size();
            }
            return rtn;
        }
    }

    public static ZeventEnqueuer getInstance() {
        return Bootstrap.getBean(ZeventEnqueuer.class);
    }
}

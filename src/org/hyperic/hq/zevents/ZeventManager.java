package org.hyperic.hq.zevents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;

/**
 * The Zevent subsystem is an event system for fast, non-reliable transmission
 * of events.  Important data should not be transmitted on this bus, since
 * it is not persisted, and there is never any guarantee of receipt.  
 * 
 * This manager provides no transactional guarantees, so the caller must 
 * rollback additions of listeners if the transaction fails. 
 */
public class ZeventManager { 
    private static final int MAX_QUEUE_ENTRIES = 100 * 1000;

    private final Log _log = LogFactory.getLog(ZeventManager.class);
    
    private static final Object INIT_LOCK = new Object();
    private static ZeventManager INSTANCE;

    // The thread group that the {@link EventQueueProcessor} comes from 
    private final ProcessorThreadGroup _threadGroup;

    // The actual queue processor thread
    private Thread _processorThread;
    
    /* Set of {@link ZeventListener}s listening to events of all types */
    private final Set _globalListeners = new HashSet(); 

    /* Map of {@link Class}es subclassing {@link ZEvent} onto lists of 
     * {@link ZeventListener}s */
    private final Map _listeners = new HashMap();
    
    private QueueProcessor _queueProcessor;
    private BlockingQueue  _eventQueue = 
        new LinkedBlockingQueue(MAX_QUEUE_ENTRIES);

    
    private ZeventManager() {
        _threadGroup = new ProcessorThreadGroup();
        _threadGroup.setDaemon(true);
    }
    
    public void shutdown() throws InterruptedException {
        while (!_eventQueue.isEmpty()) {
            System.out.println("Waiting for empty queue: " + _eventQueue.size());
            Thread.sleep(1000);
        }
        _processorThread.interrupt();
        _processorThread.join(5000);
    }
    
    private void assertClassIsZevent(Class c) {
        if (!Zevent.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("[" + c.getName() + 
                                               "] does not subclass [" +
                                               Zevent.class.getName() + "]");
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
     
        synchronized (_listeners) {
            if (_listeners.containsKey(eventClass))
                return false;
            
            _listeners.put(eventClass, new ArrayList());
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
        
        synchronized (_listeners) {
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
        synchronized (_listeners) {
            return _globalListeners.add(listener);
        }
    }
    
    /**
     * Remove a global event listener
     * 
     * @return false if the listener was not listening
     */
    public boolean removeGlobalListener(ZeventListener listener) {
        synchronized (_listeners) {
            return _globalListeners.remove(listener);
        }
    }
    
    private List getEventTypeListeners(Class eventClass) {
        synchronized (_listeners) {
            List res = (List)_listeners.get(eventClass);
            
            if (res == null)
                throw new IllegalArgumentException("Event type [" + 
                                                   eventClass.getName() + 
                                                   "] not registered");
            return res;
        }
    }
    
    /**
     * Add a listener for a specific type of event.
     * 
     * @param eventClass A subclass of {@link Zevent}
     * @return false if the listener was already registered
     */
    public boolean addListener(Class eventClass, ZeventListener listener) {
        assertClassIsZevent(eventClass);

        synchronized (_listeners) {
            List listeners = getEventTypeListeners(eventClass);

            if (listeners.contains(listener))
                return false;
            listeners.add(listener);
            return true;
        }
    }

    /**
     * Remove a specific event type listener.
     * @see #addListener(Class, ZeventListener)
     */
    public boolean removeListener(Class eventClass, ZeventListener listener) {
        assertClassIsZevent(eventClass);
        
        synchronized (_listeners) {
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
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            Zevent e = (Zevent)i.next();  

            e.enterQueue();
            _eventQueue.put(e);
        }
    }
    
    /**
     * Internal method to dispatch events.  Called by the 
     * {@link EventQueueProcessor}
     */
    void dispatchEvent(Zevent e) {
        List listeners;
            
        synchronized (_listeners) {
            List typeListeners = (List)_listeners.get(e.getClass());

            if (typeListeners == null) {
                _log.warn("Unable to dispatch event of type [" + 
                          e.getClass().getName() + "]:  Not registered");
                return;
            }
            listeners = new ArrayList(typeListeners.size() + 
                                      _globalListeners.size());
            listeners.addAll(typeListeners);
            listeners.addAll(_globalListeners);
        }
            
        /* Eventually we may want to de-queue a bunch at a time (if they are
         * the same event type, and pass those lists off all at once */
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            ZeventListener listener = (ZeventListener)i.next();

            listener.processEvents(Collections.singletonList(e));
        }
    }
    
    public static ZeventManager getInstance() {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new ZeventManager();
                QueueProcessor p = new QueueProcessor(INSTANCE, 
                                                      INSTANCE._eventQueue);
                INSTANCE._queueProcessor = p;
                INSTANCE._processorThread = new Thread(INSTANCE._threadGroup, 
                                                       p, "ZeventProcessor");
                INSTANCE._processorThread.setDaemon(true);
                INSTANCE._processorThread.start();
            }
        }
        return INSTANCE;
    }
}

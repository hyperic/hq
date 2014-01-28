/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2009], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */

package org.hyperic.hq.bizapp.server.mdb;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.server.shared.HeartbeatCurrentTime;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The RegisteredDispatcher Message-Drive Bean registers Triggers and dispatches
 * events to them
 * <p>
 *
 * </p>
 * TODO: Check if dups-ok maps correctly to the expected non-transaction semantics.
 * 
 * This is intentionally NOT TRANSACTIONAL.  Had to mark it specifically as NotSupported b/c MDBs are required to have some type of transactional boundary.
 * We are specifically NOT interacting with database or Hibernate sessions during message processing for performance reasons
 * Bound to topic/eventsTopic
 */
public class RegisteredDispatcherImpl implements MessageListener, HeartbeatCurrentTime {
    private final Log log = LogFactory.getLog(RegisteredDispatcherImpl.class);
    
    private RegisteredTriggers registeredTriggers;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private AtomicLong heartbeatTime = new AtomicLong();
    
    @Autowired
    public RegisteredDispatcherImpl(RegisteredTriggers registeredTriggers, ConcurrentStatsCollector concurrentStatsCollector) {
        this.registeredTriggers = registeredTriggers;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.heartbeatTime.set(System.currentTimeMillis());
    }
    
    @PostConstruct
    public void initStatsCollector() {
    	concurrentStatsCollector.register(ConcurrentStatsCollector.EVENT_PROCESSING_TIME);
    }

    /**
     * Dispatch the event to interested triggers.
     *
     * @param event The event.
     */
    private void dispatchEvent(AbstractEvent event) {
        // Get interested triggers
        Collection<RegisterableTriggerInterface> triggers = getInterestedTriggers(event);

        if (log.isDebugEnabled()) {
            log.debug("There are " + triggers.size() + " registered for event");
        }

        // Dispatch to each trigger
        for (RegisterableTriggerInterface registerableTrigger : triggers) {
            TriggerInterface trigger = (TriggerInterface) registerableTrigger;
            long startTime = System.currentTimeMillis();
            try {
                trigger.processEvent(event);
                concurrentStatsCollector.addStat(System.currentTimeMillis() - startTime, ConcurrentStatsCollector.EVENT_PROCESSING_TIME);
            } catch (EventTypeException e) {
                // The trigger was not meant to process this event
                log.error("dispatchEvent dispatched to trigger (" + trigger.getClass() + " that's not " +
                          "configured to handle this type of event: " + event.getClass(), e);
            } catch (Exception e) {
                // handle everything here
                log.error("Failed to dispatch event", e);
            }
        }
    }

    protected Collection<RegisterableTriggerInterface> getInterestedTriggers(AbstractEvent evt) {
        return registeredTriggers.getInterestedTriggers(evt);
    }

    /**
     * The onMessage method
     * 
     */
    @SuppressWarnings("unchecked")
    public void onMessage(Message inMessage) {
        if (!(inMessage instanceof ObjectMessage)) {
            return;
        }
        final boolean debug = log.isDebugEnabled();
        Object obj;
        try {
            ObjectMessage om = (ObjectMessage) inMessage;
            if (debug) { log.debug("Redelivering message=" + inMessage.getJMSRedelivered()); }
            obj = om.getObject();
        } catch (JMSException e) {
            log.error("Cannot open message object", e);
            return;
        }

        if (obj instanceof HeartBeatEvent) {
            final HeartBeatEvent event = (HeartBeatEvent) obj;
            final long timestamp = event.getTimestamp();
            if (debug) log.debug("setting heartbeat timestamp to " + TimeUtil.toString(timestamp));
            heartbeatTime.set(timestamp);
        }

        if (obj instanceof AbstractEvent) {
            AbstractEvent event = (AbstractEvent) obj;
            if (debug) { log.debug("1 event in the message"); }
            dispatchEvent(event);
        } else if (obj instanceof Collection<?>) {
            Collection<AbstractEvent> events = (Collection<AbstractEvent>) obj;
            if (debug) { log.debug(events.size() + " events in the message"); }
            for (AbstractEvent event : events) {
                dispatchEvent(event);
            }
        }
    }

    public long getTimeMillis() {
        return heartbeatTime.get();
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.mdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.LoggableInterface;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.shared.EventLogManager;
import org.springframework.beans.factory.annotation.Autowired;

/** 
 * The LoggingDispatcher Message-Drive Bean is intended to be used
 * to log Events.
 * 
 * Bound to topic/eventsTopic 
 */
public class LoggingDispatcherImpl implements MessageListener {
    private final Log log = LogFactory.getLog(LoggingDispatcherImpl.class);
    private EventLogManager eventLogManager;

    @Autowired
    public LoggingDispatcherImpl(EventLogManager eventLogManager) {
        this.eventLogManager = eventLogManager;
    }

    @SuppressWarnings("unchecked")
    public void onMessage(Message inMessage) {
        if (!(inMessage instanceof ObjectMessage)) {
            return;
        }

        ObjectMessage om = (ObjectMessage) inMessage;
            
        try {
            Object obj = om.getObject();
            
            if (obj instanceof AbstractEvent) {
                AbstractEvent event = (AbstractEvent) obj;
                logEvent(event);
            } else if (obj instanceof Collection<?>) {
                Collection<AbstractEvent> events = (Collection<AbstractEvent>) obj;
                logEvents(events);
            }
        } catch (JMSException e) {
            log.error("Cannot open message object", e);
        }
        
    }
    
    /**
     * Log the event if it supports logging.
     * 
     * @param event The event.
     */
    private void logEvent(AbstractEvent event) {
        try {
            if (event.isLoggingSupported()) {
                LoggableInterface le = (LoggableInterface) event;            
                eventLogManager.createLog(event, le.getSubject(), le.getLevelString(), true);
            }        
        } catch (ResourceDeletedException e) {
            log.debug(e);
        }
    }
    
    private void logEvents(Collection<AbstractEvent> events) {
        List<EventLog> loggableEvents = new ArrayList<EventLog>();
        
        for (AbstractEvent event : events) {
            try {
                if (event.isLoggingSupported()) {
                    LoggableInterface le = (LoggableInterface) event;
                    EventLog eventLog = eventLogManager.createLog(event, 
                                                        le.getSubject(), 
                                                        le.getLevelString(), 
                                                        false);
                    loggableEvents.add(eventLog);
                }
            } catch (ResourceDeletedException e) {
                log.debug(e);
            }
        }
        
        if (!loggableEvents.isEmpty()) {
            eventLogManager.insertEventLogs(loggableEvents.toArray(
                                       new EventLog[loggableEvents.size()]));
        }
    }
}

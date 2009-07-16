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

package org.hyperic.hq.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.stats.ConcurrentStatsCollector;

public class Messenger {
    private static Log _log = LogFactory.getLog(Messenger.class);

    // Static Strings for everyone
    public static final String CONN_FACTORY_JNDI = "java:/ConnectionFactory";

    // Static InitialContext for convenience
    private static InitialContext _ic;

    // Static ConnectionFactory for convenience
    private static Object _factory;

    // Cache of topics and queues
    private static Map _tAndQs = new HashMap();
    private static ThreadLocal _localTAndQs = new ThreadLocal() {
        public Object initialValue() {
            return new HashMap();
        }
    };

    // Instance variables
    private QueueConnection _qConn;
    private QueueSession    _qSession;
    private TopicConnection _tConn;
    private TopicSession    _tSession;

    public Messenger() {
    }

    public Messenger(QueueConnection qConn, QueueSession qSession) {
        _qConn    = qConn;
        _qSession = qSession;
    }

    public Messenger(TopicConnection tConn, TopicSession tSession) {
        _tConn    = tConn;
        _tSession = tSession;
    }
    
    /**
     * Reset the thread local queue.
     */
    public static void resetThreadLocalQueue() {
        ThreadLocalQueue.getInstance().clearEnqueuedObjects();
    }
    
    /**
     * Enqueue a message on the thread local.
     * 
     * @param sObj The message.
     */
    public static void enqueueMessage(Serializable sObj) {
        ThreadLocalQueue.getInstance().enqueueObject(sObj);
    }
    
    /**
     * Drain the thread local of its enqueued messages.
     * 
     * @return The enqueued messages.
     */
    public static List drainEnqueuedMessages() {
        ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
        List enqueued = queue.getEnqueuedObjects();
        queue.clearEnqueuedObjects();
        return enqueued;
    }
    
    private Queue getQueue(String name) {
        return (Queue) getDestination(name);
    }
    
    private Topic getTopic(String name) {
        return (Topic) getDestination(name);
    }
    
    private Destination getDestination(String name) {
        
        Destination result = null;
        
        try {
            
            Map tlMap = (Map) _localTAndQs.get();
            result = (Destination) tlMap.get(name);
            if (result == null) {
                synchronized (_tAndQs) {
                    result = (Destination) _tAndQs.get(name);
                    if (result == null) {
                        // Static Queue for convenience
                        result = (Destination) _ic.lookup(name);
                        _tAndQs.put(name, result);
                    }
                }

                tlMap.put(name, result);
            }
            
        } catch (NamingException ne) {
            _log.error("Naming error for " + name + ": " + ne.toString());
        }

        return result;
    }

    /**
     * Send message to a Queue.
     */
    public void sendMessage(String name, Serializable sObj) {
        QueueConnection conn = null;
        QueueSession session = null;
        try {
            if (_ic == null)
                _ic = new InitialContext();

            if (_factory == null)
                _factory = _ic.lookup(CONN_FACTORY_JNDI);

            QueueConnectionFactory qFactory = (QueueConnectionFactory) _factory;
            Queue queue = getQueue(name);
            if (queue != null) {

                // Now create a connection to send a message
                if (_qConn != null)
                    conn = _qConn;
                else
                    conn = qFactory.createQueueConnection();

                if (conn == null)
                    _log.error("QueueConnection cannot be created");

                if (_qSession != null)
                    session = _qSession;
                else
                    session = conn.createQueueSession(false,
                                                      Session.AUTO_ACKNOWLEDGE);

                // Create a sender and send the message
                final long start = System.currentTimeMillis();
                QueueSender sender = session.createSender(queue);
                ObjectMessage msg = session.createObjectMessage();
                msg.setObject(sObj);
                sender.send(msg);
                final long now = System.currentTimeMillis();
                ConcurrentStatsCollector.getInstance().addStat(
                            (now - start), ConcurrentStatsCollector.JMS_QUEUE_PUBLISH_TIME);
            }
        } catch (NamingException e) {
            _log.error("Naming error for " + name + ": " + e.toString());
        } catch (Exception e) {
            _log.error("Failed to send message ", e);
        } finally {
            // Close connections if we created them
            try { if (_qSession == null) session.close(); }
            catch (Exception e) { _log.warn("Error closing session: " + e, e); }
            try { if (_qConn == null) conn.close(); }
            catch (Exception e) { _log.warn("Error closing conn: " + e, e); }
        }
    }

    /**
     * Send a List to a Topic.
     */
    public void publishMessage(String name, List msgList, int maxSize) {
        final boolean debug = _log.isDebugEnabled();
        for (int i = 0; i < msgList.size(); i += maxSize) {
            int end = Math.min(i + maxSize, msgList.size());
            ArrayList msgObj = new ArrayList(msgList.subList(i, end));
            publishMessage(name, msgObj);
            if (debug) {
                _log.debug("Sent " + (end - i) + " batched events to " + name);
            }
        }
    }
    
    /**
     * Send message to a Topic.
     */
    public void publishMessage(String name, Serializable sObj) {
        TopicConnection conn = null;
        TopicSession session = null;
        try {
            if (_ic == null)
                _ic = new InitialContext();

            if (_factory == null)
                _factory = _ic.lookup(CONN_FACTORY_JNDI);

            TopicConnectionFactory tFactory = (TopicConnectionFactory) _factory;

            Topic topic = getTopic(name);
            if (topic != null) {

                // Now create a connection to send a message
                if (_tConn != null)
                    conn = _tConn;
                else
                    conn = tFactory.createTopicConnection();

                if (conn == null)
                    _log.error("TopicConnection cannot be created");

                if (_tSession != null)
                    session = _tSession;
                else
                    session = conn.createTopicSession(false,
                                                      Session.AUTO_ACKNOWLEDGE);

                // Create a publisher and publish the message
                final long start = System.currentTimeMillis();
                TopicPublisher publisher = session.createPublisher(topic);
                ObjectMessage msg = session.createObjectMessage();
                msg.setObject(sObj);
                publisher.publish(msg);
                final long now = System.currentTimeMillis();
                ConcurrentStatsCollector.getInstance().addStat(
                             (now - start), ConcurrentStatsCollector.JMS_TOPIC_PUBLISH_TIME);
            }
        } catch (NamingException e) {
            _log.error("Naming error for " + name + ": " + e.toString());
        } catch (Exception e) {
            _log.error("Failed to publish message: ", e);
        } finally {
            // Close connections if we created them
            try { if (_tSession == null) session.close(); }
            catch (Exception e) { _log.warn("Error closing session: " + e, e); }
            try { if (_tConn == null) conn.close(); }
            catch (Exception e) { _log.warn("Error closing conn: " + e, e); }
        }
    }
}

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

package org.hyperic.hq.common.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashMap;

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

/**
 */
public class Messenger {
    private static Log log = LogFactory.getLog(Messenger.class);

    // Static Strings for everyone
    public static final String CONN_FACTORY_JNDI = "java:/ConnectionFactory";

    // Static InitialContext for convenience
    private static InitialContext ic = null;

    // Static ConnectionFactory for convenience
    private static Object factory = null;

    // Static HashMap of Topics and Queues
    private static HashMap tAndQs = new HashMap();

    // Instance variables
    QueueConnection qConn = null;
    QueueSession qSession = null;
    TopicConnection tConn = null;
    TopicSession tSession = null;

    /**
     * Constructor for Messenger.
     */
    public Messenger() {
    }

    /**
     * Constructor for Messenger.
     */
    public Messenger(QueueConnection qConn, QueueSession qSession) {
        this.qConn = qConn;
        this.qSession = qSession;
    }

    /**
     * Constructor for Messenger.
     */
    public Messenger(TopicConnection tConn, TopicSession tSession) {
        this.tConn = tConn;
        this.tSession = tSession;
    }

    /**
     * Send message to a Queue.
     */
    public void sendMessage(String name, Serializable sObj) {
        QueueConnection conn = null;
        QueueSession session = null;
        try {
            if (ic == null)
                ic = new InitialContext();

            if (factory == null)
                factory = ic.lookup(CONN_FACTORY_JNDI);

            QueueConnectionFactory qFactory = (QueueConnectionFactory) factory;

            if (!tAndQs.containsKey(name)) {
                // Static Queue for convenience
                Queue queue = (Queue) ic.lookup(name);
                tAndQs.put(name, queue);
            }

            Queue queue = (Queue) tAndQs.get(name);

            // Now create a connection to send a message
            if (qConn != null)
                conn = qConn;
            else
                conn = qFactory.createQueueConnection();

            if (conn == null)
                log.error("QueueConnection cannot be created");
            
            if (qSession != null)
                session = qSession;
            else
                session = conn.createQueueSession(false,
                                                  Session.AUTO_ACKNOWLEDGE);

            // Create a sender and send the message
            QueueSender sender = session.createSender(queue);
            ObjectMessage msg = session.createObjectMessage();
            msg.setObject(sObj);
            sender.send(msg);
        } catch (NamingException e) {
            log.error("Naming error for " + name + ": " + e.toString());
        } catch (Exception e) {
            log.error("Failed to send message ", e);
        } finally {
            // Close connections if we created them
            try { if (qSession == null) session.close(); }
            catch (Exception e) { log.warn("Error closing session: " + e, e); }
            try { if (qConn == null) conn.close(); }
            catch (Exception e) { log.warn("Error closing conn: " + e, e); }
        }
    }

    /**
     * Send message to a Topic.
     */
    public void publishMessage(String name, Serializable sObj) {
        TopicConnection conn = null;
        TopicSession session = null;
        try {
            if (ic == null)
                ic = new InitialContext();

            if (factory == null)
                factory = ic.lookup(CONN_FACTORY_JNDI);

            TopicConnectionFactory tFactory = (TopicConnectionFactory) factory;

            if (!tAndQs.containsKey(name)) {
                // Static Queue for convenience
                Topic topic = (Topic) ic.lookup(name);
                tAndQs.put(name, topic);
            }

            Topic topic = (Topic) tAndQs.get(name);

            // Now create a connection to send a message
            if (tConn != null)
                conn = tConn;
            else
                conn = tFactory.createTopicConnection();

            if (conn == null)
                log.error("TopicConnection cannot be created");
            
            if (tSession != null)
                session = tSession;
            else
                session = conn.createTopicSession(false,
                                                  Session.AUTO_ACKNOWLEDGE);

            // Create a publisher and publish the message
            TopicPublisher publisher = session.createPublisher(topic);
            ObjectMessage msg = session.createObjectMessage();
            msg.setObject(sObj);
            publisher.publish(msg);
        } catch (NamingException e) {
            log.error("Naming error for " + name + ": " + e.toString());
        } catch (Exception e) {
            log.error("Failed to publish message: ", e);
        } finally {
            // Close connections if we created them
            try { if (tSession == null) session.close(); }
            catch (Exception e) { log.warn("Error closing session: " + e, e); }
            try { if (tConn == null) conn.close(); }
            catch (Exception e) { log.warn("Error closing conn: " + e, e); }
        }
    }

}

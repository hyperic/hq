package org.hyperic.hq.plugin.wsmq.test;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class JmsProducer {

	private static int status = 1;

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Variables
		Connection connection = null;
		Session session = null;
		Destination destination = null;
		MessageProducer producer = null;

		try {
			// Create a connection factory
			JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			JmsConnectionFactory cf = ff.createConnectionFactory();

			// Set the properties
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, "redhat");
			cf.setIntProperty(WMQConstants.WMQ_PORT, 1414);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, "TO.VENUS.CH");
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "VENUS.QMANAGER");

			// Create JMS objects
			connection = cf.createConnection("mqm", "seosal99");
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue("OUT.QUEUE");
			producer = session.createProducer(destination);

			// Start the connection
			connection.start();

			while (true) {
				long uniqueNumber = System.currentTimeMillis() % 1000;
				TextMessage message = session.createTextMessage("JmsProducer: Your lucky number today is " + uniqueNumber);
				// And, send the message
				producer.send(message);
				System.out.println("Sent message:\n" + message);
				Thread.sleep(1000);
			}

		} catch (Exception jmsex) {
			jmsex.printStackTrace();
		} finally {
			if (producer != null) {
				try {
					producer.close();
				} catch (JMSException jmsex) {
					jmsex.printStackTrace();
				}
			}

			if (session != null) {
				try {
					session.close();
				} catch (JMSException jmsex) {
					jmsex.printStackTrace();
				}
			}

			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException jmsex) {
					jmsex.printStackTrace();
				}
			}
		}
		System.exit(status);
	} // end main()

	/**
	 * Record this run as successful.
	 */
	private static void recordSuccess() {
		System.out.println("SUCCESS");
		status = 0;
	}
} // end class

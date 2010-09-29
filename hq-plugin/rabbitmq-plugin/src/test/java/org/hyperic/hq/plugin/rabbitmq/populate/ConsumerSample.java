/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.populate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
 
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * ConsumerSample populates QA RabbitMQ servers with data to test.
 * @author Helena Edelson
 */
public class ConsumerSample {

    private static int NUM_MESSAGES = 500;

    private RabbitTemplate rabbitTemplate;

    private Queue marketDataQueue;

    public ConsumerSample(RabbitTemplate rabbitTemplate, Queue marketDataQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.marketDataQueue = marketDataQueue;
    }

	protected void receiveSync() {
		for (int i = 0; i < NUM_MESSAGES; i++) {
			Message message = this.rabbitTemplate.receive();//this.marketDataQueue.getName()
			if (message == null) {
				//System.out.println("Thread [" + Thread.currentThread().getId() + "] Received Null Message!");
			}
			else {
				System.out.println("Thread [" + Thread.currentThread().getId() + "] Received Message = " + new String(message.getBody()));
				Map<String, Object> headers = message.getMessageProperties().getHeaders();
				Object objFloat = headers.get("float");
				Object objcp = headers.get("object");
				System.out.println("float header type = " + objFloat.getClass());
				System.out.println("object header type = " + objcp.getClass());
			}
		}
	}

    protected void receiveAsync() {
        SimpleMessageListener ml = new SimpleMessageListener();
		System.out.println("Main execution thread sleeping 5 seconds...");
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        System.out.println("Application exiting.");
		System.exit(0);
	}
    
	public static class SimpleMessageListener implements MessageListener {

		private final AtomicInteger messageCount = new AtomicInteger();

		public void onMessage(Message message) {
			int msgCount = this.messageCount.incrementAndGet();
			System.out.println("Thread [" + Thread.currentThread().getId()
					+ "] SimpleMessageListener Received Message " + msgCount
					+ ", = " + new String(message.getBody()));
		}
	}
}

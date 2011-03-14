package org.hyperic.hq.amqp.unit;

import org.hyperic.hq.amqp.BaseSAmqpTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Helena Edelson
 */
@Ignore
public class AtomicSAMQPTests extends BaseSAmqpTest {

    /**
     * Pass in routing
     */
    public AtomicSAMQPTests() {
        super("ping", new TopicExchange("agent.ping.request", false, false), new TopicExchange("agent.ping.response", false, false), true);
    }

    @Test
    public void testAtomic() throws InterruptedException {
        requestTemplate.convertAndSend("agent:ping-request");
        String result = (String) responseTemplate.receiveAndConvert(requestQueue.getName());
        responseTemplate.convertAndSend("agent:ping-response");
        result = (String) requestTemplate.receiveAndConvert(responseQueue.getName());
        System.out.println("received=" + result); 
        assertEquals("agent:ping-response", result);
    }

    @Test
	public void atomicSendAndReceive() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		// Set up a consumer to respond to out producer
		Future<String> received = executor.submit(new Callable<String>() {

			public String call() throws Exception {
				Message message = null;
				for (int i = 0; i < 10; i++) {
					message = responseTemplate.receive(requestQueue.getName());
					if (message != null) {
                        System.out.println("call, responseTemplate received=" + new String(message.getBody()));
                        responseTemplate.send(message);
						break;
					}
					Thread.sleep(100L);
				}
				assertNotNull("No message received", message);
                responseTemplate.send(message);
                return (String) requestTemplate.getMessageConverter().fromMessage(message);
			}

		});
		String result = (String) requestTemplate.convertSendAndReceive(requestExchange.getName(), routingKey, "message");
        System.out.println("requestTemplate received=" + result);
		assertEquals("message", received.get(1000, TimeUnit.MILLISECONDS));
		assertEquals("message", result);
		// Message was consumed so nothing left on queue
		result = (String) requestTemplate.receiveAndConvert(responseQueue.getName());
		assertEquals(null, result);
	}

}

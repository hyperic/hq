package org.hyperic.hq.amqp.unit;

import org.hyperic.hq.amqp.BaseUnitTest;
import org.junit.Test;
import org.springframework.amqp.core.Message;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Helena Edelson
 */
public class AtomicSAMQPTests extends BaseUnitTest {


    @Test
    public void testAtomic() throws InterruptedException {
        agentTemplate.convertAndSend("first-message");
        Thread.sleep(2000);
        String result = (String) agentTemplate.convertSendAndReceive("second-message");
        System.out.println("result=" + result);
        //assertEquals("message", result);
    }

    @Test
    public void testAtomicSendAndReceive() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        // Set up a consumer to respond to out producer
        Future<String> received = executor.submit(new Callable<String>() {

            public String call() throws Exception {
                Message message = null;
                for (int i = 0; i < 10; i++) {
                    message = serverTemplate.receive(serverQueue.getName());
                    if (message != null) {
                        break;
                    }
                    Thread.sleep(100L);
                }
                assertNotNull("No message received", message);
                serverTemplate.send(agentExchange.getName(), agentQueue.getName(), message);
                return (String) agentTemplate.getMessageConverter().fromMessage(message);
            }

        });
        /* send a message to the server */
        String result = (String) agentTemplate.convertSendAndReceive(serverExchange.getName(), agentQueue.getName(), "message");
        assertEquals("message", received.get(1000, TimeUnit.MILLISECONDS));
        assertEquals("message", result);
        // Message was consumed so nothing left on queue
        result = (String) agentTemplate.receiveAndConvert(agentQueue.getName());
        assertEquals(null, result);
    }

}

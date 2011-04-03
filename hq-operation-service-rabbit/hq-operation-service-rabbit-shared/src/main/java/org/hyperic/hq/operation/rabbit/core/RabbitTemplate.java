package org.hyperic.hq.operation.rabbit.core;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public interface RabbitTemplate {

    void send(String exchangeName, String routingKey, Object message) throws IOException;

    Object sendAndReceive(String exchangeName, String routingKey, String data) throws IOException, InterruptedException;
    
}

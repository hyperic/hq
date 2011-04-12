package org.hyperic.hq.operation.rabbit.shared;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Helena Edelson
 */
public class ConnectionTests {

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

    private final TaskExecutor taskExecutor = mock(TaskExecutor.class);

    private final Connection connection = mock(Connection.class);

    private final Channel channel = mock(Channel.class);
    
    @Test
    public void testConnection() throws IOException {
        when(this.connectionFactory.newConnection()).thenReturn(this.connection);
        when(this.connection.createChannel()).thenReturn(this.channel);
    }
}

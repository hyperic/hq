package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.rabbit.convert.JsonObjectMappingConverter;
import org.hyperic.hq.operation.rabbit.core.AnnotatedOperationService;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.mock;

/**
 * TODO after refactored changes
 */
public class OperationServiceTests {

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

    private final TaskExecutor taskExecutor = mock(TaskExecutor.class);

    private final Connection connection = mock(Connection.class);
    
    private AnnotatedOperationService operationService;

    private JsonObjectMappingConverter converter = new JsonObjectMappingConverter();

    private RegisterAgentRequest registerAgentRequest = new RegisterAgentRequest(null,"testAuth", "5.0", 1, "localhost", 0, "hqadmin", "hqadmin", false);


    /*@Before
    public void prepare() {
        ConnectionFactory cf = new ConnectionFactory();
        this.operationService = new AnnotatedRabbitOperationService(new SimpleRabbitTemplate(cf), new OperationToRoutingKeyRegistry(cf));
    }*/

    @Test
    public void perform() {
    /*
        this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        assertNotNull(this.operationService.getMappings().map(Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST));
        Envelope envelope = new Envelope(Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST, converter.write(registerAgentRequest));
        assertTrue(envelope.getContent().equals(converter.write(registerAgentRequest)));
        assertTrue((Boolean) this.operationService.perform(envelope));
      */
    }

    @Test
    public void discover() {
        /*this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.discover(new TestEndpoint(), OperationEndpoint.class);
        assertEquals(this.operationService.getMappings().getOperationMappings().size(), 1);*/
    }

    @Test
    public void dispatch() {
        /*this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.dispatch(Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST, registerAgentRequest);
        AnnotatedOperationEndpointRegistry.MethodInvoker invoker = this.operationService.getMappings().getOperationMappings().get(Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST);
        assertTrue(invoker.toString().contains(Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST));*/
    }

    @Test
    public void handle() throws InvocationTargetException, IllegalAccessException {
        /*this.operationService.discover(new TestEndpoint(), OperationEndpoint.class);
        this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.dispatch(Constants.ROUTING_KEY_AGENT_REGISTER_RESPONSE, registerAgentRequest);

        AnnotatedOperationEndpointRegistry.MethodInvoker invoker = this.operationService.getMappings().map(Constants.ROUTING_KEY_AGENT_REGISTER_RESPONSE);
        assertNotNull(invoker);

        invoker.invoke(converter.write(registerAgentRequest));
        Envelope envelope = new Envelope(Constants.ROUTING_KEY_AGENT_REGISTER_RESPONSE, converter.write(registerAgentRequest));

        this.operationService.handle(envelope);*/
    }


    @Test
    public void consumerTests() {
        /*RabbitMessageListenerContainer listenerContainer = new RabbitMessageListenerContainer(this.connectionFactory);
        System.out.println(listenerContainer);*/
    }
}

package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.Assert.*;

/**
 * @author Helena Edelson
 */
public class ConverterTests {

    private final JsonMappingConverter converter = new JsonMappingConverter();

    private final RegisterAgentRequest registerAgentRequest =
            new RegisterAgentRequest(null, "authTokenValue", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin", false);

    private final String agentIp = "localhost";

    private final int agentPort = 7071;

    @Test
    public void testConverterForListener() {
        Message message = this.converter.toMessage(registerAgentRequest, new MessageProperties());
        RegisterAgentRequest marshalled = (RegisterAgentRequest) this.converter.fromMessage(message);
        assertEquals(registerAgentRequest, marshalled);
    }

    @Test
    public void write() {
        assertEquals(this.converter.write(registerAgentRequest), "{\"agentToken\":null,\"authToken\":\"authTokenValue\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"unidirectional\":false,\"newTransportAgent\":false,\"operationName\":\"RegisterAgentRequest\"," +
                "\"ensureOrder\":true,\"byteaLists\":{},\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{},\"objectLists\":{}}");
    }

    @Test
    public void simpleConversionTest() {
        TestObject request = new TestObject("test-content");
        String json = this.converter.write(request);
        assertEquals("{\"content\":\"test-content\"}", json);
        Object simpleResponse = this.converter.read("{\"content\":\"test-content\"}", TestObject.class);
        assertEquals(request, simpleResponse);
    }

    @Test
    public void read() {
        Object response = this.converter.read(converter.write(registerAgentRequest), RegisterAgentRequest.class);
        assertNotNull(response);
        assertTrue(response instanceof RegisterAgentRequest);
    }

    @Test
    public void convertRegisterAgent() {
        String json = this.converter.write(registerAgentRequest);
        RegisterAgentRequest req = (RegisterAgentRequest) this.converter.read(json, RegisterAgentRequest.class);
        assertTrue(json.contains(req.getAuthToken()));
        RegisterAgentResponse response = (RegisterAgentResponse) this.converter.read(this.converter.write(new RegisterAgentResponse("agentTokenValue")), RegisterAgentResponse.class);
        assertNotNull(response);
    }
}

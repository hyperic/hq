package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Helena Edelson
 */
public class ConverterTests {

    private final JsonMappingConverter converter = new JsonMappingConverter();

    private final String agentIp = "localhost";

    private final int agentPort = 7071;

    
    @Test
    public void write() {
        String json = this.converter.write(new RegisterAgentRequest("", "testAuth", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin", false));

        assertEquals("{\"authToken\":\"testAuth\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"agentToken\":null,\"unidirectional\":false," +
                "\"newTransportAgent\":false,\"operationName\":\"RegisterAgent\",\"ensureOrder\":true,\"byteaLists\":{}," +
                "\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{},\"objectLists\":{}}", json);
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
        RegisterAgentRequest request = new RegisterAgentRequest("", "authTokenValue", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin", false);

        Object response = this.converter.read("{\"authToken\":\"authTokenValue\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"agentToken\":null,\"unidirectional\":false," +
                "\"newTransportAgent\":false,\"operationName\":\"RegisterAgent\",\"ensureOrder\":false,\"byteaLists\":{}," +
                "\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{}," +
                "\"objectLists\":{}}", RegisterAgentRequest.class);

        assertEquals(request,response);
    }

    @Test
    public void convertRegisterAgent() {
        String json = this.converter.write(new RegisterAgentRequest(null, "authTokenValue", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin", false));
        assertEquals(json,"{\"agentToken\":null,\"authToken\":\"authTokenValue\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"unidirectional\":false,\"newTransportAgent\":false,\"operationName\":\"RegisterAgentRequest\"," +
                "\"ensureOrder\":true,\"byteaLists\":{},\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{},\"objectLists\":{}}");
        RegisterAgentResponse response = (RegisterAgentResponse) this.converter.read(this.converter.write(new RegisterAgentResponse("agentTokenValue")), RegisterAgentResponse.class);
    }
}

package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.RegisterAgent;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.convert.SimpleConverter;
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
    public void convertString() {
        Converter<String, byte[]> converter = new SimpleConverter();
        String msg = "test message";
        byte[] bytes = converter.write(msg);
        String result = converter.read(bytes, Byte.class);
        assertEquals(msg, new String(bytes));
        assertEquals(result, msg);
        assertEquals(msg.getBytes().length, result.getBytes().length);
    }


    @Test
    public void write() {
        String json = this.converter.write(new RegisterAgent("testAuth", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin"));

        assertEquals("{\"authToken\":\"testAuth\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"agentToken\":null,\"unidirectional\":false," +
                "\"newTransportAgent\":false,\"operationName\":\"RegisterAgent\",\"ensureOrder\":true,\"byteaLists\":{}," +
                "\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{},\"objectLists\":{}}", json);
    }

    @Test
    public void writeSimple() {
        String json = this.converter.write(new TestObject("test-content"));
        assertEquals("{\"content\":\"test-content\"}", json);        
    }

    @Test
    public void read() {
        RegisterAgent request = new RegisterAgent("authTokenValue", "5.0", 1, this.agentIp, this.agentPort, "hqadmin", "hqadmin");

        Object response = this.converter.read("{\"authToken\":\"authTokenValue\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071," +
                "\"username\":\"hqadmin\",\"password\":\"hqadmin\",\"agentToken\":null,\"unidirectional\":false," +
                "\"newTransportAgent\":false,\"operationName\":\"RegisterAgent\",\"ensureOrder\":false,\"byteaLists\":{}," +
                "\"byteaVals\":{},\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{}," +
                "\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{},\"byteALists\":{}," +
                "\"objectLists\":{}}", RegisterAgent.class);
 
        assertEquals(request,response); 
    }

    @Test
    public void readSimple() {
        TestObject simpleRequest = new TestObject("test-content");
        Object simpleResponse = this.converter.read("{\"content\":\"test-content\"}", TestObject.class);
        assertEquals(simpleRequest, simpleResponse);
    }
}

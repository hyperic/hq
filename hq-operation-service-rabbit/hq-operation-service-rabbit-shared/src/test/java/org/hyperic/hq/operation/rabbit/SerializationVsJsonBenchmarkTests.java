package org.hyperic.hq.operation.rabbit;

import org.hyperic.hq.operation.AbstractOperation;
import org.hyperic.hq.operation.RegisterAgent;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Helena Edelson
 */
public class SerializationVsJsonBenchmarkTests {

    private JsonMappingConverter converter = new JsonMappingConverter();

    private AbstractOperation data;

    private String json;

    private long serialDurationTotal = 0;

    private long jsonDurationTotal = 0;

    private int executions = 100;

    @Before
    public void prepare() {
        this.data = new RegisterAgent("authTokenValue", "5.0", 1, "localhost", 7071, "hqadmin", "hqadmin");

        this.json = "{\"authToken\":\"authTokenValue\",\"version\":\"5.0\",\"cpuCount\":1,\"agentIp\":\"localhost\",\"agentPort\":7071,\"username\":\"hqadmin\",\"password\":\"hqadmin\"," +
                "\"agentToken\":null,\"unidirectional\":false,\"newTransportAgent\":false,\"operationName\":\"RegisterAgent\"," +
                "\"stringVals\":{},\"intVals\":{},\"doubleVals\":{},\"longVals\":{},\"byteAVals\":{},\"objectVals\":{},\"stringLists\":{},\"intLists\":{},\"doubleLists\":{}," +
                "\"byteALists\":{},\"objectLists\":{}}";
    }

    @Test
    public void test() {
        for (int i = 0; i < executions; i++) { 
            long startTime1 = System.nanoTime();
            Object serialResult = SerializationUtil.deserialize(SerializationUtil.serialize(data));
            long duration1 = System.nanoTime() - startTime1;
            this.serialDurationTotal += duration1;

            long startTime2 = System.nanoTime();
            Object jsonResult = this.converter.write(this.converter.read(json, RegisterAgent.class));
            long duration2 = System.nanoTime() - startTime2;
            this.jsonDurationTotal += duration2;
            System.out.println("serial=" + duration1 + " json=" + duration2);
        }

        long serAverage = this.serialDurationTotal / executions;
        long jsonAverage = this.jsonDurationTotal / executions;
        System.out.println("Average Serialization Time = " + serAverage);
        System.out.println("Average Json Time = " + jsonAverage);
    }

}

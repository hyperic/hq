package org.hyperic.hq.amqp;

import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * The hq server (lather) and a rabbit server MUST be running first. 
 * @author Helena Edelson
 */
@Ignore
public class BenchmarkTests extends BaseInfrastructureTest {

    private final static String AGENT_HOME = "/path/to/agent/home";

    private final String host = "localhost";

    private final int port = 7080;

    private final int executions = 1000;

    private BenchmarkClient benchmark;

    @Before
    public void doBefore() throws AgentConfigException {
        System.setProperty("agent.install.home", AGENT_HOME);
        System.setProperty("agent.bundle.home", AGENT_HOME + "/bin");
        benchmark = new BenchmarkClient(host, port);
    }

    @Test
    public void ping() throws Exception {
        long rabbitDurationTotal = 0;
        long latherDurationTotal = 0;

        for (int i = 0; i < executions; i++) {
            long startTime = System.currentTimeMillis();
            benchmark.rabbitPing(i);
            rabbitDurationTotal += (System.currentTimeMillis() - startTime);

            long startTime2 = System.currentTimeMillis();
            benchmark.latherPing();
            latherDurationTotal += (System.currentTimeMillis() - startTime2); 
        }

        results(rabbitDurationTotal, latherDurationTotal);
    }
 
    private void results(long rabbitDurationTotal, long latherDurationTotal) {
        System.out.println("Average Rabbit Execution Time = " + rabbitDurationTotal / executions);
        System.out.println("Average Lather Execution Time = " + latherDurationTotal / executions);
    }
}

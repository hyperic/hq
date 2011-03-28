package org.hyperic.hq.amqp.unit;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Helena Edelson
 */
public class RoutingLoadTests extends RoutingKeyTests {

    private final int currentAgentCount = 1000;

    private final int futureAgentCount = 10000;

    private final int futureServerCluster = 3;

    @Before
    public void doPrepare() {
        
    }

    @Test
    public void declareExchangesBindingsAndQueues() {

    }
}

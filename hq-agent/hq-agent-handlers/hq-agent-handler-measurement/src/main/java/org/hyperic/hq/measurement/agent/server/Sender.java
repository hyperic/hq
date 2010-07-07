package org.hyperic.hq.measurement.agent.server;

import org.hyperic.hq.product.MetricValue;

public interface Sender {

    public void processData(int dsnId, MetricValue data, int derivedID);

}

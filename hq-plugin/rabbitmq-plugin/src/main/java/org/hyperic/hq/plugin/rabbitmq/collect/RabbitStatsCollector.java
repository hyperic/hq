/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.collect;

import java.util.Map;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.hyperic.hq.plugin.rabbitmq.core.MessageStats;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitStatsObject;

/**
 *
 * @author administrator
 */
public abstract class RabbitStatsCollector extends RabbitMQDefaultCollector {

    public void collect(HypericRabbitAdmin rabbitAdmin) {
        RabbitStatsObject o = collectStats(rabbitAdmin);
        if (o != null) {
            processMessageStatsMetrics(o.getMessageStats());
        }
    }

    public abstract RabbitStatsObject collectStats(HypericRabbitAdmin rabbitAdmin);

    private void processMessageStatsMetrics(MessageStats stats) {
        if (stats != null) {
            setVal("publishDetails", stats.getPublishDetails());
            setVal("confirmDetails", stats.getConfirmDetails());
            setVal("deliverDetails", stats.getDeliverDetails());
            setVal("ackDetails", stats.getAckDetails());
            setVal("getDetails", stats.getGetDetails());
            setVal("getNoAckDetails", stats.getGetNoAckDetails());
            setVal("deliverNoAckDetails", stats.getDeliverNoAckDetails());
            setVal("deliverGetDetails", stats.getDeliverGetDetails());
        }
    }

    private void setVal(String string, Map<String, Double> detail) {
        if (detail != null) {
            setValue(string, detail.get("rate"));
        } else {
            setValue(string, 0);
        }
    }
}

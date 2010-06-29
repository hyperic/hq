package org.hyperic.hq.plugin.rabbitmq;

import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.objs.Queue;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

public class RabbitMQQueueCollector extends Collector{
    private static Log log = LogFactory.getLog(RabbitMQQueueCollector.class);

    @Override
    public void collect() {
        log.debug(getProperties());
        Properties props=getProperties();
        String queue=(String) props.get("queue");
        String vhost = (String) props.get("vhost");
        try {
            List<Queue> queues=RabbitMQUtils.getQueues((String) props.get("server"), vhost);
            for(Queue q:queues){
                if(q.getFullName().equals(vhost+queue)){
                    addValues(q.getProperties());
                    setAvailability(true);
                }
            }
        } catch (PluginException ex) {
            log.error(ex);
        }
    }
}

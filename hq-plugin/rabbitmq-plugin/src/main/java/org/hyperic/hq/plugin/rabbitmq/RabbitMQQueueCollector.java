/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.rabbitmq;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.objs.Queue;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
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
                }
            }
        } catch (PluginException ex) {
            log.error(ex);
        }
    }

}

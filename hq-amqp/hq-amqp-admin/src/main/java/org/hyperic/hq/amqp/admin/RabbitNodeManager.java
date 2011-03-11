package org.hyperic.hq.amqp.admin;

import org.apache.log4j.Logger;
import org.hyperic.hq.amqp.admin.erlang.BrokerStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Helena Edelson
 */
@Component
public class RabbitNodeManager {

    private static Logger logger = Logger.getLogger(RabbitNodeManager.class);

    private RabbitAdminTemplate amqpAdmin;

    @Autowired
    public RabbitNodeManager(RabbitAdminTemplate amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    public void prepare() {
        if (!running()) {
            /* TODO throw a custom exception */
            logger.error("There are no running nodes in the broker. Insure at least one node is running."); 
        }
    }

    /**
     * Tests the broker and nodes.
     * @return
     */
    private boolean running() {
        BrokerStatus status = amqpAdmin.getBrokerStatus();
        return status != null && status.getRunningNodes().size() > 0;
    }
}

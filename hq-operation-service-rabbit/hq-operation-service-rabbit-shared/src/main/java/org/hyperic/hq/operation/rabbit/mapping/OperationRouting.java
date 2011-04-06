package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.rabbit.util.RoutingType;

/**
 * @author Helena Edelson
 */
public class OperationRouting {

    private final RoutingType type;

    private final String exchangeName;

    private final String value;

    public OperationRouting(RoutingType type, String exchangeName, String value) {
        this.type = type;
        this.exchangeName = exchangeName;
        this.value = value;
    }

    public RoutingType getType() {
        return type;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getValue() {
        return value;
    }
}

package org.hyperic.hq.operation.rabbit.mapping;


/**
 * @author Helena Edelson
 */
public class OperationRouting {

    private final String exchangeName;

    private final String value;

    public OperationRouting(String exchangeName, String value) {
        this.exchangeName = exchangeName;
        this.value = value;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getValue() {
        return value;
    }
}

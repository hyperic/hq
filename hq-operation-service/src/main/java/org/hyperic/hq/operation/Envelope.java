package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Helena Edelson
 */
public final class Envelope implements OperationData {

    private final String operationName;

    private final String content;

    private final String correlationId;

    @JsonCreator
    public Envelope(@JsonProperty("operationName") String operationName, @JsonProperty("context") String content,
        @JsonProperty("correlationId") String correlationId) {
        this.operationName = operationName;
        this.content = content;
        this.correlationId = correlationId;
    }

    /**
     * Returns the name of the operation
     * @return the operation name
     */
    public String getOperationName() {
        return this.operationName;
    }

    /**
     * Returns the content of the operation
     * @return the operation content
     */
    public String getContent() {
        return this.content;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return new StringBuilder(" operationName=").append(this.operationName).append(" content=")
                .append(this.content).append(" correlationId=").append(this.correlationId).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.operationName == null ? 0 : this.operationName.hashCode());
        result = prime * result + (this.content == null ? 0 : this.content.hashCode());
        result = prime * result + (this.correlationId == null ? 0 : this.correlationId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        Envelope other = (Envelope) obj;
        if (this.operationName == null) {
            if (other.operationName != null) {
                return false;
            }
        } else if (!this.operationName.equals(other.operationName)) {
            return false;
        }
        if (this.content == null) {
            if (other.content != null) {
                return false;
            }
        } else if (!this.content.equals(other.content)) {
            return false;
        }
        if (this.correlationId == null) {
            if (other.correlationId != null) {
                return false;
            }
        } else if (!this.correlationId.equals(other.correlationId)) {
            return false;
        }
        return true;
    }
}

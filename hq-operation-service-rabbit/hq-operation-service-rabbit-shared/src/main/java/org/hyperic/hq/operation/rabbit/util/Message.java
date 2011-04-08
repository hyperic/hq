package org.hyperic.hq.operation.rabbit.util;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hyperic.hq.operation.Envelope;

/**
 * @author Helena Edelson
 */
public class Message implements Envelope {

    private final long operationId;

    private final String operationName;

    private final String context;

    private final String replyTo;

    @JsonCreator
    public Message(@JsonProperty("operationId") long operationId, @JsonProperty("operationName") String operationName,
                   @JsonProperty("context") String context, @JsonProperty("replyTo") String replyTo) {
        this.operationId = operationId;
        this.operationName = operationName;
        this.context = context;
        this.replyTo = replyTo;
    }

    /**
     * Returns the id of the operation
     * @return the operation id
     */
    public long getOperationId() {
        return this.operationId;
    }

    /**
     * Returns the name of the operation
     * @return the operation name
     */
    public String getOperationName() {
        return this.operationName;
    }

    /**
     * Returns the context of the operation
     * @return the operation context
     */
    public String getContext() {
        return this.context;
    }
 
    /**
     * Returns the destination that responses should be sent to
     * @return the destination for responses to the operation
     */
    public String getReplyTo() {
        return this.replyTo;
    }

    @Override
    public String toString() {
        return new StringBuilder("operationId=").append(this.operationId).append(" operationName=").append(this.operationName)
                .append(" context=").append(this.context).append(" responseExchange=").append(this.replyTo).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.context == null ? 0 : this.context.hashCode());
        result = prime * result + (int) (this.operationId ^ this.operationId >>> 32);
        result = prime * result + (this.operationName == null ? 0 : this.operationName.hashCode());
        result = prime * result + (this.replyTo == null ? 0 : this.replyTo.hashCode());
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

        Message other = (Message) obj;
        if (this.context == null) {
            if (other.context != null) {
                return false;
            }
        } else if (!this.context.equals(other.context)) {
            return false;
        }
        if (this.operationId != other.operationId) {
            return false;
        }
        if (this.operationName == null) {
            if (other.operationName != null) {
                return false;
            }
        } else if (!this.operationName.equals(other.operationName)) {
            return false;
        }
        if (this.replyTo == null) {
            if (other.replyTo != null) {
                return false;
            }
        } else if (!this.replyTo.equals(other.replyTo)) {
            return false;
        }
        return true;
    }
}

package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.lang.annotation.Annotation;

/**
 * @author Helena Edelson
 */
public final class Envelope implements OperationData {

    private final String operationName;

    private final String content;

    private final String replyTo;

    private final Class<? extends Annotation> type;

    @JsonCreator
    public Envelope(@JsonProperty("operationName") String operationName,
                   @JsonProperty("context") String content,
                   @JsonProperty("replyTo") String replyTo,
                   @JsonProperty("type") Class<? extends Annotation> type) {
        this.operationName = operationName;
        this.content = content;
        this.replyTo = replyTo;
        this.type = type;
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
 
    /**
     * Returns the destination that responses should be sent to
     * @return the destination for responses to the operation
     */
    public String getReplyTo() {
        return this.replyTo;
    }

    public Class<? extends Annotation> getType() {
        return type;
    }

    @Override
    public String toString() {
        return new StringBuilder(" operationName=").append(this.operationName).append(" content=")
                .append(this.content).append(" replyTo=").append(this.replyTo).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.content == null ? 0 : this.content.hashCode());
        //result = prime * result + (int) (this.operationId ^ this.operationId >>> 32);
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

        Envelope other = (Envelope) obj;
        if (this.content == null) {
            if (other.content != null) {
                return false;
            }
        } else if (!this.content.equals(other.content)) {
            return false;
        }
        /*if (this.operationId != other.operationId) {
            return false;
        }*/
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

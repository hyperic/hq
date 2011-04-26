package org.hyperic.hq.operation;

import java.io.Serializable;


public abstract class AbstractOperation implements OperationData, Serializable {

    private static final long serialVersionUID = 6991306796752066389L;
 
    private String operationName = this.getClass().getSimpleName();
 
    @Override
    public String toString() {
        return operationName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.toString() == null ? 0 : this.toString().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            AbstractOperation other = (AbstractOperation) obj;
            if (this.toString() != null && other.toString() != null) {
                return this.toString().equals(other.toString());
            }
        }
        return false;
    }

    public String getOperationName() {
        return operationName;
    }
 
}

package org.hyperic.hq.operation.rabbit.shared;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.Ignore;

import java.io.Serializable;

/**
 * @author Helena Edelson
 */
@Ignore
public class TestObject implements Serializable {

    private static final long serialVersionUID = 6991306796752066389L;
    
    private volatile String content;

    @JsonCreator
    public TestObject(@JsonProperty("content") String content) {
        this.content = content;
    }
    
    @SuppressWarnings("unused")
    public String getContent() {
        return this.content;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.content == null ? 0 : this.content.hashCode());
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
        TestObject other = (TestObject) obj;
        if (this.content == null) {
            if (other.content != null) {
                return false;
            }
        } else if (!this.content.equals(other.content)) {
            return false;
        }
        return true;
    }


}

package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Helena Edelson
 */
public class RegisterAgent extends AbstractAgentOperation {

    private String authToken;

    private String version;

    private int cpuCount;

    @JsonCreator
    public RegisterAgent(@JsonProperty("authToken") String authToken, @JsonProperty("version") String version,
                         @JsonProperty("cpuCount") int cpuCount, @JsonProperty("agentIp") String agentIp,
                         @JsonProperty("agentPort") int agentPort, @JsonProperty("username") String username,
                         @JsonProperty("password") String password) {

        super(username, password, agentIp, agentPort);
        this.authToken = authToken;
        this.version = version;
        this.cpuCount = cpuCount;
    }


    public String getAuthToken() {
        return authToken;
    }

    public String getVersion() {
        return version;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    @JsonIgnore
    public boolean isValid() {
        return this.authToken != null && this.version != null && this.cpuCount > 0;
    }

    @Override
    public String toString() {
        return super.toString() + this.authToken + this.version + this.cpuCount + this.getAgentIp() + this.getAgentPort() + this.getUsername() + this.getPassword();
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RegisterAgent other = (RegisterAgent) obj;
        if (this.toString() == null) {
            if (other.toString() != null) {
                return false;
            }
        } else if (!this.toString().equals(other.toString())) {
            return false;
        }
        return true;
    }

}

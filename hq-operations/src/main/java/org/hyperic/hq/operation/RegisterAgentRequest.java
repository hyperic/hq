package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Helena Edelson
 */
public class RegisterAgentRequest extends AbstractAgentOperation {

    private String authToken;

    private String version;

    private int cpuCount;

    @JsonCreator
    public RegisterAgentRequest(@JsonProperty("agentToken") String agentToken, @JsonProperty("authToken") String authToken, @JsonProperty("version") String version,
                         @JsonProperty("cpuCount") int cpuCount, @JsonProperty("agentIp") String agentIp,
                         @JsonProperty("agentPort") int agentPort, @JsonProperty("username") String username,
                         @JsonProperty("password") String password, @JsonProperty("unidirectional") boolean unidirectional) {
     
        super(agentToken, username, password, agentIp, agentPort, unidirectional, false);
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

    @Override
    public String toString() {
        return new StringBuilder(authToken).append(version).append(cpuCount).append(super.toString()).toString();
    } 
}

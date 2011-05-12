package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;


public class AbstractAgentOperation {

    private String agentToken;

    private String username;

    private String password;

    private String agentIp;

    private int agentPort;

    @JsonIgnore
    private boolean unidirectional;

    @JsonIgnore
    private boolean newTransportAgent;

    @JsonCreator
    public AbstractAgentOperation(@JsonProperty("agentToken") String agentToken, @JsonProperty("username") String username,
                                  @JsonProperty("password") String password, @JsonProperty("agentIp") String agentIp,
                                  @JsonProperty("agentPort") int agentPort, @JsonProperty("unidirectional") boolean unidirectional,
                                  @JsonProperty("newTransportAgent") boolean newTransportAgent) {

        this.agentToken = agentToken;
        this.username = username;
        this.password = password;
        this.agentIp = agentIp;
        this.agentPort = agentPort;
        this.unidirectional = unidirectional;
        this.newTransportAgent = newTransportAgent;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAgentIp() {
        return agentIp;
    }

    public String getAgentToken() {
        return agentToken;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public boolean isUnidirectional() {
        return unidirectional;
    }

    public boolean isNewTransportAgent() {
        return newTransportAgent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.username).append(this.password)
                .append(this.agentIp).append(this.agentPort).append(this.unidirectional).append(super.toString());

        return agentToken != null ? new StringBuilder(this.agentToken).append(sb).toString() : sb.toString();
    } 
}

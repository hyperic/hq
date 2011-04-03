package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;


public class AbstractAgentOperation extends AbstractOperation {

    @JsonIgnore
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
    public AbstractAgentOperation(@JsonProperty("username") String username,
                         @JsonProperty("password") String password, @JsonProperty("agentIp") String agentIp,
                         @JsonProperty("agentPort") int agentPort) {

        this.username = username;
        this.password = password;
        this.agentIp = agentIp;
        this.agentPort = agentPort;
    }

    @JsonCreator
    public AbstractAgentOperation(@JsonProperty("agentToken") String agentToken, @JsonProperty("username") String username,
                         @JsonProperty("password") String password, @JsonProperty("agentIp") String agentIp,
                         @JsonProperty("agentPort") int agentPort, @JsonProperty("unidirectional") boolean unidirectional,
                         @JsonProperty("newTransportAgent") boolean newTransportAgent) {

        if (agentToken == null) throw new IllegalStateException("'agentToken' must not be null.");
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
        return this.agentToken + this.username + this.password + this.agentIp + this.agentPort;
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
        AbstractAgentOperation other = (AbstractAgentOperation) obj;
        if (this.toString() == null) {
            if (other.toString() != null) {
                return false;
            }
        } else if (!this.toString().equals(other.toString())) {
            return false;
        }
        return true;
    }

    /*    public void setNewTransportAgent(boolean unidirectional) {
        this.setStringValue(PROP_NEWTRANSPORTTYPE, Boolean.TRUE.toString());
        this.setStringValue(PROP_UNIDIRECTIONAL, String.valueOf(unidirectional));
    }

    public boolean isUnidirectional() {
        boolean unidirectional = false;

        try {
            unidirectional = Boolean.valueOf(getStringValue(PROP_UNIDIRECTIONAL)).booleanValue();
        } catch (Exception e) {
            // this is an older agent that does not support the unidirectional transport
        }

        return unidirectional;
    }

    public boolean isNewTransportAgent() {
        boolean newTransportAgent = false;

        try {
            newTransportAgent = Boolean.valueOf(getStringValue(PROP_NEWTRANSPORTTYPE)).booleanValue();
        } catch (Exception e) {
            // this is an older agent that does not support the new transport
        }

        return newTransportAgent;
    }

    public void setUser(String user) {
        this.setStringValue(PROP_USER, user);
    }

    public String getUser() {
        return this.getStringValue(PROP_USER);
    }

    public void setPword(String pword) {
        this.setStringValue(PROP_PWORD, pword);
    }

    public String getPword() {
        return this.getStringValue(PROP_PWORD);
    }

    public void setAgentIP(String agentIP) {
        this.setStringValue(PROP_AGENTIP, agentIP);
    }

    public String getAgentIP() {
        return this.getStringValue(PROP_AGENTIP);
    }

    public void setAgentPort(int agentPort) {
        this.setIntValue(PROP_AGENTPORT, agentPort);
    }

    public int getAgentPort() {
        return this.getIntValue(PROP_AGENTPORT);
    }

    public void setAgentToken(String agentToken) {
        this.setStringValue(PROP_AGENTTOKEN, agentToken);
    }

    public String getAgentToken() {
        return this.getStringValue(PROP_AGENTTOKEN);
    }*/

    public void validate() throws Exception {
        try {
            this.getUsername();
            this.getPassword();
            this.getAgentIp();
            this.getAgentPort();
        } catch (Exception exc) {
           // throw new LatherRemoteException("All values not set");
        }
    }
}

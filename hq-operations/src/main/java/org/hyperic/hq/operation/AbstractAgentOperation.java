package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;


public class AbstractAgentOperation extends AbstractOperation {

    private static final long serialVersionUID = -7404476740286545689L;

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

        super(true);
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
        super(true);
        if (agentToken == null) throw new IllegalArgumentException("'agentToken' must not be null.");
        
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
                .append(this.agentIp).append(this.agentPort).append(super.toString());
         
        return agentToken != null ? new StringBuilder(this.agentToken).append(sb).toString() : sb.toString();
    }

    /* Everything from here down is legacy and should be re-evaluated */

 
   /*public void setNewTransportAgent(boolean unidirectional) {
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

    /**
     * TODO better port test
     * @throws IllegalStateException
     */
    @JsonIgnore
    public void validate() throws IllegalStateException {
        if (username == null || password == null || agentIp == null || agentPort < 1) {
            throw new IllegalStateException(this + " is not properly initialized: " + this.toString());
        }
    }
}

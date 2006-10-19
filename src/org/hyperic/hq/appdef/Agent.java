package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentValue;

import java.util.Collection;

/**
 *
 */
public class Agent extends AppdefBean
{
    private String address;
    private Integer port;
    private String authToken;
    private String agentToken;
    private String version;
    private AgentType agentType;
    private Collection platforms;

    /**
     * default constructor
     */
    public Agent()
    {
        super();
    }

    public String getAddress()
    {
        return this.address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public Integer getPort()
    {
        return this.port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getAuthToken()
    {
        return this.authToken;
    }

    public void setAuthToken(String authToken)
    {
        this.authToken = authToken;
    }

    public String getAgentToken()
    {
        return this.agentToken;
    }

    public void setAgentToken(String agentToken)
    {
        this.agentToken = agentToken;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public AgentType getAgentType()
    {
        return this.agentType;
    }

    public void setAgentType(AgentType agentType)
    {
        this.agentType = agentType;
    }

    public Collection getPlatforms()
    {
        return this.platforms;
    }

    public void setPlatforms(Collection platforms)
    {
        this.platforms = platforms;
    }

    // TODO: fix the equals and hashCode
    
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof Agent)) return false;
        Agent castOther = (Agent) other;

        return ((this.getAddress() == castOther.getAddress()) || (this.getAddress() != null && castOther.getAddress() != null && this.getAddress().equals(castOther.getAddress())))
               && ((this.getPort() == castOther.getPort()) || (this.getPort() != null && castOther.getPort() != null && this.getPort().equals(castOther.getPort())));
    }

    public int hashCode()
    {
        int result = 17;

        return result;
    }

    private AgentValue agentValue = new AgentValue();
    /**
     * legacy EJB code to get DTO (Value) object
     * @deprecated use (this) Agent object instead
     * @return
     */
    public AgentValue getAgentValue()
    {
        agentValue.setAddress(
            (getAddress() == null) ? "" : getAddress());
        agentValue.setPort(getPort().intValue());
        agentValue.setAuthToken(
            (getAuthToken() == null) ? "" : getAuthToken());
        agentValue.setAgentToken(
            (getAgentToken() == null) ? "" : getAgentToken());
        agentValue.setVersion(
            (getVersion() == null) ? "" : getVersion());
        agentValue.setId(getId());
        agentValue.setMTime(getMTime());
        agentValue.setCTime(getCTime());
        if ( getAgentType() != null )
            agentValue.setAgentType( getAgentType().getAgentTypeValue() );
        else
            agentValue.setAgentType( null );
        return agentValue;
    }

    private AgentPK _pkey = new AgentPK();
    /**
     * legacy EJB primary key accessor
     * @return
     * @deprecated
     */
    public AgentPK getPrimaryKey()
    {
        _pkey.setId(getId());
        return _pkey;
    }
}

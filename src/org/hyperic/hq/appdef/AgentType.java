package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.AgentTypeValue;

import java.util.Collection;

/**
 *
 */
public class AgentType extends AppdefBean
{
    private String name;
    private String sortName;
    private Collection agents;

    /**
     * default constructor
     */
    public AgentType()
    {
        super();
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        this.sortName = sortName;
    }

    public Collection getAgents()
    {
        return this.agents;
    }

    public void setAgents(Collection agents)
    {
        this.agents = agents;
    }

    private AgentTypeValue agentTypeValue = new AgentTypeValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) AgentType object instead
     */
    public AgentTypeValue getAgentTypeValue()
    {
        agentTypeValue.setName(getName());
        agentTypeValue.setSortName(getSortName());
        agentTypeValue.setId(getId());
        agentTypeValue.setMTime(getMTime());
        agentTypeValue.setCTime(getCTime());
        return agentTypeValue;
    }
}

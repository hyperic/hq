package org.hyperic.hq.appdef;

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
}

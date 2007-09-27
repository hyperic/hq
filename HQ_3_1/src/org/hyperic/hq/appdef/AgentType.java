/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

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
        setSortName(name);
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        this.sortName = sortName != null ? sortName.toUpperCase() : null;
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

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AgentType) || !super.equals(obj)) {
            return false;
        }
        AgentType o = (AgentType)obj;
        return (name == o.getName() || (name!=null && o.getName()!=null &&
                                        name.equals(o.getName())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (name!=null ? name.hashCode() : 0);

        return result;
    }
}

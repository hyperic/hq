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

import java.util.Collection;

public class AgentType extends AppdefBean
{
    private static final Integer TYPE_LEGACY_TRANSPORT = new Integer(1);
    private static final Integer TYPE_NEW_TRANSPORT = new Integer(2);
    
    
    private String _name;
    private Collection _agents;

    public AgentType() {
        super();
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }
    
    public boolean isNewTransportType() {
        Integer id = getId();
        
        if (id != null) {
            return id.equals(TYPE_NEW_TRANSPORT);
        }
        
        return false;
    }

    public Collection getAgents() {
        return _agents;
    }

    public void setAgents(Collection agents) {
        _agents = agents;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AgentType) || !super.equals(obj)) {
            return false;
        }
        AgentType o = (AgentType)obj;
        return (_name == o.getName() || (_name != null && o.getName() != null &&
                                         _name.equals(o.getName())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_name != null ? _name.hashCode() : 0);

        return result;
    }
}

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

package org.hyperic.hq.dao;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.shared.AgentTypeValue;

public class AgentTypeDAO extends HibernateDAO
{
    public AgentTypeDAO(DAOFactory f) {
        super(AgentType.class, f);
    }

    public AgentType findById(Integer id)
    {
        return (AgentType)super.findById(id);
    }

    public void save(AgentType entity)
    {
        super.save(entity);
    }

    public void remove(AgentType entity)
    {
        super.remove(entity);
    }

    public AgentType create(AgentTypeValue atv)
    {
        AgentType a = new AgentType();
        a.setName(atv.getName());
        save(a);
        return a;
    }

    public AgentType findByName(String name)
    {
        String sql = "from AgentType where lower(name)=?";
        return (AgentType)getSession().createQuery(sql)
            .setString(0, name.toLowerCase())
            .uniqueResult();
    }
}

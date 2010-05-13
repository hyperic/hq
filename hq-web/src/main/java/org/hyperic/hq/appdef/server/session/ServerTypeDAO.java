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

package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.type.StringType;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 */
@Repository
public class ServerTypeDAO
    extends HibernateDAO<ServerType> {
    @Autowired
    public ServerTypeDAO(SessionFactory f) {
        super(ServerType.class, f);
    }

    public void remove(ServerType entity) {
        // Remove self from PlatformType
        for (Iterator it = entity.getPlatformTypes().iterator(); it.hasNext();) {
            PlatformType platType = (PlatformType) it.next();
            platType.getServerTypesBag().remove(entity);
        }

        super.remove(entity);
    }

    public ServerType create(ServerType serverType) {
        save(serverType);
        return serverType;
    }
    
    /**
     * @param names {@link Collection} of {@link String}
     * @return {@link List} of {@link ServerType}
     */
    @SuppressWarnings("unchecked")
    public List<ServerType> findByName(Collection<String> names) {
        String sql = "from ServerType where name in (:names)";
        return getSession().createQuery(sql)
            .setParameterList("names", names, new StringType())
            .list();
    }

    public ServerType findByName(String name) {
        String sql = "from ServerType where sortName=?";
        return (ServerType) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public ServerType findByNameAndPlugin(String name, String plugin) {
        String sql = "from ServerType where sortName=? and plugin=?";
        return (ServerType) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .setString(1, plugin).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<ServerType> findByPlugin(String plugin) {
        String sql = "from ServerType where plugin=?";
        return getSession().createQuery(sql).setString(0, plugin).list();
    }
}

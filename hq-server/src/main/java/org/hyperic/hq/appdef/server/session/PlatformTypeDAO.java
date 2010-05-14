/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.type.StringType;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformTypeDAO
    extends HibernateDAO<PlatformType> {
    @Autowired
    public PlatformTypeDAO(SessionFactory f) {
        super(PlatformType.class, f);
    }

    public PlatformType create(String name, String plugin) {
        PlatformType pt = new PlatformType(name, plugin);
        save(pt);
        return pt;
    }
    
    /**
     * @param names {@link Collection} of {@link String}
     * @return {@link List} of {@link PlatformType}
     */
    @SuppressWarnings("unchecked")
    public List<AppdefResourceType> findByName(Collection<String> names) {
        String sql = "from PlatformType where name in (:names)";
        return getSession().createQuery(sql)
            .setParameterList("names", names, new StringType())
            .list();
    }

    public PlatformType findByName(String name) {
        String sql = "from PlatformType where name=?";
        return (PlatformType) getSession().createQuery(sql).setString(0, name).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<PlatformType> findByPlugin(String plugin) {
        String sql = "from PlatformType where plugin=?";
        return getSession().createQuery(sql).setString(0, plugin).list();
    }
}

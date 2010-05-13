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
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceTypeDAO
    extends HibernateDAO<ServiceType> {
    private PlatformDAO platformDAO;

    @Autowired
    public ServiceTypeDAO(SessionFactory f, PlatformDAO platformDAO) {
        super(ServiceType.class, f);
        this.platformDAO = platformDAO;
    }

    public void remove(ServiceType entity) {
        // Remove self from ServerType
        entity.getServerType().getServiceTypes().remove(entity);
        super.remove(entity);
    }

    ServiceType create(String name, String plugin, String description, boolean internal) {
        ServiceType st = new ServiceType();
        st.setName(name);
        st.setPlugin(plugin);
        st.setDescription(description);
        st.setIsInternal(internal);

        save(st);
        return st;
    }
    
    /**
     * @param names {@link Collection} of {@link String}
     * @return {@link List} of {@link ServiceType}
     */
    @SuppressWarnings("unchecked")
    public List<ServiceType> findByName(Collection<String> names) {
        String sql = "from ServiceType where name in (:names)";
        return getSession().createQuery(sql)
            .setParameterList("names", names, new StringType())
            .list();
    }

    public ServiceType findByName(String name) {
        String sql = "from ServiceType where sortName=?";
        return (ServiceType) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .setCacheable(true).setCacheRegion("ServiceType.findByName").uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<ServiceType> findByPlugin(String plugin) {
        return createCriteria().add(Restrictions.eq("plugin", plugin)).addOrder(
            Order.asc("sortName")).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ServiceType> findByServerType_orderName(int serverType, boolean asc) {
        String sql = "from ServiceType where serverType.id=? " + "order by sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, serverType).list();
    }

    @SuppressWarnings("unchecked")
    public Collection<ServiceType> findVirtualServiceTypesByPlatform(int platformId) {
        // First get the platform
        Platform platform = platformDAO.findById(new Integer(platformId));

        return createCriteria().createAlias("serverType", "svt").createAlias("svt.platformTypes",
            "pt").add(Restrictions.eq("svt.virtual", Boolean.TRUE)).add(
            Restrictions.eq("pt.id", platform.getPlatformType().getId())).addOrder(
            Order.asc("sortName")).list();
    }
}

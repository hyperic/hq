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

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class ServiceTypeDAO extends HibernateDAO
{
    public ServiceTypeDAO(DAOFactory f) {
        super(ServiceType.class, f);
    }

    public ServiceType findById(Integer id) {
        return (ServiceType) super.findById(id);
    }

    void save(ServiceType entity) {
        super.save(entity);
    }

    void remove(ServiceType entity) {
        // Remove self from ServerType
        entity.getServerType().getServiceTypes().remove(entity);
        
        super.remove(entity);        
    }

    ServiceType create(String name, String plugin, String description,
                       boolean internal)
    {
        ServiceType st = new ServiceType();
        st.setName(name);
        st.setPlugin(plugin);
        st.setDescription(description);
        st.setIsInternal(internal);
        
        save(st);
        return st;
    }
    
    public ServiceType findByName(String name) {
        String sql="from ServiceType where sortName=?";
        return (ServiceType)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .setCacheable(true)
            .setCacheRegion("ServiceType.findByName")
            .uniqueResult();
    }

    public Collection findByPlugin(String plugin) {
        return createCriteria()
            .add(Restrictions.eq("plugin", plugin))
            .addOrder(Order.asc("sortName"))
            .list();
    }

    public Collection findByServerType_orderName(int serverType, boolean asc) {
        String sql="from ServiceType where serverType.id=? " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, serverType)
            .list();
    }

    public Collection findVirtualServiceTypesByPlatform(int platformId) {
        // First get the platform
        Platform platform =
            DAOFactory.getDAOFactory().getPlatformDAO().findById(
                new Integer(platformId));
        
        return createCriteria()
            .createAlias("serverType", "svt")
            .createAlias("svt.platformTypes", "pt")
            .add(Restrictions.eq("svt.virtual", Boolean.TRUE))
            .add(Restrictions.eq("pt.id", platform.getPlatformType().getId()))
            .addOrder(Order.asc("sortName"))
            .list();
    }
}

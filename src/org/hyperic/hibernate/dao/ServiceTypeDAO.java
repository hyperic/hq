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

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;

import java.util.Collection;


/**
 * Pojo for hibernate hbm mapping file
 * TODO: fix equals and hashCode
 */
public class ServiceTypeDAO extends HibernateDAO
{
    public ServiceTypeDAO(Session session)
    {
        super(ServiceType.class, session);
    }

    public ServiceType findById(Integer id)
    {
        return (ServiceType)super.findById(id);
    }

    public void evict(ServiceType entity)
    {
        super.evict(entity);
    }

    public ServiceType merge(ServiceType entity)
    {
        return (ServiceType)super.merge(entity);
    }

    public void save(ServiceType entity)
    {
        super.save(entity);
    }

    public void remove(ServiceType entity)
    {
        super.remove(entity);
    }

    private ServiceType createServiceType(ServiceTypeValue stv)
    {
        ServiceType st = new ServiceType();
        st.setName(stv.getName());
        st.setDescription(stv.getDescription());
        st.setIsInternal(stv.getIsInternal());
        st.setPlugin(stv.getPlugin());
        return st;
    }

    public ServiceType create(ServiceTypeValue stv)
    {
        ServiceType st = createServiceType(stv);
        save(st);
        return st;
    }

    public ServiceType create(ServiceType st)
    {
        save(st);
        return st;
    }
    /**
     * Create a service type for this server type
     */
    public ServiceType createServiceType(ServerType srvtp, ServiceTypeValue stv)
    {
        // first create the service type
        ServiceType st = createServiceType(stv);
        // now set the server type to this
        st.setServerType(srvtp);
        save(st);
        return st;
    }

    public ServiceType findByName(String name)
    {
        String sql="from ServiceType where sortName=?";
        return (ServiceType)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public Collection findByPlugin(String plugin)
    {
        String sql="from ServiceType where plugin=?";
        return getSession().createQuery(sql)
            .setString(0, plugin)
            .list();
    }

    /**
     * @deprecated use findByServerType_orderName() instead
     */
    public Collection findByServerType_orderName_asc(int serverType)
    {
        return findByServerType_orderName(serverType, true);
    }

    public Collection findByServerType_orderName(int serverType, boolean asc)
    {
        String sql="from ServiceType where serverType.id=? " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, serverType)
            .list();
    }

    public Collection findVirtualServiceTypesByPlatform(int platformId)
    {
        String sql="select st from ServiceType st " +
                   " join fetch st.serverType svt " +
                   " join fetch svt.servers sv " +
                   "where svt.virtual = true and " +
                   "      sv.platform.id=? " +
                   "order by st.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, platformId)
            .list();
    }

}

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.ServiceType;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypePK;

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

    public ServiceType create(ServiceTypeValue stv)
    {
        ServiceType st = new ServiceType();
        st.setName(stv.getName());
        st.setDescription(stv.getDescription());
        st.setIsInternal(stv.getIsInternal());
        st.setPlugin(stv.getPlugin());
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
        String sql="select s from ServiceType st " +
                   " join fetch st.serverType svt " +
                   "where svt.virtual = true and " +
                   "      svt.platform.id=? " +
                   "order by st.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, platformId)
            .list();
    }

    /**
     * legacy EJB primary key finder
     * @deprecated use findById() instead/
     * @param pk
     * @return
     */
    public ServiceType findByPrimaryKey(ServiceTypePK pk)
    {
        return findById(pk.getId());
    }

}

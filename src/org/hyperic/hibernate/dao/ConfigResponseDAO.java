package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ConfigResponsePK;

/**
 * CRUD methods, finders, etc. for ConfigResponseDAO
 */
public class ConfigResponseDAO extends HibernateDAO implements IConfigResponseDAO
{
    public ConfigResponseDAO(Session session)
    {
        super(ConfigResponseDB.class, session);
    }

    public ConfigResponseDB findById(Integer id)
    {
        return (ConfigResponseDB)super.findById(id);
    }

    public void evict(ConfigResponseDB entity)
    {
        super.evict(entity);
    }

    public ConfigResponseDB merge(ConfigResponseDB entity)
    {
        return (ConfigResponseDB)super.merge(entity);
    }

    public void save(ConfigResponseDB entity)
    {
        super.save(entity);
    }

    public void remove(ConfigResponseDB entity)
    {
        super.remove(entity);
    }

    public ConfigResponseDB findByPlatformId(Integer id)
    {
        String sql = "select c from ConfigResponseDB c, Platform p where " +
                     "c.id = p.configResponse.id and " +
                     "p.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .uniqueResult();
    }

    public ConfigResponseDB findByServerId(Integer id)
    {
        String sql = "select c from ConfigResponseDB c, Server s where " +
                     "c.id = s.configResponse.id and " +
                     "s.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .uniqueResult();
    }

    public ConfigResponseDB findByServiceId(Integer id)
    {
        String sql = "select c from ConfigResponseDB c, Service s where " +
                     "c.id = s.configResponse.id and " +
                     "s.id = ?";
        return (ConfigResponseDB)getSession()
            .createQuery(sql)
            .setInteger(0, id.intValue())
            .uniqueResult();
    }

    /**
     * for legacy EJB Entity Bean access compatibility
     * @param pk
     * @return
     */
    public ConfigResponseDB findByPrimaryKey(ConfigResponsePK pk)
    {
        return findById(pk.getId());
    }
}

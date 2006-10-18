package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.ServerType;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypePK;

import java.util.Collection;

/**
 *
 */
public class ServerTypeDAO extends HibernateDAO
{
    public ServerTypeDAO(Session session)
    {
        super(ServerType.class, session);
    }

    public ServerType findById(Integer id)
    {
        return (ServerType)super.findById(id);
    }

    public void evict(ServerType entity)
    {
        super.evict(entity);
    }

    public ServerType merge(ServerType entity)
    {
        return (ServerType)super.merge(entity);
    }

    public void save(ServerType entity)
    {
        super.save(entity);
    }

    public void remove(ServerType entity)
    {
        super.remove(entity);
    }

    public ServerType create(ServerTypeValue serverType)
    {
        ServerType st = new ServerType();
        st.setName(serverType.getName());
        st.setDescription(serverType.getDescription());
        st.setPlugin(serverType.getPlugin());
        st.setVirtual(serverType.getVirtual());
        save(st);
        return st;
    }

    public ServerType create(ServerType serverType)
    {
        save(serverType);
        return serverType;
    }

    public ServerType findByName(String name)
    {
        String sql="from ServerType where sortName=?";
        return (ServerType)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .uniqueResult();
    }

    public ServerType findByNameAndPlugin(String name, String plugin)
    {
        String sql="from ServerType where sortName=? and plugin=?";
        return (ServerType)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .setString(1, plugin)
            .uniqueResult();
    }

    public Collection findByPlugin(String plugin)
    {
        String sql="from ServerType where plugin=?";
        return getSession().createQuery(sql)
            .setString(0, plugin)
            .list();
    }

    public ServerType findByPrimaryKey(ServerTypePK pk)
    {
        return findById(pk.getId());
    }
}

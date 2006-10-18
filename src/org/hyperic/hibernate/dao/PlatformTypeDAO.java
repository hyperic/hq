package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.PlatformType;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;

import java.util.Collection;

/**
 *
 */
public class PlatformTypeDAO extends HibernateDAO
{
    public PlatformTypeDAO(Session session)
    {
        super(PlatformType.class, session);
    }

    public PlatformType findById(Integer id)
    {
        return (PlatformType)super.findById(id);
    }

    public void evict(PlatformType entity)
    {
        super.evict(entity);
    }

    public PlatformType merge(PlatformType entity)
    {
        return (PlatformType)super.merge(entity);
    }

    public void save(PlatformType entity)
    {
        super.save(entity);
    }

    public void remove(PlatformType entity)
    {
        super.remove(entity);
    }

    public PlatformType create(PlatformTypeValue pvalue)
    {
        PlatformType pt = new PlatformType();
        pt.setName(pvalue.getName());
        pt.setPlugin(pvalue.getPlugin());
        save(pt);
        return pt;
    }
    
    public PlatformType findByName(String name)
    {
        String sql = "from PlatformType where name=?";
        return (PlatformType)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    public Collection findByPlugin(String plugin)
    {
        String sql = "from PlatformType where plugin=?";
        return getSession().createQuery(sql)
            .setString(0, plugin)
            .list();
    }

    public PlatformType findByPrimaryKey(PlatformTypePK pk)
    {
        return findById(pk.getId());
    }
}

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.CpropKey;
import org.hyperic.hq.appdef.shared.CPropKeyPK;

import java.util.Collection;

/**
 * CRUD methods, finders, etc. for CpropKey
 */
public class CpropKeyDAO extends HibernateDAO implements ICPropKeyDAO
{
    public CpropKeyDAO(Session session)
    {
        super(CpropKey.class, session);
    }

    protected CpropKey findById(Integer id)
    {
        return (CpropKey)super.findById(id);
    }

    public void evict(CpropKey entity)
    {
        super.evict(entity);
    }

    public CpropKey merge(CpropKey entity)
    {
        return (CpropKey)super.merge(entity);
    }

    public void save(CpropKey entity)
    {
        super.save(entity);
    }

    public void remove(CpropKey entity)
    {
        super.remove(entity);
    }

    public CpropKey create(int appdefType, int appdefTypeId, String key, String description)
    {
        CpropKey cpropkey = new CpropKey();
        cpropkey.setAppdefType(new Integer(appdefType));
        cpropkey.setAppdefTypeId(new Integer(appdefTypeId));
        cpropkey.setKey(key);
        cpropkey.setDescription(description);
        save(cpropkey);
        return cpropkey;
    }

    public Collection findByAppdefType(int appdefType, int appdefId)
    {
        String sql = "from CpropKey k where k.appdefType = ? and k.appdefTypeId = ?";
        return getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefId)
            .list();
    }

    public CpropKey findByKey(int appdefType, int appdefTypeId, String key)
    {
        String sql = "from CpropKey k where k.appdefType=? and k.appdefTypeId=? and k.key=?";
        return (CpropKey)getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefTypeId)
            .setString(2, key)
            .uniqueResult();
    }

    /**
     * for legacy EJB Entity Bean compatibility.
     * @param pk
     * @return
     */
    public CpropKey findByPrimaryKey(CPropKeyPK pk)
    {
        return findById(pk.getId());
    }
}

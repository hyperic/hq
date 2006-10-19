package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ConfigResponsePK;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

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

    /**
     * @return newly instantiated config response object
     */
    public ConfigResponseDB create()
    {
        ConfigResponseDB newConfig = new ConfigResponseDB();
        save(newConfig);
        return newConfig;
    }

    /**
     * Initialize the config response for a new platform
     */
    public ConfigResponseDB createPlatform()
    {
        ConfigResponseDB cLocal = new ConfigResponseDB();
        try {
            ConfigResponse metricCfg = new ConfigResponse();
            ConfigResponse productCfg = new ConfigResponse();
            cLocal.setProductResponse(productCfg.encode());
            cLocal.setMeasurementResponse(metricCfg.encode());
            save(cLocal);
        } catch (EncodingException e) {
            // will never happen, we're setting up an empty response
		}
        return cLocal;
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

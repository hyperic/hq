package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Server;
import org.hyperic.hq.appdef.Platform;
import org.hyperic.hq.appdef.ServerType;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Service;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.dao.DAOFactory;

import java.util.Collection;
import java.util.List;

/**
 * CRUD methods, finders, etc. for Server
 */
public class ServerDAO extends HibernateDAO
{
    public ServerDAO(Session session)
    {
        super(Server.class, session);
    }

    public Server findById(Integer id)
    {
        return (Server)super.findById(id);
    }

    public void evict(Server entity)
    {
        super.evict(entity);
    }

    public Server merge(Server entity)
    {
        return (Server)super.merge(entity);
    }

    public void save(Server entity)
    {
        super.save(entity);
    }

    public void remove(Server entity)
    {
        super.remove(entity);
    }

    public Server create(ServerValue sv)
    {
        ConfigResponseDAO dao = DAOFactory.getDAOFactory()
            .getConfigResponseDAO();
        ConfigResponseDB configResponse = new ConfigResponseDB();
        dao.save(configResponse);

        Server s = new Server();
        s.setName(sv.getName());
        s.setDescription(sv.getDescription());
        s.setInstallPath(sv.getInstallPath());
        s.setAutoinventoryIdentifier(sv.getAutoinventoryIdentifier());
        s.setServicesAutomanaged(sv.getServicesAutomanaged());
        s.setRuntimeAutodiscovery(sv.getRuntimeAutodiscovery());
        s.setWasAutodiscovered(sv.getWasAutodiscovered());
        s.setAutodiscoveryZombie(false);
        s.setOwner(sv.getOwner());
        s.setLocation(sv.getLocation());
        s.setModifiedBy(sv.getModifiedBy());
        s.setConfigResponse(configResponse);

        Platform p = new Platform();
        p.setId(sv.getPlatform().getId());
        s.setPlatform(p);

        ServerType st = new ServerType();
        st.setId(sv.getServerType().getId());
        s.setServerType(st);
        save(s);
        return s;
    }

    public Service createService(Server s, ServiceValue sv)
        throws ValidationException
    {
        // validate the service
        s.validateNewService(sv);
        // get the Service home
        ServiceDAO sLHome =
            DAOFactory.getDAOFactory().getServiceDAO();                // create it
        return sLHome.create(sv, s.getPrimaryKey());
    }

    /**
     * @deprecated use findAll_orderName()
     * @return
     */
    public Collection findAll_orderName_asc()
    {
        return findAll_orderName(true);
    }

    /**
     * @deprecated use findAll_orderName()
     * @return
     */
    public Collection findAll_orderName_desc()
    {
        return findAll_orderName(false);
    }

    public Collection findAll_orderName(boolean asc)
    {
        String sql="from Server s join fetch s.serverType st " +
                   "where st.virtual=false " +
                   "order by sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public Collection findByType(Integer sTypeId)
    {
        String sql="from Server where serverType.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, sTypeId.intValue())
            .list();
    }

    public List findByPlatform_orderName(Integer id)
    {
        String sql="from Server where platform.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByPlatform_orderName(Integer id, Boolean virtual)
    {
        String sql="from Server where platform.id=? and " +
                   "serverType.virtual=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setBoolean(1, virtual.booleanValue())
            .list();
    }

    public List findByPlatformAndType_orderName(Integer id, Integer tid)
    {
        String sql="from Server where platform.id=? and " +
                   "serverType.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .list();
    }

    public List findByPlatformAndType_orderName(Integer id, Integer tid,
                                                Boolean isVirtual)
    {
        String sql="from Server where platform.id=? and " +
                   "serverType.id=? " +
                   "serverType.virtual=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .setBoolean(2, isVirtual.booleanValue())
            .list();
    }

    public List findByName(String name)
    {
        String sql="from Server where sortName=?";
        return getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .list();
    }

    public Server findByPrimaryKey(ServerPK pk)
    {
        return findById(pk.getId());
    }
}

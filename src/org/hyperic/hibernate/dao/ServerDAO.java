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
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.dao.DAOFactory;

import java.util.Collection;
import java.util.List;
import java.util.Iterator;

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
     * legacy EJB method for creating a server.
     *
     * Create a server for this Platform. The server is assumed
     * to have an associated server type already set. This operation
     * has to be performed as part of an existing transaction.
     */
    public Server createServer(Platform p, ServerValue sv)
        throws ValidationException
    {
        // validate the object
        validateNewServer(p, sv);
        // set the parent platform to be this
        // XXX cheap hack, the ejbPostCreate in ServerEJB only
        // needs to be able to detect the foreign key of
        // the parent platform. So, I'll skip the valueobject
        // creation and create one with just an ID
        PlatformLightValue pv = new PlatformLightValue();
        pv.setId(p.getId());
        sv.setPlatform(pv);
        // get the server home
        // create it
        return create(sv);
    }

    /**
     * Validate a server value object which is to be created on this
     * platform. This method will check IP conflicts and any other
     * special constraint required to succesfully add a server instance
     * to a platform
     */
    private void validateNewServer(Platform p, ServerValue sv)
        throws ValidationException
    {
        // ensure the server value has a server type
        String msg = null;
        if(sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if(sv.idHasBeenSet()){
            msg = "This server is not new, it has ID:" + sv.getId();
        }
        if(msg == null) {
            Integer id = sv.getServerType().getId();
            Collection stypes = p.getPlatformType().getServerTypes();
            for (Iterator i = stypes.iterator(); i.hasNext();) {
                ServerType sVal = (ServerType)i.next();
                if(sVal.getId().equals(id))
                    return;
            }
            msg = "Servers of type '" + sv.getServerType().getName() +
                "' cannot be created on platforms of type '" +
                p.getPlatformType().getName() +"'";
        }
        if (msg != null) {
            throw new ValidationException(msg);
        }
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
                   "order by s.sortName " + (asc ? "asc" : "desc");
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

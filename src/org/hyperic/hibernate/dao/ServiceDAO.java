package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.Service;
import org.hyperic.hq.appdef.ServiceType;
import org.hyperic.hq.appdef.Server;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.dao.DAOFactory;

import java.util.Collection;
import java.util.List;

/**
 * CRUD methods, finders, etc. for Service
 */
public class ServiceDAO extends HibernateDAO
{
    public ServiceDAO(Session session)
    {
        super(Service.class, session);
    }

    public Service findById(Integer id)
    {
        return (Service)super.findById(id);
    }

    public void evict(Service entity)
    {
        super.evict(entity);
    }

    public Service merge(Service entity)
    {
        return (Service)super.merge(entity);
    }

    public void save(Service entity)
    {
        super.save(entity);
    }

    public void remove(Service entity)
    {
        super.remove(entity);
    }

    public Service create(ServiceValue sv, ServerPK parentPK)
    {
        ConfigResponseDB configResponse =
            DAOFactory.getDAOFactory().getConfigResponseDAO().create();
        
        Service s = new Service();
        s.setName(sv.getName());
        s.setAutodiscoveryZombie(false);
        s.setServiceRt(false);
        s.setEndUserRt(false);
        s.setDescription(sv.getDescription());
        s.setModifiedBy(sv.getModifiedBy());
        s.setLocation(sv.getLocation());
        s.setOwner(sv.getOwner());
        s.setParentId(sv.getParentId());

        if (sv.getServiceType() != null) {
            ServiceType st = new ServiceType();
            st.setId(sv.getServiceType().getId());
            s.setServiceType(st);
        }
        if (parentPK != null) {
            Server server = new Server();
            server.setId(parentPK.getId());
            s.setServer(server);
        }
        s.setConfigResponse(configResponse);
        save(s);
        return s;
    }

    public Service createService(Server s, ServiceValue sv)
        throws ValidationException
    {
        // validate the service
        s.validateNewService(sv);
        // get the Service home
        return create(sv, s.getPrimaryKey());
    }

    public Collection findByParent(Integer parentId)
    {
        String sql="from Service where parentService.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, parentId.intValue())
            .list();
    }

    public Collection findByParentAndType(Integer parentId, Integer typeId)
    {
        String sql="from Service where parentService.id=? and serviceType.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, parentId.intValue())
            .setInteger(1, typeId.intValue())
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use finaAll_orderName(boolean)
     * @return
     */
    public Collection findAll_orderName_asc()
    {
        return findAll_orderName(true);
    }

    /**
     * legacy EJB finder
     * @deprecated use finaAll_orderName(boolean)
     * @return
     */
    public Collection findAll_orderName_desc()
    {
        return findAll_orderName(false);
    }

    public Collection findAll_orderName(boolean asc)
    {
        return getSession()
            .createQuery("from Service order by sortName " +
                         (asc ? "asc" : "desc"))
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use finaAll_orderCtime(boolean)
     * @return
     */
    public Collection findAll_orderCtime_asc()
    {
        return findAll_orderCtime(true);
    }

    /**
     * legacy EJB finder
     * @deprecated use finaAll_orderCtime(boolean)
     * @return
     */
    public Collection findAll_orderCtime_desc()
    {
        return findAll_orderCtime(false);
    }

    public Collection findAll_orderCtime(boolean asc)
    {
        return getSession()
            .createQuery("from Service order by creationTime " +
                         (asc ? "asc" : "desc"))
            .list();
    }

    public Collection findByType(Integer st)
    {
        String sql="from Service where serviceType.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, st.intValue())
            .list();
    }

    public List findByName(String name)
    {
        String sql="from Service where sortName=?";
        return getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use finaByPlatform_orderName(boolean)
     * @return
     */
    public Collection findByPlatform_orderName_asc(Integer id)
    {
        return findByPlatform_orderName(id, true);
    }

    /**
     * legacy EJB finder
     * @deprecated use finaByPlatform_orderName(boolean)
     * @return
     */
    public Collection findByPlatform_orderName_desc(Integer id)
    {
        return findByPlatform_orderName(id, false);
    }

    public Collection findByPlatform_orderName(Integer id, boolean asc)
    {
        String sql="select sv from Platform p " +
                   " join fetch p.servers s " +
                   " join fetch s.services sv "+
                   "where p.id=?" +
                   "order by s.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use finaByPlatform_orderType(boolean)
     * @return
     */
    public Collection findByPlatform_orderType_asc(Integer id)
    {
        return findByPlatform_orderType(id, true);
    }

    /**
     * legacy EJB finder
     * @deprecated use finaByPlatform_orderType(boolean)
     * @return
     */
    public Collection findByPlatform_orderType_desc(Integer id)
    {
        return findByPlatform_orderType(id, false);
    }

    public Collection findByPlatform_orderType(Integer id, boolean asc)
    {
        String sql="select sv from Platform p " +
                   " join fetch p.servers s " +
                   " join fetch s.serviceType st " +
                   " join fetch s.services sv "+
                   "where p.id=?" +
                   "order by st.sortName "+
                   (asc ? "asc" : "desc") +
                   ", s.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use finaPlatformServices_orderName(boolean)
     * @return
     */
    public Collection findPlatformServices_orderName(Integer platId, boolean b)
    {
        return findPlatformServices_orderName(platId, b, true);
    }

    /**
     * legacy EJB finder
     * @deprecated use finaPlatformServices_orderName(boolean)
     * @return
     */
    public Collection findPlatformServices_orderName_desc(Integer platId,
                                                          boolean b)
    {
        return findPlatformServices_orderName(platId, b, false);
    }

    public Collection findPlatformServices_orderName(Integer platId,
                                                      boolean b,
                                                      boolean asc)
    {
        String sql="select sv from Platform p " +
                   " join fetch p.servers s " +
                   " join fetch s.serviceType st " +
                   " join fetch s.services sv "+
                   "where p.id=? and" +
                   "      st.virtual=?" +
                   "order by sv.sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, platId.intValue())
            .setBoolean(1, b)
            .list();
    }

    public List findByServer_orderName(Integer id)
    {
        String sql="from Service where server.id=? order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByServer_orderType(Integer id)
    {
        String sql="select s from Service s " +
                   " join fetch s.serviceType st " +
                   "where server.id=? " +
                   "order by st.sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByServerAndType_orderName(Integer id, Integer tid)
    {
        String sql="from Service where server.id=? and serviceType.id=? " +
                   "order by sortName";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, tid.intValue())
            .list();
    }

    public Service findByApplication(Integer appId)
    {
        String sql="select s from Service s " +
                   " join fetch s.appServices a " +
                   "where a.application.id=? ";
        return (Service)getSession().createQuery(sql)
            .setInteger(0, appId.intValue())
            .uniqueResult();
    }

    public Collection findByCluster(Integer clusterId)
    {
        String sql="select s from Service s " +
                   " join fetch s.serviceCluster c " +
                   "where c.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, clusterId.intValue())
            .list();
    }

    /**
     * legacy EJB finder
     * @deprecated use findAllClusterUnassigned_orderName(boolean)
     * @return
     */
    public Collection findAllClusterUnassigned_orderName_asc()
    {
        return findAllClusterUnassigned_orderName(true);
    }

    /**
     * legacy EJB finder
     * @deprecated use findAllClusterUnassigned_orderName(boolean)
     * @return
     */
    public Collection findAllClusterUnassigned_orderName_desc()
    {
        return findAllClusterUnassigned_orderName(false);
    }

    public Collection findAllClusterUnassigned_orderName(boolean asc)
    {
        String sql="from Service where serviceCluster is null " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    /**
     * legacy EJB finder
     * @deprecated use findAllClusterUnassigned_orderName(boolean)
     * @return
     */
    public Collection findAllClusterAppUnassigned_orderName_asc()
    {
        return findAllClusterAppUnassigned_orderName(true);
    }

    /**
     * legacy EJB finder
     * @deprecated use findAllClusterUnassigned_orderName(boolean)
     * @return
     */
    public Collection findAllClusterAppUnassigned_orderName_desc()
    {
        return findAllClusterAppUnassigned_orderName(false);
    }

    public Collection findAllClusterAppUnassigned_orderName(boolean asc)
    {
        String sql="from Service where serviceCluster is null and " +
                   "appServices.size=0 " +
                   "order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    /**
     * legacy EJB primary key finder
     * @deprecated use findById() instead
     * @param pk
     * @return
     */
    public Service findByPrimaryKey(ServicePK pk)
    {
        return findById(pk.getId());
    }
}

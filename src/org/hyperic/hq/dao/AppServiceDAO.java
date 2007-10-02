package org.hyperic.hq.dao;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.AppSvcDependency;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Service;

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

public class AppServiceDAO extends HibernateDAO
{
    private static final Log log = LogFactory.getLog(ServiceClusterDAO.class);

    public AppServiceDAO(DAOFactory f) {
        super(AppService.class, f);
    }

    public AppService findById(Integer id)
    {
        return (AppService)super.findById(id);
    }

    public void save(AppService entity)
    {
        super.save(entity);
    }

    public void remove(AppService entity)
    {
        // Need to make sure that it's removed from the map table
        Collection appDeps = DAOFactory.getDAOFactory()
            .getAppSvcDepencyDAO().findByDependents(entity);
        for (Iterator it = appDeps.iterator(); it.hasNext(); ) {
            AppSvcDependency appDep = (AppSvcDependency) it.next();
            AppService appSvc = appDep.getAppService();
            appSvc.getAppSvcDependencies().remove(appDep);
            super.remove(appDep);
        }
        
        for (Iterator it = entity.getAppSvcDependencies().iterator();
             it.hasNext(); ) {
            super.remove(it.next());
        }
        super.remove(entity);
    }

    public AppService create(Integer spk, Integer apk,
                             boolean entryPoint)
    {
        // reassociate service
        Service s = DAOFactory.getDAOFactory().getServiceDAO()
            .findById(spk);
        // reassociate application
        Application ap = DAOFactory.getDAOFactory().getApplicationDAO()
            .findById(apk);

        AppService a = new AppService();
        a.setIsEntryPoint(entryPoint);
        a.setService(s);
        a.setServiceType(s.getServiceType());
        a.setApplication(ap);
        save(a);
        return a;
    }

    /**
     * Add a non-entry point service to this application
     * @return AppServiceLocal
     */
    public AppService addEntryPoint(Application a, Integer aService)
    {
        return addService(a, aService, true);
    }

    /**
     * Add a non-entry point service to this application
     * @return AppService
     */
    public AppService addService(Application a, Integer aService)
    {
        return addService(a, aService, false);
    }

    /**
     * Add a service to this application
     * @param entryPoint - is this service an entry point?
     * @return AppServiceLocal
     */
    protected AppService addService(Application a, Integer aService,
                                    boolean entryPoint)
    {
        // first create the AppService
        return create(aService, a.getId(), entryPoint);
    }

    /**
     * Add a service cluster to this application
     * @return appService
     */
    public AppService addServiceCluster(Application a,
                                        Integer aClusterPK)
    {
        // first create the AppService
        return create(aClusterPK, a.getId());
    }

    public AppService create(Integer cpk, Integer apk)
    {
        // reassociate service cluster
        ServiceCluster sc = DAOFactory.getDAOFactory().getServiceClusterDAO()
            .findById(cpk);
        // reassociate application
        Application ap = DAOFactory.getDAOFactory().getApplicationDAO()
            .findById(apk);

        AppService a = new AppService();
        a.setIsCluster(true);
        a.setServiceCluster(sc);
        a.setServiceType(sc.getServiceType());
        a.setApplication(ap);
        save(a);
        return a;
    }

    public List findByApplication_orderName(Integer id)
    {
        // TODO: fix this query after authz conversion
        String sql=
            "select distinct a from " +
            "AppService a, Resource r, ResourceType t " +
            "where a.application.id=? and (" +
            "r.resourceType.id=t.id AND t.name='covalentAuthzResourceGroup' "+
            "AND a.serviceCluster.id IN (" +
            "SELECT id FROM ServiceCluster c where c.group.id = r.instanceId)"+
            " OR " +
            "(r.instanceId=a.service.id and " +
            "r.resourceType.id=t.id AND t.name='covalentEAMService')))";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public List findByApplication_orderType(Integer id, boolean asc)
    {
        String sql="select distinct a from AppService a " +
                   " join fetch a.serviceType st " +
                   "where a.application.id=? " +
                   "order by st.name " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public Collection findByApplication(Integer id)
    {
        String sql="select distinct a from AppService a " +
                   "where a.application.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    /**
     * @deprecated use findByApplication_orderSvcName()
     * @param id
     * @return
     */
    public Collection findByApplication_orderSvcName_asc(Integer id)
    {
        return findByApplication_orderSvcName(id, true);
    }

    /**
     * @deprecated use findByApplication_orderSvcName()
     * @param id
     * @return
     */
    public Collection findByApplication_orderSvcName_desc(Integer id)
    {
        return findByApplication_orderSvcName(id, false);
    }

    public Collection findByApplication_orderSvcName(Integer id, boolean asc)
    {
        String sql=
            "select distinct a from AppService a " +
            " join fetch a.service s " +
            "where a.application.id=? " +
            "order by s.name " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    /**
     * @deprecated use findByApplication_orderSvcType()
     * @param id
     * @return
     */
    public Collection findByApplication_orderSvcType_asc(Integer id)
    {
        return findByApplication_orderSvcType(id, true);
    }

    /**
     * @deprecated use findByApplication_orderSvcType()
     * @param id
     * @return
     */
    public Collection findByApplication_orderSvcType_desc(Integer id)
    {
        return findByApplication_orderSvcType(id, false);
    }

    public Collection findByApplication_orderSvcType(Integer id, boolean asc)
    {
        String sql=
            "select distinct a from AppService a " +
            " join fetch a.service s " +
            " join fetch a.serviceType st " +
            "where a.application.id=? " +
            "order by st.name " + (asc ? "asc" : "desc") + ", s.name";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public Collection findEntryPointsByApp(Integer id)
    {
        String sql=
            "select distinct a from AppService a " +
            "where a.application.id=? and a.isEntryPoint=true";
        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }

    public AppService findByAppAndService(Integer appId, Integer svcId)
    {
        String sql=
            "select distinct a from AppService a " +
            "where a.application.id=? and a.service.id=?";
        return (AppService)getSession().createQuery(sql)
            .setInteger(0, appId.intValue())
            .setInteger(1, svcId.intValue())
            .uniqueResult();
    }

    public AppService findByAppAndCluster(Integer appId, Integer svcClusterId)
    {
        String sql=
            "select distinct a from AppService a " +
            "where a.application.id=? and a.serviceCluster.id=?";
        return (AppService)getSession().createQuery(sql)
            .setInteger(0, appId.intValue())
            .setInteger(1, svcClusterId.intValue())
            .uniqueResult();
    }

    public AppSvcDependency addDependentService(Service s,
                                                Integer appPK,
                                                Integer depPK)
    {
        // look for the app service for **this** Service
        AppService appSvc = findByAppAndService(appPK, s.getId());
        if (appSvc == null) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK + " Service: " + s.getId());
            appSvc =create(s.getId(), appPK, true);
        }
        // try to find the app service for the dependent service
        AppService depSvc = findByAppAndService(appPK, depPK);
        if (depSvc == null) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK + " Service: " + s.getId());
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc = create(depPK, appPK, false);
        }
        // now we add the dependency
        AppSvcDependencyDAO depdao =
            DAOFactory.getDAOFactory().getAppSvcDepencyDAO();
        return depdao.create(appSvc, depSvc);
    }

    public AppSvcDependency addDependentServiceCluster(Service s,
                                                       Integer appPK,
                                                       Integer depPK)
    {
        // first we see if we can find an existing AppService object
        // if we cant, we add it

        // look for the app service for **this** Service
        AppService appSvc = findByAppAndService(appPK, s.getId());
        if (appSvc == null) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK + " Service: " + s.getId());
            appSvc = create(s.getId(), appPK, true);
        }
        // try to find the app service for the dependent service
        AppService depSvc = findByAppAndCluster(appPK, depPK);
        if (depSvc == null) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK + " ServiceCluster: " + s.getId());
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc = create(depPK, appPK);
        }
        // now we add the dependency
        AppSvcDependencyDAO depdao =
            DAOFactory.getDAOFactory().getAppSvcDepencyDAO();
        return depdao.create(appSvc, depSvc);
    }

    /**
     * add a dependent service of this cluster
     */
    public AppSvcDependency addDependentService(
        ServiceCluster sc, Integer appPK, Integer depPK)
    {
        // look for the app service for **this** cluster
        AppService appSvc = findByAppAndCluster(appPK, sc.getId());
        if (appSvc == null) {
            log.debug(
                "Creating new app service object for Application: "
                + appPK + " ServiceCluster: " + sc.getId());
            appSvc = create(sc.getId(), appPK);
        }
        // try to find the app service for the dependent service
        AppService depSvc = findByAppAndService(appPK, depPK);
        if (depSvc == null) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK + " Service: " + depPK);
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc = create(depPK, appPK, false);
        }
        // now we add the dependency
        AppSvcDependencyDAO depdao =
            DAOFactory.getDAOFactory().getAppSvcDepencyDAO();
        return depdao.create(appSvc, depSvc);
    }

    /**
     * add a dependent cluster of this cluster
     */
    public AppSvcDependency addDependentServiceCluster(
        ServiceCluster sc, Integer appPK, Integer depPK)
    {
        // look for the app service for **this** cluster
        AppService appSvc = findByAppAndCluster(appPK, sc.getId());
        if (appSvc == null) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK + " ServiceCluster: " + sc.getId());
            appSvc = create(sc.getId(), appPK);
        }
        // try to find the app service for the dependent service
        AppService depSvc = findByAppAndCluster(appPK, depPK);
        if (depSvc == null) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK+ " ServiceCluster: " + depPK);
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc = create(depPK, appPK);
        }
        // now we add the dependency
        AppSvcDependencyDAO depdao =
            DAOFactory.getDAOFactory().getAppSvcDepencyDAO();
        return depdao.create(appSvc, depSvc);
    }
}

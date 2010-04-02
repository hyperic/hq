package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.AppSvcDependency;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
@Repository
public class AppServiceDAO
    extends HibernateDAO<AppService> {

    private static final String serviceResType = AuthzConstants.serviceResType;
    private static final String groupResType = AuthzConstants.groupResType;
    private ResourceGroupDAO resourceGroupDAO;
    private ServiceDAO serviceDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private AppSvcDependencyDAO appSvcDependencyDAO;

    @Autowired
    public AppServiceDAO(SessionFactory f, ResourceGroupDAO resourceGroupDAO,
                         ServiceDAO serviceDAO, ServiceTypeDAO serviceTypeDAO,
                         AppSvcDependencyDAO appSvcDependencyDAO) {
        super(AppService.class, f);
        this.resourceGroupDAO = resourceGroupDAO;
        this.serviceDAO = serviceDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.appSvcDependencyDAO = appSvcDependencyDAO;
    }

    public AppService findById(Integer id) {
        return (AppService) super.findById(id);
    }

    public void save(AppService entity) {
        super.save(entity);
    }

    public void remove(AppService entity) {
        // Need to make sure that it's removed from the map table
        Collection appDeps = appSvcDependencyDAO.findByDependents(entity);
        for (Iterator it = appDeps.iterator(); it.hasNext();) {
            AppSvcDependency appDep = (AppSvcDependency) it.next();
            AppService appSvc = appDep.getAppService();
            appSvc.getAppSvcDependencies().remove(appDep);
            getSession().delete(appDep);
        }

        for (Iterator it = entity.getAppSvcDependencies().iterator(); it.hasNext();) {
            getSession().delete(it.next());
        }
        super.remove(entity);
    }

    public AppService create(Integer cpk, Application ap) {
        // reassociate service cluster
        ResourceGroup gr = resourceGroupDAO.findById(cpk);

        ServiceType type = serviceTypeDAO.findById(gr.getResourcePrototype().getInstanceId());
        AppService a = new AppService();
        a.setIsGroup(true);
        a.setResourceGroup(gr);
        a.setServiceType(type);
        a.setApplication(ap);
        save(a);
        return a;
    }

    public AppService create(Integer spk, Application ap, boolean entryPoint) {
        // reassociate service
        Service s = serviceDAO.findById(spk);

        AppService a = new AppService();
        a.setEntryPoint(entryPoint);
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
    public AppService addEntryPoint(Application a, Integer aService) {
        return addService(a, aService, true);
    }

    /**
     * Add a non-entry point service to this application
     * @return AppService
     */
    public AppService addService(Application a, Integer aService) {
        return addService(a, aService, false);
    }

    /**
     * Add a service to this application
     * @param entryPoint - is this service an entry point?
     * @return AppServiceLocal
     */
    protected AppService addService(Application a, Integer aService, boolean entryPoint) {
        // first create the AppService
        return create(aService, a, entryPoint);
    }

    /**
     * Add a service cluster to this application
     * @return appService
     */
    public AppService addServiceCluster(Application a, Integer aClusterPK) {
        // first create the AppService
        return create(aClusterPK, a);
    }

    public List<AppService> findByApplication_orderName(Integer id) {
        // TODO: fix this query after authz conversion
        String sql = "select distinct a from " + "AppService a, Resource r, ResourceType t "
                     + "where a.application.id=:appid and ("
                     + "r.resourceType.id=t.id AND t.name=:groupType "
                     + "AND a.resourceGroup.id IN ("
                     + "SELECT id FROM ResourceGroup g where g.id = r.instanceId)" + " OR "
                     + "(r.instanceId=a.service.id and "
                     + "r.resourceType.id=t.id AND t.name=:serviceType))) " + "order by r.name";
        return getSession().createQuery(sql).setInteger("appid", id.intValue()).setString(
            "groupType", groupResType).setString("serviceType", serviceResType).list();
    }

    public List<AppService> findByApplication_orderType(Integer id, boolean asc) {
        String sql = "select distinct a from AppService a " + " join fetch a.serviceType st " +
                     "where a.application.id=? " + "order by st.name " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public Collection findByApplication(Integer id) {
        String sql = "select distinct a from AppService a " + "where a.application.id=?";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public Collection<AppService> findByApplication_orderSvcName(Integer id, boolean asc) {
        String sql = "select distinct a from AppService a " + " join fetch a.service s " +
                     "where a.application.id=? " + "order by s.resource.name " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public Collection<AppService> findByApplication_orderSvcType(Integer id, boolean asc) {
        String sql = "select distinct a from AppService a " + " join fetch a.service s " +
                     " join fetch a.serviceType st " + "where a.application.id=? " +
                     "order by st.name " + (asc ? "asc" : "desc") + ", s.resource.name";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public Collection findEntryPointsByApp(Integer id) {
        String sql = "select distinct a from AppService a "
                     + "where a.application.id=? and a.isEntryPoint=true";
        return getSession().createQuery(sql).setInteger(0, id.intValue()).list();
    }

    public AppService findByAppAndService(Integer appId, Integer svcId) {
        String sql = "select distinct a from AppService a "
                     + "where a.application.id=? and a.service.id=?";
        return (AppService) getSession().createQuery(sql).setInteger(0, appId.intValue())
            .setInteger(1, svcId.intValue()).uniqueResult();
    }

    public AppService findByAppAndCluster(Application app, ResourceGroup g) {
        String sql = "select distinct a from AppService a "
                     + "where a.application=? and a.resourceGroup=?";
        return (AppService) getSession().createQuery(sql).setParameter(0, app).setParameter(1, g)
            .uniqueResult();
    }

    public AppSvcDependency addDependentService(Integer appSvcPK, Integer depPK) {

        // Make sure there isn't already a dependency
        AppSvcDependency dep = appSvcDependencyDAO.findByDependentAndDependor(appSvcPK, depPK);

        if (dep != null) {
            return dep;
        }

        // look for the app service for **this** Service
        AppService appSvc = findById(appSvcPK);

        // try to find the app service for the dependent service
        AppService depSvc = findById(depPK);

        // now we add the dependency
        return appSvcDependencyDAO.create(appSvc, depSvc);
    }
}

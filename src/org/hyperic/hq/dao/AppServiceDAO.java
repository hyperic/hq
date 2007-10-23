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
import org.hyperic.hq.appdef.server.session.ApplicationDAO;
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

    public AppService create(Integer spk, Integer apk, boolean entryPoint) {
        // reassociate service
        Service s = new ServiceDAO(DAOFactory.getDAOFactory()).findById(spk);
        return create(s, apk, entryPoint);
    }

    public AppService create(Service s, Integer apk, boolean entryPoint) {
        // reassociate application
        Application ap = new ApplicationDAO(DAOFactory.getDAOFactory())
            .findById(apk);

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
            "r.resourceType.id=t.id AND t.name='covalentEAMService'))) " +
            "order by r.name";
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

    public AppSvcDependency addDependentService(Integer appSvcPK,
                                                Integer depPK)
    {
        AppSvcDependencyDAO depdao =
            DAOFactory.getDAOFactory().getAppSvcDepencyDAO();
        
        // Make sure there isn't already a dependency
        AppSvcDependency depEJB =
            depdao.findByDependentAndDependor(appSvcPK, depPK);
        
        if (depEJB != null) {
            return depEJB;
        }

        // look for the app service for **this** Service
        AppService appSvc = findById(appSvcPK);

        // try to find the app service for the dependent service
        AppService depSvc = findById(depPK);

        // now we add the dependency
        return depdao.create(appSvc, depSvc);
    }
}

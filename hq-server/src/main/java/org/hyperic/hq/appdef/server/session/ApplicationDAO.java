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

package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.AppSvcDependency;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationDAO
    extends HibernateDAO<Application> {
    private final Log log = LogFactory.getLog(ApplicationDAO.class);

    private AppSvcDependencyDAO appSvcDependencyDAO;

    private AppServiceDAO appServiceDAO;

    @Autowired
    public ApplicationDAO(SessionFactory f, AppSvcDependencyDAO appSvcDependencyDAO,
                          AppServiceDAO appServiceDAO) {
        super(Application.class, f);
        this.appSvcDependencyDAO = appSvcDependencyDAO;
        this.appServiceDAO = appServiceDAO;
    }

    void addAppService(Application a, Integer added) {

        AppService as = appServiceDAO.findById(added);
        as.setApplication(a);
        a.getAppServices().add(as);
    }

    void removeAppService(Application a, Integer removed) {
        AppService as = appServiceDAO.findById(removed);
        a.getAppServices().remove(as);
    }

    public DependencyTree getDependencyTree(Application a) {
        log.debug("Getting Dependency Tree for Application: " + a.getName());
        // construct the tree
        DependencyTree aTree = new DependencyTree(a.getApplicationValueObject());
        // find all the dependency entries for this app
        Collection allDeps = appSvcDependencyDAO.findByApplication(a.getId());
        log.debug("Found: " + allDeps.size() + " dependencies");
        // now find all the app services for this app
        Collection appServices = a.getAppServiceSnapshot();
        // add them to the top level of the tree
        for (Iterator i = appServices.iterator(); i.hasNext();) {
            AppService appSvc = (AppService) i.next();
            aTree.addNode(appSvc);
        }
        for (Iterator i = allDeps.iterator(); i.hasNext();) {
            AppSvcDependency aDep = (AppSvcDependency) i.next();
            // get the appservice it refers to
            AppService appService = aDep.getAppService();
            AppService depService = aDep.getDependentService();

            if (log.isDebugEnabled())
                log.debug("AppService: " + appService + "\n depends on: " + depService);
            // add the node to the tree. The tree will take care
            // of appending the dependency if its there already
            aTree.addNode(appService, depService);
        }
        return aTree;
    }

    void setDependencyTree(Application a, DependencyTree newTree) {
        log.debug("Setting dependency tree for application: " + a.getName());
        List nodes = newTree.getNodes();

        for (int i = 0; i < nodes.size(); i++) {
            DependencyNode aNode = (DependencyNode) nodes.get(i);
            // first deal with the removed dependencies for this node
            for (int j = 0; j < aNode.getRemovedChildren().size(); j++) {
                AppService removedAsv = (AppService) aNode.getRemovedChildren().get(j);
                // this dep has been removed
                // look it up and delete it
                AppSvcDependency dep = appSvcDependencyDAO.findByDependentAndDependor(aNode
                    .getAppService().getId(), removedAsv.getId());
                if (dep != null) {
                    appSvcDependencyDAO.remove(dep);
                }
            }
            // now iterate over the new and existing deps

            AppService nodeAsv = aNode.getAppService();
            for (int j = 0; j < aNode.getChildren().size(); j++) {
                AppService depAsv = (AppService) aNode.getChildren().get(j);

                // new dependency
                appServiceDAO.addDependentService(nodeAsv.getId(), depAsv.getId());
            }

            // finally set the entry point flag on the AppService
            boolean isEntryPoint = newTree.isEntryPoint(aNode.getAppService());
            appServiceDAO.findById(aNode.getAppService().getId()).setEntryPoint(isEntryPoint);
        }
    }

    void setApplicationValue(Application a, ApplicationValue appV) {
        a.setName(appV.getName());
        a.setSortName(appV.getName().toUpperCase());
        a.setSortName(appV.getSortName());
        a.setModifiedBy(appV.getModifiedBy());
        a.setLocation(appV.getLocation());
        a.setEngContact(appV.getEngContact());
        a.setOpsContact(appV.getOpsContact());
        a.setBusinessContact(appV.getBusinessContact());
        a.setDescription(appV.getDescription());
        a.setCreationTime(appV.getCTime());

        if (appV.getAddedAppServiceValues() != null) {
            Iterator iAppServiceValue = appV.getAddedAppServiceValues().iterator();
            while (iAppServiceValue.hasNext()) {
                AppServiceValue o = (AppServiceValue) iAppServiceValue.next();
                addAppService(a, o.getId());
            }
        }
        if (appV.getRemovedAppServiceValues() != null) {
            Iterator iAppServiceValue = appV.getRemovedAppServiceValues().iterator();
            while (iAppServiceValue.hasNext()) {
                AppServiceValue o = (AppServiceValue) iAppServiceValue.next();
                removeAppService(a, o.getId());
            }
        }
        // Checks for null aggregate
        if (appV.getApplicationType() != null) {
            ApplicationType at = new ApplicationType(appV.getApplicationType().getId());
            a.setApplicationType(at);
        }
    }

    public Application create(ApplicationValue av) {
        Application app = new Application();
        setApplicationValue(app, av);

        // Save application so that it would have a valid ID
        save(app);

        return app;
    }

    public Collection<Application> findAll_orderName(boolean asc) {
        String sql = "from Application order by resource.sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public Application findByName(String name) {
        String sql = "from Application where upper(resource.name) = ?";
        return (Application) getSession().createQuery(sql).setString(0, name.toUpperCase())
            .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<Application> findByServiceId_orderName(Integer serviceId) {
        String sql = new StringBuilder()
            .append("select a from Application a ")
            .append(" join fetch a.appServices s ")
            .append("where s.service.id=? ")
            .append("order by a.resource.sortName")
            .toString();
        return getSession().createQuery(sql)
                           .setInteger(0, serviceId.intValue())
                           .setCacheable(true)
                           .setCacheRegion("Application.findByServiceId_orderName")
                           .list();
    }

    public Collection<Application> findByServiceIdOrClusterId_orderName(Integer serviceId,
                                                                        Integer groupId) {
        String sql = "select a from Application a " + " join fetch a.appServices s "
                     + "where s.service.id=? or s.resourceGroup.id=?"
                     + "order by a.resource.sortName";
        return getSession().createQuery(sql).setInteger(0, serviceId.intValue()).setInteger(1,
            groupId.intValue()).list();
    }

    public Collection<Application> findByServerId_orderName(Integer serverId, boolean asc) {
        String sql = "select a from Application a " + " join fetch a.appServices asv " +
                     " join fetch asv.service s " + "where s.server.id=? " +
                     "order by a.resource.sortName " + (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, serverId.intValue()).list();
    }

    public Collection<Application> findByPlatformId_orderName(Integer pid, boolean asc) {
        String sql = "select a from Application a " + " join fetch a.appServices asv " +
                     " join fetch asv.service s " + " join fetch s.server srv " +
                     "where srv.platform.id=? " + "order by a.resource.sortName " +
                     (asc ? "asc" : "desc");
        return getSession().createQuery(sql).setInteger(0, pid.intValue()).list();
    }

    public Collection<Application> findByServiceId_orderOwner(Integer serviceId, boolean asc) {
        String sql = "select a from Application a " + " join fetch a.appServices asv " +
                     "where asv.service.id=? " + "order by a.owner " + (asc ? "asc" : "desc") +
                     ", a.resource.sortName";
        return getSession().createQuery(sql).setInteger(0, serviceId.intValue()).list();
    }

    public Collection<Application> findByServerId_orderOwner(Integer serverId, boolean asc) {
        String sql = "select a from Application a " + " join fetch a.appServices asv " +
                     " join fetch asv.service s " + "where s.server.id=? " + "order by a.owner " +
                     (asc ? "asc" : "desc") + ", a.resource.sortName";
        return getSession().createQuery(sql).setInteger(0, serverId.intValue()).list();
    }

    public Collection<Application> findByPlatformId_orderOwner(Integer pid, boolean asc) {
        String sql = "select a from Application a " + " join fetch a.appServices asv " +
                     " join fetch asv.service s " + " join fetch s.server srv " +
                     "where srv.platform.id=? " + "order by a.owner " + (asc ? "asc" : "desc") +
                     ", a.resource.sortName";
        return getSession().createQuery(sql).setInteger(0, pid.intValue()).list();
    }

    public Collection<Application> findUsingGroup(ResourceGroup g) {
        String sql = "select a from Application a " + "join a.appServices s "
                     + "where s.resourceGroup = :group";

        return getSession().createQuery(sql).setParameter("group", g).list();
    }

    void clearResource(Resource res) {
        createQuery("update Application set resource = null where resource = ?").setParameter(0,
            res).executeUpdate();
    }

    public Collection<Application> findDeletedApplications() {
        String hql = "from Application where resource.resourceType = null";
        return createQuery(hql).list();
    }

    public boolean isApplicationService(int applicationId, int serviceId) {
        String sql = "from Application a " + "join a.appServices s "
                     + "where a.id = :aid and s.service.id = :sid";

        List results = getSession().createQuery(sql).setInteger("aid", applicationId).setInteger(
            "sid", serviceId).list();

        return results.size() == 1;
    }
}

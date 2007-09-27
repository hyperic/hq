package org.hyperic.hq.dao;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;

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

public class ServiceClusterDAO extends HibernateDAO {
    private static final Log log = LogFactory.getLog(ServiceClusterDAO.class);

    public ServiceClusterDAO(DAOFactory f) { 
        super(ServiceCluster.class, f);
    }

    public ServiceCluster findById(Integer id) {
        return (ServiceCluster)super.findById(id);
    }

    public void save(ServiceCluster entity) {
        super.save(entity);
    }

    public void remove(ServiceCluster entity) {
        for (Iterator i=entity.getAppServices().iterator(); i.hasNext(); ) {
            AppService appSvc = (AppService)i.next();
            
            appSvc.setServiceCluster(null);
        }
        
        for (Iterator i=entity.getServices().iterator(); i.hasNext(); ) {
            Service svc = (Service)i.next();
            
            svc.setServiceCluster(null);
        }
        entity.clearAppServices();
        entity.clearServices();
        super.remove(entity);
    }

    public ServiceCluster create(ServiceClusterValue scv, List serviceIds) {
        ServiceCluster sc = new ServiceCluster();
        
        ResourceGroup rg = new ResourceGroupDAO(DAOFactory.getDAOFactory())
            .findById(scv.getGroupId());
        sc.setName(scv.getName());
        sc.setDescription(scv.getDescription());
        sc.setGroup(rg);

        Set services = new HashSet(serviceIds.size());
        ServiceDAO dao = DAOFactory.getDAOFactory().getServiceDAO();
        ServiceType st = null;
        for (int i = 0; i < serviceIds.size(); i++) {
            Service service = dao.findById((Integer) serviceIds.get(i));
            if (st == null) {
                st = service.getServiceType();
            }
            services.add(service);
            service.setServiceCluster(sc);
        }
        sc.setServices(services);
        
        if (st == null && scv.getServiceType() != null) {
            st = DAOFactory.getDAOFactory().getServiceTypeDAO()
                    .findById(scv.getServiceType().getId());
        }
        
        if (st != null) {
            sc.setServiceType(st);
        }
            
        save(sc);
        return sc;
    }

    public Collection findAll_orderName(boolean asc)
    {
        String sql="from ServiceCluster order by sortName " +
                   (asc ? "asc" : "desc");
        return getSession().createQuery(sql).list();
    }

    public ServiceCluster findByName(String name)
    {
        String sql="from ServiceCluster order by sortName=?";
        return (ServiceCluster)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    public Collection findUsing(ResourceGroup g) {
        String sql = "from ServiceCluster sc " +
            " where sc.group = :group";

        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }

    /**
     * Add a service to this cluster
     * @throws AppSvcClustDuplicateAssignException - if the service is already
                                                     assigned to a cluster
     * @throws AppSvcClustIncompatSvcException     - If service is incompatible
     */
    public void addService(ServiceCluster sc, Integer serviceId)
        throws AppSvcClustDuplicateAssignException,
               AppSvcClustIncompatSvcException
    {
        ServiceDAO sdao = DAOFactory.getDAOFactory().getServiceDAO();
        Service aService = sdao.findById(serviceId);
        sc.validateMemberService(aService);
        if (sc.getServices() == null) {
            sc.setServices(new HashSet());
        }
        aService.setServiceCluster(sc);
        sc.getServices().add(aService);
    }

    /**
     * Remove a service from this cluster
     * @param serviceId
     */
    public void removeService(ServiceCluster sc, Integer serviceId)
        throws AppSvcClustIncompatSvcException
    {
        if (sc.getServices() == null) {
            return;
        }
        // reassociate service
        ServiceDAO sdao = DAOFactory.getDAOFactory().getServiceDAO();
        Service aService = sdao.findById(serviceId);
        
        // validate that it actually is in this cluster
        if(aService.getServiceCluster() == null ||
           !aService.getServiceCluster().getId().equals(sc.getId())) {
                throw new AppSvcClustIncompatSvcException("Service: " +
                    serviceId + "is not in cluster: " +
                    sc.getName());
        }
        sc.getServices().remove(aService);
    }

    public void updateCluster(ServiceClusterValue serviceCluster,
                              List serviceIds)
        throws AppSvcClustIncompatSvcException,
               AppSvcClustDuplicateAssignException
    {
        // reassociate service cluster
        ServiceCluster sc = findById(serviceCluster.getId());

        ResourceGroupDAO rdao = 
            new ResourceGroupDAO(DAOFactory.getDAOFactory());
        
        // first deal with the stuff from the value objects..
        sc.setServiceClusterValue(serviceCluster);

        if (serviceCluster.getGroupId() != null) {
            ResourceGroup rg = rdao.findById(serviceCluster.getGroupId());
            sc.setGroup(rg);
        }
        
        // now create a new set of service objects for the cluster
        Set services = new HashSet();
        for(int i = 0; i < serviceIds.size(); i++) {
            // find the service by its ID
            ServiceDAO sdao = DAOFactory.getDAOFactory().getServiceDAO();
            Service s = sdao.findById((Integer)serviceIds.get(i));
            sc.validateMemberService(s);
            services.add(s);
        }
        // this should take care of removing any services no longer in the cluster
        // and adding any new entries.
        sc.setServices(services);
        save(sc);
    }
}

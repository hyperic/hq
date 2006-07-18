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

package org.hyperic.hq.appdef.server.entity;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppServiceLocal;
import org.hyperic.hq.appdef.shared.AppServiceUtil;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;
import org.hyperic.hq.appdef.shared.AppSvcDependencyLocal;
import org.hyperic.hq.appdef.shared.AppSvcDependencyUtil;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ServiceClusterLocal;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceLocal;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ServiceClusterEJB implementaiton.  
 * Yes, the name ServiceCluster is redundant because clusters are always
 * comprised of services, but at least it's explicit, and it's a different
 * name that ClusterEJB, which was for the old clustering implementation.
 *
 * @ejb:bean name="ServiceCluster"
 *      jndi-name="ejb/appdef/ServiceCluster"
 *      local-jndi-name="LocalServiceCluster"
 *      view-type="both"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(sc) FROM ServiceCluster AS sc"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM ServiceCluster AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM ServiceCluster AS s ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM ServiceCluster AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM ServiceCluster AS s ORDER BY s.sortName DESC"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ServiceClusterLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(sc) FROM ServiceCluster AS sc WHERE sc.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ServiceClusterLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(sc) FROM ServiceCluster AS sc WHERE LCASE(sc.name) = LCASE(?1)"
 *
 * @ejb:value-object name="ServiceCluster" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *       
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SVC_CLUSTER"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ServiceClusterEJBImpl
    extends AppdefEntityBean implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_SVC_CLUSTER_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ServiceClusterEJBImpl.class.getName());

    public ServiceClusterEJBImpl() {}

    /**
     * Name of this ServiceCluster
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setName(java.lang.String name);

    /**
     * Sort name of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setSortName (java.lang.String sortName);

    /**
     * Description of this ServiceCluster
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(String desc);

    /**
     * The group id that represents this cluster in authz
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     * @jboss:column-name name="GROUP_ID"
     */
    public abstract Integer getGroupId();
    /**
     * @ejb:interface-method
     */
    public abstract void setGroupId(Integer groupId);

    /**
     * Get the value object
     * @ejb:interface-method
     * @jboss:read-only true
     * @ejb:transaction type="REQUIRESNEW"
     */
    public abstract ServiceClusterValue getServiceClusterValue();
    
    /**
     * Set the value object
     * @return void
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceClusterValue(ServiceClusterValue val);
    
    /**
     * Get the services which make up this cluster
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceCluster-Service"
     *      role-name="one-ServiceCluster-has-many-Services"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServices();
    
    /**
     * Set the services that comprise this cluster.
     * @ejb:interface-method
     */
    public abstract void setServices(java.util.Set iSvcs);
    
     /**
      * Get the ServiceType of this Service
      * @ejb:interface-method
      * @ejb:relation
      *      name="ServiceType-ServiceCluster"
      *      role-name="one-ServiceCluster-has-one-ServiceType"
      * @ejb:value-object match="*"
      *      compose="org.hyperic.hq.appdef.shared.ServiceTypeValue"
      *      compose-name="ServiceType"
      * @ejb:transaction type="SUPPORTS"
      * @jboss:relation
      *      fk-column="service_type_id"
      *      related-pk-field="id"
      * @jboss:read-only true
      */
     public abstract org.hyperic.hq.appdef.shared.ServiceTypeLocal getServiceType();
     
     /**
      * Set the ServiceType of this Service.... 
      * @ejb:interface-method
      * @ejb:transaction type="MANDATORY"
      */
     public abstract void setServiceType(org.hyperic.hq.appdef.shared.ServiceTypeLocal serviceType);
    
    /**
     * Add a service to this cluster
     * @param serviceId
     * @throws AppSvcClustDuplicateAssignException - if the service is already 
                                                     assigned to a cluster
     * @throws AppSvcClustIncompatSvcException     - If service is incompatible
     * @throws FinderException
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void addService(Integer serviceId) 
        throws AppSvcClustDuplicateAssignException, AppSvcClustIncompatSvcException, 
               FinderException, NamingException {
        ServiceLocal aService = ServiceUtil.getLocalHome()
            .findByPrimaryKey(new ServicePK(serviceId));
        this.validateMemberService(aService);
        log.debug("Adding service: " + serviceId + 
                  " to cluster: " + this.getName());
        this.getServices().add(aService);
    }
    
    /**
     * Remove a service from this cluster
     * @param serviceId
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeService(Integer serviceId) throws FinderException,
        NamingException, AppSvcClustIncompatSvcException {
        ServiceLocal aService = ServiceUtil.getLocalHome()
            .findByPrimaryKey(new ServicePK(serviceId));
        // validate that it actually is in this cluster
        if(aService.getServiceCluster() == null || 
           !aService.getServiceCluster().getId().equals(this.getId())) {
                throw new AppSvcClustIncompatSvcException("Service: " + 
                    serviceId + "is not in cluster: " +
                    this.getName());
        }
        log.debug("Removing service: " + serviceId +
                  " from cluster: " + this.getName());
        this.getServices().remove(aService);        
    }
    
    /**
     * Validate a new service to be added to this cluster. This enforces
     * service type compatibility as well as only allowing services to be in
     * one cluster at a time
     * @param serviceEJB - a ServiceLocal
     */
    private void validateMemberService(ServiceLocal aService)
        throws AppSvcClustDuplicateAssignException, AppSvcClustIncompatSvcException {
        // validate its not assigned to a cluster already or if it is
        // its assigned to this cluster.
        if(aService.getServiceCluster() != null &&
           !aService.getServiceCluster().equals(
                (ServiceClusterLocal)this.getSelfLocal())) {
            throw new AppSvcClustDuplicateAssignException ("Service: " 
                + aService.getId() 
                + " is already assigned to a cluster"); 
        }
        // validate compatibility
        if(!this.getServiceType().equals(aService.getServiceType())) {
            throw new AppSvcClustIncompatSvcException("Service: " + aService.getId()

                + " has type: " + aService.getServiceType().getName()
                + " which does not match the clusters service type: "
                + this.getServiceType());
        }
    }

    /**
     * Get the AppServices for this ServiceCluster
     * @ejb:interface-method
     * @ejb:relation
     *      name="AppService-ServiceCluster"
     *      role-name="one-ServiceCluster-has-many-AppServices"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.util.Set getAppServices();

    /**
     * Set the AppServices for this ServiceCluster
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAppServices(java.util.Set svcs);

    /**
     * Update a service cluster. All service ids passed in will be the ones stored
     * in the updated cluster definition. New ones will be added, old ones removed.
     * @param serviceCluster
     * @param serviceIds
     * @throws CreateException
     * @throws AppSvcClustDuplicateAssignException - if the service is already 
                                                     assigned to a cluster
     * @throws AppSvcClustIncompatSvcException     - If service is incompatible
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW" 
     */
    public void updateCluster(ServiceClusterValue serviceCluster,
                              List serviceIds)
        throws AppSvcClustDuplicateAssignException, FinderException, 
               AppSvcClustIncompatSvcException, NamingException {
        // first deal with the stuff from the value objects..
        this.setServiceClusterValue(serviceCluster);    
        // now create a new set of service objects for the cluster
        Set services = new HashSet();
        for(int i = 0; i < serviceIds.size(); i++) {
            // find the service by its ID
            ServiceLocal aService = ServiceUtil.getLocalHome()        
                .findByPrimaryKey(new ServicePK((Integer)serviceIds.get(i)));
            this.validateMemberService(aService);
            services.add(aService);
        }
        // this should take care of removing any services no longer in the cluster
        // and adding any new entries.
        this.setServices(services);
    }

    /**
     * add a dependent service of this cluster
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AppSvcDependencyLocal addDependentService(ApplicationPK
        appPK, ServicePK depPK) throws FinderException,
        CreateException, NamingException {
        // first we see if we can find an existing AppService object
        // if we cant, we add it
        AppServiceLocal appSvc = null;
        AppServiceLocal depSvc = null;
        // look for the app service for **this** cluster
        try {
            appSvc = AppServiceUtil.getLocalHome()
                .findByAppAndCluster(appPK.getId(), this.getId());
        } catch (FinderException e) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK.getId() + " ServiceCluster: " + getId());
            appSvc =
                AppServiceUtil.getLocalHome().create(
                    new ServiceClusterPK(getId()), appPK);
        }
        // try to find the app service for the dependent service
        try {
            depSvc = AppServiceUtil.getLocalHome()
                .findByAppAndService(appPK.getId(), depPK.getId());
        } catch (FinderException e) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK.getId() + " Service: " + getId());
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc =
                AppServiceUtil.getLocalHome().create(depPK, appPK, false);
        }
        // now we add the dependency
        AppSvcDependencyLocal  appDep =
            AppSvcDependencyUtil.getLocalHome().create(appSvc, depSvc);
        return appDep;
    }
    
    /**
     * add a dependent cluster of this cluster
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AppSvcDependencyLocal addDependentServiceCluster(ApplicationPK
        appPK, ServiceClusterPK depPK) throws FinderException,
        CreateException, NamingException {
        // first we see if we can find an existing AppService object
        // if we cant, we add it
        AppServiceLocal appSvc = null;
        AppServiceLocal depSvc = null;
        // look for the app service for **this** cluster
        try {
            appSvc = AppServiceUtil.getLocalHome()
                .findByAppAndCluster(appPK.getId(), this.getId());
        } catch (FinderException e) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK.getId() + " ServiceCluster: " + getId());
            appSvc =
                AppServiceUtil.getLocalHome().create(
                    new ServiceClusterPK(getId()), appPK);
        }
        // try to find the app service for the dependent service
        try {
            depSvc = AppServiceUtil.getLocalHome()
                .findByAppAndCluster(appPK.getId(), depPK.getId());
        } catch (FinderException e) {
            log.debug(
                "Creating new dependent app service object for Application: "
                + appPK.getId() + " ServiceCluster: " + getId());
            // dependent services are not allowed to be entry points
            // at least not here ;)
            depSvc =
                AppServiceUtil.getLocalHome().create(depPK, appPK);
        }
        // now we add the dependency
        AppSvcDependencyLocal  appDep =
            AppSvcDependencyUtil.getLocalHome().create(appSvc, depSvc);
        return appDep;
    }

    // APIs to satisfy AppdefResourceValue interface
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public String getOwner() { return ""; }
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public void setOwner(String ignore) {}
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public String getModifiedBy() { return ""; }
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public void setModifiedBy(String ignore) {}
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public String getLocation() { return ""; }
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public void setLocation(String ignore) {}
        
    /**
     * The create method using the value object
     * @param serviceCluster
     * @param serviceIdList - integers matching ids
     * @ejb:create-method
     */
    public ServiceClusterPK ejbCreate(ServiceClusterValue serviceCluster, 
                                      List serviceIds) 
        throws CreateException, AppSvcClustDuplicateAssignException,
               AppSvcClustIncompatSvcException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            if(serviceIds.size() == 0) {
                throw new AppSvcClustIncompatSvcException(
                    "A cluster can not be created without services");   
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(serviceCluster.getName());
            if (serviceCluster.getName()!=null)
                setSortName(serviceCluster.getName().toUpperCase());
            setDescription(serviceCluster.getDescription());
            setGroupId(serviceCluster.getGroupId());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }

    public void ejbPostCreate(ServiceClusterValue serviceCluster,
                              List serviceIds) 
        throws CreateException, AppSvcClustDuplicateAssignException,
               AppSvcClustIncompatSvcException {
        try {
            // set the cluster's service type to the type of the first service on 
            // the list. if any other service has a mismatched type, the create
            // will fail
            ServiceTypeLocal type = ServiceUtil.getLocalHome()
                .findByPrimaryKey(new ServicePK((Integer)serviceIds.get(0)))
                    .getServiceType();
            this.setServiceType(type);
            // add all services
            Set services = new HashSet(serviceIds.size());
            for(int i = 0; i < serviceIds.size(); i++) {
                services.add(ServiceUtil.getLocalHome().findByPrimaryKey(
                    new ServicePK((Integer)serviceIds.get(i))));
            }
            setServices(services);
        } catch (FinderException e) {
            throw new CreateException("Unable to find service to add to cluster: "
                + e.getMessage());
        } catch (NamingException e) {
            throw new CreateException("NamingError in ejbPostCreate: " 
                + e.getMessage());
        }
    }
    
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }
    public void ejbRemove() throws RemoteException, RemoveException {}

}

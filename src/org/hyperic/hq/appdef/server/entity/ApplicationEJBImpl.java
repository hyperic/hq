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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppServiceLocal;
import org.hyperic.hq.appdef.shared.AppServiceUtil;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppSvcDependencyLocal;
import org.hyperic.hq.appdef.shared.AppSvcDependencyUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ApplicationTypeLocal;
import org.hyperic.hq.appdef.shared.ApplicationTypeUtil;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.DependencyNode;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ServiceClusterUtil;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceUtil;
import org.hyperic.hq.appdef.shared.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ApplicationEJB  implementaiton.
 * @ejb:bean name="Application"
 *      jndi-name="ejb/appdef/Application"
 *      local-jndi-name="LocalApplication"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(app) FROM Application AS app"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(a) FROM Application AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(a) FROM Application AS a ORDER BY a.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(a) FROM Application AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(a) FROM Application AS a ORDER BY a.sortName DESC"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ApplicationLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(app) FROM Application AS app WHERE app.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ApplicationLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(app) FROM Application AS app WHERE LCASE(app.name) = LCASE(?1)"
 *
 * @ejb:finder signature="java.util.Collection findByServiceId_orderName(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByServiceId_orderName(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1 ORDER BY app.sortName"

 * @ejb:finder signature="java.util.Collection
        findByServiceIdOrClusterId_orderName(java.lang.Integer serviceId, java.lang.Integer clusterId)"
 *      query="SELECT DISTINCT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1 OR appsvc.serviceCluster.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection
        findByServiceIdOrClusterId_orderName(java.lang.Integer serviceId, java.lang.Integer clusterId)"
 *      query="SELECT DISTINCT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc
               WHERE appsvc.service.id = ?1 OR appsvc.serviceCluster.id = ?2
               ORDER BY app.sortName"

 * @ejb:finder signature="java.util.Collection findByServerId_orderName_asc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByServerId_orderName_asc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1 ORDER BY app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByServerId_orderName_desc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByServerId_orderName_desc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1 ORDER BY app.sortName DESC"
 *
 * @ejb:finder signature="java.util.Collection findByPlatformId_orderName_asc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByPlatformId_orderName_asc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1 ORDER BY app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByPlatformId_orderName_desc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByPlatformId_orderName_desc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1 ORDER BY app.sortName DESC"
 *
 * @ejb:finder signature="java.util.Collection findByServiceId_orderOwner_asc(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByServiceId_orderOwner_asc(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1 ORDER BY app.owner, app.sortName"

 * @ejb:finder signature="java.util.Collection findByServiceId_orderOwner_desc(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByServiceId_orderOwner_desc(java.lang.Integer serviceId)"
 *      query="SELECT OBJECT(app) FROM Application AS app, IN (app.appServices) appsvc WHERE appsvc.service.id = ?1 ORDER BY app.owner DESC, app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByServerId_orderOwner_asc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByServerId_orderOwner_asc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1 ORDER BY app.owner, app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByServerId_orderOwner_desc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByServerId_orderOwner_desc(java.lang.Integer serverId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.id = ?1 ORDER BY app.owner DESC, app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByPlatformId_orderOwner_asc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByPlatformId_orderOwner_asc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1 ORDER BY app.owner, app.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByPlatformId_orderOwner_desc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="signature="java.util.Collection findByPlatformId_orderOwner_desc(java.lang.Integer platformId)"
 *      query="SELECT OBJECT(app) FROM Application app, IN (app.appServices) appsvc WHERE appsvc.service.server.platform.id = ?1 ORDER BY app.owner DESC, app.sortName"
 * @ejb:value-object name="Application" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_APPLICATION"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class ApplicationEJBImpl extends AppdefEntityBean 
implements EntityBean {

    public final String ctx = ApplicationEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_APPLICATION_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;
    private AppdefEntityID eid = null;
    protected Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.entity.ApplicationEJBImpl");

    private Map serviceDepMap = null;

    public ApplicationEJBImpl() {
    }

    /**
     * Name of this Application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setName(java.lang.String name);

    /**
     * Sort Name of this Application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setSortName(java.lang.String name);

    /**
     * modified by of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MODIFIED_BY"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getModifiedBy();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setModifiedBy(java.lang.String comment);

    /**
     * owner by of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="OWNER"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getOwner();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setOwner(java.lang.String owner);

    /**
     * location of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="LOCATION"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getLocation();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setLocation(java.lang.String location);

    /**
     * eng contact of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="ENG_CONTACT"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getEngContact();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setEngContact(java.lang.String engContact);

    /**
     * ops contact of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="OPS_CONTACT"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getOpsContact();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setOpsContact(java.lang.String opsContact);

    /**
     * business contact of this application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="BUS_CONTACT"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getBusinessContact();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setBusinessContact(java.lang.String busContact);

    /**
     * Get the value object for this application
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract ApplicationValue getApplicationValue();

    /**
     * Set the value object for this application
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public abstract void setApplicationValue(ApplicationValue appV);

    /**
     * Description of this Application
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDescription(java.lang.String desc);

    /**
     * Get the ApplicationType of this Application
     * @ejb:interface-method
     * @ejb:relation
     *      name="ApplicationType-Application"
     *      role-name="one-application-has-one-applicationType"
     *      target-ejb="ApplicationType"
     *      target-role-name="one-applicationType-has-many-applications"
     *      target-multiple="yes"
     *
     * @ejb:transaction type="SUPPORTS"
     *
     * @ejb:value-object match="*"
     *      compose="org.hyperic.hq.appdef.shared.ApplicationTypeValue"
     *      compose-name="ApplicationType"
     * @jboss:relation
     *      fk-column="application_type_id"
     *      related-pk-field="id"
     * @jboss-read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ApplicationTypeLocal getApplicationType();

    /**
     * Set the application type
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setApplicationType(org.hyperic.hq.appdef.shared.ApplicationTypeLocal type);

    /**
     * Get the services for this application
     * @ejb:interface-method
     * @ejb:relation
     *      name="Application-AppService"
     *      role-name="one-Application-has-many-AppServices"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      match="*"
     *      type="Set"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.AppServiceValue"
     *      aggregate-name="AppServiceValue"
     *      members="org.hyperic.hq.appdef.shared.AppServiceLocal"
     *      members-name="AppService"
     */
    public abstract java.util.Set getAppServices();

    /**
     * Set the services for this application
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAppServices(java.util.Set services);

    /**
     * Add a non-entry point service to this application 
     * @param ServicePK - the service you wish to add
     * @return AppServiceLocal
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @throws ValidationException - if service is already present 
     * @throws CreateException - couldnt store record
     */
    public AppServiceLocal addService(ServicePK aService) 
        throws CreateException, NamingException {
            return addService(aService, false);
    }

    /**
     * Add a service cluster to this application
     * @param ServiceClusterPK
     * @return appServiceLocal
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AppServiceLocal addServiceCluster(ServiceClusterPK aClusterPK)
        throws CreateException, NamingException {
        // first create the AppService
        AppServiceLocal appService = AppServiceUtil.getLocalHome()
            .create(aClusterPK, new ApplicationPK(getId()));
        return appService;
    }

    /**
     * Add a non-entry point service to this application 
     * @param ServicePK - the service you wish to add
     * @return AppServiceLocal
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @throws ValidationException - if service is already present 
     * @throws CreateException - couldnt store record
     */
    public AppServiceLocal addEntryPoint(ServicePK aService) 
        throws CreateException, NamingException {
            return addService(aService, true);
    }

    /**
     * Get the DependencyTree for this application
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     * @jboss:read-only true
     */
    public DependencyTree getDependencyTree() throws NamingException,
        FinderException {
        log.debug("Getting Dependency Tree for Application: " + getName());
        // construct the tree
        DependencyTree aTree = new DependencyTree(getApplicationValueObject());
        // find all the dependency entries for this app
        Collection allDeps = AppSvcDependencyUtil.getLocalHome()
            .findByApplication(((ApplicationPK)this.getSelfLocal()
                .getPrimaryKey()).getId());
        log.debug("Found: " + allDeps.size() + " dependencies");
        // now find all the app services for this app
        Collection appServices = this.getAppServiceSnapshot();
        // add them to the top level of the tree
        for(Iterator i = appServices.iterator(); i.hasNext();) {
            AppServiceLocal appSvc = (AppServiceLocal)i.next();
            aTree.addNode(appSvc.getAppServiceValue());
        }
        for(Iterator i = allDeps.iterator(); i.hasNext();) {
            AppSvcDependencyLocal aDep = (AppSvcDependencyLocal) i.next();
            // get the appservice it refers to
            AppServiceLocal appServiceEJB = aDep.getAppService();
            AppServiceValue appService = appServiceEJB.getAppServiceValue();
            AppServiceLocal depServiceEJB = aDep.getDependentService();
            AppServiceValue depService = depServiceEJB.getAppServiceValue();
            log.debug("AppService: " + appService + "\n depends on: " +
                depService);
            // add the node to the tree. The tree will take care
            // of appending the dependency if its there already
            aTree.addNode(appService, depService); 
        }
        return aTree;
    }

    /**
     * Set the DependencyTree for this application
     * assumes all app services in the tree have been saved already
     * 
     * @param depTree
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setDependencyTree(DependencyTree newTree) 
        throws CreateException, RemoveException, 
               NamingException, FinderException {
        log.debug("Setting dependency tree for application: " + getName());
        ApplicationPK appPk = new ApplicationPK(this.getId());
        List nodes = newTree.getNodes();
        for(int i=0; i < nodes.size(); i++) {
            DependencyNode aNode = (DependencyNode)nodes.get(i);
            // first deal with the removed dependencies for this node
            for(int j=0; j < aNode.getRemovedChildren().size(); j++) {
                AppServiceValue removedAsv = 
                    (AppServiceValue)aNode.getRemovedChildren().get(j);
                // this dep has been removed
                // look it up and delete it
                AppSvcDependencyLocal depEJB = 
                    AppSvcDependencyUtil.getLocalHome()
                        .findByDependentAndDependor(
                            aNode.getAppService().getId(), 
                            removedAsv.getId());
                depEJB.remove();    
            }
            // now iterate over the new and existing deps
            AppServiceValue nodeAsv = aNode.getAppService();
            for(int j=0; j < aNode.getChildren().size(); j++) {
                AppServiceValue depAsv = 
                    (AppServiceValue)aNode.getChildren().get(j);
                // new dependency
                if(nodeAsv.getIsCluster()) {
                    if(depAsv.getIsCluster()) {
                        ServiceClusterUtil.getLocalHome().findByPrimaryKey(
                            aNode.getServiceClusterPK()).addDependentServiceCluster(
                                newTree.getAppPK(),
                                depAsv.getServiceCluster().getPrimaryKey());
                    } else {
                        ServiceClusterUtil.getLocalHome().findByPrimaryKey(
                            aNode.getServiceClusterPK()).addDependentService(
                                newTree.getAppPK(),
                                depAsv.getService().getPrimaryKey());
                    }
                } else {
                    if(depAsv.getIsCluster()) {
                        ServiceUtil.getLocalHome().findByPrimaryKey(
                            aNode.getServicePK()).addDependentServiceCluster(
                                newTree.getAppPK(),
                                depAsv.getServiceCluster().getPrimaryKey());
                    } else {
                        ServiceUtil.getLocalHome().findByPrimaryKey(
                            aNode.getServicePK()).addDependentService(
                                newTree.getAppPK(), 
                                depAsv.getService().getPrimaryKey());   
                    }
                } 
            }
            // finally set the entry point flag on the AppService
            boolean isEntryPoint = newTree.isEntryPoint(aNode.getAppService());
            AppServiceUtil.getLocalHome().findByPrimaryKey(
                aNode.getAppService().getPrimaryKey())
                    .setIsEntryPoint(isEntryPoint);
        }

    }

    /**
     * Add a service to this application 
     * @param ServicePK - the service you wish to add
     * @param entryPoint - is this service an entry point?
     * @return AppServiceLocal
     * @throws ValidationException - if service is already present 
     * @throws CreateException - couldnt store record
     */
    protected AppServiceLocal addService(ServicePK aService, boolean entryPoint) 
        throws CreateException, NamingException {
            // first create the AppService
            AppServiceLocal appService = AppServiceUtil.getLocalHome()
                .create(aService, new ApplicationPK(getId()), entryPoint);
            return appService;
    }

    /**
     * Get the non-CMR'd value object for this Application
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ApplicationValue getApplicationValueObject() {
        ApplicationValue vo = new ApplicationValue();
        vo.setName(getName());
        vo.setSortName(getSortName());
        vo.setModifiedBy(getModifiedBy());
        vo.setOwner(getOwner());
        vo.setLocation(getLocation());
        vo.setEngContact(getEngContact());
        vo.setOpsContact(getOpsContact());
        vo.setBusinessContact(getBusinessContact());
        vo.setDescription(getDescription());
        vo.setId(((ApplicationPK)this.getSelfLocal()
            .getPrimaryKey()).getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        if ( getApplicationType() != null )
            vo.setApplicationType( getApplicationType().getApplicationTypeValue() );
        else
            vo.setApplicationType( null );
        return vo;
    }

    /**
     * Get a snapshot set of app service locals
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getAppServiceSnapshot() {
        return new LinkedHashSet(getAppServices());
    }

    /**
     * The create method
     * @param appValue The application value object
     * @param services A collection of ServiceValue objects that will be
     * the initial set of services for the application.  This can be
     * null if you are creating an empty application.
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public ApplicationPK ejbCreate(
        org.hyperic.hq.appdef.shared.ApplicationValue appValue)
        throws CreateException, ValidationException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            // first we validate this thing
            validateNewApplication(appValue);
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(appValue.getName());
            if (appValue.getName()!=null)
                setSortName(appValue.getName().toUpperCase());
            setDescription(appValue.getDescription());
            setModifiedBy(appValue.getModifiedBy());
            setOwner(appValue.getOwner());
            setLocation(appValue.getLocation());
            setBusinessContact(appValue.getBusinessContact());
            setOpsContact(appValue.getOpsContact());
            setEngContact(appValue.getEngContact());
            if(log.isDebugEnabled()) {
                log.debug("Finished ejbCreate");
            }
            return null;
    }

    public void ejbPostCreate(
        org.hyperic.hq.appdef.shared.ApplicationValue appValue)
        throws CreateException, ValidationException {
        try {
            ApplicationTypeLocal appType;
            // check for application type
            if (appValue.getApplicationType() == null) {
                // set to generic type
                appType = ApplicationTypeUtil.getLocalHome()
                    .findByName(AppdefEntityConstants.GENERIC_APPLICATION_TYPE);
            } else {
                appType = ApplicationTypeUtil.getLocalHome()
                    .findByPrimaryKey(appValue.getApplicationType()
                        .getPrimaryKey()); 
            }
            this.setApplicationType(appType);
        } catch (Exception e) {
            throw new CreateException("Unable to set application type: "  + e.getMessage());
        }
    }

    /**
     * Validate a new application against its defined type, etc.
     * @param ApplicationValue - the app to validate
     * @exception ValidationException - if it fails
     */
    private void validateNewApplication(ApplicationValue newApp)
        throws ValidationException {
            String msg = null;
            // finally deal with the mess
            if(msg != null) {
                throw new ValidationException(msg);
            }
    }

    /**
     * Get the entity Id
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public AppdefEntityID getEntityId() {
        if(eid == null) {
            eid = new AppdefEntityID(
                            AppdefEntityConstants.APPDEF_TYPE_APPLICATION, 
                            getId().intValue());
        } else if (!eid.getId().equals(getId())) {
            // Sometimes the id object can get stale if this entity bean is 
            // being reused from the bean pool and was previously used by
            // a different object.
            eid = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_APPLICATION, 
                        getId().intValue());
        }
        return eid;
    }

    public void ejbActivate() throws RemoteException
    {
    }

    public void ejbPassivate() throws RemoteException
    {
    }

    public void ejbLoad() throws RemoteException
    {
    }

    public void ejbStore() throws RemoteException
    {
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoteException, RemoveException
    {
    }

}

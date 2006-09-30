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

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppServiceLocal;
import org.hyperic.hq.appdef.shared.AppServiceUtil;
import org.hyperic.hq.appdef.shared.AppSvcDependencyLocal;
import org.hyperic.hq.appdef.shared.AppSvcDependencyUtil;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ConfigResponseLocal;
import org.hyperic.hq.appdef.shared.ConfigResponseUtil;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerLocalHome;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerUtil;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeLocalHome;
import org.hyperic.hq.appdef.shared.ServiceTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypeUtil;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ServiceEJB implementaiton.
 * @ejb:bean name="Service"
 *      jndi-name="ejb/appdef/Service"
 *      local-jndi-name="LocalService"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *       
 * @ejb:interface local-extends="org.hyperic.hq.appdef.shared.AppdefResourceLocal"
 * 
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(s) FROM Service AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByParent(java.lang.Integer parentId)"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.parentId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByParentAndType(java.lang.Integer parentId, java.lang.Integer typeId)"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.parentId = ?1 AND s.serviceType.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s ORDER BY s.sortName DESC"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderCtime_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderCtime_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s ORDER BY s.cTime"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderCtime_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderCtime_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s ORDER BY s.cTime DESC"
 *
 * @ejb:finder signature="java.util.Collection findByType(java.lang.Integer st)"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceType.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE LCASE(s.name) = LCASE(?1)"
 * 
 * @ejb:finder signature="java.util.Collection findByPlatform_orderName_asc(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findByPlatform_orderName_asc (java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1 ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByPlatform_orderName_desc(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findByPlatform_orderName_desc (java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1 ORDER BY s.sortName desc"
 *
 * @ejb:finder signature="java.util.Collection findByPlatform_orderType_asc(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findByPlatform_orderType_asc (java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1 ORDER BY s.serviceType.sortName, s.sortName "
 *
 * @ejb:finder signature="java.util.Collection findByPlatform_orderType_desc(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findByPlatform_orderType_desc (java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as s WHERE p.id = ?1 ORDER BY s.serviceType.sortName desc, s.sortName"
 *
 * @ejb:finder signature="java.util.Collection findPlatformServices_orderName(java.lang.Integer platId,boolean b)"
 *      query="SELECT OBJECT(svc) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as svc WHERE p.id = ?1 AND serv.serverType.virtual = ?2"
 * @jboss:query signature="java.util.Collection findPlatformServices_orderName(java.lang.Integer platId,boolean b)"
 *      query="SELECT OBJECT(svc) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as svc WHERE p.id = ?1 AND serv.serverType.virtual = ?2 ORDER BY svc.sortName"
 * 
 * @ejb:finder signature="java.util.Collection findPlatformServices_orderName_desc(java.lang.Integer platId,boolean b)"
 *      query="SELECT OBJECT(svc) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as svc WHERE p.id = ?1 AND serv.serverType.virtual = ?2"
 * @jboss:query signature="java.util.Collection findPlatformServices_orderName_desc(java.lang.Integer platId,boolean b)"
 *      query="SELECT OBJECT(svc) FROM Platform as p, IN(p.servers) as serv, IN(serv.services) as svc WHERE p.id = ?1 AND serv.serverType.virtual = ?2 ORDER BY svc.sortName DESC"
 *
 * @ejb:finder signature="java.util.List findByServer_orderName(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByServer_orderName(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1 ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.List findByServer_orderType(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByServer_orderType(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1 ORDER BY s.serviceType.sortName"
 *
 * @ejb:finder signature="java.util.List findByServerAndType_orderName(java.lang.Integer id, java.lang.Integer tid)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1 AND s.serviceType.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByServerAndType_orderName(java.lang.Integer id, java.lang.Integer tid)"
 *      query="SELECT OBJECT(s) FROM Service as s WHERE s.server.id = ?1 AND s.serviceType.id = ?2 ORDER BY s.sortName"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ServiceLocal findByApplication(org.hyperic.hq.appdef.shared.ApplicationLocal app)"
 *      query="SELECT OBJECT(s) FROM Service AS s, IN (s.appServices) AS appsvc WHERE appsvc.application = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByCluster(java.lang.Integer clusterId)"
 *      query="SELECT OBJECT(s) FROM Service AS s, ServiceCluster c WHERE s.serviceCluster = c AND c.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAllClusterUnassigned_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllClusterUnassigned_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL ORDER BY s.sortName"
 * 
 * @ejb:finder signature="java.util.Collection findAllClusterUnassigned_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllClusterUnassigned_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL ORDER BY s.sortName DESC"
 * 
 * @ejb:finder signature="java.util.Collection findAllClusterAppUnassigned_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllClusterAppUnassigned_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL AND s.appServices IS EMPTY ORDER BY s.sortName"
 * 
 * @ejb:finder signature="java.util.Collection findAllClusterAppUnassigned_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllClusterAppUnassigned_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Service AS s WHERE s.serviceCluster IS NULL AND s.appServices IS EMPTY ORDER BY s.sortName DESC"
 *
 * @ejb:value-object name="ServiceLight" match="light" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *      cacheable="true" cacheDuration="600000"
 * @ejb:value-object name="Service" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SERVICE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ServiceEJBImpl 
    extends ServiceBaseBean
    implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_APPDEF_RESOURCE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ServiceEJBImpl.class.getName());

    public ServiceEJBImpl() {}

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
     * Is this Service an autodiscovery zombie?  Zombies
     * are services that still exist in appdef, but are marked
     * zombies because runtime autodiscovery no longer sees
     * them in its scans
     * @ejb:persistent-field
     * @jboss:column-name name="AUTODISCOVERY_ZOMBIE"
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract boolean getAutodiscoveryZombie();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAutodiscoveryZombie(boolean z);

    /**
     * Does this service have service-level RT enabled?
     * @ejb:persistent-field
     * @jboss:column-name name="SERVICE_RT"
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract boolean getServiceRt();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceRt(boolean z);

    /**
     * Does this service have end-user RT enabled?
     * @ejb:persistent-field
     * @jboss:column-name name="ENDUSER_RT"
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract boolean getEndUserRt();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setEndUserRt(boolean z);

    /**
     * modified by of this service
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MODIFIED_BY"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getModifiedBy();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setModifiedBy(java.lang.String comment);

    /**
     * owner of this service
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="OWNER"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getOwner();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setOwner(java.lang.String owner);

    /**
     * Location of this service
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="LOCATION"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getLocation();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setLocation(java.lang.String location);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="CONFIG_RESPONSE_ID"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.Integer getConfigResponseId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setConfigResponseId (java.lang.Integer crif);

    /**
     * Get the light value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * @jboss:read-only true
     */
    public abstract ServiceLightValue getServiceLightValue();

    /**
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * @jboss:read-only true
     */
    public abstract ServiceValue getServiceValue();

    /**
     * Set the value object. This method does *NOT* update any of the CMR's
     * included in the value object. This is for speed/locking reasons
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void updateService( org.hyperic.hq.appdef.shared.ServiceValue valueHolder ) {
         setDescription( valueHolder.getDescription() );
         setAutodiscoveryZombie( valueHolder.getAutodiscoveryZombie() );
         setServiceRt( valueHolder.getServiceRt() );
         setEndUserRt( valueHolder.getEndUserRt() );
         setModifiedBy( valueHolder.getModifiedBy() );
         setOwner( valueHolder.getOwner() );
         setLocation( valueHolder.getLocation() );
         setParentId( valueHolder.getParentId() );
         setName( valueHolder.getName() );
         setMTime( valueHolder.getMTime() );
         setCTime( valueHolder.getCTime() );
    }
    
    /** Parent ID of service which nests this service
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="parent_service_id"
     * @jboss:read-only true
     */
    public abstract Integer getParentId();

    /** Setter for property parentId.
     *
     * @ejb:interface-method
     */
    public abstract void setParentId(Integer parentId);

    /**
     * Get the Server that owns this Service
     * @ejb:value-object
     *      compose="org.hyperic.hq.appdef.shared.ServerLightValue"
     *      compose-name="Server"
     * @ejb:interface-method
     * @ejb:relation
     *      name="Server-Service"
     *      role-name="one-Service-has-one-Server"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="server_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServerLocal getServer();

    /**
     * Set the Server that owns this Service. 
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServer(org.hyperic.hq.appdef.shared.ServerLocal server);

    /**
     * Get the ServiceCluster that this Service belongs to
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceCluster-Service"
     *      role-name="one-Service-has-one-ServiceCluster"
     * @ejb:value-object
     *      compose="org.hyperic.hq.appdef.shared.ServiceClusterValue"
     *      compose-name="ServiceCluster"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="svc_cluster_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServiceClusterLocal getServiceCluster();

    /**
     * Set the ServiceCluster that this Service belongs to.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceCluster(org.hyperic.hq.appdef.shared.ServiceClusterLocal serviceCluster);

    /**
     * Get the AppServices for this Service
     * @ejb:interface-method
     * @ejb:relation
     *      name="Service-AppService"
     *      role-name="one-Service-has-many-AppServices"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.util.Set getAppServices();

    /**
     * Set the AppServices for this Service
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAppServices(java.util.Set svcs);

    /**
     * Get the ServiceType of this Service
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceType-Service"
     *      role-name="one-Service-has-one-ServiceType"
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
     * Compare this entity bean to a value object
     * @return true if the service value matches this entity
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public boolean matchesValueObject(ServiceValue obj) {
        boolean matches = true;
        matches = super.matchesValueObject(obj) && 
            (this.getName() != null ? this.getName().equals(obj.getName()) 
                : (obj.getName() == null)) &&
            (this.getDescription() != null ? 
                this.getDescription().equals(obj.getDescription()) 
                : (obj.getDescription() == null)) &&
            (this.getLocation() != null ? 
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) &&
            (this.getOwner() != null ? this.getOwner().equals(obj.getOwner())
                : (obj.getOwner() == null)) &&
            (this.getEndUserRt() == obj.getEndUserRt()) &&
            (this.getServiceRt() == obj.getServiceRt());
        return matches;
    }
    
    /**
     * Add a dependent service in context of an application
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
        // look for the app service for **this** Service 
        try {
            appSvc = AppServiceUtil.getLocalHome()
                .findByAppAndService(appPK.getId(), this.getId());
        } catch (FinderException e) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK.getId() + " Service: " + getId());
            appSvc =
                AppServiceUtil.getLocalHome().create(
                    new ServicePK(getId()), appPK, true);
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
     * Add a dependent service cluster in context of an application
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
        // look for the app service for **this** Service
        try {
            appSvc = AppServiceUtil.getLocalHome()
                .findByAppAndService(appPK.getId(), this.getId());
        } catch (FinderException e) {
            // didnt find it... create it.
            log.debug(
                "Creating new app service object for Application: "
                + appPK.getId() + " Service: " + getId());
            appSvc =
                AppServiceUtil.getLocalHome().create(
                    new ServicePK(getId()), appPK, true);
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


    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public ServicePK ejbCreate(ServiceValue sv, ServerPK parentPK)
        throws CreateException {
        if(log.isDebugEnabled()) {
            log.debug("Begin ejbCreate");
        }
        super.ejbCreate(ctx, SEQUENCE_NAME, sv.getName());
        if (sv.getName()!=null)
            setSortName(sv.getName().toUpperCase());
        setAutodiscoveryZombie(false);
        setServiceRt(false);
        setEndUserRt(false);
        setDescription(sv.getDescription());
        setModifiedBy(sv.getModifiedBy());
        setLocation(sv.getLocation());
        setOwner(sv.getOwner());
        setParentId(sv.getParentId());
        if(sv.getName() != null) setSortName(sv.getName().toUpperCase());
        if(log.isDebugEnabled()) {
            log.debug("Completed ejbCreate");
        }
        return null;
    }

    public void ejbPostCreate(ServiceValue sv, ServerPK parentPK)
        throws CreateException {
        ServiceTypePK stpk = null;
        try {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbPostCreate");
            }
            // look up the service type so we can set it correctly
            ServiceTypeLocalHome stHome = ServiceTypeUtil.getLocalHome();
            ServiceTypeValue stv = sv.getServiceType();
            stpk = stv.getPrimaryKey();
            ServiceTypeLocal stLocal = stHome.findByPrimaryKey(stpk);
            setServiceType(stLocal);

            // look up the parent server
            ServerLocalHome sLHome = ServerUtil.getLocalHome();
            ServerLocal parentServer = sLHome.findByPrimaryKey(parentPK);
            setServer(parentServer);

            // Setup config response entries
            ConfigResponseLocal cLocal =
                ConfigResponseUtil.getLocalHome().create();
            setConfigResponseId(cLocal.getId());

            if(log.isDebugEnabled()) {
                log.debug("Completed ejbPostCreate");
            }

        } catch (NamingException e) {
            log.error("Unable to get ServiceTypeLocalHome in ejbPostCreate", e);
            throw new CreateException("Unable to find ServiceTypeLocalHome");
        } catch (FinderException e) {
            log.error("Unable to find ServiceType: " + stpk, e);
            throw new CreateException("Unable to find ServiceType: " + stpk);
        }
    }

    /**
     * Get prim key for current instance
     */
    private ServicePK getPK() {
        return new ServicePK(this.getId());
    }
    /** Get an upcasted reference to our resource type value.
     * @return the "type value" value object upcasted to its
     *         abstract base class for use in agnostic context. */
    public AppdefResourceTypeValue getAppdefResourceTypeValue () {
        return (AppdefResourceTypeValue) getServiceType().getServiceTypeValue();
    }

    /**
     * @return the AppdefResourceType
     */
    public javax.ejb.EJBLocalObject getAppdefResourceType()
    {
        return this.getServiceType();
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

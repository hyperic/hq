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

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import java.rmi.RemoteException;
import org.hyperic.hq.appdef.shared.ApplicationUtil;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ApplicationLocal;
import org.hyperic.hq.appdef.shared.ApplicationLocalHome;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppServicePK;
import org.hyperic.hq.appdef.shared.ServiceLocalHome;
import org.hyperic.hq.appdef.shared.ServiceLocal;
import org.hyperic.hq.appdef.shared.ServiceClusterLocal;
import org.hyperic.hq.appdef.shared.ServiceClusterUtil;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ServiceUtil;
import org.hyperic.hq.common.shared.HQConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AppServiceEJB implementaiton.
 * @ejb:bean name="AppService"
 *      jndi-name="ejb/appdef/AppService"
 *      local-jndi-name="LocalAppService"
 *      view-type="both"
 *      type="CMP"
 *      cmp-version="2.x"

 * @ejb:finder 
        signature="java.util.List findByApplication_orderName (java.lang.Integer id)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
        signature="java.util.List findByApplication_orderName (java.lang.Integer id)"
 *      distinct="true"
 *      additional-columns=", res.name"
 *      from=", EAM_RESOURCE res, EAM_RESOURCE_TYPE t "
 *      where=" EAM_APP_SERVICE.application_id={0} AND (
                ( res.resource_type_id = t.id AND t.name = 'covalentAuthzResourceGroup' 
                  AND EAM_APP_SERVICE.cluster_id IN 
                    ( SELECT id FROM EAM_SVC_CLUSTER c where c.group_id = res.instance_id )
                )
                OR 
                ( res.instance_id=EAM_APP_SERVICE.service_id 
                  AND res.resource_type_id=t.id AND t.name = 'covalentEAMService')
                )"
 *      order="res.name"
 *
 * @ejb:finder signature="java.util.List findByApplication_orderType(java.lang.Integer id)"
        query=""
 *      unchecked="true"
 * @jboss:query signature="java.util.List findByApplication_orderType(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap where a.application = ap
               AND ap.id = ?1 ORDER BY a.serviceType.name asc"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(i) FROM AppService AS i"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.Collection findByApplication(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(i) FROM AppService AS i, Application AS ap where i.application = ap AND ap.id = ?1"
 *      unchecked="true"

 * @ejb:finder signature="java.util.Collection findByApplication_orderSvcName_asc(java.lang.Integer id)"
        query=""
 *      unchecked="true"
 * @jboss:query signature="java.util.Collection findByApplication_orderSvcName_asc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1 ORDER BY s.name asc"
 *
 * @ejb:finder signature="java.util.Collection findByApplication_orderSvcName_desc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1"
 *      unchecked="true"
 * @jboss:query signature="java.util.Collection findByApplication_orderSvcName_desc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1 ORDER BY s.name desc"
 *
 * @ejb:finder signature="java.util.Collection findByApplication_orderSvcType_asc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1"
 *      unchecked="true"
 * @jboss:query signature="java.util.Collection findByApplication_orderSvcType_asc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1 ORDER BY s.serviceType.name, s.name"
 *
 * @ejb:finder signature="java.util.Collection findByApplication_orderSvcType_desc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1"
 *      unchecked="true"
 * @jboss:query signature="java.util.Collection findByApplication_orderSvcType_desc(java.lang.Integer id)"
        query="SELECT DISTINCT OBJECT(a) FROM AppService AS a, Application AS ap, Service as s where a.application = ap AND a.service=s
               AND ap.id = ?1 ORDER BY s.serviceType.name desc, s.name"
 *
 * @ejb:finder signature="java.util.Collection findEntryPointsByApp(java.lang.Integer appId)"
        query="SELECT DISTINCT OBJECT(appSvc) FROM AppService AS appSvc, Application AS app where appSvc.isEntryPoint = TRUE AND appSvc.application = app AND app.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AppServiceLocal findByAppAndService(java.lang.Integer appId, java.lang.Integer svcId)"
 *      query="SELECT DISTINCT OBJECT(appSvc) FROM AppService AS appSvc, Application AS app, Service AS svc WHERE appSvc.service = svc AND appSvc.application = app AND app.id = ?1 AND svc.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AppServiceLocal findByAppAndCluster(java.lang.Integer appId, java.lang.Integer svcClusterId)"
 *      query="SELECT DISTINCT OBJECT(appSvc) FROM AppService AS appSvc, Application AS app, ServiceCluster AS cluster WHERE appSvc.isCluster = true AND appSvc.serviceCluster = cluster AND appSvc.application = app AND app.id = ?1 AND cluster.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:value-object name="AppService" match="*" instantiation="eager"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_APP_SERVICE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AppServiceEJBImpl 
    extends AppdefEntityBean implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_APP_SERVICE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;
    public final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    public final String ctx = AppServiceEJBImpl.class.getName();
    protected Log log = LogFactory.getLog(ctx);

    public AppServiceEJBImpl() {}

    /**
     * The create method using service and application PKs.
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public AppServicePK ejbCreate(org.hyperic.hq.appdef.shared.ServicePK 
        servicePK, org.hyperic.hq.appdef.shared.ApplicationPK 
        appPK, boolean entryPoint) throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setIsEntryPoint(entryPoint);
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.ServicePK 
        servicePK, org.hyperic.hq.appdef.shared.ApplicationPK appPK,
        boolean entryPoint) throws CreateException {
        try { 
            ApplicationLocalHome appLHome = ApplicationUtil.getLocalHome();
            ServiceLocalHome svLHome = ServiceUtil.getLocalHome();
            ServiceLocal service = svLHome.findByPrimaryKey(servicePK);
            ApplicationLocal app = appLHome.findByPrimaryKey(appPK);
            this.setApplication(app);
            this.setService(service);
            this.setServiceType(service.getServiceType());
        } catch (NamingException e) {
                log.error("Unable to get LocalHome in ejbPostCreate", e);
                throw new CreateException("Unable to get LocalHome in ejbPostCreate");
        } catch (FinderException e) {
                log.error("Unable to find Dependent Object in ejbPostCreate" , e);
                throw new CreateException("Unable to find dependentObject in ejbPostCreate" + e.getMessage());
        }
    }

    /**
     * The create method using servicecluster and application PKs.
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public AppServicePK ejbCreate(ServiceClusterPK clusterPK, 
                                  ApplicationPK appPK) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(ServiceClusterPK clusterPK,
                              ApplicationPK appPK) 
        throws CreateException {
        try { 
            ApplicationLocalHome appLHome = ApplicationUtil.getLocalHome();
            ServiceClusterLocal cluster = ServiceClusterUtil.getLocalHome()
                                                .findByPrimaryKey(clusterPK);
            ApplicationLocal app = appLHome.findByPrimaryKey(appPK);
            this.setApplication(app);
            this.setIsCluster(true);
            this.setServiceCluster(cluster);
            this.setServiceType(cluster.getServiceType());
        } catch (NamingException e) {
                log.error("Unable to get LocalHome in ejbPostCreate", e);
                throw new CreateException("Unable to get LocalHome in ejbPostCreate");
        } catch (FinderException e) {
                log.error("Unable to find Dependent Object in ejbPostCreate" , e);
                throw new CreateException("Unable to find dependentObject in ejbPostCreate" + e.getMessage());
        }
    }

    /**
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract AppServiceValue getAppServiceValue();

    /**
     * Set the value object
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAppServiceValue(AppServiceValue value);

    /**
     * Get the application that uses this service
     * @ejb:interface-method
     * @ejb:relation
     *      name="Application-AppService"
     *      role-name="one-AppService-has-one-Application"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="application_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ApplicationLocal getApplication();

    /**
     * Set the application for this appservice
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setApplication(org.hyperic.hq.appdef.shared.ApplicationLocal app);

    /**
     * Get the AppSvcDependency entities
     * for all this app service
     * @ejb:interface-method
     * @ejb:relation
     *      name="AppService-DepMap"
     *      role-name="one-AppService-has-many-depMaps"
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.util.Set getAppSvcDependencies();

    /**
     * Set the dependency objects
     * @ejb:interface-method
     */
    public abstract void setAppSvcDependencies(java.util.Set deps);

    /**
     * Get the service parent of this appservice
     * @ejb:interface-method
     * @ejb:relation
     *      name="Service-AppService"
     *      role-name="one-AppService-has-one-Service"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="service_id"
     *      related-pk-field="id"
     * @ejb:value-object
     *      match="*"
     *      compose="org.hyperic.hq.appdef.shared.ServiceLightValue"
     *      compose-name="Service"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServiceLocal getService();

    /**
     * Set the service parent of this appservice
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setService(org.hyperic.hq.appdef.shared.ServiceLocal service);

    /**
     * Get the cluster for this appservice
     * @ejb:interface-method
     * @ejb:relation
     *      name="AppService-ServiceCluster"
     *      role-name="one-AppService-has-one-ServiceCluster"
     *      target-ejb="ServiceCluster"
     *      target-role-name="one-Cluster-has-many-ServiceClusters"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="cluster_id"
     *      related-pk-field="id"
     * @ejb:value-object
     *      match="*"
     *      compose="org.hyperic.hq.appdef.shared.ServiceClusterValue"
     *      compose-name="ServiceCluster"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServiceClusterLocal getServiceCluster();

    /**
     * Set the cluster for this appservice
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceCluster(org.hyperic.hq.appdef.shared.ServiceClusterLocal cluster);

    /**
     * Is this appservice a cluster?
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract boolean getIsCluster();

    /** @ejb:interface-method */
    public abstract void setIsCluster(boolean isCluster);

    /**
     * is this an entry point for the application?
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="FENTRY_POINT"
     */
    public abstract boolean getIsEntryPoint();
    /** 
     * @ejb:interface-method 
     */
    public abstract void setIsEntryPoint(boolean isEntryPoint);

    /**
     * Get the ServiceType of this AppService
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceType-AppService"
     *      role-name="one-AppService-has-one-ServiceType"
     *      cascade-delete="yes"
     *      target-ejb="ServiceType"
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


    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

}

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
import javax.ejb.RemoveException;
import java.rmi.RemoteException;
import java.util.*;
import org.hyperic.util.*;
import org.hyperic.hq.appdef.shared.AppSvcDependencyPK;
import org.hyperic.hq.appdef.shared.AppServiceLocal;
import org.hyperic.hq.common.shared.HQConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AppSvcDependencyEJB implementaiton.
 * @ejb:bean name="AppSvcDependency"
 *      jndi-name="ejb/appdef/AppSvcDependency"
 *      local-jndi-name="LocalAppSvcDependency"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *       
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(s) FROM AppSvcDependency AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AppSvcDependencyLocal findByDependentAndDependor(java.lang.Integer appsvcId, java.lang.Integer depAppSvcId)"
 *      query="SELECT OBJECT(dep) FROM AppSvcDependency AS dep
               WHERE dep.appService.id = ?1 AND 
               dep.dependentService.id = ?2"
 *      unchecked="true"
 *
 * @ejb:finder signature="java.util.Collection findByAppService(java.lang.Integer appSrvId)"
 *      query="SELECT DISTINCT OBJECT(dep) FROM AppSvcDependency AS dep, AppService as srv WHERE dep.appService = srv AND srv.id = ?1"
 *      unchecked="true"
 * 
 * @ejb:finder signature="java.util.Collection findByApplication(java.lang.Integer appId)"
 *      query="SELECT DISTINCT OBJECT(dep) FROM AppSvcDependency AS dep, AppService AS srv, Application AS app WHERE dep.appService = srv AND srv.application = app AND app.id = ?1"
 *      unchecked="true"

 *
 * @ejb:finder signature="java.util.Collection findByAppAndService(java.lang.Integer appId, java.lang.Integer serviceId)"
 *      query="SELECT DISTINCT OBJECT(dep) FROM AppSvcDependency AS dep, Application AS app, Service as svc, AppService AS appSvc WHERE dep.appService = appSvc AND appSvc.service = svc AND appSvc.application = app AND app.id = ?1 AND svc.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SERVICE_DEP_MAP"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AppSvcDependencyEJBImpl extends AppdefEntityBean
    implements EntityBean {

    protected Log log = LogFactory.getLog(
        "org.hyperic.hq.appdef.server.entity.AppSvcDependencyEJBImpl");
    public final String SEQUENCE_NAME = "EAM_SERVICE_DEP_MAP_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;
    public final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    public final String ctx = AppSvcDependencyEJBImpl.class.getName();

    public AppSvcDependencyEJBImpl() {}

    /**
     * Get the dependent Appservice
     * @ejb:interface-method
     * @ejb:relation
     *      name="AppService-DepAppService"
     *      role-name="one-AppServiceDep-has-one-DepAppService"
     *      target-ejb="AppService"
     *      target-role-name="one-AppService-has-many-DepAppServices"
     *      target-multiple="yes"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="DEPENDENT_SERVICE_ID"
     *      related-pk-field="id"
     */
    public abstract org.hyperic.hq.appdef.shared.AppServiceLocal
        getDependentService();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDependentService(
        org.hyperic.hq.appdef.shared.AppServiceLocal service);

    /**
     * Get the AppServices for this Service
     * @ejb:interface-method
     * @ejb:relation
     *      name="AppService-DepMap"
     *      role-name="one-DepMap-has-one-AppService"
     * @jboss:relation
     *      fk-column="APPSERVICE_ID"
     *      related-pk-field="id"
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract org.hyperic.hq.appdef.shared.AppServiceLocal getAppService();

    /**
     * Set the AppService for this DepMap
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAppService(org.hyperic.hq.appdef.shared.AppServiceLocal appSvc);

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public AppSvcDependencyPK ejbCreate(AppServiceLocal appSvc,
        AppServiceLocal depSvc) throws CreateException {
        if(log.isDebugEnabled()) {
            log.debug("Begin ejbCreate");
        }
        super.ejbCreate(ctx, SEQUENCE_NAME);
        if(log.isDebugEnabled()) {
            log.debug("Completed ejbCreate");
        }
        return null;
    }

    public void ejbPostCreate(AppServiceLocal appSvc,
        AppServiceLocal depSvc)
        throws CreateException {
        log.debug("In ejbPostCreate");
        setAppService(appSvc);
        log.debug("Added App Service: " + appSvc);
        setDependentService(depSvc);
        log.debug("Added DependentService: " + depSvc);
    }

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

}

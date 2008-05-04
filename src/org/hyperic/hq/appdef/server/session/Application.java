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

package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.authz.HasAuthzOperations;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class Application extends AppdefResource
    implements HasAuthzOperations
{
    private static final Map _authOps;
    static {
        _authOps = new HashMap();
        
        _authOps.put("create",       AuthzConstants.appOpCreateApplication);
        _authOps.put("modify",       AuthzConstants.appOpModifyApplication);
        _authOps.put("remove",       AuthzConstants.appOpRemoveApplication);
        _authOps.put("view",         AuthzConstants.appOpViewApplication);
        _authOps.put("monitor",      AuthzConstants.appOpMonitorApplication);
        _authOps.put("control",      AuthzConstants.appOpControlApplication);
        _authOps.put("manageAlerts", AuthzConstants.appOpManageAlerts);
    }
    
    private String _engContact;
    private String _opsContact;
    private String _businessContact;
    private ApplicationType _applicationType;
    private Collection _appServices;
    private Resource _resource;

    /**
     * default constructor
     */
    public Application()
    {
        super();
    }

    public Application(Integer id)
    {
        super();
        setId(id);
    }

    public AppdefEntityID getEntityId()
    {
        return AppdefEntityID.newAppID(getId());
    }

    public String getEngContact()
    {
        return _engContact;
    }

    void setEngContact(String engContact)
    {
        _engContact = engContact;
    }

    public String getOpsContact()
    {
        return _opsContact;
    }

    void setOpsContact(String opsContact)
    {
        _opsContact = opsContact;
    }

    public String getBusinessContact()
    {
        return _businessContact;
    }

    void setBusinessContact(String businessContact)
    {
        _businessContact = businessContact;
    }

    public ApplicationType getApplicationType()
    {
        return _applicationType;
    }

    void setApplicationType(ApplicationType applicationType)
    {
        _applicationType = applicationType;
    }

    /**
     * @return the resource
     */
    public Resource getResource() {
        return _resource;
    }

    /**
     * @param resource the resource to set
     */
    void setResource(Resource resource) {
        _resource = resource;
    }

    public Collection getAppServices()
    {
        return _appServices;
    }

    void setAppServices(Collection appServices)
    {
        _appServices = appServices;
    }

    public AppService addEntryPoint(Integer aService)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }

    public AppService addServiceCluster(Integer aClusterPK)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }

    public AppService addService(Integer aService)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }
    
    void removeService(AppService service) {
        _appServices.remove(service);
    }

    public Set getAppServiceSnapshot()
    {
        if (getAppServices() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getAppServices());
    }

    private ApplicationValue applicationValue = new ApplicationValue();
    /**
     * @deprecated use (this) Application object instead
     * @return
     */
    public ApplicationValue getApplicationValue()
    {
        applicationValue.setName(getName());
        applicationValue.setSortName(getSortName());
        applicationValue.setModifiedBy(getModifiedBy());
        applicationValue.setOwner(getResource().getOwner().getName());
        applicationValue.setLocation(getLocation());
        applicationValue.setEngContact(getEngContact());
        applicationValue.setOpsContact(getOpsContact());
        applicationValue.setBusinessContact(getBusinessContact());
        applicationValue.setDescription(getDescription());
        applicationValue.setId(getId());
        applicationValue.setMTime(getMTime());
        applicationValue.setCTime(getCTime());
        applicationValue.removeAllAppServiceValues();
        if (getAppServices() != null) {
            Iterator iAppServiceValue = getAppServices().iterator();
            while (iAppServiceValue.hasNext()){
                applicationValue.addAppServiceValue(
                    ((AppService)iAppServiceValue.next()).getAppServiceValue() );
            }
        }
        applicationValue.cleanAppServiceValue();
        if ( getApplicationType() != null )
            applicationValue.setApplicationType(getApplicationType());
        else
            applicationValue.setApplicationType( null );
        return applicationValue;
    }

    public ApplicationValue getApplicationValueObject()
    {
        ApplicationValue vo = new ApplicationValue();
        vo.setName(getName());
        vo.setSortName(getSortName());
        vo.setModifiedBy(getModifiedBy());
        vo.setOwner(getResource().getOwner().getName());
        vo.setLocation(getLocation());
        vo.setEngContact(getEngContact());
        vo.setOpsContact(getOpsContact());
        vo.setBusinessContact(getBusinessContact());
        vo.setDescription(getDescription());
        vo.setId(getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        if ( getApplicationType() != null )
            vo.setApplicationType(getApplicationType());
        else
            vo.setApplicationType( null );
        return vo;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof Application) && super.equals(obj);
    }

    public AppdefResourceType getAppdefResourceType() {
        return _applicationType;
    }

    public AppdefResourceValue getAppdefResourceValue() {
        return getApplicationValue();
    }

    protected String _getAuthzOp(String op) {
        return (String)_authOps.get(op);
    }
}

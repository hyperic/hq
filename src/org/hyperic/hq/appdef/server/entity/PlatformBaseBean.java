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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

/**
 * This is the base class for PlatformEJB and AIPlatformEJB
 */
public abstract class PlatformBaseBean extends AppdefEntityBean {

    public PlatformBaseBean () {}

    private AppdefEntityID id;

    /**
     * Certificate DN of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getCertdn();
    /**
     * @ejb:interface-method view-type="remote"
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setCertdn(java.lang.String certDN);

    /**
     * FQDN of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getFqdn();
    /**
     * @ejb:interface-method view-type="remote"
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setFqdn(java.lang.String fqdn);

    /**
     * Name of this Platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method view-type="both"
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setName(java.lang.String name);

    /**
     * Get the appdefEntityId for this platform
     * @throws CreateException
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public AppdefEntityID getEntityId() {
        if(id == null) {
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_PLATFORM, 
                        getId().intValue());
        } else if (!id.getId().equals(getId())) {
            // Sometimes the id object can get stale if this entity bean is 
            // being reused from the bean pool and was previously used by
            // a different object.
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_PLATFORM, 
                        getId().intValue());
        }
        return id;
    }

    /**
     * location of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="LOCATION"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.lang.String getLocation();
    /**
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setLocation(java.lang.String location);

    /**
     * Description of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method  
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setDescription(java.lang.String desc);

    /**
     * CPU count of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="CPU_COUNT"
     * @ejb:value-object match="light"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.Integer getCpuCount();
    /**
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setCpuCount(java.lang.Integer count);

    /**
     * Provided as a convenience to subclasses.  Note that this is not a
     * "real" ejbCreate method, just like the one in AppdefEntityBean.
     */
    protected void ejbCreate(String ctx, String sequenceName,
                             String fqdn, String certdn, String name) 
        throws CreateException {

        super.ejbCreate(ctx, sequenceName);
        setFqdn(fqdn);
        setCertdn(certdn);
        setName(name);
    }
 
}

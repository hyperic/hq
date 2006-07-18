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
 * This is the base class for ServiceEJB and AIServiceEJB
 */
public abstract class ServiceBaseBean extends AppdefEntityBean {

    public ServiceBaseBean () {}
    private AppdefEntityID id;

    /**
     * Name of this Service
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setName(java.lang.String name);

    /**
     * Description of this Service
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDescription(String desc);

    /**
     * Get the appdefEntityId for this service
     * @throws CreateException
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public AppdefEntityID getEntityId() {
        if (id == null) {
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVICE, 
                        getId().intValue());
        } else if (!id.getId().equals(getId())) {
            // Sometimes the id object can get stale if this entity bean is 
            // being reused from the bean pool and was previously used by
            // a different object.
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVICE, 
                        getId().intValue());
        }
        return id;
    }
    
    /**
     * Provided as a convenience to subclasses.  Note that this is not a
     * "real" ejbCreate method, just like the one in AppdefEntityBean.
     */
    protected void ejbCreate(String ctx, String sequenceName, String name) 
        throws CreateException {

        super.ejbCreate(ctx, sequenceName);
        setName(name);
    }
}

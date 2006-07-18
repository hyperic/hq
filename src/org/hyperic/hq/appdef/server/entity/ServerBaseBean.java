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
 * This is the base class for ServerEJB and AIServerEJB
 */
public abstract class ServerBaseBean extends AppdefEntityBean {

    public ServerBaseBean () {}
    private AppdefEntityID id;

    /**
     * Name of this Server
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setName(java.lang.String name);

    /**
     * This is some unique identifier for the server, so that we
     * can distinguish it from other servers on the same machine, 
     * without any knowledge of database keys, etc.  For most servers
     * the installpath will be sufficient to distinguish the server from 
     * other servers.  But for some servers, like websphere and weblogic,
     * the admin server and nodes share the same installpath, so their
     * plugins will provide some other way to provide a unique 
     * identifier to distinguish between them.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */ 
    public abstract java.lang.String getAutoinventoryIdentifier();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAutoinventoryIdentifier(java.lang.String aiid);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */ 
    public abstract java.lang.String getInstallPath();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setInstallPath(java.lang.String path);

    /**
     * Description of this Server
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(java.lang.String desc);

    /**
     * @return true if this server's services are automanaged by
     * runtime autodiscovery scans.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */

    public abstract boolean getServicesAutomanaged();
    /**
     * @ejb:interface-method
     */
    public abstract void setServicesAutomanaged(boolean servicesManaged);

    /**
     * Get the appdefEntityId for this server
     * @throws CreateException
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public AppdefEntityID getEntityId() {
        if(id == null) {
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVER, 
                        getId().intValue());
        } else if (!id.getId().equals(getId())) {
            // Sometimes the id object can get stale if this entity bean is 
            // being reused from the bean pool and was previously used by
            // a different object.
            id = new AppdefEntityID(
                        AppdefEntityConstants.APPDEF_TYPE_SERVER, 
                        getId().intValue());
        } 
        return id;
    }
    
    /**
     * Provided as a convenience to subclasses.  Note that this is not a
     * "real" ejbCreate method, just like the one in AppdefEntityBean.
     */
    protected void ejbCreate(String ctx, String sequenceName,
                             String installPath, 
                             String autoinventoryID,
                             boolean servicesAutomanaged,
                             String name) 
        throws CreateException {

        super.ejbCreate(ctx, sequenceName);
        setInstallPath(installPath);
        if (autoinventoryID != null) {
            setAutoinventoryIdentifier(autoinventoryID);
        } else {
            // This codepath should get executed if the
            // server is created via the GUI or CLI.  The
            // autoinventory identifier should only be 
            // set for servers discovered via AI.  For manual
            // creation, we just ensure the value doesn't collide
            // with anything else that might be reported by AI
            // or created by another user.
            String id = installPath
                + "_" + System.currentTimeMillis()
                + "_" + name;
            // XXX note, we truncate to 250 chars to ensure
            // that the data is not too long for the column.
            // If we change the column size in appdef-schema, this
            // should also be updated.
            id = id.substring(0, Math.min(id.length(), 250));
            setAutoinventoryIdentifier(id);
        }
        setServicesAutomanaged(servicesAutomanaged);
        setName(name);
    }
}

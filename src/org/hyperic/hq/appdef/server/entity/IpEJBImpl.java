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

import javax.ejb.*;

import java.rmi.RemoteException;
import java.util.*;
import org.hyperic.util.*;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.IpPK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the IpBean implementaiton.
 * @ejb:bean name="Ip"
 *      jndi-name="ejb/appdef/Ip"
 *      local-jndi-name="LocalIp"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *       
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(ip) FROM Ip AS ip"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="Ip" match="*" instantiation="eager"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_IP"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class IpEJBImpl extends IpBaseBean implements EntityBean {

    public final String ctx = IpEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_IP_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ctx);

    public IpEJBImpl() {}

    /**
     * Address of this Ip
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getAddress();
    /**
     * @ejb:interface-method
     */
    public abstract void setAddress(java.lang.String address);

    /**
     * Netmask of this Ip
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getNetmask();
    /**
     * @ejb:interface-method
     */
    public abstract void setNetmask(java.lang.String netmask);

    /**
     * Get the value object
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract IpValue getIpValue();

    /**
     * Set the value object
     * @ejb:interface-method
     */
    public abstract void setIpValue(IpValue val);

    /**
     * Get the platform for this object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="Platform-Ip"
     *      role-name="one-Ip-has-one-Platform"
     *      cascade-delete="yes"
     * @jboss:relation
     *      fk-column="platform_id"  
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.PlatformLocal getPlatform();
    
    /**
     * Set the platform for this object
     * @ejb:interface-method
     * @ejb:transation type="REQUIRED"
     */
    public abstract void setPlatform(org.hyperic.hq.appdef.shared.PlatformLocal platform);

    /**
     * The create method using the data object
     * @param IpValue
     * @return IpPK
     * @ejb:create-method
     */
    public IpPK ejbCreate(org.hyperic.hq.appdef.shared.IpValue ip) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME, ip.getAddress(), 
                ip.getNetmask(), ip.getMACAddress());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }

    public void ejbPostCreate(IpValue ip) throws CreateException {}
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

}

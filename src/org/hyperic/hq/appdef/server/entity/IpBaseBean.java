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

/**
 * This is the base class for IpEJB and AIIpEJB
 */
public abstract class IpBaseBean extends AppdefEntityBean {

    public IpBaseBean () {}

    /**
     * Address of this Ip
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @ejb:transaction type="REQUIRED"
     */
    public abstract java.lang.String getAddress();
    /**
     * @ejb:interface-method
     */
    public abstract void setAddress(java.lang.String address);

    /**
     * MAC address of this Ip
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MAC_ADDRESS"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract java.lang.String getMACAddress();
    /**
     * @ejb:interface-method
     */
    public abstract void setMACAddress(java.lang.String address);

    /**
     * Netmask of this Ip
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @ejb:transaction type="REQUIRED"
     */
    public abstract java.lang.String getNetmask();
    /**
     * @ejb:interface-method
     */
    public abstract void setNetmask(java.lang.String netmask);

    /**
     * Provided as a convenience to subclasses.  Note that this is not a
     * "real" ejbCreate method, just like the one in AppdefEntityBean.
     */
    protected void ejbCreate(String ctx, String sequence, 
                             String address, String netmask, 
                             String mac) 
        throws CreateException {

        super.ejbCreate(ctx, sequence);
        setAddress(address);
        setNetmask(netmask);
        setMACAddress(mac);
    }
}

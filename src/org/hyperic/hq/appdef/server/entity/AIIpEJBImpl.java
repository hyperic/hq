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
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIIpPK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AIIpBean implementaiton.
 * @ejb:bean name="AIIp"
 *      jndi-name="ejb/appdef/AIIp"
 *      local-jndi-name="LocalAIIp"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIIpLocal findByAddress(java.lang.String addr)"
 *      query="SELECT OBJECT(aip) FROM AIIp AS aip WHERE aip.address = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AIIp" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AIAppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AIQ_IP"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AIIpEJBImpl 
    extends IpBaseBean implements EntityBean {

    public final String ctx = "org.hyperic.hq.appdef.server.entity.IpBaseBean";
    public final String SEQUENCE_NAME = "EAM_AIQ_IP_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ctx);

    public AIIpEJBImpl() {}

    /**
     * Queue status of this IP.  This is one of the
     * AIQueueConstants.Q_STATUS_XXX constants indicating why this
     * IP is in the AI queue.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract int getQueueStatus();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setQueueStatus(int queueStatus);


    /**
     * Diff status.  This is a bitmask of the
     * AIQueueConstants.Q_IP_XXX constants indicating how this
     * ip has changed.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract long getDiff();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setDiff(long diff);

    /**
     * Indicates whether the user wishes to ignore changes
     * to this ip detected by autoinventory.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract boolean getIgnored();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setIgnored(boolean ignored);

    /**
     * Get the value object
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract AIIpValue getAIIpValue();

    /**
     * Set the value object
     * @ejb:interface-method
     */
    public abstract void setAIIpValue(AIIpValue val);

    /**
     * Get the platform for this object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="AIPlatform-AIIp"
     *      role-name="one-AIIp-has-one-AIPlatform"
     *      cascade-delete="yes"
     * @jboss:relation
     *      fk-column="aiq_platform_id"  
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.AIPlatformLocal getAIPlatform();
    
    /**
     * Set the platform for this object
     * @ejb:interface-method
     * @ejb:transation type="REQUIRED"
     */
    public abstract void setAIPlatform(org.hyperic.hq.appdef.shared.AIPlatformLocal platform);

    /**
     * The create method using the data object
     * @param IpValue
     * @return IpPK
     * @ejb:create-method
     */
    public AIIpPK ejbCreate(org.hyperic.hq.appdef.shared.AIIpValue ip) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, 
                            SEQUENCE_NAME,
                            ip.getAddress(), 
                            ip.getNetmask(),
                            ip.getMACAddress());

            setQueueStatus(ip.getQueueStatus());
            setDiff       (ip.getDiff());
            setIgnored    (ip.getIgnored());

            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.AIIpValue ip)
        throws CreateException {}
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

}

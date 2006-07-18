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
import javax.ejb.RemoveException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropKeyPK;
import org.hyperic.hq.appdef.shared.CPropKeyValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.IDGeneratorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="CPropKey"
 *      jndi-name="ejb/appdef/CPropKey"
 *      local-jndi-name="LocalCPropKey"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * @ejb:transaction type="REQUIRED"
 * @ejb:value-object name="CPropKey" match="*"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(k) FROM CPropKey AS k"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByAppdefType(int appdefType, int appdefId)"
 *      query="SELECT OBJECT(k) FROM CPropKey AS k WHERE k.appdefType=?1 AND k.appdefTypeId=?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.CPropKeyLocal findByKey(int appdefType, int appdefTypeId, java.lang.String key)"
 *      query="SELECT OBJECT(k) FROM CPropKey AS k WHERE k.appdefType=?1 AND k.appdefTypeId=?2 AND k.key=?3"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_CPROP_KEY"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class CPropKeyEJBImpl 
    implements EntityBean 
{
    private final String ctx             = CPropKeyEJBImpl.class.getName();
    private final String SEQUENCE_NAME   = "EAM_CPROP_KEY_ID_SEQ";
    private final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    private Log log = LogFactory.getLog(ctx);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer Id);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="APPDEF_TYPE"
     * @jboss:read-only true
     */
    public abstract int getAppdefType();

    /**
     * @ejb:interface-method
     */
    public abstract void setAppdefType(int appdefType);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="APPDEF_TYPEID"
     * @jboss:read-only true
     */
    public abstract int getAppdefTypeId();

    /**
     * @ejb:interface-method
     */
    public abstract void setAppdefTypeId(int appdefId);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="PROPKEY"
     * @jboss:read-only true
     */
    public abstract String getKey();

    /**
     * @ejb:interface-method
     */
    public abstract void setKey(String key);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="DESCRIPTION"
     * @jboss:read-only true
     */
    public abstract String getDescription();

    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(String key);

    /**
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract CPropKeyValue getCPropKeyValue();

    /**
     * @ejb:create-method
     */
    public CPropKeyPK ejbCreate(int appdefType, int appdefTypeId, String key,
                                String description)
        throws CreateException 
    {
        try {
            Integer id =
                new Integer((int)IDGeneratorFactory.getNextId(ctx, 
                                                              SEQUENCE_NAME,
                                                              DATASOURCE_NAME));
            setId(id);
            setAppdefType(appdefType);
            setAppdefTypeId(appdefTypeId);
            setKey(key);
            setDescription(description);
        } catch(Exception e){
            this.log.error("Unable to create CPropKey entry", e);
            throw new CreateException("Failed to create CPropKey entry: " +
                                      e.getMessage());
        }
        return null;
    }

    public void ejbPostCreate(CPropKeyValue val){}
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
}

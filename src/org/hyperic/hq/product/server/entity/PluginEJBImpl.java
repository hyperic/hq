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

package org.hyperic.hq.product.server.entity;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.product.shared.PluginPK;
import org.hyperic.hq.product.shared.PluginValue;
import org.hyperic.util.jdbc.IDGeneratorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="Plugin"
 *      jndi-name="ejb/product/Plugin"
 *      local-jndi-name="LocalPlugin"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * @ejb:transaction type="REQUIRED"
 * @ejb:value-object name="Plugin" match="*"
 *
 * @ejb:finder signature="org.hyperic.hq.product.shared.PluginLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(p) FROM Plugin AS p WHERE p.name=?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @jboss:table-name table-name="EAM_PLUGIN"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class PluginEJBImpl 
    implements EntityBean 
{
    public final String ctx = PluginEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_PLUGIN_ID_SEQ";
    public final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    protected Log log = LogFactory.getLog(ctx);

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
     * @jboss:read-only true
     */
    public abstract String getName();

    /**
     * @ejb:interface-method
     */
    public abstract void setName(String name);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getPath();

    /**
     * @ejb:interface-method
     */
    public abstract void setPath(String path);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="MD5"
     * @jboss:read-only true
     */
    public abstract String getMD5();

    /**
     * @ejb:interface-method
     */
    public abstract void setMD5(String path);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getCtime();

    /**
     * @ejb:interface-method
     */
    public abstract void setCtime(long cTime);

    /**
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract PluginValue getPluginValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setPluginValue(PluginValue value);

    /**
     * @ejb:interface-method
     * @ejb:create-method
     * @jboss:read-only true
     */
    public PluginPK ejbCreate(String name, String path, String md5)
        throws CreateException
    {
        try {
            Integer id = 
                new Integer((int)IDGeneratorFactory.getNextId(ctx,
                                    SEQUENCE_NAME, DATASOURCE_NAME));
            setId(id);
            setName(name);
            setPath(path);
            setMD5(md5);
            setCtime(System.currentTimeMillis());
        } catch(Exception exc){
            this.log.error("Unable to create plugin entry");
            throw new CreateException("Failed to create plugin entry: " +
                                      exc.getMessage());
        }
            
        return null;
    }

    public void ejbPostCreate(PluginValue val) {}
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
    public void unsetEntityContext() throws RemoteException {}
}

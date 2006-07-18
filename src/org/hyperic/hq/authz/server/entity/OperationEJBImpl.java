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

package org.hyperic.hq.authz.server.entity;

import java.rmi.RemoteException;
import javax.naming.NamingException;
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.OperationPK;
import org.hyperic.hq.authz.shared.OperationLocal;
import org.hyperic.hq.authz.shared.RoleLocal;
import org.hyperic.hq.authz.shared.RoleUtil;
import org.hyperic.hq.authz.shared.RolePK;
import org.hyperic.hq.authz.shared.AuthzSubjectUtil;
import org.hyperic.util.jdbc.IDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the OperationEJB implementaiton.
 * @ejb:bean name="Operation"
 *      jndi-name="ejb/authz/Operation"
 *      local-jndi-name="LocalOperation"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.OperationLocal findByTypeAndName(org.hyperic.hq.authz.shared.ResourceTypeLocal type, java.lang.String name)"
 *      query="SELECT OBJECT(p) FROM ResourceType as t, IN (t.operations) p WHERE t = ?1 AND p.name = ?2"
 *      unchecked="true"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(p) FROM Operation p"
 *      unchecked="true"
 *
 * @ejb:value-object
 *      name="Operation"
 *      match="*"
 *      cacheable="true"
 *      cacheDuration="600000"
 *
 * @jboss:create-table false
 * @jboss:remove-table false
 * @jboss:table-name table-name="EAM_OPERATION"
 *      
 */

public abstract class OperationEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.OperationEJBImpl");
    private final String ctx = OperationEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_OPERATION_ID_SEQ";
    public OperationEJBImpl() {}

    /**
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.OperationValue getOperationValue();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setOperationValue(org.hyperic.hq.authz.shared.OperationValue opValue);

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public OperationPK ejbCreate(OperationValue opValue)
        throws CreateException {
        super.ejbCreate(ctx, SEQUENCE_NAME);
        setName(opValue.getName());
        return null;
    }

    /**
     * Get the resource types supported by this operation
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="operation-resourcetype"
     *      role-name="one-operation-has-one-resource-type"
     *      cascade-delete="yes"
     * @jboss:relation
     *      fk-column="RESOURCE_TYPE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.ResourceTypeLocal getResourceType();

    /**
     * Set the resource type for this operation
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceType(org.hyperic.hq.authz.shared.ResourceTypeLocal resType);

    public void ejbPostCreate(OperationValue opValue) throws CreateException {
        // add the operation to the root role to keep things consistent.
        try {
            // find the root role
            RoleLocal rootRole = RoleUtil.getLocalHome()
                .findByPrimaryKey(new RolePK(AuthzConstants.rootRoleId));
            // have the overlord add the bean we just created to the operation
            rootRole.setWhoami(AuthzSubjectUtil.getLocalHome()
                .findByAuth(AuthzConstants.overlordName, AuthzConstants.overlordDsn)); 
            rootRole.addOperation(getSelfLocal());
        } catch (NamingException e) {
            throw new CreateException("Failed to add operation to root role" 
                + e.getMessage());
        } catch (FinderException e) {
            throw new CreateException("Failed to add operation to root role" 
                + e.getMessage());
        } 
    }

    private OperationLocal getSelfLocal() {
        return (OperationLocal) getEntityContext().getEJBLocalObject();
    }


    public void ejbActivate() throws RemoteException { }

    public void ejbPassivate() throws RemoteException { }

    public void ejbLoad() throws RemoteException { }

    public void ejbStore() throws RemoteException { }

    public void ejbRemove() throws RemoteException, RemoveException { }

}

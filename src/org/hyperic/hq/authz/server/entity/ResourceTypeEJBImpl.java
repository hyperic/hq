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
import java.util.Iterator;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.OperationLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypePK;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ResourceTypeEJB implementaiton.
 * @ejb:bean name="ResourceType"
 *      jndi-name="ejb/authz/ResourceType"
 *      local-jndi-name="LocalResourceType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.ResourceTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(r) FROM ResourceType as r WHERE r.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(r) FROM ResourceType as r" 
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object
 *      name="ResourceType"
 *      match="*"
 *      instantiation="eager"
 *      cacheable="true"
 *      cacheDuration="600000"
 *
 * @jboss:table-name table-name="EAM_RESOURCE_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class ResourceTypeEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.ResourceTypeEJBImpl");
    private final String ctx = ResourceTypeEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_RESOURCE_TYPE_ID_SEQ";
    public ResourceTypeEJBImpl() {}

    /**
     * Get the System flag which determines whether this resource
     * type is one of the internal authz system ones
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="FSYSTEM"
     * @jboss:read-only true
     */
    public abstract boolean getSystem();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setSystem(boolean fSystem);

    /**
     * Get the Operation of this ResourceType. 
     * @ejb:interface-method
     * @ejb:relation
     *      name="operation-resourcetype"
     *      role-name="one-resourcetype-has-many-operation"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      match="*"
     *      type="Set"
     *      relation="external"
     *      aggregate="org.hyperic.hq.authz.shared.OperationValue"
     *      aggregate-name="OperationValue"
     *      members="org.hyperic.hq.authz.shared.OperationLocal"
     *      members-name="Operation"
     * @jboss:read-only true
     */
    public abstract Set getOperations(); 
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setOperations(Set operations);

    /**
     * Get a snapshot of the operations for this rt
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getOperationsSnapshot() {
        return new java.util.HashSet(getOperations());
    }

    /**
     * Get the resources of this resource type
     * @ejb:interface-method
     * @ejb:relation
     *      name="resource-resourcetype"
     *      role-name="resourcetypes-have-many-resources"
     * @jboss:read-only true
     */
    public abstract Set getResources();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResources(Set resources);

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.ResourceTypeValue getResourceTypeValue();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceTypeValue(org.hyperic.hq.authz.shared.ResourceTypeValue value);

    /**
     * Hand coded value object getter
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ResourceTypeValue getResourceTypeValueObject() {
        ResourceTypeValue rtv = new ResourceTypeValue();
        rtv.setSystem(getSystem());
        rtv.setName((getName() == null) ? "" : getName());
        rtv.setId(getId());
        rtv.removeAllOperationValues();
        return rtv;
    }

    /**
     * Add an Operation to this ResourceType
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addOperation(OperationLocal opLocal)
        throws PermissionException {
        getOperations().add(opLocal);
    }

    /**
     * Add Operations to this ResourceType
     * @param operations A set of OperationValue objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addOperations(Set operations) throws PermissionException {
        Iterator it = operations.iterator();
        while ((it != null) && it.hasNext()) {
            addOperation((OperationLocal)it.next());
        }
    }


    /**
     * Remove all operations.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeAllOperations() throws PermissionException {
        removeOperations(getOperations());
    }

    private void deleteOperation(OperationLocal opLocal) {
        if (log.isDebugEnabled()) {
            log.debug("removing " + opLocal.getName());
        }
        try {
            opLocal.remove();
        } catch (RemoveException e) {
            log.error("Error removing operation.");
        }
    }

    /**
     * Remove one operations.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeOperation(OperationLocal operation)
        throws PermissionException {
        getOperations().remove(operation);
        deleteOperation(operation);
    }

    /**
     * Remove some operations.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeOperations(Set operations) throws PermissionException {
        Object[] opArray = operations.toArray();
        int counter;

        for (counter = 0; counter < opArray.length; counter++) {
            removeOperation((OperationLocal)opArray[counter]);
        }
    }

    /** Get the resource
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcetype-resourceid"
     *      role-name="resourcetype-is-a-resource"
     *      target-ejb="Resource"
     *      target-role-name="everything-is-a-resource"
     *      target-cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="RESOURCE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract ResourceLocal getResource();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResource(ResourceLocal resource);

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public ResourceTypePK ejbCreate(AuthzSubjectLocal whoami,
                                    ResourceTypeValue value)
        throws CreateException, PermissionException {
        ResourceTypeLocalHome typeLome = null;
        ResourceTypeLocal rootType = null;
        OperationLocal op = null;
        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            rootType = typeLome.findByName(AuthzConstants.typeResourceTypeName);
        } catch (NamingException e) {
            throw new CreateException("Naming exception in ejbCreate while setting up permission check.");
        } catch (FinderException e) {
            throw new CreateException("Finder exception in ejbCreate while setting up permission check.");
        }
        super.ejbCreate(ctx, SEQUENCE_NAME);
        setName(value.getName());
        return null;
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami,
                              ResourceTypeValue value) throws CreateException {
        ResourceTypeLocalHome typeLome = null;
        ResourceTypeLocal typeLocal = null;
        ResourceLocalHome rLome = null;
        ResourceValue rValue = null;

        if (log.isDebugEnabled()) {
            log.debug("creating " + value.getName());
        }

        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            rLome = ResourceUtil.getLocalHome();
        } catch (NamingException e) {
            throw new CreateException("naming exception in " +
                                      "resourcetype postcreate");
        }

        try {
            typeLocal =
                typeLome.findByName(AuthzConstants.typeResourceTypeName);
        } catch (FinderException e) {
            throw new CreateException("finder exception in " +
                                      "resourcetype postcreate");
        }

        rValue = new ResourceValue();
        rValue.setResourceTypeValue(typeLocal.getResourceTypeValue());
        rValue.setInstanceId(getId());
        setResource(rLome.create(whoami, rValue));
        try {
            addToAuthzGroup(whoami, getResource());
        } catch (NamingException e) {
            throw new CreateException("Unable to add to authz resource group");
        } catch (FinderException e) {
            throw new CreateException("Unable to add to authz resource group");
        } catch (PermissionException e) {
            throw new CreateException("Unable to add to authz resource group: " + e.getMessage());
        }
    }

    public void ejbActivate() throws RemoteException { }

    public void ejbPassivate() throws RemoteException { }

    public void ejbLoad() throws RemoteException {
    }

    public void ejbStore() throws RemoteException { }

    /**
     * Removes a resource type.
     * @see Resource.ejbRemove()
     * A significant consequence of removing a resource type is that
     * it was cascade to all the resource of this type. For reasons
     * explained in Resource.ejbRemove()'s javadocs, there is no authorization
     * checking for resource removal. Thus, we will reach an inconsistent state
     * where a resource references a resource object that no longer exists.
     *
     * Secondly, authorization checks will fail for any resources of this type.
     *
     * The consumer should take care to make sure you don't remove types if
     * resources of that type still exist. The subsystem is unaware of the
     * actual existence of resources, only the existence of corresponding
     * authorization resource objects.
     */
    public void ejbRemove() throws RemoteException, RemoveException {
        if (!getResource().isOwner(getWhoami().getId())) {
            throw new RemoveException("Only the owner can remove the ResourceType.");
        }
    }

}

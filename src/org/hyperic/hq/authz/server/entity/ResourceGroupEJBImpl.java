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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupPK;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleLocal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ResourceGroupEJB implementaiton.
 * @ejb:bean name="ResourceGroup"
 *      jndi-name="ejb/authz/ResourceGroup"
 *      local-jndi-name="LocalResourceGroup"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.ResourceGroupLocal findByName(java.lang.String id)"
 *      query="SELECT OBJECT(r) FROM ResourceGroup AS r WHERE r.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ResourceGroupLocal findByName(java.lang.String id)"
 *      query="SELECT OBJECT(r) FROM ResourceGroup AS r WHERE LCASE(r.name) = LCASE(?1)"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(r) FROM ResourceGroup AS r"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:finder signature="java.util.Collection findByRoleIdAndSystem_orderName_asc(java.lang.Integer roleid, boolean system)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE r.id = ?1 AND rg.system = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByRoleIdAndSystem_orderName_asc(java.lang.Integer roleid, boolean system)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE r.id = ?1 AND rg.system = ?2 ORDER BY rg.sortName"
 * 
 * @ejb:finder signature="java.util.Collection findByRoleIdAndSystem_orderName_desc(java.lang.Integer roleid, boolean system)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE r.id = ?1 AND rg.system = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByRoleIdAndSystem_orderName_desc(java.lang.Integer roleid, boolean system)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE r.id = ?1 AND rg.system = ?2 ORDER BY rg.sortName DESC"
 * 
 * @ejb:finder signature="java.util.Collection findByNotRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE NOT r.id = ?1 AND rg.system = FALSE"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByNotRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE NOT r.id = ?1 AND rg.system = FALSE ORDER BY rg.sortName"
 * 
 * @ejb:finder signature="java.util.Collection findByNotRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE NOT r.id = ?1 AND rg.system = FALSE"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByNotRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg, IN (rg.roles) AS r WHERE NOT r.id = ?1 AND rg.system = FALSE ORDER BY rg.sortName DESC"
 * @ejb:finder signature="java.util.Collection findContaining_orderName_asc(
                          java.lang.Integer instanceId, java.lang.Integer typeId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql signature="java.util.Collection findContaining_orderName_asc(
                                  java.lang.Integer instanceId, java.lang.Integer typeId)"
 *      distinct="true"
 *      additional-columns=", EAM_RESOURCE_GROUP.sort_name"
 *      from=", EAM_RES_GRP_RES_MAP, EAM_RESOURCE"
 *      where="     EAM_RESOURCE_GROUP.id           = EAM_RES_GRP_RES_MAP.resource_group_id 
                AND EAM_RES_GRP_RES_MAP.resource_id = EAM_RESOURCE.id
                AND EAM_RESOURCE.instance_id        = {0}
                AND EAM_RESOURCE.resource_type_id   = {1}"
 *      order="EAM_RESOURCE_GROUP.sort_name"
 *
 * @ejb:finder signature="java.util.Collection findContaining_orderName_desc(
                          java.lang.Integer instanceId, java.lang.Integer typeId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql signature="java.util.Collection findContaining_orderName_desc(
                                  java.lang.Integer instanceId, java.lang.Integer typeId)"
 *      distinct="true"
 *      additional-columns=", EAM_RESOURCE_GROUP.sort_name"
 *      from=", EAM_RES_GRP_RES_MAP, EAM_RESOURCE"
 *      where="     EAM_RESOURCE_GROUP.id           = EAM_RES_GRP_RES_MAP.resource_group_id 
                AND EAM_RES_GRP_RES_MAP.resource_id = EAM_RESOURCE.id
                AND EAM_RESOURCE.instance_id        = {0}
                AND EAM_RESOURCE.resource_type_id   = {1}"
 *      order="EAM_RESOURCE_GROUP.sort_name desc"
 *
 * @ejb:finder signature="java.util.Collection findWithNoRoles_orderName_asc()"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg WHERE rg.roles IS EMPTY AND rg.system = FALSE"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findWithNoRoles_orderName_asc()"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg WHERE rg.roles IS EMPTY AND rg.system = FALSE ORDER BY rg.sortName"
 *
 * @ejb:finder signature="java.util.Collection findWithNoRoles_orderName_desc()"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg WHERE rg.roles IS EMPTY AND rg.system = FALSE"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findWithNoRoles_orderName_desc()"
 *      query="SELECT OBJECT(rg) FROM ResourceGroup as rg WHERE rg.roles IS EMPTY AND rg.system = FALSE ORDER BY rg.sortName DESC"
 *
 *
 * @ejb:value-object
 *      name="ResourceGroup"
 *      match="*"
 *      instantiation="eager"
 *
 * @jboss:table-name table-name="EAM_RESOURCE_GROUP"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class ResourceGroupEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.ResourceGroupEJBImpl");
    
    private final String ctx = ResourceGroupEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_RESOURCE_GROUP_ID_SEQ";
    public ResourceGroupEJBImpl() {}

    /**
     * Sort name of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setSortName (java.lang.String sortName);

    /**
     * Get the System flag which determines whether this resource
     * group is one of the internal authz system ones
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
     * Get the group type, field only applies to resource groups
     * enlisted to support the new grouping subsystem.
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="GROUPTYPE"
     * @jboss:read-only true
     */
    public abstract int getGroupType();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setGroupType(int groupType);

    /**
     * Get the group entity type, this designates the type of entities
     * that may be stored in the group (applies only to special groups)
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="GROUP_ENT_TYPE"
     * @jboss:read-only true
     */
    public abstract int getGroupEntType();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setGroupEntType(int groupEntType);

    /**
     * Get the group entity resource type. Designates a group
     * entity sub/resource type. Only applies to special
     * groups.
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="GROUP_ENT_RES_TYPE"
     * @jboss:read-only true
     */
    public abstract int getGroupEntResType();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setGroupEntResType(int groupEntResType);

    /**
     * Get the group's clusterId. This attribute has no particular
     * use in authz but is used when a resource group is later converted
     * into an appdef group.
     * 
     * @ejb:persistent-field
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="CLUSTER_ID"
     * @jboss:read-only true
     */
    public abstract int getClusterId();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setClusterId(int clusterId);

    /**
     * Description of this resource group
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="DESCRIPTION"
     * @jboss:read-only true
     */
    public abstract String getDescription();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDescription(String description);

    /**
     * Location of items in resource group.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="LOCATION"
     * @jboss:read-only true
     */
    public abstract String getLocation();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setLocation(String location);

    /**
     * Last person to modify this record.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MODIFIED_BY"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract String getModifiedBy();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setModifiedBy(String modifier);
    /**
     * Time of last modification
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract Long getMTime();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @ejb:value-object match="*"
     */
    public abstract void setMTime(Long mtime);
    /**
     * Time this EJB was created
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract Long getCTime();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setCTime(Long ctime);

    /**
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.ResourceGroupValue getResourceGroupValue();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceGroupValue(org.hyperic.hq.authz.shared.ResourceGroupValue value);

    /** Get the resource
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcegroup-resourceid"
     *      role-name="resourcegroup-is-a-resource"
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

    /** Get the resources in this group.
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcegroups-resources"
     *      role-name="resourcegroup-contains-resources"
     * @ejb:transaction type="REQUIRED"
     * @jboss:relation-table
            table-name="EAM_RES_GRP_RES_MAP"
     *      create-table="no"
     *      remove-table="no"
     * @jboss:relation
     *      fk-column="RESOURCE_ID"
     *      related-pk-field="id"
     */
    public abstract Set getResources();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResources(Set resources);

    /**
     * Return a list of instance id's for all the resources in a
     * group    
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public List getInstanceIds() throws NamingException, FinderException {
        List idList = new ArrayList();
        Iterator resIt = getResources().iterator();
        while(resIt.hasNext()) {
            ResourceLocal aRes = (ResourceLocal)resIt.next();
            idList.add(aRes.getInstanceId());
        }
        return idList;
    }

    /** Get the role that operate on this group.
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcegroups-roles"
     *      role-name="resourcegroup-are-manipulated-by-roles"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
            table-name="EAM_ROLE_RESOURCE_GROUP_MAP"
     *      create-table="no"
     *      remove-table="no"
     * @jboss:relation
     *      fk-column="ROLE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract Set getRoles();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setRoles(Set roles);

    /**
     * Add a Resource to this ResourceGroup
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addResource(ResourceLocal rLocal) throws PermissionException {
        // This authz check is now being performed in the manager. And you 
        // do not have to be the owner.
        //if (!rLocal.isOwner(getWhoami())) {
        //  throw new PermissionException("Only the owner can add this Resource to a ResourceGroup.");
        //}
        getResources().add(rLocal);
    }

    /**
     * Add Resource to this ResourceGroup
     * @param resources A set of Resource objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addResources(Set resources) throws PermissionException {
        Iterator it = resources.iterator();
        while (it != null && it.hasNext()) {
            addResource((ResourceLocal)it.next());
        }
    }

    /**
     * Remove all Resources.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeAllResources() throws PermissionException {
        removeResources(getResources());
    }

    /**
     * Remove one resource.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeResource(ResourceLocal resource)
        throws PermissionException {
        // This authz check is now being performed in the manager. And you 
        // do not have to be the owner.
        //if (!rLocal.isOwner(getWhoami())) {
        //    throw new PermissionException("Only the owner can remove this Resource from a ResourceGroup.");
        //}
        getResources().remove(resource);
    }

    /**
     * Remove some resources.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeResources(Set resources) throws PermissionException {
        Object[] rArray = resources.toArray();
        int counter;

        for (counter = 0; counter < rArray.length; counter++) {
            removeResource((ResourceLocal)rArray[counter]);
        }
    }

    /**
     * Add a Role to this ResourceGroup
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @deprecated use Role#addResourceGroup()
     */
    public void addRole(RoleLocal rLocal) {
        getRoles().add(rLocal);
    }

    /**
     * Add Role to this ResourceGroup
     * @param roles A set of Role objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @deprecated use Role#addResourceGroups()
     */
    public void addRoles(Set roles) {
        Iterator it = roles.iterator();
        while (it != null && it.hasNext()) {
            addRole((RoleLocal)it.next());
        }
    }

    /**
     * Remove all Roles.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @deprecated use Role#removeResourceGroups()
     */
    public void removeAllRoles() {
        removeRoles(getRoles());
    }

    /**
     * Remove one resource.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @deprecated use Role#removeResourceGroup()
     */
    public void removeRole(RoleLocal role) {
        getRoles().remove(role);
    }

    /**
     * Remove some resources.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @deprecated use Role#removeResourceGroups()
     */
    public void removeRoles(Set roles) {
        Object[] roleArray = roles.toArray();
        int counter;
        for (counter = 0; counter < roleArray.length; counter++) {
            removeRole((RoleLocal)roleArray[counter]);
        }
    }

    /**
     * The create method for non-System groups
     * @param whoami - creating user
     * @param group - the group value object
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public ResourceGroupPK ejbCreate(AuthzSubjectLocal whoami,
                                     ResourceGroupValue value)
        throws CreateException {
            return ejbCreate(whoami, value, false);
    }

    /**
     * The create method
     * @param whoami - creating user
     * @param group - the group value object
     * @param isSystemResource - a flag which will mark the group's resource
     * entry as a system one, preventing it from being displayed by the UI
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public ResourceGroupPK ejbCreate(AuthzSubjectLocal whoami,
                                     ResourceGroupValue value, 
                                     boolean isSystemResource)
        throws CreateException {
        super.ejbCreate(ctx, SEQUENCE_NAME);
        setName(value.getName());
        if (value.getName()!=null)
            setSortName(value.getName().toUpperCase());
        setDescription(value.getDescription());
        setLocation(value.getLocation());
        setMTime( new Long(System.currentTimeMillis()) );
        setCTime( new Long(System.currentTimeMillis()) );
        setModifiedBy( whoami.getName() );
        setSystem(value.getSystem());

        /* if the group type hasn't been set, this
           means this is only an authz group (use -1)*/
        if (value.getGroupType()==0) {
            value.setGroupType(AuthzConstants.authzDefaultResourceGroupType); 
        }

        setGroupType(value.getGroupType());
        setGroupEntType(value.getGroupEntType());
        setGroupEntResType(value.getGroupEntResType());
        setClusterId(value.getClusterId());

        return null;
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami,
                              ResourceGroupValue value) 
        throws CreateException {
        ejbPostCreate(whoami, value, false);
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami,
                              ResourceGroupValue value,
                              boolean isSystemResource) 
        throws CreateException {
        ResourceTypeLocalHome typeLome = null;
        ResourceTypeLocal typeLocal = null;
        ResourceLocalHome rLome = null;
        ResourceValue rValue = null;
        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            rLome = ResourceUtil.getLocalHome();
        } catch (NamingException e) {
            throw new CreateException("naming exception in rgroup postcreate");
        }

        try {
            typeLocal =
                typeLome.findByName(AuthzConstants.groupResourceTypeName);
        } catch (FinderException e) {
            throw new CreateException("finder exception in rgroup postcreate");
        }

        rValue = new ResourceValue();
        rValue.setResourceTypeValue(typeLocal.getResourceTypeValue());
        rValue.setInstanceId(getId());
        rValue.setName(value.getName());
        if (value.getName()!=null)
            rValue.setSortName(value.getName().toUpperCase());
        rValue.setSystem(isSystemResource);
        setResource(rLome.create(whoami, rValue));
    }

    public void ejbActivate() throws RemoteException { }

    public void ejbPassivate() throws RemoteException { }

    public void ejbLoad() throws RemoteException { }

    public void ejbStore() throws RemoteException { 
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null); 
    }

    public void ejbRemove() throws RemoteException, RemoveException {
    }

}

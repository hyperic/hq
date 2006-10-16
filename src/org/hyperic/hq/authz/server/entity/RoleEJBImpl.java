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
import org.hyperic.hq.authz.shared.ResourceGroupLocal;
import org.hyperic.hq.authz.shared.ResourceGroupUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RolePK;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.values.OwnedRoleValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the RoleEJB implementaiton.
 * @ejb:bean name="Role"
 *      jndi-name="ejb/authz/Role"
 *      local-jndi-name="LocalRole"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.RoleLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.RoleLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.authz.shared.RoleLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE LCASE(r.name) = LCASE(?1)"
 *
 * @ejb:finder signature="Collection findAll()"
 *      query="SELECT OBJECT(r) FROM Role as r"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(r) FROM Role as r"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(r) FROM Role as r ORDER BY r.sortName"
 *
 * @ejb:finder signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(r) FROM Role as r"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(r) FROM Role as r ORDER BY r.sortName DESC"
 *      
 *
 * @ejb:finder signature="Collection findBySystem(boolean sys)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.system = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="Collection findBySystem_orderName_asc(boolean sys)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.system = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findBySystem_orderName_asc(boolean sys)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.system = ?1 ORDER BY r.sortName"
 *
 * @ejb:finder signature="Collection findBySystem_orderName_desc(boolean sys)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.system = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findBySystem_orderName_desc(boolean sys)"
 *      query="SELECT OBJECT(r) FROM Role as r WHERE r.system = ?1 ORDER BY r.sortName DESC"
 *
 *
 * @ejb:finder signature="Collection findBySystemAndSubject_orderName_asc(boolean sys, java.lang.Integer subjectid)"
 *      query="SELECT OBJECT(r) FROM Role as r, IN (r.subjects) AS subj WHERE r.system = ?1 AND subj.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findBySystemAndSubject_orderName_asc(boolean sys, java.lang.Integer subjectid)"
 *      query="SELECT OBJECT(r) FROM Role as r, IN (r.subjects) AS subj WHERE r.system = ?1 AND subj.id = ?2 ORDER BY r.sortName"
 *
 * @ejb:finder signature="Collection findBySystemAndSubject_orderName_desc(boolean sys, java.lang.Integer subjectid)"
 *      query="SELECT OBJECT(r) FROM Role as r, IN (r.subjects) AS subj WHERE r.system = ?1 AND subj.id = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findBySystemAndSubject_orderName_desc(boolean sys, java.lang.Integer subjectid)"
 *      query="SELECT OBJECT(r) FROM Role as r, IN (r.subjects) AS subj WHERE r.system = ?1 AND subj.id = ?2 ORDER BY r.sortName DESC"
 *

 * @ejb:finder signature="Collection findBySystemAndSubject_orderMember_asc(boolean sys, java.lang.Integer subjectid)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="Collection findBySystemAndSubject_orderMember_asc (boolean sys, java.lang.Integer subjectid)"
 *      distinct="true"
 *      additional-columns=", EAM_ROLE.sort_name, ctable.cnt"
 *      from =", EAM_SUBJECT, EAM_SUBJECT_ROLE_MAP,
                (SELECT role_id AS rid, count(subject_id) AS cnt 
                 FROM   EAM_SUBJECT_ROLE_MAP GROUP BY role_id) ctable"
 *      where="     EAM_ROLE.fsystem = {0} 
                AND EAM_SUBJECT.id   = {1}
                AND EAM_ROLE.id      = EAM_SUBJECT_ROLE_MAP.role_id
                AND EAM_SUBJECT.id   = EAM_SUBJECT_ROLE_MAP.subject_id
                AND EAM_ROLE.id      = ctable.rid" 
 *      order=" ctable.cnt, EAM_ROLE.sort_name"
 *
 * @ejb:finder signature="Collection findBySystemAndSubject_orderMember_desc(boolean sys, java.lang.Integer subjectid)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="Collection findBySystemAndSubject_orderMember_desc (boolean sys, java.lang.Integer subjectid)"
 *      distinct="true"
 *      additional-columns=", EAM_ROLE.sort_name, ctable.cnt"
 *      from =", EAM_SUBJECT, EAM_SUBJECT_ROLE_MAP,
                (SELECT role_id AS rid, count(subject_id) AS cnt 
                 FROM   EAM_SUBJECT_ROLE_MAP GROUP BY role_id) ctable"
 *      where="     EAM_ROLE.fsystem = {0} 
                AND EAM_SUBJECT.id   = {1}
                AND EAM_ROLE.id      = EAM_SUBJECT_ROLE_MAP.role_id
                AND EAM_SUBJECT.id   = EAM_SUBJECT_ROLE_MAP.subject_id
                AND EAM_ROLE.id      = ctable.rid" 
 *      order=" ctable.cnt desc, EAM_ROLE.sort_name"
 *
 * @ejb:finder signature="Collection findBySystemAndAvailableForSubject_orderName_asc(boolean system, java.lang.Integer subjectid)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="Collection findBySystemAndAvailableForSubject_orderName_asc(boolean system, java.lang.Integer subjectid)"
 *      where=" EAM_ROLE.fsystem = {0} AND 
                EAM_ROLE.id NOT IN 
                    (SELECT role_id FROM EAM_SUBJECT_ROLE_MAP 
                     WHERE subject_id = {1}) ORDER BY EAM_ROLE.sort_name"
 *
 * @ejb:finder signature="Collection findBySystemAndAvailableForSubject_orderName_desc(boolean system, java.lang.Integer subjectid)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="Collection findBySystemAndAvailableForSubject_orderName_desc(boolean system, java.lang.Integer subjectid)"
 *      where=" EAM_ROLE.fsystem = {0} AND
                EAM_ROLE.id NOT IN
                    (SELECT role_id FROM EAM_SUBJECT_ROLE_MAP
                     WHERE subject_id = {1}) ORDER BY EAM_ROLE.sort_name DESC"
 * 
 * @ejb:finder signature="org.hyperic.hq.authz.shared.RoleLocal findAvailableRoleForSubject(java.lang.Integer roleId, java.lang.Integer subjectid)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="org.hyperic.hq.authz.shared.RoleLocal findAvailableRoleForSubject(java.lang.Integer roleId, java.lang.Integer subjectid)"
 *      where=" EAM_ROLE.id = {0} AND EAM_ROLE.id NOT IN
                    (SELECT role_id FROM EAM_SUBJECT_ROLE_MAP WHERE subject_id = {1})"
 * 
 * @ejb:finder signature="Collection findAvailableForGroup(boolean system, java.lang.Integer groupId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="Collection findAvailableForGroup(boolean system, java.lang.Integer groupId)"
 *      where="EAM_ROLE.fsystem = {0} AND
               EAM_ROLE.id NOT IN
                    (SELECT role_id FROM EAM_ROLE_RESOURCE_GROUP_MAP
                     WHERE resource_group_id = {1}) ORDER BY EAM_ROLE.sort_name"
 * 
 * 
 * @ejb:value-object
 *      name="Role"
 *      match="*"
 *      instantiation="eager"
 *
 * @jboss:table-name table-name="EAM_ROLE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class RoleEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.RoleEJBImpl");
    private final String ctx = RoleEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_ROLE_ID_SEQ";

    public RoleEJBImpl() {}

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
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.RoleValue getRoleValue();

    /**
     * Get the owned role value for this role
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public OwnedRoleValue getOwnedRoleValue() {
        OwnedRoleValue orv = new OwnedRoleValue(this.getRoleValue(),
                                                this.getResource().getOwner()
                                                    .getAuthzSubjectValue());
        return orv;
    }
                                                
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setRoleValue(org.hyperic.hq.authz.shared.RoleValue value);

    /**
     * Get the System flag which determines whether this role
     * is one of the internal authz system ones
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
     * Description of this role
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

    /** Get the resource
     * @ejb:interface-method
     * @ejb:relation
     *      name="role-resourceid"
     *      role-name="role-is-a-resource"
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

    /** Get the resource groups this role can operate on.
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcegroups-roles"
     *      role-name="roles-operate-on-resourcegroups"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
     *      table-name="EAM_ROLE_RESOURCE_GROUP_MAP"
     *      create-table="no"
     *      remove-table="no"
     * @jboss:relation
     *      fk-column="RESOURCE_GROUP_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract Set getResourceGroups();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceGroups(Set resourceGroups);

    /** Get the subject in this role.
     * @ejb:interface-method
     * @ejb:relation
     *      name="subjects-roles"
     *      role-name="roles-contain-subjects"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
     *      table-name="EAM_SUBJECT_ROLE_MAP"
     *      create-table="no"
     *      remove-table="no"
     * @jboss:relation
     *      fk-column="SUBJECT_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract Set getSubjects();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setSubjects(Set subjects);

    /** Get the operation performed by this role.
     * @ejb:interface-method
     * @ejb:relation
     *      name="roles-operations"
     *      role-name="roles-can-perform-operations"
     *      target-ejb="Operation"
     *      target-role-name="operations-are-performed-by-roles"
     *      target-multiple="yes"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      match="*"
     *      type="Set"
     *      relation="external"
     *      aggregate="org.hyperic.hq.authz.shared.OperationValue"
     *      aggregate-name="OperationValue"
     *      members="org.hyperic.hq.authz.shared.OperationLocal"
     *      members-name="Operation"
     * @jboss:relation-table
     *      table-name="EAM_ROLE_OPERATION_MAP"
     *      create-table="no"
     *      remove-table="no"
     * @jboss:relation
     *      fk-column="OPERATION_ID"
     *      related-pk-field="id"
     * @jboss:target-relation
     *      fk-column="ROLE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract Set getOperations();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setOperations(Set operations);

    /**
     * Add a ResourceGroup to this Role
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addResourceGroup(ResourceGroupLocal rgLocal) {
        getResourceGroups().add(rgLocal);
    }

    /**
     * Add ResourceGroup to this Role
     * @param resourceGroups A set of ResourceGroup objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addResourceGroups(Set resourceGroups) {
        Iterator it = resourceGroups.iterator();
        while (it != null && it.hasNext()) {
            ResourceGroupLocal group = (ResourceGroupLocal)it.next();
            addResourceGroup(group);
        }
    }

    /**
     * Remove all ResourceGroups.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeAllResourceGroups() {
        removeResourceGroups(getResourceGroups());
    }

    /**
     * Remove one resourceGroup.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeResourceGroup(ResourceGroupLocal resourceGroup)
    {
        getResourceGroups().remove(resourceGroup);
    }

    /**
     * Remove some resourceGroups.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeResourceGroups(Set resourceGroups) {
        Object[] groupArray = resourceGroups.toArray();
        int counter;
        for (counter = 0; counter < groupArray.length; counter++) {
            ResourceGroupLocal group = (ResourceGroupLocal)groupArray[counter];
            removeResourceGroup(group);
        }
    }

    /**
     * Add a AuthzSubject to this Role
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addSubject(AuthzSubjectLocal subject)
        throws PermissionException {
        if (subject.isRoot()) {
            throw new PermissionException(
                    "The super user cannot belong to a Role.");
        }
        getSubjects().add(subject);
    }

    /**
     * Add AuthzSubject to this Role
     * @param subjects A set of AuthzSubject objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addSubjects(Set subjects) throws PermissionException {
        Iterator it = subjects.iterator();
        while (it != null && it.hasNext()) {
            AuthzSubjectLocal subject = (AuthzSubjectLocal)it.next();
            addSubject(subject);
        }
    }

    /**
     * Remove all AuthzSubjects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeAllSubjects() {
        removeSubjects(getSubjects());
    }

    /**
     * Remove one subject.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeSubject(AuthzSubjectLocal subject) {
        getSubjects().remove(subject);
    }

    /**
     * Remove some subjects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeSubjects(Set subjects) {
        Object[] subjectArray = subjects.toArray();
        int counter;
        for (counter = 0; counter < subjectArray.length; counter++) {
            AuthzSubjectLocal subject =
                (AuthzSubjectLocal)subjectArray[counter];
            removeSubject(subject);
        }
    }

    /**
     * Add a Operation to this Role
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addOperation(OperationLocal operation) {
        getOperations().add(operation);
    }

    /**
     * Add Operation to this Role
     * @param operations A set of Operation objects.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void addOperations(Set operations) {
        Iterator it = operations.iterator();
        while (it != null && it.hasNext()) {
            OperationLocal op = (OperationLocal)it.next();
            addOperation(op);
        }
    }

    /**
     * Remove all Operations.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeAllOperations() {
        removeOperations(getOperations());
    }

    /**
     * Remove one operation.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeOperation(OperationLocal operation) {
        getOperations().remove(operation);
    }

    /**
     * Remove some operations.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public void removeOperations(Set operations) {
        Object[] opArray = operations.toArray();
        int counter;
        for (counter = 0; counter < opArray.length; counter++) {
            OperationLocal op = (OperationLocal)opArray[counter];
            removeOperation(op);
        }
    }

    /**
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public RolePK ejbCreate(AuthzSubjectLocal whoami, RoleValue value)
        throws CreateException {
        super.ejbCreate(ctx, SEQUENCE_NAME);
        setName(value.getName());
        if (value.getName()!=null)
            setSortName(value.getName().toUpperCase());
        setDescription(value.getDescription());
        return new RolePK(getId());
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami, RoleValue value)
        throws CreateException {
        ResourceTypeLocalHome typeLome = null;
        ResourceTypeLocal typeLocal = null;
        ResourceLocalHome rLome = null;
        ResourceValue rValue = null;
        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            rLome = ResourceUtil.getLocalHome();
        } catch (NamingException e) {
            throw new CreateException("naming exception in role postcreate");
        }

        try {
            typeLocal =
                typeLome.findByName(AuthzConstants.roleResourceTypeName);
        } catch (FinderException e) {
            throw new CreateException("finder exception in role postcreate");
        }

        rValue = new ResourceValue();
        rValue.setResourceTypeValue(typeLocal.getResourceTypeValue());
        rValue.setInstanceId(getId());
        ResourceLocal myResource = rLome.create(whoami, rValue);
        setResource(myResource);

        /**
         Add the Authz Resource Group to every role.
         This is done here so that the roles are always able
         to operate on root types such as Subjects, Roles, and Groups
        **/
        try {
            ResourceGroupLocal authzGroup = ResourceGroupUtil.getLocalHome()
                .findByName(AuthzConstants.authzResourceGroupName);
            addResourceGroup(authzGroup);
        } catch (FinderException e) {
            throw new CreateException("Unable to find authzResourceGroup");
        } catch (NamingException e) {
            throw new CreateException("Unable to add authzresourcegroup to role");
        }

        /** 
         Create a group which will contain only the resource for the
         Role we're creating, this is done so that role permissions 
         can be granted to members of the role. 
         Fix for Bug #5219
        **/
        try {
            ResourceGroupValue group = new ResourceGroupValue();
            group.setName(AuthzConstants.privateRoleGroupName + getId());
            group.setSortName(AuthzConstants.privateRoleGroupName.toUpperCase() + getId());
            group.setSystem(true);
            ResourceGroupLocal groupEJB = ResourceGroupUtil.getLocalHome()
                    .create(whoami, group, true);
            // add our resource
            groupEJB.getResources().add(myResource);
            getResourceGroups().add(groupEJB);
        } catch (NamingException e) {
            throw new CreateException("Unable to create private group for role");
        } 
    }

    public void ejbActivate() throws RemoteException { }

    public void ejbPassivate() throws RemoteException { }

    public void ejbLoad() throws RemoteException { }

    public void ejbStore() throws RemoteException {
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoveException {
        // Fix for 5304. Delete the private group too
        log.debug("Removing role");
        try {
            ResourceGroupLocal aGroup = ResourceGroupUtil.getLocalHome()
                .findByName(AuthzConstants.privateRoleGroupName + getId());
            log.debug("Removing private role group");
            aGroup.remove();
        } catch (Exception e) {
            log.error("Unable to remove private group", e);
            RemoveException r = new RemoveException(
                "Could not remove private role group");
            r.initCause(e);
            throw r;
        }
        
    }
}

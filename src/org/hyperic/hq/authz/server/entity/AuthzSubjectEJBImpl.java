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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectPK;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleLocal;
import org.hyperic.hq.authz.shared.RolePK;
import org.hyperic.hq.authz.shared.RoleUtil;

/**
 * This is the AuthzSubjectEJB implementaiton.
 * @ejb:bean name="AuthzSubject"
 *      jndi-name="ejb/authz/AuthzSubject"
 *      local-jndi-name="LocalAuthzSubject"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.AuthzSubjectLocal findByAuth(java.lang.String name, java.lang.String dsn)"
 *      query="SELECT OBJECT(r) FROM AuthzSubject as r WHERE r.name = ?1 AND r.authDsn = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.AuthzSubjectLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(r) FROM AuthzSubject as r WHERE r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.AuthzSubjectLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(r) FROM AuthzSubject as r WHERE r.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(r) FROM AuthzSubject as r"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.sortName DESC"

 * @ejb:finder 
 *      signature="java.util.Collection findAllRoot_orderName_asc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.sortName"
 *
 * @ejb:finder 
 *      signature="java.util.Collection findAllRoot_orderName_desc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.sortName DESC"
 *

 * @ejb:finder signature="java.util.Collection findAll_orderFirstName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderFirstName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.firstName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderFirstName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderFirstName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.firstName DESC"
 * 
 * 
 * @ejb:finder signature="java.util.Collection findAllRoot_orderFirstName_asc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderFirstName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.firstName"
 *
 * @ejb:finder signature="java.util.Collection findAllRoot_orderFirstName_desc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderFirstName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.firstName DESC"
 *
 *
 * @ejb:finder signature="java.util.Collection findAll_orderLastName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderLastName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.lastName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderLastName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAll_orderLastName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.system = false ORDER BY ss.lastName DESC"
 *
 * 
 * @ejb:finder 
 *      signature="java.util.Collection findAllRoot_orderLastName_asc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderLastName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.lastName"
 *
 * @ejb:finder 
 *      signature="java.util.Collection findAllRoot_orderLastName_desc()"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findAllRoot_orderLastName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE (ss.system = false OR ss.id=1) ORDER BY ss.lastName DESC"
 *
 *
 * @ejb:finder signature="java.util.Collection findByRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE r.id = ?1 AND ss.system = false ORDER BY ss.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE r.id = ?1 AND ss.system = false ORDER BY ss.sortName DESC"
 *
 *
 * @ejb:finder signature="java.util.Collection findByNotRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE NOT r.id = ?1 AND ss.system = false "
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByNotRoleId_orderName_asc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE NOT r.id = ?1 AND ss.system = false ORDER BY ss.sortName"
 *
 * @ejb:finder signature="java.util.Collection findByNotRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE NOT r.id = ?1 AND ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findByNotRoleId_orderName_desc(java.lang.Integer roleid)"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss, IN (ss.roles) AS r WHERE NOT r.id = ?1 AND ss.system = false ORDER BY ss.sortName DESC"
 *
 *
 * @ejb:finder signature="java.util.Collection findWithNoRoles_orderName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.roles IS EMPTY AND ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findWithNoRoles_orderName_asc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.roles IS EMPTY AND ss.system = false ORDER BY ss.sortName"
 *
 * @ejb:finder signature="java.util.Collection findWithNoRoles_orderName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.roles IS EMPTY AND ss.system = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="java.util.Collection findWithNoRoles_orderName_desc()"
 *      query="SELECT OBJECT(ss) FROM AuthzSubject as ss WHERE ss.roles IS EMPTY AND ss.system = false ORDER BY ss.sortName DESC"
 *
 *
 * @ejb:value-object
 *      name="AuthzSubject"
 *      match="*"
 *      instantiation="eager"
 *      cacheable="true"
 *      cacheDuration="600000"
 *
 * @jboss:table-name table-name="EAM_SUBJECT"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class AuthzSubjectEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.AuthzSubjectEJBImpl");
    private final String ctx = AuthzSubjectEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_SUBJECT_ID_SEQ";

    public AuthzSubjectEJBImpl() {}

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
    public abstract org.hyperic.hq.authz.shared.AuthzSubjectValue getAuthzSubjectValue();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAuthzSubjectValue(org.hyperic.hq.authz.shared.AuthzSubjectValue value);

    /** Get the resource
     * @ejb:interface-method
     * @ejb:relation
     *      name="subject-resourceid"
     *      role-name="subject-is-a-resource"
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
     * Get owned resources
     * @ejb:interface-method
     * @ejb:relation
     *      name="subject-resource"
     *      role-name="subject-owns-many-resources"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract Set getOwnedResources();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setOwnedResources(Set resources);

    /**
     * Get the active flag
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="FACTIVE"
     * @jboss:read-only true
     */
    public abstract boolean getActive();

    /**
     * Set the active flag
     * @ejb:interface-method
     */
    public abstract void setActive(boolean bool);

    /**
     * Get the system flag
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="FSYSTEM"
     * @jboss:read-only true
     */
    public abstract boolean getSystem();

    /**
     * Set the system flag
     * @ejb:interface-method
     */
    public abstract void setSystem(boolean bool);

    /**
     * Authentication source of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="DSN"
     * @jboss:read-only true
     */
    public abstract String getAuthDsn();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAuthDsn(String dsn);

    /**
     * Email address of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="EMAIL_ADDRESS"
     * @jboss:read-only true
     */
    public abstract String getEmailAddress();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setEmailAddress(String emailAddress);

    /**
     * SMS address of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="SMS_ADDRESS"
     * @jboss:read-only true
     */
    public abstract String getSMSAddress();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setSMSAddress(String smsAddress);

    /**
     * First name of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="FIRST_NAME"
     */
    public abstract String getFirstName();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setFirstName(String firstName);

    /**
     * Last name of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="LAST_NAME"
     * @jboss:read-only true
     */
    public abstract String getLastName();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setLastName(String lastName);

    /**
     * Phone number of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="PHONE_NUMBER"
     * @jboss:read-only true
     */
    public abstract String getPhoneNumber();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setPhoneNumber(String phoneNumber);

    /**
     * Department name of this subject
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="DEPARTMENT"
     * @jboss:read-only true
     */
    public abstract String getDepartment();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDepartment(String department);

    /** Get the role that this subject belongs to.
     * @ejb:interface-method
     * @ejb:relation
     *      name="subjects-roles"
     *      role-name="subjects-belong-to-roles"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
     *      table-name="EAM_SUBJECT_ROLE_MAP"
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
     * Tests if subject is root subject. The root subject has all permissions
     * and is uniquely identified by the Id AuthzConstants.rootSubjectId
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public boolean isRoot() {
        return getId().equals(AuthzConstants.rootSubjectId);
    }

    /**
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public AuthzSubjectPK ejbCreate(AuthzSubjectLocal whoami,
                                    AuthzSubjectValue subjectValue)
        throws CreateException, PermissionException {
        if (subjectValue.getName() == null)
            throw new CreateException("Subject name cannot be null");
        
        log.debug("Creating user: " + subjectValue.getName());
        super.ejbCreate(ctx, SEQUENCE_NAME); 
        setName(subjectValue.getName());
        setSortName(subjectValue.getName().toUpperCase());
        setFirstName(subjectValue.getFirstName());
        setLastName(subjectValue.getLastName());
        setAuthDsn(subjectValue.getAuthDsn());
        setPhoneNumber( subjectValue.getPhoneNumber() );
        setDepartment( subjectValue.getDepartment() );
        setEmailAddress( subjectValue.getEmailAddress() );
        setSMSAddress( subjectValue.getSMSAddress() );
        setActive(subjectValue.getActive()); 
        setSystem(subjectValue.getSystem()); 
        subjectValue.setId(getId());
        
        return new AuthzSubjectPK(getId());
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami,
                              AuthzSubjectValue subjectValue)
        throws CreateException, PermissionException {
        ResourceTypeLocalHome typeLome = null;
        ResourceTypeLocal typeLocal = null;
        ResourceLocalHome rLome = null;
        ResourceValue rValue = null;
        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            rLome = ResourceUtil.getLocalHome();
        } catch (NamingException e) {
            throw new CreateException("naming exception in subject postcreate");
        }

        try {
            typeLocal =
                typeLome.findByName(AuthzConstants.subjectResourceTypeName);
        } catch (FinderException e) {
            throw new CreateException("finder exception in subject postcreate");
        }

        rValue = new ResourceValue();   
        rValue.setResourceTypeValue(typeLocal.getResourceTypeValue());
        rValue.setInstanceId(getId());
        setResource(rLome.create(whoami, rValue));

        // add the resource creator role to the user
        try {
            log.debug("Adding creator role to user");
            RoleLocal creatorRole = RoleUtil.getLocalHome()
                .findByName(AuthzConstants.creatorRoleName);
            getRoles().add(creatorRole);
        } catch (NamingException e) {
            throw new CreateException("Unable to add user to ResourceCreator role");
        } catch (FinderException e) {
            throw new CreateException("Unable to add user to Resource Creator role");
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
    }

}

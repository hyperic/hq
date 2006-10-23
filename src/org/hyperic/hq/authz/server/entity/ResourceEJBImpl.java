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
import java.util.HashSet;
import java.util.List;
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
import org.hyperic.hq.authz.shared.AuthzSubjectLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationLocal;
import org.hyperic.hq.authz.shared.ResourceGroupLocal;
import org.hyperic.hq.authz.shared.ResourceGroupUtil;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourcePK;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;

/**
 * This is the ResourceEJB implementation.
 * @ejb:bean name="Resource"
 *      jndi-name="ejb/authz/Resource"
 *      local-jndi-name="LocalResource"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:util generate="physical"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.ResourceLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(r) FROM Resource as r WHERE r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.ResourceLocal findByInstanceId(org.hyperic.hq.authz.shared.ResourceTypeLocal type, java.lang.Integer id)"
 *      query="SELECT DISTINCT OBJECT(r) FROM Resource as r WHERE r.instanceId = ?2 AND r.resourceType = ?1"
 *      uncheck="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.authz.shared.ResourceLocal findByInstanceId(java.lang.Integer typeId, java.lang.Integer instId)"
 *      query="SELECT DISTINCT OBJECT(r) FROM Resource as r, ResourceType as rt WHERE r.instanceId = ?2 AND r.resourceType = rt AND rt.id = ?1"
 *      uncheck="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByOwner(org.hyperic.hq.authz.shared.AuthzSubjectLocal owner)"
 *      query="SELECT DISTINCT OBJECT(r) FROM Resource AS r, AuthzSubject as s WHERE r.owner = s AND s = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"

 * @ejb:finder signature="java.util.Collection findByOwnerAndType(
        org.hyperic.hq.authz.shared.AuthzSubjectLocal subject,
        org.hyperic.hq.authz.shared.ResourceTypeLocal type )"
 *      query="SELECT OBJECT(r)
               FROM   Resource as r
                WHERE r.owner  = ?1
                  AND r.resourceType = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findViewableSvcRes_orderName(java.lang.Integer uid, java.lang.Boolean fSystem)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql 
        signature="java.util.Collection findViewableSvcRes_orderName(java.lang.Integer uid, java.lang.Boolean fSystem)"
 *      distinct="true"
 *      additional-columns=", EAM_RESOURCE.sort_name"
 *      from=",     EAM_SUBJECT_ROLE_MAP srm,
                    EAM_ROLE_OPERATION_MAP rom,
                    EAM_ROLE_RESOURCE_GROUP_MAP rgm,
                    EAM_RES_GRP_RES_MAP rgrm"
 *      where="     EAM_RESOURCE.fsystem = {1} 
                AND srm.role_id = rom.role_id 
                AND rom.role_id = rgm.role_id 
                AND rgm.resource_group_id = rgrm.resource_group_id 
                AND rgrm.resource_id = EAM_RESOURCE.id 
                AND ( srm.subject_id = {0} OR 
                      EAM_RESOURCE.subject_id = {0})
                AND EAM_RESOURCE.resource_type_id in (
                      SELECT rt.id FROM EAM_RESOURCE_TYPE rt
                      WHERE rt.name = 'covalentEAMService' or rt.name='covalentAuthzResourceGroup')
            AND rom.operation_id IN (
                 SELECT op.id FROM EAM_OPERATION op
                 WHERE op.name = 'viewService' OR op.name = 'viewResourceGroup')
            AND EAM_RESOURCE.id NOT IN (
                 SELECT ires.id 
                 FROM   EAM_RESOURCE ires, EAM_RESOURCE_GROUP igrp 
                 WHERE  igrp.resource_id=ires.id AND (igrp.groupType<>15 OR (igrp.groupType=15 AND igrp.cluster_id=-1)))"
 *      order="sort_name"
 *
 * @ejb:finder signature="java.util.Collection findSvcRes_orderName(java.lang.Boolean fSystem)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql 
        signature="java.util.Collection findSvcRes_orderName(java.lang.Boolean fSystem)"
 *      distinct="true"
 *      additional-columns=", EAM_RESOURCE.sort_name"
 *      from=",     EAM_SUBJECT_ROLE_MAP srm,
                    EAM_ROLE_OPERATION_MAP rom,
                    EAM_ROLE_RESOURCE_GROUP_MAP rgm,
                    EAM_RES_GRP_RES_MAP rgrm"
 *      where="     EAM_RESOURCE.fsystem = {0} 
                AND srm.role_id = rom.role_id 
                AND rom.role_id = rgm.role_id 
                AND rgm.resource_group_id = rgrm.resource_group_id 
                AND rgrm.resource_id = EAM_RESOURCE.id 
                AND EAM_RESOURCE.resource_type_id in (
                      SELECT rt.id FROM EAM_RESOURCE_TYPE rt
                      WHERE rt.name = 'covalentEAMService' or rt.name='covalentAuthzResourceGroup')
            AND rom.operation_id IN (
                 SELECT op.id FROM EAM_OPERATION op
                 WHERE op.name = 'viewService' OR op.name = 'viewResourceGroup')
            AND EAM_RESOURCE.id NOT IN (
                 SELECT ires.id 
                 FROM   EAM_RESOURCE ires, EAM_RESOURCE_GROUP igrp 
                 WHERE  igrp.resource_id=ires.id AND (igrp.groupType<>15 OR (igrp.groupType=15 AND igrp.cluster_id=-1)))"
 *      order="sort_name"
 *
 * @ejb:finder signature="java.util.Collection findInGroupAuthz_orderName_asc (
        java.lang.Integer userId, java.lang.Integer groupId,
        java.lang.Boolean fSystem)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql 
        signature="java.util.Collection findInGroupAuthz_orderName_asc (
        java.lang.Integer userId, java.lang.Integer groupId,
        java.lang.Boolean fSystem)"
 *      distinct="true"
 *      alias="r"
 *      additional-columns=", r.sort_name"
 *      from=",    EAM_SUBJECT_ROLE_MAP        srm,
                   EAM_ROLE_OPERATION_MAP      rom,
                   EAM_ROLE_RESOURCE_GROUP_MAP rgm,
                   EAM_RES_GRP_RES_MAP         rgrm,
                   EAM_RESOURCE_TYPE           rt,
                   EAM_OPERATION               op"
 *      where="    r.fsystem = {2}
               AND r.id IN (
                     SELECT resource_id FROM EAM_RES_GRP_RES_MAP map
                       WHERE map.resource_group_id={1})
               AND srm.role_id = rom.role_id
               AND rom.role_id = rgm.role_id
               AND rgm.resource_group_id = rgrm.resource_group_id
               AND rgrm.resource_id = r.id
               AND (srm.subject_id = {0} OR r.subject_id = {0} OR 'covalentAuthzInternalDsn' IN (SELECT dsn FROM EAM_SUBJECT WHERE id={0}))
               AND rom.operation_id=op.id
               AND r.resource_type_id=rt.id
               AND ((rt.name='covalentEAMPlatform'  AND op.name='viewPlatform')
               OR (rt.name='covalentEAMServer'      AND op.name='viewServer')
               OR (rt.name='covalentEAMService'     AND op.name='viewService')
               OR (rt.name='covalentEAMApplication' AND op.name='viewApplication')
               OR (rt.name='covalentAuthzResourceGroup' AND op.name='viewResourceGroup'
                   AND NOT r.instance_id = {1}))"
 *      order="r.sort_name"
 *
 * @ejb:finder signature="java.util.Collection findInGroup_orderName_asc (
        java.lang.Integer groupId, java.lang.Boolean fSystem)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql 
        signature="java.util.Collection findInGroup_orderName_asc (
        java.lang.Integer groupId, java.lang.Boolean fSystem)"
 *      distinct="true"
 *      alias="r"
 *      additional-columns=", r.sort_name"
 *      from=",    EAM_SUBJECT_ROLE_MAP        srm,
                   EAM_ROLE_OPERATION_MAP      rom,
                   EAM_ROLE_RESOURCE_GROUP_MAP rgm,
                   EAM_RES_GRP_RES_MAP         rgrm,
                   EAM_RESOURCE_TYPE           rt,
                   EAM_OPERATION               op"
 *      where="    r.fsystem = {1}
               AND r.id IN (
                     SELECT resource_id FROM EAM_RES_GRP_RES_MAP map
                       WHERE map.resource_group_id={0})
               AND srm.role_id = rom.role_id
               AND rom.role_id = rgm.role_id
               AND rgm.resource_group_id = rgrm.resource_group_id
               AND rgrm.resource_id = r.id
               AND (srm.subject_id = 1 OR r.subject_id = 1 OR 'covalentAuthzInternalDsn' IN (SELECT dsn FROM EAM_SUBJECT WHERE id=1))
               AND rom.operation_id=op.id
               AND r.resource_type_id=rt.id
               AND ((rt.name='covalentEAMPlatform'  AND op.name='viewPlatform')
               OR (rt.name='covalentEAMServer'      AND op.name='viewServer')
               OR (rt.name='covalentEAMService'     AND op.name='viewService')
               OR (rt.name='covalentEAMApplication' AND op.name='viewApplication')
               OR (rt.name='covalentAuthzResourceGroup' AND op.name='viewResourceGroup'
                   AND NOT r.instance_id = {0}))"
 *      order="r.sort_name"
 *
 * @ejb:select
 *      signature="java.util.Set ejbSelectDynamic (java.lang.String ejbQl, java.lang.Object[] args)"
 *      query=""
 * @jboss:query
 *      description="Method that accepts Dynamically constructed ejbQL"
 *      signature="java.util.Set ejbSelectDynamic (java.lang.String ejbQl, java.lang.Object[] args)"
 *      dynamic="true"
 *
 * @ejb:value-object
 *      name="Resource"
 *      match="*"
 *      instantiation="eager"
 *      cacheable="true"
 *      cacheDuration="300000"
 *
 * @jboss:create-table false
 * @jboss:remove-table false
 * @jboss:table-name table-name="EAM_RESOURCE"
 */

public abstract class ResourceEJBImpl extends AuthzNamedEntity
    implements EntityBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server.entity.ResourceEJBImpl");
    private final String ctx = ResourceEJBImpl.class.getName();
    private final String SEQUENCE_NAME = "EAM_RESOURCE_ID_SEQ";

    public ResourceEJBImpl() {}

   /**
    * EJBSelect method that executes Dynamic EJBQL and returns a set of
    * ResourceLocals that match the query.
    *
    * @param ejbQl - String containing the EJBQL statement.
    * @args - Object array of arguments to be bound into EJBQL String.
    * @return Set of matching ResourceLocals.
    * @throws FinderException
    */
   public abstract Set ejbSelectDynamic(String ejbQl, Object[] args)
       throws FinderException;

    /**
     * Find the scope of resources by operation in batch. Caller can 
     * provision a batch of resources and corresponding operations.
     * This is more flexible and efficient version of "findScopeByOperation" 
     * that takes advantage of JBoss's DynamicQL feature to construct one 
     * query from multiple arguments.
     *
     * @param subjLoc   valid spider subject
     * @param resLocArr batch of ReesourceLocals
     * @param opLocArr  batch of corresponding OperationLocals
     * @return Set of ResourceLocals within permission scope for operation
     * @exception FinderException when no resources found
     *
     * @ejb:home-method
     */
   public Set ejbHomeFindScopeByOperationBatch(AuthzSubjectLocal subjLoc, 
                                               ResourceLocal[] resLocArr,
                                               OperationLocal[] opLocArr) 
       throws FinderException
    {
       StringBuffer sb;
       Object[] parmArr;
       int noArgs;
       List parms;

       parms = new ArrayList();
       noArgs = (resLocArr.length + opLocArr.length + 1);
       sb = new StringBuffer();

       sb.append ("SELECT DISTINCT OBJECT(r) " )
         .append ( "FROM Resource as r,      " )
         .append ( "  IN(r.resourceGroups) g," )
         .append ( "  IN(g.roles) e,         " )
         .append ( "  IN(e.operations) o,    " )
         .append ( "  IN(e.subjects) s       " )
         .append ( "    WHERE s = ?1         " )
         .append ( "          AND (          " );

       parms.add(subjLoc);
       for (int x=2;x<noArgs;x+=2) {
           if (x>2) sb.append(" OR ");
           sb.append(" (o = ?"+x+" AND r = ?"+(x+1)+") ");
       }
       for (int x=0;x<opLocArr.length;x++) {
           parms.add((Object)opLocArr[x]);
           parms.add((Object)resLocArr[x]);
       }
       sb.append(")");
       parmArr = (Object[]) parms.toArray(new Object[0]);

       if (log.isDebugEnabled()) {
           log.debug ("ejbHomeFindScopeByOperationBatch() [query=" +
                      sb.toString() + "]");
       }
       return ejbSelectDynamic(sb.toString(), parmArr);
    }

    /**
     * Find the scope of resources by operation in batch. Caller can 
     * provision a batch of resources and corresponding operations.
     * This is more flexible and efficient version of "findScopeByOperation" 
     * that takes advantage of JBoss's DynamicQL feature to construct one 
     * query from multiple arguments.
     *
     * @param subjLoc   valid spider subject
     * @param resLocArr batch of ReesourceLocals
     * @param opLocArr  batch of corresponding OperationLocals
     * @return Set of ResourceLocals within permission scope for operation
     * @exception FinderException when no resources found
     *
     * @ejb:home-method
     */
   public Set ejbHomeFindScopeByOperationBatch(ResourceLocal[] resLocArr)
       throws FinderException
    {
       StringBuffer sb;
       Object[] parmArr;
       int noArgs;
       List parms;

       parms = new ArrayList();
       noArgs = resLocArr.length + 1;
       sb = new StringBuffer();

       sb.append ("SELECT DISTINCT OBJECT(r) " )
         .append ( "FROM Resource as r, "      )
         .append ( "  IN(r.resourceGroups) g " )
         .append ( "      WHERE "              );

       for (int x = 1; x < noArgs; x++) {
            if (x > 1)
                sb.append(" OR ");
            sb.append(" r = ?" + x + " ");
            parms.add((Object) resLocArr[x - 1]);
        }

       parmArr = (Object[]) parms.toArray(new Object[0]);

       if (log.isDebugEnabled()) {
           log.debug ("ejbHomeFindScopeByOperationBatch() [query=" +
                      sb.toString() + "]");
       }
       return ejbSelectDynamic(sb.toString(), parmArr);
    }

    /** Sort name of this EJB
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
     * Instance Id of this resource
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:column-name name="INSTANCE_ID"
     * @jboss:read-only true
     */
    public abstract Integer getInstanceId();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setInstanceId(Integer id);

    /**
     * Get the System flag which determines whether this resource
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
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.authz.shared.ResourceValue getResourceValue();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceValue(org.hyperic.hq.authz.shared.ResourceValue value);

    /**
     * Hand coded value object getter
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ResourceValue getResourceValueObject() {
        ResourceValue vo = new ResourceValue();
        vo.setSortName(getSortName());
        vo.setInstanceId(getInstanceId());
        vo.setSystem(getSystem());
        vo.setName((getName() == null) ? "" : getName());
        vo.setId(getId());
        if ( getOwner() != null )
            vo.setAuthzSubjectValue( getOwner().getAuthzSubjectValue() );
        else
            vo.setAuthzSubjectValue( null ); 
        return vo;
    }

    /**
     * Get the ResourceType of this Resource.
     * @ejb:interface-method
     * @ejb:relation
     *      name="resource-resourcetype"
     *      role-name="resource-is-a-kind-of-resourcetype"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      match="*"
     *      aggregate="org.hyperic.hq.authz.shared.ResourceTypeValue"
     *      aggregate-name="ResourceTypeValue"
     * @jboss:relation
     *      fk-column="RESOURCE_TYPE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract ResourceTypeLocal getResourceType();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setResourceType(ResourceTypeLocal rtLocal);

    /**
     * Get the Owner of this Resource.
     * @ejb:interface-method
     * @ejb:relation
     *      name="subject-resource"
     *      role-name="resource-has-an-owner"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      match="*"
     *      aggregate="org.hyperic.hq.authz.shared.AuthzSubjectValue"
     *      aggregate-name="AuthzSubjectValue"
     * @jboss:relation
     *      fk-column="SUBJECT_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract AuthzSubjectLocal getOwner();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setOwner(AuthzSubjectLocal subject);

    /** Get the resource groups this resource belongs to.
     * @ejb:interface-method
     * @ejb:relation
     *      name="resourcegroups-resources"
     *      role-name="resources-belong-to-resourcegroups"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
     *      table-name="EAM_RES_GRP_RES_MAP"
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

    /**
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     * @jboss:read-only true
     */
    public boolean isOwner(Integer possibleOwner) {
        boolean is = false;

        if (possibleOwner == null) {
            log.error("possible Owner is NULL. This is probably not what you want.");
            /* XXX throw exception instead */
        } else {
            /* overlord owns every thing */
            if (is = possibleOwner.equals(AuthzConstants.overlordId)
                    == false) {
                if (log.isDebugEnabled() && possibleOwner != null) {
                    log.debug("User is " + possibleOwner +
                              " owner is " + getOwner().getId());
                }
                is = (possibleOwner.equals(getOwner().getId()));
            }
        }
        return is;
    }

    /**
     * @ejb:create-method
     * @ejb:transaction type="MANDATORY"
     */
    public ResourcePK ejbCreate(AuthzSubjectLocal whoami, ResourceValue value)
        throws CreateException {

        super.ejbCreate(ctx, SEQUENCE_NAME);
        Integer instanceId = value.getInstanceId();
        if (instanceId == null) {
            throw new CreateException("Instance id must be set");
        }
        setInstanceId(instanceId);
        setName(value.getName());
        if (value.getName()!=null)
            setSortName(value.getName().toUpperCase());
        setSystem(value.getSystem());
        return new ResourcePK(getId());
    }

    public void ejbPostCreate(AuthzSubjectLocal whoami, ResourceValue value)
        throws CreateException {
        ResourceTypeLocalHome typeLome = null;
        AuthzSubjectLocalHome ownerLome = null;
        ResourceTypeValue typeValue = null;
        AuthzSubjectValue ownerValue = null;

        try {
            typeLome = ResourceTypeUtil.getLocalHome();
            ownerLome = AuthzSubjectUtil.getLocalHome();
        } catch (NamingException e) {
            throw new CreateException("Naming exception.");
        }

        /* set resource type */
        typeValue = value.getResourceTypeValue();

        if (typeValue == null) {
            throw new CreateException("Null resourceType given.");
        }

        try {
            ResourceTypeLocal typeLocal =
                typeLome.findByPrimaryKey(typeValue.getPrimaryKey()); 
            setResourceType(typeLocal);
        } catch (FinderException e) {
            throw new CreateException("Null resourceType given.");
        }

        /* set owner */
        ownerValue = value.getAuthzSubjectValue();
        if (ownerValue == null) {
            /* if no owner given, default to current user */            
            setOwner(whoami);
        } else {
            try {
                AuthzSubjectLocal ownerLocal =
                    ownerLome.findByPrimaryKey(ownerValue.getPrimaryKey());
                setOwner(ownerLocal);
            } catch (FinderException e) {
                throw new CreateException("Unable to find given owner.");
            }
        }
        /* add it to the root resourcegroup */
        /* This is done as the overlord, since it is meant to be an
           anonymous, priviledged operation */
        try {
            ResourceGroupLocal authzGroup =
                ResourceGroupUtil.getLocalHome().findByName(
                    AuthzConstants.rootResourceGroupName);
            Set groups = new HashSet(1);
            groups.add(authzGroup);
            this.setResourceGroups(groups);
        } catch (FinderException e) {
            throw new CreateException("Failed to addToRootResourceGroup: " +
                                      e.getMessage());
        } catch (NamingException e) {
            throw new CreateException("Failed to addToRootResourceGroup: " +
                                      e.getMessage());
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

    /**
     * Removes a resource.
     * @see ResourceType.ejbRemove()
     * ideally, there should be one and only one check for resource
     * deletion. it would be right here.
     * it'd look like this
     *  if (!isOwner(getWhoami()) {
     *      throw new RemoveException("Only an owner can delete.");
     *  }
     *
     * however, there's no good way to ensure that whoami would be set,
     * especially in the case of cascading deletes.
     *
     * thus the ownership checks will have to occur in all of the
     * following methods instead
     * ResourceGroupEJBImpl.ejbRemove()
     * ResourceTypeEJBImpl.ejbRemove()
     * RoleEJBImpl.ejbRemove()
     * AuthzSubjectEJBImpl.ejbRemove()
     * ResourceManager.removeResource()
     *
     * one for each of the internal authz resources and one in the session
     * for consumers' resources.
     */
    public void ejbRemove() throws RemoteException, RemoveException {

    }

}

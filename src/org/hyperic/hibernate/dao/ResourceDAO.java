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

package org.hyperic.hibernate.dao;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hyperic.hq.authz.AuthzSubject;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.ResourceGroup;
import org.hyperic.hq.authz.Operation;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

/**
 * CRUD methods, finders, etc. for Resource
 */
public class ResourceDAO extends HibernateDAO
{
    Log log = LogFactory.getLog(ResourceDAO.class);

    public ResourceDAO(Session session) {
        super(Resource.class, session);
    }

    public Resource create(AuthzSubject creator, ResourceValue createInfo) {
        /* set resource type */
        ResourceTypeValue typeValue = createInfo.getResourceTypeValue();

        if (typeValue == null) {
            // XXX - decide what exception to throw here
            // throw new CreateException("Null resourceType given.");
            throw new IllegalArgumentException(
                "ResourceTypevValue is not defined");
        }
        Resource resource = new Resource(createInfo);

        ResourceType resType =
            new ResourceTypeDAO(getSession()).findById(typeValue.getId());
        resource.setResourceType(resType);

        /* set owner */
        AuthzSubjectValue ownerValue = createInfo.getAuthzSubjectValue();
        if (ownerValue != null) {
            creator = (new AuthzSubjectDAO(getSession()))
                .findById(ownerValue.getId());
        }
        resource.setOwner(creator);
        save(resource);

        /* add it to the root resourcegroup */
        /* This is done as the overlord, since it is meant to be an
           anonymous, priviledged operation */
        ResourceGroup authzGroup = new ResourceGroupDAO(getSession())
            .findByName(AuthzConstants.rootResourceGroupName);
        if (authzGroup == null) {
            throw new IllegalArgumentException("can not find Resource Group: "+
                                               AuthzConstants.rootResourceGroupName);
        }
        resource.addResourceGroup(authzGroup);

        return resource;
    }

    public Resource findById(Integer id) {
        return (Resource) super.findById(id);
    }

    public void save(Resource entity) {
        super.save(entity);
    }

    public Resource merge(Resource entity) {
        return (Resource) super.merge(entity);
    }

    public void remove(Resource entity) {
        super.remove(entity);
    }

    public void evict(Resource entity) {
        super.evict(entity);
    }

    public boolean isOwner(Resource entity, Integer possibleOwner) {
        boolean is = false;

        if (possibleOwner == null) {
            log.error("possible Owner is NULL. " +
                    "This is probably not what you want.");
            /* XXX throw exception instead */
        } else {
            /* overlord owns every thing */
            if (is = possibleOwner.equals(AuthzConstants.overlordId)
                    == false) {
                if (log.isDebugEnabled() && possibleOwner != null) {
                    log.debug("User is " + possibleOwner +
                              " owner is " + entity.getOwner().getId());
                }
                is = (possibleOwner.equals(entity.getOwner().getId()));
            }
        }
        return is;
    }

    public Resource findByInstanceId(ResourceType type, Integer id) {            
        String sql = "from Resource where instanceId = ? and" +
                     " resourceType.id = ?";
        return (Resource)getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, type.getId().intValue())
            .uniqueResult();
    }
    
    public Resource findByInstanceId(Integer typeId, Integer id) {            
        String sql = "from Resource where instanceId = ? and" +
                     " resourceType.id = ?";
        return (Resource)getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, typeId.intValue())
            .uniqueResult();
    }
    
    public Collection findByOwner(AuthzSubject owner) {
        String sql = "from Resource where owner.id = ?";
        return getSession().createQuery(sql)
                .setInteger(0, owner.getId().intValue())
                .list();
    }
    
    public Collection findByOwnerAndType(AuthzSubject owner,
                                         ResourceType type ) {
        String sql = "from Resource where owner.id = ? and resourceType.id = ?";
        return getSession().createQuery(sql)
            .setInteger(0, owner.getId().intValue())
            .setInteger(1, type.getId().intValue())
            .list();
    }

    public Collection findViewableSvcRes_orderName(Integer user,
                                                   Boolean fSystem)
    {
        // we use join fetch here to produce a single
        // outer join => the strategy here is to rely on
        // the database query optimizer to optimize the query
        // If the database is too slow, only then
        // we should remove the "fetch" clause
        // and let hibernate issue multiple selects to
        // fetch association subgraph.
        // The important point is we should first give the
        // opportunity to the database to do the "query" optimization
        // before we do anything else.
        // Note: this should be refactored to use named queries so
        // that we can perform "fetch" optimization outside of the code
        String sql="select distinct r from Resource r " +
                   " join fetch r.resourceGroups rg " +
                   " join fetch rg.roles role " +
                   " join fetch role.subjects subj " +
                   " join fetch role.operations op " +
                   " join fetch r.resourceType rt " +
                   "where " +
                   "  r.system=? and " +
                   "  (subj.id=? or r.owner.id=?) and " +
                   "  (rt.name='covalentEAMService' or " +
                   "   rt.name='covalentAuthzResourceGroup') and " +
                   "  (op.name='viewService' or " +
                   "   op.name='viewResourceGroup') and " +
                   "  rg.groupType = 15 and " +
                   "  (rg.groupType != 15 or rg.clusterId!=-1) " +
                   "order by r.sortName ";
        return getSession().createQuery(sql)
            .setInteger(0, user.intValue())
            .setInteger(1, user.intValue())
            .setBoolean(2, fSystem.booleanValue())
            .list();
    }

    public Collection findSvcRes_orderName(Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join fetch r.resourceGroups rg " +
                   " join fetch rg.roles role " +
                   " join fetch role.operations op " +
                   " join fetch r.resourceType rt " +
                   "where " +
                   "  r.system=? and " +
                   "  (rt.name='covalentEAMService' or " +
                   "   rt.name='covalentAuthzResourceGroup') and " +
                   "  (op.name='viewService' or " +
                   "   op.name='viewResourceGroup') and " +
                   "  rg.groupType = 15 and " +
                   "  (rg.groupType != 15 or rg.clusterId!=-1) " +
                   "order by r.sortName ";
        return getSession().createQuery(sql)
            .setBoolean(0, fSystem.booleanValue())
            .list();
    }

    public Collection findInGroupAuthz_orderName(Integer userId,
                                                 Integer groupId,
                                                 Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join fetch r.resourceGroups rg " +
                   " join fetch rg.roles role " +
                   " join fetch role.subjects subj " +
                   " join fetch role.operations op " +
                   "where " +
                   " r.system=? and " +
                   " (subj.id=? or " +
                   "  r.owner.id=? or " +
                   "  subj.authDsn = 'covalentAuthzInternalDsn') and " +
                   " op.resourceType.id = r.resourceType.id and " +
                   " (op.name = 'viewPlatform' or " +
                   "  op.name = 'viewServer' or " +
                   "  op.name = 'viewService' or " +
                   "  op.name = 'viewApplication' or " +
                   "  op.name = 'viewApplication' or " +
                   "  (op.name='viewResourceGroup' and " +
                   "    not r.instanceId ='viewResourceGroup') )" +
                   " order by r.sortName ";
        return getSession().createQuery(sql)
            .setInteger(0, userId.intValue())
            .setInteger(1, groupId.intValue())
            .setBoolean(2, fSystem.booleanValue())
            .list();
    }

    public Collection findInGroup_orderName(Integer groupId,
                                            Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join fetch r.resourceGroups rg " +
                   " join fetch rg.roles role " +
                   " join fetch role.subjects subj " +
                   " join fetch role.operations op " +
                   "where " +
                   " r.system=? and " +
                   " rg.id=? and " +
                   " (subj.id=1 or r.owner.id=1 or " +
                   "  subj.authDsn = 'covalentAuthzInternalDsn') and " +
                   " op.resourceType.id = r.resourceType.id and " +
                   " (op.name = 'viewPlatform' or " +
                   "  op.name = 'viewServer' or " +
                   "  op.name = 'viewService' or " +
                   "  op.name = 'viewApplication' or " +
                   "  op.name = 'viewApplication' or " +
                   "  (op.name='viewResourceGroup' and " +
                   "    not r.instanceId=?) )" +
                   " order by r.sortName ";
        return getSession().createQuery(sql)
            .setBoolean(0, fSystem.booleanValue())
            .setInteger(1, groupId.intValue())
            .setInteger(2, groupId.intValue())
            .list();
    }

    public Collection findScopeByOperationBatch(AuthzSubject subjLoc,
                                                Resource[] resLocArr,
                                                Operation[] opLocArr)
    {
        StringBuffer sb = new StringBuffer();

        sb.append ("SELECT DISTINCT r " )
            .append ( "FROM Resource r      " )
            .append ( "  join fetch r.resourceGroups g" )
            .append ( "  join fetch g.roles e         " )
            .append ( "  join fetch e.operations o    " )
            .append ( "  join fetch e.subjects s       " )
            .append ( "    WHERE s.id = ?         " )
            .append ( "          AND (          " );

        for (int x=0; x< resLocArr.length ; x++) {
            if (x>1) sb.append(" OR ");
            sb.append(" (o.id=")
                .append(opLocArr[x].getId())
                .append(" AND r.id=")
                .append(resLocArr[x].getId())
                .append(") ");
        }
        sb.append(")");
        return getSession().createQuery(sb.toString())
            .setInteger(0, subjLoc.getId().intValue())
            .list();
    }

    public Collection findScopeByOperationBatch(Resource[] resLocArr)
    {
        StringBuffer sb = new StringBuffer();

        sb.append ("SELECT DISTINCT r " )
            .append ( "FROM Resource r "      )
            .append ( "  join fetch r.resourceGroups g " )
            .append ( "WHERE "              );

        for (int x = 0; x < resLocArr.length; x++) {
            if (x > 0)
                sb.append(" OR ");
            sb.append(" r.id=")
                .append(resLocArr[x].getId());
        }
        if (log.isDebugEnabled()) {
            log.debug ("findScopeByOperationBatch() [query=" +
                       sb.toString() + "]");
        }
        return getSession().createQuery(sb.toString())
            .list();
    }
}

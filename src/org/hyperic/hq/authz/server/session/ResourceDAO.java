/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;

public class ResourceDAO 
    extends HibernateDAO
{
    private Log _log = LogFactory.getLog(ResourceDAO.class);

    public ResourceDAO(DAOFactory f) { 
        super(Resource.class, f);
    }

    Resource create(ResourceType type, Resource prototype, String name, 
                    AuthzSubject creator, Integer instanceId, boolean system) 
    {
        if (type == null) {
            throw new IllegalArgumentException("ResourceTypevValue is not " +
                                               "defined");
            
        }
        Resource resource = new Resource(type, prototype, name, creator, 
                                         instanceId, system);
                                         
        save(resource);

        /* add it to the root resourcegroup */
        ResourceGroupDAO gDao = 
            DAOFactory.getDAOFactory().getResourceGroupDAO();
        ResourceGroup authzGroup = gDao.findRootGroup();
        gDao.addMembers(authzGroup, Collections.singleton(resource));

        // Need to flush so that later permission checking can succeed
        getSession().flush();
        
        return resource;
    }

    public Resource findRootResource() {
        return findById(AuthzConstants.rootResourceId);
    }
    
    public Resource findById(Integer id) {
        return (Resource) super.findById(id);
    }

    public void save(Resource entity) {
        super.save(entity);
    }

    // XXX: What about the preAppdefResourcesDelete or resource edge tables?
    public void remove(Resource entity) {
        ResourceGroupDAO gDao = getFactory().getResourceGroupDAO();
        // Is this really necessary?  We should technically always make sure
        // the group is optimistically updated even when resources are removed.
        for (Iterator i = gDao.getGroups(entity).iterator(); i.hasNext(); ) {
            ResourceGroup group = (ResourceGroup)i.next();
            
            group.markDirty();
        }
        
        String sql = "delete GroupMember g where g.resource = :resource";
        getSession().createQuery(sql)
            .setParameter("resource", entity)
            .executeUpdate();

        getSession().flush();
        super.remove(entity);
    }

    public boolean isOwner(Resource entity, Integer possibleOwner) {
        boolean is = false;

        if (possibleOwner == null) {
            _log.error("possible Owner is NULL. " +
                       "This is probably not what you want.");
            /* XXX throw exception instead */
        } else {
            /* overlord owns every thing */
            if (is = possibleOwner.equals(AuthzConstants.overlordId) == false) {
                if (_log.isDebugEnabled() && possibleOwner != null) {
                    _log.debug("User is " + possibleOwner +
                               " owner is " + entity.getOwner().getId());
                }
                is = (possibleOwner.equals(entity.getOwner().getId()));
            }
        }
        return is;
    }

    void deleteByInstances(AppdefEntityID[] ids) {
        ResourceStartupListener.getCallbackObj().preAppdefResourcesDelete(ids);

        new ResourceEdgeDAO(DAOFactory.getDAOFactory()).deleteEdges(ids);

        // Remove from group map table
        for (int i = 0; i < ids.length; i++) {
            Resource r = findByInstanceId(ids[i].getAuthzTypeId(),
                                          ids[i].getId());
            remove(r);
        }
    }

    public Resource findByInstanceId(ResourceType type, Integer id) {
        return findByInstanceId(type.getId(), id);
    }

    List findResourcesOfType(int typeId, PageInfo pInfo) {
        String sql = "from Resource r where resourceType.id = :typeId ";
        ResourceSortField sort = (ResourceSortField)pInfo.getSort();
        
        sql += " order by " + sort.getSortString("r") + 
               (pInfo.isAscending() ? "" : " DESC");
        
        return pInfo.pageResults(getSession().createQuery(sql)
                                             .setInteger("typeId", typeId))
                    .list();
    }
    
    public Resource findByInstanceId(Integer typeId, Integer id) {            
        String sql = "from Resource where resourceType.id = ? " +
                     "and instanceId = ?";
        return (Resource)getSession().createQuery(sql)
            .setInteger(0, typeId.intValue())
            .setInteger(1, id.intValue())
            .setCacheable(true)
            .setCacheRegion("Resource.findByInstanceId")
            .uniqueResult();
    }
        
    /**
     * Find a Resource by type Id and instance Id, allowing for the query to 
     * return a stale copy of the resource (for efficiency reasons).
     * 
     * @param typeId The type Id.
     * @param id The instance Id.
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The Resource.
     */
    public Resource findByInstanceId(Integer typeId, Integer id, boolean allowStale) {                    
        FlushMode oldFlushMode = this.getSession().getFlushMode();
        
        try {
            if (allowStale) {
                this.getSession().setFlushMode(FlushMode.MANUAL);                
            }
            
            return findByInstanceId(typeId, id);            
        } finally {
            this.getSession().setFlushMode(oldFlushMode);
        }
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
        // we use join here to produce a single
        // join => the strategy here is to rely on
        // the database query optimizer to optimize the query
        // by feeding it a single query.
        //
        // The important point is we should first give the
        // opportunity to the database to do the "query" optimization
        // before we do anything else.
        // Note: this should be refactored to use named queries so
        // that we can perform "fetch" optimization outside of the code
        String sql =
            "select r from Resource r join r.resourceType rt " +
            "where r.system = :system and exists " +
                 "(select rg from ResourceGroup rg " +
                 " join rg.memberBag g " +
                 " join g.resource rs " +
                 "where ((rg.resource = r and rg.groupType = 15) or " +
                        "(rt.name = :resSvcType and r = rs)) " +
                 " and (r.owner.id = :subjId or exists " +
                      "(select role from rg.roles role " +
                                   "join role.subjects subj " +
                                   "join role.operations op " +
                       "where subj.id = :subjId and " +
                             "op.name = case rt.name " +
                                       "when (:resSvcType) then '" +
                                       AuthzConstants.serviceOpViewService +
                                       "' " +
                                       "else '" +
                                       AuthzConstants.groupOpViewResourceGroup +
                                       "' end))) " +
            "order by r.sortName";
        List resources =
            getSession().createQuery(sql)
                        .setBoolean("system", fSystem.booleanValue())
                        .setInteger("subjId", user.intValue())
                        .setString("resSvcType", AuthzConstants.serviceResType)
                        .list();

        return resources;
    }

    public Collection findSvcRes_orderName(Boolean fSystem)
    {
        String sql =
            "select r from Resource r join r.resourceType rt " +
            "where r.system = :system and " +
                  "(rt.name = :resSvcType or " +
                   "exists (select rg from ResourceGroup rg " +
                                     "join rg.resource r2 " +
                           "where r = r2 and rg.groupType = 15)) " +
                   "order by r.sortName ";
        
        List resources =
            getSession().createQuery(sql)
                        .setBoolean("system", fSystem.booleanValue())
                        .setString("resSvcType", AuthzConstants.serviceResType)
                        .list();
        
        return resources;
    }

    public Collection findInGroupAuthz_orderName(Integer userId,
                                                 Integer groupId,
                                                 Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join r.resourceGroups rgg" +
                   " join r.resourceGroups rg " +
                   " join rg.roles role " +
                   " join role.subjects subj " +
                   " join role.operations op " +
                   "where " +
                   " r.system = :system and " +
                   " rgg.id = :groupId and " +
                   " (subj.id = :subjectId or " +
                   "  r.owner.id = :subjectId or " +
                   "  subj.authDsn = 'covalentAuthzInternalDsn') and " +
                   " op.resourceType.id = r.resourceType.id and " +
                   " (" +
                   "  op.name = 'viewPlatform' or " +
                   "  op.name = 'viewServer' or " +
                   "  op.name = 'viewService' or " +
                   "  op.name = 'viewApplication' or " +
                   "  op.name = 'viewResourceGroup' )" +
                   " order by r.sortName ";
        return getSession().createQuery(sql)
            .setBoolean("system", fSystem.booleanValue())
            .setInteger("groupId", groupId.intValue())
            .setInteger("subjectId", userId.intValue())
            .list();
    }

    public Collection findInGroup_orderName(Integer groupId,
                                            Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join r.resourceGroups rgg" +
                   " join r.resourceGroups rg " +
                   " join rg.roles role " +
                   " join role.subjects subj " +
                   " join role.operations op " +
                   "where " +
                   " r.system = :system and " +
                   " rgg.id = :groupId and " +
                   " (subj.id=1 or r.owner.id=1 or " +
                   "  subj.authDsn = 'covalentAuthzInternalDsn') and " +
                   " op.resourceType.id = r.resourceType.id and " +
                   " (op.name = 'viewPlatform' or " +
                   "  op.name = 'viewServer' or " +
                   "  op.name = 'viewService' or " +
                   "  op.name = 'viewApplication' or " +
                   "  op.name='viewResourceGroup' )" +
                   " order by r.sortName ";

        return getSession().createQuery(sql)
            .setBoolean("system", fSystem.booleanValue())
            .setInteger("groupId", groupId.intValue())
            .list();
    }

    public Collection findScopeByOperationBatch(AuthzSubject subjLoc,
                                                Resource[] resLocArr,
                                                Operation[] opLocArr)
    {
        StringBuffer sb = new StringBuffer();

        sb.append ("SELECT DISTINCT r " )
            .append ( "FROM Resource r      " )
            .append ( "  join r.resourceGroups g" )
            .append ( "  join g.roles e         " )
            .append ( "  join e.operations o    " )
            .append ( "  join e.subjects s       " )
            .append ( "    WHERE s.id = ?         " )
            .append ( "          AND (          " );

        for (int x=0; x< resLocArr.length ; x++) {
            if (x>0) sb.append(" OR ");
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
    
    /**
     * Returns an ordered list of instance IDs for a given operation.
     */
    public List findAllResourcesInstancesForOperation(int opId) {
        final String sql = 
            "SELECT r.instanceId FROM Resource r, Operation o " +
            "WHERE     o.resourceType = r.resourceType" + 
            "      AND o.id = :opId";
        
        return getSession().createQuery(sql)
            .setInteger("opId", opId)
            .list();
    }
    
    int reassignResources(int oldOwner, int newOwner) {
        return getSession().createQuery("UPDATE Resource " +
                                        "SET owner.id = :newOwner " +
                                        "WHERE owner.id = :oldOwner")
            .setInteger("oldOwner", oldOwner)
            .setInteger("newOwner", newOwner)
            .executeUpdate();
    }
    
    boolean resourcesExistOfType(String typeName) {
        String sql = "select r from Resource r " + 
            "join r.prototype p " +
            "where p.name = :protoName";
        
        return getSession().createQuery(sql)
            .setParameter("protoName", typeName)
            .setMaxResults(1)
            .list().isEmpty() == false;
    }
    
    List findResourcesOfPrototype(Resource proto, PageInfo pInfo) {
        String sql = "select r from Resource r " + 
            "where r.prototype = :proto";
        
        return pInfo.pageResults(getSession().createQuery(sql)
                                     .setParameter("proto", proto)).list();
    }
    
    Resource findResourcePrototypeByName(String name) {
        String sql = "select r from Resource r " + 
            "where r.name = :name " +
            " AND r.resourceType.id in (:platProto, :svrProto, :svcProto)"; 
        
        return (Resource)getSession().createQuery(sql)
            .setParameter("name", name)
            .setParameter("platProto", AuthzConstants.authzPlatformProto)
            .setParameter("svrProto", AuthzConstants.authzServerProto)
            .setParameter("svcProto", AuthzConstants.authzServiceProto)
            .uniqueResult();
    }
    
    List findAllAppdefPrototypes() {
        String sql = "select r from Resource r " +
            "where r.resourceType.id in (:platProto, :svrProto, :svcProto)";
        
        return (List)getSession().createQuery(sql)
            .setParameter("platProto", AuthzConstants.authzPlatformProto)
            .setParameter("svrProto", AuthzConstants.authzServerProto)
            .setParameter("svcProto", AuthzConstants.authzServiceProto)
            .list();
    }
}

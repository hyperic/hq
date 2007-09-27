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

package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
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

    Resource create(ResourceType type, String name, AuthzSubject creator, 
                    Integer instanceId, boolean system)
    {
        if (type == null) {
            throw new IllegalArgumentException("ResourceTypevValue is not " +
                                               "defined");
            
        }
        Resource resource = new Resource(type, name, creator, instanceId,
                                         system);
        save(resource);

        /* add it to the root resourcegroup */
        ResourceGroup authzGroup =
            new ResourceGroupDAO(DAOFactory.getDAOFactory()).findRootGroup();
        authzGroup.addResource(resource);

        // Need to flush so that later permission checking can succeed
        getSession().flush();
        
        return resource;
    }

    public Resource findById(Integer id) {
        return (Resource) super.findById(id);
    }

    public void save(Resource entity) {
        super.save(entity);
    }

    public void remove(Resource entity) {
        // remove resource from all resourceGroups
        // Currently the resourceGroup collection is the inverse=true
        // end of many-to-many with ResourceGroup
        // Have to iterate thru resoucegroups and remove from each group
        // the resource belongs to.  Wish Hibernate supported cascade deletes
        // on many-to-many collections.
        for (Iterator i = entity.getResourceGroups().iterator(); i.hasNext();) {
            ResourceGroup rg = (ResourceGroup) i.next();
            rg.removeResource(entity);
        }
        entity.getResourceGroups().clear();        
        
        // Remove any references to the group from the group_resource map
        String sql = "delete ResGrpResMap g where g.id.resource = :resource";
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
            if (is = possibleOwner.equals(AuthzConstants.overlordId)
                    == false) {
                if (_log.isDebugEnabled() && possibleOwner != null) {
                    _log.debug("User is " + possibleOwner +
                               " owner is " + entity.getOwner().getId());
                }
                is = (possibleOwner.equals(entity.getOwner().getId()));
            }
        }
        return is;
    }

    private Map groupByAuthzType(AppdefEntityID[] ids) {

        HashMap m = new HashMap();
        for (int i = 0; i < ids.length; i++) {
            String type =
                AppdefUtil.appdefTypeIdToAuthzTypeStr(ids[i].getType());
            ArrayList idList = (ArrayList)m.get(type);
            if (idList == null) {
                idList = new ArrayList();
                m.put(type, idList);
            }
            idList.add(ids[i].getId());
        }
        return m;
    }

    private int deleteResourceObject(AppdefEntityID[] ids, String object,
                                     String col)
    {
        Map map = groupByAuthzType(ids);
        StringBuffer sql = new StringBuffer()
            .append("delete ")
            .append(object)
            .append(" where ");
        for (int i = 0; i < map.size(); i++) {
            if (i > 0) {
                sql.append(" or ");
            }
            sql.append(col)
                .append(" in (")
                .append("select r.id from Resource r, " +
                        "ResourceType rt where r.resourceType.id=rt.id and ")
                .append("rt.name = :rtname" + i + " and " )
                .append("r.instanceId in (:list" +  i + ") ")
                .append(") ");
        }
        int j = 0;
        Query q = getSession().createQuery(sql.toString());
        for (Iterator i = map.keySet().iterator(); i.hasNext(); j++) {
            String rtname = (String)i.next();
            List list = (List)map.get(rtname);
            q.setString("rtname" + j, rtname)
                .setParameterList("list" + j, list);
        }
        return q.executeUpdate();
    }

    public int deleteByInstances(AppdefEntityID[] ids) {
        ResourceStartupListener.getCallbackObj().preAppdefResourcesDelete(ids);
        
        // kludge to work around hiberate's limitation to define
        // on-delete="cascade" on many-to-many relationships
        deleteResourceObject(ids, "ResGrpResMap", "id.resource.id");
        
        // Now delete the resources
        Map map = groupByAuthzType(ids);
        StringBuffer sql = new StringBuffer("delete Resource where ");
        
        for (int i = 0; i < map.size(); i++) {
            if (i > 0) {
                sql.append(" or ");
            }

            sql.append("(instanceId in (:list" +  i + ") and resourceType.id=");
            
            sql.append("(select rt.id from ResourceType rt " +
                        "where rt.name = :rtname" + i + ")");
            
            sql.append(')');
        }
        
        Query q = getSession().createQuery(sql.toString());
        int j = 0;
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); j++) {
            Map.Entry entry = (Map.Entry) i.next();
            q.setString("rtname" + j, (String) entry.getKey())
             .setParameterList("list" + j, (List) entry.getValue());
        }
        return q.executeUpdate();
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
        String sql = "from Resource where instanceId = ? and" +
                     " resourceType.id = ?";
        return (Resource)getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .setInteger(1, typeId.intValue())
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
        String sql = "select distinct r from Resource r " +
                     " join r.resourceGroups rg " +
                     " join rg.roles role " +
                     " join role.subjects subj " +
                     " join role.operations op " +
                     " join r.resourceType rt " +
                     "where " +
                     "  r.system = :system and " +
                     "  (subj.id = :subjId or r.owner.id = :subjId) and " +
                     "  (" +
                     "   rt.name = 'covalentEAMService' or " +
                     "   (rt.name = 'covalentAuthzResourceGroup' and " +
                     "   0 < (select count(*) from Resource r2 " +
                     "        join r2.resourceGroups rg2 " +
                     "        where r.id = r2.id and rg2.groupType = 15 and" +
                     "              rg2.clusterId != -1) )) and " +
                     "  (" +
                     "   op.name = 'viewService' or " +
                     "   op.name = 'viewResourceGroup') " +
                     "order by r.sortName ";
        List resources =
            getSession().createQuery(sql)
                        .setBoolean("system", fSystem.booleanValue())
                        .setInteger("subjId", user.intValue())
                        .list();

        // Hibernate's distinct does not work well with joins - do filter here
        Integer lastId = null; // Track the last one we looked at
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Resource res = (Resource) it.next();
            if (res.getId().equals(lastId)) {
                it.remove();
            } else {
                lastId = res.getId();
            }
        }

        return resources;
    }

    public Collection findSvcRes_orderName(Boolean fSystem)
    {
        String sql="select distinct r from Resource r " +
                   " join r.resourceGroups rg " +
                   " join rg.roles role " +
                   " join role.operations op " +
                   " join r.resourceType rt " +
                   "where " +
                   "  r.system = ? and " +
                   "  (" +
                   "   rt.name = 'covalentEAMService' or " +
                   "   (rt.name = 'covalentAuthzResourceGroup' and " +
                   "   0 < (select count(*) from Resource r2 " +
                   "        join r2.resourceGroups rg2 " +
                   "        where r.id = r2.id and rg2.groupType = 15 and" +
                   "              rg2.clusterId != -1))) and " +
                   "  (" +
                   "   op.name = 'viewService' or " +
                   "   op.name = 'viewResourceGroup') " +
                   "order by r.sortName ";
        
        List resources =
            getSession().createQuery(sql)
                        .setBoolean(0, fSystem.booleanValue())
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
                   "  op.name = 'viewApplication' or " +
                   "  (op.name='viewResourceGroup' and " +
                   "    not r.instanceId = :groupId) )" +
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
                   "  op.name = 'viewApplication' or " +
                   "  (op.name='viewResourceGroup' and " +
                   "    not r.instanceId = :groupId) )" +
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
}

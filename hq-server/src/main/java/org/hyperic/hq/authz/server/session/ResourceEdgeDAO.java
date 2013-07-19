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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceEdgeDAO
    extends HibernateDAO<ResourceEdge> {
    @Autowired
    ResourceEdgeDAO(SessionFactory f) {
        super(ResourceEdge.class, f);
    }

    ResourceEdge create(Resource from, Resource to, int distance, ResourceRelation relation) {
        ResourceEdge res = new ResourceEdge(from, to, distance, relation);

        save(res);
        return res;
    }

    Map findDescendantMap(Resource r) {
        String sql = "select e.to, e.distance from ResourceEdge e " + "where e.from = :from "
                     + "and distance > :distance";

        List vals = getSession().createQuery(sql).setParameter("from", r).setInteger("distance", 0)
            .list();
        return convertDistanceListToMap(vals);
    }

    Map convertDistanceListToMap(List vals) {
        Map res = new HashMap();

        for (Iterator i = vals.iterator(); i.hasNext();) {
            Object[] ent = (Object[]) i.next();
            Resource r = (Resource) ent[0];
            Integer distance = (Integer) ent[1];
            Collection c = (Collection) res.get(distance);

            if (c == null) {
                c = new ArrayList();
                res.put(distance, c);
            }
            c.add(r);
        }
        return res;
    }

    /**
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @SuppressWarnings("unchecked")
    Collection<ResourceEdge> findChildEdges(List<Resource> resources, ResourceRelation rel) {
        String sql = "from ResourceEdge e " +
                     "where e.from in (:resources) " +
                     "and distance = 1 " +
                     "and rel_id = :rel_id ";
        final List<ResourceEdge> rtn = new ArrayList<ResourceEdge>(resources.size());
        final Query query = getSession().createQuery(sql);
        int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, size);
            final List<ResourceEdge> edges =
                query.setParameterList("resources", resources.subList(i, end))
                     .setInteger("rel_id", rel.getId().intValue())
                     .list();
            rtn.addAll(edges);
        }
        return rtn;
    }

    /**
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @SuppressWarnings("unchecked")
    Collection<ResourceEdge> findDescendantEdges(List<Resource> resources, ResourceRelation rel) {
        String sql = "from ResourceEdge e " + "where e.from in (:resources) "
                     + "and distance > :distance " + "and rel_id = :rel_id ";
        List<ResourceEdge> rtn = new ArrayList<ResourceEdge>();
        Query query = getSession().createQuery(sql);
        int size = resources.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, size);
            rtn.addAll(query.setParameterList("resources", resources.subList(i, end)).setInteger(
                "distance", 0).setInteger("rel_id", rel.getId().intValue()).list());
        }
        return rtn;
    }

    /**
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    Collection<ResourceEdge> findDescendantEdges(Resource r, ResourceRelation rel) {
        return findDescendantEdges(Collections.singletonList(r), rel);
    }

    List<ResourceEdge> findDescendantEdgesByNetworkRelation(Integer resourceId,
                                                            List<Integer> platformTypeIds,
                                                            String platformName) {
        String nameEx = null;
        String sql = "select {e.*} from EAM_RESOURCE_EDGE e " +
                     " join EAM_RESOURCE r on e.to_id = r.id " +
                     " join EAM_PLATFORM p on p.resource_id = r.id " + " where e.rel_id = " +
                     AuthzConstants.RELATION_NETWORK_ID + " and e.distance != 0 ";

        if (resourceId != null) {
            sql += " and r.id = :rid ";
        }
        if (platformTypeIds != null && !platformTypeIds.isEmpty()) {
            sql += " and p.platform_type_id in (:ptids) ";
        }

        if (platformName != null && platformName.trim().length() > 0) {
            HQDialect dialect = getHQDialect();
            nameEx = dialect.getRegExSQL("r.sort_name", ":regex", true, false);

            sql += " and (" + nameEx + ") ";
        }
        sql += " order by r.sort_name ";

        Query query = getSession().createSQLQuery(sql).addEntity("e", ResourceEdge.class);

        if (resourceId != null) {
            query.setInteger("rid", resourceId.intValue());
        }
        if (platformTypeIds != null && !platformTypeIds.isEmpty()) {
            query.setParameterList("ptids", platformTypeIds, new IntegerType());
        }

        if (nameEx != null) {
            query.setString("regex", platformName);
        }

        return query.list();
    }

    @SuppressWarnings("unchecked")
    Collection<ResourceEdge> findAncestorEdges(Resource r, ResourceRelation rel) {
        String sql = "from ResourceEdge e where e.from = :from and distance < :distance and rel_id = :rel_id ";
        return getSession().createQuery(sql).setParameter("from", r).setInteger("distance", 0)
            .setInteger("rel_id", rel.getId().intValue()).list();
    }

    void deleteEdges(Resource r) {
        String sql = "from ResourceEdge where to_id = :to or from_id = :from)";
        @SuppressWarnings("unchecked")
        List<ResourceEdge> edges = getSession().createQuery(sql).setParameter("to", r).setParameter("from", r).list();
        for (ResourceEdge edge : edges) {
            remove(edge);
        }
    }

    void deleteEdges(Resource r, ResourceRelation rel) {
        String sql = "from ResourceEdge where (to_id = :to or from_id = :from) and rel_id = :rel_id ";
        @SuppressWarnings("unchecked")
        List<ResourceEdge> edges = getSession().createQuery(sql)
            .setParameter("to", r)
            .setParameter("from", r)
            .setInteger("rel_id", rel.getId())
            .list();
        for (ResourceEdge edge : edges) {
            remove(edge);
        }
    }

    void deleteEdge(Resource parent, Resource child, ResourceRelation rel) {
        String sql = "from ResourceEdge where from_id = :from and to_id = :to and rel_id = :rel_id ";
        @SuppressWarnings("unchecked")
        List<ResourceEdge> edges = getSession().createQuery(sql)
            .setParameter("from", parent)
            .setParameter("to", child)
            .setInteger("rel_id", rel.getId())
            .list();
        for (ResourceEdge edge : edges) {
            remove(edge);
        }
    }

    @SuppressWarnings("unchecked")
	Collection<ResourceEdge> getDescendantEdgesOfType(Collection<Resource> resources, Resource prototype,
	                                                  ResourceRelation relation, int distance) {
        String sign = (distance < 0) ? "<=" : ">=";
        distance = Math.abs(distance);
        final StringBuilder hqlbuf = new StringBuilder(64)
            .append("FROM ResourceEdge re ")
            .append("WHERE distance ").append(sign).append(" :dist and relation=:relation ")
            .append("AND re.from in (:from)");
        if (prototype != null) {
            hqlbuf.append(" AND re.to.prototype = :type");
        }
        final String hql = hqlbuf.toString();
        final Query query = getSession()
            .createQuery(hql)
            .setParameterList("from", resources)
            .setParameter("relation", relation)
            .setParameter("dist", distance);
        if (prototype != null) {
            query.setParameter("type", prototype);
        }
        return query.list();
    }

    // Retrieve the parent of a given resource, if one exists, otherwise return
    // null
    ResourceEdge getParentEdge(Resource resource, ResourceRelation relation) {
        String hql = "from ResourceEdge re where re.from=:from and distance=-1 and relation=:relation";
        Iterator iterator = getSession()
            .createQuery(hql)
            .setParameter("from", resource)
            .setParameter("relation", relation)
            .iterate();
        if (iterator.hasNext()) {
            return (ResourceEdge) iterator.next();
        }
        return null;
    }

    // Retrieve the children of a given resource, base on the relation
    List<ResourceEdge> findChildEdges(Resource resource, ResourceRelation relation) {
        String hql = "from ResourceEdge re "
                     + "where re.from=:from and distance=1 and relation=:relation";

        return getSession().createQuery(hql).setParameter("from", resource).setParameter(
            "relation", relation).list();
    }

    List<ResourceEdge> findByName(String name, ResourceRelation relation) {
        return getSession().createCriteria(ResourceEdge.class).createAlias("from", "f").add(
            Restrictions.ilike("f.name", name, MatchMode.ANYWHERE)).add(
            Restrictions.eq("relation", relation)).add(Restrictions.eq("distance", new Integer(0)))
            .list();
    }

    // Returns the number of descendants of a given resource
    int getDescendantCount(Resource resource, ResourceRelation relation) {
        String hql = "select count(re) from ResourceEdge re "
                     + "where re.from=:from and distance > 0 and relation=:relation";

        return ((Number) getSession().createQuery(hql).setParameter("from", resource)
            .setParameter("relation", relation).iterate().next()).intValue();
    }

    // Checks whether or not a given resource has a relation in the edge table
    boolean hasResourceRelation(Resource resource, ResourceRelation relation) {
        return checkEdgeExistence(resource, relation, 0);
    }

    // Checks whether or not a given resource has any direct children
    boolean hasChildren(Resource parent, ResourceRelation relation) {
        return checkEdgeExistence(parent, relation, 1);
    }

    // Checks whether or not resource(s) in the edge table exist based on
    // relation and distance from the given resource
    private boolean checkEdgeExistence(Resource resource, ResourceRelation relation, int distance) {
        String hql = "select count(re) from ResourceEdge re "
                     + "where re.from=:from and distance=:distance and relation=:relation";

        int result = ((Number) getSession().createQuery(hql).setParameter("from", resource)
            .setParameter("relation", relation).setInteger("distance", distance).iterate().next())
            .intValue();

        return result > 0;
    }

    boolean isResourceChildOf(Resource parent, Resource child) {
        String sql = "from ResourceEdge re "
                     + "where re.from=:from and re.to=:to and distance=:distance and rel_id=:rel_id";
        // ...in most cases we want to check for a direct parent child
        // relationship
        int distance = 1;

        if (parent.getResourceType().getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM &&
            child.getResourceType().getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            // ...in the case where the parent is a platform and the child is a
            // service, we need to adjust the distance
            // to get the appropriate answer...
            distance = 2;
        }

        List results = getSession().createQuery(sql).setParameter("from", parent).setParameter(
            "to", child).setInteger("distance", distance).setInteger("rel_id",
            AuthzConstants.RELATION_CONTAINMENT_ID.intValue()).list();

        return results.size() == 1;
    }
}

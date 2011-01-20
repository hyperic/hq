/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.events.server.session;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AlertDefinitionDAO implements AlertDefinitionDAOInterface {

    private PermissionManager permissionManager;
    private ActionDAO actDAO;
    private TriggerDAO tDAO;
    private AlertConditionDAO alertConditionDAO;
    private ResourceDao resourceDao;
    private ResourceTypeDao resourceTypeDao;
    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public AlertDefinitionDAO(PermissionManager permissionManager,
                              ActionDAO actDAO, TriggerDAO tDAO,
                              AlertConditionDAO alertConditionDAO,
                              ResourceDao resourceDao, ResourceTypeDao resourceTypeDao) {
        this.permissionManager = permissionManager;
        this.actDAO = actDAO;
        this.tDAO = tDAO;
        this.alertConditionDAO = alertConditionDAO;
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
    }

  
    public void remove(ResourceAlertDefinition def) {
        entityManager.remove(def.getAlertDefinitionState());
        entityManager.remove(def);
    }
    
    public void remove(ResourceTypeAlertDefinition def) {
        entityManager.remove(def);
    }

    public List<ResourceAlertDefinition> findAllByResource(Resource r) {
        return entityManager.createQuery("select def from ResourceAlertDefinition where def.resource=:resource",
            ResourceAlertDefinition.class).setParameter("resource", r).getResultList();
    }
    
    public List<ResourceTypeAlertDefinition> findAllByResourceType(ResourceType type) {
        return entityManager.createQuery("select def from ResourceTypeAlertDefinition def where def.resourceType=:type",
            ResourceTypeAlertDefinition.class).setParameter("type", type).getResultList();
    }

    /**
    * Prefetches all collections associated with each alertDef that is deleted and has a
    * null resourceId into ehcache.
    * @return {@link List} of {@link Integer} of {@link AlertDefintion} ids
    */
    @SuppressWarnings("unchecked")
    public List<Integer> findAndPrefetchAllDeletedAlertDefs() {
        //TODO impl?
        return null;
        // need to pre-fetch one bag at a time due to bug
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2980
//        String hql = new StringBuilder()
//            .append("from AlertDefinition def ")
//            .append("left outer join fetch def.childrenBag cb ")
//            .append("where def.resource is null and def.deleted = '1'")
//            .toString();
//        getSession().createQuery(hql).list();
//        hql = new StringBuilder()
//            .append("from AlertDefinition def ")
//            .append("left outer join fetch def.actionsBag ab ")
//            .append("where def.resource is null and def.deleted = '1'")
//            .toString();
//        getSession().createQuery(hql).list();
//        hql = new StringBuilder()
//            .append("from AlertDefinition def ")
//            .append("left outer join fetch def.conditionsBag condb ")
//            .append("where def.resource is null and def.deleted = '1'")
//            .toString();
//        getSession().createQuery(hql).list();
//        hql = new StringBuilder()
//            .append("from AlertDefinition def ")
//            .append("left outer join fetch def.triggersBag tb ")
//            .append("where def.resource is null and def.deleted = '1'")
//            .toString();
//        getSession().createQuery(hql).list();
//        hql = new StringBuilder()
//            .append("select def.id from AlertDefinition def ")
//            .append("where def.resource is null and def.deleted = '1'")
//            .toString();
//        return getSession().createQuery(hql).list();
    }

    /**
     * Find the alert def for a given appdef entity and is child of the parent
     * alert def passed in
     * @param ent Entity to find alert defs for
     * @param parentId ID of the parent
     */
    public ResourceAlertDefinition findChildAlertDef(Resource res, Integer parentId) {
        String sql = " select a FROM ResourceAlertDefinition a WHERE "
                     + "a.resource = :res AND a.deleted = false AND a.resourceTypeAlertDefinition.id = :parent";

        return entityManager.createQuery(sql,ResourceAlertDefinition.class).setParameter("res", res)
            .setParameter("parent", parentId.intValue()).getSingleResult();

    }
    
    /**
     * Get a list of all alert definitions with an availability metric condition
     * @return a list of alert definitions
     */
    public List<AlertDefinition> findAvailAlertDefs() {
        //TODO impl?
        return null;
    	// To improve performance, need to explicitly fetch the resource,
    	// resource type, and conditions so that they are not lazy loaded
//        String hql = new StringBuilder(256)
//            .append("from AlertDefinition ad ")
//            .append("join fetch ad.resource rez ")
//            .append("join fetch rez.resourceType ")
//            .append("join fetch ad.conditionsBag c ")
//            .append("where ad.active = true ")
//            .append("and ad.deleted = false ")
//            .append("and upper(c.name) = '")
//            .append(MeasurementConstants.CAT_AVAILABILITY.toUpperCase())
//            .append("' ")
//            .append("and (ad.parent is null or ad.parent.id != 0) ")
//            .toString();
//        
//        return getSession().createQuery(hql).list();
    }

    
    @SuppressWarnings("unchecked")
    private List<ResourceAlertDefinition> findByResource(Resource res, String sort, boolean asc) {
        String sql = "select a from ResourceAlertDefinition a where a.resource = :res and " +
                     "a.deleted = false order by a." + sort + (asc ? " ASC" : " DESC");

        return entityManager.createQuery(sql).setParameter("res", res).getResultList();
    }

    public List<ResourceAlertDefinition> findByResource(Resource res) {
        return findByResource(res, true);
    }

    public List<ResourceAlertDefinition> findByResource(Resource res, boolean asc) {
        return findByResource(res, "name", asc);
    }

    public List<ResourceAlertDefinition> findByResourceSortByCtime(Resource res, boolean asc) {
        return findByResource(res, "ctime", asc);
    }

    /**
     * Return all alert definitions for the given resource and its descendants
     * @param res the root resource
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<AlertDefinition> findByRootResource(AuthzSubject subject, Resource r) {
        //TODO impl?
        return null;
//        EdgePermCheck wherePermCheck = permissionManager.makePermCheckHql("rez", true);
//        String hql = "select ad from AlertDefinition ad join ad.resource rez " + wherePermCheck +
//                     " and ad.deleted = false and rez.resourceType is not null ";
//
//        Query q = createQuery(hql);
//
//        return wherePermCheck
//            .addQueryParameters(q, subject, r, 0, Arrays.asList(AuthzConstants.VIEW_ALERTS_OPS)).list();
    }

    public void save(ResourceAlertDefinition def) {
        entityManager.persist(def);
        if (def.getAlertDefinitionState() == null) {
            AlertDefinitionState state = new AlertDefinitionState(def);
            entityManager.persist(state);
            def.setAlertDefinitionState(state);
            entityManager.merge(def);
        }
    }
    
    public void save(ResourceTypeAlertDefinition def) {
        entityManager.persist(def);
    }
    
    public void update(AlertDefinition def) {
        entityManager.merge(def);
    }
    
    int deleteByAlertDefinition(AlertDefinition def) {
        //TODO impl
        return 0;
//        String sql = "update AlertDefinition "
//                     + "set escalation = null, deleted = true, parent = null, "
//                     + "active = false, enabled = false where parent = :def";
//        
//        int ret = getSession().createQuery(sql).setParameter("def", def).executeUpdate();
//        //TODO children
//        //def.getChildrenBag().clear();
//      
//        return ret;
    }
    
    public void setAlertDefinitionValue(AlertDefinition def, AlertDefinitionValue val) {
        if(def instanceof ResourceTypeAlertDefinition) {
            ((ResourceTypeAlertDefinition)def).setResourceType(resourceTypeDao.findById(val.getAppdefId()));
        }else {
            ((ResourceAlertDefinition)def).setResource(resourceDao.findById(val.getAppdefId()));
        }
        setValue(def, val);
    }

   private void setValue(AlertDefinition def, AlertDefinitionValue val) {

         setAlertDefinitionValueNoRels(def, val);

        // def.set the resource based on the entity ID

        for (RegisteredTriggerValue tVal : val.getAddedTriggers()) {
            //TODO better way
            ((ResourceAlertDefinition)def).addTrigger(tDAO.findById(tVal.getId()));
        }

        for (RegisteredTriggerValue tVal : val.getRemovedTriggers()) {
            ((ResourceAlertDefinition)def).removeTrigger(tDAO.findById(tVal.getId()));
        }

        for (AlertConditionValue cVal : val.getAddedConditions()) {
            def.addCondition(alertConditionDAO.findById(cVal.getId()));
        }

        for (AlertConditionValue cVal : val.getRemovedConditions()) {
            def.removeCondition(alertConditionDAO.findById(cVal.getId()));
        }

        for (ActionValue aVal : val.getAddedActions()) {
            def.addAction(actDAO.findById(aVal.getId()));
        }

        for (ActionValue aVal : val.getRemovedActions()) {
            def.removeAction(actDAO.findById(aVal.getId()));
        }
    }

    /**
     * duplicates all the values obtained from master into clone. the active and
     * enabled fields are taken from master.getActive()
     * @param clone {@link AlertDefinition} set all of clone's values obtained
     *        from master.
     * @param master {@link AlertDefinitionValue} object to retrieve values from
     *        in order to update clone. Object does not change.
     */
    void setAlertDefinitionValueNoRels(final AlertDefinition clone,
                                       final AlertDefinitionValue master) {

        clone.setName(master.getName());
        clone.setDescription(master.getDescription());

        // from bug http://jira.hyperic.com/browse/HQ-1636
        // setActiveStatus() should be governed by active NOT enabled field
        clone.setActiveStatus(master.getActive());

        clone.setWillRecover(master.getWillRecover());
        clone.setNotifyFiltered(master.getNotifyFiltered());
        clone.setControlFiltered(master.getControlFiltered());
        clone.setPriority(master.getPriority());

        clone.setFrequencyType(master.getFrequencyType());
        clone.setCount(new Long(master.getCount()));
        clone.setRange(new Long(master.getRange()));
        clone.setDeleted(master.getDeleted());
    }

    @SuppressWarnings("unchecked")
    List<AlertDefinition> findDefinitions(AuthzSubject subj, AlertSeverity minSeverity,
                                          Boolean enabled, boolean excludeTypeBased, PageInfo pInfo) {
        //TODO impl?
        return null;
//        String sql = PermissionManagerFactory.getInstance().getAlertDefsHQL();
//
//        sql += " and d.deleted = false and d.resource is not null ";
//        if (enabled != null) {
//            sql += " and d.enabled = " + (enabled.booleanValue() ? "true" : "false");
//        }
//
//        sql += " and (d.parent is null";
//        if (excludeTypeBased) {
//            sql += ") ";
//        } else {
//            sql += " or not d.parent.id = 0) ";
//        }
//
//        sql += getOrderByClause(pInfo);
//
//        Query q = getSession().createQuery(sql).setInteger("priority", minSeverity.getCode());
//
//        if (sql.indexOf("subj") > 0) {
//            q.setInteger("subj", subj.getId().intValue())
//                .setParameterList("ops", AuthzConstants.VIEW_ALERTS_OPS);
//        }
//
//        return pInfo.pageResults(q).list();
    }

    private String getOrderByClause(PageInfo pInfo) {
        AlertDefSortField sort = (AlertDefSortField) pInfo.getSort();
        String res = " order by " + sort.getSortString("d", "r") +
                     (pInfo.isAscending() ? "" : " DESC");

        if (!sort.equals(AlertDefSortField.CTIME)) {
            res += ", " + AlertDefSortField.CTIME.getSortString("d", "r") + " DESC";
        }
        return res;
    }

    List<ResourceTypeAlertDefinition> findTypeBased(Boolean enabled, PageInfo pInfo) {
        //TODO this did check d.deleted before
        String sql = "select d from ResourceTypeAlertDefinition d";

        if (enabled != null) {
            sql += " where d.enabled = " + (enabled.booleanValue() ? "true" : "false");
        }
        sql += getOrderByClause(pInfo);

        //TODO return pInfo.pageResults?
        return entityManager.createQuery(sql,ResourceTypeAlertDefinition.class).getResultList();

    }

    List<ResourceAlertDefinition> getUsing(Escalation e) {
        String sql = "select d from ResourceAlertDefinition where d.escalation=:escalation";
        return entityManager.createQuery(sql,ResourceAlertDefinition.class).setParameter("escalation", e).getResultList();
    }

    boolean isResourceDefEnabled(Integer id) {
        return entityManager.createQuery(
            "select d.enabled from ResourceAlertDefinition d" + " where d.id = " + id,Boolean.class).getSingleResult();
    }

    int setChildrenActive(ResourceTypeAlertDefinition def, boolean active) {
        return entityManager.createQuery(
            "update ResourceAlertDefinition def set def.active = :active, " + "def.enabled = :active, def.mtime = :mtime "
                + "where def.resourceTypeAlertDefinition = :def").setParameter("active", active).setParameter("mtime",
            System.currentTimeMillis()).setParameter("def", def).executeUpdate();
    }

    int getNumActiveResourceAlertDefs() {
        String hql = "select count(*) from ResourceAlertDefinition d where d.active = true and d.deleted = false";
        return entityManager.createQuery(hql,Integer.class).setHint("org.hibernate.cacheable", true).
        setHint("org.hibernate.cacheRegion", "AlertDefinition.getNumActiveDefs")
            .getSingleResult();
    }

    int setChildrenEscalation(ResourceTypeAlertDefinition def, Escalation esc) {
        return entityManager.createQuery(
            "update ResourceAlertDefinition set escalation = :esc, " + "mtime = :mtime where resourceTypeAlertDefinition = :def")
            .setParameter("esc", esc).setParameter("mtime", System.currentTimeMillis()).setParameter(
                "def", def).executeUpdate();
    }

    public ResourceAlertDefinition findResourceAlertDefById(Integer id) {
        return entityManager.find(ResourceAlertDefinition.class, id);
    }
    
    public ResourceTypeAlertDefinition findResourceTypeAlertDefById(Integer id) {
        return entityManager.find(ResourceTypeAlertDefinition.class, id);
    }
    
    //TODO remove this method. Code should always distinguish b/w res alerts and type alerts
    public AlertDefinition findById(Integer id) {
        //TODO check history of all findById calls - some were get and some where findById, which
        //had diff behavior in JPA
        ResourceAlertDefinition resourceDef = findResourceAlertDefById(id);
        if(resourceDef != null) {
            return resourceDef;
        }
        return findResourceTypeAlertDefById(id);
    }
    
    public List<ResourceAlertDefinition> findAllResourceAlertDefs() {
        return entityManager.createQuery("select d from ResourceAlertDefinition d",ResourceAlertDefinition.class).getResultList();
    }
    
    public List<ResourceTypeAlertDefinition> findAllResourceTypeAlertDefs() {
        return entityManager.createQuery("select d from ResourceTypeAlertDefinition d",ResourceTypeAlertDefinition.class).getResultList();
    }
    

}

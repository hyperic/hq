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
package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class AlertDefinitionDAO extends HibernateDAO {
    private static final String[] MANAGE_ALERTS_OPS = new String[] { 
        AuthzConstants.platformOpManageAlerts,
        AuthzConstants.serverOpManageAlerts,
        AuthzConstants.serviceOpManageAlerts 
    };

    public AlertDefinitionDAO(DAOFactory f) {
        super(AlertDefinition.class, f);
    }

    void remove(AlertDefinition def) {
        super.remove(def);
    }

    public List findByAppdefEntity(int type, int id) {
        return findByAppdefEntity(type, id, "d.name");
    }

    public List findByAppdefEntitySortByCtime(int type, int id) {
        return findByAppdefEntity(type, id, "d.ctime");
    }

    private List findByAppdefEntity(int type, int id, String orderBy) {
        String sql = "from AlertDefinition d " + 
            "WHERE d.appdefType = ? AND d.appdefId = ? " +
            "AND d.deleted = false " + 
            "AND (NOT d.parent.id = 0 OR d.parent IS NULL) "+
            "ORDER BY " + orderBy;
        
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }
    
    public List findAllByEntity(AppdefEntityID aeid) {
        String sql = "from AlertDefinition d " + 
            "WHERE d.appdefType = ? AND d.appdefId = ? " +
            "AND (NOT d.parent.id = 0 OR d.parent IS NULL) ";
        
        return getSession().createQuery(sql)
            .setInteger(0, aeid.getType())
            .setInteger(1, aeid.getID())
            .list();
    }
    
    /**
     * Find the alert def for a given appdef entity and is child of the parent
     * alert def passed in
     * @param ent      Entity to find alert defs for
     * @param parentId ID of the parent
     */
    public AlertDefinition findChildAlertDef(AppdefEntityID ent,
                                             Integer parentId){
        String sql = "FROM AlertDefinition a WHERE " + 
            "a.appdefType = :appdefType AND a.appdefId = :appdefId " +
            "AND a.deleted = false AND a.parent = :parent";
        
        List defs = getSession().createQuery(sql)
            .setInteger("appdefType", ent.getType())
            .setInteger("appdefId", ent.getID())
            .setInteger("parent", parentId.intValue())
            .list();
        
        if (defs.size() == 0) {
            return null;
        }

        return (AlertDefinition) defs.get(0);
    }
    
    /**
     * Find the alert def for a given appdef entity that is the child of the 
     * parent alert def passed in, allowing for the query to return a stale copy 
     * of the alert definition (for efficiency reasons).
     * 
     * @param ent
     * @param parentId
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The alert definition or <code>null</code>.
     */
    public AlertDefinition findChildAlertDef(AppdefEntityID ent, 
                                             Integer parentId, 
                                             boolean allowStale) {
        Session session = this.getSession();
        FlushMode oldFlushMode = session.getFlushMode();
        
        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);                
            }
            
            return findChildAlertDef(ent, parentId);
        } finally {
            session.setFlushMode(oldFlushMode);
        } 
    }

    public AlertDefinition findById(Integer id) {
        return (AlertDefinition)super.findById(id);
    }
    
    /** 
     * Find an alert definition by Id, loading from the session.
     * 
     * @param id The alert definition Id.
     * @param refresh <code>true</code> to force the alert def state to be 
     *                to be re-read from the database; <code>false</code> to 
     *                allow the persistence engine to return a cached copy.
     * @return The alert definition.               
     * @throws ObjectNotFoundException if no alert definition with the give Id exists.
     */
    public AlertDefinition findById(Integer id, boolean refresh) {
        AlertDefinition def = findById(id);
        
        if (refresh) {
            getSession().refresh(def);
        }
        
        return def;
    }
    
    /** 
     * Find an alert definition by Id, loading from the session.
     * 
     * @param id The alert definition Id.
     * @return The alert definition or <code>null</code> if no alert definition 
     *         exists with the given Id.
     */
    public AlertDefinition get(Integer id) {
        return (AlertDefinition)super.get(id);
    }
    
    public List findByAppdefEntityTypeSortByCtime(AppdefEntityID id,
                                                  boolean asc) {
        return findByAppdefEntityType(id, "ctime", asc);
    }
    
    public List findByAppdefEntityType(AppdefEntityID id, boolean asc) {
        return findByAppdefEntityType(id, "name", asc);
    }
        
    private List findByAppdefEntityType(AppdefEntityID id, String sort,
                                        boolean asc) {
        String sql = "from AlertDefinition a where a.appdefType = :aType " +
            "and a.appdefId = :aId and a.deleted = false and " +
            "a.parent.id = 0 order by a." + sort + (asc ? " ASC" : " DESC");
        
        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getID())
            .list();
    }
    
    void save(AlertDefinition alert) {
        super.save(alert);
    }
    
    int deleteByEntity(AppdefEntityID ent) {
        String sql = "DELETE AlertDefinition a WHERE " + 
            "a.appdefType = :appdefType AND a.appdefId = :appdefId";

        return getSession().createQuery(sql)
            .setInteger("appdefType", ent.getType())
            .setInteger("appdefId", ent.getID())
            .executeUpdate();
    }

    void setAlertDefinitionValue(AlertDefinition def, AlertDefinitionValue val)
    {
        AlertConditionDAO cDAO =
            DAOFactory.getDAOFactory().getAlertConditionDAO();
        ActionDAO actDAO = DAOFactory.getDAOFactory().getActionDAO();
        TriggerDAO tDAO = DAOFactory.getDAOFactory().getTriggerDAO();
        
        setAlertDefinitionValueNoRels(def, val);
    
        for (Iterator i=val.getAddedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue)i.next();
            RegisteredTrigger t = tDAO.findById(tVal.getId());
            
            def.addTrigger(t);
        }
        
        for (Iterator i=val.getRemovedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue)i.next();
            RegisteredTrigger t = tDAO.findById(tVal.getId());
            
            def.removeTrigger(t);
        }
        
        for (Iterator i=val.getAddedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            AlertCondition c = cDAO.findById(cVal.getId());
            
            def.addCondition(c);
        }
    
        for (Iterator i=val.getRemovedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            AlertCondition c = cDAO.findById(cVal.getId());
            
            def.removeCondition(c);
        }
    
        for (Iterator i=val.getAddedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            Action a = actDAO.findById(aVal.getId());
            
            def.addAction(a);
        }
    
        for (Iterator i=val.getRemovedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            Action a = actDAO.findById(aVal.getId());
            
            def.removeAction(a);
        }
    }

    void setAlertDefinitionValueNoRels(AlertDefinition def,
                                       AlertDefinitionValue val) {
        AlertDefinitionDAO aDAO = DAOFactory.getDAOFactory().getAlertDefDAO();
        TriggerDAO tDAO = DAOFactory.getDAOFactory().getTriggerDAO();
        
        def.setName(val.getName());
        def.setCtime(val.getCtime());
        def.setMtime(val.getMtime());
        if (val.parentIdHasBeenSet() && val.getParentId() != null) {
            def.setParent(aDAO.findById(val.getParentId()));
        }
        def.setDescription(val.getDescription());
        def.setEnabled(val.getEnabled());
        def.setWillRecover(val.getWillRecover());
        def.setNotifyFiltered(val.getNotifyFiltered() );
        def.setControlFiltered(val.getControlFiltered() );
        def.setPriority(val.getPriority());
        
        // def.set the resource based on the entity ID
        def.setAppdefId(val.getAppdefId());
        def.setAppdefType(val.getAppdefType());
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(val.getParentId())) {
            AppdefEntityID aeid = def.getAppdefEntityId();
            ResourceDAO rDao = new ResourceDAO(DAOFactory.getDAOFactory());
            
            // Don't need to synch the Resource with the db since changes 
            // to the Resource aren't cascaded on saving the AlertDefinition.
            def.setResource(rDao.findByInstanceId(aeid.getAuthzTypeId(),
                                                  aeid.getId(), 
                                                  true));
        }

        def.setFrequencyType(val.getFrequencyType());
        def.setCount(new Long(val.getCount()));
        def.setRange(new Long(val.getRange()));
        def.setDeleted(val.getDeleted());
        if (val.actOnTriggerIdHasBeenSet()) {
            def.setActOnTrigger(tDAO.findById(new Integer(val.getActOnTriggerId())));
        }
    }
    
    List findDefinitions(AuthzSubjectValue subj, AlertSeverity minSeverity, 
                         Boolean enabled, boolean excludeTypeBased, 
                         PageInfo pInfo)
    {
        String sql = PermissionManagerFactory.getInstance().getAlertDefsHQL();
        
        sql += " and d.deleted = false";
        if (enabled != null) {
            sql += " and d.enabled = " + 
                   (enabled.booleanValue() ? "true" : "false");
        }
        
        if (excludeTypeBased) {
            sql += " and d.parent is null";
        }

        sql += getOrderByClause(pInfo);
               
        Query q = getSession().createQuery(sql)
            .setInteger("priority", minSeverity.getCode());

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.getId().intValue())
             .setParameterList("ops", MANAGE_ALERTS_OPS);
        }
        
        return pInfo.pageResults(q).list();
    }

    private String getOrderByClause(PageInfo pInfo) {
        AlertDefSortField sort = (AlertDefSortField)pInfo.getSort();
        String res = " order by " + sort.getSortString("d", "r") + 
            (pInfo.isAscending() ? "" : " DESC");
        
        if (!sort.equals(AlertDefSortField.CTIME)) {
            res += ", " + AlertDefSortField.CTIME.getSortString("d", "r") + 
                   " DESC";
        }
        return res;
    }
    
    List findTypeBased(Boolean enabled, PageInfo pInfo) {
        String sql = "from AlertDefinition d " + 
            "where d.deleted = false and d.parent.id = 0 ";
        
        if (enabled != null) {
            sql += " and d.enabled = " + 
                (enabled.booleanValue() ? "true" : "false");
        }
        sql += getOrderByClause(pInfo);
                   
        Query q = getSession().createQuery(sql);
        
        return pInfo.pageResults(q).list();
    }

}

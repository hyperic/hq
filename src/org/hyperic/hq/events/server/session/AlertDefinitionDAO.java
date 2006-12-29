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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;

public class AlertDefinitionDAO extends HibernateDAO {
    public AlertDefinitionDAO(DAOFactory f) {
        super(AlertDefinition.class, f);
    }

    void remove(AlertDefinition def) {
        super.remove(def);
    }

    void clearConditions(AlertDefinition def) {
        Collection conds = def.getConditionsBag();
        for (Iterator it = conds.iterator(); it.hasNext(); ) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setAlertDefinition(null);
            cond.setTrigger(null);
        }
        conds.clear();
    }

    void clearActions(AlertDefinition def) {
        Collection acts = def.getActionsBag();
        for (Iterator it = acts.iterator(); it.hasNext(); ) {
            Action act = (Action) it.next();
            act.setAlertDefinition(null);
        }
        acts.clear();
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
            "AND (NOT d.parent = 0 OR d.parent IS NULL) "+
            "ORDER BY " + orderBy;
        
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }
    
    /**
     * Find all alert defs for a given appdef entity.  All the alert defs
     * must be a child of the passed def
     * @param ent      Entity to find alert defs for
     * @param parentId ID of the parent
     */
    public List findChildAlertDefs(AppdefEntityID ent, Integer parentId){
        String sql = "FROM AlertDefinition a WHERE " + 
            "a.appdefType = :appdefType AND a.appdefId = :appdefId " +
            "AND a.deleted = false AND a.parent = :parent";
        
        return getSession().createQuery(sql)
            .setInteger("appdefType", ent.getType())
            .setInteger("appdefId", ent.getID())
            .setInteger("parent", parentId.intValue())
            .list();
    }

    /**
     * Find an alert definition where the ActOnTrigger is the passed value
     * @param trigger Act On Trigger used by the alert def
     */
    public AlertDefinition getFromTrigger(RegisteredTrigger trigger) {
        String sql = "FROM AlertDefinition a WHERE " +
            "a.actOnTrigger = :trigger AND enabled = true AND deleted = false";

        return (AlertDefinition)getSession().createQuery(sql)
            .setParameter("trigger", trigger)
            .uniqueResult();
    }
    
    public List findChildAlertDefinitions(AlertDefinition def) {
        String sql = "from AlertDefinition d where d.parent = :parent " + 
            "AND d.deleted = false";

        return getSession().createQuery(sql)
            .setParameter("parent", def)
            .list();
    }

    public AlertDefinition findById(Integer id) {
        return (AlertDefinition)super.findById(id);
    }
    
    public List findByAppdefEntityType(AppdefEntityID id) {
        String sql = "from AlertDefinition a where a.appdefType = :aType " +
            "and a.appdefId = :aId and a.deleted = false and " +
            "a.parent = 0 order by a.name";
        
        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getID())
            .list();
    }
    
    void save(AlertDefinition alert) {
        super.save(alert);
    }
}

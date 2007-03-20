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

    public AlertDefinition findById(Integer id) {
        return (AlertDefinition)super.findById(id);
    }
    
    public List findByAppdefEntityType(AppdefEntityID id) {
        String sql = "from AlertDefinition a where a.appdefType = :aType " +
            "and a.appdefId = :aId and a.deleted = false and " +
            "a.parent.id = 0 order by a.name";
        
        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getID())
            .list();
    }
    
    void save(AlertDefinition alert) {
        super.save(alert);
    }
}

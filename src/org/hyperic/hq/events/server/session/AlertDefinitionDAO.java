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

import org.hibernate.Session;
import org.hyperic.hibernate.dao.HibernateDAO;
import org.hyperic.hq.events.shared.AlertDefinitionPK;

public class AlertDefinitionDAO extends HibernateDAO {
    public AlertDefinitionDAO(Session session) {
        super(AlertDefinition.class, session);
    }

    public AlertDefinition findByPrimaryKey(AlertDefinitionPK pk) {
        return findById(pk.getId());
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
            "AND d.deleted = false AND d.parent != 0 AND d.parent IS NOT NULL "+
            "ORDER BY " + orderBy;
        
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .list();
    }
    
    public List findChildAlertDefinition(Integer id) {
        String sql = "from AlertDefinition d where d.parent = ? AND " + 
            "d.deleted = false";

        return getSession().createQuery(sql)
            .setInteger(0, id.intValue())
            .list();
    }
    
    public AlertDefinition findById(Integer id) {
        return (AlertDefinition)super.findById(id);
    }
    
    void save(AlertDefinition alert) {
        super.save(alert);
    }
}

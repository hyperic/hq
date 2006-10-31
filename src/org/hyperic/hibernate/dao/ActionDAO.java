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
import java.util.List;

import org.hibernate.Session;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.shared.ActionPK;

public class ActionDAO extends HibernateDAO {
    public ActionDAO(Session session) {
        super(Action.class, session);
    }

    public Action findByPrimaryKey(ActionPK pk) {
        return findById(pk.getId());
    }

    public Action findById(Integer id) {
        return (Action)super.findById(id);
    }
    
    /**
     * Find all the actions which triggered the alert in the alert log
     * -- Original EJBQL
     * @ejb:finder signature="java.util.Collection findByAlertId(java.lang.Integer aid)"
     * query="SELECT OBJECT(a) FROM Action AS a, Alert AS al, IN (al.actionLogs) AS l
     *        WHERE a.id = l.actionId AND al.id = ?1"
     * @return a collection of {@link Action}s
     */
    public List findByAlertId(int alertId) {
        String sql = "from Action a, AlertActionLog al " + 
            "where a.id = al.action AND al.id = :alertId";
        
        return getSession().createQuery(sql)
            .setInteger("alertId", alertId)
            .list();
    }
    
    // XXX -- This should be eliminated and we should have a bi-di relationsip
    //        on the actual Action object itself.  -- JMT
    public Collection getChildren(Action action) {
        String sql = "from Action a where a.parent = :parent";
        
        return getSession().createQuery(sql)
            .setParameter("parent", action)
            .list();
    }
}

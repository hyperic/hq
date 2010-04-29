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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ActionDAO
    extends HibernateDAO<Action> {

    @Autowired
    public ActionDAO(SessionFactory f) {
        super(Action.class, f);
    }

   
    void removeActions(AlertDefinition def) {
        Map<Action,List<Action>> parentToActions = new HashMap<Action,List<Action>>();
        for (Action action : def.getActions()) {
            List<Action> list;
            if (null == (list = parentToActions.get(action.getParent()))) {
                list = new ArrayList<Action>();
                parentToActions.put(action.getParent(), list);
            }
            list.add(action);
        }
        for (Map.Entry<Action,List<Action>> entry : parentToActions.entrySet() ) {
            Action parent = entry.getKey();
            if (parent == null) {
                continue;
            }
            List<Action> actions = entry.getValue();
            parent.getChildrenBag().removeAll(actions);
        }
        def.clearActions();
    }

    void removeAction(Action action) {
        if (action.getAlertDefinition() != null) {
            action.getAlertDefinition().getActionsBag().remove(action);
        }
        if (action.getParent() != null) {
            action.getParent().getChildrenBag().remove(action);
        }       
        remove(action);
    }

    @SuppressWarnings("unchecked")
    void deleteAlertDefinition(AlertDefinition def) {
        // Find all actions
        List<Action> actions = (List<Action>) createCriteria().add(
            Restrictions.eq("alertDefinition", def)).list();

        // Bulk update all actions
        String sql = "update Action set parent = null, deleted = true where " +
                     (actions.size() > 0 ? "parent in (:acts) or" : "") + " alertDefinition = :def";
        Query q = createQuery(sql).setParameter("def", def);

        if (actions.size() > 0)
            q.setParameterList("acts", actions);

        q.executeUpdate();
    }

    /**
     * Find all the actions which triggered the alert in the alert log
     * @return a collection of {@link Action}s
     */
    @SuppressWarnings("unchecked")
    public List<Action> findByAlert(Alert a) {
        String sql = "select a from Action a, AlertActionLog al "
                     + "where a.id = al.action AND al.alert = :alert";

        return (List<Action>) getSession().createQuery(sql).setParameter("alert", a).list();
    }
}

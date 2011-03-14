package org.hyperic.hq.alert.data;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertDefinition;

public class ActionRepositoryImpl implements ActionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    public void deleteByAlertDefinition(AlertDefinition def) {
        Collection<Action> actions = def.getActions();
        if (!(actions.isEmpty())) {
            String sql = "update Action a set a.parent = null, a.deleted = true where "
                         + "a.parent in (:acts) or a in (:acts)";
            Query q = entityManager.createQuery(sql).setParameter("acts", actions);
            q.executeUpdate();
        }
    }

}

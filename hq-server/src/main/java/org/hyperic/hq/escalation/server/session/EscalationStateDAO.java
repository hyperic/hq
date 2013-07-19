/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.escalation.server.session;

import java.util.Collection;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EscalationStateDAO extends HibernateDAO<EscalationState> {
    @Autowired
    EscalationStateDAO(SessionFactory f) {
        super(EscalationState.class, f);
    }

    /**
     * Find the current escalation state.
     * 
     * @param def The entity performing escalations.
     * @return The current escalation state or <code>null</code> if none exists
     *         for the entity performing escalations.
     */
    EscalationState find(PerformsEscalations def) {
        return (EscalationState) createCriteria().add(
            Expression.eq("alertDefinitionId", def.getId())).add(
            Expression.eq("alertTypeEnum", new Integer(def.getAlertType().getCode())))
            .setCacheable(true).setCacheRegion("EscalationState.findByTypeAndDef").uniqueResult();
    }

    EscalationState find(Escalatable esc) {
        Integer alertId = esc.getAlertInfo().getId();
        Integer alertType = new Integer(esc.getDefinition().getAlertType().getCode());

        return (EscalationState) createCriteria().add(Expression.eq("alertTypeEnum", alertType))
            .add(Expression.eq("alertId", alertId)).uniqueResult();
    }

    Collection<EscalationState> findStatesFor(Escalation mesc) {
        return createCriteria().add(Expression.eq("escalation", mesc)).list();
    }

    List<EscalationState> getActiveEscalations(int maxEscalations) {
        return createCriteria().addOrder(Order.asc("nextActionTime")).setMaxResults(maxEscalations)
            .list();
    }

    void handleSubjectRemoval(AuthzSubject subject) {
        String sql = "update EscalationState set " + "acknowledgedBy = null "
                     + "where acknowledgedBy = :subject";

        getSession().createQuery(sql).setParameter("subject", subject).executeUpdate();
    }

    /**
     * Delete in batch the given escalation states.
     * 
     * @param stateIds The Ids for the escalation states to delete.
     */
    void removeAllEscalationStates(Integer[] stateIds) {
        if (stateIds.length == 0) {
            return;
        }

        getSession().createQuery("delete from EscalationState s where s.id in (:stateIds)")
            .setParameterList("stateIds", stateIds).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    Collection<EscalationState> getOrphanedEscalationStates() {
        final String hql = new StringBuilder()
            .append("from EscalationState e where (alertTypeEnum = :classicType and not exists (")
                .append("select 1 from Alert a where a = e.alertId")
            .append(")) OR (alertTypeEnum = :galertType and not exists (")
                .append("select 1 from GalertLog g where g.id = e.alertId")
            .append("))")
            .toString();
        return createQuery(hql)
            .setInteger("classicType", ClassicEscalationAlertType.CLASSIC.getCode())
            .setInteger("galertType", GalertEscalationAlertType.GALERT.getCode())
            .list();
    }

}

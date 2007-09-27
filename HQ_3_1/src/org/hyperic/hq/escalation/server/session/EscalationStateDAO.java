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
package org.hyperic.hq.escalation.server.session;

import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

class EscalationStateDAO
    extends HibernateDAO
{
    EscalationStateDAO(DAOFactory f) {
        super(EscalationState.class, f);
    }

    EscalationState findById(Integer id) {
        return (EscalationState)super.findById(id);
    }
    
    EscalationState get(Integer id) {
        return (EscalationState)super.get(id);
    }

    void save(EscalationState s) {
        super.save(s);
    }

    void remove(EscalationState s) {
        super.remove(s);
    }

    /**
     * Find the current escalation state.
     * 
     * @param def The entity performing escalations.
     * @return The current escalation state or <code>null</code> if none 
     *          exists for the entity performing escalations.
     */
    EscalationState find(PerformsEscalations def) { 
        return (EscalationState)createCriteria()
            .add(Expression.eq("alertDefinitionId", def.getId()))
            .add(Expression.eq("alertTypeEnum", 
                               new Integer(def.getAlertType().getCode())))
            .setCacheable(true)
            .setCacheRegion("EscalationState.findByTypeAndDef")
            .uniqueResult();
    }
    
    EscalationState find(Escalatable esc) {
        Integer alertId = esc.getAlertInfo().getId();
        Integer alertType = 
            new Integer(esc.getDefinition().getAlertType().getCode());
        
        return (EscalationState)createCriteria()
            .add(Expression.eq("alertTypeEnum", alertType))
            .add(Expression.eq("alertId", alertId))
            .uniqueResult();
    }
    
    Collection findStatesFor(Escalation mesc) {
        return createCriteria()
            .add(Expression.eq("escalation", mesc))
            .list();
    }
    
    List getActiveEscalations(int maxEscalations) {
        return createCriteria()
            .addOrder(Order.asc("nextActionTime"))
            .setMaxResults(maxEscalations)
            .list();
    }

    /**
     * Delete in batch the given escalation states.
     * 
     * @param stateIds The Ids for the escalation states to delete.
     */
    void removeAllEscalationStates(Integer[] stateIds) {
        if (stateIds.length==0) {
            return;
        }
        
        getSession()
         .createQuery("delete from EscalationState s where s.id in (:stateIds)")
         .setParameterList("stateIds", stateIds)
         .executeUpdate();
    }
    
}

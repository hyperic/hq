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

import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

class MEscalationStateDAO
    extends HibernateDAO
{
    MEscalationStateDAO(DAOFactory f) {
        super(MEscalationState.class, f);
    }

    MEscalationState findById(Integer id) {
        return (MEscalationState)super.findById(id);
    }

    void save(MEscalationState s) {
        super.save(s);
    }

    void remove(MEscalationState s) {
        super.remove(s);
    }

    MEscalationState find(PerformsEscalations def) { 
        return (MEscalationState)createCriteria()
            .add(Expression.eq("alertDefinitionId", def.getId()))
            .add(Expression.eq("alertTypeEnum", 
                               new Integer(def.getAlertType().getCode())))
            .uniqueResult();
    }
    
    MEscalationState find(Escalatable esc) {
        Integer alertId = esc.getAlertInfo().getId();
        Integer alertType = 
            new Integer(esc.getDefinition().getAlertType().getCode());
        
        return (MEscalationState)createCriteria()
            .add(Expression.eq("alertTypeEnum", alertType))
            .add(Expression.eq("alertId", alertId))
            .uniqueResult();
    }
    
    Collection findStatesFor(MEscalation mesc) {
        return createCriteria()
            .add(Expression.eq("escalation", mesc))
            .list();
    }
}

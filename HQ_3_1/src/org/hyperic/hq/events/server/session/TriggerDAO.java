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

import java.util.Iterator;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class TriggerDAO extends HibernateDAO {
    public TriggerDAO(DAOFactory f) {
        super(RegisteredTrigger.class, f);
    }

    RegisteredTrigger create(RegisteredTriggerValue createInfo) {
        RegisteredTrigger res = new RegisteredTrigger(createInfo);
        save(res);

        //  Set the new ID just in case someone wants to use it
        createInfo.setId(res.getId());  

        EventsStartupListener.getChangedTriggerCallback()
            .afterTriggerCreated(res);
        
        return res;
    }

    void removeTriggers(AlertDefinition def) {
        EventsStartupListener.getChangedTriggerCallback()
            .beforeTriggersDeleted(def.getTriggers());
        
        def.setActOnTrigger(null);
        for (Iterator it = def.getConditions().iterator(); it.hasNext(); ) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setTrigger(null);
        }
        
        for (Iterator it = def.getTriggers().iterator(); it.hasNext(); ) {
            remove(it.next());
        }
        def.clearTriggers();
    }

    public RegisteredTrigger findById(Integer id) {
        return (RegisteredTrigger) super.findById(id);
    }

    public RegisteredTrigger get(Integer id) {
        return (RegisteredTrigger) super.get(id);
    }
}

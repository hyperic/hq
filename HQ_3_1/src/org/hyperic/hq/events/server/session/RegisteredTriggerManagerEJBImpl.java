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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

/** 
 * The trigger manager.
 *
 * @ejb:bean name="RegisteredTriggerManager"
 *      jndi-name="ejb/events/RegisteredTriggerManager"
 *      local-jndi-name="LocalRegisteredTriggerManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */

public class RegisteredTriggerManagerEJBImpl implements SessionBean {
    private TriggerDAO getTriggerDAO(){
        return DAOFactory.getDAOFactory().getTriggerDAO();
    }

    private AlertDefinitionDAO getAlertDefDAO(){
        return DAOFactory.getDAOFactory().getAlertDefDAO();
    }

    private RegisteredTrigger getRegisteredTrigger(Integer trigId) {
        return getTriggerDAO().findById(trigId);
    }
    
    /**
     * Get a collection of all triggers
     *
     * @ejb:interface-method
     */
    public Collection getAllTriggers() {
        Collection triggers;
        List triggerValues = new ArrayList();

        triggers = getTriggerDAO().findAll();

        for (Iterator i = triggers.iterator(); i.hasNext(); ) {
            RegisteredTrigger t = (RegisteredTrigger) i.next();
            
            triggerValues.add(t.getRegisteredTriggerValue());
        }

        return triggerValues;
    }

    /**
     * Create a new trigger
     *
     * @return a RegisteredTriggerValue 
     *
     * @ejb:interface-method
     */
    public RegisteredTriggerValue createTrigger(RegisteredTriggerValue val) {
        // XXX -- Things here aren't symmetrical.  The EventsBoss is currently
        // registering the trigger with the dispatcher, and updateTrigger()
        // is updating it with the dispatcher.  Seems like this should all
        // be done here in the manager
        getTriggerDAO().create(val);        // DAO method will set ID on val obj
        return val;
    }

    /**
     * Update a trigger.
     *
     * @ejb:interface-method
     */
    public void updateTrigger(RegisteredTriggerValue val) {
        RegisteredTrigger t = getRegisteredTrigger(val.getId());

        t.setRegisteredTriggerValue(val);
    }

    /**
     * Delete all triggers for an alert definition.
     *
     * @ejb:interface-method
     */
    public void deleteAlertDefinitionTriggers(Integer adId) {
        AlertDefinition def = getAlertDefDAO().findById(adId);
        getTriggerDAO().removeTriggers(def);
    }

    public static RegisteredTriggerManagerLocal getOne() {
        try {
            return RegisteredTriggerManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}
}

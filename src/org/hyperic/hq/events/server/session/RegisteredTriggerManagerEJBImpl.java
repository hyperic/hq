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

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisteredTriggerEvent;
import org.hyperic.hq.events.shared.RegisteredTriggerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerLocalHome;
import org.hyperic.hq.events.shared.RegisteredTriggerPK;
import org.hyperic.hq.events.shared.RegisteredTriggerUtil;
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
 * @ejb:transaction type="SUPPORTS"
 */

public class RegisteredTriggerManagerEJBImpl implements SessionBean {
    Log log = LogFactory.getLog(RegisteredTriggerManagerEJBImpl.class );

    RegisteredTriggerLocalHome rtHome = null;
    private RegisteredTriggerLocalHome getRTHome() {
        try {
            if (rtHome == null)
                rtHome = RegisteredTriggerUtil.getLocalHome();
            return rtHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    private RegisteredTriggerLocal getRegisteredTrigger(Integer trigId)
        throws FinderException {
        return getRTHome().findByPrimaryKey(new RegisteredTriggerPK(trigId));
    }
    
    /**
     * Get a collection of all triggers
     *
     * @ejb:interface-method
     */
    public Collection getAllTriggers() {
        Collection triggers;
        ArrayList triggerValues = new ArrayList();

        try {
            triggers = getRTHome().findAll();

            for (Iterator i = triggers.iterator(); i.hasNext(); ) {
                RegisteredTriggerLocal trigger =
                    (RegisteredTriggerLocal) i.next();
                triggerValues.add(trigger.getRegisteredTriggerValue());
            }
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
        }

        return triggerValues;
    }

    /**
     * Get a collection of all triggers
     *
     * @ejb:interface-method
     */
    public Collection getAllRegisteredTriggers() {
        Collection triggers;
        ArrayList triggerValues = new ArrayList();

        try {
            triggers = getRTHome().findAll();

            for (Iterator i = triggers.iterator(); i.hasNext(); ) {
                RegisteredTriggerLocal trigger =
                    (RegisteredTriggerLocal) i.next();
                triggerValues.add(trigger.getRegisteredTriggerValue());
            }
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
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
    public RegisteredTriggerValue createTrigger(RegisteredTriggerValue val)
        throws TriggerCreateException {
        try {
            RegisteredTriggerLocal tlocal = getRTHome().create(val);
            
            // Register the trigger with the dispatcher
            val = tlocal.getRegisteredTriggerValue();
        } catch (CreateException e) {
            throw new TriggerCreateException(e);
        }
        
        return val;
    }

    /**
     * Update a trigger.
     *
     * @ejb:interface-method
     */
    public void updateTrigger(RegisteredTriggerValue val)
        throws FinderException {
        RegisteredTriggerLocal local = this.getRegisteredTrigger(val.getId());
        local.setRegisteredTriggerValue(val);
        
        // Re-register the trigger with the dispatcher
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.UPDATE, val);
    }

    /**
     * Delete a trigger.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteTrigger(Integer trigId)
        throws FinderException, RemoveException {
        RegisteredTriggerLocal local = this.getRegisteredTrigger(trigId);
        RegisteredTriggerValue val = local.getRegisteredTriggerValue();
        local.remove();
        
        // Unregister the trigger with the dispatcher
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.DELETE, val);
    }

    /**
     * Delete all triggers for an alert definition.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteAlertDefinitionTriggers(Integer adId)
        throws FinderException, RemoveException {
        Collection triggers = getRTHome().findTriggersByAlertDef(adId);
        RegisteredTriggerValue[] vals =
            new RegisteredTriggerValue[triggers.size()];
        
        int i = 0;
        for (Iterator it = triggers.iterator(); it.hasNext(); i++) {
            RegisteredTriggerLocal local =
                (RegisteredTriggerLocal) it.next();

            vals[i] = local.getRegisteredTriggerValue();
            local.remove();
        }       

        // Unregister the trigger with the dispatcher
        RegisteredTriggerNotifier
                .broadcast(RegisteredTriggerEvent.DELETE, vals);
    }

    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C) [2004,
 * 2005, 2006], Hyperic, Inc. This file is part of HQ. HQ is free software; you
 * can redistribute it and/or modify it under the terms version 2 of the GNU
 * General Public License as published by the Free Software Foundation. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.HibernateDAOFactory;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * The trigger manager.
 * 
 * @ejb:bean name="RegisteredTriggerManager"
 *           jndi-name="ejb/events/RegisteredTriggerManager"
 *           local-jndi-name="LocalRegisteredTriggerManager" view-type="local"
 *           type="Stateless"
 * 
 * @ejb:transaction type="Required"
 */

public class RegisteredTriggerManagerEJBImpl implements SessionBean {
    private TriggerDAO getTriggerDAO() {
        return DAOFactory.getDAOFactory().getTriggerDAO();
    }

    private AlertDefinitionDAO getAlertDefDAO() {
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

        triggers = getTriggers();

        for (Iterator i = triggers.iterator(); i.hasNext();) {
            RegisteredTrigger t = (RegisteredTrigger) i.next();

            triggerValues.add(t.getRegisteredTriggerValue());
        }

        return triggerValues;
    }

    /**
     * Get a collection of all triggers
     * 
     * @ejb:interface-method
     */
    public Collection getTriggers() {
        return getTriggerDAO().findAll();
    }

    /**
     * Get the registered trigger objects associated with a given alert
     * definition.
     * 
     * @param id The alert def id.
     * @return The registered trigger objects.
     * @ejb:interface-method
     */
    public Collection getAllTriggersByAlertDefId(Integer id) {
        return getTriggerDAO().findByAlertDefinitionId(id);
    }

    /**
     * Create a new trigger
     * 
     * @return a RegisteredTriggerValue
     * 
     * @ejb:interface-method
     */
    public RegisteredTrigger createTrigger(RegisteredTriggerValue val) {
        // XXX -- Things here aren't symmetrical. The EventsBoss is currently
        // registering the trigger with the dispatcher, and updateTrigger()
        // is updating it with the dispatcher. Seems like this should all
        // be done here in the manager
        return getTriggerDAO().create(val); // DAO method will set ID on val obj
    }

    /**
     * Create new triggers
     * 
     * @return a RegisteredTriggerValue
     * 
     * @ejb:interface-method
     */
    public void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef) throws TriggerCreateException,
                                                                                   InvalidOptionException,
                                                                                   InvalidOptionValueException
    {
        ArrayList triggers = new ArrayList();

        // Create AppdefEntityID from the alert definition
        AppdefEntityID id = new AppdefEntityID(alertdef.getAppdefType(), alertdef.getAppdefId());
        // Get the frequency type
        int freqType = alertdef.getFrequencyType();
        long range = freqType == EventConstants.FREQ_COUNTER ? alertdef.getRange() : 0;

        AlertConditionValue[] conds = alertdef.getConditions();
        if (conds.length == 1) {
            // Transform into registered trigger
            RegisteredTriggerValue triggerVal = convertToTriggerValue(id, conds[0]);
            RegisteredTrigger trigger = createTrigger(triggerVal);
            // Set the trigger ID in the condition
            conds[0].setTriggerId(trigger.getId());
            alertdef.updateCondition(conds[0]);
            triggers.add(trigger);
        } else {
            for (int i = 0; i < conds.length; i++) {
                AlertConditionValue cond = conds[i];

                // Transform into registered trigger
                RegisteredTrigger rt = createTrigger(convertToTriggerValue(id, cond));
                triggers.add(rt);
                // Set the trigger ID in the condition
                conds[i].setTriggerId(rt.getId());
                alertdef.updateCondition(conds[0]);

            }
        }

        for (Iterator it = triggers.iterator(); it.hasNext();) {
            RegisteredTrigger tval = (RegisteredTrigger) it.next();
            alertdef.addTrigger(tval.getRegisteredTriggerValue());
        }
        EventsStartupListener.getChangedTriggerCallback().afterTriggersCreated(triggers);
    }

    private RegisteredTriggerValue convertToTriggerValue(AppdefEntityID id, AlertConditionValue cond) throws InvalidOptionException,
                                                                                                     InvalidOptionValueException
    {

        // Create trigger based on the type of the condition
        RegisteredTriggerValue trigger;
        try {
            Class trigClass = (Class) ConditionalTriggerInterface.MAP_COND_TRIGGER.get(new Integer(cond.getType()));

            if (trigClass == null)
                throw new InvalidOptionValueException("Condition type not yet supported");

            // Create the new instance
            Object newObj = trigClass.newInstance();

            // Make sure that the new object implements the right interface
            if (!(newObj instanceof ConditionalTriggerInterface))
                throw new InvalidOptionValueException("Condition does not generate valid trigger");

            trigger = new RegisteredTriggerValue();
            trigger.setClassname(trigClass.getName());

            // Get the config response
            ConditionalTriggerInterface ctrig = (ConditionalTriggerInterface) newObj;
            ConfigResponse resp = ctrig.getConfigResponse(id, cond);
            try {
                trigger.setConfig(resp.encode());
            } catch (EncodingException e) {
                trigger.setConfig(new byte[0]);
            }
        } catch (InstantiationException e) {
            throw new InvalidOptionValueException("Could not create a trigger instance", e);
        } catch (IllegalAccessException e) {
            throw new InvalidOptionValueException("Could not create a trigger instance", e);
        }

        return trigger;
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
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void setSessionContext(SessionContext ctx) {
    }
}

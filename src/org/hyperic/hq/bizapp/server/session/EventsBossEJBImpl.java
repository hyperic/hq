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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniPlatformNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniResourceTree;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServerNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServiceNode;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.bizapp.server.trigger.conditional.MultiConditionTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.CounterTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.DurationTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.FrequencyTriggerInterface;
import org.hyperic.hq.bizapp.shared.uibeans.DashboardAlertBean;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisteredTriggerEvent;
import org.hyperic.hq.events.server.session.RegisteredTriggerNotifier;
import org.hyperic.hq.events.shared.ActionManagerLocal;
import org.hyperic.hq.events.shared.ActionManagerUtil;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.action.MetricAlertAction;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.collection.IntHashMap;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.timer.StopWatch;

/** 
 * The BizApp's interface to the Events Subsystem
 *
 * @ejb:bean name="EventsBoss"
 *      jndi-name="ejb/bizapp/EventsBoss"
 *      local-jndi-name="LocalEventsBoss"
 *      view-type="both"
 *      type="Stateless"
 * 
 * @ejb:transaction type="NOTSUPPORTED"
 */

public class EventsBossEJBImpl extends BizappSessionEJB
    implements SessionBean {
    private SessionManager manager;

    /** Static logging context */
    protected Log log = LogFactory.getLog(EventsBossEJBImpl.class.getName());

    private RegisteredTriggerManagerLocal triggerMan      = null;
    private AlertDefinitionManagerLocal   alDefMan        = null;
    private AlertManagerLocal             alertMan        = null;
    private ActionManagerLocal            actMan          = null;
    public EventsBossEJBImpl() {
        this.manager = SessionManager.getInstance();
    }

    private RegisteredTriggerManagerLocal getRTM() {
        if (triggerMan == null) {
            try {
                triggerMan =
                    RegisteredTriggerManagerUtil.getLocalHome().create();

            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return triggerMan;
    }
    
    private AlertManagerLocal getAM() {
        if (alertMan == null) {
            try {
                alertMan = AlertManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return alertMan;
    }
        
    private AlertDefinitionManagerLocal getADM() {
        if (alDefMan == null) {
            try {
                alDefMan = AlertDefinitionManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return alDefMan;
    }

    private ActionManagerLocal getActMan() {
        if (actMan == null) {
            try {
                actMan = ActionManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return actMan;
    }

    private RegisteredTriggerValue convertToTriggerValue(
        AppdefEntityID id, AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {

        // Create trigger based on the type of the condition
        RegisteredTriggerValue trigger;
        try {
            Class trigClass = (Class)
                ConditionalTriggerInterface.MAP_COND_TRIGGER.get(
                    new Integer(cond.getType()));
            
            if (trigClass == null)
                throw new InvalidOptionValueException(
                    "Condition type not yet supported");

            // Create the new instance
            Object newObj = trigClass.newInstance();
            
            // Make sure that the new object implements the right interface
            if (newObj instanceof ConditionalTriggerInterface == false)
                throw new InvalidOptionValueException(
                    "Condition does not generate valid trigger");
            
            trigger = new RegisteredTriggerValue();
            trigger.setClassname(trigClass.getName());

            // Get the config response
            ConditionalTriggerInterface ctrig =
                (ConditionalTriggerInterface) newObj; 
            ConfigResponse resp = ctrig.getConfigResponse(id, cond);
            try {
                trigger.setConfig(resp.encode());
            } catch (EncodingException e) {
                trigger.setConfig(new byte[0]);
            }
        } catch (InstantiationException e) {
            throw new InvalidOptionValueException(
                "Could not create a trigger instance", e);
        } catch (IllegalAccessException e) {
            throw new InvalidOptionValueException(
                "Could not create a trigger instance", e);
        }

        return trigger;
    }

    /*
     * How the Boss figures out which triggers to create based on conditions
     */
    private void createTriggers(AuthzSubjectValue subject,
                                AlertDefinitionValue alertdef)
        throws TriggerCreateException, InvalidOptionException,
               InvalidOptionValueException {
        ArrayList triggers = new ArrayList();

        // Break down the conditions into registered triggers
        RegisteredTriggerValue last;
        
        // Create AppdefEntityID from the alert definition
        AppdefEntityID id = new AppdefEntityID(alertdef.getAppdefType(),
                                               alertdef.getAppdefId());
        // Get the frequency type
        int freqType = alertdef.getFrequencyType();
        long count = alertdef.getCount();
        long range = alertdef.getRange();

        AlertConditionValue[] conds = alertdef.getConditions();
        if (conds.length == 1) {
            // Transform into registered trigger
            last = convertToTriggerValue(id, conds[0]);
            last = getRTM().createTrigger(last);
            // Set the trigger ID in the condition
            conds[0].setTriggerId(last.getId());
            alertdef.updateCondition(conds[0]);
        }
        else {
            MultiConditionTrigger mcTrig = new MultiConditionTrigger();
            String condStr = new String();
            for (int i = 0; i < conds.length; i++) {
                AlertConditionValue cond = conds[i];
        
                // Transform into registered trigger
                RegisteredTriggerValue rt =
                    getRTM().createTrigger(
                        convertToTriggerValue(id, cond));
                triggers.add(rt);
                // Set the trigger ID in the condition
                conds[i].setTriggerId(rt.getId());
                alertdef.updateCondition(conds[0]);
                
                // Now add to the multi-condition
                if (cond.getRequired()) {
                    condStr += MultiConditionTrigger.AND + rt.getId();
                }
                else {
                    condStr += MultiConditionTrigger.OR + rt.getId();
                }
            }
            
            // Now create the multi-condition trigger
            last = new RegisteredTriggerValue();
            last.setClassname(mcTrig.getClass().getName());
        
            // Get the config response
            ConfigResponse resp;
            
            if (freqType == EventConstants.FREQ_DURATION) {
                resp = mcTrig.getConfigResponse(condStr, true, range);
            }
            else {
                resp = mcTrig.getConfigResponse(condStr, false, range);
            }
            
            try {
                last.setConfig(resp.encode());
            } catch (EncodingException e) {
                last.setConfig(new byte[0]);
            }
        
            // Create the multi-condition trigger    
            last = getRTM().createTrigger(last);
        }
        
        // Add the last trigger to the list
        triggers.add(last);
        
        // Now create a frequency trigger
        FrequencyTriggerInterface ftrig = null;
        switch (freqType) {
            case EventConstants.FREQ_DURATION:
                ftrig = new DurationTrigger();
                break;
            case EventConstants.FREQ_COUNTER:
                // Counter trigger
                ftrig = new CounterTrigger();
                break;
            default:
                break;
        }
        
        // If we need to create a new frequency trigger        
        if (ftrig != null) {
            RegisteredTriggerValue rt = new RegisteredTriggerValue();
            rt.setClassname(ftrig.getClass().getName());
        
            // Get the config response
            ConfigResponse resp =
                ftrig.getConfigResponse(last.getId(), range, count);
        
            try {
                rt.setConfig(resp.encode());
            } catch (EncodingException e) {
                rt.setConfig(new byte[0]);
            }
            rt = getRTM().createTrigger(rt);
            triggers.add(rt);
        }
        
        for (Iterator it = triggers.iterator(); it.hasNext(); ) {
            RegisteredTriggerValue tval = (RegisteredTriggerValue) it.next();
            alertdef.addTrigger(tval);
        }
    }

    private void cloneParentConditions(AuthzSubjectValue subject,
                                       AppdefEntityID id,
                                       AlertDefinitionValue adval,
                                       AlertConditionValue[] conds) {
        // scrub and copy the parent's conditions
        adval.removeAllConditions();
        
        for (int i = 0; i < conds.length; i++) {
            AlertConditionValue clone = new AlertConditionValue(conds[i]);
    
            try {
                switch (clone.getType()) {
                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                case EventConstants.TYPE_CHANGE:
                    Integer tid = new Integer(clone.getMeasurementId());
                    DerivedMeasurementValue dmv =
                        this.getDerivedMeasurementManager()
                            .findMeasurement(subject, tid, id.getId());
                    clone.setMeasurementId(dmv.getId().intValue());
                    break;
                case EventConstants.TYPE_ALERT:
                    Integer recoverId;
                    recoverId = getADM().findChildAlertDefinitionId(
                        id, new Integer(clone.getMeasurementId()));
                    clone.setMeasurementId(recoverId.intValue());
                    break;
                }
            } catch (MeasurementNotFoundException e) {
                // Just set to 0, it'll never fire
                clone.setMeasurementId(0);
            } catch (FinderException e) {
                // Just set to 0, it'll never fire
                clone.setMeasurementId(0);
            }
    
            // Now add it to the alert definition
            adval.addCondition(clone);
        }
    }

    /**
     * Create an alert definition
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AlertDefinitionValue createAlertDefinition(
        int sessionID, AlertDefinitionValue adval)
        throws AlertDefinitionCreateException, TriggerCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException, 
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length == 0) {
            throw new AlertDefinitionCreateException(
                "Conditions cannot be null or empty");
        }        
        // check that the user can modify alerts for resource
        canManageAlerts(subject, getAppdefEntityID(adval));
        
        // Create list of ID's to create the alert definition
        List appdefIds = new ArrayList();
        appdefIds.add(this.getAppdefEntityID(adval));
        if (adval.getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            // Look up the group
            AppdefGroupValue group;
            try {
                group = this.getAppdefBoss().findGroup(sessionID,
                        new Integer(adval.getAppdefId()));
            } catch (AppdefGroupNotFoundException e) {
                throw new AlertDefinitionCreateException(e);
            } catch (InvalidAppdefTypeException e) {
                throw new AlertDefinitionCreateException(e);
            }
            
            appdefIds.addAll(group.getAppdefGroupEntries());
        }

        ArrayList triggers = new ArrayList();
        
        AlertDefinitionValue parent = null;
        // Iterate through to create the appropriate triggers and alertdef
        for (Iterator it = appdefIds.iterator(); it.hasNext(); ) {
            AppdefEntityID id = (AppdefEntityID) it.next();

            // Reset the value object with this entity ID
            adval.setAppdefType(id.getType());
            adval.setAppdefId(id.getID());
            
            // Scrub the triggers just in case
            adval.removeAllTriggers();

            if (id.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                // If this is for the members of a group, we need to
                // scrub and copy the parent's conditions
                if (parent != null) {
                    adval.setParentId(parent.getId());                    
                    cloneParentConditions(subject, id, adval,
                                          parent.getConditions());
                }
            
                // Create the triggers
                this.createTriggers(subject, adval);
                triggers.addAll(Arrays.asList(adval.getTriggers()));
            }

            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            try {
                // Now create the alert definition
                AlertDefinitionValue created =
                    getADM().createAlertDefinition(adval);
                
                if (parent == null)
                    parent = created;
                    
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }
        
        // Broadcast the trigger creation
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.ADD,
            (RegisteredTriggerValue[]) triggers.toArray(
                new RegisteredTriggerValue[triggers.size()]));

        return parent;
    }

    /**
     * Create an alert definition for a resource type
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AlertDefinitionValue createResourceTypeAlertDefinition(
        int sessionID, AppdefEntityTypeID aetid, AlertDefinitionValue adval)
        throws AlertDefinitionCreateException, TriggerCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException, 
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length == 0) {
            throw new AlertDefinitionCreateException(
                "Conditions cannot be null or empty");
        }
        
        AlertDefinitionValue parent;
        
        // Create the parent alert definition
        adval.setAppdefType(aetid.getType());
        adval.setAppdefId(aetid.getID());
        adval.setParentId(EventConstants.TYPE_ALERT_DEF_ID);
        
        try {
            // Now create the alert definition
            parent = getADM().createAlertDefinition(adval);
        } catch (FinderException e) {
            throw new AlertDefinitionCreateException(e.getMessage());
        }

        adval.setParentId(parent.getId());

        // Lookup resources
        Integer[] entIds;
        switch (aetid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            entIds =
                getPlatformManager().getPlatformIds(subject, aetid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            entIds =
                getServerManager().getServerIds(subject, aetid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            entIds =
                getServiceManager().getServiceIds(subject, aetid.getId());
            break;
        default:
            throw new InvalidOptionException(
                "Alerts cannot be defined on appdef entity type " +
                aetid.getType());
        }
        
        ArrayList triggers = new ArrayList();
        
        // Iterate through to create the appropriate triggers and alertdef
        for (int ei = 0; ei < entIds.length; ei++) {
            AppdefEntityID id = new AppdefEntityID(aetid.getType(), entIds[ei]);

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getID());
            
            // Scrub the triggers just in case
            adval.removeAllTriggers();

            cloneParentConditions(subject, id, adval, parent.getConditions());
                        
            // Create the triggers
            this.createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));

            // Make sure the actions have the proper parentId
            adval.removeAllActions();
            ActionValue[] actions = parent.getActions();
            for (int i = 0; i < actions.length; i++) {
                actions[i].setParentId(actions[i].getId());
                adval.addAction(actions[i]);
            }
            
            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            try {
                // Now create the alert definition
                getADM().createAlertDefinition(adval);
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }
        
        // Broadcast the trigger creation
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.ADD,
            (RegisteredTriggerValue[]) triggers.toArray(
                new RegisteredTriggerValue[triggers.size()]));

        return parent;
    }

    private void setMetricAlertAction(AlertDefinitionValue adval) {
        AlertConditionValue[] conds = adval.getConditions();
        for (int i = 0; i < conds.length; i++) {
            if (conds[i].getType() == EventConstants.TYPE_THRESHOLD ||
                conds[i].getType() == EventConstants.TYPE_BASELINE  ||
                conds[i].getType() == EventConstants.TYPE_CHANGE) {

                ActionValue action = new ActionValue();
                action.setClassname(MetricAlertAction.class.getName());

                ConfigResponse config = new ConfigResponse();
                try {
                    action.setConfig(config.encode());
                } catch (EncodingException e) {
                    // This should never happen
                    log.error(
                        "Empty ConfigResponse threw an encoding error", e);
                }

                adval.addAction(action);
                break;
            }
        }
    }

    /**
     * Create an alert definition for a resource type
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     * @throws InvalidOptionValueException
     * @throws InvalidOptionException
     * @throws AlertDefinitionCreateException
     * @throws ActionCreateException
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void inheritResourceTypeAlertDefinition(AuthzSubjectValue subject,
                                                   AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException,
               InvalidOptionException, InvalidOptionValueException,
               ActionCreateException, AlertDefinitionCreateException {
        AppdefEntityValue rv = new AppdefEntityValue(id, subject);
        AppdefResourceTypeValue type = rv.getResourceTypeValue();
        
        // Find the alert definitions for the type
        AppdefEntityTypeID aetid =
            new AppdefEntityTypeID(type.getAppdefTypeId(), type.getId());
        List defs = getADM().findAlertDefinitions(
            aetid, EventConstants.TYPE_ALERT_DEF_ID, PageControl.PAGE_ALL);
        
        ArrayList triggers = new ArrayList();
        for (Iterator it = defs.iterator(); it.hasNext(); ) {
            AlertDefinitionValue adval = (AlertDefinitionValue) it.next();
            
            // Only create if definition does not already exist
            if (getADM().isAlertDefined(id, adval.getId()))
                continue;
            
            // Set the parent ID
            adval.setParentId(adval.getId());

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getID());
            
            cloneParentConditions(subject, id, adval, adval.getConditions());
        
            // Create the triggers
            this.createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));
    
            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);
    
            try {
                // Now create the alert definition
                getADM().createAlertDefinition(adval);
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }
        
        // Broadcast the trigger creation
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.ADD,
            (RegisteredTriggerValue[]) triggers.toArray(
                new RegisteredTriggerValue[triggers.size()]));
    }

    /**
     * Create a trigger
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ActionValue createAction(int sessionID, Integer adid,
                                    String className, ConfigResponse config)
        throws SessionNotFoundException, SessionTimeoutException,
               ActionCreateException, RemoveException, FinderException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);

        ActionValue action = new ActionValue();

        action.setClassname(className);
        try {
            action.setConfig( config.encode() );
        } catch (EncodingException e) {
            throw new SystemException("Couldn't encode.", e);
        }

        ArrayList alertdefs = new ArrayList();
        // Associate with alert definition
        alertdefs.add(getADM().getById(adid));
        
        // If there are any children
        alertdefs.addAll(getADM().findAlertDefinitionChildren(adid));
        
        // check that the user can actually manage alerts for this resource
        for (Iterator it = alertdefs.iterator(); it.hasNext(); ) {
            AlertDefinitionValue ad = (AlertDefinitionValue) it.next();
            canManageAlerts(subject, ad);
            action = getActMan().createAction(action);
            
            // Create an array out with the new action
            ActionValue[] actions = ad.getActions();
            Integer[] aids = new Integer[actions.length + 1];
            for (int i = 0; i < actions.length; i++) {
                aids[i] = actions[i].getId();
            }
            
            // Lastly, add the new one
            aids[actions.length] = action.getId();
            
            // Now set the actions
            getADM().updateAlertDefinitionActions(ad.getId(), aids);

            if (action.getParentId() == null) {
                action.setParentId(action.getId());
                if (log.isDebugEnabled())
                    log.debug("Set parent ID to " + action.getParentId());
            }
        }
            
        return action;
    }

    /**
     * Enable/Disable a collection of alert definitions
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void enableAlertDefinitions(int sessionID, Integer[] ids,
                                       boolean enable)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        canManageAlerts(subject, ids);
        getADM().updateAlertDefinitionsEnable(ids, enable);
    }

    /**
     * Update just the basics
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public void updateAlertDefinitionBasic(int sessionID, Integer alertDefId,
                                           String name, String desc,
                                           int priority, boolean enabled)
        throws SessionNotFoundException, SessionTimeoutException,
               FinderException, RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        canManageAlerts(subject, alertDefId);
        getADM().updateAlertDefinitionBasic(alertDefId, name, desc, priority,
                                            enabled);
    }

    /**
     * Create an alert definition
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updateAlertDefinition(int sessionID, AlertDefinitionValue adval)
        throws TriggerCreateException, InvalidOptionException,
               InvalidOptionValueException, AlertConditionCreateException,
               ActionCreateException, FinderException, RemoveException,
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length < 1) {
            throw new InvalidOptionValueException(
                "Conditions cannot be null or empty");
        }        

        ArrayList triggers = new ArrayList();
        if (EventConstants.TYPE_ALERT_DEF_ID.equals(adval.getParentId()) ||
            adval.getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            // A little more work to do for group and type alert definition
            adval = getADM().updateAlertDefinition(adval);
   
            List children =
                getADM().findAlertDefinitionChildren(adval.getId());
   
            for (Iterator it = children.iterator(); it.hasNext();) {
                AlertDefinitionValue child = (AlertDefinitionValue) it.next();
   
                AppdefEntityID id = new AppdefEntityID(child.getAppdefType(),
                                                       child.getAppdefId());

                // Now add parent's conditions, actions, and new triggers
                cloneParentConditions(subject, id, child,
                                      adval.getConditions());

                child.removeAllActions();
                ActionValue[] acts = adval.getActions();
                for (int i = 0; i < acts.length; i++) {
                    acts[i].setId(new Integer(0));
                    child.addAction(acts[i]);
                }
                
                // Set the alert definition frequency type
                child.setFrequencyType(adval.getFrequencyType());
                child.setCount(adval.getCount());
                child.setRange(adval.getRange());
                
                // Set the alert definition filtering options
                child.setWillRecover(adval.getWillRecover());
                child.setNotifyFiltered(adval.getNotifyFiltered());
                child.setControlFiltered(adval.getControlFiltered());
                    
                // Triggers are deleted by the manager
                getRTM().deleteAlertDefinitionTriggers(child.getId());
                this.createTriggers(subject, child);
                triggers.addAll(Arrays.asList(adval.getTriggers()));
            
                // Now update the alert definition
                getADM().updateAlertDefinition(child);
            }
        }
        else {
            // First, get rid of the current triggers
            getRTM().deleteAlertDefinitionTriggers(adval.getId());
            
            // Now create the new triggers
            this.createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));
                
            // Now update the alert definition
            getADM().updateAlertDefinition(adval);
        }
        
        // Broadcast the trigger creation
        RegisteredTriggerNotifier.broadcast(RegisteredTriggerEvent.ADD,
            (RegisteredTriggerValue[]) triggers.toArray(
                new RegisteredTriggerValue[triggers.size()]));
    }

    /**
     * Get actions for a given alert.
     *
     * @param aid the alert id
     *
     * @ejb:interface-method
     */
    public List getActionsForAlert(int sessionId, Integer alertId)
        throws SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject = this.manager.getSubject(sessionId);
        try {
            return getActMan().getActionsForAlert(alertId);
        } catch (FinderException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Update an action
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updateAction(int sessionID, ActionValue aval)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        getActMan().updateAction(aval);
    }

    /**
     * Delete a collection of alert definitions
     *
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="REQUIRED"
     */
    public void removeAlertDefinitions(int sessionID, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException, 
               RemoveException, PermissionException {
        getADM().deleteAlertDefinitions(id);
    }

    /**
     * Delete a collection of alert definitions
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteAlertDefinitions(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException, 
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // check security
        try {
            canManageAlerts(subject, ids);
        } catch (FinderException e) {
            throw new RemoveException("AlertDefinition was not found");
        }
        getADM().deleteAlertDefinitions(ids);
    }

    /**
     * Delete a collection of alert definitions for a type
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void deleteResourceTypeAlertDefinitions(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException, 
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        getADM().deleteAlertDefinitions(ids);
    }

    /**
     * Delete list of alerts
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void deleteAlerts(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        /* incorrect to use alert IDs as alert definition IDs
        try {
            // check security
            canManageAlerts(subject, ids);
        } catch (FinderException e) {
            throw new RemoveException("Alert Definition not found.");
        }
        */
        getAM().deleteAlerts(ids);
    }
    
    /**
     * Delete all alerts for a resource
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public int deleteAlerts(int sessionID, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // check security
        canManageAlerts(subject, aeid);
        return getAM().deleteAlerts(aeid);
    }
    
    /**
     * Delete all alerts for a given period of time
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public int deleteAlerts(int sessionID, long begin, long end)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // XXX - check security
        return getAM().deleteAlerts(begin, end);
    }
    
    /**
     * Delete all alerts for a list of alert definitions
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public int deleteAlertsForDefinitions(int sessionID, Integer[] adids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // XXX - check security
        
        // Delete alerts for definition and its children
        int count = 0;
        for (int i = 0; i < adids.length; i++) {
            count += getAM().deleteAlerts(adids[i]);
            
            List cids = getADM().findAlertDefinitionChildrenIds(adids[i]);
            
            for (Iterator it = cids.iterator(); it.hasNext(); ) {
                Integer id = (Integer) it.next();
                count += getAM().deleteAlerts(id);
            }
        }
        return count;
    }
    
    /**
     * Get an alert definition by ID
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue getAlertDefinition(int sessionID, Integer id)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        AlertDefinitionValue adval = getADM().getById(id);
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(adval.getParentId()))
            canManageAlerts(subject, getAppdefEntityID(adval));
        return adval;
    }

    /**
     * Get an alert definition by ID
     *
     * @ejb:interface-method
     */
    public AlertDefinitionBasicValue getAlertDefinitionBasic(int sessionID,
                                                             Integer id)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        AlertDefinitionBasicValue adval = getADM().getBasicById(id);
        canManageAlerts(subject, getAppdefEntityID(adval));
        return adval;
    }

    /**
     * Find an alert by ID
     *
     * @ejb:interface-method
     */
    public AlertValue getAlert(int sessionID, Integer id)
        throws SessionNotFoundException, SessionTimeoutException, 
               AlertNotFoundException, PermissionException {
        /* Can't use the alert ID as alert definition ID
        try {                    
            canManageAlerts(manager.getSubject(sessionID), id);
        } catch (FinderException e) {
            throw new AlertNotFoundException(id);                   
        }
        */
        return getAM().getById(id);
    }

    /**
     * Find an alert by ID
     *
     * @ejb:interface-method
     */
    public AlertValue getAlert(int sessionID, Integer adid, long ctime)
        throws SessionNotFoundException, SessionTimeoutException, 
               AlertNotFoundException, PermissionException {
        try {
            canManageAlerts(manager.getSubject(sessionID), adid);                   
        } catch (FinderException e) {
            throw new AlertNotFoundException(adid);
        }
        return getAM().getByAlertDefAndTime(adid, ctime);
    }

    /**
     * Get a list of all alert definitions
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public PageList findAllAlertDefinitions(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        PageList alertdefs = getADM().findAllAlertDefinitions();
        canManageAlerts(subject, alertdefs);
        return alertdefs;
    }

	/**
     * Get a collection of alert definitions for a resource
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        canManageAlerts(subject, id);
        return getADM().findAlertDefinitions(id, pc);
    }
    
    /**
     * Get a collection of alert definitions for a resource or resource type
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityID id,
                                         Integer parentId, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(id, parentId, pc);
    }
    
    /**
     * Get a collection of alert definitions for a resource or resource type
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityTypeID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(id,
                                             EventConstants.TYPE_ALERT_DEF_ID,
                                             pc);
    }
    
    /**
     * Find all alertdefs for a platform and any of its servers and services
     * @param sessionID
     * @return PageList of AlertDefinitionValues
     * @ejb:interface-method
     */
    public List findAlertDefinitionsByAgent(int sessionID, AppdefEntityID id)
    	throws SessionNotFoundException, SessionTimeoutException,
			   AppdefEntityNotFoundException, PermissionException {
    	AuthzSubjectValue subject = this.manager.getSubject(sessionID);
    	// first get the tree 
        // bomb if this isnt a platform
        if(!id.isPlatform()) {
            throw new IllegalArgumentException(id + " is not a platform");
        }
    	MiniResourceTree tree = this.getPlatformManager().getMiniResourceTree(subject, 
                                        new AppdefEntityID[] { id }, 0);
        List resourceIds = new ArrayList();
        resourceIds.add(id);
        // First add all the regular servers and services.
    	for(Iterator p = tree.getPlatformIterator();p.hasNext();) {
    	    MiniPlatformNode pn = (MiniPlatformNode)p.next();
            for(Iterator s = pn.getServerIterator(); s.hasNext();) {
                MiniServerNode sn = (MiniServerNode)s.next();
                resourceIds.add(sn.getServer().getEntityId());
                for(Iterator sv = sn.getServiceIterator(); sv.hasNext();) {
                    MiniServiceNode svn = (MiniServiceNode)sv.next();
                    resourceIds.add(svn.getService().getEntityId());
                }
            }
        }

        // Add any platform services
        PageList platformServices =
            this.getServiceManager().getPlatformServices(subject, id.getId(),
                                                         PageControl.PAGE_ALL);
        for(int i = 0; i< platformServices.size(); i++) {
            ServiceValue platformService = 
                (ServiceValue)platformServices.get(i);
            resourceIds.add(platformService.getEntityId());
        }

        List alertDefs = new ArrayList();
        for(int i = 0; i < resourceIds.size(); i++) {
            AppdefEntityID anId = (AppdefEntityID)resourceIds.get(i);
            alertDefs.addAll(this.findAlertDefinitions(sessionID, 
                                                    anId, 
                                                    PageControl.PAGE_ALL));
        }
        return alertDefs;
    }
    
    /**
     * Find all alert definition names for a resource
     * @param sessionID
     * @return Map of AlertDefinition names and IDs
     * @ejb:interface-method
     */
    public Map findAlertDefinitionNames(int sessionID, AppdefEntityID id,
                                        Integer parentId)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(parentId))
            canManageAlerts(subject, id);
        
        return getADM().findAlertDefinitionNames(id, parentId);
    }
    
    /**
     * Enable or disable alert definitions by agent
     * @param enable - true if enable false if disable
     * @ejb:interface-method
     */
    public void updateAlertsByAgent(int sessionID, AppdefEntityID platId,
                                    boolean enable)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException {
        List allAlerts = this.findAlertDefinitionsByAgent(sessionID, platId);
        for(int i = 0; i < allAlerts.size(); i++) {
            AlertDefinitionValue ad = (AlertDefinitionValue)allAlerts.get(i);
            try {
                this.enableAlertDefinitions(sessionID, new Integer[]{ad.getId()}, enable);
            } catch (FinderException e) {
                // skip it
            }
        }
    }
    
    /**
     * Check that all alerts for an agent are enabled or disabled
     * @param sessionID
     * @return true if they are all enabled false if they are all disabled
     * @ejb:interface-method
     */
    public boolean checkAllAgentAlertsEnabled(int sessionID, AppdefEntityID platId)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException {
        List allAgentAlerts = this.findAlertDefinitionsByAgent(sessionID, platId);
        for(int i = 0; i < allAgentAlerts.size(); i++) {
            AlertDefinitionValue ad = (AlertDefinitionValue)allAgentAlerts.get(i);
            if(!ad.getEnabled()) return false;
        }
        return true;
    }
    
    
    /**
     * Get a list of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // XXX no security... FIXME
        return getAM().findAllAlerts();
    }

    /**
     * Find all alerts for an appdef resource
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, AppdefEntityID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        canManageAlerts(subject, id);
        return getAM().findAlerts(id, pc);
    }

    /**
     * Find all alerts for an appdef resource
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        canManageAlerts(subject, id);
        return getAM().findAlerts(id, begin, end, pc);
    }

    /**
     * Search alerts given a set of criteria
     * @param username the username
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @throws ConfigPropertyException
     * @throws ApplicationException
     * @throws LoginException
     * @ejb:interface-method
     */
    public PageList findAlerts(String username, int count, int priority,
                               long timeRange, AppdefEntityID[] ids,
                               PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException {
        int sessionId = getAuthManager().getUnauthSessionId(username);
        return findAlerts(sessionId, count, priority, timeRange, ids, pc);
    }
    
    /**
     * Search alerts given a set of criteria
     * @param sessionID the session token
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, int count, int priority,
                               long timeRange, AppdefEntityID[] ids,
                               PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject  = this.manager.getSubject(sessionID);
        StopWatch timer = new StopWatch();
        List appentResources = null;
        if (ids == null) {
            // Assume if user can be alerted, then they can view resource,
            // otherwise, it'll be filtered out later anyways
            // find ALL alertable resources
            appentResources = 
                this.getPlatformManager().checkAlertingScope(subject);
        } else {
            // check security on all of the specified items if the list was
            // supplied by the caller
            try {
                canManageAlerts(subject, ids);
                appentResources = Arrays.asList(ids);
            } catch (FinderException e) {
                // should not happen
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("checkAlertingScope(): " + timer + " seconds");
            timer.reset();
        }
        
        PageList alerts =
            getAM().findAlerts(count, priority, timeRange, appentResources, pc);

        if (log.isDebugEnabled()) {
            log.debug("findAlerts(): " + timer + " seconds");
            timer.reset();
        }
        
        // Create the beans to return
        HashMap resMap = new HashMap();
        IntHashMap entIdMap = new IntHashMap();
        IntHashMap nameMap = new IntHashMap();
        ArrayList badIds = new ArrayList();
        
        PageList uiBeans = new PageList();
        for (Iterator it = alerts.iterator(); it.hasNext(); ){
            AlertValue alert = (AlertValue) it.next();
            Integer adId = alert.getAlertDefId();
            AppdefEntityID aeid =
                (AppdefEntityID) entIdMap.get(adId.intValue());
            String name = (String) nameMap.get(adId.intValue());
            
            if (aeid == null || name == null) {
                try {
                    aeid = getADM().getAppdefEntityIdById(adId);
                    entIdMap.put(adId.intValue(), aeid);
                    
                    name = getADM().getNameById(adId);
                    nameMap.put(adId.intValue(), name);
                } catch (FinderException e) {
                    log.error("Alert definition: " + alert.getAlertDefId() +
                              " not found for alert ID: " + alert.getId());
                    continue;
                }
            }

            if (badIds.contains(aeid))
                continue;
            
            // Check to see if we already have the resource in the hash map
            AppdefResourceValue resource =
                (AppdefResourceValue) resMap.get(aeid);

            if (resource == null) {
                AppdefEntityValue entVal = new AppdefEntityValue(aeid, subject);

                try {
                    resource = entVal.getResourceValue();
                    resMap.put(aeid, resource);
                } catch (Exception e) {
                    // Probably because the resource does not exist
                    badIds.add(aeid);
                    continue;
                }
            }
            
            DashboardAlertBean bean = new DashboardAlertBean();
            bean.setResource(resource);
            bean.setAlertId(alert.getId());
            bean.setAlertDefName(name);
            bean.setCtime(alert.getCtime());                

            uiBeans.add(bean);
        }

        // Lastly, set the total size
        uiBeans.setTotalSize(alerts.getTotalSize());
        
        if (log.isDebugEnabled())
            log.debug("create UI beans: " + timer + " seconds");
        
        return uiBeans;
    }

    /**
     * Fetch the two most recent alerts for a given user
     * @param sessionID the session token
     * @ejb:interface-method
     */
    public List findUserAlerts(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        StopWatch timer = new StopWatch();
        AuthzSubjectValue subject  = this.manager.getSubject(sessionID);
        // no need to check security here
        List alerts = getAM().findSubjectAlerts(subject.getId());
        log.debug("Time to find user alerts: " + timer.getElapsed());        
        return alerts;
    }

    /**
     * Get config schema info for an action class
     *
     * @ejb:interface-method
     */
    public ConfigSchema getActionConfigSchema(int sessionID, String actionClass)
        throws SessionNotFoundException, SessionTimeoutException, 
               EncodingException {
        ActionInterface iface;
        Class c;

        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        try {
            c = Class.forName(actionClass);
            iface = (ActionInterface) c.newInstance();
        } catch(Exception exc){
            throw new EncodingException("Failed to instantiate class: " + exc);
        }
        return iface.getConfigSchema();
    }

    /*
     * The following trigger API's are specific to the CLI only
     */
     
    /**
     * Get config schema info for a trigger class
     *
     * @ejb:interface-method
     */
    public ConfigSchema getRegisteredTriggerConfigSchema(int sessionID, 
                                                         String triggerClass)
        throws SessionNotFoundException, SessionTimeoutException, 
               EncodingException {
        AuthzSubjectValue subject;
        RegisterableTriggerInterface iface;
        Class c;

        subject  = this.manager.getSubject(sessionID);
        try {
            c = Class.forName(triggerClass);
            iface = (RegisterableTriggerInterface) c.newInstance();
        } catch(Exception exc){
            throw new EncodingException("Failed to instantiate class: " + exc);
        }
        return iface.getConfigSchema();
    }

    /**
     * Create a trigger
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Integer addRegisteredTrigger(int sessionID, String className,
                                        ConfigResponse config,
                                        AlertDefinitionValue adval)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, AlertDefinitionCreateException,
               FinderException, PermissionException, 
               TriggerCreateException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        // check security
        canManageAlerts(subject, getAppdefEntityID(adval));
        RegisteredTriggerValue trigger = new RegisteredTriggerValue();
        trigger.setClassname(className);
        try {
            trigger.setConfig( config.encode() );
        } catch (EncodingException e) {
            throw new SystemException("Couldn't encode.", e);
        }
        trigger = getRTM().createTrigger(trigger);
        
        // See if we need to create an alert definition to go with the trigger
        if (adval != null) {
            // Add the new trigger to the alert definition
            adval.addTrigger(trigger);
            
            // Create a condition with this trigger ID
            AlertConditionValue acval = new AlertConditionValue();
            acval.setType(EventConstants.TYPE_THRESHOLD);
            acval.setTriggerId(trigger.getId());
            acval.setName("Measurement Name");
            acval.setComparator("=");
            adval.addCondition(acval);           
            
            adval = getADM().createAlertDefinition(adval);
            return adval.getId();
        }
        
        return trigger.getId();
    }

    /**
     * Get a collection of all triggers using the 'Registered Dispatcher'
     *
     * @ejb:interface-method
     */
    public Collection getAllRegisteredTriggers(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException {
        AuthzSubjectValue subject = this.manager.getSubject(sessionID);
        return getRTM().getAllRegisteredTriggers();
    }

    private AppdefEntityID getAppdefEntityID(AlertDefinitionValue ad) {
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }
    
    private AppdefEntityID getAppdefEntityID(AlertDefinitionBasicValue ad) {
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }
    
    private void canManageAlerts(AuthzSubjectValue who, Integer[] adIds)
        throws PermissionException, FinderException {
        for(int i = 0; i < adIds.length; i++) {
            canManageAlerts(who, adIds[i]);
        }
    }
    
    private void canManageAlerts(AuthzSubjectValue who, Integer alertDefId)
        throws PermissionException, FinderException {
        AlertDefinitionBasicValue ad = this.getADM().getBasicById(alertDefId);
        canManageAlerts(who, ad);        
    }
    
    private void canManageAlerts(AuthzSubjectValue who, AppdefEntityID[] ids)
        throws PermissionException, FinderException {
        for (int i = 0; i < ids.length; i++) {
            canManageAlerts(who, ids[i]);
        }
    }
    
    private void canManageAlerts(AuthzSubjectValue who, List ads)
        throws PermissionException {
        for(int i = 0; i < ads.size(); i++) {
            AlertDefinitionValue ad = (AlertDefinitionValue)ads.get(i);
            canManageAlerts(who, getAppdefEntityID(ad));                                         
        }
    }
    
    private void canManageAlerts(AuthzSubjectValue who,
                                 AlertDefinitionBasicValue ad)
        throws PermissionException {
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(ad.getParentId()))
            canManageAlerts(who, getAppdefEntityID(ad));
    }
    
    private void canManageAlerts(AuthzSubjectValue who,
                                 AlertDefinitionValue ad)
        throws PermissionException {
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(ad.getParentId()))
            canManageAlerts(who, getAppdefEntityID(ad));
    }
    
    private void canManageAlerts(AuthzSubjectValue who, AppdefEntityID what)
        throws PermissionException {
        this.getPlatformManager().checkAlertingPermission(who, what);
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext ctx) {}
}

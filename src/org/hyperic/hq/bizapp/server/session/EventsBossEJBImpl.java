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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceTreeGenerator;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceChangeCallback;
import org.hyperic.hq.authz.server.session.SubjectRemoveCallback;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.bizapp.server.trigger.conditional.MultiConditionTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.CounterTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.DurationTrigger;
import org.hyperic.hq.bizapp.server.trigger.frequency.FrequencyTriggerInterface;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.ActionManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.shared.ActionManagerLocal;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.action.MetricAlertAction;
import org.hyperic.hq.measurement.server.session.DefaultMetricEnableCallback;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** 
 * The BizApp's interface to the Events Subsystem
 *
 * @ejb:bean name="EventsBoss"
 *      jndi-name="ejb/bizapp/EventsBoss"
 *      local-jndi-name="LocalEventsBoss"
 *      view-type="both"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */

public class EventsBossEJBImpl 
    extends BizappSessionEJB
    implements SessionBean 
{
    private Log _log = LogFactory.getLog(EventsBossEJBImpl.class);
    
    private SessionManager manager;

    public EventsBossEJBImpl() {
        manager = SessionManager.getInstance();
    }

    private EscalationManagerLocal getEscMan() {
        return EscalationManagerEJBImpl.getOne();
    }
    
    private RegisteredTriggerManagerLocal getRTM() {
        return RegisteredTriggerManagerEJBImpl.getOne();
    }
    
    private AlertManagerLocal getAM() {
        return AlertManagerEJBImpl.getOne();
    }
        
    private AlertDefinitionManagerLocal getADM() {
        return AlertDefinitionManagerEJBImpl.getOne();
    }

    private ActionManagerLocal getActMan() {
        return ActionManagerEJBImpl.getOne();
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
            if (!(newObj instanceof ConditionalTriggerInterface))
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
                    
                    // Don't need to synch the DerivedMeasurement with the db 
                    // since changes to the DerivedMeasurement aren't cascaded 
                    // on saving the AlertCondition.
                    DerivedMeasurement dmv =
                        getMetricManager().findMeasurement(subject, tid,
                                                           id.getId(), true);
                    clone.setMeasurementId(dmv.getId().intValue());
                    break;
                case EventConstants.TYPE_ALERT:
                    
                    // Don't need to synch the child alert definition Id lookup.
                    Integer recoverId = 
                        getADM().findChildAlertDefinitionId(id, 
                                          new Integer(clone.getMeasurementId()),
                                          true);
                                        
                    if (recoverId == null) {                        
                        // recoverId should never be null, but if it is and assertions 
                        // are disabled, just move on.
                        assert false : "recover Id should not be null.";
                        
                        _log.error("A recovery alert has no associated recover " +
                                   "from alert. Setting alert condition " +
                                   "measurement Id to 0.");
                        clone.setMeasurementId(0);
                    } else {
                        clone.setMeasurementId(recoverId.intValue());                        
                    }
                    
                    break;
                }
            } catch (MeasurementNotFoundException e) {
                // Just set to 0, it'll never fire
                clone.setMeasurementId(0);
            }
    
            // Now add it to the alert definition
            adval.addCondition(clone);
        }
    }

    private void cloneParentActions(AppdefEntityID id,
                                    AlertDefinitionValue child,
                                    ActionValue[] actions) {
        child.removeAllActions();
        for (int i = 0; i < actions.length; i++) {
            ActionValue childAct;
            try {
                ActionInterface actInst = (ActionInterface)
                    Class.forName(actions[i].getClassname()).newInstance();
                ConfigResponse config =
                    ConfigResponse.decode(actions[i].getConfig());
                actInst.setParentActionConfig(id, config);
                childAct =
                    new ActionValue(null, actInst.getImplementor(),
                                    actInst.getConfigResponse().encode(),
                                    actions[i].getId());
            } catch (Exception e) {
                // Not a valid action, skip it then
                _log.debug(actions[i].getClassname(), e);
                continue;
            }
            child.addAction(childAct);
        }
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * @ejb:interface-method 
     */
    public int[] getAlertCount(int sessionID, AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, FinderException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

        int[] counts = getAM().getAlertCount(ids);
        counts = GalertManagerEJBImpl.getOne().fillAlertCount(subject, ids,
                                                              counts);
        return counts;
    }

    /**
     * Create an alert definition
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue createAlertDefinition(int sessionID, 
                                                     AlertDefinitionValue adval)
        throws AlertDefinitionCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException, 
               SessionNotFoundException, SessionTimeoutException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length == 0) {
            throw new AlertDefinitionCreateException("Conditions cannot be " +  
                                                     "null or empty");
            
        }        
        
        // Create list of ID's to create the alert definition
        List appdefIds = new ArrayList();
        appdefIds.add(getAppdefEntityID(adval));
        if (adval.getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            // Look up the group
            AppdefGroupValue group;
            try {
                group = getAppdefBoss().findGroup(sessionID,
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
        AlertDefinitionManagerLocal adm = getADM();
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
                createTriggers(subject, adval);
                triggers.addAll(Arrays.asList(adval.getTriggers()));
            }

            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            try {
                // Now create the alert definition
                AlertDefinitionValue created =
                    adm.createAlertDefinition(subject, adval);
                
                if (parent == null)
                    parent = created;
                    
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }

        return parent;
    }

    /**
     * Create an alert definition for a resource type
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue createResourceTypeAlertDefinition(
        int sessionID, AppdefEntityTypeID aetid, AlertDefinitionValue adval)
        throws AlertDefinitionCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException, 
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

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
            parent = getADM().createAlertDefinition(subject, adval);
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
        AlertDefinitionManagerLocal adm = getADM();
        for (int ei = 0; ei < entIds.length; ei++) {
            AppdefEntityID id = new AppdefEntityID(aetid.getType(), entIds[ei]);

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getID());
            
            // Scrub the triggers just in case
            adval.removeAllTriggers();

            cloneParentConditions(subject, id, adval, parent.getConditions());
                        
            // Create the triggers
            createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));

            // Make sure the actions have the proper parentId
            cloneParentActions(id, adval, parent.getActions());
            
            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            try {
                // Now create the alert definition
                adm.createAlertDefinition(subject, adval);
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }

        return parent;
    }

    private void setMetricAlertAction(AlertDefinitionValue adval) {
        AlertConditionValue[] conds = adval.getConditions();
        for (int i = 0; i < conds.length; i++) {
            if (conds[i].getType() == EventConstants.TYPE_THRESHOLD ||
                conds[i].getType() == EventConstants.TYPE_BASELINE  ||
                conds[i].getType() == EventConstants.TYPE_CHANGE) 
            {
                ActionValue action = new ActionValue();
                action.setClassname(MetricAlertAction.class.getName());

                ConfigResponse config = new ConfigResponse();
                try {
                    action.setConfig(config.encode());
                } catch (EncodingException e) {
                    // This should never happen
                    _log.error("Empty ConfigResponse threw an encoding error", 
                              e);
                }

                adval.addAction(action);
                break;
            }
        }
    }

    /**
     * @ejb:interface-method
     */
    public void inheritResourceTypeAlertDefinition(AuthzSubjectValue subject,
                                                   AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException,
               InvalidOptionException, InvalidOptionValueException,
               AlertDefinitionCreateException 
    {
        AppdefEntityValue rv = new AppdefEntityValue(id, subject);
        AppdefResourceTypeValue type = rv.getResourceTypeValue();
        
        // Find the alert definitions for the type
        AppdefEntityTypeID aetid =
            new AppdefEntityTypeID(type.getAppdefType(), type.getId());
        
        // The alert definitions should be returned sorted by creation time.
        // This should minimize the possibility of creating a recovery alert 
        // before the recover from alert.
        PageControl pc = new PageControl(0, 
                                         PageControl.SIZE_UNLIMITED, 
                                         PageControl.SORT_ASC, 
                                         SortAttribute.CTIME);
                
        List defs = getADM().findAlertDefinitions(subject,
            aetid, EventConstants.TYPE_ALERT_DEF_ID, pc);
        
        AlertDefinitionManagerLocal adm = getADM();
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
            createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));
    
            // Recreate the actions
            cloneParentActions(id, adval, adval.getActions());
            
            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);
    
            try {
                // Now create the alert definition
                adm.createAlertDefinition(subject, adval);
            } catch (FinderException e) {
                throw new AlertDefinitionCreateException(e.getMessage());
            }
        }
    }

    /**
     * @ejb:interface-method
     */
    public ActionValue createAction(int sessionID, Integer adid,
                                    String className, ConfigResponse config)
        throws SessionNotFoundException, SessionTimeoutException,
               ActionCreateException, RemoveException, FinderException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

        ActionValue action = new ActionValue();

        action.setClassname(className);
        try {
            action.setConfig( config.encode() );
        } catch (EncodingException e) {
            throw new SystemException("Couldn't encode.", e);
        }

        ArrayList alertdefs = new ArrayList();
        
        // check that the user can actually manage alerts for this resource
        AlertDefinition ad = getADM().getByIdAndCheck(subject, adid);
        alertdefs.add(ad);
        
        // If there are any children
        alertdefs.addAll(ad.getChildren());
        
        Action root = null;
        ActionManagerLocal actMan = getActMan();
        for (Iterator it = alertdefs.iterator(); it.hasNext(); ) {
            ad = (AlertDefinition) it.next();

            if (root == null) {
                root = actMan.createAction(ad, action, null);
            }
            else {
                actMan.createAction(ad, action, root);
            }
        }
            
        return action;
    }

    /**
     * Activate/deactivate a collection of alert definitions
     *
     * @ejb:interface-method
     */
    public void activateAlertDefinitions(int sessionID, Integer[] ids,
                                         boolean activate)
        throws SessionNotFoundException, SessionTimeoutException, 
               FinderException, PermissionException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        getADM().updateAlertDefinitionsActiveStatus(subject, ids, activate);
    }

    /**
     * Update just the basics
     *
     * @ejb:interface-method
     */
    public void updateAlertDefinitionBasic(int sessionID, Integer alertDefId,
                                           String name, String desc,
                                           int priority, boolean activate)
        throws SessionNotFoundException, SessionTimeoutException,
               FinderException, RemoveException, PermissionException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        getADM().updateAlertDefinitionBasic(subject, alertDefId, name, desc,
                                            priority, activate);
    }

    /**
     * @ejb:interface-method
     */
    public void updateAlertDefinition(int sessionID, AlertDefinitionValue adval)
        throws TriggerCreateException, InvalidOptionException,
               InvalidOptionValueException, AlertConditionCreateException,
               ActionCreateException, FinderException, RemoveException,
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length < 1) {
            throw new InvalidOptionValueException(
                "Conditions cannot be null or empty");
        }        

        AlertDefinitionManagerLocal adm = getADM();
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

                cloneParentActions(id, child, adval.getActions());
                
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
                child.removeAllTriggers();
                createTriggers(subject, child);
                triggers.addAll(Arrays.asList(adval.getTriggers()));
            
                // Now update the alert definition
                adm.updateAlertDefinition(child);
            }
        }
        else {
            // First, get rid of the current triggers
            getRTM().deleteAlertDefinitionTriggers(adval.getId());
            adval.removeAllTriggers();

            // Now create the new triggers
            createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));
                
            // Now update the alert definition
            adm.updateAlertDefinition(adval);
        }
    }

    /**
     * Get actions for a given alert.
     *
     * @param alertId the alert id
     *
     * @ejb:interface-method
     */
    public List getActionsForAlert(int sessionId, Integer alertId)
        throws SessionNotFoundException, SessionTimeoutException
    {
        manager.getSubjectPojo(sessionId);
        return getActMan().getActionsForAlert(alertId.intValue());
    }

    /**
     * Update an action
     *
     * @ejb:interface-method
     */
    public void updateAction(int sessionID, ActionValue aval)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        manager.getSubjectPojo(sessionID);
        getActMan().updateAction(aval);
    }

    /**
     * Delete a collection of alert definitions
     *
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException, 
               RemoveException, PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        getADM().deleteAlertDefinitions(subject, ids);
    }

    /**
     * Delete list of alerts
     *
     * @ejb:interface-method
     */
    public void deleteAlerts(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException 
    {
        manager.getSubjectPojo(sessionID);
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
     */
    public int deleteAlerts(int sessionID, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getAM().deleteAlerts(subject, aeid);
    }
    
    /**
     * Delete all alerts for a given period of time
     *
     * @ejb:interface-method
     */
    public int deleteAlerts(int sessionID, long begin, long end)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException 
    {
        manager.getSubjectPojo(sessionID);
        // XXX - check security
        return getAM().deleteAlerts(begin, end);
    }
    
    /**
     * Delete all alerts for a list of alert definitions
     * @throws FinderException if alert definition is not found
     *
     * @ejb:interface-method
     */
    public int deleteAlertsForDefinitions(int sessionID, Integer[] adids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException, FinderException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        
        // Delete alerts for definition and its children
        int count = 0;
        AlertDefinitionManagerLocal adm = getADM();
        for (int i = 0; i < adids.length; i++) {
            AlertDefinition def = adm.getByIdAndCheck(subject, adids[i]);
            count += getAM().deleteAlerts(subject, def);
            
            Collection children = def.getChildren();
            
            for (Iterator it = children.iterator(); it.hasNext(); ) {
                AlertDefinition child = (AlertDefinition) it.next();
                count += getAM().deleteAlerts(subject, child);
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
               FinderException, PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().getById(subject, id);
    }

    /**
     * Find an alert by ID
     *
     * @ejb:interface-method
     */
    public AlertValue getAlert(int sessionID, Integer id)
        throws SessionNotFoundException, SessionTimeoutException, 
               AlertNotFoundException, PermissionException 
    {
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
     * Get a list of all alert definitions
     *
     * @ejb:interface-method
     */
    public PageList findAllAlertDefinitions(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().findAllAlertDefinitions(subject);
    }

	/**
     * Get a collection of alert definitions for a resource
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(subject, id, pc);
    }
    
    /**
     * Get a collection of alert definitions for a resource or resource type
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityID id,
                                         Integer parentId, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(subject, id, parentId, pc);
    }
    
    /**
     * Get a collection of alert definitions for a resource or resource type
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityTypeID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(subject, id,
                                             EventConstants.TYPE_ALERT_DEF_ID,
                                             pc);
    }
    
    /**
     * Find all alertdefs for a platform and any of its servers and services
     * @return PageList of AlertDefinitionValues
     * @ejb:interface-method
     */
    public List findAlertDefinitionsByAgent(int sessionID, AppdefEntityID id)
    	throws SessionNotFoundException, SessionTimeoutException,
			   AppdefEntityNotFoundException, PermissionException 
    {
    	AuthzSubject subject = manager.getSubjectPojo(sessionID);
    	// first get the tree 
        // bomb if this isnt a platform
        if(!id.isPlatform()) {
            throw new IllegalArgumentException(id + " is not a platform");
        }
        
        ResourceTree tree =
            getApplicationManager().getResourceTree(subject,
                                                    new AppdefEntityID[] { id },
                                                    ResourceTreeGenerator.
                                                    TRAVERSE_UP);
        List resourceIds = new ArrayList();
        resourceIds.add(id);
        // First add all the regular servers and services.
    	for (Iterator p = tree.getPlatformIterator(); p.hasNext();) {
            PlatformNode pn = (PlatformNode) p.next();
            for (Iterator s = pn.getServerIterator(); s.hasNext();) {
                ServerNode sn = (ServerNode) s.next();
                resourceIds.add(sn.getServer().getEntityId());
                for (Iterator sv = sn.getServiceIterator(); sv.hasNext();) {
                    ServiceNode svn = (ServiceNode) sv.next();
                    resourceIds.add(svn.getService().getEntityId());
                }
            }
        }

        // Add any platform services
    	AppdefEntityValue aeval = new AppdefEntityValue(id, subject);
        List platformServices =
            aeval.getAssociatedServices(PageControl.PAGE_ALL);
        
        for(int i = 0; i< platformServices.size(); i++) {
            ServiceValue platformService = 
                (ServiceValue)platformServices.get(i);
            resourceIds.add(platformService.getEntityId());
        }

        AlertDefinitionManagerLocal adm = getADM();
        List alertDefs = new ArrayList();
        for(int i = 0; i < resourceIds.size(); i++) {
            AppdefEntityID anId = (AppdefEntityID)resourceIds.get(i);
            alertDefs.addAll(adm.findAlertDefinitions(subject, anId));
        }
        return alertDefs;
    }
    
    /**
     * Find all alert definition names for a resource
     * @return Map of AlertDefinition names and IDs
     * @ejb:interface-method
     */
    public Map findAlertDefinitionNames(int sessionID, AppdefEntityID id,
                                        Integer parentId)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitionNames(subject, id, parentId);
    }
    
    /**
     * Activate or deactivate alert definitions by agent.
     * 
     * @ejb:interface-method
     */
    public void updateAlertsByAgent(int sessionID, AppdefEntityID platId,
                                    boolean activate)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        List allAlerts = findAlertDefinitionsByAgent(sessionID, platId);
        AlertDefinitionManagerLocal adm = getADM();
        for (Iterator it = allAlerts.iterator(); it.hasNext();) {
            AlertDefinition ad = (AlertDefinition) it.next();
            adm.updateAlertDefinitionActiveStatus(subject, ad, activate);
        }
    }
    
    /**
     * Check that all alerts for an agent are enabled or disabled
     * @return true if they are all enabled false if they are all disabled
     * @ejb:interface-method
     */
    public boolean checkAllAgentAlertsEnabled(int sessionID,
                                              AppdefEntityID platId)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException 
    {
        List allAgentAlerts = findAlertDefinitionsByAgent(sessionID, platId);
        for(int i = 0; i < allAgentAlerts.size(); i++) {
            AlertDefinition ad = (AlertDefinition) allAgentAlerts.get(i);
            if(!ad.isEnabled())
                return false;
        }
        return true;
    }
    
    
    /**
     * Get a list of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException 
    {
        manager.getSubjectPojo(sessionID);
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
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getAM().findAlerts(subject, id, pc);
    }

    /**
     * Find all alerts for an appdef resource
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        return getAM().findAlerts(subject, id, begin, end, pc);
    }

    /**
     * Search alerts given a set of criteria
     * @param username the username
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     * @ejb:interface-method
     */
    public List findRecentAlerts(String username, int count, int priority,
                                 long timeRange, AppdefEntityID[] ids) 
        throws LoginException, ApplicationException, ConfigPropertyException 
    {
        int sessionId = getAuthManager().getUnauthSessionId(username);
        return findRecentAlerts(sessionId, count, priority, timeRange, ids);
    }
    
    /**
     * Search recent alerts given a set of criteria
     * @param sessionID the session token
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     * @ejb:interface-method
     */
    public List findRecentAlerts(int sessionID, int count, int priority, 
                                 long timeRange, AppdefEntityID[] ids)  
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException 
    {
        AuthzSubjectValue subject  = manager.getSubject(sessionID);
        long cur = System.currentTimeMillis();
        
        List appentResources =
            ids != null ? appentResources = Arrays.asList(ids) : null;
        
        // Assume if user can be alerted, then they can view resource,
        // otherwise, it'll be filtered out later anyways
        List alerts = getAM().findEscalatables(subject, count, priority, 
                                               timeRange, cur, appentResources);

        // CheckAlertingScope now only used for galerts
        if (ids == null) {
            // find ALL alertable resources
            appentResources = getPlatformManager().checkAlertingScope(subject);
        }
        
        GalertManagerLocal gMan = GalertManagerEJBImpl.getOne();
        List galerts = gMan.findEscalatables(subject, count, priority, 
                                             timeRange, cur, appentResources);   
        alerts.addAll(galerts);

        Collections.sort(alerts, new Comparator() {
            public int compare(Object o1, Object o2) {
                long l1 = ((Escalatable)o1).getAlertInfo().getTimestamp();
                long l2 = ((Escalatable)o2).getAlertInfo().getTimestamp();
                
                // Reverse sort
                if (l1 > l2)
                    return -1;
                else if (l1 < l2)
                    return 1;
                else 
                    return 0;
            }
        });
        
        Set goodIds = new HashSet();
        List badIds = new ArrayList();
        
        List res = new ArrayList();
        for (Iterator i = alerts.iterator();
             i.hasNext() && res.size() <= count; ) 
        {
            Escalatable alert = (Escalatable) i.next();
            PerformsEscalations def = alert.getDefinition();
            AlertDefinitionInterface defInfo = def.getDefinitionInfo();
            AppdefEntityID aeid;

            aeid = new AppdefEntityID(defInfo.getAppdefType(),
                                      defInfo.getAppdefId());

            if (badIds.contains(aeid))
                continue;
            
            // Check to see if we already have the resource in the hash map
            if (!goodIds.contains(aeid)) {
                AppdefEntityValue entVal = new AppdefEntityValue(aeid, subject);

                try {
                    entVal.getName();
                    goodIds.add(aeid);
                } catch (Exception e) {
                    // Probably because the resource does not exist
                    badIds.add(aeid);
                    continue;
                }
            }
            
            res.add(alert);
        }
        
        return res;
    }

    /**
     * Get config schema info for an action class
     *
     * @ejb:interface-method
     */
    public ConfigSchema getActionConfigSchema(int sessionID, String actionClass)
        throws SessionNotFoundException, SessionTimeoutException, 
               EncodingException 
    {
        manager.getSubjectPojo(sessionID);
        ActionInterface iface;
        try {
            Class c = Class.forName(actionClass);
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
               EncodingException 
    {
        manager.getSubjectPojo(sessionID);
        RegisterableTriggerInterface iface;
        Class c;

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
     */
    public Integer addRegisteredTrigger(int sessionID, String className,
                                        ConfigResponse config,
                                        AlertDefinitionValue adval)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, AlertDefinitionCreateException,
               FinderException, PermissionException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        // check security
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
            
            adval = getADM().createAlertDefinition(subject, adval);
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
        throws SessionNotFoundException, SessionTimeoutException
    {
        manager.getSubjectPojo(sessionID);
        return getRTM().getAllTriggers();
    }
    
    /**
     * Flush registered triggers cache
     * @throws SessionTimeoutException 
     * @throws SessionNotFoundException 
     * @ejb:interface-method
     */
    public void flushRegisteredTriggers(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException {
        manager.getSubjectPojo(sessionID);
        RegisteredTriggers.reinitialize();
    }

    /**
     * @ejb:interface-method
     */
    public void deleteEscalationByName(int sessionID, String name)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);
        Escalation e = getEscMan().findByName(name);
        
        getEscMan().deleteEscalation(subject, e);
    }

    /**
     * @ejb:interface-method
     */
    public void deleteEscalationById(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        deleteEscalationById(sessionID, new Integer[]{id});
    }

    /**
     * remove escalation by id
     * @ejb:interface-method
     */
    public void deleteEscalationById(int sessionID, Integer[] ids)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);
        EscalationManagerLocal escMan = getEscMan();

        for (int i=0; i<ids.length; i++) {
            Escalation e = escMan.findById(ids[i]);
            
            escMan.deleteEscalation(subject, e);
        }
    }


    /**
     * retrieve escalation by alert definition id.
     */
    private Escalation findEscalationByAlertDefId(AuthzSubjectValue subject,
                                                  Integer id, 
                                                  EscalationAlertType type)
        throws PermissionException
    {
        return getEscMan().findByDefId(type, id);
    }
    
    /**
     * retrieve escalation name by alert definition id.
     *
     * @ejb:interface-method
     */
    public Integer getEscalationIdByAlertDefId(int sessionID, Integer id,
                                               EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, FinderException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        Escalation esc = findEscalationByAlertDefId(subject, id, alertType);

        return esc == null ? null : esc.getId();
    }

    /**
     * set escalation name by alert definition id.
     *
     * @ejb:interface-method
     */
    public void setEscalationByAlertDefId(int sessionID, Integer id,
                                          Integer escId,
                                          EscalationAlertType alertType) 
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.getSubjectPojo(sessionID);
        Escalation escalation = findEscalationById(sessionID, escId);
        // TODO: check permission
        getEscMan().setEscalation(alertType, id, escalation);
    }
    
    /**
     * unset escalation by alert definition id.
     *
     * @ejb:interface-method
     */
    public void unsetEscalationByAlertDefId(int sessionID, Integer id,
                                            EscalationAlertType alertType) 
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.getSubjectPojo(sessionID);
        // TODO: check permission
        getEscMan().setEscalation(alertType, id, null);
    }
    
    /**
     * retrieve escalation JSONObject by alert definition id.
     *
     * @ejb:interface-method
     */
    public JSONObject jsonEscalationByAlertDefId(int sessionID, Integer id,
                                                 EscalationAlertType alertType)
        throws SessionException, PermissionException, JSONException, 
               FinderException
    {
        AuthzSubjectValue subject = manager.getSubject(sessionID);

        Escalation e = findEscalationByAlertDefId(subject, id, alertType);
        return e == null ? null 
                         : new JSONObject().put(e.getJsonName(), e.toJSON());
    }

    /**
     * retrieve escalation object by escalation id.
     *
     * @ejb:interface-method
     */
    public Escalation findEscalationById(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);
        Escalation e = getEscMan().findById(subject, id);

        // XXX: Temporarily get around lazy loading problem        
        e.isPauseAllowed();
        e.getMaxPauseTime();
        return e;
    }

    /**
     * @ejb:interface-method
     */
    public void addAction(int sessionID, Escalation e, 
                          ActionConfigInterface cfg, long waitTime)  
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.getSubjectPojo(sessionID);
        getEscMan().addAction(e, cfg, waitTime);
    }
    
    /**
     * @ejb:interface-method
     */
    public void removeAction(int sessionID, Integer escId, Integer actId)  
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.getSubjectPojo(sessionID);
        Escalation e = getEscMan().findById(escId);
        
        if (e != null) {
            getEscMan().removeAction(e, actId);
        }
    }
    
    /**
     * Retrieve a list of {@link EscalationState}s, representing the active
     * escalations in the system.  
     * 
     * @ejb:interface-method
     */
    public List getActiveEscalations(int sessionId, int maxEscalations) 
        throws SessionException
    {
        manager.authenticate(sessionId);

        return getEscMan().getActiveEscalations(maxEscalations);
    }
    
    /**
     * Gets the escalatable associated with the specified state
     * @ejb:interface-method
     */
    public Escalatable getEscalatable(int sessionId, EscalationState state) 
        throws SessionException
    {
        manager.authenticate(sessionId);
        return getEscMan().getEscalatable(state);
    }
    
    /**
     * retrieve all escalation policy names as a Array of JSONObject.
     *
     * Escalation json finders begin with json* to be consistent with
     * DAO finder convention
     *
     * @ejb:interface-method
     */
    public JSONArray listAllEscalationName(int sessionID)
        throws JSONException, SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject  subject = manager.getSubjectPojo(sessionID);
        Collection all = getEscMan().findAll(subject);
        JSONArray jarr = new JSONArray();
        for (Iterator i = all.iterator(); i.hasNext(); ) {
            Escalation esc = (Escalation)i.next();
            jarr.put(new JSONObject()
                .put("id", esc.getId())
                .put("name", esc.getName()));
        }
        return jarr;
    }

    private AppdefEntityID getAppdefEntityID(AlertDefinitionValue ad) {
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }

    /**
     * Create a new escalation.  If alertDefId is non-null, the escalation
     * will also be associated with the given alert definition.  
     * 
     * @ejb:interface-method
     */
    public Escalation createEscalation(int sessionID, String name, String desc,
                                       boolean allowPause, long maxWaitTime,
                                       boolean notifyAll, 
                                       EscalationAlertType alertType,
                                       Integer alertDefId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, DuplicateObjectException
    {
        manager.getSubjectPojo(sessionID);
        Escalation res;
        
        // XXX -- We need to do perm-checking here
        
        res = getEscMan().createEscalation(name, desc, allowPause, maxWaitTime, 
                                           notifyAll);
        
        if (alertDefId != null) {
            // The alert def needs to use this escalation
            getEscMan().setEscalation(alertType, alertDefId, res);
        }
        return res;
    }

    /**
     * Update basic escalation properties
     * 
     * @ejb:interface-method
     */
    public void updateEscalation(int sessionID, Escalation escalation,
                                 String name, String desc, long maxWait,
                                 boolean pausable, boolean notifyAll)
        throws SessionTimeoutException, SessionNotFoundException, 
               PermissionException, DuplicateObjectException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);

        getEscMan().updateEscalation(subject, escalation, name, desc, 
                                     pausable, maxWait, notifyAll);
    }

    /**
     * @ejb:interface-method
     */
    public void acknowledgeAlert(int sessionID, EscalationAlertType alertType, 
                                 Integer alertID, long pauseWaitTime,
                                 String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);

        getEscMan().acknowledgeAlert(subject, alertType, alertID, moreInfo);
    }

    /**
     * @ejb:interface-method
     */
    public void fixAlert(int sessionID, EscalationAlertType alertType,
                         Integer alertID, String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        AuthzSubject subject = manager.getSubjectPojo(sessionID);
        
        getEscMan().fixAlert(subject, alertType, alertID, moreInfo);
    }
    
    /**
     * Get the last fix if available
     * @ejb:interface-method
     */
    public String getLastFix(int sessionID, Integer defId)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, FinderException {
        AuthzSubjectValue subject = manager.getSubject(sessionID);
        
        // Look for the last fixed alert
        AlertDefinition def = getADM().getByIdAndCheck(subject, defId);
        return getEscMan().getLastFix(def);
    }
    
    /**
     * See if alerts are enabled
     * @ejb:interface-method
     */
    public boolean alertsAllowed(int sessionId)
        throws SessionNotFoundException, SessionTimeoutException {
        manager.getSubjectPojo(sessionId);
        return getAM().alertsAllowed();
    }

    /**
     * Set to allow/disallow alerts
     * @ejb:interface-method
     */
    public void setAlertsAllowed(int sessionId, boolean allowed)
        throws SessionNotFoundException, SessionTimeoutException {
        manager.getSubjectPojo(sessionId);
        getAM().setAlertsAllowed(allowed);
    }

    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Events Boss starting up!");
        
        HQApp app = HQApp.getInstance();
        app.registerCallbackListener(DefaultMetricEnableCallback.class,
            new DefaultMetricEnableCallback() {
                public void metricsEnabled(AppdefEntityID ent) {
                    try {
                        _log.info("Inheriting type-based alert defs for " +ent);
                        EventsBossLocal eb = EventsBossEJBImpl.getOne();
                        AuthzSubjectValue overlord = 
                            AuthzSubjectManagerEJBImpl.getOne().getOverlord();
                        eb.inheritResourceTypeAlertDefinition(overlord, ent);
                    } catch(Exception e) {
                        throw new SystemException(e);
                    }
                }
            }
        );
        
        app.registerCallbackListener(ResourceChangeCallback.class,
            new ResourceChangeCallback() {
                public void preAppdefResourcesDelete(AppdefEntityID[] ids) {
                    // Need to tell alert definitions to dissociate the
                    // Resources
                    AlertDefinitionManagerLocal adm = getADM();
                    for (int i = 0; i < ids.length; i++) {
                        adm.disassociateResource(ids[i]);
                    }
                }
            }
        );
        
        app.registerCallbackListener(SubjectRemoveCallback.class, 
            new SubjectRemoveCallback() {
                public void subjectRemoved(AuthzSubject toDelete) {
                    getEscMan().handleSubjectRemoval(toDelete);
                    getAM().handleSubjectRemoval(toDelete);
                }
            
            }
        );
        
        // Add listener to remove alert definition and alerts after resources
        // are deleted.
        HashSet events = new HashSet();
        events.add (ResourceDeletedZevent.class);
        ZeventManager.getInstance().addBufferedListener(events,
            new ZeventListener() {
                public void processEvents(List events) {
                    AlertDefinitionManagerLocal adm = getADM();
                    for (Iterator i = events.iterator(); i.hasNext();) {
                        ResourceZevent z = (ResourceZevent) i.next();
                        if (z instanceof ResourceDeletedZevent) {
                            adm.cleanupAlertDefinitions(z.getAppdefEntityID());
                        }
                    }
                }
                
                public String toString() {
                    return "AlertDefCleanupListener";
                }
            }
        );
    }

    public static EventsBossLocal getOne() {
        try {
            return EventsBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
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

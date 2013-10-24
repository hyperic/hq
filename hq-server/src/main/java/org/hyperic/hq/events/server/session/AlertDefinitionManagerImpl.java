/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.action.EnableAlertDefActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.action.MetricAlertAction;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MetricsEnabledEvent;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Stores Events to and deletes Events from storage
 * 
 * </p>
 * 
 */
@Service
@Transactional
public class AlertDefinitionManagerImpl implements AlertDefinitionManager,
    ApplicationListener<ApplicationEvent> {
    
    private final Log log = LogFactory.getLog(AlertDefinitionManagerImpl.class);

    private final AlertPermissionManager alertPermissionManager;

    private final String VALUE_PROCESSOR = PagerProcessor_events.class.getName();

    private Pager _valuePager = null;

    private AlertDefinitionDAO alertDefDao;

    private ActionDAO actionDao;

    private AlertConditionDAO alertConditionDAO;
    
    private AlertDAO alertDAO;
    
    private final MeasurementManager measurementManager;

    private final RegisteredTriggerManager registeredTriggerManager;

    private final ResourceManager resourceManager;

    private final EscalationManager escalationManager;
    
    private final AuthzSubjectManager authzSubjectManager;

    private final AlertAuditFactory alertAuditFactory;
    
    private final AvailabilityDownAlertDefinitionCache availabilityDownAlertDefinitionCache;

    @Autowired
    public AlertDefinitionManagerImpl(AlertPermissionManager alertPermissionManager, AlertDefinitionDAO alertDefDao,
                                      ActionDAO actionDao, AlertConditionDAO alertConditionDAO, 
                                      MeasurementManager measurementManager, RegisteredTriggerManager registeredTriggerManager,
                                      ResourceManager resourceManager, EscalationManager escalationManager,
                                      AlertAuditFactory alertAuditFactory, AlertDAO alertDAO,
                                      AuthzSubjectManager authzSubjectManager,
                                      AvailabilityDownAlertDefinitionCache availabilityDownAlertDefinitionCache) {
        this.alertPermissionManager = alertPermissionManager;
        this.alertDefDao = alertDefDao;
        this.actionDao = actionDao;
        this.alertConditionDAO = alertConditionDAO;
        this.measurementManager = measurementManager;
        this.registeredTriggerManager = registeredTriggerManager;
        this.resourceManager = resourceManager;
        this.escalationManager = escalationManager;
        this.authzSubjectManager = authzSubjectManager;
        this.alertAuditFactory = alertAuditFactory;
        this.alertDAO = alertDAO;
        this.availabilityDownAlertDefinitionCache = availabilityDownAlertDefinitionCache;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        _valuePager = Pager.getPager(VALUE_PROCESSOR);
    }
    
    @PreDestroy
    public final void destroy() { 
        this.alertDefDao = null ; 
        this.actionDao = null ; 
        this.alertConditionDAO = null ; 
        this.alertDAO = null ; 
    }//EOM 
    
    public void onApplicationEvent(ApplicationEvent event) {        
        if (event instanceof ResourceDeleteRequestedEvent) {
            disassociateResource(((ResourceDeleteRequestedEvent) event).getResource());
        } else if (event instanceof MetricsEnabledEvent) {
            metricsEnabled(((MetricsEnabledEvent) event).getEntityId());
        }
    }
    
    private boolean deleteAlertDefinitionStuff(AuthzSubject subj, AlertDefinition alertdef, EscalationManager escMan) {
        StopWatch watch = new StopWatch();

        // Delete escalation state
        watch.markTimeBegin("endEscalation");
        if (alertdef.getEscalation() != null && !alertdef.isResourceTypeDefinition()) {
            escMan.endEscalation(alertdef);
        }
        watch.markTimeEnd("endEscalation");

        availabilityDownAlertDefinitionCache.removeFromCache(alertdef);

        if (log.isDebugEnabled()) {
            log.debug("deleteAlertDefinitionStuff: " + watch);
        }

        return true;
    }

    /**
     * Remove alert definitions. It is assumed that the subject has permission
     * to remove this alert definition and any of its' child alert definitions.
     */
    private boolean deleteAlertDefinition(AuthzSubject subj, AlertDefinition alertdef, boolean force)
        throws PermissionException {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        if (force) { // Used when resources are being deleted
            // Disassociate from Resource so that the Resource can be deleted
            alertdef.setResource(null);
        } else {
            // If there are any children, delete them, too
            if(debug){
                watch.markTimeBegin("delete children");
            }
            List<AlertDefinition> childBag = new ArrayList<AlertDefinition>(alertdef.getChildrenBag());
            for (AlertDefinition child : childBag) {
                deleteAlertDefinitionStuff(subj, child, escalationManager);
                registeredTriggerManager.deleteTriggers(child);
            }
            if (debug) watch.markTimeBegin("deleteByAlertDefinition");
            alertDefDao.deleteByAlertDefinition(alertdef);
            if(debug) {
                watch.markTimeEnd("deleteByAlertDefinition");
                watch.markTimeEnd("delete children");
            }
        }

        deleteAlertDefinitionStuff(subj, alertdef, escalationManager);

        if (debug)  watch.markTimeBegin("deleteTriggers");
        registeredTriggerManager.deleteTriggers(alertdef);
        if(debug) watch.markTimeBegin("deleteTriggers");

        if(debug) watch.markTimeBegin("markActionsDeleted");
        actionDao.deleteAlertDefinition(alertdef);
        if(debug) watch.markTimeBegin("markActionsDeleted");

        if(debug) watch.markTimeBegin("mark deleted");
        // Disassociated from escalations
        alertdef.setEscalation(null);
        alertdef.setDeleted(true);
        alertdef.setActiveStatus(false);
        // Disassociate from parent
        // This must be at the very end since we use the parent to determine
        // whether or not this is a resource type alert definition.
        alertdef.setParent(null);
        

        if (debug) {
            watch.markTimeEnd("mark deleted");
            log.debug("deleteAlertDefinition: " + watch);
        }

        return true;
    }

    /**
     * Get the MetricAlertAction ActionValue from an AlertDefinitionValue. If
     * none exists, return null.
     */
    private ActionValue getMetricAlertAction(AlertDefinitionValue adv) {
        ActionValue[] actions = adv.getActions();
        for (int i = 0; i < actions.length; ++i) {
            String actionClass = actions[i].getClassname();
            if (MetricAlertAction.class.getName().equals(actionClass)) {
                return actions[i];
            }
        }
        return null;
    }

    private void setMetricAlertAction(AlertDefinitionValue adval) {
        AlertConditionValue[] conds = adval.getConditions();
        for (int i = 0; i < conds.length; i++) {
            if (conds[i].getType() == EventConstants.TYPE_THRESHOLD ||
                conds[i].getType() == EventConstants.TYPE_BASELINE ||
                conds[i].getType() == EventConstants.TYPE_CHANGE) {
                ActionValue action = getMetricAlertAction(adval);

                // if MetricAlertAction doesn't exist, add one
                if (action == null) {
                    action = new ActionValue();
                    action.setClassname(MetricAlertAction.class.getName());

                    ConfigResponse config = new ConfigResponse();
                    try {
                        action.setConfig(config.encode());
                    } catch (EncodingException e) {
                        // This should never happen
                        log.error("Empty ConfigResponse threw an encoding error", e);
                    }

                    adval.addAction(action);
                }
                break;

            }
        }
    }
     
    /**
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AuthzSubject subj, AlertDefinitionValue a)
        throws AlertDefinitionCreateException, PermissionException {
        if (EventConstants.TYPE_ALERT_DEF_ID.equals(a.getParentId())) {
           
            // ...check that user has access to resource type alert definitions alert definition's resource...
            alertPermissionManager.canCreateResourceTypeAlertDefinitionTemplate(subj);
            // Subject permissions should have already been checked when
            // creating
            // the parent (resource type) alert definition.
        } else if (!a.parentIdHasBeenSet()) {
            // ...check that user has create permission on alert definition's resource...
            alertPermissionManager.canCreateAlertDefinition(subj, new AppdefEntityID(a.getAppdefType(),
                a.getAppdefId()));
        }
        return createAlertDefinition(a);
    }

    /**
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AlertDefinitionValue a) 
    	throws AlertDefinitionCreateException {

        // Create a measurement AlertLogAction if necessary
        setMetricAlertAction(a);
        
        // HHQ-1054: since the alert definition mtime is managed explicitly,
        // let's initialize it
        a.initializeMTimeToNow();

        AlertDefinition res = new AlertDefinition();

        // The following is duplicated out of what the Impl did. Makes sense
        a.cleanAction();
        a.cleanCondition();
        a.cleanTrigger();
        alertDefDao.setAlertDefinitionValue(res, a);

        // Create new conditions
        AlertConditionValue[] conds = a.getConditions();

        for (AlertConditionValue condition : conds) {
            RegisteredTrigger trigger = condition.getTriggerId() != null ? registeredTriggerManager.findById(condition
                .getTriggerId()) : null;

            AlertCondition cond = res.createCondition(condition, trigger);

            if (res.getName() == null || res.getName().trim().length() == 0) {
                Measurement dm = measurementManager.findMeasurementById(new Integer(cond.getMeasurementId()));

                if (dm == null) {
                	throw new AlertDefinitionCreateException(
                				"Could not automatically name the alert definition "
                				+ "because the AlertCondition (id=" + cond.getId() 
                				+ ") has an associated Measurement (id="
                				+ cond.getMeasurementId() + ") that does not exist.");
                }
                
                String predefinedAlertDefName = cond.describe(dm);
                
                if (predefinedAlertDefName == null 
                		|| predefinedAlertDefName.trim().length() == 0) {
                	throw new AlertDefinitionCreateException(
                				"Could not automatically name the alert definition "
                				+ "based on the alert condition.");
                }
                
                res.setName(predefinedAlertDefName);
            }

            if (cond.getType() == EventConstants.TYPE_ALERT) {
                setEnableAlertDefAction(a, cond.getMeasurementId());
            }

            alertConditionDAO.save(cond);
        }

        // Create actions
        ActionValue[] actions = a.getActions();

        for (ActionValue action : actions) {
            Action parent = null;

            if (action.getParentId() != null)
                parent = actionDao.findById(action.getParentId());

            Action act = res.createAction(action.getClassname(), action.getConfig(), parent);
            actionDao.save(act);
        }

        // Set triggers

        for (RegisteredTriggerValue trigger : a.getTriggers()) {
            RegisteredTrigger trig;
            // Triggers were already created by bizapp, so we only need
            // to add them to our list
            trig = registeredTriggerManager.findById(trigger.getId());
            trig.setAlertDefinition(res);
        }

        Integer esclId = a.getEscalationId();
        if (esclId != null) {
            Escalation escalation = escalationManager.findById(esclId);
            res.setEscalation(escalation);
        }
        // Alert definitions are the root of the cascade relationship, so
        // we must explicitly save them
        alertDefDao.save(res);
        
        availabilityDownAlertDefinitionCache.removeFromCache(res);

        return res.getAlertDefinitionValue();
    }

    /**
     * Update just the basics
     * @throws PermissionException
     * 
     */
    public void updateAlertDefinitionBasic(AuthzSubject subj, Integer id, String name, String desc, int priority,
                                           boolean activate) throws PermissionException {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        
        AlertDefinition def = alertDefDao.findById(id);
        // ...check that user has modify permission on alert definition's resource...
        alertPermissionManager.canModifyAlertDefinition(subj, def.getAppdefEntityId());
        int initCapacity = def.getChildren().size() + 1;
        List<AlertDefinition> alertdefs = new ArrayList<AlertDefinition>(initCapacity);
        List<Integer> defIds = new ArrayList<Integer>(initCapacity);
        alertdefs.add(def);
        
        if (debug) watch.markTimeBegin("getChildren");

        // If there are any children, add them, too
        alertdefs.addAll(def.getChildren());
        
        if (debug) {
            watch.markTimeEnd("getChildren");
            watch.markTimeBegin("updateBasic");
        }
        
        for (AlertDefinition child : alertdefs) {
            child.setName(name);
            child.setDescription(desc);
            child.setPriority(priority);

            if (child.isActive() != activate || child.isEnabled() != activate) {
                child.setActiveStatus(activate);
                alertAuditFactory.enableAlert(child, subj);
                defIds.add(def.getId());
            }
            child.setMtime(System.currentTimeMillis());

            availabilityDownAlertDefinitionCache.removeFromCache(child);
        }
        if (debug) {
            watch.markTimeEnd("updateBasic");
            watch.markTimeBegin("setAlertDefinitionTriggersEnabled");
        }
        
        registeredTriggerManager.setAlertDefinitionTriggersEnabled(defIds, activate);
        if (debug) {
            watch.markTimeEnd("setAlertDefinitionTriggersEnabled");
            log.debug("updateAlertDefinitionBasic[" + initCapacity + "]: " + watch);
        }
    }

    /**
     * Get the EnableAlertDefAction ActionValue from an AlertDefinitionValue. If
     * none exists, return null.
     */
    private ActionValue getEnableAlertDefAction(AlertDefinitionValue adv) {
        EnableAlertDefActionConfig cfg = new EnableAlertDefActionConfig();
        for (ActionValue action : adv.getActions()) {
            String actionClass = action.getClassname();
            if (cfg.getImplementor().equals(actionClass))
                return action;
        }
        return null;
    }
    
        private void setEnableAlertDefAction(AlertDefinitionValue adval, int recoverId) {
                EnableAlertDefActionConfig action =
                    new EnableAlertDefActionConfig();
        
                // Find recovery actions first
                ActionValue recoverAction = getEnableAlertDefAction(adval);
                
                if (recoverAction != null) {
                    try {
                        ConfigResponse configResponse =
                            ConfigResponse.decode(recoverAction.getConfig());
                        action.init(configResponse);
        
                        if (action.getAlertDefId() != recoverId) {
                            action.setAlertDefId(recoverId);
                            recoverAction.setConfig(action
                                                        .getConfigResponse()
                                                        .encode());
                            adval.updateAction(recoverAction);
                        }
                    } catch (Exception e) {
                        adval.removeAction(recoverAction);
                        recoverAction = null;
                    }
                }
        
                // Add action if doesn't exist
                if (recoverAction == null) {
                    recoverAction = new ActionValue();
                    action.setAlertDefId(recoverId);
                    recoverAction.setClassname(action.getImplementor());
        
                    try {
                        recoverAction
                        .setConfig(action.getConfigResponse().encode());
                    } catch (EncodingException e) {
                        log.debug("Error encoding EnableAlertDefAction", e);
                    } catch (InvalidOptionException e) {
                        log.debug("Error encoding EnableAlertDefAction", e);
                    } catch (InvalidOptionValueException e) {
                        log.debug("Error encoding EnableAlertDefAction", e);
                    }
        
                    adval.addAction(recoverAction);
                }
            }
            


    /**
     * Update an alert definition
     */
    public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval) throws AlertConditionCreateException,
        ActionCreateException {

        AlertDefinition aldef = alertDefDao.findById(adval.getId());

        // Create a measurement AlertLogAction if necessary
        setMetricAlertAction(adval);

        // Find recovery actions first
        int recoverId = -1;

        // See if the conditions changed
        if (adval.getAddedConditions().size() > 0 || adval.getUpdatedConditions().size() > 0 ||
            adval.getRemovedConditions().size() > 0) {
            // We need to keep old conditions around for the logs. So
            // we'll create new conditions and update the alert
            // definition, but we won't remove the old conditions.
            AlertConditionValue[] conds = adval.getConditions();
            aldef.clearConditions();
            for (AlertConditionValue condition : conds) {
                RegisteredTrigger trigger = null;

                // Trigger ID is null for resource type alerts
                if (condition.getTriggerId() != null)
                    trigger = registeredTriggerManager.findById(condition.getTriggerId());

                if (condition.getType() == EventConstants.TYPE_ALERT) {                   
                   recoverId = condition.getMeasurementId();
                }

                aldef.createCondition(condition, trigger);
            }
        }
        
        if (recoverId > 0) {
            setEnableAlertDefAction(adval, recoverId);
        } else {
            // Remove recover action if exists
            ActionValue recoverAction = getEnableAlertDefAction(adval);
            if (recoverAction != null) {
                adval.removeAction(recoverAction);
             }
        }

        // See if the actions changed
        if (adval.getAddedActions().size() > 0 || adval.getUpdatedActions().size() > 0 ||
            adval.getRemovedActions().size() > 0 ||   adval.getActions().length != aldef.getActions().size()) {
            // We need to keep old actions around for the logs. So
            // we'll create new actions and update the alert
            // definition, but we won't remove the old actions.
            ActionValue[] actions = adval.getActions();
            aldef.clearActions();
            for (ActionValue action : actions) {
                Action parent = null;

                if (action.getParentId() != null)
                    parent = actionDao.findById(action.getParentId());

                actionDao.save(aldef.createAction(action.getClassname(), action.getConfig(), parent));
            }
        }

        // Find out the last trigger ID (bizapp should have created them)
        RegisteredTriggerValue[] triggers = adval.getTriggers();
        for (int i = 0; i < triggers.length; i++) {
            RegisteredTrigger t = registeredTriggerManager.findById(triggers[i].getId());
            t.setAlertDefinition(aldef);
        }

        // Lastly, the modification time
        adval.setMtime(System.currentTimeMillis());

        // Now set the alertdef
        alertDefDao.setAlertDefinitionValueNoRels(aldef, adval);
        if (adval.isEscalationIdHasBeenSet()) {
            Integer esclId = adval.getEscalationId();
            Escalation escl = escalationManager.findById(esclId);

            aldef.setEscalation(escl);
        }

        // Alert definitions are the root of the cascade relationship, so
        // we must explicitly save them
        alertDefDao.save(aldef);

        availabilityDownAlertDefinitionCache.removeFromCache(aldef);

        return aldef.getAlertDefinitionValue();
    }

    /**
     * Activate/deactivate an alert definitions.
     * 
     */
    public void updateAlertDefinitionsActiveStatus(AuthzSubject subj, Integer[] ids, boolean activate)
        throws PermissionException {
        List<AlertDefinition> alertdefs = new ArrayList<AlertDefinition>();

        for (int i = 0; i < ids.length; i++) {
            alertdefs.add((alertDefDao.get(ids[i])));
        }

        for (AlertDefinition alertDef : alertdefs) {
            updateAlertDefinitionActiveStatus(subj, alertDef, activate);
        }
    }

    /**
     * Activate/deactivate an alert definition.
     */
    public void updateAlertDefinitionActiveStatus(AuthzSubject subj, AlertDefinition def, boolean activate)
        throws PermissionException {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        // ...check that user has modify permission on alert definition's resource...
        alertPermissionManager.canModifyAlertDefinition(subj, def.getAppdefEntityId());

        if (def.isActive() != activate || def.isEnabled() != activate) {
            def.setActiveStatus(activate);
            def.setMtime(System.currentTimeMillis());
            alertAuditFactory.enableAlert(def, subj);
            // process the children
            if (debug) watch.markTimeBegin("getChildren");
            Collection<AlertDefinition> children = def.getChildren();
            if (debug) watch.markTimeEnd("getChildren");
            List<Integer> defIds = new ArrayList<Integer>(children.size()+1);
            defIds.add(def.getId());
            for (AlertDefinition childDef : children) {
                defIds.add(childDef.getId());
            }
            if (debug) watch.markTimeBegin("setAlertDefinitionTriggersEnabled");
            registeredTriggerManager.setAlertDefinitionTriggersEnabled(defIds, activate);
            if (debug) watch.markTimeEnd("setAlertDefinitionTriggersEnabled");
        }

        alertDefDao.setChildrenActive(def, activate);

        availabilityDownAlertDefinitionCache.removeFromCache(def);
    }

    /**
     * Enable/Disable an alert definition. For internal use only where the mtime
     * does not need to be reset on each update.
     * 
     * @return <code>true</code> if the enable/disable succeeded.
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj, AlertDefinition def, boolean enable)
        throws PermissionException {

        boolean succeeded = false;

        if (def.isEnabled() != enable) {
            // ...check that user has modify permission on alert definition's resource...
            alertPermissionManager.canModifyAlertDefinition(subj, def.getAppdefEntityId());
            def.setEnabledStatus(enable);
            registeredTriggerManager.setAlertDefinitionTriggersEnabled(def.getId(), enable);
            succeeded = true;
        }

        return succeeded;
    }

    /**
     * Enable/Disable an alert definition. For internal use only where the mtime
     * does not need to be reset on each update.
     * 
     * @return <code>true</code> if the enable/disable succeeded.
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj, Integer defId, boolean enable)
        throws PermissionException {

        return updateAlertDefinitionInternalEnable(subj, Collections.singletonList(defId), enable);
    }
    
    /**
         * Enable/Disable an alert definition. For internal use only where the mtime
         * does not need to be reset on each update.
         *
         * @return <code>true</code> if the enable/disable succeeded.
         */
        public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj,
                                                           List<Integer> ids,
                                                           boolean enable)
            throws PermissionException {
     
          
            List<Integer> triggerDefIds = new ArrayList<Integer>(ids.size());
            
            for (Integer alertDefId : ids ) {
                AlertDefinition def = alertDefDao.get(alertDefId);
                
                if (def != null && def.isEnabled() != enable) {
                    // ...check that user has modify permission on alert definition's resource...
                    alertPermissionManager.canModifyAlertDefinition(subj, def.getAppdefEntityId());
                    def.setEnabledStatus(enable);
                    triggerDefIds.add(def.getId());
                }
            }
            
            if (!triggerDefIds.isEmpty()) {
                // HQ-1799: enable the triggers in batch to improve performance
                registeredTriggerManager.setAlertDefinitionTriggersEnabled(triggerDefIds, enable);
                return true;
            } else {
                return false;
            }
        }    


    /**
     * Set the escalation on the alert definition
     * 
     */
    public void setEscalation(AuthzSubject subj, Integer defId, Integer escId) throws PermissionException {
        AlertDefinition def = alertDefDao.findById(defId);
        // ...check that user has modify permission on alert definition's resource...
        alertPermissionManager.canModifyAlertDefinition(subj, def.getAppdefEntityId());

        Escalation esc = escalationManager.findById(escId);

        // End any escalation we were previously doing.
        escalationManager.endEscalation(def);

        def.setEscalation(esc);
        def.setMtime(System.currentTimeMillis());

        // End all children's escalation
        for (AlertDefinition child : def.getChildren()) {
            escalationManager.endEscalation(child);
        }

        alertDefDao.setChildrenEscalation(def, esc);
    }

    /**
     * Returns the {@link AlertDefinition}s using the passed escalation.
     */
    @Transactional(readOnly=true)
    public Collection<AlertDefinition> getUsing(Escalation e) {
        return alertDefDao.getUsing(e);
    }

    /**
     * Remove alert definitions
     */
    public void deleteAlertDefinitions(AuthzSubject subj, Integer[] ids) throws PermissionException {
        for (int i = 0; i < ids.length; i++) {
            AlertDefinition alertdef = alertDefDao.findById(ids[i]);

            // Don't delete child alert definitions
            if (alertdef.getParent() != null && !EventConstants.TYPE_ALERT_DEF_ID.equals(alertdef.getParent().getId())) {
                continue;
            }

            // ...check that user has delete permission on alert definitions...
            alertPermissionManager.canDeleteAlertDefinition(subj, alertdef.getAppdefEntityId());
            alertAuditFactory.deleteAlert(alertdef, subj);
            deleteAlertDefinition(subj, alertdef, false);
        }
    }

    /**
     * Set Resource to null on entity's alert definitions
     */
    private void disassociateResource(Resource r) {
        ResourceType resourceType = r.getResourceType();
        if ((null != resourceType) &&  AuthzConstants.authzPolicy.equals(resourceType.getId())) {
            // bug HQ-4644
            return;
        }
        List<AlertDefinition> adefs = alertDefDao.findAllByResource(r);

        for (AlertDefinition alertdef : adefs) {
            alertdef.setResource(null);
            alertdef.setDeleted(true);
        }
        alertDefDao.getSession().flush();
    }

    private void metricsEnabled(AppdefEntityID ent) {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Inheriting type-based alert defs for " + ent);
            }
            AuthzSubject hqadmin = authzSubjectManager.getSubjectById(AuthzConstants.rootSubjectId);
            inheritResourceTypeAlertDefinition(hqadmin, ent);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    private void inheritResourceTypeAlertDefinition(AuthzSubject subject, AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException, InvalidOptionException,
        InvalidOptionValueException, AlertDefinitionCreateException {

        AppdefEntityValue rv = new AppdefEntityValue(id, subject);
        AppdefResourceType type = rv.getAppdefResourceType();

        // Find the alert definitions for the type
        AppdefEntityTypeID aetid = new AppdefEntityTypeID(type.getAppdefType(), type.getId());

        // The alert definitions should be returned sorted by creation time.
        // This should minimize the possibility of creating a recovery alert
        // before the recover from alert.
        PageControl pc = new PageControl(0, PageControl.SIZE_UNLIMITED, PageControl.SORT_ASC,
            SortAttribute.CTIME);

        List<AlertDefinitionValue> defs = findAlertDefinitions(subject,
            aetid, pc);

        ArrayList<RegisteredTriggerValue> triggers = new ArrayList<RegisteredTriggerValue>();
        for (AlertDefinitionValue adval : defs) {
            // Only create if definition does not already exist
            if (isAlertDefined(id, adval.getId())) {
                continue;
            }

            // Set the parent ID
            adval.setParentId(adval.getId());

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getId());

            try {
                boolean succeeded = cloneParentConditions(subject, id, adval,
                    adval.getConditions(), true, false);

                if (!succeeded) {
                    continue;
                }
            } catch (MeasurementNotFoundException e) {
                throw new AlertDefinitionCreateException(
                    "Expected parent condition cloning to fail silently", e);
            }

            // Create the triggers
            registeredTriggerManager.createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));

            // Recreate the actions
            cloneParentActions(id, adval, adval.getActions());

            // Now create the alert definition
            createAlertDefinition(subject, adval);
        }
    }

    /**
     * Clone the parent actions into the alert definition.
     */
    public void cloneParentActions(AppdefEntityID parentId, AlertDefinitionValue child,
                                   ActionValue[] actions) {
        child.removeAllActions();
        for (int i = 0; i < actions.length; i++) {
            ActionValue childAct;
            try {
                ActionInterface actInst = (ActionInterface) Class
                    .forName(actions[i].getClassname()).newInstance();
                ConfigResponse config = ConfigResponse.decode(actions[i].getConfig());
                actInst.setParentActionConfig(parentId, config);
                childAct = new ActionValue(null, actInst.getImplementor(), actInst
                    .getConfigResponse().encode(), actions[i].getId());
            } catch (Exception e) {
                // Not a valid action, skip it then
                log.debug("Invalid action to clone: " + actions[i].getClassname(), e);
                continue;
            }
            child.addAction(childAct);
        }
    }
    
    /**
     * Clone the parent conditions into the alert definition.
     * 
     * @param subject The subject.
     * @param id The entity to which the alert definition is assigned.
     * @param adval The alert definition where the cloned conditions are set.
     * @param conds The parent conditions to clone.
     * @param failSilently <code>true</code> fail silently if cloning fails
     *        because no measurement is found corresponding to the measurement
     *        template specified in a parent condition; <code>false</code> to
     *        throw a {@link MeasurementNotFoundException} when this occurs.
     * @param allowStale True if we don't need to perform a flush to query for measurements
     * (this will be the case if we are not in the same transaction that measurements are created in)    
     * @return <code>true</code> if cloning succeeded; <code>false</code> if
     *         cloning failed.
     */
    public boolean cloneParentConditions(AuthzSubject subject, AppdefEntityID id,
                                         AlertDefinitionValue adval, AlertConditionValue[] conds,
                                         boolean failSilently, boolean allowStale) 
        throws MeasurementNotFoundException {
        
        // scrub and copy the parent's conditions
        adval.removeAllConditions();

        for (int i = 0; i < conds.length; i++) {
            AlertConditionValue clone = new AlertConditionValue(conds[i]);

            switch (clone.getType()) {
                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                case EventConstants.TYPE_CHANGE:
                    Integer tid = new Integer(clone.getMeasurementId());

                    // If allowStale is true, don't need to synch the Measurement with the db
                    // since changes to the Measurement aren't cascaded
                    // on saving the AlertCondition.
                    try {
                        Measurement dmv = measurementManager.findMeasurement(subject, tid, id
                            .getId(), allowStale);
                        clone.setMeasurementId(dmv.getId().intValue());
                    } catch (MeasurementNotFoundException e) {
                        log.error("No measurement found for entity " + id +
                                  " associated with template id=" + tid +
                                  ". Alert definition name [" + adval.getName() + "]");
                        log.debug("Root cause", e);

                        if (failSilently) {
                            log.info("Alert condition creation failed. " +
                                     "The alert definition for entity " + id + " with name [" +
                                     adval.getName() + "] should not be created.");
                            // Just set to 0, it'll never fire
                            clone.setMeasurementId(0);
                            return false;
                        } else {
                            throw e;
                        }
                    }

                    break;
                case EventConstants.TYPE_ALERT:

                    // Don't need to synch the child alert definition Id lookup.
                    Integer recoverId = findChildAlertDefinitionId(id,
                        new Integer(clone.getMeasurementId()), true);

                    if (recoverId == null) {
                        // recoverId should never be null, but if it is and
                        // assertions
                        // are disabled, just move on.
                        assert false : "recover Id should not be null.";

                        log.error("A recovery alert has no associated recover "
                                   + "from alert. Setting alert condition "
                                   + "measurement Id to 0.");
                        clone.setMeasurementId(0);
                    } else {
                        clone.setMeasurementId(recoverId.intValue());
                    }

                    break;
            }

            // Now add it to the alert definition
            adval.addCondition(clone);
        }

        return true;
    }
    
    @Transactional(readOnly=true)
    public List<Integer> getAllDeletedAlertDefs() {
        return alertDefDao.findAndPrefetchAllDeletedAlertDefs();
    }

    /**
     * Clean up alert definitions and alerts for removed resources
     * 
     */
    public void cleanupAlertDefs(List<Integer> alertDefIds) {
        if (alertDefIds.size() <= 0) {
            return;
        }
        StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        int i=0;
        try {
            final List<AlertDefinition> alertDefs = new ArrayList<AlertDefinition>(alertDefIds.size());
            for (Integer alertdefId : alertDefIds) {
                if (debug) watch.markTimeBegin("findById");
                final AlertDefinition alertdef = alertDefDao.findById(alertdefId);
                if (debug) watch.markTimeEnd("findById");
                alertDefs.add(alertdef);
            }
            if (debug) watch.markTimeBegin("deleteByAlertDefinition");
            alertDAO.deleteByAlertDefinitions(alertDefs);
            if(debug) watch.markTimeEnd("deleteByAlertDefinition");
            
            if (debug) watch.markTimeBegin("loop");
            for (AlertDefinition alertdef : alertDefs) {
                // Remove the conditions
                if(debug) watch.markTimeBegin("remove conditions and triggers");
                alertdef.clearConditions();
                alertdef.getTriggersBag().clear();
                if(debug) watch.markTimeEnd("remove conditions and triggers");
    
                // Remove the actions
                if (debug) watch.markTimeBegin("removeActions");
                actionDao.removeActions(alertdef);
                if(debug) watch.markTimeEnd("removeActions");
    
                if(debug) watch.markTimeBegin("remove from parent");
                if (alertdef.getParent() != null) {
                    alertdef.getParent().getChildrenBag().remove(alertdef);
                }
                if(debug) watch.markTimeBegin("remove from parent");
    
                // Actually remove the definition
                if(debug) watch.markTimeBegin("remove");
                alertDefDao.remove(alertdef);
                if(debug) watch.markTimeBegin("remove");
                i++;
            }
            if (debug) watch.markTimeEnd("loop");
        } finally {
            if (debug) log.debug("deleted " + i + " alertDefs: " + watch);
        }
    }

    /**
     * Find an alert definition and return a value object
     * @throws PermissionException if user does not have permission to manage
     *         alerts
     */
    @Transactional(readOnly=true)
    public AlertDefinitionValue getById(AuthzSubject subj, Integer id) throws PermissionException {
        AlertDefinitionValue adv = null;
        AlertDefinition ad = getByIdAndCheck(subj, id);
        if (ad != null) {
            adv = ad.getAlertDefinitionValue();
        }
        return adv;
    }

    /**
     * Find an alert definition
     * @throws PermissionException if user does not have permission to manage
     *         alerts
     */
    @Transactional(readOnly=true)
    public AlertDefinition getByIdAndCheck(AuthzSubject subj, Integer id) throws PermissionException {
        AlertDefinition ad = alertDefDao.get(id);
        if (ad != null) {
            if (ad.isDeleted()) {
                ad = null;
            } else {
                Resource r = ad.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    ad = null;
                }
            }

            if (ad != null) {
                // ...check that user has view permission on alert definitions...
                alertPermissionManager.canViewAlertDefinition(subj, ad.getAppdefEntityId());
            }
        }
        return ad;
    }

    /**
     * Find an alert definition and return a basic value. This is called by the
     * abstract trigger, so it does no permission checking.
     * 
     * @param id The alert def Id.
     */
    @Transactional(readOnly=true)
    public AlertDefinition getByIdNoCheck(Integer id) {
        return alertDefDao.get(id);
    }

    /**
     * Check if an alert definition is a resource type alert definition.
     * 
     * @param id The alert def Id.
     * @return <code>true</code> if the alert definition is a resource type
     *         alert definition.
     * 
     */
    @Transactional(readOnly=true)
    public boolean isResourceTypeAlertDefinition(Integer id) {
        AlertDefinition ad = alertDefDao.get(id);
        return ad.isResourceTypeDefinition();
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public AlertDefinition findAlertDefinitionById(Integer id) {
        return alertDefDao.findById(id);
    }

    /**
     * Get an alert definition's name
     * 
     */
    @Transactional(readOnly=true)
    public String getNameById(Integer id) {
        return alertDefDao.get(id).getName();
    }

    /**
     * Get an alert definition's conditions
     * 
     */
    @Transactional(readOnly=true)
    public AlertConditionValue[] getConditionsById(Integer id) {
        AlertDefinition def = alertDefDao.get(id);
        Collection<AlertCondition> conds = def.getConditions();
        AlertConditionValue[] condVals = new AlertConditionValue[conds.size()];
        int i = 0;
        for (AlertCondition cond : conds) {
            condVals[i] = cond.getAlertConditionValue();
            i++;
        }
        return condVals;
    }

    /**
     * Get list of alert conditions for a resource or resource type
     * 
     */
    @Transactional(readOnly=true)
    public boolean isAlertDefined(AppdefEntityID id, Integer parentId) {
        Resource res = resourceManager.findResource(id);
        return alertDefDao.findChildAlertDef(res, parentId) != null;
    }

    /**
     * Get list of all alert conditions
     * 
     * @return a PageList of {@link AlertDefinitionValue} objects
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findAllAlertDefinitions(AuthzSubject subj) {
        List<AlertDefinitionValue> vals = new ArrayList<AlertDefinitionValue>();

        for (AlertDefinition a : alertDefDao.findAll()) {
            try {
                // Only return the alert definitions that user can see
                // ...check that user has view permission on alert definitions...
                alertPermissionManager.canViewAlertDefinition(subj, a.getAppdefEntityId());
            } catch (PermissionException e) {
                continue;
            }
            vals.add(a.getAlertDefinitionValue());
        }
        return new PageList<AlertDefinitionValue>(vals, vals.size());
    }

    /**
     * Get the resource-specific alert definition ID by parent ID, allowing for
     * the query to return a stale copy of the alert definition (for efficiency
     * reasons).
     * 
     * @param aeid The resource.
     * @param pid The ID of the resource type alert definition (parent ID).
     * @param allowStale <code>true</code> to allow stale copies of an alert
     *        definition in the query results; <code>false</code> to never allow
     *        stale copies, potentially always forcing a sync with the database.
     * @return The alert definition ID or <code>null</code> if no alert
     *         definition is found for the resource.
     * 
     */
    @Transactional(readOnly=true)
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid, Integer pid, boolean allowStale) {
        Resource res = resourceManager.findResource(aeid);
        AlertDefinition def = alertDefDao.findChildAlertDef(res, pid, true);

        return def == null ? null : def.getId();
    }

    /**
     * Find alert definitions passing the criteria.
     * 
     * @param minSeverity Specifies the minimum severity that the defs should be
     *        set for
     * @param enabled If non-null, specifies the nature of the returned
     *        definitions (i.e. only return enabled or disabled defs)
     * @param excludeTypeBased If true, exclude any alert definitions associated
     *        with a type-based def.
     * @param pInfo Paging information. The sort field must be a value from
     *        {@link AlertDefSortField}
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subj, AlertSeverity minSeverity, Boolean enabled,
                                                      boolean excludeTypeBased, PageInfo pInfo) {
        return alertDefDao.findDefinitions(subj, minSeverity, enabled, excludeTypeBased, pInfo);
    }

    /**
     * Get the list of type-based alert definitions.
     * 
     * @param enabled If non-null, specifies the nature of the returned defs.
     * @param pInfo Paging information. The sort field must be a value from
     *        {@link AlertDefSortField}
     * 
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findTypeBasedDefinitions(AuthzSubject subj, Boolean enabled, PageInfo pInfo)
        throws PermissionException {
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(subj.getId())) {
            throw new PermissionException("Only administrators can do this");
        }
        return alertDefDao.findTypeBased(enabled, pInfo);
    }

    /**
     * Get list of alert definition POJOs for a resource
     * @throws PermissionException if user cannot manage alerts for resource
     * 
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        // ...check that user has view permission on alert definitions...
        alertPermissionManager.canViewAlertDefinition(subject, id);
        return alertDefDao.findByResource(resourceManager.findResource(id));
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findAlertDefinitions(AuthzSubject subj, AppdefEntityID id, PageControl pc)
        throws PermissionException {
        // ...check that user has view permission on alert definitions...
        alertPermissionManager.canViewAlertDefinition(subj, id);
        Resource res = resourceManager.findResource(id);

        List<AlertDefinition> adefs;
        if (pc.getSortattribute() == SortAttribute.CTIME) {
            adefs = alertDefDao.findByResourceSortByCtime(res, !pc.isDescending());
        } else {
            adefs = alertDefDao.findByResource(res, !pc.isDescending());
        }
        // TODO:G
        return _valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get list of alert definitions for a resource type.
     * 
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subject, Resource prototype)
        throws PermissionException {
        // TODO: Check admin permission?
        return alertDefDao.findByResource(prototype);
    }

    /**
     * Get list of alert conditions for a resource or resource type
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findAlertDefinitions(AuthzSubject subj, AppdefEntityTypeID aetid,
                                                               PageControl pc) throws PermissionException {
        Resource res = resourceManager.findResourcePrototype(aetid);
        Collection<AlertDefinition> adefs;
        if (pc.getSortattribute() == SortAttribute.CTIME) {
            adefs = alertDefDao.findByResourceSortByCtime(res, pc.isAscending());
        } else {
            adefs = alertDefDao.findByResource(res, pc.isAscending());
        }
        // TODO:G
        return _valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get a list of all alert definitions for the resource and its descendents
     * @param subj the caller
     * @param res the root resource
     * @return a list of alert definitions
     * 
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findRelatedAlertDefinitions(AuthzSubject subj, Resource res) {
        return alertDefDao.findByRootResource(subj, res);
    }

    /**
     * Get a list of all alert definitions with an availability metric condition
     * @param subj the caller
     * @return a list of alert definitions
     */
    @Transactional(readOnly=true)
    public List<AlertDefinition> findAvailAlertDefinitions(AuthzSubject subj) 
    	throws PermissionException {
    	
        if (!PermissionManagerFactory.getInstance()
                .hasAdminPermission(subj.getId())) {
            throw new PermissionException("Only administrators can do this");
        }
        
        return alertDefDao.findAvailAlertDefs();
    }
    
    /**
     * Get list of children alert definition for a parent alert definition
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findAlertDefinitionChildren(Integer id) {
        AlertDefinition def = alertDefDao.findById(id);

        PageControl pc = PageControl.PAGE_ALL;
        // TODO:G
        return _valuePager.seek(def.getChildren(), pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get list of alert definition names for a resource
     * 
     */
    @Transactional(readOnly=true)
    public SortedMap<String, Integer> findAlertDefinitionNames(AuthzSubject subj, AppdefEntityID id, Integer parentId)
        throws PermissionException {
        if (parentId == null) {
            // ...check that user has view permission on alert definitions...
            alertPermissionManager.canViewAlertDefinition(subj, id);
        }
        return findAlertDefinitionNames(id, parentId);
    }

    /**
     * Get list of alert definition names for a resource
     * 
     */
    @Transactional(readOnly=true)
    public SortedMap<String, Integer> findAlertDefinitionNames(AppdefEntityID id, Integer parentId) {
        AlertDefinitionDAO aDao = alertDefDao;
        TreeMap<String, Integer> ret = new TreeMap<String, Integer>();
        Collection<AlertDefinition> adefs;

        if (parentId != null) {
            if (EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) {
                AppdefEntityTypeID aetid = new AppdefEntityTypeID(id.getType(), id.getId());
                Resource res = resourceManager.findResourcePrototype(aetid);
                adefs = aDao.findByResource(res);
            } else {
                AlertDefinition def = alertDefDao.findById(parentId);
                adefs = def.getChildren();
            }
        } else {
            Resource res = resourceManager.findResource(id);
            adefs = aDao.findByResource(res);
        }

        // Use name as key so that map is sorted
        for (AlertDefinition adLocal : adefs) {
            ret.put(adLocal.getName(), adLocal.getId());
        }
        return ret;
    }

    /**
     * Return array of two values: enabled and act on trigger ID
     * 
     */
    @Transactional(readOnly=true)
    public boolean isEnabled(Integer id) {
        return alertDefDao.isEnabled(id);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public int getActiveCount() {
        return alertDefDao.getNumActiveDefs();
    }
}

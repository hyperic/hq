/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C)
 * [2004-2009], Hyperic, Inc. This file is part of HQ. HQ is free software; you
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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.action.EnableAlertDefActionConfig;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.ActionCreateException;
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
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class AlertDefinitionManagerImpl implements AlertDefinitionManager, ApplicationContextAware {
    private Log log = LogFactory.getLog(AlertDefinitionManagerImpl.class);

    private AlertPermissionManager alertPermissionManager;

    private final String VALUE_PROCESSOR = PagerProcessor_events.class.getName();

    private Pager _valuePager = null;

    private AlertDefinitionDAO alertDefDao;

    private ActionDAO actionDao;

    private AlertConditionDAO alertConditionDAO;

    private MeasurementDAO measurementDAO;

    private RegisteredTriggerManager registeredTriggerManager;

    private ResourceManager resourceManager;

    private EscalationManager escalationManager;

    private AlertAuditFactory alertAuditFactory;
    
    private ApplicationContext applicationContext;

    @Autowired
    public AlertDefinitionManagerImpl(AlertPermissionManager alertPermissionManager, AlertDefinitionDAO alertDefDao,
                                      ActionDAO actionDao, AlertConditionDAO alertConditionDAO, 
                                      MeasurementDAO measurementDAO, RegisteredTriggerManager registeredTriggerManager,
                                      ResourceManager resourceManager, EscalationManager escalationManager,
                                      AlertAuditFactory alertAuditFactory) {
        this.alertPermissionManager = alertPermissionManager;
        this.alertDefDao = alertDefDao;
        this.actionDao = actionDao;
        this.alertConditionDAO = alertConditionDAO;
        this.measurementDAO = measurementDAO;
        this.registeredTriggerManager = registeredTriggerManager;
        this.resourceManager = resourceManager;
        this.escalationManager = escalationManager;
        this.alertAuditFactory = alertAuditFactory;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        _valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

    private boolean deleteAlertDefinitionStuff(AuthzSubject subj, AlertDefinition alertdef, EscalationManager escMan) {
        StopWatch watch = new StopWatch();

        // Delete escalation state
        watch.markTimeBegin("endEscalation");
        if (alertdef.getEscalation() != null && !alertdef.isResourceTypeDefinition()) {
            escMan.endEscalation(alertdef);
        }
        watch.markTimeEnd("endEscalation");

        applicationContext.publishEvent(new AlertDefinitionDeletedEvent(alertdef));

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
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AuthzSubject subj, AlertDefinitionValue a)
        throws AlertDefinitionCreateException, PermissionException {
        if (EventConstants.TYPE_ALERT_DEF_ID.equals(a.getParentId())) {
            alertPermissionManager.canManageAlerts(subj, new AppdefEntityTypeID(a.getAppdefType(), a.getAppdefId()));
            // Subject permissions should have already been checked when
            // creating
            // the parent (resource type) alert definition.
        } else if (!a.parentIdHasBeenSet()) {
            alertPermissionManager.canManageAlerts(subj, new AppdefEntityID(a.getAppdefType(), a.getAppdefId()));
        }
        return createAlertDefinition(a);
    }

    /**
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AlertDefinitionValue a) {

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

            if (res.getName() == null || res.getName().length() == 0) {
                Measurement dm = null;
                if (cond.getType() == EventConstants.TYPE_THRESHOLD || cond.getType() == EventConstants.TYPE_BASELINE) {

                    dm = measurementDAO.findById(new Integer(cond.getMeasurementId()));
                }
                if (dm == null) {
                    log.warn("AlertCondition (id=" + cond.getId() + ") has an " + "associated Measurement (id=" +
                             cond.getMeasurementId() + ") that does not exist, ignoring");
                    continue;
                }
                res.setName(cond.describe(dm));
            }

            if (cond.getType() == EventConstants.TYPE_ALERT) {
                ActionValue recoverAction = new ActionValue();

                EnableAlertDefActionConfig action = new EnableAlertDefActionConfig();
                action.setAlertDefId(cond.getMeasurementId());

                recoverAction.setClassname(action.getImplementor());
                try {
                    recoverAction.setConfig(action.getConfigResponse().encode());
                } catch (EncodingException e) {
                    log.debug("Error encoding EnableAlertDefAction", e);
                } catch (InvalidOptionException e) {
                    log.debug("Error encoding EnableAlertDefAction", e);
                } catch (InvalidOptionValueException e) {
                    log.debug("Error encoding EnableAlertDefAction", e);
                }

                a.addAction(recoverAction);
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

        applicationContext.publishEvent(new AlertDefinitionCreatedEvent(res));

        return res.getAlertDefinitionValue();
    }

    /**
     * Update just the basics
     * @throws PermissionException
     * 
     */
    public void updateAlertDefinitionBasic(AuthzSubject subj, Integer id, String name, String desc, int priority,
                                           boolean activate) throws PermissionException {
        AlertDefinition def = alertDefDao.findById(id);
        alertPermissionManager.canManageAlerts(subj, def);
        List<AlertDefinition> alertdefs = new ArrayList<AlertDefinition>(def.getChildren().size() + 1);
        alertdefs.add(def);

        // If there are any children, add them, too
        alertdefs.addAll(def.getChildren());
        for (AlertDefinition child : alertdefs) {
            child.setName(name);
            child.setDescription(desc);
            child.setPriority(priority);

            if (child.isActive() != activate || child.isEnabled() != activate) {
                child.setActiveStatus(activate);
                alertAuditFactory.enableAlert(child, subj);
                registeredTriggerManager.setAlertDefinitionTriggersEnabled(child.getId(), activate);
            }
            child.setMtime(System.currentTimeMillis());

            applicationContext.publishEvent(new AlertDefinitionChangedEvent(child));
        }
    }

    /**
     * Get the EnableAlertDefAction ActionValue from an AlertDefinitionValue. If
     * none exists, return null.
     */
    private ActionValue getEnableAction(AlertDefinitionValue adv) {
        EnableAlertDefActionConfig cfg = new EnableAlertDefActionConfig();
        for (ActionValue action : adv.getActions()) {
            String actionClass = action.getClassname();
            if (cfg.getImplementor().equals(actionClass))
                return action;
        }
        return null;
    }

    /**
     * Update an alert definition
     */
    public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval) throws AlertConditionCreateException,
        ActionCreateException {

        AlertDefinition aldef = alertDefDao.findById(adval.getId());

        // Find recovery actions first
        ActionValue recoverAction = getEnableAction(adval);
        adval.removeAction(recoverAction);

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
                    EnableAlertDefActionConfig action = new EnableAlertDefActionConfig();
                    if (recoverAction != null) {
                        try {
                            ConfigResponse configResponse = ConfigResponse.decode(recoverAction.getConfig());
                            action.init(configResponse);

                            if (action.getAlertDefId() != condition.getMeasurementId()) {
                                action.setAlertDefId(condition.getMeasurementId());
                                recoverAction.setConfig(action.getConfigResponse().encode());
                                adval.updateAction(recoverAction);
                            }
                        } catch (Exception e) {
                            recoverAction = null;
                        }
                    }

                    // Add action if doesn't exist
                    if (recoverAction == null) {
                        recoverAction = new ActionValue();
                        action.setAlertDefId(condition.getMeasurementId());
                        recoverAction.setClassname(action.getImplementor());

                        try {
                            recoverAction.setConfig(action.getConfigResponse().encode());
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

                aldef.createCondition(condition, trigger);
            }
        }

        // See if the actions changed
        if (adval.getAddedActions().size() > 0 || adval.getUpdatedActions().size() > 0 ||
            adval.getRemovedActions().size() > 0) {
            // We need to keep old actions around for the logs. So
            // we'll create new actions and update the alert
            // definition, but we won't remove the old conditions.
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

        applicationContext.publishEvent(new AlertDefinitionChangedEvent(aldef));

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

        alertPermissionManager.canManageAlerts(subj, def);

        if (def.isActive() != activate || def.isEnabled() != activate) {
            def.setActiveStatus(activate);
            def.setMtime(System.currentTimeMillis());
            alertAuditFactory.enableAlert(def, subj);
            registeredTriggerManager.setAlertDefinitionTriggersEnabled(def.getId(), activate);
        }

        alertDefDao.setChildrenActive(def, activate);

        applicationContext.publishEvent(new AlertDefinitionChangedEvent(def));
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
            alertPermissionManager.canManageAlerts(subj, def.getAppdefEntityId());
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

        AlertDefinition def = alertDefDao.get(defId);

        return updateAlertDefinitionInternalEnable(subj, def, enable);
    }

    /**
     * Set the escalation on the alert definition
     * 
     */
    public void setEscalation(AuthzSubject subj, Integer defId, Integer escId) throws PermissionException {
        AlertDefinition def = alertDefDao.findById(defId);
        alertPermissionManager.canManageAlerts(subj, def);

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

            alertPermissionManager.canManageAlerts(subj, alertdef);
            alertAuditFactory.deleteAlert(alertdef, subj);
            deleteAlertDefinition(subj, alertdef, false);
        }
    }

    /**
     * Set Resource to null on entity's alert definitions
     */
    public void disassociateResource(Resource r) {
        List<AlertDefinition> adefs = alertDefDao.findAllByResource(r);

        for (AlertDefinition alertdef : adefs) {
            alertdef.setResource(null);
            alertdef.setDeleted(true);
        }
        alertDefDao.getSession().flush();
    }

    /**
     * Clean up alert definitions and alerts for removed resources
     * 
     */
    public void cleanupAlertDefinitions(AppdefEntityID aeid) {
        StopWatch watch = new StopWatch();

        List<AlertDefinition> adefs = alertDefDao.findAllDeletedResources();
        for (AlertDefinition alertdef : adefs) {
            // Delete the alerts
            watch.markTimeBegin("deleteByAlertDefinition");
            alertDefDao.deleteByAlertDefinition(alertdef);
            watch.markTimeEnd("deleteByAlertDefinition");

            // Get the alerts deleted
            alertDefDao.getSession().flush();

            // Remove the conditions
            watch.markTimeBegin("remove conditions and triggers");
            alertdef.clearConditions();
            alertdef.getTriggersBag().clear();
            watch.markTimeEnd("remove conditions and triggers");

            // Remove the actions
            watch.markTimeBegin("removeActions");
            actionDao.removeActions(alertdef);
            watch.markTimeEnd("removeActions");

            watch.markTimeBegin("remove from parent");
            if (alertdef.getParent() != null) {
                alertdef.getParent().getChildrenBag().remove(alertdef);
            }
            watch.markTimeBegin("remove from parent");

            // Actually remove the definition
            watch.markTimeBegin("remove");
            alertDefDao.remove(alertdef);
            watch.markTimeBegin("remove");

            if (log.isDebugEnabled()) {
                log.debug("deleteAlertDefinition: " + watch);
            }
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
                alertPermissionManager.canManageAlerts(subj, alertPermissionManager.getAppdefEntityID(ad));
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
                alertPermissionManager.canManageAlerts(subj, alertPermissionManager.getAppdefEntityID(a));
            } catch (PermissionException e) {
                continue;
            }
            vals.add(a.getAlertDefinitionValue());
        }
        return new PageList<AlertDefinitionValue>(vals, vals.size());
    }

    /**
     * Get list of all child conditions
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findChildAlertDefinitions(Integer id) {
        AlertDefinition def = alertDefDao.findById(id);
        List<AlertDefinitionValue> vals = new ArrayList<AlertDefinitionValue>();
        Collection<AlertDefinition> ads = def.getChildren();
        for (AlertDefinition child : ads) {
            // Don't touch the deleted children
            if (!child.isDeleted() && child.getResource() != null) {
                vals.add(child.getAlertDefinitionValue());
            }
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
        alertPermissionManager.canManageAlerts(subject, id);
        Resource res = resourceManager.findResource(id);
        return alertDefDao.findByResource(res);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AlertDefinitionValue> findAlertDefinitions(AuthzSubject subj, AppdefEntityID id, PageControl pc)
        throws PermissionException {
        alertPermissionManager.canManageAlerts(subj, id);
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
        return alertDefDao.findAllByResource(prototype);
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
            alertPermissionManager.canManageAlerts(subj, id);
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }
}

/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004, 2005, 2006],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisterableTriggerRepository;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The trigger manager.
 * 
 */
@Service
@Transactional
public class RegisteredTriggerManagerImpl implements RegisteredTriggerManager {

    private final Log log = LogFactory.getLog(RegisteredTriggerManagerImpl.class);

    private AlertConditionEvaluatorFactory alertConditionEvaluatorFactory;

    private RegisterableTriggerRepository registeredTriggerRepository;

    private TriggerDAOInterface triggerDAO;

    private ZeventEnqueuer zeventEnqueuer;

    private AlertConditionEvaluatorRepository alertConditionEvaluatorRepository;

    private AlertDefinitionDAOInterface alertDefinitionDAO;

    @Autowired
    public RegisteredTriggerManagerImpl(AlertConditionEvaluatorFactory alertConditionEvaluatorFactory,
                                        TriggerDAOInterface triggerDAO, ZeventEnqueuer zeventEnqueuer,
                                        AlertConditionEvaluatorRepository alertConditionEvaluatorRepository,
                                        AlertDefinitionDAOInterface alertDefinitionDAO) {
        this.alertConditionEvaluatorFactory = alertConditionEvaluatorFactory;
        this.triggerDAO = triggerDAO;
        this.zeventEnqueuer = zeventEnqueuer;
        this.alertConditionEvaluatorRepository = alertConditionEvaluatorRepository;
        this.alertDefinitionDAO = alertDefinitionDAO;
    }

    /**
     * Processes {@link TriggerCreatedEvent}s that indicate that triggers should
     * be created
     * 
     */
    public void handleTriggerCreatedEvents(Collection<TriggersCreatedZevent> events) {
        for (TriggersCreatedZevent z : events) {
            Integer alertDefId = ((TriggersCreatedZeventSource) z.getSourceId()).getId();
            registerTriggers(alertDefId);
        }
    }

    /**
     * Initialize the in-memory triggers and update the RegisteredTriggers
     * repository
     * 
     * 
     */
    @Transactional(readOnly=true)
    public void initializeTriggers(RegisterableTriggerRepository registeredTriggerRepository) {
        log.debug("Initializing triggers");
        long startTime = System.currentTimeMillis();
        this.registeredTriggerRepository = registeredTriggerRepository;
        // Only initialize the enabled triggers. If disabled ones become enabled
        // we will lazy init
        log.debug("Fetching enabled triggers");
        Collection<RegisteredTrigger> registeredTriggers = triggerDAO.findAllEnabledTriggers();
        if (log.isDebugEnabled()) {
            log.debug("Found " + registeredTriggers.size() + " triggers");
        }
        initializeTriggers(registeredTriggers);
        if (log.isInfoEnabled()) {
            log.info("Finished initializing triggers in " + (System.currentTimeMillis() - startTime) + " ms.");
        }
    }

    private void registerTriggers(Integer alertDefId) {
        if (this.registeredTriggerRepository == null) {
            // The repository hasn't been initialized yet. Triggers will be
            // created when that occurs.
            return;
        }
        Collection<RegisteredTrigger> registeredTriggers = getAllTriggersByAlertDefId(alertDefId);
        if (!(registeredTriggers.isEmpty())) {
            AlertDefinition alertDefinition = getDefinitionFromTrigger((RegisteredTrigger) registeredTriggers
                .iterator().next());
            if (alertDefinition == null) {
                log.warn("Unable to find AlertDefinition with id: " + alertDefId + ".  These alerts will not fire.");
                return;
            }
            AlertConditionEvaluator alertConditionEvaluator = alertConditionEvaluatorFactory.create(alertDefinition);
            alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
            for (RegisteredTrigger tv : registeredTriggers) {
                // Try to register each trigger, if exception, then move on
                try {
                    registerTrigger(tv.getRegisteredTriggerValue(), alertConditionEvaluator, alertDefinition
                        .isEnabled());
                } catch (Exception e) {
                    log.error("Error registering trigger", e);
                }
            }
        }
    }

    private void initializeTriggers(Collection<RegisteredTrigger> registeredTriggers) {
        initializeAlertConditionEvaluators(registeredTriggers);
        for (RegisteredTrigger tv : registeredTriggers) {
            // Try to register each trigger, if exception, then move on
            try {
                AlertDefinition def = getDefinitionFromTrigger(tv);

                if (def != null) {
                    AlertConditionEvaluator evaluator = (AlertConditionEvaluator) alertConditionEvaluatorRepository
                        .getAlertConditionEvaluatorById(def.getId());

                    if (evaluator != null) {
                        registerTrigger(tv.getRegisteredTriggerValue(), evaluator, def.isEnabled());
                    }
                }
            } catch (Exception e) {
                log.error("Error registering trigger", e);
            }
        }
    }

    private void initializeAlertConditionEvaluators(Collection<RegisteredTrigger> registeredTriggers) {
        // Create AlertConditionEvaluator for each AlertDefinition, so they can
        // be shared by all triggers associated with the alertDef
        for (RegisteredTrigger tv : registeredTriggers) {
            try {
                AlertDefinition def = getDefinitionFromTrigger(tv);
                if (def == null) {
                    log.warn("Unable to find AlertDefinition for trigger with id " + tv.getId() +
                             ".  These alerts will not fire.");
                } else if (alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(def.getId()) == null) {
                    alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluatorFactory
                        .create(def));
                }
            } catch (Exception e) {
                log.error("Error retrieving alert definition for trigger", e);
            }
        }
    }

    /**
     * Register a trigger. NOTE: This method should be called within an active
     * Hibernate session/transaction. Trigger init method may interact with a DB
     * @param tv The trigger to register
     * @param alertDef The trigger's corresponding alert definition
     * @param enableTrigger True if trigger should be enabled
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvalidTriggerDataException
     */
    void registerTrigger(RegisteredTriggerValue tv, AlertConditionEvaluator alertConditionEvaluator,
                         boolean enableTrigger) throws InstantiationException, IllegalAccessException,
        InvalidTriggerDataException {
        Class<?> tc;
        try {
            tc = Class.forName(tv.getClassname());
        } catch (ClassNotFoundException e) {
            log.warn("Trigger class " + tv.getClassname() +
                     " is not supported.  Triggers of this type should be removed from the database.");
            return;
        }
        RegisterableTriggerInterface trigger = (RegisterableTriggerInterface) tc.newInstance();
        trigger.init(tv, alertConditionEvaluator);
        trigger.setEnabled(enableTrigger);
        this.registeredTriggerRepository.addTrigger(trigger);
    }

    /**
     * Enable or disable triggers associated with an alert definition
     * 
     * 
     */
    public void setAlertDefinitionTriggersEnabled(Integer alertDefId, boolean enabled) {
        Collection<Integer> triggerIds = getTriggerIdsByAlertDefId(alertDefId);
        addPostCommitSetEnabledListener(alertDefId, triggerIds, enabled);
    }

    private void addPostCommitSetEnabledListener(final Integer alertDefId, final Collection<Integer> triggerIds,
                                                 final boolean enabled) {
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                public void suspend() {
                }
                
                public void resume() {
                }
                
                public void flush() {
                }
                
                public void beforeCompletion() {
                }
                
                public void beforeCommit(boolean readOnly) {
                }
                
                public void afterCompletion(int status) {
                }
                
                public void afterCommit() {
                    try {
                        setTriggersEnabled(alertDefId, triggerIds, enabled);
                    } catch (Exception e) {
                        log.error("Error setting triggers enabled to " + enabled, e);
                    }
                }
            });
        } catch (Throwable t) {
            log.error("Error registering to set triggers enabled to " + enabled, t);
        }
    }

    void setTriggersEnabled(final Integer alertDefinitionId, final Collection<Integer> triggerIds, final boolean enabled) {
        if (this.registeredTriggerRepository != null) {
            if (enabled == true && !(triggerIds.isEmpty())) {
                Integer triggerId = (Integer) triggerIds.iterator().next();
                if (registeredTriggerRepository.getTriggerById(triggerId) == null) {
                    try {
                        zeventEnqueuer.enqueueEvent(new TriggersCreatedZevent(alertDefinitionId));
                    } catch (InterruptedException e) {
                        log.error("Error sending event to create newly enabled triggers for alert definition " +
                                  alertDefinitionId + ". These alerts will not fire", e);
                    }
                    return;
                }
            }
            registeredTriggerRepository.setTriggersEnabled(triggerIds, enabled);
        }
    }

    Collection<Integer> getTriggerIdsByAlertDefId(Integer id) {
        Collection<RegisteredTrigger> triggers = getAllTriggersByAlertDefId(id);
        List<Integer> triggerIds = new ArrayList<Integer>();
        for (RegisteredTrigger trigger : triggers) {
            triggerIds.add(trigger.getId());
        }
        return triggerIds;
    }

    /**
     * Finds a trigger by its ID, assuming existence
     * @param id The trigger ID
     * @return The trigger with the specified ID (exception will occur if
     *         trigger does not exist)
     * 
     * 
     */
    @Transactional(readOnly=true)
    public RegisteredTrigger findById(Integer id) {
        return triggerDAO.findById(id);
    }

    void unregisterTriggers(Integer alertDefinitionId, Collection<RegisteredTrigger> triggers) {
        // No point unregistering if repository has not been intialized
        if (this.registeredTriggerRepository != null) {
            for (RegisteredTrigger trigger : triggers) {
                this.registeredTriggerRepository.removeTrigger(trigger.getId());
            }
            alertConditionEvaluatorRepository.removeAlertConditionEvaluator(alertDefinitionId);
        }
    }

    private AlertDefinition getDefinitionFromTrigger(RegisteredTrigger trigger) {
        AlertDefinition def = trigger.getAlertDefinition();
        if (def != null && !def.isDeleted()) {
            return def;
        } else {
            return null;
        }
    }

    /**
     * Get the registered trigger objects associated with a given alert
     * definition.
     * 
     * @param id The alert def id.
     * @return The registered trigger objects.
     */
    private Collection<RegisteredTrigger> getAllTriggersByAlertDefId(Integer id) {
        return triggerDAO.findByAlertDefinitionId(id);
    }

    /**
     * Create a new trigger
     * 
     * @return a RegisteredTriggerValue
     * 
     * 
     */
    public RegisteredTrigger createTrigger(RegisteredTriggerValue val) {
        // XXX -- Things here aren't symmetrical. The EventsBoss is currently
        // registering the trigger with the dispatcher, and updateTrigger()
        // is updating it with the dispatcher. Seems like this should all
        // be done here in the manager
        return triggerDAO.create(val); // DAO method will set ID on val obj
    }

    /**
     * Create new triggers
     * 
     * @return a RegisteredTriggerValue
     * 
     * 
     */
    public void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef) throws TriggerCreateException,
        InvalidOptionException, InvalidOptionValueException {
        final List<RegisteredTrigger> triggers = new ArrayList<RegisteredTrigger>();

        // Create AppdefEntityID from the alert definition
        AppdefEntityID id = new AppdefEntityID(alertdef.getAppdefType(), alertdef.getAppdefId());

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

        for (RegisteredTrigger tval : triggers) {
            alertdef.addTrigger(tval.getRegisteredTriggerValue());
        }
        addCommitListener(triggers);
    }

    private void addCommitListener(final Collection<RegisteredTrigger> triggers) {
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {           
                public void suspend() {
                }
                
                public void resume() {
                }
                
                public void flush() {
                }
                
                public void beforeCompletion() {
                }
                
                public void beforeCommit(boolean readOnly) {
                }
                
                public void afterCompletion(int status) {
                }
                
                public void afterCommit() {
                    try {
                        // We need to process this in a new transaction,
                        // AlertDef ID should be set now that original tx is
                        // committed
                        if (!(triggers.isEmpty())) {
                            AlertDefinition alertDefinition = getDefinitionFromTrigger((RegisteredTrigger) triggers
                                .iterator().next());
                            if (alertDefinition != null) {
                                zeventEnqueuer.enqueueEvent(new TriggersCreatedZevent(alertDefinition.getId()));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error creating triggers for alert definition.", e);
                    }
                }
            });
        } catch (Throwable t) {
            log.error("Error registering to create triggers for alert definition.", t);
        }
    }

    private RegisteredTriggerValue convertToTriggerValue(AppdefEntityID id, AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {

        // Create trigger based on the type of the condition
        RegisteredTriggerValue trigger;
        try {
            Class<?> trigClass = (Class<?>) ConditionalTriggerInterface.MAP_COND_TRIGGER
                .get(new Integer(cond.getType()));

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
     * Delete all triggers for an alert definition.
     * 
     * 
     */
    public void deleteAlertDefinitionTriggers(Integer adId) {
        AlertDefinition def = alertDefinitionDAO.findById(adId);
        unregisterTriggers(adId, def.getTriggers());
        triggerDAO.removeTriggers(def);
    }

    /**
     * Completely deletes all triggers when an alert definition is deleted
     * 
     * 
     */
    public void deleteTriggers(AlertDefinition alertDef) {
        unregisterTriggers(alertDef.getId(), alertDef.getTriggers());
        triggerDAO.deleteAlertDefinition(alertDef);
    }

    void setRegisteredTriggerRepository(RegisterableTriggerRepository registeredTriggerRepository) {
        this.registeredTriggerRepository = registeredTriggerRepository;
    }

    void setZeventEnqueuer(ZeventEnqueuer zeventEnqueuer) {
        this.zeventEnqueuer = zeventEnqueuer;
    }
}

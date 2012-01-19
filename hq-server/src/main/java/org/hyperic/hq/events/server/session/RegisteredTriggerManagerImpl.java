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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisterableTriggerRepository;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.events.shared.RegisteredTriggerManager;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.timer.StopWatch;
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
    
    private final Log traceLog = LogFactory.getLog(RegisteredTriggerManagerImpl.class.getName() + "Trace");

    private final AlertConditionEvaluatorFactory alertConditionEvaluatorFactory;

    private RegisterableTriggerRepository registeredTriggerRepository;

    private final TriggerDAOInterface triggerDAO;

    private ZeventEnqueuer zeventEnqueuer;

    private final AlertConditionEvaluatorRepository alertConditionEvaluatorRepository;

    private final AlertDefinitionDAOInterface alertDefinitionDAO;
    
    private final AlertDAO alertDAO;
    
    private final EventLogManager eventLogManager;
    
    private final DBUtil dbUtil;

    @Autowired
    public RegisteredTriggerManagerImpl(AlertConditionEvaluatorFactory alertConditionEvaluatorFactory,
                                        TriggerDAOInterface triggerDAO, ZeventEnqueuer zeventEnqueuer,
                                        AlertConditionEvaluatorRepository alertConditionEvaluatorRepository,
                                        AlertDefinitionDAOInterface alertDefinitionDAO, RegisterableTriggerRepository registerableTriggerRepository, 
                                        AlertDAO alertDAO, EventLogManager eventLogManager, DBUtil dbUtil) {
        this.alertConditionEvaluatorFactory = alertConditionEvaluatorFactory;
        this.triggerDAO = triggerDAO;
        this.zeventEnqueuer = zeventEnqueuer;
        this.alertConditionEvaluatorRepository = alertConditionEvaluatorRepository;
        this.alertDefinitionDAO = alertDefinitionDAO;
        this.registeredTriggerRepository = registerableTriggerRepository;
        this.alertDAO = alertDAO;
        this.eventLogManager = eventLogManager;
        this.dbUtil = dbUtil;
    }
    
    @PostConstruct
    public void cleanupRegisteredTriggers() {
        Connection conn = null;
        Statement stmt = null;
        Boolean autocommit = null;
        boolean commit = false;
        try {
            conn = dbUtil.getConnection();
            autocommit = Boolean.valueOf(conn.getAutoCommit());
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            stmt.addBatch(
                "update EAM_ALERT_CONDITION set trigger_id = null " +
                "WHERE exists (" +
                    "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' " +
                    "AND EAM_ALERT_CONDITION.alert_definition_id = id" +
                ")");
            stmt.addBatch(
                "delete from EAM_REGISTERED_TRIGGER WHERE exists (" +
                    "select 1 from EAM_ALERT_DEFINITION WHERE deleted = '1' " +
                    "AND EAM_REGISTERED_TRIGGER.alert_definition_id = id" +
                ")");
            int[] rows = stmt.executeBatch();
            conn.commit();
            commit = true;
            log.info("disassociated " + rows[0] + " triggers in EAM_ALERT_CONDITION" +
                " from their deleted alert definitions");
            log.info("deleted " + rows[1] + " rows from EAM_REGISTERED_TRIGGER");
        } catch (SQLException e) {
            log.error(e, e);
        }  finally {
            resetAutocommit(conn, autocommit);
            if (!commit) rollback(conn);
            DBUtil.closeJDBCObjects(
                RegisteredTriggerManagerImpl.class.getName(), conn, stmt, null);
        }
    }
    
    private void rollback(Connection conn) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException e) {
            log.error(e, e);
        }
    }

    private void resetAutocommit(Connection conn, Boolean autocommit) {
        try {
            if (autocommit != null) {
                conn.setAutoCommit(autocommit.booleanValue());
            }
        } catch (SQLException e) {
            log.error(e, e);
        }
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
    public void initializeTriggers() {
        StopWatch watch = new StopWatch();
        
        // Only initialize the enabled triggers. If disabled ones become enabled
        // we will lazy init
        log.info("Initializing triggers");
        this.registeredTriggerRepository.init();
        
        try {
            watch.markTimeBegin("findAllEnabledTriggers");
            Collection<RegisteredTrigger> registeredTriggers = triggerDAO.findAllEnabledTriggers();
            watch.markTimeEnd("findAllEnabledTriggers");
        
            log.info("Found " + registeredTriggers.size() + " enabled triggers");
        
            watch.markTimeBegin("initializeTriggers");
            initializeTriggers(registeredTriggers);
            watch.markTimeEnd("initializeTriggers");
        } finally {        
            log.info("Finished initializing triggers: " + watch);
        }
    }

    private void registerTriggers(Integer alertDefId) {
        Collection<RegisteredTrigger> registeredTriggers = getAllTriggersByAlertDefId(alertDefId);
        if (!(registeredTriggers.isEmpty())) {
            AlertDefinition alertDefinition = getDefinitionFromTrigger(registeredTriggers.iterator()
                                                                                                             .next());
            if (alertDefinition == null) {
                log.warn("Unable to find AlertDefinition with id: " + alertDefId + ".  These alerts will not fire.");
                return;
            }
            
            AlertConditionEvaluator alertConditionEvaluator = alertConditionEvaluatorFactory.create(alertDefinition);
            initializeAlertConditionEvaluatorState(alertConditionEvaluator);                        
            alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
            
            for (RegisteredTrigger tv : registeredTriggers) {
                // Try to register each trigger, if exception, then move on
                try {
                    registerTrigger(tv.getRegisteredTriggerValue(),
                                    alertConditionEvaluator,
                                    alertDefinition.isEnabled());
                } catch (Exception e) {
                    log.error("Error registering trigger", e);
                }
            }
        }
    }
    
    private void initializeAlertConditionEvaluatorState(AlertConditionEvaluator ace) {        
        Serializable initialState = null;
        
        if (ace instanceof RecoveryConditionEvaluator) {
            // HQ-1903: The initial state of a recovery alert definition
            // should be reconstituted from the db so that it is aware of
            // the current state of the problem alert definition.
            RecoveryConditionEvaluator rce = (RecoveryConditionEvaluator) ace;
            Integer alertDefId = rce.getRecoveringFromAlertDefinitionId();
           
            
            try {
                AlertDefinition def = alertDefinitionDAO.findById(alertDefId);
                Alert alert = alertDAO.findLastByDefinition(def, false);
                if (alert != null) {
                    AlertDefinition ad = alert.getAlertDefinition();
                    initialState = 
                        new AlertFiredEvent(alert.getId(), 
                                            ad.getId(), 
                                            AppdefUtil.newAppdefEntityId(ad.getResource()), 
                                            ad.getName(),
                                            alert.getTimestamp(), 
                                            null);
                }
            } catch (Exception e) {
               //Alert not found
            }
        }      
        
        if (initialState != null) {
            ace.initialize(initialState);
        }
    }

    private void initializeTriggers(Collection<RegisteredTrigger> registeredTriggers) {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        if (debug) {
            watch.markTimeBegin("findLastUnfixedAlertFiredEvents");
        }
            
        Map<Integer,AlertFiredEvent> alertFiredEvents = findLastUnfixedAlertFiredEvents(registeredTriggers);
        
        if (debug) {
            watch.markTimeEnd("findLastUnfixedAlertFiredEvents");
            watch.markTimeBegin("initializeAlertConditionEvaluators");
        }
                
        initializeAlertConditionEvaluators(registeredTriggers, alertFiredEvents);
        
        if (debug) {
            watch.markTimeEnd("initializeAlertConditionEvaluators");
            watch.markTimeBegin("registerTriggers[" + registeredTriggers.size() + "]");
        }
                
        for (RegisteredTrigger tv : registeredTriggers) {
            // Try to register each trigger, if exception, then move on
            try {
                AlertDefinition def = getDefinitionFromTrigger(tv);
            
                if (def != null) {
                    AlertConditionEvaluator evaluator = 
                        alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(def.getId());
                
                    if (evaluator != null) {
                        registerTrigger(tv.getRegisteredTriggerValue(),
                                        evaluator,
                                        def.isEnabled());
                    }
                }
            } catch (Exception e) {
                log.error("Error registering trigger", e);
            }
        }
        
        if (debug) {
            watch.markTimeEnd("registerTriggers[" + registeredTriggers.size() + "]");            
            log.debug("initializeTriggers: " + watch);
        }
    }

    /**
     * Create AlertConditionEvaluator for each AlertDefinition, so they can
     * be shared by all triggers associated with the alertDef
     * 
     * @param registeredTriggers Collection of all enabled triggers
     * @param alertFiredEvents Map of alert definition id {@link Integer} to {@link AlertFiredEvent}
     */
    private void initializeAlertConditionEvaluators(Collection<RegisteredTrigger> registeredTriggers,
                                                    Map<Integer,AlertFiredEvent> alertFiredEvents) {
        for (RegisteredTrigger tv : registeredTriggers) {
            try {
                AlertDefinition def = getDefinitionFromTrigger(tv);
                if (def == null) {
                    log.warn("Unable to find AlertDefinition for trigger with id " +
                             tv.getId() + ".  These alerts will not fire.");
                } else if (alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(def.getId()) == null) {
                    AlertConditionEvaluator alertConditionEvaluator =
                        alertConditionEvaluatorFactory.create(def);
                    Serializable initialState = null;
                    
                    if (alertConditionEvaluator instanceof RecoveryConditionEvaluator) {
                        RecoveryConditionEvaluator rce =
                            (RecoveryConditionEvaluator) alertConditionEvaluator;
                        if (alertFiredEvents == null){
                            log.warn("Initial state map empty for RecoveryConditionEvaluator of trigger with id "+
                                    tv.getId() + ". These alerts will not be fired");
                            continue;
                        }
                            
                        initialState = alertFiredEvents.get(
                            rce.getRecoveringFromAlertDefinitionId());
                    } else {
                        initialState = alertConditionEvaluatorRepository
                            .getStateRepository()
                            .getAlertConditionEvaluatorStates()
                            .get(def.getId());
                    }
                    
                    alertConditionEvaluator.initialize(initialState);
                    
                    Serializable initialExStrategyState =
                        alertConditionEvaluatorRepository
                        .getStateRepository()
                        .getExecutionStrategyStates()
                        .get(def.getId());
                    
                    alertConditionEvaluator.getExecutionStrategy()
                                           .initialize(initialExStrategyState);
                    
                    alertConditionEvaluatorRepository.addAlertConditionEvaluator(
                        alertConditionEvaluator);
                }
            } catch (Exception e) {
                log.error("Error retrieving alert definition for trigger", e);
            }
        }
    }
    
    /**
     * 
     * @return {@link Map} of alert definition id {@link Integer} to {@link AlertFiredEvent}
     */
    private Map<Integer,AlertFiredEvent> findLastUnfixedAlertFiredEvents(Collection<RegisteredTrigger> registeredTriggers) {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        Set<Integer> alertDefinitionIds = new HashSet<Integer>(registeredTriggers.size());

        if (debug) {
            watch.markTimeBegin("getRecoveringFromAlertDefId");
        }

        try {
            for (RegisteredTrigger tv : registeredTriggers) {
                AlertDefinition def = getDefinitionFromTrigger(tv);
                if (def != null) {
                    for (AlertCondition condition : def.getConditions()) {
                        if (condition.getType() == EventConstants.TYPE_ALERT) {
                            Integer recoveringFromAlertDefId =
                                Integer.valueOf(condition.getMeasurementId());
                            alertDefinitionIds.add(recoveringFromAlertDefId);                        
                            break;
                        }
                    }
                }
            }
        } finally {
            if (debug) {
                watch.markTimeEnd("getRecoveringFromAlertDefId");
                log.debug("alertDefinitionIds.size="+alertDefinitionIds.size());
                log.debug(watch);
                watch.markTimeBegin("find");
            }
        }

        Map<Integer,AlertFiredEvent> results = null;
        try {
            results = eventLogManager.findLastUnfixedAlertFiredEvents();
        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
        
        if (debug) {
            watch.markTimeEnd("find");
            log.debug("findLastUnfixedAlertFiredEvents: " + watch);
        }
        
        return results;
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
                     " is not supported.  Triggers of this type should be " +
                     "removed from the database.");
            return;
        }
        RegisterableTriggerInterface trigger =
            (RegisterableTriggerInterface) tc.newInstance();
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
        setAlertDefinitionTriggersEnabled(Collections.singletonList(alertDefId), enabled);
    }
    
    public void setAlertDefinitionTriggersEnabled(List<Integer> alertDefIds, boolean enabled) {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        if (debug) watch.markTimeBegin("getTriggerIdsByAlertDefIds");

        Map<Integer,List<Integer>> alertDefTriggerMap = getTriggerIdsByAlertDefIds(alertDefIds);

        if (debug) {
            watch.markTimeEnd("getTriggerIdsByAlertDefIds");
            watch.markTimeBegin("addPostCommitSetEnabledListener");
        }

        addPostCommitSetEnabledListener(alertDefTriggerMap, enabled);

        if (debug) {
            watch.markTimeEnd("addPostCommitSetEnabledListener");
            
            if (traceLog.isDebugEnabled()) {
                traceLog.debug("alertDefTriggerMap: " + alertDefTriggerMap);
            }

            log.debug("setAlertDefinitionTriggersEnabled: " + watch);
        }
    }
    
    

    private void addPostCommitSetEnabledListener(final Map<Integer,List<Integer>> alertDefTriggerMap,
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
                        setTriggersEnabled(alertDefTriggerMap, enabled);
                    } catch (Exception e) {
                        log.error("Error setting triggers enabled to " + enabled, e);
                    }
                }
            });
        } catch (Throwable t) {
            log.error("Error registering to set triggers enabled to " + enabled, t);
        }
    }

    void setTriggersEnabled(final Map<Integer,List<Integer>> alertDefTriggerMap, final boolean enabled) {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
                
       
            List<TriggersCreatedZevent> zevents = new ArrayList<TriggersCreatedZevent>();
            List<Integer> enabledTriggersToProcess = new ArrayList<Integer>();
            
            if (debug) {
                watch.markTimeBegin("processTriggers");
            }

            for (Integer alertDefId : alertDefTriggerMap.keySet()) {
                
                Collection<Integer> triggerIds =  alertDefTriggerMap.get(alertDefId);
                
                if (enabled && !triggerIds.isEmpty()) {
                    Integer triggerId = triggerIds.iterator().next();
                    if (registeredTriggerRepository.getTriggerById(triggerId) == null) {
                        zevents.add(new TriggersCreatedZevent(alertDefId));
                        continue;
                    }
                }
                enabledTriggersToProcess.addAll(triggerIds);
            }
            
            if (debug) {
                watch.markTimeEnd("processTriggers");
            }

            if (!zevents.isEmpty()) {
                if (debug) {
                    watch.markTimeBegin("enqueueEvents");
                }
                try {
                    zeventEnqueuer.enqueueEvents(zevents);
                } catch (InterruptedException e) {
                    log.error("Error sending event to create newly enabled triggers for alert definition "
                                  + alertDefTriggerMap.keySet() + ". These alerts will not fire", e);
                }
                if (debug) watch.markTimeEnd("enqueueEvents");
            }
            
            if (!enabledTriggersToProcess.isEmpty()) {
                if (debug) watch.markTimeBegin("setTriggersEnabled");
                registeredTriggerRepository.setTriggersEnabled(enabledTriggersToProcess, enabled);
                if (debug) watch.markTimeEnd("setTriggersEnabled");
            }
       
        
        log.debug("setTriggersEnabled: " + watch);
    }

    Map<Integer,List<Integer>> getTriggerIdsByAlertDefIds(List<Integer> alertDefIds) {
        return triggerDAO
                    .findTriggerIdsByAlertDefinitionIds(alertDefIds);
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
       
            for (RegisteredTrigger trigger : triggers) {
                this.registeredTriggerRepository.removeTrigger(trigger.getId());
            }
            alertConditionEvaluatorRepository.removeAlertConditionEvaluator(alertDefinitionId);
       
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
    public void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef)
        throws TriggerCreateException, InvalidOptionException, InvalidOptionValueException
    {
        createTriggers(subject, alertdef, true);
    }
    
    /**
     * Create new triggers
     *
     * @param subject The user creating the trigger
     * @param alertdef The alert definition value object
     * @param addTxListener Indicates whether a TriggersCreatedListener should be added.
     *                      The default value is true. HHQ-3423: To improve performance when
     *                      creating resource type alert definitions, this should be set to false.
     *                      If false, it is the caller's responsibility to call
     *                      addTriggersCreatedListener() to ensure triggers are registered.
     *  
     * 
     *
     * 
     */
    public void createTriggers(AuthzSubject subject, AlertDefinitionValue alertdef, boolean addTxListener) 
        throws TriggerCreateException, InvalidOptionException, InvalidOptionValueException
    {
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
        
        // HHQ-3423: Add the TransactionListener after all the triggers are created
        if (addTxListener) {
            addTriggersCreatedTxListener(triggers);
        }
    }
    public void addTriggersCreatedTxListener(final List list) {
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
                    final boolean debug = log.isDebugEnabled();
                    StopWatch watch = new StopWatch();
                    try {
                        // We need to process this in a new transaction,
                        // AlertDef ID should be set now that original tx is
                        // committed
                        if (!list.isEmpty()) {
                            Object object = list.get(0);
                            if (object instanceof Zevent) {
                                if (debug) watch.markTimeBegin(
                                    "enqueueEvents[" + list.size() + "]");
                                zeventEnqueuer.enqueueEvents(list);                             
                                if (debug) watch.markTimeEnd(
                                    "enqueueEvents[" + list.size() + "]");                                
                            } else if (object instanceof RegisteredTrigger) {
                                RegisteredTrigger trigger = (RegisteredTrigger) object;
                                AlertDefinition alertDefinition =
                                    getDefinitionFromTrigger(trigger);
                                if (alertDefinition != null) {
                                    if (debug) watch.markTimeBegin("enqueueEvent");
                                    zeventEnqueuer.enqueueEvent(
                                        new TriggersCreatedZevent(alertDefinition.getId()));                             
                                    if (debug) watch.markTimeEnd("enqueueEvent");
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error creating triggers for alert definition.", e);
                    } finally {
                        if (debug) {
                            log.debug("TransactionListener.afterCommit: time=" + watch);
                        }
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
     * @param adId The alert definition id
     * 
     * 
     */
    public void deleteTriggers(Integer adId) {
        AlertDefinition def = alertDefinitionDAO.findById(adId);        
        deleteTriggers(def);
    }

    /**
     * Completely deletes all triggers when an alert definition is deleted
     *
     * @param alertDef The alert definition
     * 
     * 
     */
    public void deleteTriggers(AlertDefinition alertDef) {
        unregisterTriggers(alertDef.getId(), alertDef.getTriggers());
        alertDef.clearTriggers();
    }

    void setRegisteredTriggerRepository(RegisterableTriggerRepository registeredTriggerRepository) {
        this.registeredTriggerRepository = registeredTriggerRepository;
    }

    void setZeventEnqueuer(ZeventEnqueuer zeventEnqueuer) {
        this.zeventEnqueuer = zeventEnqueuer;
    }
}

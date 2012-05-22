/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.bizapp.shared;

import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.SchedulerException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local interface for EventsBoss.
 */
public interface EventsBoss {
    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     */
    public int[] getAlertCount(int sessionID, org.hyperic.hq.appdef.shared.AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Get the number of alerts for the given array of AppdefEntityID's, mapping AppdefEntityID to it's alerts count
     * 
     */
    @Transactional(readOnly = true)
    public Map<AppdefEntityID, Integer> getAlertCountMapped(int sessionID, AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;
    
    /**
     * Create an alert definition
     */
    public AlertDefinitionValue createAlertDefinition(int sessionID, AlertDefinitionValue adval)
        throws org.hyperic.hq.events.AlertDefinitionCreateException, PermissionException,
        InvalidOptionException, InvalidOptionValueException, SessionException;

    /**
     * Create an alert definition for a resource type
     */
    public AlertDefinitionValue createResourceTypeAlertDefinition(int sessionID,
                                                                  AppdefEntityTypeID aetid,
                                                                  AlertDefinitionValue adval)
        throws org.hyperic.hq.events.AlertDefinitionCreateException, PermissionException,
        InvalidOptionException, InvalidOptionValueException, SessionNotFoundException,
        SessionTimeoutException;

    public Action createAction(int sessionID, Integer adid, String className, ConfigResponse config)
        throws SessionNotFoundException, SessionTimeoutException, ActionCreateException,
        PermissionException;

    /**
     * Activate/deactivate a collection of alert definitions
     */
    public void activateAlertDefinitions(int sessionID, java.lang.Integer[] ids, boolean activate)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Activate or deactivate alert definitions by AppdefEntityID.
     */
    public void activateAlertDefinitions(int sessionID,
                                         org.hyperic.hq.appdef.shared.AppdefEntityID[] eids,
                                         boolean activate) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Update just the basics
     */
    public void updateAlertDefinitionBasic(int sessionID, Integer alertDefId, String name,
                                           String desc, int priority, boolean activate)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    public void updateAlertDefinition(int sessionID, AlertDefinitionValue adval)
        throws TriggerCreateException, InvalidOptionException, InvalidOptionValueException,
        AlertConditionCreateException, ActionCreateException, SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Get actions for a given alert.
     * @param alertId the alert id
     */
    public List<ActionValue> getActionsForAlert(int sessionId, Integer alertId)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Update an action
     */
    public void updateAction(int sessionID, ActionValue aval) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Delete a collection of alert definitions
     */
    public void deleteAlertDefinitions(int sessionID, java.lang.Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Delete list of alerts
     */
    public void deleteAlerts(int sessionID, java.lang.Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Delete all alerts for a list of alert definitions
     * 
     */
    public int deleteAlertsForDefinitions(int sessionID, java.lang.Integer[] adids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Get an alert definition by ID
     */
    public AlertDefinitionValue getAlertDefinition(int sessionID, Integer id)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Find an alert by ID
     */
    public Alert getAlert(int sessionID, Integer id) throws SessionNotFoundException,
        SessionTimeoutException, AlertNotFoundException;

    /**
     * Get a list of all alert definitions
     */
    public PageList<AlertDefinitionValue> findAllAlertDefinitions(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Get a collection of alert definitions for a resource
     */
    public PageList<AlertDefinitionValue> findAlertDefinitions(int sessionID, AppdefEntityID id,
                                                               PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Get a collection of alert definitions for a resource or resource type
     */
    public PageList<AlertDefinitionValue> findAlertDefinitions(int sessionID,
                                                               AppdefEntityTypeID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Find all alert definition names for a resource
     * @return Map of AlertDefinition names and IDs
     */
    public Map<String, Integer> findAlertDefinitionNames(int sessionID, AppdefEntityID id,
                                                         Integer parentId)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException;

    /**
     * Find all alerts for an appdef resource
     */
    public PageList<Alert> findAlerts(int sessionID, AppdefEntityID id, long begin, long end,
                                      PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException;

    /**
     * Search alerts given a set of criteria
     * @param username the username
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     */
    public List<Escalatable> findRecentAlerts(String username, int count, int priority,
                                              long timeRange,
                                              org.hyperic.hq.appdef.shared.AppdefEntityID[] ids)
        throws LoginException, ApplicationException, ConfigPropertyException;

    /**
     * Search recent alerts given a set of criteria
     * @param sessionID the session token
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     */
    public List<Escalatable> findRecentAlerts(int sessionID, int count, int priority,
                                              long timeRange,
                                              org.hyperic.hq.appdef.shared.AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Get config schema info for an action class
     */
    public ConfigSchema getActionConfigSchema(int sessionID, String actionClass)
        throws SessionNotFoundException, SessionTimeoutException,
        org.hyperic.util.config.EncodingException;

    /**
     * Get config schema info for a trigger class
     */
    public ConfigSchema getRegisteredTriggerConfigSchema(int sessionID, String triggerClass)
        throws SessionNotFoundException, SessionTimeoutException,
        org.hyperic.util.config.EncodingException;

    public void deleteEscalationByName(int sessionID, String name) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, org.hyperic.hq.common.ApplicationException;

    public void deleteEscalationById(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, org.hyperic.hq.common.ApplicationException;

    /**
     * remove escalation by id
     */
    public void deleteEscalationById(int sessionID, java.lang.Integer[] ids)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        org.hyperic.hq.common.ApplicationException;

    /**
     * retrieve escalation name by alert definition id.
     */
    public Integer getEscalationIdByAlertDefId(int sessionID, Integer id,
                                               EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * set escalation name by alert definition id.
     */
    public void setEscalationByAlertDefId(int sessionID, Integer id, Integer escId,
                                          EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * unset escalation by alert definition id.
     */
    public void unsetEscalationByAlertDefId(int sessionID, Integer id, EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * retrieve escalation JSONObject by alert definition id.
     */
    public JSONObject jsonEscalationByAlertDefId(int sessionID, Integer id,
                                                 EscalationAlertType alertType)
        throws org.hyperic.hq.auth.shared.SessionException, PermissionException, JSONException;

    /**
     * retrieve escalation object by escalation id.
     */
    public Escalation findEscalationById(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException;

    public void addAction(int sessionID, Escalation e, ActionConfigInterface cfg, long waitTime)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    public void removeAction(int sessionID, Integer escId, Integer actId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Retrieve a list of {@link EscalationState}s, representing the active
     * escalations in the system.
     */
    public List<EscalationState> getActiveEscalations(int sessionId, int maxEscalations)
        throws org.hyperic.hq.auth.shared.SessionException;

    /**
     * Gets the escalatable associated with the specified state
     */
    public Escalatable getEscalatable(int sessionId, EscalationState state)
        throws org.hyperic.hq.auth.shared.SessionException;

    /**
     * retrieve all escalation policy names as a Array of JSONObject. Escalation
     * json finders begin with json* to be consistent with DAO finder convention
     */
    public JSONArray listAllEscalationName(int sessionID) throws JSONException,
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Create a new escalation. If alertDefId is non-null, the escalation will
     * also be associated with the given alert definition.
     */
    public Escalation createEscalation(int sessionID, String name, String desc, boolean allowPause,
                                       long maxWaitTime, boolean notifyAll, boolean repeat,
                                       EscalationAlertType alertType, Integer alertDefId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        DuplicateObjectException;

    /**
     * Update basic escalation properties
     */
    public void updateEscalation(int sessionID, Escalation escalation, String name, String desc,
                                 long maxWait, boolean pausable, boolean notifyAll, boolean repeat)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        DuplicateObjectException;

    public boolean acknowledgeAlert(int sessionID, EscalationAlertType alertType, Integer alertID,
                                    long pauseWaitTime, String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        ActionExecuteException;

    /**
     * Fix a single alert. Method is "NotSupported" since all the alert fixes
     * may take longer than the transaction timeout. No need for a transaction
     * in this context.
     */
    public void fixAlert(int sessionID, EscalationAlertType alertType, Integer alertID,
                         String moreInfo) throws SessionTimeoutException, SessionNotFoundException,
        PermissionException, ActionExecuteException;

    /**
     * Fix a batch of alerts. Method is "NotSupported" since all the alert fixes
     * may take longer than the transaction timeout. No need for a transaction
     * in this context.
     */
    public void fixAlert(int sessionID, EscalationAlertType alertType, Integer alertID,
                         String moreInfo, boolean fixAllPrevious) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, ActionExecuteException;

    /**
     * Get the last fix if available
     */
    public String getLastFix(int sessionID, Integer defId) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException;

    /**
     * Get a maintenance event by group id
     */
    public MaintenanceEvent getMaintenanceEvent(int sessionId, Integer groupId)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        SchedulerException;

    /**
     * Schedule a maintenance event
     */
    public MaintenanceEvent scheduleMaintenanceEvent(int sessionId, MaintenanceEvent event)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        SchedulerException;

    /**
     * Schedule a maintenance event
     */
    public void unscheduleMaintenanceEvent(int sessionId, MaintenanceEvent event)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        SchedulerException;

}

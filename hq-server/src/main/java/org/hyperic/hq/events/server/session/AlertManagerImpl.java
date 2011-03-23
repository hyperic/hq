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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.alert.data.AlertActionLogRepository;
import org.hyperic.hq.alert.data.AlertConditionRepository;
import org.hyperic.hq.alert.data.AlertRepository;
import org.hyperic.hq.alert.data.ResourceAlertDefinitionRepository;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.server.session.SubjectDeleteRequestedEvent;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.EntityNotFoundException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.MeasurementRepository;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventSource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.messaging.MessagePublisher;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link AlertManager}
 */
@Service
@Transactional
public class AlertManagerImpl implements AlertManager,
    ApplicationListener<SubjectDeleteRequestedEvent> {

    private AlertPermissionManager alertPermissionManager;

    private final Log log = LogFactory.getLog(AlertManagerImpl.class.getName());

    private Pager pojoPager;

    private ResourceAlertDefinitionRepository resAlertDefRepository;

    private AlertActionLogRepository alertActionLogRepository;

    private AlertRepository alertRepository;

    private AlertConditionRepository alertConditionRepository;

    private MeasurementRepository measurementRepository;

    private ResourceManager resourceManager;

    private AlertDefinitionManager alertDefinitionManager;

    private AuthzSubjectManager authzSubjectManager;

    private EscalationManager escalationManager;

    private MessagePublisher messagePublisher;

    private AlertRegulator alertRegulator;
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    @Autowired
    public AlertManagerImpl(AlertPermissionManager alertPermissionManager,
                            ResourceAlertDefinitionRepository resAlertDefRepository, 
                            AlertActionLogRepository alertActionLogRepository,
                            AlertRepository alertRepository, AlertConditionRepository alertConditionRepository,
                            MeasurementRepository measurementRepository, ResourceManager resourceManager,
                            AlertDefinitionManager alertDefinitionManager,
                            AuthzSubjectManager authzSubjectManager,
                            EscalationManager escalationManager, MessagePublisher messagePublisher,
                            AlertRegulator alertRegulator, ConcurrentStatsCollector concurrentStatsCollector) {
        this.alertPermissionManager = alertPermissionManager;
        this.resAlertDefRepository = resAlertDefRepository;
        this.alertActionLogRepository = alertActionLogRepository;
        this.alertRepository = alertRepository;
        this.alertConditionRepository = alertConditionRepository;
        this.measurementRepository = measurementRepository;
        this.resourceManager = resourceManager;
        this.alertDefinitionManager = alertDefinitionManager;
        this.authzSubjectManager = authzSubjectManager;
        this.escalationManager = escalationManager;
        this.alertRegulator = alertRegulator;
        this.messagePublisher = messagePublisher;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        pojoPager = Pager.getDefaultPager();
        concurrentStatsCollector.register(ConcurrentStatsCollector.FIRE_ALERT_TIME);
    }

    public void onApplicationEvent(SubjectDeleteRequestedEvent event) {
        subjectRemoved(((SubjectDeleteRequestedEvent) event).getSubject());
    }
    
    private void subjectRemoved(AuthzSubject toDelete) {
        escalationManager.handleSubjectRemoval(toDelete);
        handleSubjectRemoval(toDelete);        
    }
    
    /**
     * Create a new alert.
     * 
     * @param def The alert definition.
     * @param ctime The alert creation time.
     * 
     */
    public Alert createAlert(AlertDefinition def, long ctime) {
        Alert alert = new Alert();
        //TODO better way
        alert.setAlertDefinition((ResourceAlertDefinition)def);
        alert.setCtime(ctime);
        alertRepository.save(alert);
        ResourceAlertDefinition resAlertDef = alert.getAlertDefinition();
        if(resAlertDef.getLastFired() < alert.getCtime()) {
            resAlertDef.setLastFired(alert.getCtime());
            resAlertDefRepository.save(resAlertDef);
        }
        return alert;
    }

    /**
     * Simply mark an alert object as fixed
     * 
     * 
     */
    public void setAlertFixed(Alert alert) {
        alert.setFixed(true);

        // If the alert definition is set to "recover", then we should enable
        // it.
        AlertDefinition def = alert.getAlertDefinition();

        if (def.isWillRecover()) {
            try {
                alertDefinitionManager.updateAlertDefinitionInternalEnable(authzSubjectManager
                    .getOverlordPojo(), def, true);
            } catch (PermissionException e) {
                log.error("Error re-enabling alert with ID: " + def.getId() +
                          " after it was fixed.", e);
            }
        }
    }

    /**
     * Log the details of an action's execution
     * 
     * 
     */
    public void logActionDetail(Alert alert, Action action, String detail, AuthzSubject subject) {
        alert.createActionLog(detail, action, subject);
    }

    public void addConditionLogs(Alert alert, AlertConditionLogValue[] logs) {
        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = alertConditionRepository.findById(logs[i].getCondition().getId());
            if(cond == null) {
                throw new EntityNotFoundException("Alert Condition with ID: " +logs[i].getCondition().getId() + 
                    " was not found");
            }
            alert.createConditionLog(logs[i].getValue(), cond);
        }
    }

    /**
     * Remove alerts
     * 
     */
    public void deleteAlerts(Integer[] ids) {
        alertRepository.deleteByIds(Arrays.asList(ids));
    }

    /**
     * Remove alerts for an alert definition
     * @throws PermissionException
     * 
     */
    public int deleteAlerts(AuthzSubject subj, AlertDefinition ad) throws PermissionException {
        // ...check that user has delete permission on alert definition's resource...
        //TODO perm check
        //alertPermissionManager.canDeleteAlertDefinition(subj, ad.getAppdefEntityId());
        return alertRepository.deleteByAlertDefinition(ad);
    }

    /**
     * Remove alerts before the specified create time (ctime) that do not have an associated
     * escalation
     * The max number of records to delete is specified by maxDeletes
     */
    public int deleteAlerts(long before, int maxDeletes) {
        return alertRepository.deleteByCreateTime(before, maxDeletes);
    }
    
    @Transactional(readOnly = true)
    public Alert getAlertById(Integer id) {
        return alertRepository.findById(id);
    }

    /**
     * Find an alert pojo by ID
     */
    @Transactional(readOnly = true)
    public Alert findAlertById(Integer id) {
        Alert alert = alertRepository.findById(id);
        if(alert == null) {
            throw new EntityNotFoundException("Alert with ID: " + id + " was not found");
        }
        alert.setAckable(escalationManager.isAlertAcknowledgeable(alert.getId(), alert
            .getDefinition()));

        return alert;
    }

    /**
     * Find the last unfixed alert by definition ID
     *
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Alert findLastUnfixedByDefinition(AuthzSubject subj, Integer id) {
        try {
            AlertDefinition def = resAlertDefRepository.findById(id);
            if(def == null) {
                return null;
            }
            return alertRepository.findLastByDefinition(def, false);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Find the last alert by definition ID
     * @throws PermissionException
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Alert findLastFixedByDefinition(AlertDefinition def) {
        try {
            return alertRepository.findLastByDefinition(def, true);
        } catch (Exception e) {
            return null;
        }
    }
    
   
    /**
     * Get the # of alerts within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getAlertCount() {
        return alertRepository.count();
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * 
     */
    @Transactional(readOnly = true)
    public int[] getAlertCount(AppdefEntityID[] ids) {
        int[] counts = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isPlatform() || ids[i].isServer() || ids[i].isService()) {
                counts[i] = (int) alertRepository.countByResource(resourceManager.findResource(ids[i]));
            }
        }
        return counts;
    }

    /**
     * Processes {@link AlertConditionSatisfiedZEvent} that indicate that an
     * alert should be created
     * 
     * To minimize StaleStateExceptions, this method should only be called once
     * in one transaction.
     * 
     * 
     */
    @Transactional
    public void fireAlert(AlertConditionsSatisfiedZEvent event) {
        if (!alertRegulator.alertsAllowed()) {
            log.debug("Alert not firing because they are not allowed");
            return;
        }
        long startTime = System.currentTimeMillis();
        try {

            Integer adId = Integer.valueOf(((AlertConditionsSatisfiedZEventSource) event
                .getSourceId()).getId());

            AlertDefinition alertDef = null;

            // Check persisted alert def status
            if (!alertDefinitionManager.isEnabled(adId)) {
                return;
            }

            alertDef = alertDefinitionManager.getByIdNoCheck(adId);

            if (alertDef.getFrequencyType() == EventConstants.FREQ_ONCE || alertDef.isWillRecover()) {
                // Disable the alert definition now that we've fired
                alertDefinitionManager.updateAlertDefinitionInternalEnable(authzSubjectManager
                    .getOverlordPojo(), alertDef, false);
            }

            EscalatableCreator creator = new ClassicEscalatableCreator(alertDef, event,
                messagePublisher, this);
            Resource res = creator.getAlertDefinition().getResource();
            if (res == null || res.isInAsyncDeleteState()) {
                return;
            }

            // Now start escalation
            if (alertDef.getEscalation() != null) {
                escalationManager.startEscalation(alertDef, creator);
            } else {
                creator.createEscalatable();
            }

            if (log.isDebugEnabled()) {
                log.debug("Alert definition " + alertDef.getName() + " (id=" + alertDef.getId() +
                          ") fired.");
            }

            concurrentStatsCollector.addStat(System.currentTimeMillis() - startTime, ConcurrentStatsCollector.FIRE_ALERT_TIME);
        } catch (PermissionException e) {
            log.error("Alert not firing due to a permissions issue", e);
        } catch (ResourceDeletedException e) {
            log.debug(e, e);
        }
    }

    /**
     * Get a collection of alerts for an AppdefEntityID and time range
     * @throws PermissionException
     * 
     * 
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public PageList<Alert> findAlerts(AuthzSubject subj, AppdefEntityID id, long begin, long end,
                                      PageControl pc) throws PermissionException {
        // ...check that user has view permission on alert definition's resource...
        alertPermissionManager.canViewAlertDefinition(subj, id);
        List<Alert> alerts = alertRepository.findByResourceInRange(resourceManager.findResource(id),
            begin, end, pc.getSortattribute() == SortAttribute.NAME, pc.isAscending());

        return pojoPager.seek(alerts, pc);
    }

    /**
     * A more optimized look up which includes the permission checking
     * 
     */
    @Transactional(readOnly = true)
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
                                  boolean inEsc, boolean notFixed, Integer groupId,
                                  PageInfo pageInfo) throws PermissionException {
        return findAlerts(subj, priority, timeRange, endTime, inEsc, notFixed, groupId, null,
            pageInfo);
    }
    
    
    
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
                                  boolean inEsc, boolean notFixed, Integer groupId,
                                  Integer alertDefId, Sort sort) throws PermissionException {
        // [HHQ-2946] Only round up if end time is not a multiple of a minute
        long mod = endTime % 60000;
        if (mod > 0) {
            // Time voodoo the end time to the nearest minute so that we might
            // be able to use cached results.
            endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        }
        return alertRepository.findByCreateTimeAndPriority(endTime - timeRange, endTime, priority,
            inEsc, notFixed, groupId, alertDefId, sort);
    }

    @Transactional(readOnly = true)
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
        boolean inEsc, boolean notFixed, Integer groupId, Sort sort) throws PermissionException {
        return findAlerts(subj,priority,timeRange,endTime,inEsc,notFixed,groupId,null,sort);
    }

    /**
     * A more optimized look up which includes the permission checking
     * @return {@link List} of {@link Alert}s
     * 
     */
    @Transactional(readOnly = true)
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
                                  boolean inEsc, boolean notFixed, Integer groupId,
                                  Integer alertDefId, PageInfo pageInfo) throws PermissionException {
        // [HHQ-2946] Only round up if end time is not a multiple of a minute
        long mod = endTime % 60000;
        if (mod > 0) {
            // Time voodoo the end time to the nearest minute so that we might
            // be able to use cached results.
            endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        }
        AlertSortField sortField = (AlertSortField) pageInfo.getSort();
        Sort sort = new Sort(pageInfo.isAscending()? Direction.ASC: Direction.DESC,sortField.getSortString("a", "d", "r"));
        PageRequest pageRequest = new PageRequest(pageInfo.getPageNum(), pageInfo.getPageSize(), sort);
        
        return alertRepository.findByCreateTimeAndPriority(endTime - timeRange, endTime, priority,
            inEsc, notFixed, groupId, alertDefId, pageRequest).getContent();
    }
    
    

    

    /**
     * Search alerts given a set of criteria
     * 
     * @param timeRange the amount of milliseconds prior to current that the
     *        alerts will be contained in. e.g. the beginning of the time range
     *        will be (current - timeRante)
     * @param page TODO
     * 
     * @param includes {@link List} of {@link AppdefEntityID}s to filter, may be
     *        null for all.
     * 
     */
    @Transactional(readOnly = true)
    public List<Alert> findAlerts(AuthzSubject subj, int count, int priority, long timeRange,
                                  long endTime, List<AppdefEntityID> includes)
        throws PermissionException {
        List<Alert> result = new ArrayList<Alert>();
        final Set<AppdefEntityID> inclSet = (includes == null) ? null
                                                              : new HashSet<AppdefEntityID>(
                                                                  includes);

        for (int index = 0; result.size() < count; index++) {
            // Permission checking included
            PageInfo pInfo = PageInfo.create(index, count, AlertSortField.DATE, false);
            // XXX need to change this to pass in specific includes so that
            // the session does not blow up with too many objects
            List<Alert> alerts = findAlerts(subj.getId(), priority, timeRange, endTime, false,
                false, null, pInfo);
            if (alerts.size() == 0) {
                break;
            }
            if (inclSet != null) {
                for (Alert alert : alerts) {
                    AlertDefinition alertdef = alert.getAlertDefinition();

                    // Filter by appdef entity
                    //TODO resources of type
                    if(alertdef instanceof ResourceAlertDefinition) {
                        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(((ResourceAlertDefinition)alertdef).getResource());
                        if (!inclSet.contains(aeid)) {
                            continue;
                        }
                    }

                    // Add it
                    result.add(alert);

                    // Finished
                    if (result.size() == count) {
                        break;
                    }
                }
            } else {
                return alerts;
            }
        }

        return result;
    }

    /**
     * Find escalatables for a resource in a given time range.
     * 
     * @see findAlerts(AuthzSubject, int, int, long, long, List)
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Escalatable> findEscalatables(AuthzSubject subj, int count, int priority,
                                              long timeRange, long endTime,
                                              List<AppdefEntityID> includes)
        throws PermissionException {
        List<Alert> alerts = findAlerts(subj, count, priority, timeRange, endTime, includes);
        return convertAlertsToEscalatables(alerts);
    }

    /**
     * A more optimized look up which includes the permission checking
     * 
     */
    @Transactional(readOnly = true)
    public int getUnfixedCount(Integer subj, long timeRange, long endTime, Integer groupId)
        throws PermissionException {
        // Time voodoo the end time to the nearest minute so that we might
        // be able to use cached results
        endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        Number count = alertRepository.countByCreateTimeAndPriority(endTime - timeRange, endTime, 0,
            false, true, groupId, null);
        if (count != null)
            return count.intValue();

        return 0;
    }

    private List<Escalatable> convertAlertsToEscalatables(Collection<Alert> alerts) {
        List<Escalatable> res = new ArrayList<Escalatable>(alerts.size());

        for (Alert a : alerts) {
            // due to async deletes this could be null. just ignore and continue
            //TODO anyting with type alerts?
            if (a.getAlertDefinition() instanceof ResourceAlertDefinition && 
                    ((ResourceAlertDefinition)a.getAlertDefinition()).getResource().isInAsyncDeleteState()) {
                continue;
            }
            Escalatable e = ClassicEscalatableCreator.createEscalatable(a, getShortReason(a),
                getLongReason(a));
            res.add(e);
        }
        return res;
    }

    /**
     * Get the long reason for an alert
     * 
     */
    @Transactional(readOnly = true)
    public String getShortReason(Alert alert) {
        AlertDefinition def = alert.getAlertDefinition();
        Resource r = def.getResource();
        if (r == null || r.isInAsyncDeleteState()) {
            return "alertid=" + alert.getId() + " is associated with an invalid or deleted resource";
        }
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(r);
        AppdefEntityValue aev = new AppdefEntityValue(aeid, authzSubjectManager.getOverlordPojo());

        String name = "";

        try {
            name = aev.getName();
        } catch (AppdefEntityNotFoundException e) {
            log.warn("Alert short reason requested for invalid resource " + aeid);
        } catch (PermissionException e) {
            // Should never happen
            log.error("Overlord does not have permission for resource " + aeid);
        }

        // Get the alert definition's conditions
        Collection<AlertConditionLog> clogs = alert.getConditionLog();

        StringBuffer text = new StringBuffer(def.getName()).append(" ").append(name).append(" ");

        for (AlertConditionLog log : clogs) {
            AlertCondition cond = log.getCondition();

            switch (cond.getType()) {
                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                    // Value is already formatted by HHQ-2573
                    String actualValue = log.getValue();

                    text.append(cond.getName()).append(" (").append(actualValue).append(") ");
                    break;
                case EventConstants.TYPE_CONTROL:
                    text.append(cond.getName());
                    break;
                case EventConstants.TYPE_CHANGE:
                    text.append(cond.getName()).append(" (").append(log.getValue()).append(") ");
                    break;
                case EventConstants.TYPE_CUST_PROP:
                    text.append(cond.getName()).append(" (").append(log.getValue()).append(") ");
                    break;
                case EventConstants.TYPE_LOG:
                    text.append("Log (").append(log.getValue()).append(") ");
                    break;
                case EventConstants.TYPE_CFG_CHG:
                    text.append("Config changed (").append(log.getValue()).append(") ");
                    break;
                default:
                    break;
            }
        }

        // Get the short reason for the alert
        return text.toString();
    }

    /**
     * Get the long reason for an alert
     * 
     */
    @Transactional(readOnly = true)
    public String getLongReason(Alert alert) {
        final String indent = "    ";

        // Get the alert definition's conditions
        Collection<AlertConditionLog> clogs = alert.getConditionLog();

        AlertConditionLog[] logs = (AlertConditionLog[]) clogs.toArray(new AlertConditionLog[clogs
            .size()]);

        StringBuffer text = new StringBuffer();

        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = logs[i].getCondition();

            if (i == 0) {
                text.append("\n").append(indent).append("If ");
            } else {
                text.append("\n").append(indent).append(cond.isRequired() ? "AND " : "OR ");
            }

            Measurement dm = null;

            switch (cond.getType()) {
                case EventConstants.TYPE_THRESHOLD:
                case EventConstants.TYPE_BASELINE:
                    dm = measurementRepository.findById(new Integer(cond.getMeasurementId()));
                    if(dm == null) {
                        throw new EntityNotFoundException("Measurement with ID: " + cond.getMeasurementId() + 
                            " not found");
                    }
                    text.append(cond.describe(dm));

                    // Value is already formatted by HHQ-2573
                    String actualValue = logs[i].getValue();
                    text.append(" (actual value = ").append(actualValue).append(")");
                    break;
                case EventConstants.TYPE_CONTROL:
                    text.append(cond.describe(dm));
                    break;
                case EventConstants.TYPE_CHANGE:
                    text.append(cond.describe(dm)).append(" (New value: ").append(
                        logs[i].getValue()).append(")");
                    break;
                case EventConstants.TYPE_CUST_PROP:
                    text.append(cond.describe(dm)).append("\n").append(indent).append(
                        logs[i].getValue());
                    break;
                case EventConstants.TYPE_LOG:
                    text.append(cond.describe(dm)).append("\n").append(indent).append("Log: ")
                        .append(logs[i].getValue());
                    break;
                case EventConstants.TYPE_CFG_CHG:
                    text.append(cond.describe(dm)).append("\n").append(indent).append("Details: ")
                        .append(logs[i].getValue());
                    break;
                default:
                    break;
            }
        }

        return text.toString();
    }

    /**
     * 
     */
    public void handleSubjectRemoval(AuthzSubject subject) {
        alertActionLogRepository.removeSubject(subject);
    }

}

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.SubjectDeleteRequestedEvent;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventSource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
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
 * Default implementation of {@link AlertManager}
 */
@Service
@Transactional
public class AlertManagerImpl implements AlertManager,
    ApplicationListener<ApplicationEvent> {

    private AlertPermissionManager alertPermissionManager;

    private final Log log = LogFactory.getLog(AlertManagerImpl.class.getName());

    private Pager pojoPager;

    private AlertDefinitionDAO alertDefDao;

    private AlertActionLogDAO alertActionLogDAO;

    private AlertDAO alertDAO;

    private AlertConditionDAO alertConditionDAO;

    private MeasurementDAO measurementDAO;

    private ResourceManager resourceManager;

    private AlertDefinitionManager alertDefinitionManager;

    private AuthzSubjectManager authzSubjectManager;

    private EscalationManager escalationManager;

    private MessagePublisher messagePublisher;

    private AlertRegulator alertRegulator;
    private ConcurrentStatsCollector concurrentStatsCollector;

    private PermissionManager permissionManager;

    @Autowired
    public AlertManagerImpl(AlertPermissionManager alertPermissionManager,
                            AlertDefinitionDAO alertDefDao, AlertActionLogDAO alertActionLogDAO,
                            AlertDAO alertDAO, AlertConditionDAO alertConditionDAO,
                            MeasurementDAO measurementDAO, ResourceManager resourceManager,
                            AlertDefinitionManager alertDefinitionManager,
                            AuthzSubjectManager authzSubjectManager,
                            EscalationManager escalationManager, MessagePublisher messagePublisher,
                            AlertRegulator alertRegulator, ConcurrentStatsCollector concurrentStatsCollector,
                            PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        this.alertPermissionManager = alertPermissionManager;
        this.alertDefDao = alertDefDao;
        this.alertActionLogDAO = alertActionLogDAO;
        this.alertDAO = alertDAO;
        this.alertConditionDAO = alertConditionDAO;
        this.measurementDAO = measurementDAO;
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

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof SubjectDeleteRequestedEvent) {
            subjectRemoved(((SubjectDeleteRequestedEvent) event).getSubject());
        }
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
        alert.setAlertDefinition(def);
        alert.setCtime(ctime);
        alertDAO.save(alert);
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
        AlertConditionDAO dao = alertConditionDAO;
        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = dao.findById(logs[i].getCondition().getId());
            alert.createConditionLog(logs[i].getValue(), cond);
        }
    }

    /**
     * Remove alerts
     * 
     */
    public void deleteAlerts(Integer[] ids) {
        alertDAO.deleteByIds(ids);
    }

    /**
     * Remove alerts for an alert definition
     * @throws PermissionException
     * 
     */
    public int deleteAlerts(AuthzSubject subj, AlertDefinition ad) throws PermissionException {
        // ...check that user has delete permission on alert definition's resource...
        alertPermissionManager.canDeleteAlertDefinition(subj, ad.getAppdefEntityId());
        return alertDAO.deleteByAlertDefinition(ad);
    }

    /**
     * Remove alerts before the specified create time (ctime) that do not have an associated
     * escalation
     * The max number of records to delete is specified by maxDeletes
     */
    public int deleteAlerts(long before, int maxDeletes) {
        return alertDAO.deleteAlertsByCreateTime(before, maxDeletes);
    }
    
    @Transactional(readOnly = true)
    public Alert getAlertById(Integer id) {
        return alertDAO.get(id);
    }

    /**
     * Find an alert pojo by ID
     */
    @Transactional(readOnly = true)
    public Alert findAlertById(Integer id) {
        Alert alert = alertDAO.findById(id);
        Hibernate.initialize(alert);

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
            AlertDefinition def = alertDefDao.findById(id);
            return alertDAO.findLastByDefinition(def, false);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Find all last unfixed alerts
     *
     * 
     */
    @Transactional(readOnly=true)
    public Map<Integer,Alert> findAllLastUnfixed() {        
        StopWatch watch = new StopWatch();
        Map<Integer,Alert> unfixedAlerts = null;
        try {
            unfixedAlerts = 
                alertDAO.findAllLastUnfixed();
        } catch (Exception e) {
            unfixedAlerts = new HashMap<Integer,Alert>(0,1);
            log.error("Error finding all last unfixed alerts", e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("findAllLastUnfixed: " + watch);
            }
        }
        return unfixedAlerts;
    }
    
    
    /**
     * Find the last alerts for the given resource
     */
    @Transactional(readOnly=true)
    public Map<Integer,Alert> findLastByResource(AuthzSubject subj,  Resource r, boolean includeDescendants,
                                                 boolean fixed) {
            final StopWatch watch = new StopWatch();
            final boolean debug = log.isDebugEnabled();
            final List<ResourceType> types = new ArrayList<ResourceType>();
            types.add(resourceManager.findResourceTypeById(AuthzConstants.authzPlatform));
            types.add(resourceManager.findResourceTypeById(AuthzConstants.authzServer));
            types.add(resourceManager.findResourceTypeById(AuthzConstants.authzService));
            types.add(resourceManager.findResourceTypeById(AuthzConstants.authzGroup));
            final Set<Integer> viewable = permissionManager.findViewableResources(subj, types);
            final Collection<Resource> resources = new HashSet<Resource>();
            resources.add(r);
            setDescendants(r, includeDescendants, resources, debug, watch);
            final Map<Integer,Alert> unfixedAlerts = alertDAO.findLastByResource(resources, fixed);
            for (final Iterator<Entry<Integer, Alert>> it=unfixedAlerts.entrySet().iterator(); it.hasNext(); ) {
                final Entry<Integer, Alert> entry = it.next();
                final Integer alertDefId = entry.getKey();
                final AlertDefinition def = alertDefDao.get(alertDefId);
                if (def == null) {
                    continue;
                }
                final Resource resource = def.getResource();
                if (resource == null || resource.isInAsyncDeleteState()) {
                    continue;
                }
                if (!viewable.contains(resource.getId())) {
                    it.remove();
                }
            }
            if (debug) log.debug("findLastByResource: "  + watch);
            return unfixedAlerts;
    }

    private void setDescendants(Resource r, boolean includeDescendants, Collection<Resource> resources,
                                boolean debug, StopWatch watch) {
        if (includeDescendants) {
            if (debug) watch.markTimeBegin("findDescendantEdges");
            final Collection<ResourceEdge> edges =
                resourceManager.findDescendantResourceEdges(r, resourceManager.getContainmentRelation());
            for (final ResourceEdge edge : edges) {
                resources.add(edge.getTo());
            }
            if (debug) watch.markTimeEnd("findDescendantEdges");
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
            return alertDAO.findLastByDefinition(def, true);
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
    public Alert findLastByDefinition(Integer id) {
        try {
            AlertDefinition def = alertDefDao.findById(id);
            return alertDAO.findLastByDefinition(def);
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
        return new Integer(alertDAO.size());
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * 
     */
    @Transactional(readOnly = true)
    public int[] getAlertCount(AppdefEntityID[] ids) {
        AlertDAO dao = alertDAO;
        int[] counts = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isPlatform() || ids[i].isServer() || ids[i].isService()) {
                counts[i] = dao.countAlerts(resourceManager.findResource(ids[i])).intValue();
            }
        }
        return counts;
    }
    
    /**
     * Get the number of alerts for the given array of AppdefEntityID's as 
     * a map that maps the AppdefEntityID to it's resource alerts number
     */
    @Transactional(readOnly = true)
    public Map<AppdefEntityID, Integer> getAlertCountMapped(AppdefEntityID[] ids) {
        AlertDAO dao = alertDAO;
        Map<AppdefEntityID, Integer> counts = new HashMap<AppdefEntityID, Integer>();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isPlatform() || ids[i].isServer() || ids[i].isService()) {
            	counts.put(ids[i],dao.countAlerts(resourceManager.findResource(ids[i])).intValue());
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

            Integer adId = Integer.valueOf(((AlertConditionsSatisfiedZEventSource) event.getSourceId()).getId());

            // Check persisted alert def status
            if (!alertDefinitionManager.isEnabled(adId)) {
                return;
            }

            AlertDefinition alertDef = alertDefinitionManager.getByIdNoCheck(adId);
            Resource r = alertDef.getResource();
            if (r == null || r.isInAsyncDeleteState()) {
                return;
            }

            if (alertDef.getFrequencyType() == EventConstants.FREQ_ONCE || alertDef.isWillRecover()) {
                // Disable the alert definition now that we've fired
                alertDefinitionManager.updateAlertDefinitionInternalEnable(
                    authzSubjectManager.getOverlordPojo(), alertDef, false);
            }

            EscalatableCreator creator = new ClassicEscalatableCreator(alertDef, event, messagePublisher, this);

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
        List<Alert> alerts = alertDAO.findByAppdefEntityInRange(resourceManager.findResource(id),
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
        return alertDAO.findByCreateTimeAndPriority(subj, endTime - timeRange, endTime, priority,
            inEsc, notFixed, groupId, alertDefId, pageInfo);
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
                    AppdefEntityID aeid = alertdef.getAppdefEntityId();
                    if (!inclSet.contains(aeid)) {
                        continue;
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
     * A more optimized look up for a group which includes the permission checking
     * 
     */
    @Transactional(readOnly = true)
    public Map<Integer,List<Alert>> getUnfixedByResource(Integer subj, long timeRange, long endTime)
        throws PermissionException {
        return getUnfixedByResource(subj, timeRange, endTime, null);
    }

    /**
     * A more optimized look up for a resource which includes the permission checking
     * 
     */
    @Transactional(readOnly = true)
    public int getUnfixedCount(Integer subj, long timeRange, long endTime, Resource r)
        throws PermissionException {
        return getUnfixedByResource(subj, timeRange, endTime, r.getId()).size();
    }
    
    private Map<Integer, List<Alert>> getUnfixedByResource(Integer subj, long timeRange, long endTime, Integer resourceId)
    	throws PermissionException {
        // Time voodoo the end time to the nearest minute so that we might
        // be able to use cached results
        endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        return alertDAO.getUnfixedByResource(subj, endTime - timeRange, endTime, 0,
                                            false, true);
    }

    private List<Escalatable> convertAlertsToEscalatables(Collection<Alert> alerts) {
        List<Escalatable> res = new ArrayList<Escalatable>(alerts.size());

        for (Alert a : alerts) {
            // due to async deletes this could be null. just ignore and continue
            if (a.getAlertDefinition().getResource().isInAsyncDeleteState()) {
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
                    dm = measurementDAO.findById(new Integer(cond.getMeasurementId()));
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
        alertActionLogDAO.handleSubjectRemoval(subject);
    }

    public Collection<AlertDefinition> getOrphanedAlertDefs() {
        return alertDefDao.getOrphanedAlertDefs();
    }

    public void deleteAlertDef(AlertDefinition def) {
        alertDefDao.deleteByAlertDefinition(def);
    }

}

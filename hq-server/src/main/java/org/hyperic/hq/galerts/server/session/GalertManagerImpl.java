/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.SubjectDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupMembersChangedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupRelatedEvent;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.shared.CrispoManager;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.processor.GalertProcessor;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Service
@Transactional
public class GalertManagerImpl implements GalertManager, ApplicationListener<ApplicationEvent> {
    private final Log _log = LogFactory.getLog(GalertManagerImpl.class);

    private ExecutionStrategyTypeInfoDAO _stratTypeDAO;
    private final GalertDefDAO _defDAO;
    private final GalertAuxLogDAO _auxLogDAO;
    private final GalertLogDAO _logDAO;
    private final GalertActionLogDAO _actionLogDAO;
    private final CrispoManager crispoManager;
    private final EscalationManager escalationManager;
    private final GalertProcessor galertProcessor;
    private final ResourceGroupManager resourceGroupManager;

    @Autowired
    public GalertManagerImpl(ExecutionStrategyTypeInfoDAO stratTypeDAO, GalertDefDAO defDAO,
                             GalertAuxLogDAO auxLogDAO, GalertLogDAO logDAO,
                             GalertActionLogDAO actionLogDAO, CrispoManager crispoManager,
                             EscalationManager escalationManager,
                             ResourceGroupManager resourceGroupManager, GalertProcessor gAlertProcessor) {
        _stratTypeDAO = stratTypeDAO;
        _defDAO = defDAO;
        _auxLogDAO = auxLogDAO;
        _logDAO = logDAO;
        _actionLogDAO = actionLogDAO;
        this.crispoManager = crispoManager;
        this.escalationManager = escalationManager;
        this.resourceGroupManager = resourceGroupManager;
        this.galertProcessor = gAlertProcessor;
    }

    /**
     * Update basic properties of an alert definition
     * 
     * If any of the passed params are non-null, they will be updated with the
     * new value
     * 
     * 
     */
    public void update(GalertDef def, String name, String desc, AlertSeverity severity,
                       Boolean enabled) {
        boolean seriousUpdate = false;
        boolean updateName = false;

        if (def.isDeleted()) {
            throw new IllegalArgumentException("Unable to update a def "
                                               + "which has already been " + "deleted");
        }

        if (name != null) {
            def.setName(name);
            updateName = true;
        }

        if (desc != null) {
            def.setDescription(desc);
        }

        if (severity != null) {
            def.setSeverity(severity);
        }

        if (enabled != null) {
            def.setEnabled(enabled.booleanValue());
            seriousUpdate = true;
        }

        if (seriousUpdate) {
            def.setMtime(System.currentTimeMillis());
            galertProcessor.loadReloadOrUnload(def);
        } else if (updateName) {
            def.setMtime(System.currentTimeMillis());
            galertProcessor.alertDefUpdated(def, name);
        }
    }

    /**
     * Update the escalation of an alert def
     * 
     */
    public void update(GalertDef def, Escalation escalation) {
        def.setEscalation(escalation);
        def.setMtime(System.currentTimeMillis());

        // End any escalation we were previously doing.
        escalationManager.endEscalation(def);
        // If the alert def was in the middle of an escalation, ending the
        // escalation will prevent users from being aware that the alert def
        // has fired. Thus, they might not fix the alert and reset the triggers.
        // We'll reload the alert def so that the triggers are reset forcefully.
        galertProcessor.loadReloadOrUnload(def);
    }

    /**
     * Enable/disable an alert def
     * 
     */
    public void enable(GalertDef def, boolean enable) {
        update(def, null, null, null, Boolean.valueOf(enable));
    }

    /**
     * Enable/disable an alert def by id
     */
    public void enable(Integer id, boolean enable) {
    	// TODO: Add permission check
    	GalertDef def = findById(id);
    	enable(def, enable);
    }

    /**
     * Find all alert definitions for the specified group
     * 
     */
    @Transactional(readOnly = true)
    public PageList<GalertDef> findAlertDefs(ResourceGroup g, PageControl pc) {
        Pager pager = Pager.getDefaultPager();
        // TODO: G
        return pager.seek(_defDAO.findAll(g), pc);
    }

    /**
     * Find all group alert definitions.
     * 
     * @param minSeverity Minimum severity for returned defs
     * @param enabled If non-null specifies the nature of the 'enabled' flag for
     *        the results.
     * @param pInfo Paging information. Must contain a sort field from
     *        {@link GalertDefSortField}
     * 
     */
    @Transactional(readOnly = true)
    public List<GalertDef> findAlertDefs(AuthzSubject subj, AlertSeverity minSeverity,
                                         Boolean enabled, PageInfo pInfo) {
        return _defDAO.findAll(subj, minSeverity, enabled, pInfo);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ExecutionStrategyTypeInfo> findAllStrategyTypes() {
        return _stratTypeDAO.findAll();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ExecutionStrategyTypeInfo findStrategyType(Integer id) {
        return _stratTypeDAO.findById(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ExecutionStrategyTypeInfo findStrategyType(ExecutionStrategyType t) {
        return _stratTypeDAO.find(t);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public GalertDef findById(Integer id) {
        return _defDAO.findById(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public GalertAuxLog findAuxLogById(Integer id) {
        return _auxLogDAO.findById(id);
    }

    /**
     * Retrieve the Gtriggers for a partition in the given galert def.
     * 
     * @param id The galert def id.
     * @param partition The partition.
     * @return The list of Gtriggers.
     * 
     */
    @Transactional(readOnly = true)
    public List<Gtrigger> getTriggersById(Integer id, GalertDefPartition partition) {
        List<Gtrigger> triggers = new ArrayList<Gtrigger>();

        GalertDef def = findById(id);

        ExecutionStrategyInfo strategy = def.getStrategy(partition);

        if (strategy != null) {
            List<GtriggerInfo> triggerInfos = strategy.getTriggers();

            for (GtriggerInfo triggerInfo : triggerInfos) {
                Gtrigger trigger = triggerInfo.getTrigger();
                trigger.setGroup(def.getGroup());
                triggers.add(trigger);
            }
        }

        return triggers;
    }

    /**
     * Save the alert log and associated auxillary log information to the DB.
     * 
     * DevNote: Since the GalertAuxLog table needs to be written first (for
     * foreign-key from the auxType tables), we first traverse all the logs and
     * save them. Then, we perform the same traversal and save the specific
     * logs.
     * 
     * 
     */
    public GalertLog createAlertLog(GalertDef def, ExecutionReason reason)
        throws ResourceDeletedException {
        Resource r = def.getResource();
        if (r == null || r.isInAsyncDeleteState()) {
            throw ResourceDeletedException.newInstance(r);
        }
        Map<GalertAuxLog, AlertAuxLog> gAuxLogToAuxLog = new HashMap<GalertAuxLog, AlertAuxLog>(); // Stores
        // real
        // logs
        // to
        // auxType
        // logs
        GalertLog newLog = new GalertLog(def, reason, System.currentTimeMillis());
        addAuxLogChildren(newLog, null, reason.getAuxLogs(), gAuxLogToAuxLog);
        _logDAO.save(newLog);

        for (Map.Entry<GalertAuxLog, AlertAuxLog> ent : gAuxLogToAuxLog.entrySet()) {
            GalertAuxLog gAuxLog = ent.getKey();
            AlertAuxLog auxLog = ent.getValue();
            AlertAuxLogProvider provider = auxLog.getProvider();

            if (provider != null)
                provider.save(gAuxLog.getId().intValue(), auxLog);
        }
        def.setLastFired(new Long(System.currentTimeMillis()));
        return newLog;
    }

    private void addAuxLogChildren(GalertLog alert, GalertAuxLog parent, List<AlertAuxLog> auxLogs,
                                   Map<GalertAuxLog, AlertAuxLog> gAuxLogToAuxLog) {
        for (AlertAuxLog auxLog : auxLogs) {
            GalertAuxLog newLog;

            newLog = alert.addAuxLog(auxLog, parent);
            gAuxLogToAuxLog.put(newLog, auxLog);
            addAuxLogChildren(alert, newLog, auxLog.getChildren(), gAuxLogToAuxLog);
        }
    }

    /**
     * 
     */
    public void createActionLog(GalertLog alert, String detail, Action action, AuthzSubject subject) {
        GalertActionLog log = alert.createActionLog(detail, action, subject);

        _actionLogDAO.save(log);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<GalertLog> findAlertLogs(GalertDef def) {
        return _logDAO.findAll(def.getGroup());
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public GalertLog findLastFixedByDef(GalertDef def) {
        try {
            return _logDAO.findLastByDefinition(def, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simply sets the 'fixed' flag on an alert
     * 
     */
    public void fixAlert(GalertLog alert) {
        alert.setFixed(true);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Escalatable findEscalatableAlert(Integer id) {
        GalertLog alert = _logDAO.findById(id);
        _logDAO.getSession().refresh(alert);
        return GalertEscalatableCreator.createEscalatable(alert);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public GalertLog findAlertLog(Integer id) {
        return _logDAO.findById(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<GalertLog> findAlertLogs(ResourceGroup group) {
        return _logDAO.findAll(group);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<GalertLog> findAlertLogsByTimeWindow(ResourceGroup group, long begin, long end,
                                                         PageControl pc) {
        return _logDAO.findByTimeWindow(group, begin, end, pc);
    }

    /**
     * 
     */
    public List<GalertLog> findUnfixedAlertLogsByTimeWindow(ResourceGroup group, long begin,
                                                            long end) {
        return _logDAO.findUnfixedByTimeWindow(group, begin, end);
    }

    /**
     * @see findAlerts
     * @return a list of {@link Escalatable}s
     * 
     */
    public List<Escalatable> findEscalatables(AuthzSubject subj, int count, int priority,
                                              long timeRange, long endTime,
                                              List<AppdefEntityID> includes)
        throws PermissionException {
        List<GalertLog> alerts = findAlerts(subj, count, priority, timeRange, endTime, includes);
        List<Escalatable> res = new ArrayList<Escalatable>(alerts.size());

        for (GalertLog alert : alerts) {
            res.add(GalertEscalatableCreator.createEscalatable(alert));
        }
        return res;
    }

    /**
     * Find group alerts based on a set of criteria
     * 
     * @param subj Subject doing the finding
     * @param count Max # of alerts to return
     * @param priority A value from {@link EventConstants}
     * @param timeRange the amount of milliseconds prior to current that the
     *        alerts will be contained in. e.g. the beginning of the time range
     *        will be (current - timeRante)
     * @param includes A list of entity IDs to include in the result. If null
     *        then ignore and return all.
     * 
     * @return a list of {@link GalertLog}s
     * 
     */
    public List<GalertLog> findAlerts(AuthzSubject subj, int count, int priority, long timeRange,
                                      long endTime, List<AppdefEntityID> includes)
        throws PermissionException {
        PageInfo pInfo = PageInfo.create(0, count, GalertLogSortField.DATE, false);

        if (priority == EventConstants.PRIORITY_ALL) {
            // if priority is "all" set the severity code as low
            // this is essentially "all"
            priority = AlertSeverity.LOW.getCode();
        }
        
        AlertSeverity s = AlertSeverity.findByCode(priority);
        List<GalertLog> alerts = _logDAO.findByCreateTimeAndPriority(subj.getId(),
            endTime - timeRange, endTime, s, false, false, null, null, pInfo);

        List<GalertLog> result = new ArrayList<GalertLog>();
        for (GalertLog l : alerts) {
            GalertDef def = l.getAlertDef();
            if (def.getResource().isInAsyncDeleteState()) {
                continue;
            }

            // Filter by appdef entity
            AppdefEntityID aeid = def.getAppdefID();
            if (includes != null && !includes.contains(aeid))
                continue;

            result.add(l);
        }

        return result;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<GalertLog> findAlerts(AuthzSubject subj, AlertSeverity severity, long timeRange,
                                      long endTime, boolean inEsc, boolean notFixed,
                                      Integer groupId, PageInfo pInfo) {
        return findAlerts(subj, severity, timeRange, endTime, inEsc, notFixed, groupId, null, pInfo);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<GalertLog> findAlerts(AuthzSubject subj, AlertSeverity severity, long timeRange,
                                      long endTime, boolean inEsc, boolean notFixed,
                                      Integer groupId, Integer galertDefId, PageInfo pInfo) {
        return _logDAO.findByCreateTimeAndPriority(subj.getId(), endTime - timeRange, endTime,
            severity, inEsc, notFixed, groupId, galertDefId, pInfo);
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * 
     */
    public int[] fillAlertCount(AuthzSubject subj, AppdefEntityID[] ids, int[] counts)
        throws PermissionException {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isGroup()) {
                ResourceGroup group = resourceGroupManager.findResourceGroupById(subj, ids[i]
                    .getId());

                counts[i] = _logDAO.countAlerts(group).intValue();
            }
        }
        return counts;
    }
    
    /**
     * fill the number of alerts for the given array of AppdefEntityID's , mapping AppdefEntityID to it's alerts count
     * 
     */
    public void fillAlertCount(AuthzSubject subj, AppdefEntityID[] ids, Map<AppdefEntityID, Integer> counts)
        throws PermissionException {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isGroup()) {
                ResourceGroup group = resourceGroupManager.findResourceGroupById(subj, ids[i]
                    .getId());

                counts.put(ids[i], _logDAO.countAlerts(group).intValue());
            }
        }
    }

    /**
     * 
     */
    public void deleteAlertLog(GalertLog log) {
        _logDAO.remove(log);
    }

    /**
     * 
     */
    public void deleteAlertLogs(ResourceGroup group) {
        _logDAO.removeAll(group);
    }

    /**
     * Register an execution strategy.
     * 
     */
    public ExecutionStrategyTypeInfo registerExecutionStrategy(ExecutionStrategyType stratType) {
        ExecutionStrategyTypeInfo info = _stratTypeDAO.find(stratType);

        if (info != null) {
            _log.warn("Execution strategy type [" + stratType.getClass().getName() +
                      "] already registered");
            return info;
        }

        info = new ExecutionStrategyTypeInfo(stratType);
        _stratTypeDAO.save(info);
        return info;
    }

    /**
     * Unregister an execution strategy. This will fail if any alert definitions
     * are currently using the strategy
     * 
     */
    public void unregisterExecutionStrategy(ExecutionStrategyType sType) {
        ExecutionStrategyTypeInfo info = _stratTypeDAO.find(sType);

        if (info == null) {
            _log.warn("Execution strategy [" + sType.getClass().getName() +
                      "] already unregistered");
            return;
        }

        if (_defDAO.countByStrategy(info) != 0) {
            throw new IllegalArgumentException("Unable to unregister [ " +
                                               sType.getClass().getName() +
                                               "] alert defs are using it");
        }

        _stratTypeDAO.remove(info);
    }

    /**
     * Configure triggers for a given partition.
     * 
     * @param triggerInfos A list of {@link GtriggerTypeInfo}s
     * @param configs A list of {@link ConfigResponse}s, one for each trigger
     *        info
     * 
     * 
     */
    public void configureTriggers(GalertDef def, GalertDefPartition partition,
                                  List<GtriggerTypeInfo> triggerInfos, List<ConfigResponse> configs) {
        ExecutionStrategyInfo strat;

        if (triggerInfos.size() != configs.size()) {
            throw new IllegalArgumentException("Must be a config for each " + "trigger");
        }

        strat = def.getStrategy(partition);

        // Delete the old triggers if there were any
        List<Crispo> crispos = new ArrayList<Crispo>();
        for (GtriggerInfo t : strat.getTriggers()) {
            crispos.add(t.getConfigCrispo());
            _defDAO.remove(t);
        }

        for (Iterator<Crispo> i = crispos.iterator(); i.hasNext();) {
            crispoManager.deleteCrispo(i.next());
        }
        strat.clearTriggers();

        // Now add the new triggers
        Iterator<ConfigResponse> j = configs.iterator();
        for (GtriggerTypeInfo typeInfo : triggerInfos) {
            ConfigResponse config = j.next();
            Crispo crispo = crispoManager.create(config);
            GtriggerInfo t = strat.addTrigger(typeInfo, crispo, def.getGroup(), partition);

            _defDAO.save(t);
        }
        galertProcessor.loadReloadOrUnload(def);
    }

    /**
     * 
     */
    public ExecutionStrategyInfo addPartition(GalertDef def, GalertDefPartition partition,
                                              ExecutionStrategyTypeInfo stratType,
                                              ConfigResponse stratConfig) {
        Crispo stratCrispo = crispoManager.create(stratConfig);
        ExecutionStrategyInfo res = def.addPartition(partition, stratType, stratCrispo);

        _stratTypeDAO.save(res);
        galertProcessor.loadReloadOrUnload(def);
        return res;
    }

    /**
     * 
     */
    public GalertDef createAlertDef(AuthzSubject subject, String name, String description,
                                    AlertSeverity severity, boolean enabled, ResourceGroup group) {
        GalertDef def;

        def = new GalertDef(name, description, severity, enabled, group);

        _defDAO.save(def);
        galertProcessor.validateAlertDef(def);
        return def;
    }

    /**
     * Reload an alert definition. Probably should only be called internally
     * here.
     * 
     * 
     */
    public void reloadAlertDef(GalertDef def) {
        galertProcessor.loadReloadOrUnload(def);
    }

    /**
     * Mark an alert definition as deleted. This will remove it from all
     * dialogues, but will leave all the data (specific alerts) in place.
     * 
     * 
     */
    public void markDefDeleted(GalertDef def) {
        update(def, null, null, null, Boolean.FALSE);
        def.setEscalation(null);
        def.setDeleted(true);
    }

    /**
     * Delete an alert definition along with all logs which are tied to it.
     * 
     * 
     */
    public void nukeAlertDef(GalertDef def) {
        List<Crispo> nukeCrispos = new ArrayList<Crispo>();
        Integer defId = def.getId();

        for (AlertAuxLogProvider p : AlertAuxLogProvider.findAll()) {
            p.deleteAll(def);
        }
        _auxLogDAO.removeAll(def);

        // Kill the logs
        _logDAO.removeAll(def);

        for (ExecutionStrategyInfo strat : def.getStrategies()) {
            // Reconfigure the def to have 0 triggers (i.e. nuke the instances)
            // TODO: G (Collections.emptyList() should have worked, but at least
            // Eclipse issues an error)
            configureTriggers(def, strat.getPartition(), Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
            nukeCrispos.add(strat.getConfigCrispo());
        }

        _defDAO.remove(def);

        for (Crispo c : nukeCrispos) {
            crispoManager.deleteCrispo(c);
        }
        galertProcessor.alertDefDeleted(defId);
    }

    /**
     * Returns a list of {@link GalertDef}s using the passed escalation.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<GalertDef> getUsing(Escalation e) {
        return _defDAO.getUsing(e);
    }

    /**
     * Start an escalation for a group alert definition.
     * 
     * 
     */
    public void startEscalation(GalertDef def, ExecutionReason reason) {
        escalationManager.startEscalation(def, new GalertEscalatableCreator(this, def, reason));
    }

    /**
     * Remove all the galert defs associated with this resource group.
     * 
     * 
     */
    public void processGroupDeletion(ResourceGroup g) {
        Collection<GalertDef> defs = _defDAO.findAbsolutelyAllGalertDefs(g);

        for (GalertDef def : defs) {
            _log.debug("Cascade deleting GalertDef[" + def.getName() + "]");
            nukeAlertDef(def);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof GroupDeleteRequestedEvent) {
            processGroupDeletion(((GroupDeleteRequestedEvent) event).getGroup());
        } else if (event instanceof GroupMembersChangedEvent) {
            groupMembersChanged(((GroupRelatedEvent) event).getGroup());
        } else if (event instanceof SubjectDeleteRequestedEvent) {
            _actionLogDAO.handleSubjectRemoval(((SubjectDeleteRequestedEvent) event).getSubject());
        }

    }

    private void groupMembersChanged(ResourceGroup g) {
        Collection<GalertDef> defs = findAlertDefs(g, PageControl.PAGE_ALL);

        for (GalertDef def : defs) {
            reloadAlertDef(def);
        }
        _log.debug("Group members changed for group [" + g + "]");
    }

    @PostConstruct
    public void initialize() {
        galertProcessor.startupInitialize();
        // Make sure the escalation enumeration is loaded and registered so 
        // that the escalations run
        GalertEscalationAlertType.class.getClass();
        
        // Make sure we have the aux-log provider loaded
        GalertAuxLogProvider.class.toString();
    }
    
    @PreDestroy
    public void destroy() { 
        this._stratTypeDAO = null; 
    }//EOM 

}

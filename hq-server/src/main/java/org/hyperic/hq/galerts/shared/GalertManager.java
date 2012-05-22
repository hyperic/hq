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
package org.hyperic.hq.galerts.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertDefSortField;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for GalertManager
 */
public interface GalertManager {
    /**
     * Update basic properties of an alert definition If any of the passed
     * params are non-null, they will be updated with the new value
     */
    public void update(GalertDef def, String name, String desc, AlertSeverity severity,
                       Boolean enabled);

    /**
     * Update the escalation of an alert def
     */
    public void update(GalertDef def, Escalation escalation);

    /**
     * Enable/disable an alert def
     */
    public void enable(GalertDef def, boolean enable);

    /**
     * Enable/disable an alert def by id
     */
    public void enable(Integer id, boolean enable);

    /**
     * Find all alert definitions for the specified group
     */
    public PageList<GalertDef> findAlertDefs(ResourceGroup g, PageControl pc);

    /**
     * Find all group alert
     * @param minSeverity Minimum severity for returned defs
     * @param enabled If non-null specifies the nature of the 'enabled' flag for
     *        the
     * @param pInfo Paging Must contain a sort field from
     *        {@link GalertDefSortField}
     */
    public List<GalertDef> findAlertDefs(AuthzSubject subj, AlertSeverity minSeverity,
                                         Boolean enabled, PageInfo pInfo);

    public Collection<ExecutionStrategyTypeInfo> findAllStrategyTypes();

    public ExecutionStrategyTypeInfo findStrategyType(Integer id);

    public ExecutionStrategyTypeInfo findStrategyType(ExecutionStrategyType t);

    public GalertDef findById(Integer id);

    public GalertAuxLog findAuxLogById(Integer id);

    /**
     * Retrieve the Gtriggers for a partition in the given galert
     * @param id The galert def
     * @param partition The
     * @return The list of
     */
    public List<Gtrigger> getTriggersById(Integer id, GalertDefPartition partition);

    /**
     * Save the alert log and associated auxillary log information to the
     * DevNote: Since the GalertAuxLog table needs to be written first (for
     * foreign-key from the auxType tables), we first traverse all the logs and
     * save Then, we perform the same traversal and save the specific
     */
    public GalertLog createAlertLog(GalertDef def, ExecutionReason reason)
        throws ResourceDeletedException;

    public void createActionLog(GalertLog alert, String detail, Action action, AuthzSubject subject);

    public List<GalertLog> findAlertLogs(GalertDef def);

    public GalertLog findLastFixedByDef(GalertDef def);

    /**
     * Simply sets the 'fixed' flag on an alert
     */
    public void fixAlert(GalertLog alert);

    public Escalatable findEscalatableAlert(Integer id);

    public GalertLog findAlertLog(Integer id);

    public List<GalertLog> findAlertLogs(ResourceGroup group);

    public PageList<GalertLog> findAlertLogsByTimeWindow(ResourceGroup group, long begin, long end,
                                                         PageControl pc);

    public List<GalertLog> findUnfixedAlertLogsByTimeWindow(ResourceGroup group, long begin,
                                                            long end);

    public List<Escalatable> findEscalatables(AuthzSubject subj, int count, int priority,
                                              long timeRange, long endTime,
                                              List<AppdefEntityID> includes)
        throws PermissionException;

    /**
     * Find group alerts based on a set of criteria
     * @param subj Subject doing the finding
     * @param count Max # of alerts to return
     * @param priority A value from {@link EventConstants}
     * @param timeRange the amount of milliseconds prior to current that the
     *        alerts will be contained the beginning of the time range will be
     *        (current - timeRante)
     * @param includes A list of entity IDs to include in the If null then
     *        ignore and return
     * @return a list of {@link GalertLog}s
     */
    public List<GalertLog> findAlerts(AuthzSubject subj, int count, int priority, long timeRange,
                                      long endTime, List<AppdefEntityID> includes)
        throws PermissionException;

    public List<GalertLog> findAlerts(AuthzSubject subj, AlertSeverity severity, long timeRange,
                                      long endTime, boolean inEsc, boolean notFixed,
                                      Integer groupId, PageInfo pInfo);

    public List<GalertLog> findAlerts(AuthzSubject subj, AlertSeverity severity, long timeRange,
                                      long endTime, boolean inEsc, boolean notFixed,
                                      Integer groupId, Integer galertDefId, PageInfo pInfo);

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     */
    public int[] fillAlertCount(AuthzSubject subj, AppdefEntityID[] ids, int[] counts)
        throws PermissionException;

    /**
     * fill the number of alerts for the given array of AppdefEntityID's , mapping AppdefEntityID to it's alerts count
     */
	public void fillAlertCount(AuthzSubject subject,
			AppdefEntityID[] ids, Map<AppdefEntityID, Integer> counts)  throws PermissionException;;
	
    public void deleteAlertLog(GalertLog log);

    public void deleteAlertLogs(ResourceGroup group);

    /**
     * Register an execution
     */
    public ExecutionStrategyTypeInfo registerExecutionStrategy(ExecutionStrategyType stratType);

    /**
     * Unregister an execution This will fail if any alert definitions are
     * currently using the strategy
     */
    public void unregisterExecutionStrategy(ExecutionStrategyType sType);

    /**
     * Configure triggers for a given
     * @param triggerInfos A list of {@link GtriggerTypeInfo}s
     * @param configs A list of {@link ConfigResponse}s, one for each trigger
     *        info
     */
    public void configureTriggers(GalertDef def, GalertDefPartition partition,
                                  List<GtriggerTypeInfo> triggerInfos, List<ConfigResponse> configs);

    public ExecutionStrategyInfo addPartition(GalertDef def, GalertDefPartition partition,
                                              ExecutionStrategyTypeInfo stratType,
                                              ConfigResponse stratConfig);

    public GalertDef createAlertDef(AuthzSubject subject, String name, String description,
                                    AlertSeverity severity, boolean enabled, ResourceGroup group);

    /**
     * Reload an alert Probably should only be called internally
     */
    public void reloadAlertDef(GalertDef def);

    /**
     * Mark an alert definition as This will remove it from all dialogues, but
     * will leave all the data (specific alerts) in
     */
    public void markDefDeleted(GalertDef def);

    /**
     * Delete an alert definition along with all logs which are tied to
     */
    public void nukeAlertDef(GalertDef def);

    /**
     * Returns a list of {@link GalertDef}s using the passed
     */
    public Collection<GalertDef> getUsing(Escalation e);

    /**
     * Start an escalation for a group alert
     */
    public void startEscalation(GalertDef def, ExecutionReason reason);

    /**
     * Remove all the galert defs associated with this resource
     */
    public void processGroupDeletion(ResourceGroup g);

    

}

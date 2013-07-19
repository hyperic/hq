/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.events.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local interface for AlertManager.
 */
public interface AlertManager {
    /**
     * Create a new alert.
     * @param def The alert definition.
     * @param ctime The alert creation time.
     */
    public Alert createAlert(AlertDefinition def, long ctime);

    /**
     * Simply mark an alert object as fixed
     */
    public void setAlertFixed(Alert alert);

    /**
     * Log the details of an action's execution
     */
    public void logActionDetail(Alert alert, Action action, String detail, AuthzSubject subject);

    /**
     * Remove alerts
     */
    public void deleteAlerts(java.lang.Integer[] ids);

    /**
     * Remove alerts for an alert definition
     * @throws PermissionException
     */
    public int deleteAlerts(AuthzSubject subj, AlertDefinition ad) throws PermissionException;

    Alert getAlertById(Integer id);

    /**
     * Find an alert pojo by ID
     */
    public Alert findAlertById(Integer id);

    /**
     * Find the last alert by definition ID
     * @throws PermissionException
     */
    public Alert findLastUnfixedByDefinition(AuthzSubject subj, Integer id);
    
    /**
     * Find all last unfixed alerts
     *
     * 
     */
    public Map<Integer,Alert> findAllLastUnfixed();
    
    /**
     * Find the last alerts for the given resource
     *
     * 
     */
    public Map<Integer,Alert> findLastByResource(AuthzSubject subj, 
                                  Resource r,
                                  boolean includeDescendants,
                                  boolean fixed);

    /**
     * Find the last alert by definition ID
     * @throws PermissionException
     */
    public Alert findLastFixedByDefinition(AlertDefinition def);
    
    Alert findLastByDefinition(Integer id);

    /**
     * Get the # of alerts within HQ inventory
     */
    public Number getAlertCount();

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     */
    public int[] getAlertCount(org.hyperic.hq.appdef.shared.AppdefEntityID[] ids);

    
    /**
     * Get the number of alerts for the given array of AppdefEntityID's as 
     * a map that maps the AppdefEntityID to it's resource alerts number
     * 
     */
    @Transactional(readOnly = true)
    public Map<AppdefEntityID, Integer> getAlertCountMapped(AppdefEntityID[] ids);
    
    /**
     * Processes {@link AlertConditionSatisfiedZEvent} that indicate that an
     * alert should be created To minimize StaleStateExceptions, this method
     * should only be called once in one transaction.
     */
    public void fireAlert(AlertConditionsSatisfiedZEvent event);

    /**
     * Get a collection of alerts for an AppdefEntityID and time range
     * @throws PermissionException
     */
    public PageList<Alert> findAlerts(AuthzSubject subj, AppdefEntityID id, long begin, long end,
                                      PageControl pc) throws PermissionException;

    /**
     * A more optimized look up which includes the permission checking
     */
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
                                  boolean inEsc, boolean notFixed, Integer groupId,
                                  PageInfo pageInfo) throws PermissionException;

    /**
     * A more optimized look up which includes the permission checking
     * @return {@link List} of {@link Alert}s
     */
    public List<Alert> findAlerts(Integer subj, int priority, long timeRange, long endTime,
                                  boolean inEsc, boolean notFixed, Integer groupId,
                                  Integer alertDefId, PageInfo pageInfo) throws PermissionException;

    /**
     * Search alerts given a set of criteria
     * @param timeRange the amount of milliseconds prior to current that the
     *        alerts will be contained in. e.g. the beginning of the time range
     *        will be (current - timeRante)
     * @param page TODO
     * @param includes {@link List} of {@link AppdefEntityID}s to filter, may be
     *        null for all.
     */
    public List<Alert> findAlerts(AuthzSubject subj, int count, int priority, long timeRange,
                                  long endTime, List<AppdefEntityID> includes)
        throws PermissionException;

    /**
     * Find escalatables for a resource in a given time range.
     * @see findAlerts(AuthzSubject, int, int, long, long, List)
     */
    public List<Escalatable> findEscalatables(AuthzSubject subj, int count, int priority,
                                              long timeRange, long endTime,
                                              List<AppdefEntityID> includes)
        throws PermissionException;

    /**
     * A more optimized look up for a group which includes the permission checking
     */
    public Map<Integer,List<Alert>> getUnfixedByResource(Integer subj, long timeRange, long endTime)
        throws PermissionException;

    /**
     * A more optimized look up a resource which includes the permission checking
     */
    public int getUnfixedCount(Integer subj, long timeRange, long endTime, Resource r)
        throws PermissionException;

    /**
     * Get the long reason for an alert
     */
    public String getShortReason(Alert alert);

    /**
     * Get the long reason for an alert
     */
    public String getLongReason(Alert alert);

    public void handleSubjectRemoval(AuthzSubject subject);

    /**
     * Remove alerts before the specified create time (ctime) that do not have an associated
     * escalation
     * The max number of records to delete is specified by maxDeletes
     */
    public int deleteAlerts(long before, int maxDeletes);

    public Collection<AlertDefinition> getOrphanedAlertDefs();

    public void deleteAlertDef(AlertDefinition def);

}

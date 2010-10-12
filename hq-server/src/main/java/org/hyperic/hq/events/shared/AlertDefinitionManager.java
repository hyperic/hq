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
package org.hyperic.hq.events.shared;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.server.session.AlertDefSortField;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AlertDefinitionManager.
 */
public interface AlertDefinitionManager {
    /**
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AuthzSubject subj, AlertDefinitionValue a)
        throws AlertDefinitionCreateException, PermissionException;

    /**
     * Create a new alert definition
     */
    public AlertDefinitionValue createAlertDefinition(AlertDefinitionValue a)
    	throws AlertDefinitionCreateException;

    /**
     * Update just the basics
     * @throws PermissionException
     */
    public void updateAlertDefinitionBasic(AuthzSubject subj, java.lang.Integer id, java.lang.String name,
                                           java.lang.String desc, int priority, boolean activate)
        throws PermissionException;

    /**
     * Update an alert definition
     */
    public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval) throws AlertConditionCreateException,
        ActionCreateException;

    /**
     * Activate/deactivate an alert
     */
    public void updateAlertDefinitionsActiveStatus(AuthzSubject subj, java.lang.Integer[] ids, boolean activate)
        throws PermissionException;

    /**
     * Activate/deactivate an alert
     */
    public void updateAlertDefinitionActiveStatus(AuthzSubject subj,
                                                  org.hyperic.hq.events.server.session.AlertDefinition def,
                                                  boolean activate) throws PermissionException;

    /**
     * Enable/Disable an alert For internal use only where the mtime does not
     * need to be reset on each
     * @return <code>true</code> if the enable/disable
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj,
                                                       org.hyperic.hq.events.server.session.AlertDefinition def,
                                                       boolean enable) throws PermissionException;

    /**
     * Enable/Disable an alert For internal use only where the mtime does not
     * need to be reset on each
     * @return <code>true</code> if the enable/disable
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj, java.lang.Integer defId, boolean enable)
        throws PermissionException;
    
    
    /**
     * Enable/Disable an alert definition. For internal use only where the mtime
     * does not need to be reset on each update.
     *
     * @return <code>true</code> if the enable/disable succeeded.
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj,
                                                       List<Integer> ids,
                                                       boolean enable)
        throws PermissionException;

    /**
     * Set the escalation on the alert definition
     */
    public void setEscalation(AuthzSubject subj, java.lang.Integer defId, java.lang.Integer escId)
        throws PermissionException;

    /**
     * Returns the {@link AlertDefinition}s using the passed
     */
    public Collection<AlertDefinition> getUsing(Escalation e);

    /**
     * Remove alert definitions
     */
    public void deleteAlertDefinitions(AuthzSubject subj, java.lang.Integer[] ids) throws PermissionException;

    /**
     * Clean up alert definitions and alerts for removed resources
     */
    public void cleanupAlertDefs(List<Integer> alertDefIds);
    
    List<Integer> getAllDeletedAlertDefs();

    /**
     * Find an alert definition and return a value object
     * @throws PermissionException if user does not have permission to manage
     *         alerts
     */
    public AlertDefinitionValue getById(AuthzSubject subj, java.lang.Integer id) throws PermissionException;

    /**
     * Find an alert definition
     * @throws PermissionException if user does not have permission to manage
     *         alerts
     */
    public AlertDefinition getByIdAndCheck(AuthzSubject subj, java.lang.Integer id) throws PermissionException;

    /**
     * Find an alert definition and return a basic This is called by the
     * abstract trigger, so it does no permission
     * @param id The alert def
     */
    public AlertDefinition getByIdNoCheck(Integer id);

    /**
     * Check if an alert definition is a resource type alert
     * @param id The alert def
     * @return <code>true</code> if the alert definition is a resource type
     *         alert
     * 
     */
    public boolean isResourceTypeAlertDefinition(Integer id);

    public AlertDefinition findAlertDefinitionById(Integer id);

    /**
     * Get an alert definition's name
     */
    public String getNameById(Integer id);

    /**
     * Get an alert definition's conditions
     */
    public AlertConditionValue[] getConditionsById(Integer id);

    /**
     * Get list of alert conditions for a resource or resource type
     */
    public boolean isAlertDefined(AppdefEntityID id, java.lang.Integer parentId);

    /**
     * Get list of all alert conditions
     * @return a PageList of {@link AlertDefinitionValue} objects
     */
    public PageList<AlertDefinitionValue> findAllAlertDefinitions(AuthzSubject subj);

    /**
     * Get the resource-specific alert definition ID by parent ID, allowing for
     * the query to return a stale copy of the alert definition (for efficiency
     * reasons).
     * @param aeid The
     * @param pid The ID of the resource type alert definition (parent ID).
     * @param allowStale <code>true</code> to allow stale copies of an alert
     *        definition in the query results; <code>false</code> to never allow
     *        stale copies, potentially always forcing a sync with the
     * @return The alert definition ID or <code>null</code> if no alert
     *         definition is found for the
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid, java.lang.Integer pid, boolean allowStale);

    /**
     * Find alert definitions passing the
     * @param minSeverity Specifies the minimum severity that the defs should be
     *        set for
     * @param enabled If non-null, specifies the nature of the returned
     *        definitions (i.e. only return enabled or disabled defs)
     * @param excludeTypeBased If true, exclude any alert definitions associated
     *        with a type-based
     * @param pInfo Paging The sort field must be a value from
     *        {@link AlertDefSortField}
     */
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subj,
                                                      org.hyperic.hq.events.AlertSeverity minSeverity,
                                                      java.lang.Boolean enabled, boolean excludeTypeBased,
                                                      org.hyperic.hibernate.PageInfo pInfo);

    /**
     * Get the list of type-based alert
     * @param enabled If non-null, specifies the nature of the returned
     * @param pInfo Paging The sort field must be a value from
     *        {@link AlertDefSortField}
     */
    public List<AlertDefinition> findTypeBasedDefinitions(AuthzSubject subj, java.lang.Boolean enabled,
                                                          org.hyperic.hibernate.PageInfo pInfo)
        throws PermissionException;

    /**
     * Get list of alert definition POJOs for a resource
     * @throws PermissionException if user cannot manage alerts for resource
     */
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subject,
                                                      org.hyperic.hq.appdef.shared.AppdefEntityID id)
        throws PermissionException;

    public PageList<AlertDefinitionValue> findAlertDefinitions(AuthzSubject subj,
                                                               org.hyperic.hq.appdef.shared.AppdefEntityID id,
                                                               org.hyperic.util.pager.PageControl pc)
        throws PermissionException;

    /**
     * Get list of alert definitions for a resource
     */
    public List<AlertDefinition> findAlertDefinitions(AuthzSubject subject,
                                                      org.hyperic.hq.authz.server.session.Resource prototype)
        throws PermissionException;

    /**
     * Get list of alert conditions for a resource or resource type
     */
    public PageList<AlertDefinitionValue> findAlertDefinitions(AuthzSubject subj,
                                                               org.hyperic.hq.appdef.shared.AppdefEntityTypeID aetid,
                                                               org.hyperic.util.pager.PageControl pc)
        throws PermissionException;

    /**
     * Get a list of all alert definitions for the resource and its descendents
     * @param subj the caller
     * @param res the root resource
     * @return a list of alert definitions
     */
    public List<AlertDefinition> findRelatedAlertDefinitions(AuthzSubject subj,
                                                             org.hyperic.hq.authz.server.session.Resource res);

    /**
     * Get a list of all alert definitions with an availability metric condition
     * @param subj the caller
     * @return a list of alert definitions
     */
    public List<AlertDefinition> findAvailAlertDefinitions(AuthzSubject subj) 
		throws PermissionException;
    
    /**
     * Get list of children alert definition for a parent alert definition
     */
    public PageList<AlertDefinitionValue> findAlertDefinitionChildren(Integer id);

    /**
     * Get list of alert definition names for a resource
     */
    public SortedMap<String, Integer> findAlertDefinitionNames(AuthzSubject subj,
                                                               org.hyperic.hq.appdef.shared.AppdefEntityID id,
                                                               java.lang.Integer parentId) throws PermissionException;

    /**
     * Get list of alert definition names for a resource
     */
    public SortedMap<String, Integer> findAlertDefinitionNames(AppdefEntityID id, java.lang.Integer parentId);

    /**
     * Clone the parent actions into the alert definition.
     */
    public void cloneParentActions(AppdefEntityID parentId, AlertDefinitionValue child,
                                   ActionValue[] actions);
    
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
        throws MeasurementNotFoundException;
    
    /**
     * Return array of two values: enabled and act on trigger ID
     */
    public boolean isEnabled(Integer id);

    public int getActiveCount();

}
